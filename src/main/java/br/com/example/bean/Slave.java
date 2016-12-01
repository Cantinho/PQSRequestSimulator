package br.com.example.bean;

import br.com.example.statistics.IRequestStatisticallyProfilable;
import br.com.example.statistics.IStatistics;
import br.com.example.statistics.RequestStatistics;
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
public class Slave implements IRequestStatisticallyProfilable {

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
        this(applicationID, masterSerialNumber, 2, 5, 2, 5);
    }

    public void init() {
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
     * executes POST /pa for devices
     * @return
     */
    private String pa(String body){
        long startTimestamp = new Date().getTime();
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Serial-Number", masterSerialNumber);
        headers.put("Application-ID", applicationID);


        String response = POST("/pa", headers, body);
        long endTimestamp = new Date().getTime();
        synchronized (requestStatisticsList) {
            RequestStatistics requestStatistics = new RequestStatistics(masterSerialNumber + "_" + applicationID, body == null ? "pa - wbody" : "pa - nobody", startTimestamp, endTimestamp);
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

            String response = pa(null);
            LOGGER.info("PA PULL CENTRAL-SN [" + masterSerialNumber + "] APP-ID [" + applicationID + "]: " + response);
        }

        public void shutdown() {
            shutdown = true;
        }
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

                String body = "7B4CAAABBBCCCDDDEEEFFF";
                String response = pa(body);
                LOGGER.info("PA POST CENTRAL-SN [" + masterSerialNumber + "] APP-ID [" + applicationID + "]: " + response);
            }
        }

        public void shutdown() {
            shutdown = true;
        }
    }

}
