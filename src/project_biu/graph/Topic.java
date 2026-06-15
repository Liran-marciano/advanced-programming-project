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

    /**
     * The most recent message that flowed through this topic, or {@code null}
     * if nothing has been published yet. Used by the Ex 6 view layer to
     * render a "topic name | last value" table without having to subscribe
     * a snooping agent to every topic.
     */
    private volatile Message lastMessage;

    Topic(String name) {
        this.name = name;
    }

    /**
     * Adds an agent as a subscriber.
     * @param a agent to add as a subscriber
     */
    public void subscribe(Agent a) {
        subs.add(a);
    }

    /**
     * Removes an agent from subscribers.
     * @param a agent to remove from subscribers
     */
    public void unsubscribe(Agent a) {
        subs.remove(a);
    }

    /**
     * Broadcasts a message to every subscriber.
     * @param m the message to broadcast to all subscribers
     */
    public void publish(Message m) {
        this.lastMessage = m;
        // Iterate a snapshot so callbacks may (un)subscribe during dispatch
        // without triggering ConcurrentModificationException.
        for (Agent a : new ArrayList<>(subs)) {
            a.callback(name, m);
        }
    }

    /**
     * Latest message published through this topic, or {@code null} if none yet.
     *
     * @return the most recently published message, or {@code null}
     */
    public Message getLastMessage() {
        return lastMessage;
    }

    /**
     * Forgets the most recently published message so {@link #getLastMessage()}
     * reverts to {@code null}. Subscriptions and publishers are untouched, so
     * the topology survives — only the cached value goes away. Used by the
     * Ex 6 view to clear the table without tearing down the graph.
     */
    public void clearLastMessage() {
        this.lastMessage = null;
    }

    /**
     * Registers an agent as a publisher of this topic.
     * @param a agent to register as a publisher of this topic
     */
    public void addPublisher(Agent a) {
        pubs.add(a);
    }

    /**
     * Unregisters a publisher agent.
     * @param a publisher agent to unregister
     */
    public void removePublisher(Agent a) {
        pubs.remove(a);
    }
}
