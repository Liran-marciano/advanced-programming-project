package project_biu;

import project_biu.server.HTTPServer;
import project_biu.server.MyHTTPServer;
import project_biu.servlets.ConfLoader;
import project_biu.servlets.HtmlLoader;
import project_biu.servlets.TopicDisplayer;
import project_biu.servlets.TopicReset;

/**
 * Entry point of the Advanced Programming project.
 *
 * <p>Stands up a {@link MyHTTPServer} on port {@code 8080} with a worker
 * pool of 5 threads, registers the three project servlets, blocks until
 * the user presses Enter on stdin, then shuts everything down cleanly.
 *
 * <p>After {@code start()} returns, open
 * <a href="http://localhost:8080/app/index.html">http://localhost:8080/app/index.html</a>
 * in a browser to use the application.
 */
public class Main {

    /** Utility class; not meant to be instantiated. */
    public Main() { /* no setup needed */ }

    /**
     * Boots the HTTP server, registers servlets, and waits for stdin to shut down.
     *
     * @param args command-line arguments (ignored)
     * @throws Exception if the server fails to start or stdin read fails
     */
    public static void main(String[] args) throws Exception {
        HTTPServer server = new MyHTTPServer(8080, 5);
        server.addServlet("GET",  "/publish", new TopicDisplayer());
        server.addServlet("POST", "/upload",  new ConfLoader());
        server.addServlet("GET",  "/reset",   new TopicReset());
        server.addServlet("GET",  "/app/",    new HtmlLoader("html_files"));
        server.start();

        System.out.println("Server running on http://localhost:8080/app/index.html");
        System.out.println("Press Enter to shut down.");
        System.in.read();

        server.close();
        System.out.println("done");
    }
}
