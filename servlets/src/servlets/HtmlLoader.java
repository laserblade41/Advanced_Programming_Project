package servlets;

import server.RequestParser;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

// These are the servlets that handle the /app/ prefix endpoint.
public class HtmlLoader implements Servlet {

    private final String baseDir;

    public HtmlLoader(String baseDir) {
        this.baseDir = baseDir;
    }

    @Override
    public void handle(RequestParser.RequestInfo ri, OutputStream toClient) throws IOException {
        String[] segments = ri.getUriSegments();
        String relativePath = "";

        // Extract requested file from segments (skipping matched servlet prefix, e.g. "app")
        if (segments == null || segments.length <= 1) {
            relativePath = "index.html";
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i < segments.length; i++) {
                sb.append(segments[i]);
                if (i < segments.length - 1) {
                    sb.append(File.separator);
                }
            }
            relativePath = sb.toString();
        }

        // Try locating the file relative to the baseDir
        File file = new File(baseDir, relativePath);
        if (!file.exists()) {
            // Fallback: check relative to user.dir
            file = new File(System.getProperty("user.dir") + File.separator + baseDir, relativePath);
        }

        // Check if file doesn't exist or is a directory
        if (!file.exists() || file.isDirectory()) {
            String errorHtml = "<!DOCTYPE html>\n" +
                    "<html lang=\"en\">\n" +
                    "<head>\n" +
                    "    <meta charset=\"UTF-8\">\n" +
                    "    <title>404 Not Found</title>\n" +
                    "    <style>\n" +
                    "        body { background-color: #0b0f19; color: #f8fafc; font-family: sans-serif; text-align: center; padding: 50px; }\n" +
                    "        h1 { color: #ef4444; }\n" +
                    "        p { color: #94a3b8; }\n" +
                    "    </style>\n" +
                    "</head>\n" +
                    "<body>\n" +
                    "    <h1>404 Not Found</h1>\n" +
                    "    <p>The requested resource was not found on this server.</p>\n" +
                    "</body>\n" +
                    "</html>";

            byte[] bodyBytes = errorHtml.getBytes(StandardCharsets.UTF_8);
            String responseHeaders = "HTTP/1.1 404 Not Found\r\n" +
                    "Content-Type: text/html; charset=UTF-8\r\n" +
                    "Content-Length: " + bodyBytes.length + "\r\n" +
                    "Connection: close\r\n" +
                    "\r\n";

            toClient.write(responseHeaders.getBytes(StandardCharsets.UTF_8));
            toClient.write(bodyBytes);
            toClient.flush();
            return;
        }

        // Load the file content
        byte[] fileBytes = Files.readAllBytes(file.toPath());

        // Determine correct Content-Type header
        String contentType = "text/html";
        String lowerName = file.getName().toLowerCase();
        if (lowerName.endsWith(".css")) {
            contentType = "text/css";
        } else if (lowerName.endsWith(".js")) {
            contentType = "text/javascript";
        } else if (lowerName.endsWith(".png")) {
            contentType = "image/png";
        } else if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) {
            contentType = "image/jpeg";
        } else if (lowerName.endsWith(".gif")) {
            contentType = "image/gif";
        } else if (lowerName.endsWith(".txt")) {
            contentType = "text/plain";
        }

        String responseHeaders = "HTTP/1.1 200 OK\r\n" +
                "Content-Type: " + contentType + "\r\n" +
                "Content-Length: " + fileBytes.length + "\r\n" +
                "Connection: close\r\n" +
                "\r\n";

        toClient.write(responseHeaders.getBytes(StandardCharsets.UTF_8));
        toClient.write(fileBytes);
        toClient.flush();
    }

    @Override
    public void close() throws IOException {
        // Clean up resources if needed
    }
}
