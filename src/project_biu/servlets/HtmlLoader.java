package project_biu.servlets;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import project_biu.server.RequestParser.RequestInfo;

/**
 * Serves static HTML files out of a configured directory in response to
 * any {@code GET /app/...} request.
 *
 * <p>The folder is passed to the constructor &mdash; this servlet never
 * hard-codes {@code "html_files"} (per the Ex 6 spec, the same servlet
 * should be reusable across projects that organise their views
 * differently).
 *
 * <p>URL convention: the final {@code URISegment} names the file. So
 * {@code GET /app/index.html} resolves to {@code <htmlDir>/index.html}.
 * A bare {@code GET /app/} falls back to {@code index.html}. Requests
 * for missing files get a small 404 HTML page rather than a connection
 * drop, so the user sees something useful in their iframe.
 */
public class HtmlLoader implements Servlet {

    private final Path htmlDir;

    /**
     * Constructs a loader that serves static files out of {@code htmlDir}.
     *
     * @param htmlDir directory holding the static HTML files this servlet
     *                may serve; resolved relative to the JVM's working
     *                directory if not absolute
     */
    public HtmlLoader(String htmlDir) {
        this.htmlDir = Paths.get(htmlDir).toAbsolutePath().normalize();
    }

    @Override
    public void handle(RequestInfo ri, OutputStream toClient) throws IOException {
        String filename = pickFilename(ri);
        Path requested = htmlDir.resolve(filename).normalize();

        // Defence in depth: refuse anything that tries to escape htmlDir
        // via "../" or absolute paths smuggled in through the URL.
        if (!requested.startsWith(htmlDir) || !Files.exists(requested) || !Files.isRegularFile(requested)) {
            writeNotFound(toClient, filename);
            return;
        }

        byte[] body = Files.readAllBytes(requested);
        writeResponse(toClient, "200 OK", contentTypeFor(filename), body);
    }

    @Override
    public void close() {
        // No background resources.
    }

    /** Pull the file name out of the URI segments; default to {@code index.html}. */
    private static String pickFilename(RequestInfo ri) {
        String[] segs = ri.getUriSegments();
        if (segs == null || segs.length == 0) return "index.html";
        String last = segs[segs.length - 1];
        // /app/ alone -> segs == ["app"]; treat as "give me index.html".
        if ("app".equals(last) && segs.length == 1) return "index.html";
        // Strip any leftover query string artefact (defensive; shouldn't happen
        // because RequestParser keeps the query off the segments list, but
        // never trust untrusted input).
        int q = last.indexOf('?');
        if (q >= 0) last = last.substring(0, q);
        return last.isEmpty() ? "index.html" : last;
    }

    /** Tiny MIME map so JS / CSS / etc. served from the same folder work too. */
    private static String contentTypeFor(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".html") || lower.endsWith(".htm")) return "text/html; charset=UTF-8";
        if (lower.endsWith(".css"))  return "text/css; charset=UTF-8";
        if (lower.endsWith(".js"))   return "application/javascript; charset=UTF-8";
        if (lower.endsWith(".svg"))  return "image/svg+xml";
        if (lower.endsWith(".png"))  return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".json")) return "application/json; charset=UTF-8";
        return "text/plain; charset=UTF-8";
    }

    private static void writeNotFound(OutputStream out, String name) throws IOException {
        String html = "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><title>Not found</title>"
                + "<link href=\"https://fonts.googleapis.com/css2?family=Inter:wght@400;600&family=JetBrains+Mono&display=swap\" rel=\"stylesheet\">"
                + "<style>"
                + "html,body{margin:0;height:100vh;display:flex;align-items:center;justify-content:center;flex-direction:column;"
                + "background:radial-gradient(ellipse at center,#1e293b 0%,#0f172a 70%);"
                + "color:#f1f5f9;font-family:'Inter',sans-serif;gap:8px;}"
                + ".code{font-size:64px;font-weight:700;background:linear-gradient(135deg,#f43f5e,#a78bfa);-webkit-background-clip:text;background-clip:text;color:transparent;line-height:1;}"
                + ".msg{color:#94a3b8;font-size:14px;}"
                + ".file{font-family:'JetBrains Mono',monospace;background:rgba(255,255,255,0.05);padding:2px 8px;border-radius:4px;color:#38bdf8;}"
                + "</style></head><body>"
                + "<div class=\"code\">404</div>"
                + "<div class=\"msg\">No file <span class=\"file\">" + escape(name) + "</span> in the view directory.</div>"
                + "</body></html>";
        writeResponse(out, "404 Not Found", "text/html; charset=UTF-8",
                      html.getBytes(StandardCharsets.UTF_8));
    }

    /** Writes a complete HTTP response (status line + headers + body). */
    static void writeResponse(OutputStream out, String status, String contentType, byte[] body) throws IOException {
        String header = "HTTP/1.1 " + status + "\r\n"
                      + "Content-Type: " + contentType + "\r\n"
                      + "Content-Length: " + body.length + "\r\n"
                      + "Connection: close\r\n"
                      + "\r\n";
        out.write(header.getBytes(StandardCharsets.UTF_8));
        out.write(body);
        out.flush();
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
