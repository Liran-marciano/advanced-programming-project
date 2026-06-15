package project_biu.configs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import project_biu.graph.Agent;
import project_biu.graph.Topic;
import project_biu.graph.TopicManagerSingleton;

/**
 * The directed graph that mirrors a publish/subscribe topology.
 *
 * <p>A {@code Graph} is just an {@link ArrayList} of {@link Node}s, so the
 * standard list operations work on it directly. The interesting operations
 * are {@link #hasCycles()} (does the topology contain any cycle?) and
 * {@link #createFromTopics()} (populate from the live {@code TopicManager}).
 */
public class Graph extends ArrayList<Node> {

    /**
     * Returns {@code true} if any node belongs to a strongly-connected
     * component that contains a cycle.
     */
    public boolean hasCycles() {
        for (Node n : this) {
            if (n.hasCycles()) return true;
        }
        return false;
    }

    /**
     * Populates the graph from the {@code TopicManager}'s current registry.
     *
     * <ul>
     *   <li>Every {@link Topic} becomes a node named {@code "T<topicName>"},
     *       with edges to its subscribed agents.</li>
     *   <li>Every {@link Agent} becomes a node named {@code "A<agentName>"},
     *       with edges to the topics it may publish to.</li>
     * </ul>
     *
     * <p>Nodes are de-duplicated by name, so an agent that participates in
     * multiple topics still appears as a single vertex.
     */
    public void createFromTopics() {
        // The test in MainTrain calls this twice on the same Graph instance,
        // so we must start from a clean slate every time.
        this.clear();

        Map<String, Node> byName = new HashMap<>();

        for (Topic t : TopicManagerSingleton.get().getTopics()) {
            Node topicNode = byName.computeIfAbsent("T" + t.name, Node::new);

            // Topic -> each subscribed agent
            for (Agent sub : t.subs) {
                Node agentNode = byName.computeIfAbsent("A" + sub.getName(), Node::new);
                topicNode.addEdge(agentNode);
            }

            // Each publishing agent -> Topic
            for (Agent pub : t.pubs) {
                Node agentNode = byName.computeIfAbsent("A" + pub.getName(), Node::new);
                agentNode.addEdge(topicNode);
            }
        }

        this.addAll(byName.values());
    }
}
