package br.com.example;

import br.com.example.bean.Master;
import br.com.example.bean.Slave;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by jordao on 27/11/16.
 */
public class ExecuteRequests {

    /** number of centrals connected to the cloud service */
    private final static int CENTRALS_AMOUNT = 10;
    /** number of slaves allowed to speak to each central */
    private final static int AMOUNT_OF_DEVICES_PER_CENTRAL = 3;
    /** interval time in which each central is going to PULL */
    private final static int PULLING_INTERVAL = 8;
    /** interval time in which each device is going do execute a POST action */
    private final static int PA_INTERVAL = 10;

    private static List<Master> masters;
    private static List<Slave> slaves;

    /** We create the number of central defined above */
    public static void executeCentrals(){
        masters = new ArrayList<Master>();
        for (int i = 0; i < CENTRALS_AMOUNT; i++){
            Master master = new Master("SN-"+i);
            masters.add(master);
            master.start();
        }
    }
    /** for each central we create a new device accordingly to the amount defined above */
    public static void executeDevices(){
        slaves = new ArrayList<Slave>();
        for (int i = 0; i < CENTRALS_AMOUNT; i++){
            for (int j = 0; j < AMOUNT_OF_DEVICES_PER_CENTRAL; j++){
                Slave slave = new Slave("APPID-"+j, "SN-"+i);
                slaves.add(slave);
                slave.start();
            }
        }
    }

    public static void stopMasters() {
        Iterator<Master> masterIterator = masters.iterator();
        while(masterIterator.hasNext()) {
            try {
                masterIterator.next().stop();
            } catch (Exception e) {

            }
        }
    }

    public static void stopSlaves() {
        Iterator<Slave> slaveIterator = slaves.iterator();
        while(slaveIterator.hasNext()) {
            try {
                slaveIterator.next().stop();
            } catch (Exception e) {

            }
        }
    }


    /** good and old main method =P */
    public static void main(String[] args){
        executeDevices();
        executeCentrals();

        // TODO after some N secods we have to stop all threads and collect results.

        //stopMasters();
        //stopSlaves();

    }



}
