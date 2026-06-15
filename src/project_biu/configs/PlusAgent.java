package project_biu.configs;

import project_biu.graph.Agent;
import project_biu.graph.Message;
import project_biu.graph.TopicManagerSingleton;
import project_biu.graph.TopicManagerSingleton.TopicManager;

/**
 * Two-input addition agent used by {@code GenericConfig}.
 *
 * <p>Reads {@code subs[0]} and {@code subs[1]} from the topic manager, keeps
 * the latest numeric value of each, and publishes their sum to {@code pubs[0]}
 * whenever both inputs are valid numbers.
 *
 * <p>The {@code (String[], String[])} constructor signature is what
 * {@code GenericConfig} expects when it loads classes reflectively, so any
 * agent meant to be loaded from a config file must share this signature.
 */
public class PlusAgent implements Agent {

    private final String[] subs;
    private final String[] pubs;

    private double x = 0.0;
    private double y = 0.0;

    /**
     * Reflective constructor used by {@code GenericConfig}.
     *
     * @param subs input topics (uses {@code subs[0]} and {@code subs[1]})
     * @param pubs output topics (uses {@code pubs[0]})
     */
    public PlusAgent(String[] subs, String[] pubs) {
        this.subs = subs;
        this.pubs = pubs;

        TopicManager tm = TopicManagerSingleton.get();
        if (subs.length > 0) tm.getTopic(subs[0]).subscribe(this);
        if (subs.length > 1) tm.getTopic(subs[1]).subscribe(this);
        if (pubs.length > 0) tm.getTopic(pubs[0]).addPublisher(this);
    }

    @Override
    public String getName() {
        // Include the output topic so two PlusAgent instances in the same
        // config (e.g. one publishing "Sum", another "Total") get distinct
        // names. Without this, Graph.createFromTopics() would collapse them
        // into one node because it dedupes by getName().
        return (pubs != null && pubs.length > 0) ? "Plus[" + pubs[0] + "]" : "PlusAgent";
    }

    @Override
    public void reset() {
        x = 0.0;
        y = 0.0;
    }

    @Override
    public void callback(String topic, Message msg) {
        if (Double.isNaN(msg.asDouble)) return;

        if (subs.length > 0 && topic.equals(subs[0])) {
            x = msg.asDouble;
        } else if (subs.length > 1 && topic.equals(subs[1])) {
            y = msg.asDouble;
        } else {
            return;
        }

        if (pubs.length > 0) {
            TopicManagerSingleton.get().getTopic(pubs[0]).publish(new Message(x + y));
        }
    }

    @Override
    public void close() {
        // No resources to release.
    }
}
