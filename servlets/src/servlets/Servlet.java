package servlets;

import java.io.IOException;
import java.io.OutputStream;
import server.RequestParser;

/**
 * Core extension point for handling HTTP requests in the server framework.
 *
 * <p>A {@code Servlet} receives a parsed {@link RequestParser.RequestInfo} and the client's
 * {@link OutputStream}. It is responsible for writing a <strong>complete</strong> HTTP/1.1
 * response, including the status line, headers, the blank line separator ({@code \r\n\r\n}),
 * and the response body.</p>
 *
 * <p>Servlets are registered on an {@link server.HTTPServer} via
 * {@link server.HTTPServer#addServlet}. When a matching request arrives, the server invokes
 * {@link #handle} on a worker thread from its {@link java.util.concurrent.ExecutorService}
 * thread pool.</p>
 *
 * <p><strong>Thread safety:</strong> The server does not serialize calls to the same servlet
 * instance. If a servlet holds shared mutable state, it must synchronize access or use
 * concurrent data structures. Stateless servlets are inherently safe for concurrent use.</p>
 *
 * <p>Example custom servlet:</p>
 * <pre>{@code
 * public class EchoServlet implements Servlet {
 *     public void handle(RequestParser.RequestInfo ri, OutputStream out) throws IOException {
 *         byte[] body = ("Echo: " + ri.getUri()).getBytes();
 *         out.write(("HTTP/1.1 200 OK\r\nContent-Length: " + body.length + "\r\n\r\n").getBytes());
 *         out.write(body);
 *         out.flush();
 *     }
 *     public void close() throws IOException { }
 * }
 * }</pre>
 *
 * @see server.HTTPServer
 * @see server.MyHTTPServer
 * @see RequestParser.RequestInfo
 */
public interface Servlet {

    /**
     * Handles an incoming HTTP request and writes the response to the client.
     *
     * <p>The implementation must write a full HTTP/1.1 response to {@code toClient},
     * including status line, headers, and body. The server closes the client socket after
     * this method returns.</p>
     *
     * @param ri the parsed request information (method, URI, parameters, body)
     * @param toClient the output stream connected to the client socket
     * @throws IOException if an I/O error occurs while writing the response
     */
    void handle(RequestParser.RequestInfo ri, OutputStream toClient) throws IOException;

    /**
     * Releases any resources held by this servlet.
     *
     * <p>Called once per registered servlet instance during
     * {@link server.HTTPServer#close()}. Implementations that do not hold resources may
     * provide an empty body.</p>
     *
     * @throws IOException if an I/O error occurs while closing resources
     */
    void close() throws IOException;
}
