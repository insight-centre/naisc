package org.insightcentre.uld.naisc;

import org.insightcentre.uld.naisc.util.Option;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.PrintStream;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Objects;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.jena.rdf.model.Resource;
import org.insightcentre.uld.naisc.util.None;
import org.insightcentre.uld.naisc.util.Pair;
import org.insightcentre.uld.naisc.util.Some;
import org.insightcentre.uld.naisc.util.StringPair;

/**
 * A set of alignments
 * 
 * @author John McCrae
 */
public class AlignmentSet extends AbstractCollection<Alignment> {
    /** The alignments */
    private final List<Alignment> alignments;

    public AlignmentSet() {
        this.alignments = new ArrayList<>();
    }

    
    @JsonCreator public AlignmentSet(List<Alignment> alignments) {
        this.alignments = alignments;
    }
    private Map<String, Map<Pair<Resource,Resource>, Alignment>> index;

    /**
     * Get an alignment if it is in this set
     * @param id1 The left element that is aligned
     * @param id2 The right element that is aligned
     * @param property The property to matche
     * @return The alignments between these elements
     */
    public Option<Alignment> find(Resource id1, Resource id2, String property) {
        if(index == null) {
            Map<String, Map<Pair<Resource,Resource>, Alignment>> map = new HashMap<>();
            for(Alignment alignment : alignments) {
                if(!map.containsKey(alignment.relation)) {
                    map.put(alignment.relation, new HashMap<>());
                }
                map.get(alignment.relation).put(new Pair<>(alignment.entity1,
                            alignment.entity2), alignment);
            }
            index = map;
        }
        Map<Pair<Resource, Resource>, Alignment> byPair = index.get(property);
        if(byPair == null)
            return new None<>();
        final Pair<Resource, Resource> sp = new Pair<>(id1, id2);
        return byPair.containsKey(sp) ? new Some<>(byPair.get(sp)) : new None<>();

    }
    
    public boolean remove(Alignment alignment) {
        boolean rv = alignments.remove(alignment);
        if(index != null) {
            index.get(alignment.relation).remove(new Pair<>(alignment.entity1, alignment.entity2));
        }
        return rv;
        
    }

    public List<Alignment> getAlignments() {
        return Collections.unmodifiableList(alignments);
    }
    
    @SuppressWarnings("Convert2Lambda")
    public void sortAlignments() {
        alignments.sort(new Comparator<Alignment>() {
            @Override
            public int compare(Alignment o1, Alignment o2) {
                int i = Double.compare(o1.score, o2.score);
                if(i != 0) return -i;
                i = o1.entity1.toString().compareTo(o2.entity1.toString());
                if(i != 0) return i;
                i = o1.entity2.toString().compareTo(o2.entity2.toString());
                if(i != 0) return i;
                return o1.relation.compareTo(o2.relation);
            }
        });
    }

    @Override
    public boolean add(Alignment alignment) {
        boolean rv = this.alignments.add(alignment);
        if(index != null) {
            if(!index.containsKey(alignment.relation))
                index.put(alignment.relation, new HashMap<>());
            index.get(alignment.relation).put(new Pair<>(alignment.entity1, alignment.entity2), alignment);
        }
        return rv;
    }
    
    @Override
    public Iterator<Alignment> iterator() {
        final Iterator<Alignment> iter = alignments.iterator();
        return new Iterator<Alignment>() {
            Alignment next;
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public Alignment next() {
                return next = iter.next();
            }

            @Override
            public void remove() {
                iter.remove();
                if(index != null) index.get(next.relation).remove(new Pair<>(next.entity1, next.entity2));
            }
        };
    }

    @Override
    public int size() {
        return alignments.size();
    }
    
    private String toXML(String rel) {
        if(rel == null ? Alignment.SKOS_EXACT_MATCH == null : rel.equals(Alignment.SKOS_EXACT_MATCH)) {
            return "=";
        /*} else if(rel == Relation.broader) {
            return ">";
        } else if(rel == Relation.narrower) {
            return "<";
        } else if(rel == Relation.incompatible) {
            return "%";
        } else if(rel == Relation.instance) {
            return "HasInstance";
        } else if(rel == Relation.isInstanceOf) {
            return "InstanceOf";*/
        } else {
            throw new RuntimeException("Relation not supported by XML");
        }
    }
    
    public void toXML(PrintStream out) {
        out.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        out.println("<rdf:RDF xmlns=\"http://knowledgeweb.semanticweb.org/heterogeneity/alignment\"");
        out.println("  xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\""); 
        out.println("  xmlns:xsd=\"http://www.w3.org/2001/XMLSchema#\">");
        out.println("<Alignment>");
        out.println("  <xml>yes</xml>");
        out.println("  <level>0</level>");
        out.println("  <type>??</type>");
        for(Alignment alignment : alignments) {
            out.println("  <map>");
            out.println("    <Cell>");
            out.println(String.format("      <entity1 rdf:resource=\"%s\"/>", StringEscapeUtils.escapeXml11(alignment.entity1.getURI())));
            out.println(String.format("      <entity2 rdf:resource=\"%s\"/>", StringEscapeUtils.escapeXml11(alignment.entity2.getURI())));
            out.println(String.format("      <measure rdf:datatype=\"xsd:float\">%.6f</measure>", alignment.score));
            out.println(String.format("      <relation>%s</relation>", toXML(alignment.relation))); 
            out.println("    </Cell>");
            out.println("  </map>");
        }
        out.println("</Alignment>");
        out.println("</rdf:RDF>");
        out.flush();
    }
    
    public void toRDF(PrintStream out) {
        for(Alignment alignment : alignments) {
            out.println(String.format("<%s> <%s> <%s> . # %.4f", alignment.entity1, alignment.relation, alignment.entity2, alignment.score));
        }
    }
    
    public Set<String> properties() {
        return alignments.stream().map(x -> x.relation).collect(Collectors.toSet());
    }
    
    private ObjectMapper mapper;

    @Override
    public String toString() {
        return "AlignmentSet{" + "alignments=" + alignments + '}';
    }

   


    @Override
    public int hashCode() {
        int hash = 3;
        hash = 59 * hash + Objects.hashCode(this.alignments);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final AlignmentSet other = (AlignmentSet) obj;
        if (!Objects.equals(this.alignments, other.alignments)) {
            return false;
        }
        return true;
    }
    
}
