package configs;

import graph.*;
import graph.TopicManagerSingleton.TopicManager;

/**
 * Agent that adds two numeric inputs and publishes the sum.
 *
 * <p>Subscribes to two input topics and publishes the sum to an output topic once
 * both inputs have received valid numeric values. Waits until both {@code subs[0]}
 * and {@code subs[1]} have published before computing.</p>
 *
 * <p><strong>Thread safety:</strong> {@link #callback} and {@link #reset} are
 * {@code synchronized}.</p>
 *
 * @see Agent
 * @see IncAgent
 * @see BinOpAgent
 */
public class PlusAgent implements Agent {

    private final String[] subs;
    private final String[] pubs;

    private double x = 0;
    private double y = 0;
    private boolean hasX = false;
    private boolean hasY = false;

    /**
     * Creates a plus agent with the given subscription and publication topics.
     *
     * @param subs array of two input topic names
     * @param pubs array of output topic names; only {@code pubs[0]} is used
     */
    public PlusAgent(String[] subs, String[] pubs) {
        this.subs = subs;
        this.pubs = pubs;
    }

    /**
     * Convenience constructor for a two-input, single-output addition agent.
     *
     * @param in1 the first input topic name
     * @param in2 the second input topic name
     * @param out the output topic name
     */
    public PlusAgent(String in1, String in2, String out) {
        this(new String[]{in1, in2}, new String[]{out});
    }

    /**
     * Returns the agent name.
     *
     * @return {@code "PlusAgent"}
     */
    @Override
    public String getName() {
        return "PlusAgent";
    }

    /**
     * Clears cached input values so the agent waits for fresh messages.
     */
    @Override
    public synchronized void reset() {
        x = 0;
        y = 0;
        hasX = false;
        hasY = false;
    }

    /**
     * Releases resources (no-op for this agent).
     */
    @Override
    public void close() {
    }

    /**
     * Accumulates input values and publishes their sum when both inputs are available.
     *
     * <p>Maps {@code subs[0]} to the first operand and {@code subs[1]} to the second.
     * Publishes {@code x + y} to {@code pubs[0]} only when both operands are valid
     * numbers.</p>
     *
     * @param topic the topic that published the message
     * @param msg the incoming message
     */
    // synchronized because, although a ParallelAgent wrapper normally serializes calls, this
    // agent keeps mutable cross-call state (x/y/hasX/hasY); the lock guards against any
    // concurrent publisher touching that state directly.
    @Override
    public synchronized void callback(String topic, Message msg) {
        if (subs == null || subs.length < 2) {
            return;
        }

        // Route the incoming value to the correct operand slot based on which topic sent it.
        double v = msg.asDouble;
        if (topic.equals(subs[0])) {
            x = v;
            hasX = true;
        } else if (topic.equals(subs[1])) {
            y = v;
            hasY = true;
        } else {
            return; // message from an unrelated topic: ignore.
        }

        // Only emit a sum once BOTH inputs have arrived and are valid numbers (not NaN). This
        // "wait for all inputs" gate is what makes the agent correct in an async graph where
        // the two operands can arrive in any order and at different times.
        if (hasX && hasY && !Double.isNaN(x) && !Double.isNaN(y)) {
            if (pubs != null && pubs.length >= 1) {
                TopicManager tm = TopicManagerSingleton.get();
                tm.getTopic(pubs[0]).publish(new Message(x + y));
            }
        }
    }
}
