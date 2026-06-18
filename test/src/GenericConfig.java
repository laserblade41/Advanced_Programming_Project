package test;

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

import test.TopicManagerSingleton.TopicManager;

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

    public void setConfFile(String path) {
        this.confFile = path;
    }

    @Override
    public void create() {
        if (confFile == null || confFile.isEmpty()) {
            throw new IllegalStateException("conf file not set");
        }

        if (!wirings.isEmpty()) {
            close();
        }

        List<String> lines = readConfigLines();

        if (lines.size() % 3 != 0) {
            throw new IllegalStateException("invalid config file format");
        }

        TopicManager tm = TopicManagerSingleton.get();

        for (int i = 0; i < lines.size(); i += 3) {
            String className = lines.get(i);
            String[] subs = parseSubs(lines.get(i + 1));
            String[] pubs = parseSubs(lines.get(i + 2));

            try {
                Agent agent = createAgent(className, subs, pubs);

                ParallelAgent wrapper = new ParallelAgent(agent, 10);
                for (String sub : subs) {
                    tm.getTopic(sub).subscribe(wrapper);
                }
                for (String pub : pubs) {
                    tm.getTopic(pub).addPublisher(wrapper);
                }

                wirings.add(new AgentWiring(wrapper, subs, pubs));
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("failed to create agent: " + className, e);
            }
        }
    }

    private List<String> readConfigLines() {
        String fileName = confBasename();
        IOException lastError = null;

        for (Path path : buildSearchPaths(fileName)) {
            try {
                if (Files.exists(path)) {
                    return trimNonEmptyLines(Files.readAllLines(path));
                }
            } catch (IOException e) {
                lastError = e;
            }
        }

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

    private List<Path> buildSearchPaths(String fileName) {
        List<Path> paths = new ArrayList<Path>();
        java.util.LinkedHashSet<String> seen = new java.util.LinkedHashSet<String>();

        addSearchPath(paths, seen, Paths.get(confFile));
        addSearchPath(paths, seen, Paths.get(System.getProperty("user.dir"), confFile));
        addSearchPath(paths, seen, Paths.get("config_files", fileName));
        addSearchPath(paths, seen, Paths.get(System.getProperty("user.dir"), "config_files", fileName));

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

    private static Agent createAgent(String className, String[] subs, String[] pubs) throws ReflectiveOperationException {
        Class<?> clazz = loadAgentClass(className);
        try {
            Constructor<?> ctor = clazz.getConstructor(String[].class, String[].class);
            return (Agent) ctor.newInstance((Object) subs, (Object) pubs);
        } catch (NoSuchMethodException e) {
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

    private static String[] resolveClassCandidates(String className) {
        List<String> candidates = new ArrayList<String>();
        candidates.add(className);
        if (className.startsWith("project_biu.configs.")) {
            candidates.add("test." + className.substring("project_biu.configs.".length()));
        }
        if (className.indexOf('.') < 0) {
            candidates.add("test." + className);
        }
        return candidates.toArray(new String[candidates.size()]);
    }

    private static String[] parseSubs(String subsLine) {
        String[] parts = subsLine.split(",");
        for (int i = 0; i < parts.length; i++) {
            parts[i] = parts[i].trim();
        }
        return parts;
    }

    @Override
    public String getName() {
        return "Generic Config";
    }

    @Override
    public int getVersion() {
        return 1;
    }

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
