package graph;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Decorator that executes an {@link Agent}'s {@link Agent#callback} on a background worker thread.
 *
 * <p>{@code ParallelAgent} wraps a delegate agent and enqueues incoming messages into a
 * bounded {@link BlockingQueue}. A dedicated worker thread dequeues messages and invokes
 * the delegate's {@link Agent#callback}, preventing long-running agent logic from blocking
 * the publisher's thread (e.g. an HTTP request handler).</p>
 *
 * <p>Used by {@link configs.GenericConfig} to wrap every agent loaded from a configuration
 * file with a queue capacity of 10.</p>
 *
 * <p><strong>Thread safety:</strong> The queue is thread-safe. The wrapped agent's
 * {@link Agent#callback} always runs on the single worker thread, serializing execution
 * for that agent. {@link #callback} blocks when the queue is full.</p>
 *
 * @see Agent
 * @see configs.GenericConfig
 */
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

    /**
     * Creates a parallel wrapper around the given agent with a bounded message queue.
     *
     * <p>Starts a background worker thread that processes queued messages until
     * {@link #close()} is called.</p>
     *
     * @param agent the delegate agent whose {@link Agent#callback} will run on the worker thread
     * @param capacity the maximum number of messages that can be queued before
     *                 {@link #callback} blocks
     */
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

    /**
     * Enqueues a message for asynchronous processing by the wrapped agent.
     *
     * <p>Blocks if the queue has reached its maximum capacity.</p>
     *
     * @param topic the name of the topic that published the message
     * @param msg the published message
     */
    @Override
    public void callback(String topic, Message msg) {
        try {
            // put() blocks/waits if the queue has reached its maximum capacity
            queue.put(new TopicMessage(topic, msg));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Stops the worker thread and closes the wrapped agent.
     *
     * <p>Interrupts the worker, waits for it to finish via {@link Thread#join()}, then
     * delegates to {@link Agent#close()} on the wrapped agent.</p>
     */
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

    /**
     * Returns the name of the wrapped agent.
     *
     * @return the delegate agent's name from {@link Agent#getName()}
     */
    @Override
    public String getName() {
        return agent.getName();
    }

    /**
     * Resets the wrapped agent's internal state.
     */
    @Override
    public void reset() {
        agent.reset();
    }
}
