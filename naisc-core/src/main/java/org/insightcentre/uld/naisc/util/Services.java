package org.insightcentre.uld.naisc.util;

/**
 * Automatically load a service by name.
 * 
 * @author John McCrae
 */
public class Services {
   
    private Services() { }
    public static <A> A get(Class<A> clazz, String name) {
       try {
           return (A)Class.forName("org.insightcentre.uld.naisc." + name).newInstance();
       } catch(ClassNotFoundException | IllegalAccessException | InstantiationException x) {
           try {
                return (A)Class.forName(name).newInstance();
            } catch(ClassNotFoundException | IllegalAccessException | InstantiationException x2) {
                throw new RuntimeException("Could not load service: " + name, x2);
            }
       }
    }
}
