package br.com.example.s;

/**
 * Created by samirtf on 28/11/16.
 */
public interface is {

    long getSequence();

    String getLabel();

    String getMessage();

    long getTotalTime();

    String print(boolean messageSuppressed);

    String csv();

}
