package br.com.processor;

/**
 * Created by jordao on 12/12/16.
 */
public class SimpleMessage implements IMessage {

    public static final String HELLO_WORLD_MESSAGE = "HELLO WORLD";
    public static final String STATUS_MESSAGE = "STATUS";
    public static final String OK = "OK";
    public static final String ERROR = "ERROR";

    private String message;

    public SimpleMessage() {}

    public SimpleMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
