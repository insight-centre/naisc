package org.insightcentre.uld.naisc.elexis.Model;


import com.fasterxml.jackson.annotation.JsonProperty;
import org.jvnet.hk2.annotations.Optional;

import javax.validation.constraints.NotEmpty;

public class MessageBody {
    @JsonProperty("source")
    @NotEmpty
    Source source;

    @JsonProperty("target")
    @NotEmpty
    Target target;

    @JsonProperty("configuration")
    @Optional
    Configuration configuration;

    public MessageBody()
    {
        setSource(source);
        setTarget(target);
        setConfiguration(configuration);
    }

    // Getter Methods

    public Source getSource() {
        return source;
    }

    public Target getTarget() {
        return target;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    // Setter Methods

    public void setSource(Source source) {
        this.source = source;
    }

    public void setTarget(Target target) {
        this.target = target;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }
}