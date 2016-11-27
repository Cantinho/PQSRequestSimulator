package br.com.example.bean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static br.com.example.request.Request.POST;

/**
 * Created by jordao on 27/11/16.
 */
public class Slave {

    private final static Logger LOGGER = LoggerFactory.getLogger(Slave.class);

    private String applicationID;
    private String masterSerialNumber;
    private final int MINIMUM_PULLING_INTERVAL;
    private final int PULLING_OFFSET;
    private final int MINIMUM_PUSHING_INTERVAL;
    private final int PUSHING_OFFSET;
    private List<Future> runnableFutures;



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
        this(applicationID, masterSerialNumber, 8, 4, 4, 6);
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

    }

    private Runnable createPuller() {
        return new SlavePuller();
    }

    private Runnable createPusher() {
        return new SlavePusher();
    }


    /**
     * executes POST /pa for devices
     * @return
     */
    private String pa(){
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Serial-Number", masterSerialNumber);
        headers.put("Application-ID", applicationID);
        String body = "7B4CAAABBBCCCDDDEEEFFF";

        String response = POST("/pa", headers, body);

        return response;
    }

    public void run() {
        String response = pa();
        LOGGER.info("PA CENTRAL-SN [" + masterSerialNumber + "] APP-ID [" + applicationID + "]: " + response);
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

    class SlavePuller extends Thread implements Runnable {
        private volatile boolean shutdown = false;
        public void run() {
            if(shutdown) {
                try {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // TODO call puller method here
        }

        public void shutdown() {
            shutdown = true;
        }
    }


    class SlavePusher extends Thread implements Runnable {
        private volatile boolean shutdown = false;
        public void run() {
            if(shutdown) {
                try {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            // TODO call push method here
        }

        public void shutdown() {
            shutdown = true;
        }
    }

}
