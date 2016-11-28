package br.com.example.bean;


import br.com.example.statistics.IRequestStatisticallyProfilable;
import br.com.example.statistics.IStatistics;
import br.com.example.statistics.RequestStatistics;
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
public class Master implements IRequestStatisticallyProfilable {

    private final static Logger LOGGER = LoggerFactory.getLogger(Master.class);
    private static int statisticalSequence = 0;

    private String serialNumber;
    private final int MINIMUM_PULLING_INTERVAL;
    private final int PULLING_OFFSET;
    private final int MINIMUM_PUSHING_INTERVAL;
    private final int PUSHING_OFFSET;
    private List<Future> runnableFutures;

    private List<IStatistics> requestStatisticsList = new LinkedList<IStatistics>();

    public Master(String serialNumber, int minimumPullingInterval, int pullingOffset,
                  int minimumPushingInterval, int pushingOffset) {
        this.serialNumber = serialNumber;
        this.MINIMUM_PULLING_INTERVAL = minimumPullingInterval;
        this.PULLING_OFFSET = pullingOffset;
        this.MINIMUM_PUSHING_INTERVAL = minimumPushingInterval;
        this.PUSHING_OFFSET = pushingOffset;
    }

    public Master(String serialNumber) {
        this(serialNumber, 1, 0, 1, 6);
    }

    public void init() {
        runnableFutures = new ArrayList<Future>();
    }

    public void start() {
        Runnable puller = createPuller();
        Runnable pusher = createPusher();
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(2);
        Future<?> pullerFuture = executorService.scheduleAtFixedRate(puller, 0, MINIMUM_PULLING_INTERVAL, TimeUnit.SECONDS);

        Future<?> pusherFuture = executorService.scheduleAtFixedRate(pusher, 0, MINIMUM_PUSHING_INTERVAL, TimeUnit.SECONDS);
        runnableFutures.add(pullerFuture);
        runnableFutures.add(pusherFuture);
    }

    public void stop() {
        Iterator<Future> runnableFutureIterator = runnableFutures.iterator();
        while(runnableFutureIterator.hasNext()) {
            try {
                runnableFutureIterator.next().cancel(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            runnableFutures.clear();
            runnableFutures = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Runnable createPuller() {
        return new MasterPuller(PULLING_OFFSET);
    }

    private Runnable createPusher() {
        return new MasterPusher(PUSHING_OFFSET, false);
    }


    /**
     * executes GET /pull for centrals
     * @return
     */
    private String pull(){
        long startTimestamp = new Date().getTime();
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Serial-Number", serialNumber);

        String response = GET("/pull", headers);
        long endTimestamp = new Date().getTime();
        RequestStatistics requestStatistics = new RequestStatistics("pull", "", endTimestamp - startTimestamp);
        requestStatisticsList.add(requestStatistics);
        return response;
    }

    /**
     * executes POST /pc for centrals
     * @param message
     * @return
     */
    private String pc(Message message, Map<String, String> headers){
        long startTimestamp = new Date().getTime();
        headers.put("Application-ID", message.getApplicationID());
        String body = message.getMessage();

        String response = POST("/pc", headers, body);
        long endTimestamp = new Date().getTime();
        RequestStatistics requestStatistics = new RequestStatistics("pc", "", endTimestamp - startTimestamp);
        requestStatisticsList.add(requestStatistics);
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
                e.printStackTrace();
            }

            String response = pull();
            LOGGER.info("PULL CENTRAL-SN [" + serialNumber + "]: " + response);
            if(!response.equals("{}")){
                Map<String, String> headers = new HashMap<String, String>();
                headers.put("Serial-Number", serialNumber);

                Message message = (new Gson()).fromJson(response, Message.class);
                response = pc(message, headers);
                LOGGER.info("PC CENTRAL-SN [" + serialNumber + "] APP-ID [" + message.getApplicationID() + "]: " + response);
            }
        }

        public void shutdown() {
            shutdown = true;
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
                    e.printStackTrace();
                }

                Map<String, String> headers = new HashMap<String, String>();
                headers.put("Serial-Number", serialNumber);
                headers.put("Broadcast", "true");

                Message message = new Message(serialNumber, null, String.valueOf(new Date().getTime()), "10", "7B43FFFBBBCCCAAADDD");
                String response = pc(message, headers);
                LOGGER.info("PC CENTRAL-SN [" + serialNumber + "] APP-ID [" + message.getApplicationID() + "]: " + response);
            }
        }

        public void shutdown() {
            shutdown = true;
        }
    }

}
