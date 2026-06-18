package server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import servlets.Servlet;


public class MyHTTPServer extends Thread implements HTTPServer {

    private int port;
    private boolean isRunning;
    private ExecutorService threadPool;
    private ServerSocket serverSocket;

    // Thread-safe maps for mapping URIs to Servlets
    private ConcurrentHashMap<String, Servlet> getServlets;
    private ConcurrentHashMap<String, Servlet> postServlets;
    private ConcurrentHashMap<String, Servlet> deleteServlets;

    public MyHTTPServer(int port, int nThreads) {
        this.port = port;
        this.isRunning = false;

        // Executor service to handle clients concurrently
        this.threadPool = Executors.newFixedThreadPool(nThreads);

        this.getServlets = new ConcurrentHashMap<>();
        this.postServlets = new ConcurrentHashMap<>();
        this.deleteServlets = new ConcurrentHashMap<>();
    }

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

    // Implementing start by simply starting the Thread (Option A from the document)
    @Override
    public void start() {
        super.start();
    }

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
            switch (command.toUpperCase()) {
                case "GET": return getServlets;
                case "POST": return postServlets;
                case "DELETE": return deleteServlets;
                default: return null;
            }
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