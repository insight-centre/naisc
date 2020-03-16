package org.insightcentre.uld.naisc.naiscelexis.Model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MessageWrapper {
    @JsonProperty("MessageBody")
    private MessageBody messageBody;

    public MessageBody getMessageBody() {
        return messageBody;
    }

    public void setMessageBody(MessageBody messageBody) {
        this.messageBody = messageBody;
    }
}
