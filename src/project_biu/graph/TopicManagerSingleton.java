package project_biu.graph;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The single registry of every {@link Topic} in the system.
 *
 * <p>The implementation uses the lazy inner-class idiom (Bill Pugh singleton):
 * the inner {@code TopicManager} class is loaded only on the first call to
 * {@link #get()}, and the JVM's own class-loading guarantees give us a
 * thread-safe, lazily-initialised singleton without {@code synchronized} blocks
 * or double-checked locking.
 *
 * <p>Topics are kept in a flyweight: ask for a topic by name and you either
 * get the existing one or a freshly created one that is then cached forever.
 */
public class TopicManagerSingleton {

    /** Entry point for clients. Returns the one and only {@link TopicManager}. */
    public static TopicManager get() {
        return TopicManager.instance;
    }

    /**
     * The actual manager. Kept package-private so that only classes in
     * {@code project_biu.graph} can call the package-private {@link Topic}
     * constructor (it does the {@code new Topic(...)} for us).
     */
    public static class TopicManager {

        private static final TopicManager instance = new TopicManager();

        private final ConcurrentHashMap<String, Topic> topics = new ConcurrentHashMap<>();

        private TopicManager() {
        }

        /**
         * Returns the topic with the given name, creating it on first lookup.
         * Atomic with respect to concurrent callers thanks to
         * {@link ConcurrentHashMap#computeIfAbsent}.
         */
        public Topic getTopic(String name) {
            return topics.computeIfAbsent(name, Topic::new);
        }

        /** Read-only view of every topic currently registered. */
        public Collection<Topic> getTopics() {
            return topics.values();
        }

        /** Drops every topic. Mainly useful between tests. */
        public void clear() {
            topics.clear();
        }
    }
}
