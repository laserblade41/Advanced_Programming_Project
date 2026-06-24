package configs;

/**
 * Lifecycle contract for loading and wiring agents into the computational graph.
 *
 * <p><strong>SOLID:</strong> {@code Config} is an Open/Closed extension point - the system
 * supports new ways of building a graph (file-based {@link GenericConfig}, hard-coded
 * {@link MathExampleConfig}, or any future loader) purely by adding implementations, with no
 * change to the consuming code. Callers depend on this abstraction (Dependency Inversion)
 * rather than on a specific loader.</p>
 *
 * <p>A {@code Config} implementation creates {@link graph.Agent} instances, subscribes them
 * to input {@link graph.Topic}s, and registers them as publishers on output topics. The
 * configuration can be torn down via {@link #close()} to release subscriptions and
 * resources.</p>
 *
 * <p>Example usage with {@link GenericConfig}:</p>
 * <pre>{@code
 * GenericConfig config = new GenericConfig();
 * config.setConfFile("config_files/simple.conf");
 * config.create();
 * // ... graph is now active ...
 * config.close();
 * }</pre>
 *
 * @see GenericConfig
 * @see MathExampleConfig
 * @see graph.TopicManagerSingleton
 */
public interface Config {

    /**
     * Loads the configuration and wires agents to topics.
     *
     * <p>Implementations instantiate agents, subscribe them to input topics, and register
     * them as publishers on output topics.</p>
     */
    void create();

    /**
     * Returns the human-readable name of this configuration.
     *
     * @return the configuration name
     */
    String getName();

    /**
     * Returns the version number of this configuration.
     *
     * @return the configuration version
     */
    int getVersion();

    /**
     * Tears down the configuration and releases all wired agents.
     *
     * <p>Unsubscribes agents from topics and closes any held resources.</p>
     */
    void close();
}
