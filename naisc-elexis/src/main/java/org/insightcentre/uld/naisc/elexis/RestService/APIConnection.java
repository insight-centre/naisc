package org.insightcentre.uld.naisc.elexis.RestService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Class to make API requests and return the response as String
 *
 * @author Suruchi Gupta
 */
public class APIConnection {
    private static URL endpoint;

    /**
     * Creating a new object
     * @param endpoint
     */
    public APIConnection(URL endpoint) {
        this.endpoint = endpoint;
    }

    /**
     * Generic method to execute get API calls
     * @param endpoint
     * @return API response as String
     */
    public String executeAPICall(URL endpoint) {
        String apiResponse = null;
        try {
            HttpURLConnection conn = (HttpURLConnection) endpoint.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
            }

            BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));
            StringBuilder sb = new StringBuilder();
            String line;

            while ((line = br.readLine()) != null) {
                sb.append(line+"\n");
            }
            br.close();
            apiResponse =  sb.toString();

            conn.disconnect();

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return apiResponse;
    }
}
