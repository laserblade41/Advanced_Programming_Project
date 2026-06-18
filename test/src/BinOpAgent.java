package test;

import java.util.function.BinaryOperator;

public class BinOpAgent implements Agent {

    private final String name;   // agent name, will be "A" + baseName to match tests
    private final String in1;
    private final String in2;
    private final String out;
    private final BinaryOperator<Double> op;

    private double val1 = Double.NaN;
    private double val2 = Double.NaN;

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

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void reset() {
        val1 = Double.NaN;
        val2 = Double.NaN;
    }

    @Override
    public void close() {
        TopicManagerSingleton.get().getTopic(in1).unsubscribe(this);
        TopicManagerSingleton.get().getTopic(in2).unsubscribe(this);
        TopicManagerSingleton.get().getTopic(out).removePublisher(this);
    }

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
