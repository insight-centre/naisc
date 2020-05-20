package org.insightcentre.uld.naisc.lens;

import eu.monnetproject.lang.Language;

import java.util.*;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.insightcentre.uld.naisc.*;

import static org.insightcentre.uld.naisc.lens.OntoLex.Dialect.ONTOLEX;
import org.insightcentre.uld.naisc.main.Configs;
import org.insightcentre.uld.naisc.util.Labels;
import org.insightcentre.uld.naisc.util.LangStringPair;
import org.insightcentre.uld.naisc.util.None;
import org.insightcentre.uld.naisc.util.Option;
import org.insightcentre.uld.naisc.util.Some;

/**
 * Read labels from OntoLex-Lemon.
 *
 * @author John McCrae
 */
public class OntoLex implements LensFactory {

    @Override
    public Lens makeLens(Dataset dataset, Map<String, Object> params) {
        Configuration config = Configs.loadConfig(Configuration.class, params);
        if(config.dialect == null) {
            config.dialect = ONTOLEX;
        }
        return new OntoLexImpl(config.dialect, dataset,
                config.language == null ? null : Language.get(config.language), config.onlyCanonical);
    }

    /**
     * The configuration class for the OntoLex lens
     */
    public static class Configuration {

        /**
         * The dialect to use
         */
        @ConfigurationParameter(description = "The dialect (namespace) to use")
        public Dialect dialect = ONTOLEX;
        /**
         * Only use canonical forms or use all forms
         */
        @ConfigurationParameter(description = "Only use canonical forms or use all forms")
        public boolean onlyCanonical = true;
        /**
         * The language to extract, null for first language available
         */
        @ConfigurationParameter(description = "The language to extract, null for first language available")
        public String language = null;
    }

    /**
     * The dialects of OntoLex
     */
    public enum Dialect {
        /**
         * The OntoLex vocabulary, e.g. http://www.w3.org/ns/lemon/ontolex
         */
        ONTOLEX,
        /**
         * The lemon-model.net vocabulary, e.g., http://lemon-model.net/lemon
         */
        LEMON,
        /**
         * The Monnet vocabulary, e.g., http://www.monnet-project.eu
         */
        MONNET_LEMON
    }

    private static class OntoLexImpl implements Lens {

        private final Dialect dialect;
        private final Dataset model;
        private final boolean onlyCanonical;
        private final Property RDFS_LABEL;
        private final Language language;

        public OntoLexImpl(Dialect dialect, Dataset model, Language language, boolean onlyCanonical) {
            this.dialect = dialect;
            this.model = model;
            this.onlyCanonical = onlyCanonical;
            this.language = language;
            RDFS_LABEL = model.createProperty("http://www.w3.org/2000/01/rdf-schema#label");
        }

        private Property prop(String name) {
            switch (dialect) {
                case ONTOLEX:
                    return model.createProperty("http://www.w3.org/ns/lemon/ontolex#"+ name);
                case LEMON:
                    return model.createProperty("http://lemon-model.net/lemon#"+ name);
                case MONNET_LEMON:
                    return model.createProperty("http://www.monnet-project.eu/lemon#"+ name);
            }
            throw new RuntimeException("Dialect not valid");
        }

        private final List<Literal> forms(Resource r) {
            Set<Resource> entries = new HashSet<>();
            ResIterator iter1 = model.listSubjectsWithProperty(prop("denotes"), r);
            while (iter1.hasNext()) {
                entries.add(iter1.next());
            }
            NodeIterator iter2 = model.listObjectsOfProperty(r, prop("isDenotedBy"));
            while (iter2.hasNext()) {
                RDFNode n = iter2.next();
                if (n.isResource()) {
                    entries.add(n.asResource());
                }
            }
            Set<Resource> senses = new HashSet<>();
            ResIterator iter3 = model.listSubjectsWithProperty(prop("reference"), r);
            while (iter3.hasNext()) {
                senses.add(iter3.next());
            }
            NodeIterator iter4 = model.listObjectsOfProperty(r, prop("isReferenceOf"));
            while (iter4.hasNext()) {
                RDFNode n = iter4.next();
                if (n.isResource()) {
                    senses.add(n.asResource());
                }
            }
            for (Resource sense : senses) {
                ResIterator iter5 = model.listSubjectsWithProperty(prop("sense"), sense);
                while (iter5.hasNext()) {
                    entries.add(iter5.next());
                }
                NodeIterator iter6 = model.listObjectsOfProperty(sense, prop("isSenseOf"));
                while (iter6.hasNext()) {
                    RDFNode n = iter6.next();
                    if (n.isResource()) {
                        entries.add(n.asResource());
                    }
                }
            }
            List<Literal> forms = new ArrayList<>();
            for (Resource entry : entries) {
                NodeIterator iter7 = model.listObjectsOfProperty(entry, RDFS_LABEL);
                while (iter7.hasNext()) {
                    RDFNode n = iter7.next();
                    if (n.isLiteral()) {
                        forms.add(n.asLiteral());
                    }
                }
                Set<Resource> formObjs = new HashSet<>();
                NodeIterator iter8 = model.listObjectsOfProperty(entry, prop("canonicalForm"));
                while (iter8.hasNext()) {
                    RDFNode n = iter8.next();
                    if (n.isResource()) {
                        formObjs.add(n.asResource());
                    }
                }
                if (!onlyCanonical) {

                    NodeIterator iter9 = model.listObjectsOfProperty(entry, prop("lexicalForm"));
                    while (iter9.hasNext()) {
                        RDFNode n = iter9.next();
                        if (n.isResource()) {
                            formObjs.add(n.asResource());
                        }
                    }

                    NodeIterator iter10 = model.listObjectsOfProperty(entry, prop("otherForm"));
                    while (iter10.hasNext()) {
                        RDFNode n = iter10.next();
                        if (n.isResource()) {
                            formObjs.add(n.asResource());
                        }
                    }
                }
                for (Resource form : formObjs) {
                    NodeIterator iter11 = model.listObjectsOfProperty(form, prop("writtenRep"));
                    while (iter11.hasNext()) {
                        RDFNode n = iter11.next();
                        if (n.isLiteral()) {
                            forms.add(n.asLiteral());
                        }
                    }

                }
            }
            return forms;
        }

        @Override
        public Collection<LensResult> extract(URIRes res1, URIRes res2, NaiscListener log) {
            Resource entity1 = res1.toJena(model);
            Resource entity2 = res2.toJena(model);
            List<Literal> lit1 = forms(entity1);
            List<Literal> lit2 = forms(entity2);
            final List<LangStringPair> labels = Labels.closestLabelsByLang(lit1, lit2);
            for(LangStringPair label : labels) {
                if(language == null || label.lang1.equals(language)) {
                    return new Some<>(LensResult.fromLangStringPair(label, "ontolex"));
                }
            }
            return new None<>();
        }
    }

}
