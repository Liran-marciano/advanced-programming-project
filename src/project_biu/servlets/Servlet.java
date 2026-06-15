package project_biu.servlets;

import java.io.IOException;
import java.io.OutputStream;

import project_biu.server.RequestParser.RequestInfo;

/**
 * Pluggable HTTP request handler.
 *
 * <p>Implementations are registered with a {@code MyHTTPServer} against a
 * specific HTTP verb and URI prefix; the server invokes
 * {@link #handle(RequestInfo, OutputStream)} for every request whose verb
 * and URI prefix the implementation owns. Implementations are responsible
 * for writing the full HTTP response (status line, headers, blank line,
 * body) to {@code toClient}.
 */
public interface Servlet {

    /** Process a parsed request and write the response to {@code toClient}. */
    void handle(RequestInfo ri, OutputStream toClient) throws IOException;

    /** Release any resources the servlet is holding (called by the server on shutdown). */
    void close() throws IOException;
}
