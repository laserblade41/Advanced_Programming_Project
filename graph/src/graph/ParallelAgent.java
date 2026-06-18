package graph;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class ParallelAgent implements Agent {

    private final Agent agent;
    private final BlockingQueue<TopicMessage> queue;
    private final Thread workerThread;

    private static class TopicMessage {
        public final String topic;
        public final Message message;

        public TopicMessage(String topic, Message message) {
            this.topic = topic;
            this.message = message;
        }
    }

    public ParallelAgent(Agent agent, int capacity) {
        this.agent = agent; // The wrapped agent

        // Initialize a thread-safe BlockingQueue with the given capacity
        this.queue = new ArrayBlockingQueue<>(capacity);

        // Create and start the background worker thread
        this.workerThread = new Thread(() -> {
            // Loop until the thread is interrupted (usually by close())
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // take() blocks/waits if the queue is empty, using 0 CPU
                    TopicMessage tm = queue.take();

                    // Execute the wrapped agent's original callback on this background thread
                    this.agent.callback(tm.topic, tm.message);

                } catch (InterruptedException e) {
                    // Thread was interrupted while waiting on take(), exit the loop cleanly
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });

        this.workerThread.start();
    }

    @Override
    public void callback(String topic, Message msg) {
        try {
            // put() blocks/waits if the queue has reached its maximum capacity
            queue.put(new TopicMessage(topic, msg));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void close() {
        workerThread.interrupt();
        try {
            workerThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        agent.close();
    }

    @Override
    public String getName() {
        return agent.getName();
    }

    @Override
    public void reset() {
        agent.reset();
    }
}