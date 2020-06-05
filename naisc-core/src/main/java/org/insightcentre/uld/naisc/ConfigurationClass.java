package org.insightcentre.uld.naisc;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates the class used for configuration of a component
 *
 * @author John McCrae
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigurationClass {
    /**
     * The description of what this component does
     */
    public String description() default "";

    /**
     * The readable name of this component
     */
    public String name() default "";
}
