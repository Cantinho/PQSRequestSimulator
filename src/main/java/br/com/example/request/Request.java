package br.com.example.request;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

/**
 * Created by jordao on 27/11/16.
 */
public class Request {

    private final static Logger LOGGER = LoggerFactory.getLogger(Request.class.getName());


    /** url for connecting to the cloud service */
    private final static String URL = "http://ec2-52-67-203-68.sa-east-1.compute.amazonaws.com:5050";

    private Request(){}

    /**
     * Method GET is utilized only with the /pull endpoint.
     * @param endpoint: /pull
     * @param headers: "Serial-Number"
     * @return
     */
    public static String GET(String endpoint, Map<String, String> headers){

        StringBuffer response = new StringBuffer();
        BufferedReader reader = null;
        try {
            URL url = new URL(URL + endpoint);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            // adding headers to request
            if(headers != null && headers.size() != 0){
                for (String key : headers.keySet()){
                    conn.setRequestProperty(key, headers.get(key));
                }
            }

            int responseCode = conn.getResponseCode();
            if(responseCode == 200){
                reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String line = null;
                while((line = reader.readLine()) != null){
                    response.append(line);
                }
            }

        } catch (MalformedURLException e) {
            LOGGER.error(e.getMessage());
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                LOGGER.error(e.getMessage());
            }
        }
        return response.toString();
    }

    /**
     * Method POST can be utilized either for /pa or /pc endpoints.
     * @param endpoint: /pa or /pc
     * @param headers for /pa: "Serial-Number", "Application-ID", "Message-Amount"
     * @param headers for /pc: "Serial-Number", "Application-ID", "Broadcast", "Multicast"
     * @param body: a packet (7B4C111AAABBBCCC) or NULL
     * @return
     */
    public static String POST(String endpoint, Map<String, String> headers, String body) {

        StringBuffer response = new StringBuffer();
        BufferedReader reader = null;
        try {
            URL url = new URL(URL + endpoint);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");

            // adding headers to request
            if(headers != null && headers.size() != 0){
                for (String key : headers.keySet()){
                    conn.setRequestProperty(key, headers.get(key));
                }
            }

            // Send post request
            conn.setDoOutput(true);
            DataOutputStream dataOutputStream = new DataOutputStream(conn.getOutputStream());
            if (body != null) dataOutputStream.writeBytes(body);
            dataOutputStream.flush();
            dataOutputStream.close();

            int responseCode = conn.getResponseCode();
            if(responseCode == 200){
                reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String line = null;
                while((line = reader.readLine()) != null){
                    response.append(line);
                }
            }

        } catch (MalformedURLException e) {
            LOGGER.error(e.getMessage());
        } catch (IOException e) {
            LOGGER.error(e.getMessage());
        } finally {
            try {
                if(reader != null)
                    reader.close();
            } catch (IOException e) {
                LOGGER.error(e.getMessage());
            }
        }
        return response.toString();
    }
}
