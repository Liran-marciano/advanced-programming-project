package project_biu.graph;

/**
 * A software agent that reacts to messages published on {@link Topic}s.
 *
 * <p>Agents can subscribe to topics in order to receive their messages through
 * {@link #callback(String, Message)} and can publish to topics in order to
 * notify other agents downstream. This is the observer half of the
 * publish/subscribe pattern that drives the computational graph.
 */
public interface Agent {

    /** Returns the agent's name (useful for graph diagrams and debugging). */
    String getName();

    /** Resets the agent's internal state. */
    void reset();

    /**
     * Invoked when a {@link Message} is published to a topic this agent
     * subscribed to.
     *
     * @param topic the name of the topic the message arrived on
     * @param msg   the message itself
     */
    void callback(String topic, Message msg);

    /** Releases resources held by the agent. */
    void close();
}
