package test;

import java.util.Date;

public class Message {
    public final byte[] data;
    public final String asText;
    public final double asDouble;
    public final Date date;

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
    // constructor for double
    public Message(double value) {
        this.asDouble = value;
        this.asText = Double.toString(value);
        this.data = this.asText.getBytes();
        this.date = new Date();
    }
}
