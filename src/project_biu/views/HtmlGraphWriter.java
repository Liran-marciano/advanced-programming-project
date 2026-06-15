package project_biu.views;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import project_biu.configs.Graph;
import project_biu.configs.Node;
import project_biu.graph.Message;
import project_biu.graph.Topic;
import project_biu.graph.TopicManagerSingleton;

/**
 * Generates the browser-facing HTML view for a {@link Graph}.
 *
 * <p>Per the Ex 6 spec, this class is the only place that produces the
 * graph view, and it does so by loading the static {@code graph.html}
 * template and substituting in SVG fragments for the nodes and edges
 * &mdash; the HTML/CSS itself is never hand-built in Java. That keeps
 * every visual tweak (gradients, animations, typography) in the
 * designer's editor instead of in this file.
 *
 * <p>Layout: nodes are placed topologically &mdash; sources (no incoming
 * edges) on the left, their successors in the next column, and so on.
 * Within each column nodes are spaced evenly along the vertical axis.
 * Topics render as rounded rectangles, agents as circles. Each topic
 * gets a role-based gradient class ({@code input}, {@code intermediate}
 * or {@code output}) so the CSS in the template can paint them
 * differently.
 */
public final class HtmlGraphWriter {

    /** Path of the static template, relative to the working directory. */
    private static final Path TEMPLATE_PATH = Paths.get("html_files", "graph.html");

    /** Base canvas size when the graph has up to {@code BASE_COLUMNS} columns. */
    private static final int BASE_WIDTH   = 760;
    private static final int HEIGHT       = 500;
    private static final int BASE_COLUMNS = 5;
    /** Extra horizontal room each extra column gets so edges stay visible. */
    private static final int COLUMN_STRIDE = 110;

    /** Inner padding so nodes never touch the SVG edge. */
    private static final int PAD_X = 90;
    private static final int PAD_Y = 70;

    /** Half-dimensions of a topic rectangle. */
    private static final int RECT_HW = 36;
    private static final int RECT_HH = 18;
    /** Radius of an agent circle (sized to fit labels like "Plus[Total]"). */
    private static final int CIRCLE_R = 36;

    private HtmlGraphWriter() { /* no instances */ }

    /**
     * Returns the HTML for visualising {@code g}, line-by-line so callers
     * can stream the result to an {@link java.io.OutputStream}.
     *
     * @param graph the graph to render; nodes whose names start with
     *              {@code "T"} are drawn as topics, those starting with
     *              {@code "A"} as agents (matching the convention from
     *              {@link Graph#createFromTopics()})
     * @return the rendered HTML split on line boundaries
     */
    public static List<String> getGraphHTML(Graph graph) {
        String template = readTemplate();

        Map<Node, Integer> level   = computeLevels(graph);
        int maxLevel = level.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        int width    = widthFor(maxLevel + 1);

        Map<Node, int[]> positions = layout(graph, level, maxLevel, width);
        String nodesSvg = buildNodes(graph, positions, level, maxLevel);
        String edgesSvg = buildEdges(graph, positions);

        String rendered = template
                .replace("{{SVG_WIDTH}}",  Integer.toString(width))
                .replace("{{SVG_HEIGHT}}", Integer.toString(HEIGHT))
                .replace("{{NODES}}", nodesSvg)
                .replace("{{EDGES}}", edgesSvg);

        return new ArrayList<>(Arrays.asList(rendered.split("\\r?\\n", -1)));
    }

    /**
     * Canvas width scales with column count so long pipelines (e.g. {@code
     * chain.conf}'s 7 columns) still leave enough room for visible edges
     * between adjacent middle-row nodes.
     */
    private static int widthFor(int columns) {
        if (columns <= BASE_COLUMNS) return BASE_WIDTH;
        return BASE_WIDTH + (columns - BASE_COLUMNS) * COLUMN_STRIDE;
    }

    // ---------------------------------------------------------------- template

    private static String readTemplate() {
        try {
            return new String(Files.readAllBytes(TEMPLATE_PATH), StandardCharsets.UTF_8);
        } catch (IOException e) {
            // Fallback minimal template so the view still works even if
            // html_files/graph.html was deleted. Losing the file shouldn't
            // crash the server.
            return "<!DOCTYPE html><html><body>"
                 + "<svg width=\"{{SVG_WIDTH}}\" height=\"{{SVG_HEIGHT}}\">"
                 + "{{EDGES}}{{NODES}}"
                 + "</svg></body></html>";
        }
    }

    // ------------------------------------------------------------------ layout

    /**
     * Topological layering: column index = longest path from a source node,
     * row index = position within the column. The result is a left-to-right
     * reading of the dataflow.
     */
    private static Map<Node, int[]> layout(Graph graph, Map<Node, Integer> level,
                                           int maxLevel, int width) {
        Map<Integer, List<Node>> byLevel = new LinkedHashMap<>();
        for (int i = 0; i <= maxLevel; i++) byLevel.put(i, new ArrayList<>());
        for (Node n : graph) byLevel.get(level.get(n)).add(n);

        Map<Node, int[]> positions = new HashMap<>();
        int columns = maxLevel + 1;
        int usableW = width  - 2 * PAD_X;
        int usableH = HEIGHT - 2 * PAD_Y;

        for (int col = 0; col < columns; col++) {
            List<Node> nodes = byLevel.get(col);
            int rows = nodes.size();
            int x = (columns == 1)
                    ? width / 2
                    : PAD_X + (int) Math.round(usableW * col / (double) (columns - 1));
            for (int row = 0; row < rows; row++) {
                int y = (rows == 1)
                        ? HEIGHT / 2
                        : PAD_Y + (int) Math.round(usableH * row / (double) (rows - 1));
                positions.put(nodes.get(row), new int[]{x, y});
            }
        }
        return positions;
    }

    /**
     * Assigns each node a level = longest path length from any source
     * (in-degree-zero) node. Implemented as Kahn's topological sort: a
     * node is enqueued only when its <em>last</em> predecessor has been
     * processed, so by the time we relax its outgoing edges the node's
     * own level is final. Cycle members never reach in-degree 0 and fall
     * back to level 0 below &mdash; the graph still renders, just not
     * strictly layered.
     */
    private static Map<Node, Integer> computeLevels(Graph graph) {
        // remaining[n] = in-edges still to be resolved before n can be processed.
        Map<Node, Integer> remaining = new HashMap<>();
        for (Node n : graph) remaining.put(n, 0);
        for (Node n : graph) {
            for (Node e : n.getEdges()) {
                remaining.merge(e, 1, Integer::sum);
            }
        }

        Map<Node, Integer> level = new HashMap<>();
        Deque<Node> queue = new ArrayDeque<>();
        for (Map.Entry<Node, Integer> entry : remaining.entrySet()) {
            if (entry.getValue() == 0) {
                level.put(entry.getKey(), 0);
                queue.add(entry.getKey());
            }
        }

        while (!queue.isEmpty()) {
            Node n = queue.poll();
            int nl = level.get(n);
            for (Node e : n.getEdges()) {
                int proposed = nl + 1;
                if (proposed > level.getOrDefault(e, -1)) {
                    level.put(e, proposed);
                }
                int r = remaining.get(e) - 1;
                remaining.put(e, r);
                if (r == 0) {
                    queue.add(e);
                }
            }
        }

        // Cycle members never reach remaining=0; pin them to level 0 so
        // they still get a column.
        for (Node n : graph) level.putIfAbsent(n, 0);
        return level;
    }

    /**
     * Returns one of {@code input}, {@code intermediate}, {@code output}
     * for a topic node. Agents always return {@code intermediate} (the
     * caller skips this for agents anyway).
     */
    private static String topicRole(Node topic, Map<Node, Integer> level, int maxLevel) {
        int lvl = level.getOrDefault(topic, 0);
        if (lvl == 0)                 return "input";
        if (lvl >= maxLevel)          return "output";
        return "intermediate";
    }

    // ------------------------------------------------------------------ nodes

    private static String buildNodes(Graph graph, Map<Node, int[]> positions,
                                     Map<Node, Integer> level, int maxLevel) {
        StringBuilder sb = new StringBuilder();
        for (Node n : graph) {
            int[] xy = positions.get(n);
            int x = xy[0], y = xy[1];
            String full  = n.getName();
            String label = full.length() > 1 ? full.substring(1) : full;

            if (full.startsWith("T")) {
                String role = topicRole(n, level, maxLevel);
                sb.append("        <rect class=\"topic-rect ").append(role).append("\" ")
                  .append("x=\"").append(x - RECT_HW).append("\" ")
                  .append("y=\"").append(y - RECT_HH).append("\" ")
                  .append("width=\"").append(RECT_HW * 2).append("\" ")
                  .append("height=\"").append(RECT_HH * 2).append("\" ")
                  .append("rx=\"6\"/>\n");

                sb.append("        <text class=\"node-label\" x=\"").append(x)
                  .append("\" y=\"").append(y).append("\">")
                  .append(escape(label)).append("</text>\n");

                // Floating value label above the rectangle, with a subtle
                // glowing pill behind it so it pops on the dark canvas.
                String value = lookupLatestValue(label);
                if (value != null) {
                    int padX = Math.max(14, 6 + 7 * value.length() / 2);
                    int by = y - RECT_HH - 26;
                    sb.append("        <rect class=\"value-bg\" ")
                      .append("x=\"").append(x - padX).append("\" ")
                      .append("y=\"").append(by).append("\" ")
                      .append("width=\"").append(padX * 2).append("\" ")
                      .append("height=\"20\" rx=\"10\"/>\n");
                    sb.append("        <text class=\"value-label\" x=\"").append(x)
                      .append("\" y=\"").append(by + 14).append("\">")
                      .append(escape(value)).append("</text>\n");
                }
            } else {
                sb.append("        <circle class=\"agent-circ\" cx=\"").append(x)
                  .append("\" cy=\"").append(y)
                  .append("\" r=\"").append(CIRCLE_R).append("\"/>\n");
                sb.append("        <text class=\"node-label agent-label\" x=\"").append(x)
                  .append("\" y=\"").append(y).append("\">")
                  .append(escape(prettyAgentLabel(label))).append("</text>\n");
            }
        }
        return sb.toString();
    }

    /**
     * Trim a node name to something that fits inside an agent circle.
     * Strips a leading package prefix if any survived, then keeps the
     * first ~14 characters so labels like {@code "Plus[Total]"} stay
     * readable but {@code "VeryLongAgentName[X]"} doesn't overflow.
     */
    private static String prettyAgentLabel(String label) {
        if (label == null) return "";
        int dot = label.lastIndexOf('.');
        String shortName = (dot >= 0) ? label.substring(dot + 1) : label;
        if (shortName.length() > 14) shortName = shortName.substring(0, 13) + "…";
        return shortName;
    }

    /**
     * Look up the latest value of a topic by name. Returns {@code null}
     * when no message has been published yet (so the caller can skip
     * the label entirely rather than show "null").
     */
    private static String lookupLatestValue(String topicName) {
        for (Topic t : TopicManagerSingleton.get().getTopics()) {
            if (t.name.equals(topicName)) {
                Message m = t.getLastMessage();
                if (m == null) return null;
                if (!Double.isNaN(m.asDouble)) {
                    if (m.asDouble == Math.floor(m.asDouble) && !Double.isInfinite(m.asDouble)) {
                        return Long.toString((long) m.asDouble);
                    }
                    return Double.toString(m.asDouble);
                }
                return m.asText;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ edges

    /**
     * Curved bezier edges that land on the node boundary (so the arrowhead
     * doesn't bury itself in the centre). The control points lift the
     * curve out horizontally, which reads well for a left-to-right layered
     * layout but still looks OK for vertical / diagonal arrangements.
     */
    private static String buildEdges(Graph graph, Map<Node, int[]> positions) {
        StringBuilder sb = new StringBuilder();
        for (Node from : graph) {
            int[] f = positions.get(from);
            for (Node to : from.getEdges()) {
                int[] t = positions.get(to);
                if (f == null || t == null) continue;

                int[] start = boundaryPoint(from, f, t);
                int[] end   = boundaryPoint(to,   t, f);

                double dx = end[0] - start[0];
                double cpOffset = Math.max(40, Math.abs(dx) * 0.45);

                int c1x = (int) Math.round(start[0] + cpOffset);
                int c1y = start[1];
                int c2x = (int) Math.round(end[0]   - cpOffset);
                int c2y = end[1];

                sb.append("        <path class=\"edge\" d=\"M ")
                  .append(start[0]).append(' ').append(start[1])
                  .append(" C ").append(c1x).append(' ').append(c1y)
                  .append(", ").append(c2x).append(' ').append(c2y)
                  .append(", ").append(end[0]).append(' ').append(end[1])
                  .append("\" marker-end=\"url(#arrow)\"/>\n");
            }
        }
        return sb.toString();
    }

    /**
     * Pulls the endpoint of an edge from the centre of a node out to its
     * visible boundary, so the arrowhead lands on the edge of the rect /
     * circle instead of being buried in the middle.
     */
    private static int[] boundaryPoint(Node node, int[] selfPos, int[] otherPos) {
        double dx = otherPos[0] - selfPos[0];
        double dy = otherPos[1] - selfPos[1];
        double len = Math.hypot(dx, dy);
        if (len < 1e-6) return new int[]{selfPos[0], selfPos[1]};
        if (node.getName().startsWith("T")) {
            // Rectangle: shrink along the direction by the half-width/height
            // that the line crosses first.
            double scale = Math.min(RECT_HW / Math.abs(dx), RECT_HH / Math.abs(dy));
            double offX = dx * scale;
            double offY = dy * scale;
            return new int[]{
                (int) Math.round(selfPos[0] + offX),
                (int) Math.round(selfPos[1] + offY)
            };
        }
        // Circle: trim by the radius.
        double k = CIRCLE_R / len;
        return new int[]{
            (int) Math.round(selfPos[0] + dx * k),
            (int) Math.round(selfPos[1] + dy * k)
        };
    }

    // ------------------------------------------------------------------ utils

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
