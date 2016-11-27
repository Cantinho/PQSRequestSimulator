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
    private final static int MASTERS_AMOUNT = 10;
    /** number of slaves allowed to speak to each central */
    private final static int AMOUNT_OF_SLAVE_PER_MASTER = 3;

    private static List<Master> masters;
    private static List<Slave> slaves;

    /** We create the number of masters defined above */
    public static void executeMasters(){
        masters = new ArrayList<Master>();
        for (int i = 0; i < MASTERS_AMOUNT; i++){
            Master master = new Master("SN-"+i);
            master.init();
            masters.add(master);
            master.start();
        }
    }
    /** for each master we create a new slave accordingly to the amount defined above */
    public static void executeSlaves(){
        slaves = new ArrayList<Slave>();
        for (int i = 0; i < MASTERS_AMOUNT; i++){
            for (int j = 0; j < AMOUNT_OF_SLAVE_PER_MASTER; j++){
                Slave slave = new Slave("APPID-"+j, "SN-"+i);
                slave.init();
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

    public static void startAll() {
        executeSlaves();
        executeMasters();
    }

    public static void stopAll() {
        stopMasters();
        stopSlaves();
    }


    /** good and old main method =P */
    public static void main(String[] args) throws InterruptedException {

        startAll();
        // TODO after some N secods we have to stop all threads and collect results.
        Thread.sleep(50000);
        stopAll();


    }



}
