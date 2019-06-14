package org.insightcentre.uld.naisc.blocking;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.insightcentre.uld.naisc.BlockingStrategy;
import org.insightcentre.uld.naisc.BlockingStrategyFactory;
import org.insightcentre.uld.naisc.Dataset;
import org.insightcentre.uld.naisc.util.Pair;

/**
 * A mapping that applies sense level linking on OntoLex
 *
 * @author John McCrae
 */
public class OntoLex implements BlockingStrategyFactory {

    @Override
    public BlockingStrategy makeBlockingStrategy(Map<String, Object> params) {
        return new OntoLexImpl();
    }

    private static final String[] LEXICAL_ENTRY_URLS = new String[]{
        "http://www.w3.org/ns/lemon/ontolex#LexicalEntry",
        "http://www.w3.org/ns/lemon/ontolex#Word",
        "http://www.w3.org/ns/lemon/ontolex#MultiwordExpression",
        "http://www.w3.org/ns/lemon/ontolex#Affix",
        "http://lemon-model.net/lemon#LexicalEntry",
        "http://lemon-model.net/lemon#Part",
        "http://lemon-model.net/lemon#Word",
        "http://lemon-model.net/lemon#Phrase",
        "http://www.monnet-project.eu/lemon#LexicalEntry",
        "http://www.monnet-project.eu/lemon#Part",
        "http://www.monnet-project.eu/lemon#Word",
        "http://www.monnet-project.eu/lemon#Phrase",
        "http://www.lexinfo.net/ontology/2.0/lexinfo#Adjective",
        "http://www.lexinfo.net/ontology/2.0/lexinfo#Adposition",
        "http://www.lexinfo.net/ontology/2.0/lexinfo#Postposition",
        "http://www.lexinfo.net/ontology/2.0/lexinfo#Preposition",
        "http://www.lexinfo.net/ontology/2.0/lexinfo#Adverb",
        "http://www.lexinfo.net/ontology/2.0/lexinfo#Conjunction",
        "http://www.lexinfo.net/ontology/2.0/lexinfo#Determiner",
        "http://www.lexinfo.net/ontology/2.0/lexinfo#Article",
        "http://www.lexinfo.net/ontology/2.0/lexinfo#FusedPreposition",
        "http://www.lexinfo.net/ontology/2.0/lexinfo#Interjection",
        "http://www.lexinfo.net/ontology/2.0/lexinfo#Noun",
        "http://www.lexinfo.net/ontology/2.0/lexinfo#CommonNoun",
        "http://www.lexinfo.net/ontology/2.0/lexinfo#ProperNoun",
        "http://www.lexinfo.net/ontology/2.0/lexinfo#Numeral",
        "http://www.lexinfo.net/ontology/2.0/lexinfo#Particle",
        "http://www.lexinfo.net/ontology/2.0/lexinfo#Pronoun",
        "http://www.lexinfo.net/ontology/2.0/lexinfo#Symbol",
        "http://www.lexinfo.net/ontology/2.0/lexinfo#Verb"
    };

    private static final String[] FORM_URIS = new String[]{
        "http://www.w3.org/ns/lemon/ontolex#canonicalForm",
        "http://www.w3.org/ns/lemon/ontolex#form", // This is never correct, but we understand it
        "http://www.w3.org/ns/lemon/ontolex#lexicalForm",
        "http://lemon-model.net/lemon#canonicalForm",
        "http://lemon-model.net/lemon#form",
        "http://lemon-model.net/lemon#lexicalForm",
        "http://www.monnet-project.eu/lemon#canonicalForm",
        "http://www.monnet-project.eu/lemon#form",
        "http://www.monnet-project.eu/lemon#lexicalForm"
    };

    private static final String[] REP_URIS = new String[]{
        "http://www.w3.org/ns/lemon/ontolex#writtenRep",
        "http://lemon-model.net/lemon#writtenRep",
        "http://www.monnet-project.eu/lemon#writtenRep"
    };

    private static final String[] SENSE_URIS = new String[]{
        "http://www.w3.org/ns/lemon/ontolex#sense",
        "http://lemon-model.net/lemon#sense",
        "http://www.monnet-project.eu/lemon#sense"
    };

    private static class OntoLexImpl implements BlockingStrategy {

        @Override
        public Iterable<Pair<Resource, Resource>> block(Dataset _left, Dataset _right) {
            Model left = _left.asModel().getOrExcept(new RuntimeException("Does not support SPARQL endpoints"));
            Model right = _right.asModel().getOrExcept(new RuntimeException("Does not support SPARQL endpoints"));
            Set<Resource> leftEntries = extractEntries(left);
            Set<Resource> rightEntries = extractEntries(right);
            final Map<LangString, Set<Resource>> leftByLabel = byLabel(leftEntries, left);
            final Map<LangString, Set<Resource>> rightByLabel = byLabel(rightEntries, right);
            return new Iterable<Pair<Resource, Resource>>() {
                @Override
                public Iterator<Pair<Resource, Resource>> iterator() {
                    return leftByLabel.entrySet().stream().flatMap((Map.Entry<LangString, Set<Resource>> r) -> {
                        if (rightByLabel.containsKey(r.getKey())) {
                            return rightByLabel.get(r.getKey()).stream().flatMap((Resource r2) -> {
                                return r.getValue().stream().flatMap((Resource r1) -> {
                                    return getSensePairs(r1, left, r2, right).stream();
                                });
                            });
                        } else {
                            return Collections.EMPTY_SET.stream();
                        }
                    }).iterator();
                }
            };
        }

        @Override
        public int estimateSize(Dataset _left, Dataset _right) {
            Model left = _left.asModel().getOrExcept(new RuntimeException("Does not support SPARQL endpoints"));
            Model right = _right.asModel().getOrExcept(new RuntimeException("Does not support SPARQL endpoints"));
            Set<Resource> leftEntries = extractEntries(left);
            Set<Resource> rightEntries = extractEntries(right);
            final Map<LangString, Set<Resource>> leftByLabel = byLabel(leftEntries, left);
            final Map<LangString, Set<Resource>> rightByLabel = byLabel(rightEntries, right);
            int i = 0;
            for (Map.Entry<LangString, Set<Resource>> le : leftByLabel.entrySet()) {
                if (rightByLabel.containsKey(le.getKey())) {
                    for(Resource r1 : le.getValue()) {
                        for(Resource r2 : rightByLabel.get(le.getKey())) {
                            i += getSensePairs(r1, left, r2, right).size();
                        }
                    }
                }
            }
            return i;
        }

        private Set<Resource> extractEntries(Model model) {
            Set<Resource> entries = new HashSet<>();
            for (String entryURL : LEXICAL_ENTRY_URLS) {
                ResIterator i = model.listSubjectsWithProperty(RDF.type, model.createResource(entryURL));
                while (i.hasNext()) {
                    entries.add(i.next());
                }
            }
            return entries;
        }

        private Map<LangString, Set<Resource>> byLabel(Set<Resource> resources, Model model) {
            Map<LangString, Set<Resource>> byLabel = new HashMap<>();
            for (Resource entry : resources) {
                NodeIterator i1 = model.listObjectsOfProperty(entry, RDFS.label);
                while (i1.hasNext()) {
                    RDFNode n = i1.next();
                    if (n.isLiteral()) {
                        LangString ls = new LangString(n.asLiteral());
                        if (!byLabel.containsKey(ls)) {
                            byLabel.put(ls, new HashSet<>());
                        }
                        byLabel.get(ls).add(entry);
                    }
                }
                for (String formURL : FORM_URIS) {
                    NodeIterator i2 = model.listObjectsOfProperty(entry, model.createProperty(formURL));
                    while (i2.hasNext()) {
                        RDFNode n2 = i2.next();
                        if (n2.isResource()) {
                            for (String repURL : REP_URIS) {
                                NodeIterator i3 = model.listObjectsOfProperty(n2.asResource(), model.createProperty(repURL));
                                while (i3.hasNext()) {
                                    RDFNode n3 = i3.next();
                                    if (n3.isLiteral()) {
                                        LangString ls = new LangString(n3.asLiteral());
                                        if (!byLabel.containsKey(ls)) {
                                            byLabel.put(ls, new HashSet<>());
                                        }
                                        byLabel.get(ls).add(entry);

                                    }
                                }

                            }
                        }
                    }
                }
            }
            return byLabel;

        }

        private static final List<Pair<Resource, Resource>> getSensePairs(Resource l, Model left, Resource r, Model right) {
            List<Pair<Resource, Resource>> pairs = new ArrayList<>();
            for (String senseURL1 : SENSE_URIS) {
                NodeIterator i1 = left.listObjectsOfProperty(l, left.getProperty(senseURL1));
                while (i1.hasNext()) {
                    RDFNode n1 = i1.next();
                    if (n1.isResource()) {
                        for (String senseURL2 : SENSE_URIS) {
                            NodeIterator i2 = right.listObjectsOfProperty(r, right.getProperty(senseURL2));
                            while (i2.hasNext()) {
                                RDFNode n2 = i2.next();
                                if (n2.isResource()) {
                                    pairs.add(new Pair<>(n1.asResource(), n2.asResource()));
                                }
                            }
                        }

                    }
                }

            }
            return pairs;
        }

        private static class LangString {

            private final String string;
            private final String lang;

            public LangString(String string, String lang) {
                this.string = string;
                this.lang = lang;
            }

            public LangString(Literal lit) {
                this.string = lit.getLexicalForm();
                this.lang = lit.getLanguage() == null ? "" : lit.getLanguage();
            }

            @Override
            public int hashCode() {
                int hash = 5;
                hash = 73 * hash + Objects.hashCode(this.string);
                hash = 73 * hash + Objects.hashCode(this.lang);
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
                final LangString other = (LangString) obj;
                if (!Objects.equals(this.string, other.string)) {
                    return false;
                }
                if (!Objects.equals(this.lang, other.lang)) {
                    return false;
                }
                return true;
            }

        }

    }
}
