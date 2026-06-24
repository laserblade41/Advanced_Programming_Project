package graph;

/**
 * Core interface for computational nodes in the pub-sub graph engine.
 *
 * <p>An {@code Agent} subscribes to one or more {@link Topic}s and reacts to incoming
 * {@link Message}s by publishing results to output topics. Agents are wired together at
 * runtime by {@link configs.Config} implementations (e.g. {@link configs.GenericConfig}) and
 * may be wrapped in a {@link ParallelAgent} for asynchronous execution.</p>
 *
 * <p><strong>Thread safety:</strong> {@link #callback} may be invoked concurrently from
 * multiple publisher threads unless the agent is wrapped in {@link ParallelAgent}. Implementations
 * that hold mutable state must synchronize access (see {@link configs.PlusAgent} and
 * {@link configs.IncAgent}).</p>
 *
 * @see Topic
 * @see ParallelAgent
 * @see Message
 */
public interface Agent {

    /**
     * Returns the unique display name of this agent.
     *
     * <p>Used as the node identifier when building a {@link configs.Graph} visualization.</p>
     *
     * @return the agent name (e.g. {@code "PlusAgent"}, {@code "Aplus"})
     */
    String getName();

    /**
     * Resets any internal state accumulated during processing.
     *
     * <p>Called to clear cached input values before a new computation cycle.</p>
     */
    void reset();

    /**
     * Handles an incoming message published on a subscribed topic.
     *
     * @param topic the name of the topic that published the message
     * @param msg the published message value
     */
    void callback(String topic, Message msg);

    /**
     * Releases resources held by this agent.
     *
     * <p>Implementations should unsubscribe from topics and stop any background threads.</p>
     */
    void close();
}
