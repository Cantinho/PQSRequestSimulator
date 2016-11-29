package br.com.example.statistics;

import com.sun.javafx.binding.StringFormatter;

/**
 * Created by samirtf on 28/11/16.
 */
public class RequestStatistics implements IStatistics {

    private static long globalSequence = 0;

    private final long sequence;
    private final String label;
    private final String message;
    private final long startTime;
    private final long endTime;

    public RequestStatistics(String label, String message, long startTime, long endTime) {
        synchronized (RequestStatistics.this) {
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
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.getClass().getName() + ":[");
        stringBuilder.append("sequence:" + getSequence() + ";");
        stringBuilder.append("startTime:" + getStartTime() + ";");
        stringBuilder.append("endTime:" + getEndTime() + ";");
        stringBuilder.append("totalTime:" + getTotalTime() + ";");
        stringBuilder.append("label:" + label + ";");
        if(!messageSuppressed) stringBuilder.append("message:" + message);
        stringBuilder.append("]");
        return stringBuilder.toString();
    }

    @Override
    public String toString() {
        return print(false);
    }

    public String csv() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(String.format("%06d", getSequence()) + ";");
        stringBuilder.append(String.format("%d", getStartTime()) + ";");
        stringBuilder.append(String.format("%d", getEndTime()) + ";");
        stringBuilder.append(String.format("%06d", getTotalTime()) + ";");
        stringBuilder.append(getLabel() + ";");
        stringBuilder.append(getMessage());
        return stringBuilder.toString();
    }
    
}
