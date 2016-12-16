package br.com.p;

/**
 * Created by jordao on 12/12/16.
 */
public interface mp {

    im processMessage(final String message);

    String synthMessage(im message);

    String getStatusMessage(final String message, final boolean statusCode);

}
