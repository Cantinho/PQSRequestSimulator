package br.com.example.bean;


import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static br.com.example.request.Request.GET;
import static br.com.example.request.Request.POST;

/**
 * Created by jordao on 27/11/16.
 */
public class Master {

    private final static Logger LOGGER = LoggerFactory.getLogger(Master.class);

    private String serialNumber;
    private final int minimumPullingInterval;
    private final int pullingOffset;
    private final int minimumPushingInterval;
    private final int pushingOffset;
    private Puller puller;
    private Pusher pusher;

    public Master(String serialNumber, int minimumPullingInterval, int pullingOffset,
                  int minimumPushingInterval, int pushingOffset) {
        this.serialNumber = serialNumber;
        this.minimumPullingInterval = minimumPullingInterval;
        this.pullingOffset = pullingOffset;
        this.minimumPushingInterval = minimumPushingInterval;
        this.pushingOffset = pushingOffset;
    }

    public Master(String serialNumber) {
        this(serialNumber, 8, 4, 4, 6);
    }

    public void init() {
        puller = createPuller();
        pusher = createPusher();
    }

    public void start() {
        puller.run();
        pusher.run();
    }

    public void stop() {
        try {
            puller.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            pusher.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Puller createPuller() {
        return new Puller();
    }

    private Pusher createPusher() {
        return new Pusher();
    }





    /**
     * executes GET /pull for centrals
     * @return
     */
    private String pull(){

        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Serial-Number", serialNumber);

        String response = GET("/pull", headers);

        return response;
    }

    /**
     * executes POST /pc for centrals
     * @param message
     * @return
     */
    private String pc(Message message){
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Serial-Number", serialNumber);
        headers.put("Application-ID", message.getApplicationID());
        String body = message.getMessage();

        String response = POST("/pc", headers, body);

        return response;
    }

    public void run() {
        String response = pull();
        LOGGER.info("PULL CENTRAL-SN [" + serialNumber + "]: " + response);
        if(!response.equals("{}")){
            Message message = (new Gson()).fromJson(response, Message.class);
            response = pc(message);
            LOGGER.info("PC CENTRAL-SN [" + serialNumber + "] APP-ID [" + message.getApplicationID() + "]: " + response);
        }
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    class Puller extends Thread implements Runnable {
        private volatile boolean shutdown = false;
        public void run() {
            if(shutdown) {
                try {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            String response = pull();
            LOGGER.info("PULL CENTRAL-SN [" + serialNumber + "]: " + response);
            if(!response.equals("{}")){
                Message message = (new Gson()).fromJson(response, Message.class);
                response = pc(message);
                LOGGER.info("PC CENTRAL-SN [" + serialNumber + "] APP-ID [" + message.getApplicationID() + "]: " + response);
            }
        }

        public void shutdown() {
            shutdown = true;
        }
    }


    class Pusher extends Thread implements Runnable {
        private volatile boolean shutdown = false;
        public void run() {
            if(shutdown) {
                try {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            // do push
        }

        public void shutdown() {
            shutdown = true;
        }
    }

}
