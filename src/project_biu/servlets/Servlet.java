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

    /**
     * Process a parsed request and write the full HTTP response (status
     * line, headers, blank line, body) to {@code toClient}.
     *
     * @param ri        parsed request &mdash; URI, parameters, content bytes
     * @param toClient  the socket's output stream; the implementation must
     *                  write a syntactically valid HTTP response here
     * @throws IOException if writing to the socket fails (network error,
     *                     client closed prematurely, &hellip;)
     */
    void handle(RequestInfo ri, OutputStream toClient) throws IOException;

    /**
     * Release any resources the servlet is holding. Called by
     * {@link project_biu.server.HTTPServer#close()} when the server is
     * shutting down.
     *
     * @throws IOException if cleanup fails
     */
    void close() throws IOException;
}
