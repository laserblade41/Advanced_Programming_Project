package server;
import servlets.Servlet;

/**
 * Contract for a minimal, servlet-based HTTP server.
 *
 * <p><strong>SOLID:</strong> this interface enforces Dependency Inversion - callers (e.g.
 * {@code Main}) program against {@code HTTPServer} rather than the concrete
 * {@link MyHTTPServer}, and the server in turn dispatches to the {@link servlets.Servlet}
 * abstraction. It is also Open/Closed: new routes/behaviors are added by registering new
 * servlets via {@link #addServlet}, never by editing the server itself.</p>
 *
 * <p>This interface defines the registration and lifecycle API for an embeddable HTTP server.
 * Implementations accept incoming TCP connections, parse requests, and dispatch them to
 * registered {@link servlets.Servlet} handlers. The server extends {@link Runnable} so it
 * can run on its own thread.</p>
 *
 * <p>Supported HTTP methods for servlet registration are {@code GET}, {@code POST}, and
 * {@code DELETE} (case-insensitive at registration time). URI routing uses
 * <strong>longest prefix match</strong>: the registered URI prefix with the greatest length
 * that matches the start of the request path is selected. See {@link MyHTTPServer} for the
 * reference implementation.</p>
 *
 * <p>Typical usage:</p>
 * <pre>{@code
 * HTTPServer server = new MyHTTPServer(8080, 5);
 * server.addServlet("GET", "/app/", new HtmlLoader("html_files"));
 * server.addServlet("POST", "/upload", new ConfLoader());
 * server.start();
 * // ... application runs ...
 * server.close();
 * }</pre>
 *
 * @see MyHTTPServer
 * @see servlets.Servlet
 */
public interface HTTPServer extends Runnable{

    /**
     * Registers a servlet to handle requests for the given HTTP method and URI prefix.
     *
     * <p>If a servlet is already registered for the same method and URI, it is replaced.</p>
     *
     * @param httpCommanmd the HTTP method name (e.g. {@code "GET"}, {@code "POST"}, {@code "DELETE"})
     * @param uri the URI path prefix to match (e.g. {@code "/app/"})
     * @param s the servlet instance that will handle matching requests
     */
    public void addServlet(String httpCommanmd, String uri, Servlet s);

    /**
     * Removes a previously registered servlet mapping.
     *
     * <p>If no mapping exists for the given method and URI, this method has no effect.</p>
     *
     * @param httpCommanmd the HTTP method name of the mapping to remove
     * @param uri the URI path prefix of the mapping to remove
     */
    public void removeServlet(String httpCommanmd, String uri);

    /**
     * Starts the server and begins accepting client connections.
     *
     * <p>The threading model is implementation-defined. In {@link MyHTTPServer}, this starts
     * the accept loop on a background thread.</p>
     */
    public void start();

    /**
     * Shuts down the server gracefully.
     *
     * <p>Implementations should stop accepting new connections, terminate or drain worker
     * threads, and invoke {@link Servlet#close()} on all registered servlets to release
     * their resources.</p>
     */
    public void close();
}
