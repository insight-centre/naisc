package org.insightcentre.uld.naisc.elexis.Model;

import lombok.Data;
import org.insightcentre.uld.naisc.main.Configuration;
import org.jvnet.hk2.annotations.Optional;

import javax.validation.constraints.NotEmpty;

/**
 * Stores the MessageBody for the linking request
 *
 * @author Suruchi Gupta
 */
@Data
public class MessageBody {

    @NotEmpty
    Source source;

    @NotEmpty
    Target target;

    @Optional
    Configuration configuration;
}