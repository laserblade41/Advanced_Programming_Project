package servlets;

import server.RequestParser;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Servlet that serves static files from a directory under a registered URI prefix.
 *
 * <p>Typically registered as {@code server.addServlet("GET", "/app/", new HtmlLoader("html_files"))}.
 * A request to {@code GET /app/index.html} resolves to {@code html_files/index.html} relative
 * to the configured base directory.</p>
 *
 * <p><strong>Path resolution:</strong> The first URI segment (the servlet prefix, e.g.
 * {@code "app"}) is skipped; remaining segments form the relative file path. If no sub-path
 * is present, {@code index.html} is served by default. Files are first looked up relative
 * to {@code baseDir}, then relative to {@code user.dir/baseDir} as a fallback.</p>
 *
 * <p><strong>Content types:</strong> Determined by file extension — HTML, CSS, JavaScript,
 * PNG, JPEG, GIF, and plain text are supported. Missing files or directories yield a
 * {@code 404 Not Found} HTML error page.</p>
 *
 * <p><strong>Thread safety:</strong> This servlet is stateless after construction (the
 * {@code baseDir} field is final) and safe for concurrent reads of the filesystem.</p>
 *
 * @see Servlet
 * @see server.HTTPServer#addServlet
 */
public class HtmlLoader implements Servlet {

    private final String baseDir;

    /**
     * Creates a static-file servlet rooted at the given directory.
     *
     * @param baseDir the root directory from which static assets are served
     */
    public HtmlLoader(String baseDir) {
        this.baseDir = baseDir;
    }

    /**
     * Resolves the requested file from URI segments and writes an HTTP response.
     *
     * <p>Returns {@code 200 OK} with the appropriate {@code Content-Type} when the file
     * exists, or {@code 404 Not Found} with a styled HTML error page otherwise.</p>
     *
     * @param ri the parsed request; {@link RequestParser.RequestInfo#getUriSegments()} is
     *           used to determine the relative file path
     * @param toClient the client output stream for the HTTP response
     * @throws IOException if an I/O error occurs while reading the file or writing the response
     */
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

        // Map the file extension to a MIME type so browsers render assets correctly
        // (defaults to text/html for unknown/extension-less files).
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
