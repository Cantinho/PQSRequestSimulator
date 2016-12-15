package br.com.processor;

/**
 * Created by jordao on 12/12/16.
 */
public interface IMessageProcessor {

    IMessage processMessage(final String message);

    String synthMessage(IMessage message);

    String getStatusMessage(final String message, final boolean statusCode);

}
