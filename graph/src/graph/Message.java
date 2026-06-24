package graph;

import java.util.Date;

/**
 * Immutable value wrapper for data flowing through the pub-sub graph.
 *
 * <p>A {@code Message} can be constructed from a byte array, a string, or a double. The
 * {@link #asText} and {@link #asDouble} fields provide convenient views of the same
 * underlying value. If the text cannot be parsed as a number, {@link #asDouble} is set
 * to {@link Double#NaN}.</p>
 *
 * <p>The {@link #date} field records the creation timestamp of the message.</p>
 *
 * @see Topic#publish
 * @see Agent#callback
 */
public class Message {

    /** The raw byte content of the message. */
    public final byte[] data;

    /** The message content interpreted as a UTF-8 string. */
    public final String asText;

    /**
     * The message content interpreted as a {@code double}.
     * Set to {@link Double#NaN} if the text is not a valid number.
     */
    public final double asDouble;

    /** The timestamp when this message was created. */
    public final Date date;

    /**
     * Creates a message from a byte array.
     *
     * @param data the raw message bytes
     */
    // constructor for byte array
    public Message(byte[] data) {
        this.data = data;
        this.asText = new String(data);
        double tempDouble;
        try {
            tempDouble = Double.parseDouble(this.asText);
        } catch (NumberFormatException e) {
            tempDouble = Double.NaN;
        }
        this.asDouble = tempDouble;
        this.date = new Date();
    }

    /**
     * Creates a message from a string value.
     *
     * @param text the message text
     */
    // constructor for String
    public Message(String text) {
        this.data = text.getBytes();
        this.asText = text;
        double tempDouble;
        try {
            tempDouble = Double.parseDouble(text);
        } catch (NumberFormatException e) {
            tempDouble = Double.NaN;
        }
        this.asDouble = tempDouble;
        this.date = new Date();
    }

    /**
     * Creates a message from a numeric value.
     *
     * @param value the numeric message value
     */
    // constructor for double
    public Message(double value) {
        this.asDouble = value;
        this.asText = Double.toString(value);
        this.data = this.asText.getBytes();
        this.date = new Date();
    }
}
