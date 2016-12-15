package br.com.processor;

/**
 * Created by jordao on 12/12/16.
 */
public interface ComunicationProtocol {

    void processRequest(final String request);

    void processResponse(final String response);

}
