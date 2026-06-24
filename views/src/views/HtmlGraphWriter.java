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

public class HtmlGraphWriter {

    public static List<String> getGraphHTML(Graph g) {
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
                    label = topicName + "\\n[ " + lastMsg.asText + " ]";
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

        // Regex replace dummy data arrays with the dynamic ones
        // In graph.html, we have nodesData and edgesData arrays
        String result = template;
        result = result.replaceAll("const nodesData\\s*=\\s*\\[[\\s\\S]*?\\]\\s*;", java.util.regex.Matcher.quoteReplacement("const nodesData = " + nodesJs.toString() + ";"));
        result = result.replaceAll("const edgesData\\s*=\\s*\\[[\\s\\S]*?\\]\\s*;", java.util.regex.Matcher.quoteReplacement("const edgesData = " + edgesJs.toString() + ";"));

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
