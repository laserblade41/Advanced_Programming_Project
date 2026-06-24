package servlets;

import server.RequestParser;
import configs.GenericConfig;
import configs.Graph;
import views.HtmlGraphWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// These are the servlets that handle the /upload endpoint.
public class ConfLoader implements Servlet {

    @Override
    public void handle(RequestParser.RequestInfo ri, OutputStream toClient) throws IOException {
        String filename = null;
        String fileContent = null;

        try {
            byte[] body = ri.getContent();
            boolean isMultipart = false;

            if (body != null && body.length > 0) {
                // Check if it's a multipart form upload by reading the first characters
                String bodyPrefix = new String(body, 0, Math.min(body.length, 10), StandardCharsets.UTF_8);
                if (bodyPrefix.startsWith("--")) {
                    isMultipart = true;
                    MultipartData md = parseMultipart(body);
                    if (md != null) {
                        filename = md.filename;
                        fileContent = md.fileContent;
                    }
                }
            }

            // Fallback for non-multipart requests (such as automatic testing tools/dummy tests)
            if (!isMultipart) {
                Map<String, String> params = ri.getParameters();
                if (params != null) {
                    filename = params.get("filename");
                    if (filename == null) {
                        filename = params.get("Filename");
                    }
                }
                if (body != null) {
                    fileContent = new String(body, StandardCharsets.UTF_8).trim();
                }
            }

            // Default filename if not extracted
            if (filename == null || filename.trim().isEmpty()) {
                filename = "uploaded_config.conf";
            } else {
                filename = filename.trim();
            }

            if (fileContent == null || fileContent.trim().isEmpty()) {
                throw new IllegalArgumentException("Configuration file content is empty.");
            }

            // Save the uploaded file on the server side in the config_files/ directory
            File dir = new File("config_files");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File file = new File(dir, filename);
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(fileContent);
            }

            // Load the GenericConfig
            GenericConfig config = new GenericConfig();
            config.setConfFile(file.getPath());
            config.create();

            // Create a Graph from the active topics
            Graph g = new Graph();
            g.createFromTopics();

            // Obtain the HTML representation by delegating to views.HtmlGraphWriter
            List<String> htmlLines = HtmlGraphWriter.getGraphHTML(g);
            StringBuilder htmlContent = new StringBuilder();
            for (String line : htmlLines) {
                htmlContent.append(line).append("\n");
            }

            byte[] bodyBytes = htmlContent.toString().getBytes(StandardCharsets.UTF_8);
            String responseHeaders = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: text/html; charset=UTF-8\r\n" +
                    "Content-Length: " + bodyBytes.length + "\r\n" +
                    "Connection: close\r\n" +
                    "\r\n";

            toClient.write(responseHeaders.getBytes(StandardCharsets.UTF_8));
            toClient.write(bodyBytes);
            toClient.flush();

        } catch (Exception e) {
            e.printStackTrace();
            String errorMsg = "Error loading configuration: " + e.getMessage();
            byte[] errBytes = errorMsg.getBytes(StandardCharsets.UTF_8);
            String errHeaders = "HTTP/1.1 400 Bad Request\r\n" +
                    "Content-Type: text/plain; charset=UTF-8\r\n" +
                    "Content-Length: " + errBytes.length + "\r\n" +
                    "Connection: close\r\n" +
                    "\r\n";

            toClient.write(errHeaders.getBytes(StandardCharsets.UTF_8));
            toClient.write(errBytes);
            toClient.flush();
        }
    }

    private static class MultipartData {
        String filename;
        String fileContent;
    }

    private MultipartData parseMultipart(byte[] body) {
        if (body == null || body.length == 0) return null;
        try {
            // Find boundary from the first line of the body
            int endOfLine = -1;
            for (int i = 0; i < body.length; i++) {
                if (body[i] == '\n') {
                    endOfLine = i;
                    break;
                }
            }
            if (endOfLine == -1) return null;

            String firstLine = new String(body, 0, endOfLine, StandardCharsets.UTF_8).trim();
            if (!firstLine.startsWith("--")) return null;

            String boundary = firstLine;
            byte[] boundaryBytes = boundary.getBytes(StandardCharsets.UTF_8);

            List<byte[]> parts = splitBytes(body, boundaryBytes);
            MultipartData md = new MultipartData();

            for (byte[] part : parts) {
                if (part.length == 0) continue;

                int doubleNewlineIndex = findDoubleNewline(part);
                if (doubleNewlineIndex == -1) continue;

                int headersLen = doubleNewlineIndex;
                int contentStart = doubleNewlineIndex + 4; // assuming \r\n\r\n
                if (part[doubleNewlineIndex] == '\n' && part[doubleNewlineIndex + 1] == '\n') {
                    contentStart = doubleNewlineIndex + 2;
                } else if (part[doubleNewlineIndex + 1] == '\n' && part[doubleNewlineIndex + 2] == '\n') {
                    contentStart = doubleNewlineIndex + 3; // \r\n\n
                }

                String headers = new String(part, 0, headersLen, StandardCharsets.UTF_8);
                int contentLen = part.length - contentStart;

                // Strip trailing boundary/form-data formatting newlines
                if (contentLen >= 2 && part[part.length - 2] == '\r' && part[part.length - 1] == '\n') {
                    contentLen -= 2;
                } else if (contentLen >= 1 && part[part.length - 1] == '\n') {
                    contentLen -= 1;
                }
                if (contentLen < 0) contentLen = 0;

                byte[] contentBytes = new byte[contentLen];
                System.arraycopy(part, contentStart, contentBytes, 0, contentLen);
                String contentStr = new String(contentBytes, StandardCharsets.UTF_8).trim();

                if (headers.contains("name=\"filename\"") || headers.contains("name=\"Filename\"")) {
                    md.filename = contentStr;
                } else if (headers.contains("name=\"file\"")) {
                    if (md.filename == null || md.filename.isEmpty()) {
                        java.util.regex.Matcher m = java.util.regex.Pattern.compile("filename=\"([^\"]+)\"").matcher(headers);
                        if (m.find()) {
                            md.filename = m.group(1);
                        }
                    }
                    md.fileContent = new String(contentBytes, StandardCharsets.UTF_8);
                }
            }
            return md;
        } catch (Exception e) {
            return null;
        }
    }

    private int findDoubleNewline(byte[] arr) {
        for (int i = 0; i < arr.length - 3; i++) {
            if (arr[i] == '\r' && arr[i + 1] == '\n' && arr[i + 2] == '\r' && arr[i + 3] == '\n') {
                return i;
            }
        }
        for (int i = 0; i < arr.length - 1; i++) {
            if (arr[i] == '\n' && arr[i + 1] == '\n') {
                return i;
            }
        }
        return -1;
    }

    private List<byte[]> splitBytes(byte[] data, byte[] boundary) {
        List<byte[]> parts = new ArrayList<>();
        int searchLen = data.length - boundary.length + 1;
        int lastIndex = 0;

        for (int i = 0; i < searchLen; i++) {
            boolean match = true;
            for (int j = 0; j < boundary.length; j++) {
                if (data[i + j] != boundary[j]) {
                    match = false;
                    break;
                }
            }
            if (match) {
                if (i > lastIndex) {
                    int start = lastIndex;
                    while (start < i && (data[start] == '\r' || data[start] == '\n' || data[start] == '-')) {
                        start++;
                    }
                    int len = i - start;
                    if (len > 0) {
                        byte[] part = new byte[len];
                        System.arraycopy(data, start, part, 0, len);
                        parts.add(part);
                    }
                }
                lastIndex = i + boundary.length;
                i = lastIndex - 1;
            }
        }
        return parts;
    }

    @Override
    public void close() throws IOException {
        // Clean up resources if needed
    }
}
