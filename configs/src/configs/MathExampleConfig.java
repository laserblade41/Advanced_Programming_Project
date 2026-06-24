package configs;

/**
 * Example configuration that wires a pipeline of three {@link BinOpAgent} instances.
 *
 * <p>On {@link #create()}, instantiates:</p>
 * <ul>
 *   <li>{@code plus}:  {@code A + B → R1}</li>
 *   <li>{@code minus}: {@code A - B → R2}</li>
 *   <li>{@code mul}:   {@code R1 * R2 → R3}</li>
 * </ul>
 *
 * <p>Serves as a programmatic alternative to file-based loading via
 * {@link GenericConfig}.</p>
 *
 * @see Config
 * @see BinOpAgent
 */
public class MathExampleConfig implements Config {

    /**
     * Creates and wires the three binary-operation agents.
     */
    @Override
    public void create() {
        new BinOpAgent("plus", "A", "B", "R1", (x,y)->x+y);
        new BinOpAgent("minus", "A", "B", "R2", (x,y)->x-y);
        new BinOpAgent("mul", "R1", "R2", "R3", (x,y)->x*y);
    }

    /**
     * Returns the display name of this configuration.
     *
     * @return {@code "Math Example"}
     */
    @Override
    public String getName() {
        return "Math Example";
    }

    /**
     * Returns the version of this configuration.
     *
     * @return {@code 1}
     */
    @Override
    public int getVersion() {
        return 1;
    }

    /**
     * Releases resources (no-op; agents are not explicitly closed in this example).
     */
    public void close() {
    }
}
