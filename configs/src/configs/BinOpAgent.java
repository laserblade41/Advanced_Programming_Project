package configs;

import java.util.function.BinaryOperator;
import graph.*;

/**
 * Generic two-input binary-operation agent.
 *
 * <p>Subscribes to two input topics and applies a {@link BinaryOperator} when both
 * inputs have valid numeric values, publishing the result to an output topic. The
 * agent name is {@code "A"} + {@code baseName} (e.g. {@code "Aplus"}).</p>
 *
 * <p>Unlike {@link PlusAgent}, this agent auto-subscribes and registers as a publisher
 * in its constructor via {@link graph.TopicManagerSingleton}.</p>
 *
 * <p><strong>Thread safety:</strong> {@link #callback} is {@code synchronized}.</p>
 *
 * @see Agent
 * @see PlusAgent
 * @see MathExampleConfig
 */
public class BinOpAgent implements Agent {

    private final String name;   // agent name, will be "A" + baseName to match tests
    private final String in1;
    private final String in2;
    private final String out;
    private final BinaryOperator<Double> op;

    private double val1 = Double.NaN;
    private double val2 = Double.NaN;

    /**
     * Creates a binary-operation agent and wires it to the topic manager.
     *
     * <p>Subscribes to {@code in1} and {@code in2}, registers as publisher on {@code out}.</p>
     *
     * @param baseName the base name used to form the agent name ({@code "A" + baseName})
     * @param in1 the first input topic name
     * @param in2 the second input topic name
     * @param out the output topic name
     * @param op the binary operation to apply when both inputs are available
     */
    public BinOpAgent(String baseName, String in1, String in2, String out, BinaryOperator<Double> op) {
        this.name = "A" + baseName;
        this.in1 = in1;
        this.in2 = in2;
        this.out = out;
        this.op = op;
        // subscribe to inputs and register as publisher for output
        TopicManagerSingleton.get().getTopic(in1).subscribe(this);
        TopicManagerSingleton.get().getTopic(in2).subscribe(this);
        TopicManagerSingleton.get().getTopic(out).addPublisher(this);
    }

    /**
     * Returns the agent name.
     *
     * @return {@code "A"} concatenated with the base name provided at construction
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Clears cached input values to {@link Double#NaN}.
     */
    @Override
    public void reset() {
        val1 = Double.NaN;
        val2 = Double.NaN;
    }

    /**
     * Unsubscribes from input topics and removes publisher registration.
     */
    @Override
    public void close() {
        TopicManagerSingleton.get().getTopic(in1).unsubscribe(this);
        TopicManagerSingleton.get().getTopic(in2).unsubscribe(this);
        TopicManagerSingleton.get().getTopic(out).removePublisher(this);
    }

    /**
     * Accumulates input values and publishes the result of the binary operation.
     *
     * <p>Publishes only when both {@code val1} and {@code val2} are valid numbers.</p>
     *
     * @param topic the topic that published the message
     * @param msg the incoming message
     */
    @Override
    public synchronized void callback(String topic, Message msg) {
        double v = msg.asDouble;
        if (topic.equals(in1)) {
            val1 = v;
        } else if (topic.equals(in2)) {
            val2 = v;
        } else {
            return;
        }

        if (!Double.isNaN(val1) && !Double.isNaN(val2)) {
            double res = op.apply(val1, val2);
            TopicManagerSingleton.get().getTopic(out).publish(new Message(res));
        }
    }
}
