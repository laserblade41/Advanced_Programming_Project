package views;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import configs.Graph;
import configs.Node;
import graph.Message;

public class HtmlGraphWriter {

    private static final String TEMPLATE_PATH = "html_files/graph.html";
    private static final String PLACEHOLDER = "/* INJECT_GRAPH_DATA_HERE */";

    public static List<String> getGraphHTML(Graph graph) {
        List<String> templateLines = loadTemplateLines();
        String graphDataJs = buildGraphDataJs(graph);
        return injectAndReturn(templateLines, graphDataJs);
    }

    private static List<String> loadTemplateLines() {
        String fileName = templateBasename();
        IOException lastError = null;

        for (Path path : buildSearchPaths(fileName)) {
            try {
                if (Files.exists(path)) {
                    return Files.readAllLines(path, StandardCharsets.UTF_8);
                }
            } catch (IOException e) {
                lastError = e;
            }
        }

        InputStream in = HtmlGraphWriter.class.getClassLoader().getResourceAsStream(TEMPLATE_PATH);
        if (in == null) {
            in = Thread.currentThread().getContextClassLoader().getResourceAsStream(TEMPLATE_PATH);
        }
        if (in == null) {
            in = HtmlGraphWriter.class.getClassLoader().getResourceAsStream(fileName);
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
            throw new RuntimeException("failed to read HTML template: " + TEMPLATE_PATH, lastError);
        }
        throw new RuntimeException("failed to read HTML template: " + TEMPLATE_PATH);
    }

    private static String templateBasename() {
        int slash = Math.max(TEMPLATE_PATH.lastIndexOf('/'), TEMPLATE_PATH.lastIndexOf('\\'));
        return slash >= 0 ? TEMPLATE_PATH.substring(slash + 1) : TEMPLATE_PATH;
    }

    private static List<Path> buildSearchPaths(String fileName) {
        List<Path> paths = new ArrayList<Path>();
        LinkedHashSet<String> seen = new LinkedHashSet<String>();

        addSearchPath(paths, seen, Paths.get(TEMPLATE_PATH));
        addSearchPath(paths, seen, Paths.get(System.getProperty("user.dir"), TEMPLATE_PATH));
        addSearchPath(paths, seen, Paths.get("html_files", fileName));
        addSearchPath(paths, seen, Paths.get(System.getProperty("user.dir"), "html_files", fileName));

        Path dir = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        for (int i = 0; i < 6 && dir != null; i++) {
            addSearchPath(paths, seen, dir.resolve(TEMPLATE_PATH));
            addSearchPath(paths, seen, dir.resolve("html_files").resolve(fileName));
            dir = dir.getParent();
        }

        return paths;
    }

    private static void addSearchPath(List<Path> paths, LinkedHashSet<String> seen, Path path) {
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
                lines.add(line);
            }
        } finally {
            reader.close();
        }
        return lines;
    }

    private static String buildGraphDataJs(Graph graph) {
        StringBuilder sb = new StringBuilder();
        sb.append("const nodesData = [\n");
        boolean firstNode = true;
        for (Node node : graph) {
            if (!firstNode) {
                sb.append(",\n");
            }
            firstNode = false;
            appendNodeEntry(sb, node);
        }
        sb.append("\n];\n\nconst edgesData = [\n");
        boolean firstEdge = true;
        for (Node from : graph) {
            for (Node to : from.getEdges()) {
                if (!firstEdge) {
                    sb.append(",\n");
                }
                firstEdge = false;
                sb.append("    { from: '").append(escapeJs(from.getName()))
                  .append("', to: '").append(escapeJs(to.getName())).append("' }");
            }
        }
        sb.append("\n];");
        return sb.toString();
    }

    private static void appendNodeEntry(StringBuilder sb, Node node) {
        String name = node.getName();
        boolean isTopic = name.startsWith("T");
        String type = isTopic ? "topic" : "agent";
        String label = buildLabel(node, isTopic);

        sb.append("    { id: '").append(escapeJs(name))
          .append("', label: '").append(escapeJs(label))
          .append("', type: '").append(type).append("' }");
    }

    private static String buildLabel(Node node, boolean isTopic) {
        String name = node.getName();
        if (!isTopic) {
            return name;
        }
        String topicName = name.substring(1);
        Message msg = node.getMessage();
        if (msg != null) {
            return topicName + "\n[ " + formatValue(msg.asDouble) + " ]";
        }
        return topicName + "\n[ ]";
    }

    private static String formatValue(double value) {
        if (value == (long) value) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    private static String escapeJs(String s) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\':
                    out.append("\\\\");
                    break;
                case '\'':
                    out.append("\\'");
                    break;
                case '\n':
                    out.append("\\n");
                    break;
                case '\r':
                    out.append("\\r");
                    break;
                default:
                    out.append(c);
                    break;
            }
        }
        return out.toString();
    }

    private static List<String> injectAndReturn(List<String> templateLines, String graphDataJs) {
        StringBuilder html = new StringBuilder();
        boolean found = false;
        for (String line : templateLines) {
            if (line.contains(PLACEHOLDER)) {
                found = true;
                html.append(line.replace(PLACEHOLDER, graphDataJs));
            } else {
                html.append(line);
            }
            html.append('\n');
        }
        if (!found) {
            throw new IllegalStateException("placeholder not found in template: " + PLACEHOLDER);
        }
        String result = html.toString();
        if (result.endsWith("\n")) {
            result = result.substring(0, result.length() - 1);
        }
        List<String> lines = new ArrayList<String>();
        int start = 0;
        for (int i = 0; i < result.length(); i++) {
            if (result.charAt(i) == '\n') {
                lines.add(result.substring(start, i));
                start = i + 1;
            }
        }
        if (start < result.length()) {
            lines.add(result.substring(start));
        }
        return lines;
    }
}
