package project_biu.graph;

import java.util.Date;

/**
 * An immutable carrier of information that flows through the publish/subscribe
 * graph.
 *
 * <p>A {@code Message} can be inspected as raw bytes, as text, or as a
 * {@code double}. Construction from a textual value that is not a valid number
 * does not raise an exception; instead {@link #asDouble} is set to
 * {@link Double#NaN}. Every message records the timestamp at which it was
 * created.
 *
 * <p>Byte conversions deliberately use the JVM's default charset (i.e. plain
 * {@code String#getBytes()} / {@code new String(byte[])}) so that round-trips
 * agree with code on the same JVM that does the same.
 */
public class Message {

    /** Raw byte payload. */
    public final byte[] data;

    /** Textual view of the payload. */
    public final String asText;

    /** Numeric view of the payload ({@link Double#NaN} when not a number). */
    public final double asDouble;

    /** Creation timestamp. */
    public final Date date;

    /**
     * Canonical constructor. The {@code asText} value drives the other
     * conversions, so that every {@code Message} ends up with all three
     * representations consistent.
     *
     * @param text the textual payload; if it parses as a {@code double} that
     *             value is also exposed via {@link #asDouble}, otherwise
     *             {@code asDouble} is {@link Double#NaN}
     */
    public Message(String text) {
        this.asText = text;
        this.data = text.getBytes();
        double parsed;
        try {
            parsed = Double.parseDouble(text);
        } catch (NumberFormatException e) {
            parsed = Double.NaN;
        }
        this.asDouble = parsed;
        this.date = new Date();
    }

    /**
     * Convenience constructor that decodes the bytes using the JVM default
     * charset.
     *
     * @param data raw bytes to decode
     */
    public Message(byte[] data) {
        this(new String(data));
    }

    /**
     * Convenience constructor that stores a number as its textual form.
     *
     * @param value the numeric payload
     */
    public Message(double value) {
        this(Double.toString(value));
    }
}
