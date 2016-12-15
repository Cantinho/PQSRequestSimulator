package br.com.processor;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;

/**
 * Created by jordao on 12/12/16.
 */
public interface ComunicationProtocol {

    void processRequest(final String request);

    void processResponse(final String response);

    void processRequest(HttpResponse<JsonNode> request);

    void processResponse(HttpResponse<JsonNode> request);

}
