package br.com.example.bean;

import br.com.example.statistics.IRequestStatisticallyProfilable;
import br.com.example.statistics.IStatistics;
import br.com.example.statistics.RequestStatistics;
import br.com.processor.ComplexMessage;
import br.com.processor.ComplexMessageProcessor;
import br.com.processor.ComunicationProtocol;
import br.com.processor.IMessageProcessor;
import br.com.processor.mapper.SimpleMessageMapper;
import com.google.gson.Gson;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static br.com.example.request.Request.GET;
import static br.com.example.request.Request.POST;
import static br.com.processor.ComplexMessage.*;

/**
 * Copyright 2016 Cantinho. All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * @author Samir Trajano Feitosa
 * @author Jordão Ezequiel Serafim de Araújo
 * @author Cantinho - Github https://github.com/Cantinho
 * @since 2016
 * @license Apache 2.0
 *
 * This file is licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.  For additional information regarding
 * copyright in this work, please see the NOTICE file in the top level
 * directory of this distribution.
 *
 */
public class Slave implements IRequestStatisticallyProfilable, ComunicationProtocol {

    private final static Logger LOGGER = LoggerFactory.getLogger(Slave.class);

    private String slaveId;
    private String masterSN;
    private final int MINIMUM_PULLING_INTERVAL;
    private final int PULLING_OFFSET;
    private final int MINIMUM_PUSHING_INTERVAL;
    private final int PUSHING_OFFSET;
    private List<Future> runnablePullerFutures;
    private List<Future> runnablePusherFutures;

    private List<IStatistics> requestStatisticsList = new LinkedList<IStatistics>();

    private static byte sequence = 0;

    // TODO change this to package attributes;
    /**
     * Ficar imprimindo o status local (lock) após pooling.
     * Ao conectar, enviar mensagem para o master pedindo status.
     * Quando spull do lock for chamando, enviar mensagem para a de trocar o estado do lock.
     * Essa mensagem chega no master. Este processa e envia resposta para slave.
     * O Slave recebe a mensagem do status referente à mensagem enviada e atualiza seu status (lock).
     * Imprimir lock do slave.
     * Fazer o mesmo a cada pooling no Master
     */
    private boolean[] locks = {false, false}; // ficar estado do master;
    private boolean connected = false;

    public Slave(String slaveId, String masterSN, int minimumPullingInterval, int pullingOffset,
                 int minimumPushingInterval, int pushingOffset) {
        this.slaveId = slaveId;
        this.masterSN = masterSN;
        this.MINIMUM_PULLING_INTERVAL = minimumPullingInterval;
        this.PULLING_OFFSET = pullingOffset;
        this.MINIMUM_PUSHING_INTERVAL = minimumPushingInterval;
        this.PUSHING_OFFSET = pushingOffset;
    }

    public Slave(String slaveId, String masterSN) {
        this(slaveId, masterSN, 3, 5, 5, 5);
    }

    private synchronized static byte incrementSequence() {
        sequence += (byte) 0x01;
        return sequence;
    }

    private synchronized byte getSequence() {
        return sequence;
    }

    public void init() {


        processResponse(connectToCloudService());

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
     * executes POST /spush for devices
     * @return
     */
    private synchronized String spush(String body){
        long startTimestamp = new Date().getTime();
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Master-SN", masterSN);
        headers.put("Slave-ID", slaveId);
        headers.put("Content-Type", "application/json");

        String response = POST("/spush", headers, body);

        long endTimestamp = new Date().getTime();
        synchronized (requestStatisticsList) {
            RequestStatistics requestStatistics = new RequestStatistics(masterSN + "_" + slaveId, "spush", startTimestamp, endTimestamp);
            requestStatisticsList.add(requestStatistics);
        }
        return response;
    }

    /**
     * executes POST /spull for devices
     * @return
     */
    private synchronized String spull(Integer messageAmount){
        long startTimestamp = new Date().getTime();
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Master-SN", masterSN);
        headers.put("Slave-ID", slaveId);
        headers.put("Message-Amount", messageAmount.toString());

        String response = GET("/spull", headers);

        long endTimestamp = new Date().getTime();
        synchronized (requestStatisticsList) {
            RequestStatistics requestStatistics = new RequestStatistics(masterSN + "_" + slaveId, "spull - wbody", startTimestamp, endTimestamp);
            requestStatisticsList.add(requestStatistics);
        }
        return response;
    }

    public String getSlaveId() {
        return slaveId;
    }

    public void setSlaveId(String slaveId) {
        this.slaveId = slaveId;
    }

    public String getMasterSN() {
        return masterSN;
    }

    public void setMasterSN(String centralSerialNumber) {
        this.masterSN = centralSerialNumber;
    }

    public List<IStatistics> collectStatistics() {
        return requestStatisticsList;
    }



    @Override
    public void processRequest(String request) {

    }

    @Override
    public void processRequest(HttpResponse<JsonNode> request) {
        if(request != null && request.getBody() != null &&
                !request.getBody().toString().trim().isEmpty() &&
                !request.getBody().toString().trim().equals("{}")){

            SimpleMessageMapper msg = new Gson().fromJson(request.getBody().toString(), SimpleMessageMapper.class);
            System.out.println("SLV - processRequest - msg:" + msg.getMessage());
            if(msg.getMessage() != null && !msg.getMessage().trim().isEmpty()) {
                IMessageProcessor messageProcessor = new ComplexMessageProcessor();
                final ComplexMessage processedMessage = (ComplexMessage) messageProcessor.processMessage(msg.getMessage());

                switch (processedMessage.getCommand()) {
                    case STATUS:
                        processStatusRequest(processedMessage.getData());
                        break;
                    default:
                        LOGGER.warn("#TAG Slave COMMAND + [ " + processedMessage.getCommand() + " ] NOT FOUND.");
                        break;
                }
            }
        }
    }

    @Override
    public void processResponse(String response) {

    }

    @Override
    public void processResponse(HttpResponse<JsonNode> response) {
        SimpleMessageMapper msg = new Gson().fromJson(response.getBody().toString(), SimpleMessageMapper.class);
        IMessageProcessor messageProcessor = new ComplexMessageProcessor();
        final ComplexMessage processedMessage = (ComplexMessage) messageProcessor.processMessage(msg.getMessage());

        switch (processedMessage.getCommand()) {
            case DISCONNECT:
                LOGGER.warn("#TAG Slave [ " + slaveId + " ] - processResponse: DISCONNECT");
                processDisconnectResponse(processedMessage.getData());
                break;
            case CONNECT:
                LOGGER.warn("#TAG Slave [ " + slaveId + " ] - processResponse: CONNECT");
                processConnectResponse(processedMessage.getData());
                if(connected) {
                    processResponse(createStatusMessage());
                }
                break;
            case STATUS:
                LOGGER.warn("#TAG Slave [ " + slaveId + " ] - processResponse: STATUS");
                processStatusResponse(processedMessage.getData());
                break;
            case LOCK:
                LOGGER.warn("#TAG Slave [ " + slaveId + " ] - processResponse: LOCK");
                processLockResponse(processedMessage.getData());
                break;
            case UNLOCK:
                LOGGER.warn("#TAG Slave [ " + slaveId + " ] - processResponse: UNLOCK");
                processUnlockResponse(processedMessage.getData());
                break;
            default:
                LOGGER.warn("#TAG Slave COMMAND + [ " + processedMessage.getCommand() + " ] NOT FOUND.");
        }
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
            try {
                processRequest(spull(1));
            } catch (Exception e) {
                e.printStackTrace();
            }

            //LOGGER.info("PA PULL MASTER-SN [" + masterSN + "] SLAVE-ID [" + slaveId + "]: " + response );
        }

        public void shutdown() {
            shutdown = true;
        }
    }

    synchronized String createStatusMessage() {
        return createMessage(STATUS, "");
    }

    synchronized String createLockMessage(int partition) {
        return createGenericLockMessage(partition, true);
    }

    synchronized String createUnlockMessage(int partition) {
        return createGenericLockMessage(partition, false);
    }

    synchronized String createConnectMessage() {
        return createMessage(CONNECT, "");

    }

    synchronized String createDisconnectMessage() {
        return createMessage(DISCONNECT, "");
    }

    synchronized String createMessage(final String command, final String data) {
        final String currentSequence = String.format("%02X", (byte) (incrementSequence() & 0xFF));
        //byte command
        final String dummyChecksum = String.format("%02X", (byte) 0x00);
        ComplexMessage ComplexMessage = new ComplexMessage(HEADER, currentSequence, command, data, dummyChecksum);
        ComplexMessage.recalculateChecksum();
        return ComplexMessage.getMessage();
    }

    synchronized String createGenericLockMessage(int partition, boolean lock) {
        return createMessage((lock ? LOCK : UNLOCK), String.format("%02X", (byte) (partition & 0xFF)));
    }

    synchronized String processConnectResponse(final String status) {
        final Integer statusCode = Integer.valueOf(status);
        if(statusCode == 1) {
            connected = true;
            LOGGER.warn("#TAG Slave [ " + slaveId + " ]: status connection: [ connected ].");
        } else {
            connected = false;
        }
        return status;
    }

    synchronized String processDisconnectResponse(final String status) {
        final Integer statusCode = Integer.valueOf(status);
        if(statusCode == 1) {
            connected = false;
            LOGGER.warn("#TAG Slave [ " + slaveId + " ]: status connection: [ connected ].");
        }
        return status;
    }

    synchronized String processStatusResponse(final String status) {
        return status;
    }

    synchronized String processLockResponse(final String status) {
        return status;
    }

    synchronized String processUnlockResponse(final String status) {
        return status;
    }

    synchronized String processStatusRequest(final String data) {
        final String lock0 = data.substring(0, 2);
        final String lock1 = data.substring(2, 4);
        try {
            locks[0] = Integer.valueOf(lock0) == 1 ? true : false;
            locks[1] = Integer.valueOf(lock1) == 1 ? true : false;
            LOGGER.warn("#TAG Slave [ " + slaveId + " ]: status locks[0]:[ " + locks[0] + " ] locks[1]:[ " + locks[1] + " ].");
            return OK;
        } catch (Exception e) {
            LOGGER.error("Error when parsing lock string to boolean");
            e.printStackTrace();
        }
        return ERROR;
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

                SimpleMessageMapper SimpleMessageMapper = new SimpleMessageMapper();
                if(connected) {
                    final int randomLock = new Random().nextInt(2);
                    if(locks[randomLock]) {
                        SimpleMessageMapper.setMessage(createUnlockMessage(randomLock));
                        processResponse(spush(SimpleMessageMapper.toJson()));
                        LOGGER.warn("#TAG Slave [ " + slaveId + " ]: status locks[" + randomLock + "]: [ CHANGE lock REQUIRED ] to [ UNLOCK ].");
                    } else {
                        SimpleMessageMapper.setMessage(createLockMessage(randomLock));
                        processResponse(spush(SimpleMessageMapper.toJson()));
                        LOGGER.warn("#TAG Slave [ " + slaveId + " ]: status locks[" + randomLock + "]: [ CHANGE lock REQUIRED ] to [ LOCK ].");
                    }
                }
            }
        }

        public void shutdown() {
            shutdown = true;
        }
    }

    private String connectToCloudService(){
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Master-SN", masterSN);
        headers.put("Slave-ID", slaveId);
        headers.put("Content-Type", "application/json");
        String response = "";
        try {
            SimpleMessageMapper SimpleMessageMapper = new SimpleMessageMapper();
            SimpleMessageMapper.setMessage(createConnectMessage());
            response = POST("/sconn", headers, SimpleMessageMapper.toJson());
        }catch (Exception e){
            e.printStackTrace();
        }

        return response;
    }

    private String disconnectFromCloudService() {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Master-SN", masterSN);
        headers.put("Slave-ID", slaveId);
        headers.put("Content-Type", "application/json");
        String response = "";
        try {
            SimpleMessageMapper SimpleMessageMapper = new SimpleMessageMapper();
            SimpleMessageMapper.setMessage(createDisconnectMessage());
            response = POST("/sconn", headers, SimpleMessageMapper.toJson());
        }catch (Exception e){
            e.printStackTrace();
        }

        return response;
    }


}
