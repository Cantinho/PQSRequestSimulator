package br.com.example;

import br.com.example.s.is;
import br.com.example.b.m;
import br.com.example.b.sl;

import java.util.*;

/**
 * Created by jordao on 27/11/16.
 */
public class rs {

    /** number of centrals connected to the cloud service */
    private final static int MASTERS_AMOUNT = 1;
    /** number of slaves allowed to speak to each central */
    private final static int AMOUNT_OF_SLAVE_PER_MASTER = 0;

    private static List<m> ms;
    private static List<sl> slaves;

    /** We create the number of masters defined above */
    public static void executeMasters(){
        ms = new ArrayList<m>();
        for (int i = 0; i < MASTERS_AMOUNT; i++){
//            Master master = new Master("SN-"+i);
            m m = new m("100");
            m.init();
            ms.add(m);
            m.start();
        }
    }
    /** for each master we create a new slave accordingly to the amount defined above */
    public static void executeSlaves(){
        slaves = new ArrayList<sl>();
        for (int i = 0; i < MASTERS_AMOUNT; i++){
            for (int j = 0; j < AMOUNT_OF_SLAVE_PER_MASTER; j++){
                sl slave = new sl("APPID-"+j, "SN-"+i);
                slave.init();
                slaves.add(slave);
                slave.start();
            }
        }
    }


    public static void stopMasters(int runnableType) {
        Iterator<m> masterIterator = ms.iterator();
        while(masterIterator.hasNext()) {
            try {
                m m = masterIterator.next();
                if(runnableType == 1) {
                    m.stopPullers();
                } else if(runnableType == 2) {
                    m.stopPushers();
                } else {
                    m.stopPullers();
                    m.stopPushers();
                }
            } catch (Exception e) {}
        }
    }

    public static void stopSlaves(int runnableType) {
        Iterator<sl> slaveIterator = slaves.iterator();
        while(slaveIterator.hasNext()) {
            try {
                sl slave = slaveIterator.next();
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

    public static List<is> collectMasterStatistics() {
        List<is> masterStatistics = new ArrayList<is>();
        Iterator<m> mastersIterator = ms.iterator();
        synchronized (ms) {
            while (mastersIterator.hasNext()) {
                m m = mastersIterator.next();
                List<is> currentMasterStatistics = m.collectStatistics();
                Iterator<is> currentMasterStatisticsIterator = currentMasterStatistics.iterator();
                while (currentMasterStatisticsIterator.hasNext()) {
                    masterStatistics.add(currentMasterStatisticsIterator.next());
                }
            }
        }
        return masterStatistics;
    }

    public static List<is> collectSlaveStatistics() {
        List<is> slaveStatistics = new ArrayList<is>();
        Iterator<sl> slaveIterator = slaves.iterator();
        synchronized (slaves) {
            while (slaveIterator.hasNext()) {
                sl slave = slaveIterator.next();
                List<is> currentSlaveStatistics = slave.collectStatistics();
                Iterator<is> currentSlaveStatisticsIterator = currentSlaveStatistics.iterator();
                while (currentSlaveStatisticsIterator.hasNext()) {
                    if(currentSlaveStatisticsIterator != null) {
                        slaveStatistics.add(currentSlaveStatisticsIterator.next());
                    }
                }
            }
        }
        return slaveStatistics;
    }

    public static List<is> collectAllStatistics() {
        List<is> allStatistics = new ArrayList<is>(collectMasterStatistics());
        allStatistics.addAll(collectSlaveStatistics());
        return allStatistics;
    }


    /** good and old main method =P */
    public static void main(String[] args) throws InterruptedException {

        startAll();
        // TODO after some N secods we have to stop all threads and collect results.
        Thread.sleep(Integer.MAX_VALUE);

        stopAll();
    }



}
