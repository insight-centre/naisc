package org.insightcentre.uld.naisc;

import org.apache.commons.text.StringEscapeUtils;
import org.insightcentre.uld.naisc.util.Option;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.PrintStream;
import java.util.*;
import java.util.stream.Collectors;
import org.insightcentre.uld.naisc.util.None;
import org.insightcentre.uld.naisc.util.Pair;
import org.insightcentre.uld.naisc.util.Some;
import org.jetbrains.annotations.Nullable;

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
    private Map<String, Map<Pair<URIRes,URIRes>, Alignment>> index;

    /**
     * Get an alignment if it is in this set
     * @param id1 The left element that is aligned
     * @param id2 The right element that is aligned
     * @param property The property to match
     * @return The alignments between these elements
     */
    public Option<Alignment> find(URIRes id1, URIRes id2, String property) {
        if(index == null) {
            buildIndex();
        }
        Map<Pair<URIRes, URIRes>, Alignment> byPair = index.get(property);
        if(byPair == null)
            return new None<>();
        final Pair<URIRes, URIRes> sp = new Pair<>(id1, id2);
        return byPair.containsKey(sp) ? new Some<>(byPair.get(sp)) : new None<>();

    }

    private void buildIndex() {
        Map<String, Map<Pair<URIRes,URIRes>, Alignment>> map = new HashMap<>();
        for(Alignment alignment : alignments) {
            if(!map.containsKey(alignment.property)) {
                map.put(alignment.property, new HashMap<>());
            }
            map.get(alignment.property).put(new Pair<>(alignment.entity1,
                    alignment.entity2), alignment);
        }
        index = map;
    }


    /**
     * Is there a link the assignment set between two resources
     *
     * @param id1 The left id
     * @param id2 The right id
     * @return True if a link exists between these resources
     */
    public boolean hasLink(URIRes id1, URIRes id2) {
        if(index == null) {
            buildIndex();
        }
        for(Map<Pair<URIRes,URIRes>, Alignment> byPair : index.values()) {
            final Pair<URIRes, URIRes> sp = new Pair<>(id1, id2);
            if(byPair.containsKey(sp)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Find the link between two elements
     *
     * @param id1 The left id
     * @param id2 The right id
     * @return A property linking two resources or null if none is found
     */
    public @Nullable String findLink(URIRes id1, URIRes id2) {
        if(index == null) {
            buildIndex();
        }
        for(Map<Pair<URIRes,URIRes>, Alignment> byPair : index.values()) {
            final Pair<URIRes, URIRes> sp = new Pair<>(id1, id2);
            if(byPair.containsKey(sp)) {
                return byPair.get(sp).property;
            }
        }
        return null;
    }

        /**
         * Does this set contain a particular alignment
         * @param alignment The alignment
         * @return True if the alignment is in the set
         */
        public boolean contains(Alignment alignment) {
            return find(alignment.entity1, alignment.entity2, alignment.property).has();
        }

    public boolean remove(Alignment alignment) {
        boolean rv = alignments.remove(alignment);
        if(index != null) {
            index.get(alignment.property).remove(new Pair<>(alignment.entity1, alignment.entity2));
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
                int i = Double.compare(o1.probability, o2.probability);
                if(i != 0) return -i;
                i = o1.entity1.toString().compareTo(o2.entity1.toString());
                if(i != 0) return i;
                i = o1.entity2.toString().compareTo(o2.entity2.toString());
                if(i != 0) return i;
                return o1.property.compareTo(o2.property);
            }
        });
    }

    @Override
    public boolean add(Alignment alignment) {
        boolean rv = this.alignments.add(alignment);
        if(index != null) {
            if(!index.containsKey(alignment.property))
                index.put(alignment.property, new HashMap<>());
            index.get(alignment.property).put(new Pair<>(alignment.entity1, alignment.entity2), alignment);
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
                if(index != null) index.get(next.property).remove(new Pair<>(next.entity1, next.entity2));
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
            out.println(String.format("      <measure rdf:datatype=\"xsd:float\">%.6f</measure>", alignment.probability));
            out.println(String.format("      <property>%s</property>", toXML(alignment.property)));
            out.println("    </Cell>");
            out.println("  </map>");
        }
        out.println("</Alignment>");
        out.println("</rdf:RDF>");
        out.flush();
    }

    public void toRDF(PrintStream out) {
        for(Alignment alignment : alignments) {
            out.println(String.format("<%s> <%s> <%s> . # %.4f", alignment.entity1, alignment.property, alignment.entity2, alignment.probability));
        }
    }

    public Set<String> properties() {
        return alignments.stream().map(x -> x.property).collect(Collectors.toSet());
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

    /**
     * Get the ith alignment
     * @param i The index
     * @return The ith alignment
     */
    public Alignment get(int i) {
        return alignments.get(i);
    }

}
