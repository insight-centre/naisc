package org.insightcentre.uld.naisc.elexis.Model;


import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotEmpty;

public class MessageBody {
    @JsonProperty("source")
    @NotEmpty
    Source SourceObject;

    @JsonProperty("target")
    @NotEmpty
    Target TargetObject;

    @JsonProperty("configuration")
    Configuration ConfigurationObject;


    // Getter Methods

    public Source getSource() {
        return SourceObject;
    }

    public Target getTarget() {
        return TargetObject;
    }

    public Configuration getConfiguration() {
        return ConfigurationObject;
    }

    // Setter Methods

    public void setSource(Source sourceObject) {
        this.SourceObject = sourceObject;
    }

    public void setTarget(Target targetObject) {
        this.TargetObject = targetObject;
    }

    public void setConfiguration(Configuration configurationObject) {
        this.ConfigurationObject = configurationObject;
    }
}