package org.insightcentre.uld.naisc.elexis;

import org.apache.logging.log4j.message.Message;
import org.insightcentre.uld.naisc.elexis.Controller.SubmitLink;
import org.insightcentre.uld.naisc.elexis.Model.*;
import org.insightcentre.uld.naisc.elexis.RestService.ELEXISRest;
import org.insightcentre.uld.naisc.elexis.RestService.ELEXISRestFactory;
import org.insightcentre.uld.naisc.main.Configuration;
import org.insightcentre.uld.naisc.scorer.ModelNotTrainedException;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;

public class ElexisIntegrationTest {

    @Test
    public void testIntegration() throws ModelNotTrainedException, TransformerException, IOException {
        ELEXISRest.factory = new ELEXISRestFactory() {
            @Override
            public ELEXISRest make(URL endpoint) {
                if(endpoint.toString().equals("http://www.example.com/left")) {
                    return new DummyELEXISRest(endpoint, "left",
                            Arrays.asList("bank", "boat"),
                            Arrays.asList(Arrays.asList("money place", "side of river"), Arrays.asList("floaty thing")));
                } else if(endpoint.toString().equals("http://www.example.com/right")) {
                    return new DummyELEXISRest(endpoint, "right",
                        Arrays.asList("bank", "boat"),
                        Arrays.asList(Arrays.asList("money money", "river side"), Arrays.asList("not a ship")));
                } else {
                    throw new IllegalArgumentException();
                }
            }
        };
        SubmitLink submitLink = new SubmitLink();
        submitLink.postConstruct();
        MessageBody messageBody = new MessageBody();
        Source source = new Source();
        source.setEndpoint("http://www.example.com/left");
        source.setId("left");
        messageBody.setSource(source);
        Target target = new Target();
        target.setEndpoint("http://www.example.com/right");
        target.setId("right");
        messageBody.setTarget(target);

        submitLink.submitLinkRequest(messageBody);
    }


    @Test
    public void testIntegration2() throws ModelNotTrainedException, TransformerException, IOException {
        ELEXISRest.factory = new ELEXISRestFactory() {
            @Override
            public ELEXISRest make(URL endpoint) {
                if(endpoint.toString().equals("http://www.example.com/left")) {
                    return new DummyELEXISRest(endpoint, "left",
                            Arrays.asList("bank", "boat"),
                            Arrays.asList(Arrays.asList("money place", "side of river"), Arrays.asList("floaty thing")));
                } else if(endpoint.toString().equals("http://www.example.com/right")) {
                    DummyELEXISRest der = new DummyELEXISRest(endpoint, "right",
                            Arrays.asList("bank", "boat", "booby"),
                            Arrays.asList(Arrays.asList("money money", "river side"), Arrays.asList("not a ship"), Arrays.asList("a mistake")));
                    der.boobyTrap = "booby";
                    return der;
                } else {
                    throw new IllegalArgumentException();
                }
            }
        };
        SubmitLink submitLink = new SubmitLink();
        submitLink.postConstruct();
        MessageBody messageBody = new MessageBody();
        Source source = new Source();
        source.setEndpoint("http://www.example.com/left");
        source.setId("left");
        messageBody.setSource(source);
        Target target = new Target();
        target.setEndpoint("http://www.example.com/right");
        target.setId("right");
        messageBody.setTarget(target);

        submitLink.submitLinkRequest(messageBody);
    }
}
