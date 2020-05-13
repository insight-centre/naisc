package org.insightcentre.uld.naisc.constraint;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import org.apache.jena.rdf.model.Resource;
import org.insightcentre.uld.naisc.Alignment;
import static org.insightcentre.uld.naisc.constraint.Bijective.Surjection.bijective;

import org.insightcentre.uld.naisc.URIRes;
import org.insightcentre.uld.naisc.util.SimpleCache;

/**
 * A constraint of strict bijectivity, that is no element on either side may be
 * linked to more than one element on the other side. Note this constraint can
 * be more efficiently solved with the
 * {@link org.insightcentre.uld.naisc.matcher.UniqueAssignment} solver.
 *
 * @author John McCrae
 */
public class Bijective implements ConstraintFactory {

    /**
     * Configuration of bijective constraint. There are currently no parameters
     */
    public static class Configuration {
        /** The type of constraint */
        public Surjection surjection = bijective;
    }
    
    /** Allows some variants on bijectivity */
    public enum Surjection {
        /** Allow multiple links to match to the same right node */
        surjective, 
        /** Allow multiple links to match to the same left node */
        inverseSurjective,
        /** Do not allow any multiple linking */
        bijective,
    }

    @Override
    public Constraint make(Map<String, Object> params) {
        Configuration config = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).convertValue(params, Configuration.class);
        if(config.surjection == null)
            config.surjection = bijective;
        return new BijectiveImpl(new HashMap<>(), new HashMap<>(), config.surjection, 0.0);
    }

    private static class BijectiveImpl extends Constraint {
        final Map<URIRes, List<Alignment>> byLeft;
        final Map<URIRes, List<Alignment>> byRight;
        final Surjection surjection;

        public BijectiveImpl(Map<URIRes, List<Alignment>> byLeft, Map<URIRes, List<Alignment>> byRight, Surjection surjection, double score) {
            super(score);
            this.byLeft = byLeft;
            this.byRight = byRight;
            this.surjection = surjection;
        }

        @Override
        public List<Alignment> alignments() {
            return byLeft.values().stream().flatMap((x) -> x.stream()).collect(Collectors.toList());
        }

        @Override
        public boolean canAdd(Alignment alignment) {
            return (!byLeft.containsKey(alignment.entity1) || surjection == Surjection.inverseSurjective) &&
                    (!byRight.containsKey(alignment.entity2) || surjection == Surjection.surjective);
        }

        @Override
        public void add(Alignment alignment) {
            score += delta(alignment);
            if(!byLeft.containsKey(alignment.entity1)) {
                byLeft.put(alignment.entity1, new ArrayList<>());
            }
            byLeft.get(alignment.entity1).add(alignment);
            
            if(!byRight.containsKey(alignment.entity2)) {
                byRight.put(alignment.entity2, new ArrayList<>());
            }
            byRight.get(alignment.entity2).add(alignment);
        }

        @Override
        public Constraint copy() {
            Map<URIRes, List<Alignment>> newByLeft = new HashMap<>(byLeft);
            Map<URIRes, List<Alignment>> newByRight = new HashMap<>(byRight);
            return new BijectiveImpl(newByLeft, newByRight, surjection, score);
        }
        
        
        
        
    }
            
    /*private static class BijectiveImpl extends Constraint {
        final SimpleCache<BijectiveCacheEntry, Boolean> cache;
        final long id;
        final Random random;
        final Alignment alignment;
        final BijectiveImpl parent;
        final Surjection surjection;

        public BijectiveImpl(SimpleCache<BijectiveCacheEntry, Boolean> cache, Random random,
                Surjection surjection) {
            super(0.0);
            this.cache = cache;
            this.id = random.nextLong();
            this.random = random;
            this.alignment = null;
            this.parent = null;
            this.surjection = surjection;
        }

        public BijectiveImpl(SimpleCache<BijectiveCacheEntry, Boolean> cache, 
                Random random, Alignment alignment, BijectiveImpl parent, Surjection surjection, 
                double probability) {
            super(probability);
            this.cache = cache;
            this.id = random.nextLong();
            this.random = random;
            this.alignment = alignment;
            this.parent = parent;
            this.surjection = surjection;
        }

       

        @Override
        public Constraint add(Alignment alignment) {
            double newscore = this.probability + delta(alignment);
            return new BijectiveImpl(cache, random, alignment, this, surjection, newscore);
        }

        @Override
        public boolean canAdd(Alignment alignment) {
            return (surjection == inverseSurjective || !lcontains(alignment.entity1)) && 
                    (surjection == surjective || !rcontains(alignment.entity2));
        }

        private boolean lcontains(URIRes s) {
            if(alignment == null) {
                return false;
            } else if (alignment.entity1.equals(s)) {
                return true;
            } else {
                final BijectiveCacheEntry bce = new BijectiveCacheEntry(id, s);
                return cache.get(bce, new SimpleCache.Get<BijectiveCacheEntry, Boolean>() {
                    @Override
                    public Boolean get(BijectiveCacheEntry e) {
                        return parent.lcontains(e.text);
                    }
                });
            }
        }
        
        private boolean rcontains(URIRes s) {
            if(alignment == null) {
                return false;
            } else if (alignment.entity2.equals(s)) {
                return true;
            } else {
                final BijectiveCacheEntry bce = new BijectiveCacheEntry(~id, s);
                return cache.get(bce, new SimpleCache.Get<BijectiveCacheEntry, Boolean>() {
                    @Override
                    public Boolean get(BijectiveCacheEntry e) {
                        return parent.rcontains(e.text);
                    }
                });
            }
        }

        
        @Override
        public List<Alignment> alignments(List<Alignment> alignments) {
            if(alignment != null) {
                alignments.add(alignment);
                return parent.alignments(alignments);
            } else {
                return alignments;
            }
        }
        
    }

    private static class BijectiveCacheEntry {
        private final long id;
        private final URIRes text;

        public BijectiveCacheEntry(long id, URIRes text) {
            this.id = id;
            this.text = text;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 53 * hash + (int) (this.id ^ (this.id >>> 32));
            hash = 53 * hash + Objects.hashCode(this.text);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final BijectiveCacheEntry other = (BijectiveCacheEntry) obj;
            if (this.id != other.id) {
                return false;
            }
            if (!Objects.equals(this.text, other.text)) {
                return false;
            }
            return true;
        }
        
    }*/
}
