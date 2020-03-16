package org.insightcentre.uld.naisc;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A configuration parameter in a services configuration
 * 
 * @author John McCrae
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigurationParameter {
    /**
     * The description (value to show the user 
     * @return The string describing the field
     */
    String description();
    /** 
     * The default value to set 
     * @return The (JSON) initial value
     */
    String defaultValue() default "";
}
