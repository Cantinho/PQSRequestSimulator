package br.com.example.bean;


import br.com.example.statistics.IRequestStatisticallyProfilable;
import br.com.example.statistics.IStatistics;
import br.com.example.statistics.RequestStatistics;
import br.com.processor.*;
import br.com.processor.mapper.MessageMapper;
import com.google.gson.Gson;
import com.mashape.unirest.http.Headers;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;

import static br.com.example.request.Request.GET;
import static br.com.example.request.Request.POST;
import static br.com.example.request.Request.get;
import static br.com.example.request.Request.post;
import static br.com.processor.CloudiaMessage.*;

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


    /**
     * known commands
     */
    private boolean[] locks = {false, false};
    private boolean connected = false;


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
        sequence += (byte)(0x01);
        return sequence;
    }

    private synchronized byte getSequence() {
        return sequence;
    }

    public void init() {

        try {
            processResponse(connectToCloudService());
        } catch (Exception e) {
            e.printStackTrace();
        }
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
    private synchronized String old_cpull(){
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
     * executes GET /cpull for centrals
     * @return
     */
    private HttpResponse<JsonNode> cpull(){
        long startTimestamp = new Date().getTime();
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Serial-Number", serialNumber);

        HttpResponse<JsonNode> response = get("/cpull", headers);
        long endTimestamp = new Date().getTime();
        synchronized (requestStatisticsList) {
            RequestStatistics requestStatistics = new RequestStatistics(serialNumber, "cpull", startTimestamp, endTimestamp);
            requestStatisticsList.add(requestStatistics);
        }
        return response;
    }

    private HttpResponse<JsonNode> cpush(String body, Map<String, String> headers){
        long startTimestamp = new Date().getTime();

        HttpResponse<JsonNode> response = post("/cpush", headers, body);
        long endTimestamp = new Date().getTime();
        synchronized (requestStatisticsList) {
            RequestStatistics requestStatistics = new RequestStatistics(serialNumber, "cpush", startTimestamp, endTimestamp);
            requestStatisticsList.add(requestStatistics);
        }
        LOGGER.warn("Master - cpush - body:" + body);
        LOGGER.warn("Master - cpush - response:[" + response +"]");
        return response;
    }

    private synchronized String old_cpush(String body, Map<String, String> headers){
        long startTimestamp = new Date().getTime();

        String response = POST("/cpush", headers, body);
        long endTimestamp = new Date().getTime();
        synchronized (requestStatisticsList) {
            RequestStatistics requestStatistics = new RequestStatistics(serialNumber, "cpush", startTimestamp, endTimestamp);
            requestStatisticsList.add(requestStatistics);
        }
        LOGGER.warn("Master - cpush - body:" + body);
        LOGGER.warn("Master - cpush - response:[" + response +"]");
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

            System.out.println("Master [" + serialNumber + "] - Puller - LIVE");
            Random rand = new Random();
            int randomInterval = rand.nextInt(pullingOffset + 1);

            try {
                Thread.sleep(randomInterval * 1000);
            } catch (InterruptedException e) {
                //e.printStackTrace();
            }


            System.out.println("Master ["+serialNumber+"] - Puller - LIVE");
            try {
                processRequest(cpull());
            }catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void shutdown() {
            shutdown = true;
        }

    }

    @Override
    public void processRequest(HttpResponse<JsonNode> request) {
        if(request != null && !request.equals("{}")){
            LOGGER.warn("PROCESS REQUEST:" + request);

            MessageMapper messageMapper = (new Gson()).fromJson(request.getBody().toString(), MessageMapper.class);
            String applicationID = "";
            if(messageMapper != null && messageMapper.getMsg() != null && !messageMapper.getMsg().trim().equals("")) System.out.println("PROCESS REQUEST:" + messageMapper.getMsg());

            Map<String, String> headers = new HashMap<String, String>();
            Headers requestHeaders = request.getHeaders();
            //System.out.println(headers);
            headers.put("Serial-Number", serialNumber);
            List<String> applicationIdHeader = requestHeaders.get("Application-ID");
            if(applicationIdHeader != null && !applicationIdHeader.isEmpty()) {
                applicationID = applicationIdHeader.get(0);
                headers.put("Application-ID",applicationIdHeader.get(0));
            } else {
                return;
            }

            headers.put("Content-Type", "application/json");
            headers.put("Content-Type", "application/json");
            headers.put("Broadcast", "true");

            if(messageMapper.getMsg() == null || messageMapper.getMsg().trim().isEmpty()){
                return;
            }

            IMessageProcessor messageProcessor = new CloudiaMessageProcessor();
            final CloudiaMessage processedMessage = (CloudiaMessage) messageProcessor.processMessage(messageMapper.getMsg());

            switch (processedMessage.getCommand()) {
                case LOCK: {
                    LOGGER.warn("#TAG Master [ " + serialNumber + " ]: change lock status required to LOCK");
                    processLock(processedMessage.getData(), true);
                    messageMapper.setMsg(createStatusMessage(getMasterStatus()));
                    processResponse(cpush(messageMapper.toJson(), headers));
                    break;
                }
                case UNLOCK: {
                    LOGGER.warn("#TAG Master [ " + serialNumber + " ]: change lock status required to UNLOCK");
                    processLock(processedMessage.getData(), false);
                    messageMapper.setMsg(createStatusMessage(getMasterStatus()));
                    processResponse(cpush(messageMapper.toJson(), headers));
                    break;
                }
                default:
                    LOGGER.warn("#TAG Master COMMAND + [ " + processedMessage.getCommand() + " ] NOT FOUND.");
                    break;
            }

            LOGGER.info("PC CENTRAL-SN [" + serialNumber + "] APP-ID [" + applicationID + "]: " + request);
        }
    }

    @Override
    public void processRequest(String response) throws Exception {
        throw new Exception("Not yet implemented");
    }

    @Override
    public void processResponse(HttpResponse<JsonNode> response) {

        if(response == null || response.getBody() == null || response.getBody().toString().trim().isEmpty()) {
            return;
        }
        LOGGER.warn("Master - processResponse - response:" + response);
        MessageMapper msg = new Gson().fromJson(response.getBody().toString(), MessageMapper.class);
        if(msg != null && msg.getMsg() != null && !msg.getMsg().trim().equals("")) System.out.println("PROCESS RESPONSE:" + msg.getMsg());
        LOGGER.warn("Master - processResponse - msg:" + msg);
        LOGGER.warn("Master - processResponse - msg.getMsg():" + (msg == null ? "null" : msg.getMsg()));
        IMessageProcessor messageProcessor = new CloudiaMessageProcessor();
        final CloudiaMessage processedMessage = (CloudiaMessage) messageProcessor.processMessage(msg.getMsg());

        switch (processedMessage.getCommand()) {
            case DISCONNECT:
                LOGGER.warn("#TAG Master [ " + serialNumber + " ]: DISCONNECT");
                processDisconnectResponse(processedMessage.getData());
                break;
            case CONNECT:
                LOGGER.warn("#TAG Master [ " + serialNumber + " ]: CONNECT");
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

    @Override
    public void processResponse(String response) throws Exception {
        throw new Exception("Not yet implemented");
    }

    synchronized String processConnectResponse(final String status) {
        final Integer statusCode = Integer.valueOf(status);
        if(statusCode == 1) {
            connected = true;
            LOGGER.warn("#TAG Master [ " + serialNumber + " ]: status connection: [ connected ].");
        } else {
            connected = false;
        }
        return status;
    }

    synchronized String processDisconnectResponse(final String status) {
        final Integer statusCode = Integer.valueOf(status);
        if(statusCode == 1) {
            connected = false;
            LOGGER.warn("#TAG Master [ " + serialNumber + " ]: status connection: [ connected ].");
        }
        return status;
    }

    synchronized String processStatusResponse(final String status) {
        // TODO FIX code mocked
        //return status;
        return OK;
    }

    private synchronized String getMasterStatus() {
        boolean lock0Status = locks[0];
        final String lock0StatusHex = String.format("%02X", lock0Status ? (byte) 0x01 : (byte) 0x02);
        boolean lock1Status = locks[1];
        final String lock1StatusHex = String.format("%02X", lock1Status ? (byte) 0x01 : (byte) 0x02);
        return lock0StatusHex + lock1StatusHex;
    }

    synchronized String createStatusMessage(String status) {
        return createMessage(STATUS, status);
    }

    synchronized String createConnectMessage(final String code) {
        return createMessage(CONNECT, code);
    }

    private synchronized String createMessage(final String command, final String data) {
        final String currentSequence = String.format("%02X", (byte) (incrementSequence() & 0xFF));
        //byte command
        final String dummyChecksum = String.format("%02X", (byte)(0xFF));

        IMessageProcessor messageProcessor = new CloudiaMessageProcessor();
        CloudiaMessage cm = new CloudiaMessage("7B", currentSequence, command, data, dummyChecksum);
        cm.recalculateChecksum();
        return messageProcessor.synthMessage(cm);
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

                System.out.println("Master [" + serialNumber + "] - Pusher - LIVE");
                if(connected) {
                    try {
                        Map<String, String> headers = new HashMap<String, String>();
                        headers.put("Serial-Number", serialNumber);
                        headers.put("Content-Type", "application/json");
                        headers.put("Broadcast", "true");

                        MessageMapper messageMapper = new MessageMapper();
                        locks[new Random().nextInt(2)] = new Random().nextBoolean();
                        messageMapper.setMsg(createStatusMessage(getMasterStatus()));

                        processResponse(cpush(messageMapper.toJson(), headers));
                        LOGGER.info("PC CENTRAL-SN [" + serialNumber + "] APP-ID [ broadcast ]: " + messageMapper.getMsg());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        public void shutdown() {
            shutdown = true;
        }
    }

    private HttpResponse<JsonNode> connectToCloudService(){
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Serial-Number", serialNumber);
        headers.put("Content-Type", "application/json");
        HttpResponse<JsonNode> response = null;
        try {
            MessageMapper messageMapper = new MessageMapper();
            messageMapper.setMsg(createConnectMessage(""));
            response = post("/cconn", headers, messageMapper.toJson());
        }catch (Exception e){
            e.printStackTrace();
        }
        return response;
    }

}
