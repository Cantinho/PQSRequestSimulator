package br.com.example.bean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static br.com.example.request.Request.POST;

/**
 * Created by jordao on 27/11/16.
 */
public class Device implements Runnable {

    private final static Logger LOGGER = LoggerFactory.getLogger(Device.class);

    private String applicationID;
    private String centralSerialNumber;

    public Device(String applicationID, String centralSerialNumber) {
        this.applicationID = applicationID;
        this.centralSerialNumber = centralSerialNumber;
    }

    /**
     * executes POST /pa for devices
     * @return
     */
    private String pa(){
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Serial-Number", centralSerialNumber);
        headers.put("Application-ID", applicationID);
        String body = "7B4CAAABBBCCCDDDEEEFFF";

        String response = POST("/pa", headers, body);

        return response;
    }

    public void run() {
        String response = pa();
        LOGGER.info("PA CENTRAL-SN ["+centralSerialNumber+"] APP-ID ["+applicationID+"]: " + response);
    }

    public String getApplicationID() {
        return applicationID;
    }

    public void setApplicationID(String applicationID) {
        this.applicationID = applicationID;
    }

    public String getCentralSerialNumber() {
        return centralSerialNumber;
    }

    public void setCentralSerialNumber(String centralSerialNumber) {
        this.centralSerialNumber = centralSerialNumber;
    }

}
