package br.com.example.s;

/**
 * Created by samirtf on 28/11/16.
 */
public class rss implements is {

    private static long globalSequence = 0;

    private final long sequence;
    private final String label;
    private final String message;
    private final long startTime;
    private final long endTime;

    public rss(String label, String message, long startTime, long endTime) {
        synchronized (rss.this) {
            this.sequence = globalSequence++;
        }
        this.label = label;
        this.message = message;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public long getSequence() {
        return sequence;
    }

    public String getLabel() {
        return label;
    }

    public String getMessage() {
        return message;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public long getTotalTime() {
        return endTime - startTime;
    }

    public String print(boolean messageSuppressed) {
        return "";
    }

    @Override
    public String toString() {
        return print(false);
    }

    public String csv() {
        return "";
    }

}
