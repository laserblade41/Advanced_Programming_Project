package server;

import servlets.Servlet;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


/**
 * Default {@link HTTPServer} implementation backed by a {@link ServerSocket} and a fixed
 * thread pool.
 *
 * <p>This class extends {@link Thread}. Calling {@link #start()} delegates to
 * {@link Thread#start()}, which runs the accept loop in {@link #run()}. Each accepted
 * client connection is handed off to a {@code ClientHandler} task submitted to an
 * {@link ExecutorService}.</p>
 *
 * <p><strong>Request routing:</strong> For each incoming request, the server selects the
 * servlet map that corresponds to the HTTP method ({@code GET}, {@code POST}, or
 * {@code DELETE}), then finds the registered URI prefix with the <strong>longest matching
 * prefix</strong> against the request path (query string is stripped before matching).
 * If no servlet matches, the client receives {@code 404 Not Found}. If the HTTP method is
 * not supported, the client receives {@code 405 Method Not Allowed}.</p>
 *
 * <p><strong>Thread safety:</strong></p>
 * <ul>
 *   <li>Servlet registries ({@code getServlets}, {@code postServlets}, {@code deleteServlets})
 *       are {@link ConcurrentHashMap} instances, so {@link #addServlet} and
 *       {@link #removeServlet} may be called concurrently with active request handling.</li>
 *   <li>The {@link ExecutorService} created via {@link Executors#newFixedThreadPool(int)}
 *       processes multiple client connections in parallel, up to {@code nThreads} at a time.</li>
 *   <li>Because multiple pool threads may invoke {@link Servlet#handle} on the same servlet
 *       instance concurrently, servlet implementations that hold shared mutable state must
 *       provide their own synchronization.</li>
 *   <li>The {@code isRunning} flag coordinates shutdown of the accept loop.</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * MyHTTPServer server = new MyHTTPServer(8080, 5);
 * server.addServlet("GET", "/app/", new HtmlLoader("html_files"));
 * server.addServlet("POST", "/api/data", myServlet);
 * server.start();
 * // ... block or run other application logic ...
 * server.close();
 * }</pre>
 *
 * @see HTTPServer
 * @see RequestParser
 * @see Servlet
 */
public class MyHTTPServer extends Thread implements HTTPServer {

    private int port;
    private boolean isRunning;
    private ExecutorService threadPool;
    private ServerSocket serverSocket;

    // Thread-safe maps for mapping URIs to Servlets
    private ConcurrentHashMap<String, Servlet> getServlets;
    private ConcurrentHashMap<String, Servlet> postServlets;
    private ConcurrentHashMap<String, Servlet> deleteServlets;

    /**
     * Creates a new HTTP server bound to the given port with a fixed-size thread pool.
     *
     * @param port the TCP port on which the server will listen for incoming connections
     * @param nThreads the maximum number of client connections handled concurrently
     */
    public MyHTTPServer(int port, int nThreads) {
        this.port = port;
        this.isRunning = false;

        // Executor service to handle clients concurrently
        this.threadPool = Executors.newFixedThreadPool(nThreads);

        this.getServlets = new ConcurrentHashMap<>();
        this.postServlets = new ConcurrentHashMap<>();
        this.deleteServlets = new ConcurrentHashMap<>();
    }

    /**
     * Registers a servlet for the given HTTP method and URI prefix.
     *
     * <p>The HTTP method is matched case-insensitively. Supported methods are
     * {@code GET}, {@code POST}, and {@code DELETE}. An existing mapping for the same
     * method and URI is overwritten.</p>
     *
     * @param httpCommand the HTTP method name (e.g. {@code "GET"})
     * @param uri the URI path prefix to match (e.g. {@code "/app/"})
     * @param s the servlet instance that will handle matching requests
     */
    @Override
    public void addServlet(String httpCommand, String uri, Servlet s) {
        switch (httpCommand.toUpperCase()) {
            case "GET":
                getServlets.put(uri, s);
                break;
            case "POST":
                postServlets.put(uri, s);
                break;
            case "DELETE":
                deleteServlets.put(uri, s);
                break;
        }
    }

    /**
     * Removes a servlet mapping for the given HTTP method and URI prefix.
     *
     * <p>The HTTP method is matched case-insensitively. If no mapping exists, this method
     * has no effect.</p>
     *
     * @param httpCommand the HTTP method name of the mapping to remove
     * @param uri the URI path prefix of the mapping to remove
     */
    @Override
    public void removeServlet(String httpCommand, String uri) {
        switch (httpCommand.toUpperCase()) {
            case "GET":
                getServlets.remove(uri);
                break;
            case "POST":
                postServlets.remove(uri);
                break;
            case "DELETE":
                deleteServlets.remove(uri);
                break;
        }
    }

    /**
     * Starts the server accept loop on this thread.
     *
     * <p>Delegates to {@link Thread#start()}, which invokes {@link #run()} asynchronously.</p>
     */
    // Implementing start by simply starting the Thread (Option A from the document)
    @Override
    public void start() {
        super.start();
    }

    /**
     * Main server loop: binds a {@link ServerSocket}, accepts client connections, and
     * dispatches each to the thread pool.
     *
     * <p>The server socket has a 1-second accept timeout so the loop can periodically check
     * {@code isRunning} and exit cleanly. When the loop ends (normally or due to I/O error),
     * {@link #close()} is called from a {@code finally} block.</p>
     */
    @Override
    public void run() {
        this.isRunning = true;
        try {
            this.serverSocket = new ServerSocket(this.port);
            // Set timeout to 1 second so the loop can check isRunning periodically
            this.serverSocket.setSoTimeout(1000);

            while (this.isRunning) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    // Pass the connected client to the thread pool
                    threadPool.submit(new ClientHandler(clientSocket));
                } catch (SocketTimeoutException e) {
                    // Timeout is expected every second; loop continues if isRunning is true
                }
            }
        } catch (IOException e) {
            if (isRunning) {
                e.printStackTrace();
            }
        } finally {
            close();
        }
    }

    /**
     * Shuts down the server and releases all resources.
     *
     * <p>This method sets {@code isRunning} to {@code false}, closes the server socket,
     * shuts down the thread pool (waiting up to 2 seconds for termination), and invokes
     * {@link Servlet#close()} on every registered servlet. If called from a thread other
     * than the server thread, it also waits for the server thread to finish via
     * {@link Thread#join()}.</p>
     */
    @Override
    public void close() {
        this.isRunning = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            // ignore
        }

        // Shut down the thread pool smoothly
        threadPool.shutdown();
        try {
            threadPool.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Close all registered servlets
        try {
            closeAllServlets(getServlets);
            closeAllServlets(postServlets);
            closeAllServlets(deleteServlets);
        } catch (IOException e) {
            // ignore
        }

        if (Thread.currentThread() != this) {
            try {
                this.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void closeAllServlets(Map<String, Servlet> servlets) throws IOException {
        for (Servlet s : servlets.values()) {
            s.close();
        }
    }

    // --- Inner class to handle individual client requests in a background thread ---
    private class ClientHandler implements Runnable {
        private Socket clientSocket;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                 OutputStream out = clientSocket.getOutputStream()) {

                // 1. Parse the incoming request
                RequestParser.RequestInfo requestInfo = RequestParser.parseRequest(reader);

                if (requestInfo != null) {
                    // 2. Select the correct map based on HTTP command
                    Map<String, Servlet> targetMap = getTargetMap(requestInfo.getHttpCommand());

                    if (targetMap != null) {
                        // 3. Find the longest matching URI prefix
                        Servlet matchingServlet = findLongestMatchServlet(targetMap, requestInfo.getUri());

                        if (matchingServlet != null) {
                            // 4. Handle the request
                            matchingServlet.handle(requestInfo, out);
                        } else {
                            sendErrorResponse(out, 404, "Not Found");
                        }
                    } else {
                        sendErrorResponse(out, 405, "Method Not Allowed");
                    }
                }

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private Map<String, Servlet> getTargetMap(String command) {
            return switch (command.toUpperCase()) {
                case "GET" -> getServlets;
                case "POST" -> postServlets;
                case "DELETE" -> deleteServlets;
                default -> null;
            };
        }

        // Implementation of the longest prefix match algorithm
        private Servlet findLongestMatchServlet(Map<String, Servlet> map, String requestUri) {
            String requestPath = requestUri.split("\\?")[0];
            String longestMatch = "";
            Servlet bestServlet = null;

            for (String key : map.keySet()) {
                if (requestPath.startsWith(key) && key.length() > longestMatch.length()) {
                    longestMatch = key;
                    bestServlet = map.get(key);
                }
            }
            return bestServlet;
        }

        private void sendErrorResponse(OutputStream out, int code, String message) throws IOException {
            String response = "HTTP/1.1 " + code + " " + message + "\r\n\r\n";
            out.write(response.getBytes());
            out.flush();
        }
    }
}
