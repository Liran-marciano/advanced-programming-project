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

    /**
     * Register {@code s} to handle requests whose verb is {@code httpCommanmd}
     * and whose URI starts with {@code uri}. Subsequent calls with the same
     * (verb, URI) pair replace the previous mapping.
     *
     * @param httpCommanmd HTTP verb to match (e.g. {@code "GET"}, {@code "POST"});
     *                     misspelling preserved to match the course skeleton
     * @param uri          URI prefix this servlet should handle (longest-prefix
     *                     match wins at dispatch time)
     * @param s            the servlet that will receive matching requests
     */
    void addServlet(String httpCommanmd, String uri, Servlet s);

    /**
     * Unregister whatever servlet is currently mapped at this (verb, URI).
     * A no-op if no such mapping exists.
     *
     * @param httpCommanmd HTTP verb of the registration to remove
     * @param uri          URI prefix of the registration to remove
     */
    void removeServlet(String httpCommanmd, String uri);

    /** Start the accept loop in the background. */
    void start();

    /** Stop the accept loop, shut down workers, and close every registered servlet. */
    void close();
}
