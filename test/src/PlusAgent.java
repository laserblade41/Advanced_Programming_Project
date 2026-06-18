package test;

import test.TopicManagerSingleton.TopicManager;

public class PlusAgent implements Agent {

    private final String[] subs;
    private final String[] pubs;

    private double x = 0;
    private double y = 0;
    private boolean hasX = false;
    private boolean hasY = false;

    public PlusAgent(String[] subs, String[] pubs) {
        this.subs = subs;
        this.pubs = pubs;
    }

    public PlusAgent(String in1, String in2, String out) {
        this(new String[]{in1, in2}, new String[]{out});
    }

    @Override
    public String getName() {
        return "PlusAgent";
    }

    @Override
    public synchronized void reset() {
        x = 0;
        y = 0;
        hasX = false;
        hasY = false;
    }

    @Override
    public void close() {
    }

    @Override
    public synchronized void callback(String topic, Message msg) {
        if (subs == null || subs.length < 2) {
            return;
        }

        double v = msg.asDouble;
        if (topic.equals(subs[0])) {
            x = v;
            hasX = true;
        } else if (topic.equals(subs[1])) {
            y = v;
            hasY = true;
        } else {
            return;
        }

        if (hasX && hasY && !Double.isNaN(x) && !Double.isNaN(y)) {
            if (pubs != null && pubs.length >= 1) {
                TopicManager tm = TopicManagerSingleton.get();
                tm.getTopic(pubs[0]).publish(new Message(x + y));
            }
        }
    }
}
