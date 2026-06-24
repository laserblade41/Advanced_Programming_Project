package configs;

import graph.*;
import graph.TopicManagerSingleton.TopicManager;

/**
 * Agent that increments a numeric input by 1 and publishes the result.
 *
 * <p>Subscribes to the first input topic in {@code subs} and publishes
 * {@code input + 1} to the first output topic in {@code pubs} when a numeric
 * message is received.</p>
 *
 * <p><strong>Thread safety:</strong> {@link #callback} is {@code synchronized}.</p>
 *
 * @see Agent
 * @see PlusAgent
 */
public class IncAgent implements Agent {

    private final String[] subs;
    private final String[] pubs;

    /**
     * Creates an increment agent with the given subscription and publication topics.
     *
     * @param subs array of input topic names; only {@code subs[0]} is used
     * @param pubs array of output topic names; only {@code pubs[0]} is used
     */
    public IncAgent(String[] subs, String[] pubs) {
        this.subs = subs;
        this.pubs = pubs;
    }

    /**
     * Convenience constructor for a single-input, single-output increment agent.
     *
     * @param in the input topic name
     * @param out the output topic name
     */
    public IncAgent(String in, String out) {
        this(new String[]{in}, new String[]{out});
    }

    /**
     * Returns the agent name.
     *
     * @return {@code "IncAgent"}
     */
    @Override
    public String getName() {
        return "IncAgent";
    }

    /**
     * Resets internal state (no-op for this agent).
     */
    @Override
    public void reset() {
    }

    /**
     * Releases resources (no-op for this agent).
     */
    @Override
    public void close() {
    }

    /**
     * Handles an incoming message by publishing {@code value + 1} to the output topic.
     *
     * <p>Only processes messages on {@code subs[0]} with a valid numeric value.</p>
     *
     * @param topic the topic that published the message
     * @param msg the incoming message
     */
    // synchronized for safe publication of state/results even if invoked from multiple threads.
    @Override
    public synchronized void callback(String topic, Message msg) {
        // Single-input agent: only react to messages on its one subscribed topic.
        if (subs == null || subs.length < 1 || !topic.equals(subs[0])) {
            return;
        }
        // Guard against non-numeric payloads; only publish input+1 for valid numbers.
        double v = msg.asDouble;
        if (!Double.isNaN(v)) {
            if (pubs != null && pubs.length >= 1) {
                TopicManager tm = TopicManagerSingleton.get();
                tm.getTopic(pubs[0]).publish(new Message(v + 1));
            }
        }
    }
}
