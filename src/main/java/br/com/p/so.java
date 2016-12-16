package br.com.p;

/**
 * Created by jordao on 12/12/16.
 */
public class so implements mp {

    public synchronized im processMessage(final String message){
        return new sm(message);
    }

    public synchronized String synthMessage(final im message) {
        return message.getMessage();
    }

    public String getStatusMessage(String message, boolean statusCode) {
        if(statusCode) {
            return cm.OK;
        } else {
            return cm.ERROR;
        }
    }
}
