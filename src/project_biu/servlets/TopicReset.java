package project_biu.servlets;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Set;

import project_biu.graph.Agent;
import project_biu.graph.Topic;
import project_biu.graph.TopicManagerSingleton;
import project_biu.server.RequestParser.RequestInfo;

/**
 * Servlet bound to {@code GET /reset}.
 *
 * <p>Clears the {@link Topic#getLastMessage() last value} of every topic
 * without touching the topology &mdash; subscriptions, publishers and
 * agent threads stay alive. The user then sees the table and graph
 * reset to their empty state, but doesn't need to re-upload the config.
 *
 * <p>The response itself is delegated to {@link TopicDisplayer}: after
 * the clear, the same topic-table page (with its centre-frame refresh
 * script) is sent back, so both the table and the graph re-render with
 * empty values in a single round-trip.
 */
public class TopicReset implements Servlet {

    /** Constructs a new {@code TopicReset}. */
    public TopicReset() { /* no setup needed */ }

    private final TopicDisplayer delegate = new TopicDisplayer();

    @Override
    public void handle(RequestInfo ri, OutputStream toClient) throws IOException {
        // 1. Clear every topic's cached "latest value" so the table / graph
        //    re-render with dashes instead of stale numbers.
        // 2. Walk every agent reachable through the topics (de-duplicated
        //    via identity), and call reset() on each one. Without this,
        //    agents like PlusAgent would keep their internal x/y, leading
        //    to bogus values on the next publish (e.g. A=99 -> Sum=104
        //    because PlusAgent still held y=5 from before).
        Set<Agent> seen = Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        for (Topic t : TopicManagerSingleton.get().getTopics()) {
            t.clearLastMessage();
            for (Agent a : t.subs) if (seen.add(a)) a.reset();
            for (Agent a : t.pubs) if (seen.add(a)) a.reset();
        }
        // Hand off to TopicDisplayer to render the now-empty table and
        // trigger the centre-frame graph refresh. No duplication.
        delegate.handle(ri, toClient);
    }

    @Override
    public void close() {
        delegate.close();
    }
}
