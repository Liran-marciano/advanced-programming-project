package project_biu.server;

import project_biu.servlets.Servlet;

/**
 * Generic HTTP server abstraction.
 *
 * <p>Implementations map (HTTP verb, URI prefix) pairs to {@link Servlet}s
 * and dispatch incoming client requests to the right one. The server itself
 * is {@link Runnable} (its accept loop is a long-running task) but for
 * convenience the implementation extends {@link Thread}, so {@link #start()}
 * launches the accept loop in the background.
 */
public interface HTTPServer extends Runnable {

    /** Register {@code s} to handle requests with this verb and URI prefix. */
    void addServlet(String httpCommanmd, String uri, Servlet s);

    /** Unregister whatever servlet is currently mapped at this (verb, URI). */
    void removeServlet(String httpCommanmd, String uri);

    /** Start the accept loop in the background. */
    void start();

    /** Stop the accept loop, shut down workers, and close every registered servlet. */
    void close();
}
