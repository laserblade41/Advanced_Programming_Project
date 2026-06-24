package servlets;

import server.RequestParser;
import graph.TopicManagerSingleton;
import graph.Topic;
import graph.Message;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Application servlet that handles the {@code /publish} endpoint for topic message publication.
 *
 * <p>This servlet is part of the computational-graph dashboard. It extracts a topic name and
 * message value from the request, optionally publishes them via
 * {@link graph.TopicManagerSingleton}, and always returns an HTML page listing all topics
 * and their last published values.</p>
 *
 * <p><strong>Parameter extraction:</strong> Topic and message are read from query parameters
 * ({@code topic}/{@code Topic} and {@code message}/{@code Message}). If absent, URI segments
 * are used as a fallback (e.g. {@code /publish/myTopic/myValue} or
 * {@code /myTopic/myValue}).</p>
 *
 * <p>When a message is successfully published, embedded JavaScript refreshes the parent
 * dashboard's graph frame (dashboard-specific behavior).</p>
 *
 * <p><strong>Thread safety:</strong> Relies on {@link graph.TopicManagerSingleton} for
 * concurrent topic access; the servlet itself holds no mutable instance state.</p>
 *
 * @see Servlet
 * @see graph.TopicManagerSingleton
 */
public class TopicDisplayer implements Servlet {

    /**
     * Publishes a message to a topic (if parameters are present) and returns an HTML table
     * of all topic values.
     *
     * @param ri the parsed request containing topic and message in parameters or URI segments
     * @param toClient the client output stream for the HTTP response
     * @throws IOException if an I/O error occurs while writing the response
     */
    @Override
    public void handle(RequestParser.RequestInfo ri, OutputStream toClient) throws IOException {
        // Extract the topic and message from the HTTP request.
        // Try getting them from query parameters (case-insensitive variants check)
        Map<String, String> params = ri.getParameters();
        String topic = null;
        String message = null;

        if (params != null) {
            topic = params.get("topic");
            if (topic == null) {
                topic = params.get("Topic");
            }
            message = params.get("message");
            if (message == null) {
                message = params.get("Message");
            }
        }

        // Fallback to URI segments if parameters are not present in query string
        if (topic == null || message == null) {
            String[] uriSegments = ri.getUriSegments();
            if (uriSegments != null) {
                if (uriSegments.length >= 3 && "publish".equals(uriSegments[0])) {
                    topic = uriSegments[1];
                    message = uriSegments[2];
                } else if (uriSegments.length >= 2) {
                    topic = uriSegments[0];
                    message = uriSegments[1];
                }
            }
        }

        // Only publish when both a topic and a message were resolved. This is the entry point
        // that injects user input into the pub-sub graph, triggering any subscribed agents.
        boolean published = false;
        if (topic != null && message != null && !topic.trim().isEmpty() && !message.trim().isEmpty()) {
            TopicManagerSingleton.get().getTopic(topic.trim()).publish(new Message(message.trim()));
            published = true;
        }

        // Build the HTML response: one table row per topic showing its last published value.
        StringBuilder rowsHtml = new StringBuilder();
        Map<String, Topic> topics = TopicManagerSingleton.get().getAllTopics();

        if (topics.isEmpty()) {
            rowsHtml.append("<tr><td colspan='2' class='no-topics'>No topics registered yet</td></tr>");
        } else {
            // Sort names so the table order is stable/deterministic between refreshes.
            List<String> sortedNames = new ArrayList<>(topics.keySet());
            Collections.sort(sortedNames);
            for (String name : sortedNames) {
                Topic t = topics.get(name);
                Message lastMsg = t.getLastMessage();
                String valStr = (lastMsg != null) ? lastMsg.asText : "N/A";
                rowsHtml.append("<tr>")
                        .append("<td class='topic-name'>").append(escapeHtml(name)).append("</td>")
                        .append("<td class='topic-value'>").append(escapeHtml(valStr)).append("</td>")
                        .append("</tr>");
            }
        }

        String htmlContent = "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>Topic Values</title>\n" +
                "    <link href=\"https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600&display=swap\" rel=\"stylesheet\">\n" +
                "    <style>\n" +
                "        :root {\n" +
                "            --bg-color: #0b0f19;\n" +
                "            --card-bg: rgba(30, 41, 59, 0.4);\n" +
                "            --card-border: rgba(255, 255, 255, 0.05);\n" +
                "            --accent-blue: #3b82f6;\n" +
                "            --accent-green: #10b981;\n" +
                "            --text-primary: #f1f5f9;\n" +
                "            --text-secondary: #94a3b8;\n" +
                "        }\n" +
                "\n" +
                "        body {\n" +
                "            margin: 0;\n" +
                "            padding: 1.5rem;\n" +
                "            background-color: var(--bg-color);\n" +
                "            color: var(--text-primary);\n" +
                "            font-family: 'Inter', sans-serif;\n" +
                "            box-sizing: border-box;\n" +
                "        }\n" +
                "\n" +
                "        .table-container {\n" +
                "            background: var(--card-bg);\n" +
                "            border: 1px solid var(--card-border);\n" +
                "            border-radius: 12px;\n" +
                "            padding: 1.5rem;\n" +
                "            backdrop-filter: blur(8px);\n" +
                "            box-shadow: 0 4px 20px -2px rgba(0, 0, 0, 0.3);\n" +
                "            margin: 0 auto;\n" +
                "        }\n" +
                "\n" +
                "        h2 {\n" +
                "            margin-top: 0;\n" +
                "            margin-bottom: 1.25rem;\n" +
                "            font-size: 1.2rem;\n" +
                "            font-weight: 600;\n" +
                "            color: var(--accent-green);\n" +
                "            display: flex;\n" +
                "            align-items: center;\n" +
                "            gap: 0.5rem;\n" +
                "        }\n" +
                "\n" +
                "        table {\n" +
                "            width: 100%;\n" +
                "            border-collapse: collapse;\n" +
                "            text-align: left;\n" +
                "        }\n" +
                "\n" +
                "        th, td {\n" +
                "            padding: 0.75rem 1rem;\n" +
                "            border-bottom: 1px solid rgba(255, 255, 255, 0.05);\n" +
                "        }\n" +
                "\n" +
                "        th {\n" +
                "            color: var(--text-secondary);\n" +
                "            font-size: 0.8rem;\n" +
                "            font-weight: 600;\n" +
                "            text-transform: uppercase;\n" +
                "            letter-spacing: 0.05em;\n" +
                "            background: rgba(15, 23, 42, 0.4);\n" +
                "        }\n" +
                "\n" +
                "        tr:last-child td {\n" +
                "            border-bottom: none;\n" +
                "        }\n" +
                "\n" +
                "        tr:hover td {\n" +
                "            background: rgba(255, 255, 255, 0.02);\n" +
                "        }\n" +
                "\n" +
                "        .topic-name {\n" +
                "            font-weight: 500;\n" +
                "            color: var(--text-primary);\n" +
                "        }\n" +
                "\n" +
                "        .topic-value {\n" +
                "            font-family: monospace;\n" +
                "            font-weight: 600;\n" +
                "            color: var(--accent-blue);\n" +
                "        }\n" +
                "\n" +
                "        .no-topics {\n" +
                "            text-align: center;\n" +
                "            color: var(--text-secondary);\n" +
                "            font-style: italic;\n" +
                "            padding: 2rem;\n" +
                "        }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div class=\"table-container\">\n" +
                "        <h2>📊 Topic Values</h2>\n" +
                "        <table>\n" +
                "            <thead>\n" +
                "                <tr>\n" +
                "                    <th>Topic Name</th>\n" +
                "                    <th>Last Value</th>\n" +
                "                </tr>\n" +
                "            </thead>\n" +
                "            <tbody>\n" +
                rowsHtml.toString() +
                "            </tbody>\n" +
                "        </table>\n" +
                "    </div>\n" +
                "    <script>\n" +
                "        const isPublishAction = " + published + ";\n" +
                "        if (isPublishAction && window.parent && window.parent.frames['graph-center']) {\n" +
                "            window.parent.frames['graph-center'].location.href = 'http://localhost:8080/upload';\n" +
                "        }\n" +
                "    </script>\n" +
                "</body>\n" +
                "</html>";

        byte[] bodyBytes = htmlContent.getBytes("UTF-8");
        String responseHeaders = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: text/html; charset=UTF-8\r\n" +
                "Content-Length: " + bodyBytes.length + "\r\n" +
                "Connection: close\r\n" +
                "\r\n";

        toClient.write(responseHeaders.getBytes("UTF-8"));
        toClient.write(bodyBytes);
        toClient.flush();
    }

    // Escapes HTML metacharacters so topic names/values can't break the page layout or inject
    // markup (basic XSS guard) when echoed back into the table.
    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }

    /**
     * Releases servlet resources.
     *
     * <p>Currently a no-op; no persistent resources are held between requests.</p>
     *
     * @throws IOException if an I/O error occurs during cleanup
     */
    @Override
    public void close() throws IOException {
        // Clean up resources if needed
    }
}
