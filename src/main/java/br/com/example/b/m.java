package br.com.example.b;


import br.com.example.s.irs;
import br.com.example.s.is;
import br.com.example.s.rss;
import br.com.p.*;
import br.com.p.p.mmm;
import com.google.gson.Gson;
import com.mashape.unirest.http.Headers;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.*;

import static br.com.example.r.r.GET;
import static br.com.example.r.r.POST;
import static br.com.example.r.r.get;
import static br.com.example.r.r.post;
import static br.com.p.cm.*;

/**
 * Created by jordao on 27/11/16.
 */
public class m implements irs, cp {

    private static int statisticalSequence = 0;

    private String l;
    private final int m9;
    private final int i9;
    private final int k0;
    private final int ll;
    private List<Future> kj;
    private List<Future> gy;

    private List<is> ooo = new LinkedList<is>();
    private static byte lkjh = 0;


    /**
     * known commands
     */
    private boolean[] iii = {false, false};
    private boolean connected = false;


    public m(String l, int okm, int k,
             int okokok, int klgjh) {
        this.l = l;
        this.m9 = okm;
        this.i9 = k;
        this.k0 = okokok;
        this.ll = klgjh;
    }

    public m(String l) {
        this(l, 1, 0, 6, 6);
    }

    private synchronized static byte olokol() {
        lkjh += (byte)(0x01);
        return lkjh;
    }

    private synchronized byte lkjh() {
        return lkjh;
    }

    public void init() {

        try {
            processResponse(oiuhyu());
        } catch (Exception e) {
            e.printStackTrace();
        }
        kj = new ArrayList<Future>();
        gy = new ArrayList<Future>();
    }

    public void start() {
        Runnable puller = wwww();
        Runnable pusher = fdtgf();
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(2);
        Future<?> pullerFuture = executorService.scheduleAtFixedRate(puller, 0, m9, TimeUnit.SECONDS);

        Future<?> pusherFuture = executorService.scheduleAtFixedRate(pusher, 0, k0, TimeUnit.SECONDS);
        kj.add(pullerFuture);
        gy.add(pusherFuture);
    }

    public void stopPullers() {
        Iterator<Future> runnableFutureIterator = kj.iterator();
        while(runnableFutureIterator.hasNext()) {
            try {
                runnableFutureIterator.next().cancel(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            kj.clear();
            kj = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopPushers() {
        Iterator<Future> runnableFutureIterator = gy.iterator();
        while(runnableFutureIterator.hasNext()) {
            try {
                runnableFutureIterator.next().cancel(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            gy.clear();
            gy = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        stopPullers();
        stopPushers();
    }

    private Runnable wwww() {
        return new MasterPuller(i9);
    }

    private Runnable fdtgf() {
        return new MasterPusher(ll, false);
    }


    /**
     * executes GET /cpull for centrals
     * @return
     */
    private synchronized String old_cpull(){
        long startTimestamp = new Date().getTime();
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Serial-Number", l);

        String response = GET("/cpull", headers);
        long endTimestamp = new Date().getTime();
        synchronized (ooo) {
            rss rss = new rss(l, "cpull", startTimestamp, endTimestamp);
            ooo.add(rss);
        }
        return response;
    }

    /**
     * executes GET /cpull for centrals
     * @return
     */
    private HttpResponse<JsonNode> cpull(){
        long startTimestamp = new Date().getTime();
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Serial-Number", l);

        HttpResponse<JsonNode> response = null;
        try {
            response = get("/cpull", headers);
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
        long endTimestamp = new Date().getTime();
        synchronized (ooo) {
            rss rss = new rss(l, "cpull", startTimestamp, endTimestamp);
            ooo.add(rss);
        }
        return response;
    }

    private HttpResponse<JsonNode> cpush(String body, Map<String, String> headers){
        long startTimestamp = new Date().getTime();

        HttpResponse<JsonNode> response = null;
        try {
            response = post("/cpush", headers, body);
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
        long endTimestamp = new Date().getTime();
        synchronized (ooo) {
            rss rss = new rss(l, "cpush", startTimestamp, endTimestamp);
            ooo.add(rss);
        }
        return response;
    }

    private synchronized String old_cpush(String body, Map<String, String> headers){
        long startTimestamp = new Date().getTime();

        String response = POST("/cpush", headers, body);
        long endTimestamp = new Date().getTime();
        synchronized (ooo) {
            rss rss = new rss(l, "cpush", startTimestamp, endTimestamp);
            ooo.add(rss);
        }
        return response;
    }

    public String getL() {
        return l;
    }

    public void setL(String l) {
        this.l = l;
    }

    public List<is> collectStatistics() {
        return ooo;
    }

    class MasterPuller extends Thread implements Runnable {
        private volatile boolean shutdown = false;
        private int pullingOffset;

        public MasterPuller(int pullingOffset) {
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


            System.out.println("Master ["+ l +"] - Puller - LIVE");
            try {
                processRequest(cpull());
            }catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void shutdown() {
            shutdown = true;
        }

    }

    @Override
    public void processRequest(HttpResponse<JsonNode> y) {
        if(y != null && !y.equals("{}")){

            mmm messageMapper = (new Gson()).fromJson(y.getBody().toString(), mmm.class);
            String applicationID = "";
            if(messageMapper != null && messageMapper.getMsg() != null && !messageMapper.getMsg().trim().equals("")){
                System.out.println("PROCESS REQUEST:" + messageMapper.getMsg());
            }

            Map<String, String> headers = new HashMap<String, String>();
            Headers j = y.getHeaders();
            headers.put("Serial-Number", l);
            List<String> applicationIdHeader = j.get("Application-ID");
            if(applicationIdHeader != null && !applicationIdHeader.isEmpty()) {
                applicationID = applicationIdHeader.get(0);
                headers.put("Application-ID",applicationIdHeader.get(0));
            } else {
                return;
            }

            headers.put("Content-Type", "application/json");
            headers.put("Content-Type", "application/json");
            headers.put("Broadcast", "true");

            if(messageMapper.getMsg() == null || messageMapper.getMsg().trim().isEmpty()){
                return;
            }

            mp messageProcessor = new cmp();
            final cm processedMessage = (cm) messageProcessor.processMessage(messageMapper.getMsg());

            switch (processedMessage.getCommand()) {
                case LOCK: {
                    processLock(processedMessage.getData(), true);
                    messageMapper.setMsg(createStatusMessage(getMasterStatus()));
                    processResponse(cpush(messageMapper.toJson(), headers));
                    break;
                }
                case UNLOCK: {
                    processLock(processedMessage.getData(), false);
                    messageMapper.setMsg(createStatusMessage(getMasterStatus()));
                    processResponse(cpush(messageMapper.toJson(), headers));
                    break;
                }
                case STATUS: {
                    messageMapper.setMsg(createStatusMessage(getMasterStatus()));
                    processResponse(cpush(messageMapper.toJson(), headers));
                    break;
                }
                default:
                    break;
            }

        }
    }

    @Override
    public void processRequest(String response) throws Exception {
        throw new Exception("Not yet implemented");
    }

    @Override
    public void processResponse(HttpResponse<JsonNode> response) {

        if(response == null || response.getBody() == null || response.getBody().toString().trim().isEmpty()) {
            return;
        }
        mmm msg = new Gson().fromJson(response.getBody().toString(), mmm.class);
        if(msg != null && msg.getMsg() != null && !msg.getMsg().trim().equals("")) System.out.println("PROCESS RESPONSE:" + msg.getMsg());
        mp messageProcessor = new cmp();
        final cm processedMessage = (cm) messageProcessor.processMessage(msg.getMsg());

        switch (processedMessage.getCommand()) {
            case DISCONNECT:
                tgf(processedMessage.getData());
                break;
            case CONNECT:
                edre(processedMessage.getData());
                break;
            case STATUS:
                yui(processedMessage.getData());
                break;
            default:
                break;
        }
    }

    @Override
    public void processResponse(String response) throws Exception {
        throw new Exception("Not yet implemented");
    }

    synchronized String edre(final String status) {
        final Integer statusCode = Integer.valueOf(status);
        if(statusCode == 1) {
            connected = true;
        } else {
            connected = false;
        }
        return status;
    }

    synchronized String tgf(final String status) {
        final Integer statusCode = Integer.valueOf(status);
        if(statusCode == 1) {
            connected = false;
        }
        return status;
    }

    synchronized String yui(final String status) {
        // TODO FIX code mocked
        //return status;
        return OK;
    }

    private synchronized String getMasterStatus() {
        boolean lock0Status = iii[0];
        final String lock0StatusHex = String.format("%02X", lock0Status ? (byte) 0x01 : (byte) 0x02);
        boolean lock1Status = iii[1];
        final String lock1StatusHex = String.format("%02X", lock1Status ? (byte) 0x01 : (byte) 0x02);
        return lock0StatusHex + lock1StatusHex;
    }

    synchronized String createStatusMessage(String status) {
        return createMessage(STATUS, status);
    }

    synchronized String createConnectMessage(final String code) {
        return createMessage(CONNECT, code);
    }

    private synchronized String createMessage(final String command, final String data) {
        final String currentSequence = String.format("%02X", (byte) (olokol() & 0xFF));
        //byte command
        final String dummyChecksum = String.format("%02X", (byte)(0xFF));

        mp messageProcessor = new cmp();
        cm cm = new cm("7B", currentSequence, command, data, dummyChecksum);
        cm.dfsfdg();
        return messageProcessor.synthMessage(cm);
    }



    void processLock(final String lock, boolean isLocked) {
        try {
            final int lockIndex = Integer.valueOf(lock.substring(0, 2));
            iii[lockIndex] = isLocked;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    class MasterPusher extends Thread implements Runnable {
        private volatile boolean shutdown = false;
        private int pushingOffset;
        private boolean sleepMode = false;

        public MasterPusher(int pushingOffset, boolean sleepMode){
            this.pushingOffset = pushingOffset;
            this.sleepMode = sleepMode;
        }

        public MasterPusher(int pushingOffset){
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

                System.out.println("Master [" + l + "] - Pusher - LIVE");
                if(connected) {
                    try {
                        Map<String, String> headers = new HashMap<String, String>();
                        headers.put("Serial-Number", l);
                        headers.put("Content-Type", "application/json");
                        headers.put("Broadcast", "true");

                        mmm messageMapper = new mmm();
                        iii[new Random().nextInt(2)] = new Random().nextBoolean();
                        messageMapper.setMsg(createStatusMessage(getMasterStatus()));

                        processResponse(cpush(messageMapper.toJson(), headers));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        public void shutdown() {
            shutdown = true;
        }
    }

    private HttpResponse<JsonNode> oiuhyu(){
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Serial-Number", l);
        headers.put("Content-Type", "application/json");
        HttpResponse<JsonNode> response = null;
        try {
            mmm mm = new mmm();
            mm.setMsg(createConnectMessage(""));
            response = post("/cconn", headers, mm.toJson());
        }catch (Exception e){
            e.printStackTrace();
        }
        return response;
    }

}
