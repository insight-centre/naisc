package org.insightcentre.uld.naisc.util;

import it.unimi.dsi.fastutil.ints.Int2DoubleMap;

/**
 * Tools for working with vectors
 * 
 * @author John McCrae
 */
public class Vectors {
    private Vectors() {}
    
   /**
    * Calculate the cosine of two vectors
    * @param _v1 Vector 1
    * @param _v2 Vector 2
    * @return Sum(x*y)/(norm(x)*norm(y))
    */
   public static double cosine(Vector _v1, Vector _v2) {
       final Vector v1 = _v1.size() < _v2.size() ? _v1 : _v2;
       final Vector v2 = _v1.size() < _v2.size() ? _v2 : _v1;
        if(v1.len() != v2.len()) throw new LinearAlgebraException(String.format("Vector lengths do not match %d <-> %d", v1.len(), v2.len()));

       double aa = 0, ab = 0;

       for(Int2DoubleMap.Entry s : v1.sparseEntrySet()) {
           aa += s.getDoubleValue()*s.getDoubleValue();
           ab += s.getDoubleValue()*v2.getDouble(s.getIntKey());
       }
       final double bnorm = v2.norm();
       if(aa == 0 || bnorm == 0) {
           return 0.0;
       } else {
           return ab / Math.sqrt(aa) / bnorm;
       }
   } 
}
