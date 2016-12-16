package br.com.example.r;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import java.net.NoRouteToHostException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;
import javax.security.cert.CertificateException;
import javax.security.cert.X509Certificate;

import org.apache.http.client.HttpClient;
//import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.StrictHostnameVerifier;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.HttpClients;
//import org.apache.http.ssl.SSLContextBuilder;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

/**
 * Created by jordao on 27/11/16.
 */
public class r {


    /** url for connecting to the cloud service */
    //private final static String URL = "http://localhost:8080";
//    private final static String URL = "http://10.100.100.100:5050";
    private final static String URL = "https://ec2-52-67-203-68.sa-east-1.compute.amazonaws.com:5051";

    private r(){}

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
        } catch (IOException e) {
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
            }
        }
        return response.toString();
    }

    public static HttpResponse<JsonNode> post(final String endpoint, final Map<String, String> headers, final String body) throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
        try {
            SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustSelfSignedStrategy() {
                public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    return true;
                }
            }).build();
            HttpClient unsafeHttpClient = HttpClients.custom().setSslcontext(sslContext).setHostnameVerifier(new AllowAllHostnameVerifier()).build();
            Unirest.setHttpClient(unsafeHttpClient);

            return Unirest.post(URL + endpoint).headers(headers).body(body).asJson();
        } catch (UnirestException e) {
            e.printStackTrace();
        }
        return null;
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

            if(headers != null && headers.size() != 0){
                for (String key : headers.keySet()){
                    conn.setRequestProperty(key, headers.get(key));
                }
            }

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
        } catch (IOException e) {
        } finally {
            try {
                if(reader != null)
                    reader.close();
            } catch (IOException e) {
            }
        }
        return response.toString();
    }

    public static HttpResponse<JsonNode> get(final String endpoint, final Map<String, String> headers) throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
        try {

            SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustSelfSignedStrategy() {
                public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    return true;
                }
            }).build();
            HttpClient unsafeHttpClient = HttpClients.custom().setSslcontext(sslContext).setHostnameVerifier(new AllowAllHostnameVerifier()).build();
            Unirest.setHttpClient(unsafeHttpClient);

            return Unirest.get(URL + endpoint).headers(headers).asJson();
        } catch (UnirestException e) {
            e.printStackTrace();
        }
        return null;
    }
}
