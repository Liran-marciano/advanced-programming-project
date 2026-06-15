package project_biu.servlets;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;

import project_biu.configs.Graph;
import project_biu.configs.GenericConfig;
import project_biu.graph.TopicManagerSingleton;
import project_biu.server.RequestParser.RequestInfo;
import project_biu.views.HtmlGraphWriter;

/**
 * Servlet bound to {@code POST /upload}.
 *
 * <p>Receives a configuration file uploaded from the browser, saves it
 * on the server side, instantiates a {@link GenericConfig} from it,
 * rebuilds the computational {@link Graph}, and returns the rendered
 * HTML view of that graph (delegating the actual rendering to
 * {@link HtmlGraphWriter}, per the spec's separation-of-concerns
 * recommendation).
 *
 * <p>This servlet also owns the lifetime of the most recently loaded
 * config so that subsequent uploads dispose of the previous one cleanly
 * before standing up the new topology. Calling {@link #close()} (via
 * {@code server.close()} on shutdown) tears down whichever config is
 * currently in flight.
 */
public class ConfLoader implements Servlet {

    /** Constructs a new {@code ConfLoader} with no currently-loaded config. */
    public ConfLoader() { /* no setup needed */ }

    /** Where uploaded configs are persisted so the user can find them later. */
    private static final Path UPLOAD_DIR = Paths.get("config_files");

    /** The config that produced the currently-deployed graph, or {@code null}. */
    private GenericConfig currentConfig;

    @Override
    public synchronized void handle(RequestInfo ri, OutputStream toClient) throws IOException {
        // The upload form sends the body as:
        //   filename="<name>"
        //   <blank line>
        //   <file content>
        // ...which our RequestParser turns into a "filename" parameter plus
        // the raw bytes of the file in getContent().
        Map<String, String> params = ri.getParameters();
        String filename = stripQuotes(params.get("filename"));
        if (filename == null || filename.isEmpty()) filename = "uploaded.conf";

        byte[] content = ri.getContent();
        if (content == null || content.length == 0) {
            HtmlLoader.writeResponse(toClient, "400 Bad Request", "text/html; charset=UTF-8",
                    errorPage("The uploaded file was empty.").getBytes(StandardCharsets.UTF_8));
            return;
        }

        // Persist the upload alongside the project's other config files.
        Files.createDirectories(UPLOAD_DIR);
        Path saved = UPLOAD_DIR.resolve(filename).normalize();
        if (!saved.startsWith(UPLOAD_DIR.toAbsolutePath().normalize())
                && !saved.startsWith(UPLOAD_DIR)) {
            // Defensive: reject paths that try to escape config_files/.
            HtmlLoader.writeResponse(toClient, "400 Bad Request", "text/html; charset=UTF-8",
                    errorPage("Illegal filename.").getBytes(StandardCharsets.UTF_8));
            return;
        }
        Files.write(saved, content,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE);

        // Tear down any previous deployment (workers, subscriptions) before
        // we stand up the new one. We also clear the topic registry so the
        // table view doesn't accumulate stale entries across uploads.
        if (currentConfig != null) {
            currentConfig.close();
            currentConfig = null;
        }
        TopicManagerSingleton.get().clear();

        GenericConfig config = new GenericConfig();
        config.setConfFile(saved.toString());
        try {
            config.create();
        } catch (RuntimeException ex) {
            config.close();
            HtmlLoader.writeResponse(toClient, "400 Bad Request", "text/html; charset=UTF-8",
                    errorPage("Could not load config: " + ex.getMessage()).getBytes(StandardCharsets.UTF_8));
            return;
        }
        currentConfig = config;

        Graph graph = new Graph();
        graph.createFromTopics();

        List<String> lines = HtmlGraphWriter.getGraphHTML(graph);
        StringBuilder body = new StringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            body.append(lines.get(i));
            if (i < lines.size() - 1) body.append('\n');
        }
        HtmlLoader.writeResponse(toClient, "200 OK", "text/html; charset=UTF-8",
                body.toString().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public synchronized void close() {
        if (currentConfig != null) {
            currentConfig.close();
            currentConfig = null;
        }
    }

    // --------------------------------------------------------------- helpers

    /**
     * Strips a single layer of double quotes from a parameter value, since
     * the browser-side JS sends {@code filename="simple.conf"} and our
     * parser keeps the quotes as part of the value.
     */
    private static String stripQuotes(String s) {
        if (s == null) return null;
        s = s.trim();
        if (s.length() >= 2 && s.charAt(0) == '"' && s.charAt(s.length() - 1) == '"') {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

    private static String errorPage(String msg) {
        return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><title>Error</title>"
                + "<link href=\"https://fonts.googleapis.com/css2?family=Inter:wght@400;600;700&family=JetBrains+Mono&display=swap\" rel=\"stylesheet\">"
                + "<style>"
                + "html,body{margin:0;height:100vh;display:flex;align-items:center;justify-content:center;flex-direction:column;"
                + "background:radial-gradient(ellipse at center,#1e293b 0%,#0f172a 70%);"
                + "color:#f1f5f9;font-family:'Inter',sans-serif;gap:14px;padding:24px;box-sizing:border-box;text-align:center;}"
                + ".icon{font-size:48px;}"
                + ".title{font-size:18px;font-weight:700;color:#f43f5e;}"
                + ".msg{color:#94a3b8;font-size:13px;max-width:420px;font-family:'JetBrains Mono',monospace;background:rgba(255,255,255,0.05);padding:12px 16px;border-radius:8px;border:1px solid rgba(244,63,94,0.2);}"
                + "</style></head><body>"
                + "<div class=\"icon\">&#9888;</div>"
                + "<div class=\"title\">Deployment failed</div>"
                + "<div class=\"msg\">"
                + msg.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                + "</div></body></html>";
    }
}
