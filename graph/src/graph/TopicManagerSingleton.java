package graph;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Provides global access to the singleton {@link TopicManager} registry.
 *
 * <p><strong>Design pattern - Thread-Safe Singleton (initialization-on-demand holder):</strong>
 * the single instance lives in the static inner class {@link TopicManager} as a
 * {@code private static final} field. The JVM guarantees a class is loaded and its static
 * initializers run exactly once, lazily, and in a thread-safe manner the first time the class
 * is referenced (here, the first call to {@link #get()}). This gives us lazy initialization
 * and full thread safety <em>without</em> any {@code synchronized} blocks or double-checked
 * locking.</p>
 *
 * <p>All topics in the computational graph are managed through this singleton. External
 * code obtains the manager via {@link #get()} and creates or retrieves topics by name.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * Topic topic = TopicManagerSingleton.get().getTopic("A");
 * topic.publish(new Message(5.0));
 * }</pre>
 *
 * @see Topic
 * @see TopicManagerSingleton.TopicManager
 */
public class TopicManagerSingleton {

    /**
     * Returns the singleton {@link TopicManager} instance.
     *
     * @return the global topic manager
     */
    public static TopicManager get() {
        return TopicManager.instance;
    }

    /**
     * Thread-safe registry of all {@link Topic} instances in the system.
     *
     * <p>Topics are stored in a {@link ConcurrentHashMap}, allowing safe concurrent
     * creation and lookup from multiple HTTP request threads or agent callbacks.</p>
     */
    public static class TopicManager{
        // The lone instance. Being static final, it is created once during class loading;
        // the JVM's class-init lock makes this publication safe across all threads.
        private static final TopicManager instance = new TopicManager();
        // ConcurrentHashMap lets many HTTP/agent threads create and look up topics at once
        // without external locking.
        ConcurrentHashMap<String, Topic> topics = new ConcurrentHashMap<>();
        // Private constructor prevents any other code from creating a second manager.
        private TopicManager() {

        }

        /**
         * Returns the topic with the given name, creating it if it does not yet exist.
         *
         * @param name the unique topic name
         * @return the existing or newly created {@link Topic}
         */
        public Topic getTopic(String name) {
            // computeIfAbsent performs an atomic get-or-create: if two threads ask for the
            // same new topic concurrently, exactly one Topic is constructed and shared,
            // avoiding duplicate channels in the graph.
            return topics.computeIfAbsent(name, Topic::new);
        }

        /**
         * Returns the live map of all registered topics.
         *
         * @return the {@link ConcurrentHashMap} of topic names to {@link Topic} instances;
         *         modifications to the map affect the global registry
         */
        public ConcurrentHashMap<String, Topic> getAllTopics() {
            return topics;
        }
        void clear() {
            topics.clear();
        }
    }
}
