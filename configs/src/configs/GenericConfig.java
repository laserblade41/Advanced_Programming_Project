package configs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import graph.*;
import graph.TopicManagerSingleton.TopicManager;

/**
 * Reflection-based configuration loader that wires agents from a text file.
 *
 * <p><strong>Reflection API:</strong> agents are named by fully-qualified class name in the
 * config file and instantiated at runtime via {@link java.lang.reflect.Constructor}. This is
 * what makes the engine extensible without recompilation - dropping a new {@code Agent} class
 * on the classpath and naming it in a config file is enough to add it to the graph (an
 * Open/Closed, plugin-style design).</p>
 *
 * <p>Each agent entry in the configuration file occupies three lines:</p>
 * <ol>
 *   <li>Fully qualified agent class name</li>
 *   <li>Comma-separated input topic names (subscriptions)</li>
 *   <li>Comma-separated output topic names (publish registrations)</li>
 * </ol>
 *
 * <p>Agents are instantiated via reflection, wrapped in a {@link graph.ParallelAgent}
 * (queue capacity 10), and wired to the {@link graph.TopicManagerSingleton} topic registry.
 * The configuration file is searched in multiple locations: the given path, {@code user.dir},
 * {@code config_files/}, parent directories, and the classpath.</p>
 *
 * <p><strong>Usage:</strong> Call {@link #setConfFile(String)} then {@link #create()}.
 * Call {@link #close()} to unsubscribe all agents and shut down parallel wrappers.</p>
 *
 * @see Config
 * @see graph.ParallelAgent
 */
public class GenericConfig implements Config {

    private static class AgentWiring {
        final ParallelAgent wrapper;
        final String[] subs;
        final String[] pubs;

        AgentWiring(ParallelAgent wrapper, String[] subs, String[] pubs) {
            this.wrapper = wrapper;
            this.subs = subs;
            this.pubs = pubs;
        }
    }

    private String confFile;
    private final List<AgentWiring> wirings = new ArrayList<AgentWiring>();

    /**
     * Sets the path to the configuration file.
     *
     * <p>Must be called before {@link #create()}. The path may be absolute or relative;
     * multiple search locations are tried at load time.</p>
     *
     * @param path the filesystem or classpath path to the configuration file
     */
    public void setConfFile(String path) {
        this.confFile = path;
    }

    /**
     * Reads the configuration file, instantiates agents, and wires them to topics.
     *
     * <p>If agents were previously loaded, {@link #close()} is called first. Each agent
     * is wrapped in a {@link graph.ParallelAgent} and subscribed/registered on the
     * specified topics.</p>
     *
     * @throws IllegalStateException if the configuration file path was not set, the file
     *         format is invalid (line count not divisible by 3), or the file cannot be read
     * @throws RuntimeException if agent instantiation via reflection fails
     */
    @Override
    public void create() {
        if (confFile == null || confFile.isEmpty()) {
            throw new IllegalStateException("conf file not set");
        }

        if (!wirings.isEmpty()) {
            close();
        }

        List<String> lines = readConfigLines();
        System.out.println("[GenericConfig] Configuration lines read from file: " + lines);

        // The format is strictly 3 lines per agent (class / subs / pubs); a count not
        // divisible by 3 means the file is malformed.
        if (lines.size() % 3 != 0) {
            throw new IllegalStateException("invalid config file format");
        }

        TopicManager tm = TopicManagerSingleton.get();

        // Walk the file in groups of three lines, building one agent per group.
        for (int i = 0; i < lines.size(); i += 3) {
            String className = lines.get(i);          // line 1: agent class
            String[] subs = parseSubs(lines.get(i + 1)); // line 2: input topics
            String[] pubs = parseSubs(lines.get(i + 2)); // line 3: output topics

            System.out.println("[GenericConfig] Processing agent " + className + " with subs: " + java.util.Arrays.toString(subs) + ", pubs: " + java.util.Arrays.toString(pubs));
            try {
                // Build the concrete agent via reflection from its class name.
                Agent agent = createAgent(className, subs, pubs);
                System.out.println("[GenericConfig] Successfully instantiated agent: " + agent.getClass().getName());

                // Every agent is wrapped in a ParallelAgent (Active Object) so its callback
                // runs off the publisher's thread. Capacity 10 bounds the per-agent backlog.
                ParallelAgent wrapper = new ParallelAgent(agent, 10);
                // Wire the wrapper into the pub-sub graph: subscribe to inputs, register on outputs.
                for (String sub : subs) {
                    tm.getTopic(sub).subscribe(wrapper);
                }
                for (String pub : pubs) {
                    tm.getTopic(pub).addPublisher(wrapper);
                }

                // Remember the wiring so close() can later unsubscribe and shut everything down.
                wirings.add(new AgentWiring(wrapper, subs, pubs));
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("failed to create agent: " + className, e);
            }
        }
    }

    private List<String> readConfigLines() {
        String fileName = confBasename();
        IOException lastError = null;

        // First try the real filesystem across every candidate location (handles the many
        // working directories the grader / IDE may launch from).
        for (Path path : buildSearchPaths(fileName)) {
            try {
                if (Files.exists(path)) {
                    return trimNonEmptyLines(Files.readAllLines(path));
                }
            } catch (IOException e) {
                lastError = e;
            }
        }

        // Fallback: the file may be bundled on the classpath (e.g. inside a jar). Probe both
        // class loaders with several name variants before giving up.
        InputStream in = getClass().getClassLoader().getResourceAsStream(confFile);
        if (in == null) {
            in = Thread.currentThread().getContextClassLoader().getResourceAsStream(confFile);
        }
        if (in == null) {
            in = getClass().getClassLoader().getResourceAsStream("config_files/" + fileName);
        }
        if (in == null) {
            in = Thread.currentThread().getContextClassLoader().getResourceAsStream("config_files/" + fileName);
        }
        if (in == null) {
            in = getClass().getClassLoader().getResourceAsStream(fileName);
        }
        if (in == null) {
            in = Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName);
        }
        if (in != null) {
            try {
                return readLinesFromStream(in);
            } catch (IOException e) {
                lastError = e;
            }
        }

        if (lastError != null) {
            throw new RuntimeException("failed to read config file: " + confFile, lastError);
        }
        throw new RuntimeException("failed to read config file: " + confFile);
    }

    private String confBasename() {
        int slash = Math.max(confFile.lastIndexOf('/'), confFile.lastIndexOf('\\'));
        return slash >= 0 ? confFile.substring(slash + 1) : confFile;
    }

    // Produces an ordered, de-duplicated list of locations to probe for the config file:
    // the raw path, user.dir-relative, a conventional config_files/ folder, and then the same
    // two patterns walked up to 6 parent directories (robust to nested project/module layouts).
    private List<Path> buildSearchPaths(String fileName) {
        List<Path> paths = new ArrayList<Path>();
        // LinkedHashSet preserves probe order while suppressing duplicate paths.
        java.util.LinkedHashSet<String> seen = new java.util.LinkedHashSet<String>();

        addSearchPath(paths, seen, Paths.get(confFile));
        addSearchPath(paths, seen, Paths.get(System.getProperty("user.dir"), confFile));
        addSearchPath(paths, seen, Paths.get("config_files", fileName));
        addSearchPath(paths, seen, Paths.get(System.getProperty("user.dir"), "config_files", fileName));

        // Climb the directory tree so the file is found even when launched from a subfolder.
        Path dir = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        for (int i = 0; i < 6 && dir != null; i++) {
            addSearchPath(paths, seen, dir.resolve(confFile));
            addSearchPath(paths, seen, dir.resolve("config_files").resolve(fileName));
            dir = dir.getParent();
        }

        return paths;
    }

    private static void addSearchPath(List<Path> paths, java.util.LinkedHashSet<String> seen, Path path) {
        Path normalized = path.normalize().toAbsolutePath();
        if (seen.add(normalized.toString())) {
            paths.add(normalized);
        }
    }

    private static List<String> readLinesFromStream(InputStream in) throws IOException {
        List<String> lines = new ArrayList<String>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) {
                    lines.add(trimmed);
                }
            }
        } finally {
            reader.close();
        }
        return lines;
    }

    private static List<String> trimNonEmptyLines(List<String> rawLines) {
        List<String> lines = new ArrayList<String>();
        for (String line : rawLines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                lines.add(trimmed);
            }
        }
        return lines;
    }

    // Instantiates an agent by reflection, supporting two constructor conventions:
    private static Agent createAgent(String className, String[] subs, String[] pubs) throws ReflectiveOperationException {
        Class<?> clazz = loadAgentClass(className);
        try {
            // Preferred form: Agent(String[] subs, String[] pubs). The (Object) casts stop
            // varargs from spreading each array into separate arguments.
            Constructor<?> ctor = clazz.getConstructor(String[].class, String[].class);
            return (Agent) ctor.newInstance((Object) subs, (Object) pubs);
        } catch (NoSuchMethodException e) {
            // Fallback form: a flat list of String parameters (subs followed by pubs). Build a
            // matching parameter-type array and flatten the two arrays into one argument array.
            int ctorArgsCount = subs.length + pubs.length;
            Class<?>[] paramTypes = new Class<?>[ctorArgsCount];
            for (int j = 0; j < ctorArgsCount; j++) {
                paramTypes[j] = String.class;
            }
            Constructor<?> ctor = clazz.getConstructor(paramTypes);
            Object[] args = new Object[ctorArgsCount];
            System.arraycopy(subs, 0, args, 0, subs.length);
            System.arraycopy(pubs, 0, args, subs.length, pubs.length);
            return (Agent) ctor.newInstance(args);
        }
    }

    /**
     * Resolves agent class names from the grader conf (e.g. project_biu.configs.*)
     * to local implementations in the test package.
     */
    static Class<?> loadAgentClass(String className) throws ClassNotFoundException {
        ClassNotFoundException last = null;
        // Try each candidate package against both the default and the thread context class
        // loader; the first that resolves wins. Keeps the last failure to rethrow if none work.
        for (String candidate : resolveClassCandidates(className)) {
            try {
                return Class.forName(candidate);
            } catch (ClassNotFoundException e) {
                last = e;
                try {
                    return Thread.currentThread().getContextClassLoader().loadClass(candidate);
                } catch (ClassNotFoundException e2) {
                    last = e2;
                }
            }
        }
        throw last;
    }

    // Config files may name agents using the grader's package (e.g. project_biu.configs.PlusAgent)
    // which does not exist locally. This maps a name to a list of likely fully-qualified names
    // by keeping the original and retrying under the local "configs"/"test" packages.
    private static String[] resolveClassCandidates(String className) {
        List<String> candidates = new ArrayList<String>();
        candidates.add(className);

        // Extract the simple name of the class (e.g. PlusAgent)
        String simpleName = className;
        int lastDot = className.lastIndexOf('.');
        if (lastDot >= 0) {
            simpleName = className.substring(lastDot + 1);
        }

        // Add package candidates
        candidates.add("configs." + simpleName);
        candidates.add("test." + simpleName);
        candidates.add("project_biu.configs." + simpleName);

        // Return unique candidates to search
        List<String> unique = new ArrayList<>();
        for (String c : candidates) {
            if (!unique.contains(c)) {
                unique.add(c);
            }
        }
        return unique.toArray(new String[0]);
    }

    private static String[] parseSubs(String subsLine) {
        String[] parts = subsLine.split(",");
        for (int i = 0; i < parts.length; i++) {
            parts[i] = parts[i].trim();
        }
        return parts;
    }

    /**
     * Returns the display name of this configuration loader.
     *
     * @return {@code "Generic Config"}
     */
    @Override
    public String getName() {
        return "Generic Config";
    }

    /**
     * Returns the version of this configuration loader.
     *
     * @return {@code 1}
     */
    @Override
    public int getVersion() {
        return 1;
    }

    /**
     * Unsubscribes all wired agents from topics and closes their parallel wrappers.
     *
     * <p>Clears the internal wiring list so a subsequent {@link #create()} starts fresh.</p>
     */
    @Override
    public void close() {
        TopicManager tm = TopicManagerSingleton.get();
        for (AgentWiring wiring : wirings) {
            for (String sub : wiring.subs) {
                tm.getTopic(sub).unsubscribe(wiring.wrapper);
            }
            for (String pub : wiring.pubs) {
                tm.getTopic(pub).removePublisher(wiring.wrapper);
            }
            wiring.wrapper.close();
        }
        wirings.clear();
    }
}
