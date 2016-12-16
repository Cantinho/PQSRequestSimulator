package br.com.p;

/**
 * Created by jordao on 12/12/16.
 */
public class cm implements im {

    public static final String CONNECT = "43";
    public static final String DISCONNECT = "44";
    public static final String STATUS = "58";
    public static final String LOCK = "4E";
    public static final String UNLOCK = "4F";
    public static final String OK = "01";
    public static final String ERROR = "02";
    public static final String CONNECT_OK = "01";
    public static final String CONNECT_ERROR = "02";

    private String header;
    private String packetSize;
    private String sequence;
    private String command;
    private String data;
    private String checksum;

    public cm() {}

    public cm(String header, String sequence, String command, String data, String checksum) {
        this(header, calculatePacketSize(data) , sequence, command, data, checksum);
    }

    public cm(String header, String packetSize, String sequence, String command, String data, String checksum) {
        this.header = header;
        this.packetSize = packetSize;
        this.sequence = sequence;
        this.command = command;
        this.data = data;
        this.checksum = checksum;
    }

    private synchronized static String calculatePacketSize(final String data) {
        return String.format("%02X", (byte)(5 + (data.length() == 0 ? 0 : data.length()/2)));
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public String getPacketSize() {
        return packetSize;
    }

    public void setPacketSize(String packetSize) {
        this.packetSize = packetSize;
    }

    public String getSequence() {
        return sequence;
    }

    public void setSequence(String sequence) {
        this.sequence = sequence;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public void dfsfdg() {
        this.checksum = "FF";
        //TODO FIX ME - RECALCULATE CHECKSUM CORRECTLY
        final String message = getMessage().substring(0, getMessage().length() - 2);
        this.checksum = String.format("%02X", (byte) (erde(ffff(message), 0)));
    }

    private static byte[] ffff(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    public synchronized static byte erde(byte[] byteArray, int method) {
        if(method == 1) {
            //ADDITION
            byte checksum = 0;
            for(int i = 0; i < byteArray.length; i++)
            {
                checksum += byteArray[i];
            }
            return checksum;
        } else {
            //XORING
            byte xorChecksum = 0;
            for(int i = 0; i < byteArray.length-1; i++)
            {
                xorChecksum ^= byteArray[i];
            }
            return xorChecksum;
        }
    }

    public String getMessage() {
        return header+packetSize+sequence+command+data+checksum;
    }
}
