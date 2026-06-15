package project_biu.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import project_biu.servlets.Servlet;

/**
 * A minimal homemade HTTP server.
 *
 * <p>{@code MyHTTPServer} extends {@link Thread}, so calling
 * {@link Thread#start()} (declared on {@link HTTPServer} too) runs the
 * accept loop on a background thread. Incoming connections are handed to a
 * bounded {@link ExecutorService} so that requests are served concurrently
 * but the server never opens more than {@code nThreads} worker threads.
 *
 * <p>Servlets are looked up by <strong>longest URI prefix</strong>, not
 * exact match: a servlet registered at {@code /app/} therefore handles
 * {@code /app/index.html}, while one registered at {@code /publish} handles
 * {@code /publish?topic=A}. This matches the convention the project's
 * later exercises rely on.
 */
public class MyHTTPServer extends Thread implements HTTPServer {

    private final int port;
    private final int nThreads;

    // One verb -> URI -> Servlet map per HTTP verb we care about.
    private final ConcurrentHashMap<String, Servlet> getServlets    = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Servlet> postServlets   = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Servlet> deleteServlets = new ConcurrentHashMap<>();

    private volatile boolean stopped = false;
    private volatile ServerSocket serverSocket;
    private ExecutorService executor;

    /**
     * Constructs the server; call {@link #start()} to begin accepting connections.
     *
     * @param port     TCP port to listen on
     * @param nThreads worker pool size for handling concurrent requests
     */
    public MyHTTPServer(int port, int nThreads) {
        this.port = port;
        this.nThreads = nThreads;
    }

    /**
     * Overridden so callers can safely connect to the server the instant
     * {@code start()} returns. We open the {@link ServerSocket} and the
     * worker pool synchronously here, then start the background accept
     * loop via {@link Thread#start()}.
     */
    @Override
    public synchronized void start() {
        if (executor == null) {
            executor = Executors.newFixedThreadPool(nThreads);
        }
        if (serverSocket == null) {
            try {
                serverSocket = new ServerSocket(port);
                serverSocket.setSoTimeout(1000);
            } catch (IOException e) {
                throw new RuntimeException("Could not open server socket on port " + port, e);
            }
        }
        super.start();
    }

    @Override
    public void addServlet(String httpCommanmd, String uri, Servlet s) {
        ConcurrentHashMap<String, Servlet> map = mapFor(httpCommanmd);
        if (map != null) map.put(uri, s);
    }

    @Override
    public void removeServlet(String httpCommanmd, String uri) {
        ConcurrentHashMap<String, Servlet> map = mapFor(httpCommanmd);
        if (map != null) map.remove(uri);
    }

    private ConcurrentHashMap<String, Servlet> mapFor(String verb) {
        if (verb == null) return null;
        if (verb.equalsIgnoreCase("GET"))    return getServlets;
        if (verb.equalsIgnoreCase("POST"))   return postServlets;
        if (verb.equalsIgnoreCase("DELETE")) return deleteServlets;
        return null;
    }

    /**
     * The accept loop. Times out every second so we can re-check
     * {@link #stopped} without blocking forever on a quiet socket.
     * Assumes {@link #start()} has already opened the {@code ServerSocket}
     * and created the executor.
     */
    @Override
    public void run() {
        if (serverSocket == null || executor == null) return;
        while (!stopped) {
            try {
                Socket client = serverSocket.accept();
                executor.submit(() -> handle(client));
            } catch (SocketTimeoutException ignored) {
                // expected; loop and re-check stopped
            } catch (IOException e) {
                if (!stopped) {
                    // Broken accept shouldn't crash the loop.
                }
            }
        }
    }

    /**
     * Worker-thread body. Parses a request off the socket, looks up the
     * matching servlet, and lets it write the response.
     */
    private void handle(Socket client) {
        try (Socket c = client;
             BufferedReader reader = new BufferedReader(new InputStreamReader(c.getInputStream()))) {

            RequestParser.RequestInfo info = RequestParser.parseRequest(reader);
            if (info == null || info.getHttpCommand() == null || info.getHttpCommand().isEmpty()) {
                return;
            }

            Servlet servlet = findServlet(info);
            OutputStream out = c.getOutputStream();
            if (servlet != null) {
                servlet.handle(info, out);
            }
            out.flush();
        } catch (IOException e) {
            // Client closed early or sent garbage; just drop the connection.
        }
    }

    /**
     * Longest-prefix URI lookup, per the project spec. A servlet registered
     * at {@code /app/} wins over one at {@code /app} when both match.
     */
    private Servlet findServlet(RequestParser.RequestInfo info) {
        ConcurrentHashMap<String, Servlet> map = mapFor(info.getHttpCommand());
        if (map == null) return null;
        String uri = info.getUri();
        String bestKey = null;
        for (String key : map.keySet()) {
            if (uri.startsWith(key)) {
                if (bestKey == null || key.length() > bestKey.length()) {
                    bestKey = key;
                }
            }
        }
        return bestKey == null ? null : map.get(bestKey);
    }

    @Override
    public void close() {
        stopped = true;

        // Stop accepting and unblock the accept loop immediately.
        ServerSocket ss = serverSocket;
        if (ss != null) {
            try { ss.close(); } catch (IOException ignored) {}
        }

        // Drain in-flight requests then shut workers down.
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                executor.shutdownNow();
            }
        }

        // Close every registered servlet.
        closeAll(getServlets);
        closeAll(postServlets);
        closeAll(deleteServlets);
    }

    private void closeAll(ConcurrentHashMap<String, Servlet> map) {
        for (Servlet s : map.values()) {
            try { s.close(); } catch (IOException ignored) {}
        }
        map.clear();
    }
}
