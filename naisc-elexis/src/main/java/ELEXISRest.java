

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Class to access the data from ELEXIS REST APIs
 * defined at: https://elexis-eu.github.io/elexis-rest/
 *
 * @author Suruchi Gupta
 */
public class ELEXISRest {
    private static URL endpoint;

    /**
     * Creating a new object
     *
     * @param endpoint
     */
    public ELEXISRest(URL endpoint) {
        this.endpoint = endpoint;
    }

    /**
     * Generic method to execute get API calls
     *
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
            apiResponse = br.readLine();
            conn.disconnect();

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return apiResponse;
    }

    /**
     * Calls dictionaries endpoint
     *
     * @return List of all dictionaries available
     */
    public List<String> getDictionaries() throws MalformedURLException {
        URL dictEndpoint = new URL(endpoint.toString()+"/dictionaries");
        String response = executeAPICall(dictEndpoint);

        JSONObject jsonResponse = new JSONObject(response);

        ArrayList<String> dictionaries = new ArrayList<String>();
        if(jsonResponse.has("dictionaries")) {
            JSONArray dictArray = (JSONArray) jsonResponse.get("dictionaries");
            dictArray.forEach(dict -> dictionaries.add((String) dict));
        }
        return dictionaries;
    }

    public MetaData aboutDictionary(String dictionary) throws MalformedURLException, JsonProcessingException {
        URL aboutDictEndPoint = new URL(endpoint.toString()+"/about/"+dictionary);
        String response = executeAPICall(aboutDictEndPoint);

        ObjectMapper objectMapper = new ObjectMapper();
        MetaData metaData = objectMapper.readValue(response, MetaData.class);

        return metaData;
    }
}
