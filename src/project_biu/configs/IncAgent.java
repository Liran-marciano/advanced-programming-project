package project_biu.configs;

import project_biu.graph.Agent;
import project_biu.graph.Message;
import project_biu.graph.TopicManagerSingleton;
import project_biu.graph.TopicManagerSingleton.TopicManager;

/**
 * Single-input increment agent used by {@code GenericConfig}.
 *
 * <p>Subscribes to {@code subs[0]} and, on every numeric message it receives,
 * publishes the input value plus one to {@code pubs[0]}.
 *
 * <p>Like {@link PlusAgent}, it offers the {@code (String[], String[])}
 * constructor that {@code GenericConfig} requires when loading the class
 * reflectively from a config file.
 */
public class IncAgent implements Agent {

    private final String[] subs;
    private final String[] pubs;

    public IncAgent(String[] subs, String[] pubs) {
        this.subs = subs;
        this.pubs = pubs;

        TopicManager tm = TopicManagerSingleton.get();
        if (subs.length > 0) tm.getTopic(subs[0]).subscribe(this);
        if (pubs.length > 0) tm.getTopic(pubs[0]).addPublisher(this);
    }

    @Override
    public String getName() {
        return "IncAgent";
    }

    @Override
    public void reset() {
        // No state to reset.
    }

    @Override
    public void callback(String topic, Message msg) {
        if (Double.isNaN(msg.asDouble)) return;
        if (subs.length == 0 || !topic.equals(subs[0])) return;
        if (pubs.length > 0) {
            TopicManagerSingleton.get().getTopic(pubs[0]).publish(new Message(msg.asDouble + 1));
        }
    }

    @Override
    public void close() {
        // No resources to release.
    }
}
