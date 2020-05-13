package org.insightcentre.uld.naisc.util;

import java.lang.reflect.InvocationTargetException;

/**
 * Automatically load a service by name.
 * 
 * @author John McCrae
 */
public class Services {
   
    private Services() { }
    public static <A> A get(Class<A> clazz, String name) {
       try {
           return (A)Class.forName("org.insightcentre.uld.naisc." + name).getConstructor().newInstance();
       } catch(ClassNotFoundException | IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException x) {
           try {
                return (A)Class.forName(name).getConstructor().newInstance();
            } catch(ClassNotFoundException | IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException x2) {
                throw new RuntimeException("Could not load service: " + name, x2);
            }
       }
    }
}
