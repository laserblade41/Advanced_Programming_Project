package test;

import test.TopicManagerSingleton.TopicManager;

public class IncAgent implements Agent {

    private final String[] subs;
    private final String[] pubs;

    public IncAgent(String[] subs, String[] pubs) {
        this.subs = subs;
        this.pubs = pubs;
    }

    public IncAgent(String in, String out) {
        this(new String[]{in}, new String[]{out});
    }

    @Override
    public String getName() {
        return "IncAgent";
    }

    @Override
    public void reset() {
    }

    @Override
    public void close() {
    }

    @Override
    public synchronized void callback(String topic, Message msg) {
        if (subs == null || subs.length < 1 || !topic.equals(subs[0])) {
            return;
        }
        double v = msg.asDouble;
        if (!Double.isNaN(v)) {
            if (pubs != null && pubs.length >= 1) {
                TopicManager tm = TopicManagerSingleton.get();
                tm.getTopic(pubs[0]).publish(new Message(v + 1));
            }
        }
    }
}
