package br.com.processor;

/**
 * Created by jordao on 12/12/16.
 */
public class CloudiaMessageProcessor implements IMessageProcessor {

    public synchronized IMessage processMessage(final String message){
        //System.out.println("Cloudia Message Processor - processMessage:" + message);
        int messageLength = message.length();
        String header = message.substring(0, 2);
        String packetSize = message.substring(2, 4);
        String sequence = message.substring(4, 6);
        String command = message.substring(6, 8);
        String data = message.substring(8, messageLength - 2);
        String checksum = message.substring(messageLength - 2, messageLength);

        return new CloudiaMessage(header, packetSize, sequence, command, data, checksum);
    }

    public synchronized String synthMessage(final IMessage message) {
        return message.getMessage();
    }

    public synchronized String getStatusMessage(String message, boolean statusCode) {

        final CloudiaMessage cloudiaMessage = (CloudiaMessage) processMessage(message);
        String statusMessage;
        switch (cloudiaMessage.getCommand()) {
            case CloudiaMessage.CONNECT:
                statusMessage = (statusCode ? CloudiaMessage.CONNECT_OK : CloudiaMessage.CONNECT_ERROR);
                break;
            default:
                statusMessage = (statusCode ? CloudiaMessage.OK : CloudiaMessage.ERROR);
                break;
        }

        CloudiaMessage responseCloudiaMessage = new CloudiaMessage(cloudiaMessage.getHeader(),
                cloudiaMessage.getSequence(), cloudiaMessage.getCommand(), statusMessage, "");

        responseCloudiaMessage.recalculateChecksum();
        return responseCloudiaMessage.getMessage();
    }
}
