package servlets;

import java.io.IOException;
import java.io.OutputStream;
import server.RequestParser;

public interface Servlet {
    void handle(RequestParser.RequestInfo ri, OutputStream toClient) throws IOException;
    void close() throws IOException;
}
