package org.insightcentre.uld.naisc.elexis.Controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import sun.net.www.protocol.http.HttpURLConnection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;


@RestController
public class SubmitLink {

    @GetMapping("/dummy")
    public String index() {
        return "Greetings from Spring Boot!";
    }

    @GetMapping("/testConnection")
    public String testConnection() throws MalformedURLException {
        String apiResponse = null;
        URL endpoint = new URL("http://localhost:8000");
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
                sb.append(line).append("\n");
            }
            br.close();
            apiResponse = sb.toString();

            conn.disconnect();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return apiResponse;
    }

    @PostMapping("/submit")
    //@RequestMapping(value = "/submit", method = RequestMethod.POST )
    public String submitLinkRequest() {
        //URL endpoint = new URL("http://localhost:8000");
        //ELEXISRest elexisRest = new ELEXISRest(endpoint);
        //return elexisRest.getDictionaries();

        return "Got it!!";
    }
}
