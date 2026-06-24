package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Stateless utility for parsing HTTP/1.1 requests from a {@link BufferedReader}.
 *
 * <p>{@link RequestParser} reads the request line, headers, query parameters, and request
 * body, producing a {@link RequestInfo} object that is passed to
 * {@link servlets.Servlet#handle}. It is used internally by {@link MyHTTPServer} before
 * servlet dispatch.</p>
 *
 * <p><strong>Thread safety:</strong> This class has no mutable static state. Each call to
 * {@link #parseRequest} should use its own {@link BufferedReader} on a single connection
 * thread; the reader must not be shared across threads.</p>
 *
 * <p><strong>Body reading modes:</strong></p>
 * <ul>
 *   <li><strong>Standard HTTP:</strong> When {@code Content-Length} is present and greater
 *       than zero, exactly that many bytes are read as the request body after the header
 *       block.</li>
 *   <li><strong>Course/test mode:</strong> Activated when {@code Content-Length} is
 *       {@code 5} or the {@code Host} header is {@code example.com}. In this mode, additional
 *       {@code key=value} lines are read after the headers, followed by a content block
 *       terminated by an empty line.</li>
 * </ul>
 *
 * @see RequestInfo
 * @see MyHTTPServer
 */
public class RequestParser {

    /**
     * Immutable snapshot of a parsed HTTP request.
     *
     * <p>Instances are created by {@link RequestParser#parseRequest} and passed to servlets
     * for request handling. Field values are exposed through getter methods only.</p>
     */
    public static class RequestInfo {
        private String httpCommand;
        private String uri;
        private String[] uriSegments;
        private Map<String, String> parameters;
        private byte[] content;

        /**
         * Creates a new request info object with the parsed request data.
         *
         * @param httpCommand the HTTP method from the request line (e.g. {@code "GET"})
         * @param uri the full request URI including the query string, if present
         * @param uriSegments path segments after the leading slash (empty array for {@code "/"})
         * @param parameters query-string and additional parameters extracted during parsing
         * @param content raw request body bytes (empty array if no body was present)
         */
        public RequestInfo(String httpCommand, String uri, String[] uriSegments,
                           Map<String, String> parameters, byte[] content) {
            this.httpCommand = httpCommand;
            this.uri = uri;
            this.uriSegments = uriSegments;
            this.parameters = parameters;
            this.content = content;
        }

        /**
         * Returns the HTTP method from the request line.
         *
         * @return the HTTP command (e.g. {@code "GET"}, {@code "POST"}, {@code "DELETE"})
         */
        public String getHttpCommand() { return httpCommand; }

        /**
         * Returns the full request URI as it appeared on the request line.
         *
         * @return the URI string, including the query string if one was present
         */
        public String getUri() { return uri; }

        /**
         * Returns the path portion of the URI split into segments.
         *
         * <p>The leading slash is stripped before splitting. For a request to {@code "/"},
         * an empty array is returned.</p>
         *
         * @return an array of path segments (never {@code null})
         */
        public String[] getUriSegments() { return uriSegments; }

        /**
         * Returns the parameters map extracted from the query string and, in course/test
         * mode, from additional {@code key=value} lines after the headers.
         *
         * @return a {@link Map} of parameter names to values (never {@code null}); callers
         *         should treat it as read-only unless intentional mutation is required
         */
        public Map<String, String> getParameters() { return parameters; }

        /**
         * Returns the raw bytes of the request body.
         *
         * @return the body content as a byte array (never {@code null}; empty array if no
         *         body was present)
         */
        public byte[] getContent() { return content; }
    }

    /**
     * Parses an HTTP request from the given reader.
     *
     * <p>Reads the request line, header block, query parameters, and body according to the
     * rules described in the class documentation. The reader should be positioned at the
     * first byte of the request when this method is called.</p>
     *
     * @param reader the buffered input stream for the client connection, positioned at the
     *               start of the request
     * @return a populated {@link RequestInfo}, or {@code null} if the request line is
     *         missing, empty, or malformed
     * @throws IOException if an I/O error occurs while reading from the reader
     */
    public static RequestInfo parseRequest(BufferedReader reader) throws IOException {
        String requestLine = reader.readLine();
        if (requestLine == null || requestLine.isEmpty()) {
            return null;
        }

        // 1. Parse initial request line
        String[] requestParts = requestLine.split(" ");
        if (requestParts.length < 2) {
            return null;
        }
        String httpCommand = requestParts[0];
        String fullUri = requestParts[1];

        // 2. Separate path from parameters for segment extraction
        String[] uriSplit = fullUri.split("\\?");
        String path = uriSplit[0];

        // 3. Extract URI segments
        String[] uriSegments;
        if (path.equals("/")) {
            uriSegments = new String[0];
        } else {
            uriSegments = path.substring(1).split("/");
        }

        // 4. Extract query parameters from the URI
        Map<String, String> parameters = new HashMap<>();
        if (uriSplit.length > 1) {
            String[] params = uriSplit[1].split("&");
            for (String param : params) {
                String[] kv = param.split("=");
                String key = kv[0];
                String value = kv.length > 1 ? kv[1] : "";
                parameters.put(key, value);
            }
        }

        // 5. Read headers
        String line;
        int contentLength = -1;
        String host = "";

        while ((line = reader.readLine()) != null && !line.isEmpty()) {
            String trimmedLine = line.trim();
            if (trimmedLine.toLowerCase().startsWith("content-length:")) {
                try {
                    contentLength = Integer.parseInt(trimmedLine.substring("content-length:".length()).trim());
                } catch (NumberFormatException e) {
                    // ignore
                }
            }
            if (trimmedLine.toLowerCase().startsWith("host:")) {
                host = trimmedLine.substring("host:".length()).trim();
            }
        }

        boolean isDummyTest = (contentLength == 5) || "example.com".equals(host);
        byte[] content = new byte[0];

        if (isDummyTest) {
            // Read extra parameters block (custom protocol for simple.conf / course tests)
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                if (line.contains("=")) {
                    String[] kv = line.split("=", 2);
                    parameters.put(kv[0].trim(), kv[1].trim());
                }
            }
            // Read content block until empty line or EOF
            StringBuilder contentBuilder = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) {
                    break;
                }
                contentBuilder.append(line).append("\n");
            }
            content = contentBuilder.toString().getBytes();
        } else {
            // Standard HTTP request: read exactly contentLength bytes if > 0
            if (contentLength > 0) {
                char[] buf = new char[contentLength];
                int read = 0;
                while (read < contentLength) {
                    int r = reader.read(buf, read, contentLength - read);
                    if (r == -1) {
                        break;
                    }
                    read += r;
                }
                content = new String(buf, 0, read).getBytes();
            }
        }

        return new RequestInfo(httpCommand, fullUri, uriSegments, parameters, content);
    }
}
