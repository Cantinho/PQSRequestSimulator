package br.com.example;

import br.com.example.bean.Central;
import br.com.example.bean.Device;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by jordao on 27/11/16.
 */
public class ExecuteRequests {

    /** number of centrals connected to the cloud service */
    private final static int CENTRALS_AMOUNT = 10;
    /** number of devices allowed to speak to each central */
    private final static int AMOUNT_OF_DEVICES_PER_CENTRAL = 3;
    /** interval time in which each central is going to PULL */
    private final static int PULLING_INTERVAL = 8;
    /** interval time in which each device is going do execute a POST action */
    private final static int PA_INTERVAL = 10;


    /** We create the number of central defined above */
    public static void executeCentrals(){
        for (int i = 0; i < CENTRALS_AMOUNT; i++){
            Central central = new Central("SN-"+i);

            ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
            executor.scheduleAtFixedRate(central, 0, PULLING_INTERVAL, TimeUnit.SECONDS);
        }
    }
    /** for each central we create a new device accordingly to the amount defined above */
    public static void executeDevices(){
        for (int i = 0; i < CENTRALS_AMOUNT; i++){
            for (int j = 0; j < AMOUNT_OF_DEVICES_PER_CENTRAL; j++){
                Device device = new Device("APPID-"+j, "SN-"+i);

                ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
                executor.scheduleAtFixedRate(device, 0, PA_INTERVAL, TimeUnit.SECONDS);
            }
        }
    }

    /** good and old main method =P */
    public static void main(String[] args){
        executeDevices();
        executeCentrals();
    }



}
