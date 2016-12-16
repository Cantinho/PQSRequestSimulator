package br.com.p;

import static br.com.p.cm.CONNECT;

/**
 * Created by jordao on 12/12/16.
 */
public class cmp implements mp {

    public synchronized im processMessage(final String message){
        //System.out.println("Cloudia Message Processor - processMessage:" + message);
        int messageLength = message.length();
        String header = message.substring(0, 2);
        String packetSize = message.substring(2, 4);
        String sequence = message.substring(4, 6);
        String command = message.substring(6, 8);
        String data = message.substring(8, messageLength - 2);
        String checksum = message.substring(messageLength - 2, messageLength);

        return new cm(header, packetSize, sequence, command, data, checksum);
    }

    public synchronized String synthMessage(final im message) {
        return message.getMessage();
    }

    public synchronized String getStatusMessage(String message, boolean statusCode) {

        final cm cm = (cm) processMessage(message);
        String statusMessage;
        switch (cm.getCommand()) {
            case CONNECT:
                statusMessage = (statusCode ? cm.CONNECT_OK : cm.CONNECT_ERROR);
                break;
            default:
                statusMessage = (statusCode ? cm.OK : cm.ERROR);
                break;
        }

        cm responseCm = new cm(cm.getHeader(),
                cm.getSequence(), cm.getCommand(), statusMessage, "");

        responseCm.dfsfdg();
        return responseCm.getMessage();
    }
}
