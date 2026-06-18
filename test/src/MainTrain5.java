package test;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


public class MainTrain5 { // RequestParser
    

    private static void testParseRequest() {
        // Test data
        String request = "GET /api/resource?id=123&name=test HTTP/1.1\n" +
                            "Host: example.com\n" +
                            "Content-Length: 5\n"+
                            "\n" +
                            "filename=\"hello_world.txt\"\n"+
                            "\n" +
                            "hello world!\n"+
                            "\n" ;

        BufferedReader input=new BufferedReader(new InputStreamReader(new ByteArrayInputStream(request.getBytes())));
        try {
            RequestParser.RequestInfo requestInfo = RequestParser.parseRequest(input);

            // Test HTTP command
            if (!requestInfo.getHttpCommand().equals("GET")) {
                System.out.println("HTTP command test failed (-5)");
            }

            // Test URI
            if (!requestInfo.getUri().equals("/api/resource?id=123&name=test")) {
                System.out.println("URI test failed (-5)");
            }

            // Test URI segments
            String[] expectedUriSegments = {"api", "resource"};
            if (!Arrays.equals(requestInfo.getUriSegments(), expectedUriSegments)) {
                System.out.println("URI segments test failed (-5)");
                for(String s : requestInfo.getUriSegments()){
                    System.out.println(s);
                }
            } 
            // Test parameters
            Map<String, String> expectedParams = new HashMap<>();
            expectedParams.put("id", "123");
            expectedParams.put("name", "test");
            expectedParams.put("filename","\"hello_world.txt\"");
            if (!requestInfo.getParameters().equals(expectedParams)) {
                System.out.println("Parameters test failed (-5)");
            }

            // Test content
            byte[] expectedContent = "hello world!\n".getBytes();
            if (!Arrays.equals(requestInfo.getContent(), expectedContent)) {
                System.out.println("Content test failed (-5)");
            } 
            input.close();
        } catch (IOException e) {
            System.out.println("Exception occurred during parsing: " + e.getMessage() + " (-5)");
        }        
    }


    public static void testServer() throws Exception{
        int initialThreads = Thread.activeCount();
        MyHTTPServer server = new MyHTTPServer(8080, 5);

        // Add a test servlet
        server.addServlet("GET", "/calc", new Servlet() {
            @Override
            public void handle(RequestParser.RequestInfo ri, java.io.OutputStream toClient) throws IOException {
                String aStr = ri.getParameters().get("a");
                String bStr = ri.getParameters().get("b");
                int a = aStr != null ? Integer.parseInt(aStr) : 0;
                int b = bStr != null ? Integer.parseInt(bStr) : 0;
                int sum = a + b;
                String response = "HTTP/1.1 200 OK\r\nContent-Length: " + String.valueOf(sum).length() + "\r\n\r\n" + sum;
                toClient.write(response.getBytes());
                toClient.flush();
            }

            @Override
            public void close() throws IOException {
            }
        });

        server.start();
        Thread.sleep(200);

        int threadsAfterStart = Thread.activeCount();
        if (threadsAfterStart != initialThreads + 1) {
            System.out.println("Warning: expected exactly " + (initialThreads + 1) + " active threads after start, but got " + threadsAfterStart);
        }

        // Connect client socket
        java.net.Socket client = new java.net.Socket("localhost", 8080);
        java.io.PrintWriter out = new java.io.PrintWriter(client.getOutputStream(), true);
        BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));

        // Send GET request
        out.print("GET /calc?a=10&b=20 HTTP/1.1\r\nHost: localhost:8080\r\n\r\n");
        out.flush();

        // Read response
        String line = in.readLine();
        if (line == null || !line.contains("200 OK")) {
            System.out.println("Server test FAILED: expected 200 OK status line");
        }

        // Consume headers
        while ((line = in.readLine()) != null && !line.isEmpty()) {
            // consume headers
        }

        // Read body
        char[] buf = new char[2];
        in.read(buf);
        String body = new String(buf);
        if (!body.equals("30")) {
            System.out.println("Server test FAILED: expected body '30', got '" + body + "'");
        } else {
            System.out.println("Server test PASSED: 10 + 20 = 30");
        }

        client.close();
        server.close();

        Thread.sleep(2000);
        int threadsAfterClose = Thread.activeCount();
        if (threadsAfterClose != initialThreads) {
            System.out.println("Warning: threads not fully cleaned up. Expected " + initialThreads + ", got " + threadsAfterClose);
        } else {
            System.out.println("Server cleanup PASSED: all threads closed.");
        }
    }
    
    public static void main(String[] args) {
        testParseRequest(); // 40 points
        try{
            testServer(); // 60
        }catch(Exception e){
            System.out.println("your server throwed an exception (-60)");
        }
        System.out.println("done");
    }

}
