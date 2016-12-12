package br.com.example.bean;


import br.com.example.statistics.IRequestStatisticallyProfilable;
import br.com.example.statistics.IStatistics;
import br.com.example.statistics.RequestStatistics;
import br.com.processor.*;
import br.com.processor.mapper.MessageMapper;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;

import static br.com.example.request.Request.GET;
import static br.com.example.request.Request.POST;

/**
 * Created by jordao on 27/11/16.
 */
public class Master implements IRequestStatisticallyProfilable, ComunicationProtocol {

    private final static Logger LOGGER = LoggerFactory.getLogger(Master.class);
    private static int statisticalSequence = 0;

    private String serialNumber;
    private final int MINIMUM_PULLING_INTERVAL;
    private final int PULLING_OFFSET;
    private final int MINIMUM_PUSHING_INTERVAL;
    private final int PUSHING_OFFSET;
    private List<Future> runnablePullersFutures;
    private List<Future> runnablePushersFutures;

    private List<IStatistics> requestStatisticsList = new LinkedList<IStatistics>();
    private static byte sequence = 0;
    private final String OK = "01";
    private final String ERROR = "02";

    /**
     * known commands
     */
    private boolean[] locks = {false, false};
    private final String CONNECT = "43";
    private final String STATUS = "58";
    private final String LOCK = "4E";
    private final String UNLOCK = "4F";


    public Master(String serialNumber, int minimumPullingInterval, int pullingOffset,
                  int minimumPushingInterval, int pushingOffset) {
        this.serialNumber = serialNumber;
        this.MINIMUM_PULLING_INTERVAL = minimumPullingInterval;
        this.PULLING_OFFSET = pullingOffset;
        this.MINIMUM_PUSHING_INTERVAL = minimumPushingInterval;
        this.PUSHING_OFFSET = pushingOffset;
    }

    public Master(String serialNumber) {
        this(serialNumber, 1, 0, 6, 6);
    }

    private synchronized static byte incrementSequence() {
        sequence += sequence + (0xFF & 1);
        return sequence;
    }

    private synchronized byte getSequence() {
        return sequence;
    }

    public void init() {

        processResponse(connectToCloudService());

        runnablePullersFutures = new ArrayList<Future>();
        runnablePushersFutures = new ArrayList<Future>();
    }

    public void start() {
        Runnable puller = createPuller();
        Runnable pusher = createPusher();
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(2);
        Future<?> pullerFuture = executorService.scheduleAtFixedRate(puller, 0, MINIMUM_PULLING_INTERVAL, TimeUnit.SECONDS);

        Future<?> pusherFuture = executorService.scheduleAtFixedRate(pusher, 0, MINIMUM_PUSHING_INTERVAL, TimeUnit.SECONDS);
        runnablePullersFutures.add(pullerFuture);
        runnablePushersFutures.add(pusherFuture);
    }

    public void stopPullers() {
        Iterator<Future> runnableFutureIterator = runnablePullersFutures.iterator();
        while(runnableFutureIterator.hasNext()) {
            try {
                runnableFutureIterator.next().cancel(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            runnablePullersFutures.clear();
            runnablePullersFutures = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopPushers() {
        Iterator<Future> runnableFutureIterator = runnablePushersFutures.iterator();
        while(runnableFutureIterator.hasNext()) {
            try {
                runnableFutureIterator.next().cancel(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            runnablePushersFutures.clear();
            runnablePushersFutures = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        stopPullers();
        stopPushers();
    }

    private Runnable createPuller() {
        return new MasterPuller(PULLING_OFFSET);
    }

    private Runnable createPusher() {
        return new MasterPusher(PUSHING_OFFSET, false);
    }


    /**
     * executes GET /cpull for centrals
     * @return
     */
    private String cpull(){
        long startTimestamp = new Date().getTime();
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Serial-Number", serialNumber);

        String response = GET("/cpull", headers);
        long endTimestamp = new Date().getTime();
        synchronized (requestStatisticsList) {
            RequestStatistics requestStatistics = new RequestStatistics(serialNumber, "cpull", startTimestamp, endTimestamp);
            requestStatisticsList.add(requestStatistics);
        }
        return response;
    }

    /**
     * executes POST /cpush for centrals
     * @param message
     * @return
     */
    private String cpush(Message message, Map<String, String> headers){
        long startTimestamp = new Date().getTime();
        headers.put("Application-ID", message.getApplicationID());
        headers.put("Content-Type", "application/json");
        String body = message.getMessage();

        String response = POST("/cpush", headers, body);
        long endTimestamp = new Date().getTime();
        synchronized (requestStatisticsList) {
            RequestStatistics requestStatistics = new RequestStatistics(serialNumber, "cpush", startTimestamp, endTimestamp);
            requestStatisticsList.add(requestStatistics);
        }
        return response;
    }

    private String cpush(String applicationID, String body, Map<String, String> headers){
        long startTimestamp = new Date().getTime();
        headers.put("Application-ID", applicationID);
        headers.put("Content-Type", "application/json");

        String response = POST("/cpush", headers, body);
        long endTimestamp = new Date().getTime();
        synchronized (requestStatisticsList) {
            RequestStatistics requestStatistics = new RequestStatistics(serialNumber, "cpush", startTimestamp, endTimestamp);
            requestStatisticsList.add(requestStatistics);
        }
        return response;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public List<IStatistics> collectStatistics() {
        return requestStatisticsList;
    }

    class MasterPuller extends Thread implements Runnable {
        private volatile boolean shutdown = false;
        private int pullingOffset;

        public MasterPuller(int pullingOffset) {
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

            processRequest(cpull());
        }

        public void shutdown() {
            shutdown = true;
        }


    }

    @Override
    public void processRequest(String request) {
        LOGGER.info("PULL CENTRAL-SN [" + serialNumber + "]: " + request);
        if(!request.equals("{}")){
            Map<String, String> headers = new HashMap<String, String>();
            headers.put("Serial-Number", serialNumber);

            Message message = (new Gson()).fromJson(request, Message.class);


            IMessageProcessor messageProcessor = new MessageProcessor();
            final CloudiaMessage processedMessage = (CloudiaMessage) messageProcessor.processMessage(message.getMessage());

            switch (processedMessage.getCommand()) {
                case LOCK:
                    LOGGER.warn("#TAG Master [ " + serialNumber + " ]: change lock status required to LOCK");
                    headers.put("Broadcast", "true");
                    processLock(processedMessage.getData(), true);
                    processResponse(cpush(message.getApplicationID(), createStatusMessage(getMasterStatus()), headers));
                    break;
                case UNLOCK:
                    LOGGER.warn("#TAG Master [ " + serialNumber + " ]: change lock status required to UNLOCK");
                    headers.put("Broadcast", "true");
                    processLock(processedMessage.getData(), false);
                    processResponse(cpush(message.getApplicationID(), createStatusMessage(getMasterStatus()), headers));
                    break;
                default:
                    LOGGER.warn("#TAG Master COMMAND + [ " + processedMessage.getCommand() + " ] NOT FOUND.");
                    cpush(message, headers);
                    break;
            }

            LOGGER.info("PC CENTRAL-SN [" + serialNumber + "] APP-ID [" + message.getApplicationID() + "]: " + request);
        }
    }

    @Override
    public void processResponse(String response) {

        MessageMapper msg = new Gson().fromJson(response, MessageMapper.class);

        IMessageProcessor messageProcessor = new MessageProcessor();
        final CloudiaMessage processedMessage = (CloudiaMessage) messageProcessor.processMessage(msg.getMsg());

        switch (processedMessage.getCommand()) {
            case CONNECT:
                processConnectResponse(processedMessage.getData());
                break;
            case STATUS:
                LOGGER.warn("#TAG Master [ " + serialNumber + " ]: STATUS");
                processStatusResponse(processedMessage.getData());
                break;
            default:
                LOGGER.warn("#TAG Master COMMAND + [ " + processedMessage.getCommand() + " ] NOT FOUND.");
        }
    }

    synchronized String processConnectResponse(final String status) {
        // TODO FIX code mocked
        //return status;
        return OK;
    }

    synchronized String processStatusResponse(final String status) {
        // TODO FIX code mocked
        //return status;
        return OK;
    }



    private synchronized String getMasterStatus() {
        boolean lock0Status = locks[0];
        final String lock0StatusHex = Byte.toString(lock0Status ? (byte) 0x01 : (byte) 0x02);
        boolean lock1Status = locks[1];
        final String lock1StatusHex = Byte.toString(lock1Status ? (byte) 0x01 : (byte) 0x02);
        return lock0StatusHex + lock1StatusHex;
    }

    synchronized String createStatusMessage(String status) {
        return createMessage(STATUS, status);
    }

    synchronized String createConnectMessage(final String code) {
        return createMessage(CONNECT, code);
    }

    private synchronized String createMessage(final String command, final String data) {
        final byte currentSequence = incrementSequence();
        //byte command
        final String dummyChecksum = Byte.toString((byte) 0);

        IMessageProcessor messageProcessor = new MessageProcessor();
        return messageProcessor.synthMessage(new CloudiaMessage("7B", Byte.toString(currentSequence), command, data, dummyChecksum));
    }


    void processLock(final String lock, boolean isLocked) {
        try {
            final int lockIndex = Integer.valueOf(lock.substring(0, 2));
            locks[lockIndex] = isLocked;
        } catch (Exception e) {
            LOGGER.error("Error when parsing lock string to boolean");
            e.printStackTrace();
        }
    }

    class MasterPusher extends Thread implements Runnable {
        private volatile boolean shutdown = false;
        private int pushingOffset;
        private boolean sleepMode = false;

        public MasterPusher(int pushingOffset, boolean sleepMode){
            this.pushingOffset = pushingOffset;
            this.sleepMode = sleepMode;
        }

        public MasterPusher(int pushingOffset){
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

                Map<String, String> headers = new HashMap<String, String>();
                headers.put("Serial-Number", serialNumber);
                headers.put("Broadcast", "true");

                Message message = new Message(serialNumber, null, String.valueOf(new Date().getTime()), "10", "7B43FFFBBBCCCAAADDD");
                String response = cpush(message, headers);
                LOGGER.info("PC CENTRAL-SN [" + serialNumber + "] APP-ID [" + message.getApplicationID() + "]: " + response);
            }
        }

        public void shutdown() {
            shutdown = true;
        }
    }

    private String connectToCloudService(){
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Serial-Number", serialNumber);
        headers.put("Content-Type", "application/json");
        String response = "";
        try {
            response = POST("/cconn", headers, createConnectMessage(""));
        }catch (Exception e){
            e.printStackTrace();
        }
        return response;
    }

}
