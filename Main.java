import server.HTTPServer;
import server.MyHTTPServer;
import servlets.TopicDisplayer;
import servlets.ConfLoader;
import servlets.HtmlLoader;

/**
 * Application entry point: bootstraps the HTTP server and wires up the dashboard routes.
 *
 * <p>Note the program depends on the {@link HTTPServer} abstraction, not the concrete
 * implementation - the only place the concrete type appears is the constructor call. Routing
 * is configured declaratively by registering servlets against (method, URI) pairs, so adding
 * features means adding servlets here, never modifying the server itself.</p>
 */
public class Main {
    public static void main(String[] args) throws Exception {
        // Construct the server: listen on port 8080 with a pool of 5 worker threads.
        HTTPServer server = new MyHTTPServer(8080, 5);

        // Route table mapping each endpoint to the servlet that handles it:
        // GET /publish  -> publish a message and render the topic-values table.
        server.addServlet("GET", "/publish", new TopicDisplayer());
        // /upload (GET + POST share one instance): upload a config and render the graph.
        ConfLoader confLoader = new ConfLoader();
        server.addServlet("POST", "/upload", confLoader);
        server.addServlet("GET", "/upload", confLoader);
        // GET /app/ -> serve static dashboard assets from the html_files directory.
        server.addServlet("GET", "/app/", new HtmlLoader("html_files"));

        // Launch the accept loop on its own thread, then block the main thread until the user
        // presses Enter; this keeps the process alive while the server runs in the background.
        server.start();
        System.in.read();
        // Graceful shutdown: stop accepting, drain the pool, and close all servlets.
        server.close();
        System.out.println("done");
    }
}
