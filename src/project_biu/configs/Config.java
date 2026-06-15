package project_biu.configs;

/**
 * A bundle that knows how to instantiate a particular set of agents and
 * topic subscriptions, i.e. a particular computational graph shape.
 *
 * <p>Implementations call {@code TopicManagerSingleton} to create their
 * agents in {@link #create()}; later exercises will gain a {@code close()}
 * method to dispose of those agents cleanly.
 */
public interface Config {

    /** Build the agents and subscriptions that this config represents. */
    void create();

    /**
     * Human-readable name for the config.
     *
     * @return the config's display name
     */
    String getName();

    /**
     * Version stamp (useful when a config evolves over time).
     *
     * @return the config's version number
     */
    int getVersion();

    /** Dispose of every agent created by {@link #create()}. */
    void close();
}
