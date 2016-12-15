package br.com.processor;

/**
 * Created by jordao on 12/12/16.
 */
public class SimpleMessageProcessor implements IMessageProcessor {

    public synchronized IMessage processMessage(final String message){
        return new SimpleMessage(message);
    }

    public synchronized String synthMessage(final IMessage message) {
        return message.getMessage();
    }

    public String getStatusMessage(String message, boolean statusCode) {
        if(statusCode) {
            return CloudiaMessage.OK;
        } else {
            return CloudiaMessage.ERROR;
        }
    }
}
