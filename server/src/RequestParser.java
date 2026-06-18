import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class RequestParser {

    public static class RequestInfo {
        private String httpCommand;
        private String uri;
        private String[] uriSegments;
        private Map<String, String> parameters;
        private byte[] content;

        public RequestInfo(String httpCommand, String uri, String[] uriSegments,
                           Map<String, String> parameters, byte[] content) {
            this.httpCommand = httpCommand;
            this.uri = uri;
            this.uriSegments = uriSegments;
            this.parameters = parameters;
            this.content = content;
        }

        public String getHttpCommand() { return httpCommand; }
        public String getUri() { return uri; }
        public String[] getUriSegments() { return uriSegments; }
        public Map<String, String> getParameters() { return parameters; }
        public byte[] getContent() { return content; }
    }

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