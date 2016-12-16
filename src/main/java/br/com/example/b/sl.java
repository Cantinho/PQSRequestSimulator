package br.com.example.b;

import br.com.example.s.irs;
import br.com.example.s.is;
import br.com.example.s.rss;
import br.com.p.cm;
import br.com.p.cp;
import br.com.p.mp;
import br.com.p.cmp;
import br.com.p.p.mmm;
import com.google.gson.Gson;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static br.com.example.r.r.GET;
import static br.com.example.r.r.POST;
import static br.com.p.cm.*;

/**
 * Created by jordao on 27/11/16.
 */
public class sl implements irs, cp {

    private String applicationID;
    private String masterSerialNumber;
    private final int MINIMUM_PULLING_INTERVAL;
    private final int PULLING_OFFSET;
    private final int MINIMUM_PUSHING_INTERVAL;
    private final int PUSHING_OFFSET;
    private List<Future> runnablePullerFutures;
    private List<Future> runnablePusherFutures;

    private List<is> requestStatisticsList = new LinkedList<is>();

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

    public sl(String applicationID, String masterSerialNumber, int minimumPullingInterval, int pullingOffset,
                 int minimumPushingInterval, int pushingOffset) {
        this.applicationID = applicationID;
        this.masterSerialNumber = masterSerialNumber;
        this.MINIMUM_PULLING_INTERVAL = minimumPullingInterval;
        this.PULLING_OFFSET = pullingOffset;
        this.MINIMUM_PUSHING_INTERVAL = minimumPushingInterval;
        this.PUSHING_OFFSET = pushingOffset;
    }

    public sl(String applicationID, String masterSerialNumber) {
        this(applicationID, masterSerialNumber, 3, 5, 5, 5);
    }

    private synchronized static byte incrementSequence() {
        sequence += (byte) 0x01;
        return sequence;
    }

    private synchronized byte getSequence() {
        return sequence;
    }

    public void init() {


        processResponse(connectToCloudService());

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
     * executes POST /apush for devices
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
            rss rss = new rss(masterSerialNumber + "_" + applicationID, "apush", startTimestamp, endTimestamp);
            requestStatisticsList.add(rss);
        }
        return response;
    }

    /**
     * executes POST /apull for devices
     * @return
     */
    private synchronized String apull(Integer messageAmount){
        long startTimestamp = new Date().getTime();
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Serial-Number", masterSerialNumber);
        headers.put("Application-ID", applicationID);
        headers.put("Message-Amount", messageAmount.toString());

        String response = GET("/apull", headers);

        long endTimestamp = new Date().getTime();
        synchronized (requestStatisticsList) {
            rss rss = new rss(masterSerialNumber + "_" + applicationID, "apull - wbody", startTimestamp, endTimestamp);
            requestStatisticsList.add(rss);
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

    public List<is> collectStatistics() {
        return requestStatisticsList;
    }



    @Override
    public void processRequest(String request) {

    }

    @Override
    public void processRequest(HttpResponse<JsonNode> request) {
        if(request != null && request.getBody() != null &&
                !request.getBody().toString().trim().isEmpty() &&
                !request.getBody().toString().trim().equals("{}")){

            mmm msg = new Gson().fromJson(request.getBody().toString(), mmm.class);
            System.out.println("SLV - processRequest - msg:" + msg.getMsg());
            if(msg.getMsg() != null && !msg.getMsg().trim().isEmpty()) {
                mp messageProcessor = new cmp();
                final cm processedMessage = (cm) messageProcessor.processMessage(msg.getMsg());

                switch (processedMessage.getCommand()) {
                    case STATUS:
                        processStatusRequest(processedMessage.getData());
                        break;
                    default:
                        break;
                }
            }
        }
    }

    @Override
    public void processResponse(String response) {

    }

    @Override
    public void processResponse(HttpResponse<JsonNode> response) {
        mmm msg = new Gson().fromJson(response.getBody().toString(), mmm.class);
        mp messageProcessor = new cmp();
        final cm processedMessage = (cm) messageProcessor.processMessage(msg.getMsg());

        switch (processedMessage.getCommand()) {
            case DISCONNECT:
                processDisconnectResponse(processedMessage.getData());
                break;
            case CONNECT:
                processConnectResponse(processedMessage.getData());
                if(connected) {
                    processResponse(createStatusMessage());
                }
                break;
            case STATUS:
                processStatusResponse(processedMessage.getData());
                break;
            case LOCK:
                processLockResponse(processedMessage.getData());
                break;
            case UNLOCK:
                processUnlockResponse(processedMessage.getData());
                break;
            default:
                break;
        }
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
                processRequest(apull(1));
            } catch (Exception e) {
                e.printStackTrace();
            }

            //LOGGER.info("PA PULL CENTRAL-SN [" + masterSerialNumber + "] APP-ID [" + applicationID + "]: " + response );
        }

        public void shutdown() {
            shutdown = true;
        }
    }



    synchronized String createStatusMessage() {
        return createMessage(STATUS, "");
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

    synchronized String createDisconnectMessage() {
        return createMessage(DISCONNECT, "");
    }

    synchronized String createMessage(final String command, final String data) {
        final String currentSequence = String.format("%02X", (byte) (incrementSequence() & 0xFF));
        //byte command
        final String dummyChecksum = String.format("%02X", (byte) 0x00);
        cm cm = new cm("7B", currentSequence, command, data, dummyChecksum);
        cm.dfsfdg();
        return cm.getMessage();
    }

    synchronized String createGenericLockMessage(int partition, boolean lock) {
        return createMessage((lock ? LOCK : UNLOCK), String.format("%02X", (byte) (partition & 0xFF)));
    }

    synchronized String processConnectResponse(final String status) {
        final Integer statusCode = Integer.valueOf(status);
        if(statusCode == 1) {
            connected = true;
        } else {
            connected = false;
        }
        return status;
    }

    synchronized String processDisconnectResponse(final String status) {
        final Integer statusCode = Integer.valueOf(status);
        if(statusCode == 1) {
            connected = false;
        }
        return status;
    }

    synchronized String processStatusResponse(final String status) {
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
            return OK;
        } catch (Exception e) {
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

                mmm messageMapper = new mmm();
                if(connected) {
                    final int randomLock = new Random().nextInt(2);
                    if(locks[randomLock]) {
                        messageMapper.setMsg(createUnlockMessage(randomLock));
                        processResponse(apush(messageMapper.toJson()));
                    } else {
                        messageMapper.setMsg(createLockMessage(randomLock));
                        processResponse(apush(messageMapper.toJson()));
                    }
                }
            }
        }

        public void shutdown() {
            shutdown = true;
        }
    }

    private String connectToCloudService(){
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Serial-Number", masterSerialNumber);
        headers.put("Application-ID", applicationID);
        headers.put("Content-Type", "application/json");
        String response = "";
        try {
            mmm messageMapper = new mmm();
            messageMapper.setMsg(createConnectMessage());
            response = POST("/aconn", headers, messageMapper.toJson());
        }catch (Exception e){
            e.printStackTrace();
        }

        return response;
    }

    private String disconnectFromCloudService() {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Serial-Number", masterSerialNumber);
        headers.put("Application-ID", applicationID);
        headers.put("Content-Type", "application/json");
        String response = "";
        try {
            mmm messageMapper = new mmm();
            messageMapper.setMsg(createDisconnectMessage());
            response = POST("/aconn", headers, messageMapper.toJson());
        }catch (Exception e){
            e.printStackTrace();
        }

        return response;
    }


}
