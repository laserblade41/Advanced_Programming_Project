package graph;

import java.util.concurrent.ConcurrentHashMap;

public class TopicManagerSingleton {

    public static TopicManager get() {
        return TopicManager.instance;
    }
    public static class TopicManager{
        private static final TopicManager instance = new TopicManager();
        ConcurrentHashMap<String, Topic> topics = new ConcurrentHashMap<>();
        private TopicManager() {

        }
        public Topic getTopic(String name) {
            // if topic exists return it, otherwise create a new one and return it
            return topics.computeIfAbsent(name, Topic::new);
        }
        public ConcurrentHashMap<String, Topic> getAllTopics() {
            return topics;
        }
        void clear() {
            topics.clear();
        }
    }
}
