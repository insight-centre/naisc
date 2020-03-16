package org.insightcentre.uld.naisc.util;

import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.objects.ObjectSet;

/**
 * A vector that supports some basic arithmetic operations
 * 
 * @author John McCrae
 */
public interface Vector extends DoubleList {
   /**
    * Return the non-zero values of this vector
    * @return An object set containing the non-zero values of this vector
    */ 
   ObjectSet<Int2DoubleMap.Entry> sparseEntrySet();
   /**
    * The sum of all values in the vector
    * @return The sum
    */
   double sum();
   /**
    * The norm of the vector. That is Sqrt(Sum(x^2))
    * @return The norm
    */
   double norm();
   
   /**
    * Add another vector to this one
    * @param v The vector to add
    * @return this
    */
   Vector add(Vector v);
   
   /**
    * Add another vector to this one
    * @param v The vector to add
    * @param d Scale the vector by d
    * @return this
    */
   Vector add(Vector v, double d);


   /**
    * Multiple all values in the vector by a constant
    * @param d The scaling factor
    * @return this
    */
   Vector scale(double d);

   /**
    * Return the pairwise product of this vector
    * @param v The other vector
    * @return this * v
    */
   Vector pairwise(Vector v);

   /**
    * Apply abs to each element of the vector
    * @return this
    */
   Vector abs();

   /**
    * Apply tanh to each element of the vector
    * @return this
    */
   Vector tanh();

   /**
    * Apply tanh to each element of the vector
    * @return this
    */
   Vector sigmoid();

   /**
    * The length of this vector
    * @return The length
    */
   int len();
}
