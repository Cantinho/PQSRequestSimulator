package br.com.example.bean;

import br.com.example.statistics.IRequestStatisticallyProfilable;
import br.com.example.statistics.IStatistics;
import br.com.example.statistics.RequestStatistics;
import br.com.processor.ComunicationProtocol;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static br.com.example.request.Request.POST;

/**
 * Created by jordao on 27/11/16.
 */
public class Slave implements IRequestStatisticallyProfilable, ComunicationProtocol {

    private final static Logger LOGGER = LoggerFactory.getLogger(Slave.class);

    private String applicationID;
    private String masterSerialNumber;
    private final int MINIMUM_PULLING_INTERVAL;
    private final int PULLING_OFFSET;
    private final int MINIMUM_PUSHING_INTERVAL;
    private final int PUSHING_OFFSET;
    private List<Future> runnablePullerFutures;
    private List<Future> runnablePusherFutures;

    private List<IStatistics> requestStatisticsList = new LinkedList<IStatistics>();

    private static byte sequence = 0;

    // TODO change this to package attributes;
    /**
     * Ficar imprimindo o status local (lock) após pooling.
     * Ao conectar, enviar mensagem para o master pedindo status.
     * Quando apull do lock for chamando, enviar mensagem para a de trocar o estado do lock.
     * Essa mensagem chega no master. Este processa e envia resposta para slave.
     * O Slave recebe a mensagem do status referente à mensagem enviada e atualiza seu status (lock).
     * Imprimir lock do slave.
     * Fazer o mesmo a cada pooling no Master
     */
    private boolean[] locks = {false, false}; // ficar estado do master;
    private boolean connected = false;
    private final String CONNECT = "43";
    private final String STATUS = "58";
    private final String LOCK = "4E";
    private final String UNLOCK = "4F";
    private final String OK = "01";
    private final String ERROR = "02";

    public Slave(String applicationID, String masterSerialNumber, int minimumPullingInterval, int pullingOffset,
                 int minimumPushingInterval, int pushingOffset) {
        this.applicationID = applicationID;
        this.masterSerialNumber = masterSerialNumber;
        this.MINIMUM_PULLING_INTERVAL = minimumPullingInterval;
        this.PULLING_OFFSET = pullingOffset;
        this.MINIMUM_PUSHING_INTERVAL = minimumPushingInterval;
        this.PUSHING_OFFSET = pushingOffset;
    }

    public Slave(String applicationID, String masterSerialNumber) {
        this(applicationID, masterSerialNumber, 3, 5, 5, 5);
    }

    private synchronized static byte incrementSequence() {
        sequence += sequence + (0xFF & 1);
        return sequence;
    }

    private synchronized byte getSequence() {
        return sequence;
    }

    public void init() {

        LOGGER.warn("#TAG Slave [ " + applicationID + " ]: status connection: [ not connected ]");
        if(connectToCentral()){
            LOGGER.warn("#TAG Slave [ " + applicationID + " ]: status connection: [ connection required ]");
        }

        runnablePullerFutures = new ArrayList<Future>();
        runnablePusherFutures = new ArrayList<Future>();
    }

    public void start() {
        Runnable puller = createPuller();
        Runnable pusher = createPusher();
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(2);
        Future<?> pullerFuture = executorService.scheduleAtFixedRate(puller, 0, MINIMUM_PULLING_INTERVAL, TimeUnit.SECONDS);
        Future<?> pusherFuture = executorService.scheduleAtFixedRate(pusher, 0, MINIMUM_PUSHING_INTERVAL, TimeUnit.SECONDS);
        runnablePullerFutures.add(pullerFuture);
        runnablePusherFutures.add(pusherFuture);
    }

    public void stopPullers() {
        Iterator<Future> runnableFutureIterator = runnablePullerFutures.iterator();
        while(runnableFutureIterator.hasNext()) {
            try {
                runnableFutureIterator.next().cancel(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            runnablePullerFutures.clear();
            runnablePullerFutures = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopPushers() {
        Iterator<Future> runnableFutureIterator = runnablePusherFutures.iterator();
        while(runnableFutureIterator.hasNext()) {
            try {
                runnableFutureIterator.next().cancel(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            runnablePusherFutures.clear();
            runnablePusherFutures = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopAll() {
        stopPullers();
        stopPushers();
    }

    private Runnable createPuller() {
        return new SlavePuller(PULLING_OFFSET);
    }

    private Runnable createPusher() {
        return new SlavePusher(PUSHING_OFFSET);
    }


    /**
     * executes POST /apull for devices
     * @return
     */
    private synchronized String apush(String body){
        long startTimestamp = new Date().getTime();
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Serial-Number", masterSerialNumber);
        headers.put("Application-ID", applicationID);
        headers.put("Content-Type", "application/json");


        String response = POST("/apush", headers, body);

        long endTimestamp = new Date().getTime();
        synchronized (requestStatisticsList) {
            RequestStatistics requestStatistics = new RequestStatistics(masterSerialNumber + "_" + applicationID, "apush", startTimestamp, endTimestamp);
            requestStatisticsList.add(requestStatistics);
        }
        return response;
    }

    /**
     * executes POST /apull for devices
     * @return
     */
    private synchronized String apull(){
        long startTimestamp = new Date().getTime();
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Serial-Number", masterSerialNumber);
        headers.put("Application-ID", applicationID);
        headers.put("Content-Type", "application/json");

        String response = POST("/apull", headers, null);

        long endTimestamp = new Date().getTime();
        synchronized (requestStatisticsList) {
            RequestStatistics requestStatistics = new RequestStatistics(masterSerialNumber + "_" + applicationID, "apull", startTimestamp, endTimestamp);
            requestStatisticsList.add(requestStatistics);
        }
        return response;
    }

    public String getApplicationID() {
        return applicationID;
    }

    public void setApplicationID(String applicationID) {
        this.applicationID = applicationID;
    }

    public String getMasterSerialNumber() {
        return masterSerialNumber;
    }

    public void setMasterSerialNumber(String centralSerialNumber) {
        this.masterSerialNumber = centralSerialNumber;
    }

    public List<IStatistics> collectStatistics() {
        return requestStatisticsList;
    }



    @Override
    public void processRequest(String s) {

    }

    @Override
    public void processResponse(String s) {

    }

    class SlavePuller extends Thread implements Runnable {
        private volatile boolean shutdown = false;
        private int pullingOffset;
        public SlavePuller(int pullingOffset){
            this.pullingOffset = pullingOffset;
        }
        public void run() {
            if(shutdown) {
                try {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            Random rand = new Random();
            int randomInterval = rand.nextInt(pullingOffset + 1);

            try {
                Thread.sleep(randomInterval * 1000);
            } catch (InterruptedException e) {
                //e.printStackTrace();
            }
            try {
                String response = apull();
                if(response != null && !response.equals("{}")){
                    Message msg = new Gson().fromJson(response, Message.class);
                    String res = msg.getMessage();
                    processMessage(res);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            //LOGGER.info("PA PULL CENTRAL-SN [" + masterSerialNumber + "] APP-ID [" + applicationID + "]: " + response );
        }

        public void shutdown() {
            shutdown = true;
        }
    }



    synchronized String createLockMessage(int partition) {
        return createGenericLockMessage(partition, true);
    }

    synchronized String createUnlockMessage(int partition) {
        return createGenericLockMessage(partition, false);
    }

    synchronized String createConnectMessage() {
        return createMessage(CONNECT, "");

    }

    synchronized String createMessage(final String command, final String data) {
        final String header = "7B";
        final byte currentSequence = incrementSequence();
        //byte command
        final String dummyChecksum = Byte.toString((byte) 0);
        final String packetSize = Byte.toString((byte) (5 + (data.length() == 0 ? 0 : data.length()/2)));
        return header + packetSize + Byte.toString(currentSequence) + command + data + dummyChecksum;
    }

    synchronized String createGenericLockMessage(int partition, boolean lock) {
        return createMessage((lock ? LOCK : UNLOCK), Byte.toString(Byte.parseByte( (partition & 0xFF) + "", 16)));
    }

    synchronized String processMessage(final String message) {
        // TODO something useful with header, packetSize, sequence and checksum.
        final int messageLength = message.length();
        final String header = message.substring(0, 2);
        final String packetSize = message.substring(2, 4);
        final String sequence = message.substring(4, 6);
        final String command = message.substring(6, 8);
        final String data = message.substring(8, messageLength - 2);
        final String checksum = message.substring(messageLength - 2, messageLength);
        switch (command) {
            case CONNECT:
                return processConnectResponse(data);
            case LOCK:
                return processLockResponse(data);
            case UNLOCK:
                return processUnlockResponse(data);
            case STATUS:
                return processStatusRequest(data);
            default:
                LOGGER.warn("#TAG Slave COMMAND + [ " + command + " ] NOT FOUND.");
                return null;
        }
    }

    synchronized String processConnectResponse(final String status) {
        final Integer statusCode = Integer.valueOf(status);
        if(statusCode == 1) {
            connected = true;
            LOGGER.warn("#TAG Slave [ " + applicationID + " ]: status connection: [ connected ].");
        } else {
            connected = false;
        }
        return status;
    }

    synchronized String processLockResponse(final String status) {
        return status;
    }

    synchronized String processUnlockResponse(final String status) {
        return status;
    }

    synchronized String processStatusRequest(final String data) {
        final String lock0 = data.substring(0, 2);
        final String lock1 = data.substring(2, 4);
        try {
            locks[0] = Integer.valueOf(lock0) == 1 ? true : false;
            locks[1] = Integer.valueOf(lock1) == 1 ? true : false;
            LOGGER.warn("#TAG Slave [ " + applicationID + " ]: status locks[0]:[ " + locks[0] + " ] locks[1]:[ " + locks[1] + " ].");
            return OK;
        } catch (Exception e) {
            LOGGER.error("Error when parsing lock string to boolean");
            e.printStackTrace();
        }
        return ERROR;
    }


    class SlavePusher extends Thread implements Runnable {
        private final int pushingOffset;
        private volatile boolean shutdown = false;
        private boolean sleepMode = false;

        public SlavePusher(int pushingOffset, boolean sleepMode) {
            this.pushingOffset = pushingOffset;
            this.sleepMode = sleepMode;
        }

        public SlavePusher(int pushingOffset) {
            this(pushingOffset, false);
        }

        public void run() {
            if(shutdown) {
                try {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if(!sleepMode) {
                Random rand = new Random();
                int randomInterval = rand.nextInt(pushingOffset + 1);

                try {
                    Thread.sleep(randomInterval * 1000);
                } catch (InterruptedException e) {
                    //e.printStackTrace();
                }

                //String body = "7B4CAAABBBCCCDDDEEEFFF";
                if(connected) {
                    String response = null;
                    final int randomLock = new Random().nextInt(2);
                    if(locks[randomLock]) {
                        response = apush(createUnlockMessage(randomLock));
                        LOGGER.warn("#TAG Slave [ " + applicationID + " ]: status locks[" + randomLock + "]: [ CHANGE lock REQUIRED ] to [ UNLOCK ].");
                    } else {
                        response = apush(createLockMessage(randomLock));
                        LOGGER.warn("#TAG Slave [ " + applicationID + " ]: status locks[" + randomLock + "]: [ CHANGE lock REQUIRED ] to [ LOCK ].");
                    }
                    LOGGER.info("PA POST CENTRAL-SN [" + masterSerialNumber + "] APP-ID [" + applicationID + "]: " + response);
                }
            }
        }

        public void shutdown() {
            shutdown = true;
        }
    }

    private boolean connectToCentral(){
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Serial-Number", masterSerialNumber);
        headers.put("Application-ID", applicationID);
        headers.put("Content-Type", "application/json");
        String response = "";
        try {
            response = POST("/sconn", headers, createConnectMessage());
        }catch (Exception e){
            e.printStackTrace();
        }

        return response.equals("RECEIVED");
    }


}
