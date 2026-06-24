package views;

import configs.Graph;
import configs.Node;
import graph.TopicManagerSingleton;
import graph.Topic;
import graph.Message;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * View-layer utility that generates HTML graph visualization pages.
 *
 * <p>Reads the {@code html_files/graph.html} Vis.js template, injects dynamic node and
 * edge data from a {@link configs.Graph}, and appends a script to refresh the dashboard's
 * topic-values frame. Used by {@link servlets.ConfLoader} to render the computational
 * graph in the browser.</p>
 *
 * <p>Topic nodes (names starting with {@code "T"}) display their last published value
 * in the label when available via {@link graph.TopicManagerSingleton}.</p>
 *
 * @see configs.Graph
 * @see configs.Node
 * @see servlets.ConfLoader
 */
public class HtmlGraphWriter {

    /**
     * Builds a complete HTML graph visualization as a list of lines.
     *
     * <p>Loads the graph template from {@code html_files/graph.html} (with filesystem
     * fallbacks), replaces the {@code /* INJECT_GRAPH_DATA_HERE *\/} placeholder with
     * JavaScript node/edge arrays derived from {@code g}, and appends a parent-frame
     * reload script for the values panel.</p>
     *
     * @param g the graph model to visualize
     * @param isConfigUpload {@code true} when triggered by a configuration upload;
     *                       reserved for future use in the template logic
     * @return a {@link List} of HTML lines, one entry per line of the output document
     */
    public static List<String> getGraphHTML(Graph g, boolean isConfigUpload) {
        // Find and read html_files/graph.html template
        Path path = Paths.get("html_files", "graph.html");
        if (!Files.exists(path)) {
            path = Paths.get(System.getProperty("user.dir"), "html_files", "graph.html");
        }

        String template = "";
        try {
            if (Files.exists(path)) {
                template = Files.readString(path, StandardCharsets.UTF_8);
            } else {
                // Fallback basic template if file is not found
                template = getDefaultFallbackTemplate();
            }
        } catch (IOException e) {
            template = getDefaultFallbackTemplate();
        }

        // Dynamically build JS representation for nodes and edges
        StringBuilder nodesJs = new StringBuilder();
        nodesJs.append("[\n");
        for (int i = 0; i < g.size(); i++) {
            Node n = g.get(i);
            String id = n.getName();
            boolean isTopic = id.startsWith("T");
            String label = id;
            String type = "agent";
            
            if (isTopic) {
                String topicName = id.substring(1);
                type = "topic";
                Topic t = TopicManagerSingleton.get().getTopic(topicName);
                Message lastMsg = (t != null) ? t.getLastMessage() : null;
                if (lastMsg != null) {
                    label = topicName + "\n[ " + lastMsg.asText + " ]";
                } else {
                    label = topicName;
                }
            }
            
            nodesJs.append("            { id: '").append(escapeJs(id))
                   .append("', label: '").append(escapeJs(label))
                   .append("', type: '").append(type).append("' }");
            if (i < g.size() - 1) {
                nodesJs.append(",\n");
            }
        }
        nodesJs.append("\n        ]");

        StringBuilder edgesJs = new StringBuilder();
        edgesJs.append("[\n");
        List<String> edgeLines = new ArrayList<>();
        for (Node n : g) {
            for (Node target : n.getEdges()) {
                edgeLines.add("            { from: '" + escapeJs(n.getName()) + "', to: '" + escapeJs(target.getName()) + "' }");
            }
        }
        for (int i = 0; i < edgeLines.size(); i++) {
            edgesJs.append(edgeLines.get(i));
            if (i < edgeLines.size() - 1) {
                edgesJs.append(",\n");
            }
        }
        edgesJs.append("\n        ]");

        // Replace the placeholder with dynamic nodes and edges data
        String result = template;
        String graphDataJs = "const nodesData = " + nodesJs.toString() + ";\n" +
                             "const edgesData = " + edgesJs.toString() + ";";
        result = result.replace("/* INJECT_GRAPH_DATA_HERE */", graphDataJs);

        // Always append parent's right-frame reload script to ensure any background/asynchronous 
        // agent calculations (e.g. PlusAgent) are correctly reflected in the topic values table.
        int lastIndex = result.lastIndexOf("</script>");
        if (lastIndex >= 0) {
            String reloadScript = "\n        if (window.parent && window.parent.frames['values-right']) {\n" +
                           "            window.parent.frames['values-right'].location.href = 'http://localhost:8080/publish';\n" +
                           "        }\n";
            result = result.substring(0, lastIndex) + reloadScript + result.substring(lastIndex);
        }

        // Split by lines and return
        String[] lines = result.split("\\r?\\n");
        List<String> list = new ArrayList<>();
        for (String line : lines) {
            list.add(line);
        }
        return list;
    }

    private static String escapeJs(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private static String getDefaultFallbackTemplate() {
        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "    <title>Visualization Fallback</title>\n" +
                "    <script type=\"text/javascript\" src=\"https://unpkg.com/vis-network/standalone/umd/vis-network.min.js\"></script>\n" +
                "</head>\n" +
                "<body style=\"background-color:#0b0f19; color:white;\">\n" +
                "    <div id=\"network-container\" style=\"width:100%; height:100vh;\"></div>\n" +
                "    <script>\n" +
                "        const nodesData = [];\n" +
                "        const edgesData = [];\n" +
                "        // visualization code placeholder\n" +
                "    </script>\n" +
                "</body>\n" +
                "</html>";
    }
}
