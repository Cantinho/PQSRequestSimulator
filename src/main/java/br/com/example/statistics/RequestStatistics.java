package br.com.example.statistics;

/**
 * Created by samirtf on 28/11/16.
 */
public class RequestStatistics implements IStatistics {

    private static long globalSequence = 0;

    private final long sequence;
    private final String label;
    private final String message;
    private final long totalTime;

    public RequestStatistics(String label, String message, long totalTime) {
        this.sequence = globalSequence++;
        this.label = label;
        this.message = message;
        this.totalTime = totalTime;
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

    public long getTotalTime() {
        return totalTime;
    }

    public String print(boolean messageSuppressed) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.getClass().getName() + ":[");
        stringBuilder.append("sequence:" + sequence + ";");
        stringBuilder.append("label:" + label + ";");
        stringBuilder.append("totalTime:" + totalTime + ";");
        if(!messageSuppressed) stringBuilder.append("message:" + message);
        stringBuilder.append("]");
        return stringBuilder.toString();
    }


}
