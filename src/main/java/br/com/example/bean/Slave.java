package br.com.example.bean;

import br.com.example.statistics.IRequestStatisticallyProfilable;
import br.com.example.statistics.IStatistics;
import br.com.example.statistics.RequestStatistics;
import br.com.processor.CloudiaMessage;
import br.com.processor.ComunicationProtocol;
import br.com.processor.IMessageProcessor;
import br.com.processor.CloudiaMessageProcessor;
import br.com.processor.mapper.MessageMapper;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static br.com.example.request.Request.GET;
import static br.com.example.request.Request.POST;
import static br.com.processor.CloudiaMessage.*;

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
            RequestStatistics requestStatistics = new RequestStatistics(masterSerialNumber + "_" + applicationID, "apush", startTimestamp, endTimestamp);
            requestStatisticsList.add(requestStatistics);
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
            RequestStatistics requestStatistics = new RequestStatistics(masterSerialNumber + "_" + applicationID, "apull - wbody", startTimestamp, endTimestamp);
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
    public void processRequest(String request) {

        if(request != null && !request.trim().isEmpty() && !request.equals("{}")){
            MessageMapper msg = new Gson().fromJson(request, MessageMapper.class);
            System.out.println("SLV - processRequest - msg:" + msg.getMsg());
            if(msg.getMsg() != null && !msg.getMsg().trim().isEmpty()) {
                IMessageProcessor messageProcessor = new CloudiaMessageProcessor();
                final CloudiaMessage processedMessage = (CloudiaMessage) messageProcessor.processMessage(msg.getMsg());

                switch (processedMessage.getCommand()) {
                    case STATUS:
                        processStatusRequest(processedMessage.getData());
                        break;
                    default:
                        LOGGER.warn("#TAG Slave COMMAND + [ " + processedMessage.getCommand() + " ] NOT FOUND.");
                        break;
                }
            }
        }
    }

    @Override
    public void processResponse(String response) {

        MessageMapper msg = new Gson().fromJson(response, MessageMapper.class);
        IMessageProcessor messageProcessor = new CloudiaMessageProcessor();
        final CloudiaMessage processedMessage = (CloudiaMessage) messageProcessor.processMessage(msg.getMsg());

        switch (processedMessage.getCommand()) {
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
                LOGGER.warn("#TAG Slave COMMAND + [ " + processedMessage.getCommand() + " ] NOT FOUND.");
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

    synchronized String createMessage(final String command, final String data) {
        final String currentSequence = String.format("%02d", Byte.parseByte( (incrementSequence() & 0xFF) + "", 16));
        //byte command
        final String dummyChecksum = String.format("%02d", (byte) 0);
        CloudiaMessage cloudiaMessage = new CloudiaMessage("7B", currentSequence, command, data, dummyChecksum);
        cloudiaMessage.recalculateChecksum();
        return cloudiaMessage.getMessage();
    }

    synchronized String createGenericLockMessage(int partition, boolean lock) {
        return createMessage((lock ? LOCK : UNLOCK), String.format("%02d", Byte.parseByte( (partition & 0xFF) + "", 16)));
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

                MessageMapper messageMapper = new MessageMapper();
                if(connected) {
                    final int randomLock = new Random().nextInt(2);
                    if(locks[randomLock]) {
                        messageMapper.setMsg(createUnlockMessage(randomLock));
                        processResponse(apush(messageMapper.toJson()));
                        LOGGER.warn("#TAG Slave [ " + applicationID + " ]: status locks[" + randomLock + "]: [ CHANGE lock REQUIRED ] to [ UNLOCK ].");
                    } else {
                        messageMapper.setMsg(createLockMessage(randomLock));
                        processResponse(apush(messageMapper.toJson()));
                        LOGGER.warn("#TAG Slave [ " + applicationID + " ]: status locks[" + randomLock + "]: [ CHANGE lock REQUIRED ] to [ LOCK ].");
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
            MessageMapper messageMapper = new MessageMapper();
            messageMapper.setMsg(createConnectMessage());
            response = POST("/aconn", headers, messageMapper.toJson());
        }catch (Exception e){
            e.printStackTrace();
        }

        return response;
    }


}
