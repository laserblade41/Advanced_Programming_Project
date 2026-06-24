package graph;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Provides global access to the singleton {@link TopicManager} registry.
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
        private static final TopicManager instance = new TopicManager();
        ConcurrentHashMap<String, Topic> topics = new ConcurrentHashMap<>();
        private TopicManager() {

        }

        /**
         * Returns the topic with the given name, creating it if it does not yet exist.
         *
         * @param name the unique topic name
         * @return the existing or newly created {@link Topic}
         */
        public Topic getTopic(String name) {
            // if topic exists return it, otherwise create a new one and return it
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
