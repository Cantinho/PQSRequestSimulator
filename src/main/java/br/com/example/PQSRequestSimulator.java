package br.com.example;

import br.com.example.statistics.IStatistics;
import br.com.example.bean.Master;
import br.com.example.bean.Slave;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.*;

/**
 * Created by jordao on 27/11/16.
 */
public class PQSRequestSimulator {

    /** number of centrals connected to the cloud service */
    private final static int MASTERS_AMOUNT = 1;
    /** number of slaves allowed to speak to each central */
    private final static int AMOUNT_OF_SLAVE_PER_MASTER = 0;

    private static List<Master> masters;
    private static List<Slave> slaves;

    /** We create the number of masters defined above */
    public static void executeMasters(){
        masters = new ArrayList<Master>();
        for (int i = 0; i < MASTERS_AMOUNT; i++){
//            Master master = new Master("SN-"+i);
            Master master = new Master("500");
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


    public static void stopMasters(int runnableType) {
        Iterator<Master> masterIterator = masters.iterator();
        while(masterIterator.hasNext()) {
            try {
                Master master = masterIterator.next();
                if(runnableType == 1) {
                    master.stopPullers();
                } else if(runnableType == 2) {
                    master.stopPushers();
                } else {
                    master.stopPullers();
                    master.stopPushers();
                }
            } catch (Exception e) {}
        }
    }

    public static void stopSlaves(int runnableType) {
        Iterator<Slave> slaveIterator = slaves.iterator();
        while(slaveIterator.hasNext()) {
            try {
                Slave slave = slaveIterator.next();
                if(runnableType == 1) {
                    slave.stopPullers();
                } else if(runnableType == 2) {
                    slave.stopPushers();
                } else {
                    slave.stopPullers();
                    slave.stopPushers();
                }
            } catch (Exception e) {}
        }
    }

    public static void startAll() {
        executeSlaves();
        executeMasters();
    }

    public static void stopAll() {
        stopMasters(0);
        stopSlaves(0);
    }

    public static List<IStatistics> collectMasterStatistics() {
        List<IStatistics> masterStatistics = new ArrayList<IStatistics>();
        Iterator<Master> mastersIterator = masters.iterator();
        synchronized (masters) {
            while (mastersIterator.hasNext()) {
                Master master = mastersIterator.next();
                List<IStatistics> currentMasterStatistics = master.collectStatistics();
                Iterator<IStatistics> currentMasterStatisticsIterator = currentMasterStatistics.iterator();
                while (currentMasterStatisticsIterator.hasNext()) {
                    masterStatistics.add(currentMasterStatisticsIterator.next());
                }
            }
        }
        return masterStatistics;
    }

    public static List<IStatistics> collectSlaveStatistics() {
        List<IStatistics> slaveStatistics = new ArrayList<IStatistics>();
        Iterator<Slave> slaveIterator = slaves.iterator();
        synchronized (slaves) {
            while (slaveIterator.hasNext()) {
                Slave slave = slaveIterator.next();
                List<IStatistics> currentSlaveStatistics = slave.collectStatistics();
                Iterator<IStatistics> currentSlaveStatisticsIterator = currentSlaveStatistics.iterator();
                while (currentSlaveStatisticsIterator.hasNext()) {
                    if(currentSlaveStatisticsIterator != null) {
                        slaveStatistics.add(currentSlaveStatisticsIterator.next());
                    }
                }
            }
        }
        return slaveStatistics;
    }

    public static List<IStatistics> collectAllStatistics() {
        List<IStatistics> allStatistics = new ArrayList<IStatistics>(collectMasterStatistics());
        allStatistics.addAll(collectSlaveStatistics());
        return allStatistics;
    }


    /** good and old main method =P */
    public static void main(String[] args) throws InterruptedException {

        startAll();
        // TODO after some N secods we have to stop all threads and collect results.
        Thread.sleep(Integer.MAX_VALUE);

        stopAll();
        System.out.println("PQSRequestSimulator Statistics\n\n");
        System.out.println("Masters amount:" + MASTERS_AMOUNT);
        System.out.println("Slaves per Master:" + AMOUNT_OF_SLAVE_PER_MASTER);
        System.out.println("Sequence; StartTime; EndTime; TotalTime; Label; Message\n");
        List<IStatistics> statistics = collectAllStatistics();
        Iterator<IStatistics> iStatisticsIterator = statistics.iterator();
        while(iStatisticsIterator.hasNext()) {
            System.out.println(iStatisticsIterator.next().csv());
        }
        System.out.println(collectAllStatistics() + "\n");

        //programOne();
    }




    private static void programOne() {
        System.out.println("Program One\n");
        System.out.println("Starting all...");
        startAll();
        int millis = 20000;
        System.out.println("Waiting for " + millis/1000 + " seconds...");
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {}

        System.out.println("Stopping slave pushers...");
        //stopSlaves(2);
        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Stopping slave pullers...");
        //stopSlaves(1);
        try {
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Stopping all masters...");
        stopMasters(0);

        System.out.println("\nPrinting statistics...");
        System.out.println("PQSRequestSimulator Statistics\n");
        List<IStatistics> statistics = collectAllStatistics();
        Iterator<IStatistics> iStatisticsIterator = statistics.iterator();
        while(iStatisticsIterator.hasNext()) {
            System.out.println(iStatisticsIterator.next().csv());
        }
    }


}
