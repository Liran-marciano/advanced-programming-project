package project_biu.graph;

import java.util.ArrayList;

/**
 * A named broadcast channel that agents can subscribe to or publish through.
 *
 * <p>When a {@link Message} is {@link #publish(Message) published} the topic
 * invokes {@link Agent#callback(String, Message)} on every subscriber.
 *
 * <p>The constructor is intentionally package-private so that only
 * {@code TopicManagerSingleton}'s {@code TopicManager} can create new
 * instances — clients must obtain topics through that single entry point.
 */
public class Topic {

    /** The topic's name (its unique identifier across the system). */
    public final String name;

    /** Agents that subscribed to this topic. */
    public final ArrayList<Agent> subs = new ArrayList<>();

    /** Agents that may publish to this topic. */
    public final ArrayList<Agent> pubs = new ArrayList<>();

    Topic(String name) {
        this.name = name;
    }

    public void subscribe(Agent a) {
        subs.add(a);
    }

    public void unsubscribe(Agent a) {
        subs.remove(a);
    }

    public void publish(Message m) {
        // Iterate a snapshot so callbacks may (un)subscribe during dispatch
        // without triggering ConcurrentModificationException.
        for (Agent a : new ArrayList<>(subs)) {
            a.callback(name, m);
        }
    }

    public void addPublisher(Agent a) {
        pubs.add(a);
    }

    public void removePublisher(Agent a) {
        pubs.remove(a);
    }
}
