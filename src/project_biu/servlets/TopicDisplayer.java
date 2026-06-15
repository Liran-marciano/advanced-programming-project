package project_biu.servlets;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import project_biu.configs.Graph;
import project_biu.graph.Message;
import project_biu.graph.Topic;
import project_biu.graph.TopicManagerSingleton;
import project_biu.server.RequestParser.RequestInfo;
import project_biu.views.HtmlGraphWriter;

/**
 * Servlet bound to {@code GET /publish}.
 *
 * <p>Pulls the {@code topic} and {@code message} query parameters out of
 * the request, publishes the message through the {@link TopicManagerSingleton}
 * &mdash; thereby waking up every subscriber on that topic &mdash; and
 * then writes back a small HTML page containing a two-column table of
 * every known topic and its latest value.
 *
 * <p>As a quality-of-life touch beyond the minimum requirements, the
 * response also includes a tiny {@code <script>} block that asks the
 * browser to refresh the centre iframe with the current graph view, so
 * the values printed above the topic nodes stay in sync with the table.
 */
public class TopicDisplayer implements Servlet {

    /** Constructs a new {@code TopicDisplayer}. */
    public TopicDisplayer() { /* no setup needed */ }

    /** Short pause after publishing so ParallelAgent workers can propagate
     *  values downstream before we snapshot the graph for the view. */
    private static final long PROPAGATION_DELAY_MS = 150;

    @Override
    public void handle(RequestInfo ri, OutputStream toClient) throws IOException {
        Map<String, String> params = ri.getParameters();
        String topicName = params.get("topic");
        String message   = params.get("message");

        if (topicName != null && !topicName.isEmpty() && message != null) {
            Topic t = TopicManagerSingleton.get().getTopic(topicName);
            t.publish(new Message(message));
            try {
                Thread.sleep(PROPAGATION_DELAY_MS);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }

        byte[] body = renderTablePage().getBytes(StandardCharsets.UTF_8);
        HtmlLoader.writeResponse(toClient, "200 OK", "text/html; charset=UTF-8", body);
    }

    @Override
    public void close() {
        // No background resources.
    }

    /**
     * Builds the response page sent to the right iframe: a visible
     * topic-values table, followed by a small script that refreshes
     * the centre iframe with the current graph.
     */
    private static String renderTablePage() {
        String tableHtml = renderTable();

        Graph graph = new Graph();
        graph.createFromTopics();
        List<String> graphLines = HtmlGraphWriter.getGraphHTML(graph);
        StringBuilder joined = new StringBuilder();
        for (int i = 0; i < graphLines.size(); i++) {
            joined.append(graphLines.get(i));
            if (i < graphLines.size() - 1) joined.append('\n');
        }

        StringBuilder page = new StringBuilder();
        page.append(tableHtml);
        page.append("<script>\n");
        page.append("  try {\n");
        page.append("    const center = parent && parent.document\n");
        page.append("      ? parent.document.getElementsByName('centerFrame')[0]\n");
        page.append("      : null;\n");
        page.append("    if (center) center.srcdoc = ").append(jsString(joined.toString())).append(";\n");
        page.append("  } catch (e) { /* cross-frame access blocked; ignore */ }\n");
        page.append("</script>\n");
        return page.toString();
    }

    /** Builds the HTML table of {topic name | latest value} rows. */
    private static String renderTable() {
        Collection<Topic> topics = TopicManagerSingleton.get().getTopics();

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"UTF-8\">");
        html.append("<title>Topics</title>");
        html.append("<link href=\"https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=JetBrains+Mono:wght@500;700&display=swap\" rel=\"stylesheet\">");
        html.append("<style>");
        html.append(":root{--bg:#0f172a;--panel:#1e293b;--border:rgba(255,255,255,0.08);--text-1:#f1f5f9;--text-2:#94a3b8;--accent:#38bdf8;}");
        html.append("body{font-family:'Inter',-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;padding:16px;background:var(--bg);color:var(--text-1);font-size:13px;margin:0;}");
        html.append("h2{margin:0 0 12px 0;font-size:11px;text-transform:uppercase;letter-spacing:1.4px;color:var(--text-2);display:flex;align-items:center;gap:8px;}");
        html.append("h2 .pill{width:6px;height:6px;border-radius:50%;background:var(--accent);box-shadow:0 0 8px var(--accent);}");
        html.append("table{border-collapse:separate;border-spacing:0;width:100%;background:var(--panel);border:1px solid var(--border);border-radius:10px;overflow:hidden;box-shadow:0 2px 12px rgba(0,0,0,0.3);}");
        html.append("th,td{padding:10px 12px;text-align:left;border-bottom:1px solid var(--border);}");
        html.append("th{background:rgba(255,255,255,0.03);font-weight:600;color:var(--text-2);font-size:10px;text-transform:uppercase;letter-spacing:0.8px;}");
        html.append("tr:last-child td{border-bottom:none;}");
        html.append("tr:hover td{background:rgba(56,189,248,0.05);}");
        html.append("td.name{font-weight:600;}");
        html.append("td.value{font-family:'JetBrains Mono','SF Mono',Menlo,monospace;color:var(--accent);font-weight:700;text-align:right;animation:slideIn 0.4s ease-out;}");
        html.append("td.value.empty{color:var(--text-2);font-weight:400;}");
        html.append("@keyframes slideIn{from{opacity:0;transform:translateX(-6px);}to{opacity:1;transform:translateX(0);}}");
        html.append(".empty-state{color:var(--text-2);padding:20px;text-align:center;background:var(--panel);border:1px solid var(--border);border-radius:10px;}");
        html.append(".count{font-size:10px;color:var(--text-2);margin-left:6px;font-family:'JetBrains Mono',monospace;}");
        html.append("</style></head><body>");
        html.append("<h2><span class=\"pill\"></span>Topic values <span class=\"count\">")
            .append(topics.size()).append("</span></h2>");

        if (topics.isEmpty()) {
            html.append("<div class=\"empty-state\">No topics yet.<br><span style=\"font-size:11px;\">Deploy a config to populate this table.</span></div>");
        } else {
            html.append("<table><thead><tr><th>Topic</th><th style=\"text-align:right;\">Latest value</th></tr></thead><tbody>");
            for (Topic t : topics) {
                Message last = t.getLastMessage();
                boolean hasValue = last != null;
                String shown = hasValue ? escape(formatValue(last)) : "&mdash;";
                html.append("<tr><td class=\"name\">").append(escape(t.name)).append("</td>")
                    .append("<td class=\"value").append(hasValue ? "" : " empty").append("\">")
                    .append(shown).append("</td></tr>");
            }
            html.append("</tbody></table>");
        }
        html.append("</body></html>");
        return html.toString();
    }

    /** Prefer the numeric form when valid, otherwise fall back to text. */
    private static String formatValue(Message m) {
        if (!Double.isNaN(m.asDouble)) {
            double d = m.asDouble;
            if (d == Math.floor(d) && !Double.isInfinite(d)) {
                return Long.toString((long) d);
            }
            return Double.toString(d);
        }
        return m.asText == null ? "" : m.asText;
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    /**
     * Encodes an arbitrary string as a JavaScript string literal. Quotes
     * are escaped, line/control characters are unicode-escaped, and
     * {@code <}/{@code >}/{@code &} are unicode-escaped too so an
     * embedded {@code </script>} can't break out of the wrapping tag.
     */
    private static String jsString(String s) {
        if (s == null) return "\"\"";
        StringBuilder sb = new StringBuilder(s.length() + 16);
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                case '<':  sb.append("\\u003c"); break;
                case '>':  sb.append("\\u003e"); break;
                case '&':  sb.append("\\u0026"); break;
                default:
                    int code = (int) c;
                    if (code < 0x20 || code == 0x2028 || code == 0x2029) {
                        sb.append(String.format("\\u%04x", code));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
        return sb.toString();
    }
}
