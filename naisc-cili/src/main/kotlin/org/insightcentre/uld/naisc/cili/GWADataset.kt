package org.insightcentre.uld.naisc.cili

import org.apache.jena.query.Query
import org.apache.jena.query.QueryExecution
import org.apache.jena.rdf.model.*
import org.apache.jena.rdf.model.impl.NodeIteratorImpl
import org.apache.jena.rdf.model.impl.ResIteratorImpl
import org.apache.jena.rdf.model.impl.StmtIteratorImpl
import org.insightcentre.uld.naisc.Alignment.SKOS_EXACT_MATCH
import org.insightcentre.uld.naisc.Dataset
import org.insightcentre.uld.naisc.cili.CILISQLiteDataset.Companion.ILI
import org.insightcentre.uld.naisc.cili.CILISQLiteDataset.Companion.RDFS_LABEL
import org.insightcentre.uld.naisc.cili.CILISQLiteDataset.Companion.WN
import org.insightcentre.uld.naisc.cili.CILISQLiteDataset.Companion.WN_DEF
import org.insightcentre.uld.naisc.cili.CILISQLiteDataset.Companion.WN_EXAMPLE
import org.insightcentre.uld.naisc.cili.CILISQLiteDataset.Companion.WN_POS
import org.insightcentre.uld.naisc.util.None
import org.insightcentre.uld.naisc.util.Option
import org.w3c.dom.Element
import java.io.File
import java.net.URL
import javax.xml.parsers.DocumentBuilderFactory

class GWADataset(xmlFile : File) : Dataset {
    val stats : List<Statement>
    val model = ModelFactory.createDefaultModel()
    val prefix : String
    companion object {
        val POS_MAP = mapOf(
            "n" to "noun",
            "v" to "verb",
            "a" to "adjective",
            "r" to "adverb",
            "s" to "adjective_satellite",
            "c" to "conjunction",
            "p" to "adposition",
            "x" to "other",
            "u" to "unknown"
        )
    }

    val id = xmlFile.toString()
    override fun id() = id

    init {
        val dbFactory = DocumentBuilderFactory.newInstance()
        val dBuilder = dbFactory.newDocumentBuilder()
        val doc = dBuilder.parse(xmlFile)
        val children = doc.documentElement.getElementsByTagName("Lexicon")
        var lang0 = ""
        var synset2lemmas = mutableMapOf<String, List<String>>()
        var entryPos = mutableMapOf<String, String>()
        var synsetsTemp = mutableListOf<Synset>()
        for(i in 0 until children.length) {
            val child = children.item(i)
            if(child is Element) {
                lang0 = child.getAttribute("language")
            }
            val children2 = child.childNodes
            for(j in 0 until children2.length) {
                val child2 = children2.item(j)
                if(child2 is Element) {
                    if(child2.tagName == "LexicalEntry") {
                        processEntry(child2, synset2lemmas, entryPos)
                    } else if(child2.tagName == "Synset") {
                        synsetsTemp.add(processSynset(child2, synset2lemmas, entryPos, lang0))
                    } else {
                        throw UnsupportedOperationException("Unexpected tag name: " + child2.tagName)
                    }
                }
            }
        }
        prefix = "file:" + xmlFile.absolutePath + "#"
        stats = synsetsTemp.flatMap { ss -> ss.statements(model, prefix) }
    }

    private fun processEntry(elem : Element, synset2lemmas : MutableMap<String, List<String>>,
            entryPos : MutableMap<String, String>) {
        val lemma = elem.getElementsByTagName("Lemma")
        if(lemma == null || lemma.length != 1) {
            throw LMFFormatException("LexicalEntry without Lemma")
        }
        val pos = POS_MAP.get(lemma.item(0)?.attributes?.getNamedItem("partOfSpeech")?.textContent) ?: throw LMFFormatException("Unknown part of speech value" + lemma.item(0)?.attributes?.getNamedItem("partOfSpeech")?.textContent)
        val lemmaText : String? = lemma.item(0)?.attributes?.getNamedItem("writtenForm")?.textContent
        if(lemmaText == null) {
            throw LMFFormatException("Lemma without @writtenForm")
        }
        val senses = elem.getElementsByTagName("Sense")
        if(senses == null) {
            throw LMFFormatException("LexicalEntry has no Senses")
        }
        for(i in 0 until senses.length) {
            val synset = senses.item(i).attributes.getNamedItem("synset").textContent
            if(synset == null) {
                throw LMFFormatException("Sense without synset")
            }
            synset2lemmas.put(synset, (synset2lemmas.get(synset)?: listOf<String>()) + lemmaText)
            entryPos.put(synset, pos)
        }
    }

    private fun processSynset(elem : Element, synset2lemmas: Map<String, List<String>>, entryPos : Map<String, String>,
        lang : String) : Synset {
       val idTag = elem.attributes.getNamedItem("id") ?: throw LMFFormatException("Synset without @id")
        val id = idTag.textContent
         val definitions = elem.getElementsByTagName("Definition")
        if(definitions == null) {
            throw LMFFormatException("No definitions")
        }
        val defs = mutableListOf<String>()
        for(i in 0 until definitions.length) {
            defs.add(definitions.item(i).textContent)
        }
        val examples = elem.getElementsByTagName("Example")
        val exs = mutableListOf<String>()
        if(examples != null) {
            for(i in 0 until examples.length) {
                exs.add(examples.item(i).textContent)
            }
        }
        val partOfSpeech = elem.attributes.getNamedItem("partOfSpeech")
        val pos : String
        if(partOfSpeech == null) {
            val epos = entryPos.get(id)
            if(epos != null) {
                pos = epos
            } else {
                throw LMFFormatException("No part of speech for synset with no lemmas")
            }
        } else {
            pos = POS_MAP.get(partOfSpeech.textContent) ?: throw LMFFormatException("Unsupported pos value: " + partOfSpeech.textContent)
        }
        val iliAttr = elem.attributes.getNamedItem("ili")
        var ili = 0
        if(iliAttr != null && iliAttr.textContent.matches("i\\d+".toRegex())) {
            ili = iliAttr.textContent.substring(1).toInt()
        }
        val relations = elem.getElementsByTagName("SynsetRelation")
        val rels = mutableListOf<Pair<String,String>>()
        if(relations != null) {
            for(i in 0 until relations.length) {
                val j = relations.item(i)
                val t = j.attributes.getNamedItem("relType")
                val g = j.attributes.getNamedItem("target")
                if(t != null && g != null) {
                    rels.add(Pair(t.textContent, g.textContent))
                } else {
                    throw LMFFormatException("SynsetRelation without relType or target")
                }
            }
        }

        return Synset(id, lang, synset2lemmas.get(id) ?: listOf(),
            pos, defs, exs, rels, ili)

    }

    override fun asEndpoint(): Option<URL> {
        return None()
    }

    override fun listSubjects(): ResIterator {
        return ResIteratorImpl(stats.map { s -> s.subject }.toSet().iterator())
    }

    override fun createProperty(uri: String?): Property {
        return model.createProperty(uri)
    }

    override fun createResource(uri: String?): Resource {
        return model.createResource(uri)
    }

    override fun listSubjectsWithProperty(property: Property?): ResIterator {
        return ResIteratorImpl(stats.filter { s -> s.predicate == property }
                .map{ s -> s.subject }
                .toSet()
                .iterator())
    }

    override fun listSubjectsWithProperty(property: Property?, obj: RDFNode?): ResIterator {
        return ResIteratorImpl(stats.filter { s ->
            (property == null || s.predicate == property) &&
                    (obj == null || s.`object` == obj) }
                .map { s -> s.subject }
                .toSet()
                .iterator())
    }

    override fun listObjectsOfProperty(r: Resource?, property: Property?): NodeIterator {
        return NodeIteratorImpl(stats.filter { s ->
            (r == null || s.subject == r) &&
                    (property == null || s.predicate == property)
        }.map { s -> s.`object` }
                .toSet().iterator(), null)
    }

    override fun listStatements(source: Resource?, prop: Property?, rdfNode: RDFNode?): StmtIterator {
        return StmtIteratorImpl(stats.filter { s ->
                (source == null || source == s.subject) &&
                        (prop == null || prop == s.predicate) &&
                        (rdfNode == null || rdfNode == s.`object`)
        }.iterator())
    }

    override fun listStatements(): StmtIterator {
        return StmtIteratorImpl(stats.iterator())
    }

    override fun createQuery(query: Query?): QueryExecution {
        throw UnsupportedOperationException("Cannot query an XML file")
    }

}

data class Synset(val id : String, val lang : String, val lemmas : List<String>, val pos : String, val definitions : List<String>,
    val examples : List<String>, val relations : List<Pair<String, String>>, val ili : Int) {
    fun statements(model : Model, prefix : String) : List<Statement> {
        val subj = model.createResource(prefix + id)
        return lemmas.map { l ->
            model.createStatement(
                subj,
                model.createProperty(RDFS_LABEL),
                model.createLiteral(l, lang)
            )
        } + listOf(model.createStatement(
                subj,
                model.createProperty(WN_POS),
                model.createResource(WN + pos))) +
        definitions.map { d ->
            model.createStatement(subj,
                model.createProperty(WN_DEF),
                model.createLiteral(d, lang)
            )
        } +
        examples.map { x ->
            model.createStatement(subj,
                model.createProperty(WN_EXAMPLE),
                model.createLiteral(x, lang))
         } +
         relations.map { r ->
            model.createStatement(subj,
                model.createProperty(WN + r.first),
                model.createResource(prefix + r.second)
            )
         } + (if(ili > 0) {
            listOf(model.createStatement(subj,
                model.createProperty(SKOS_EXACT_MATCH),
                model.createResource(ILI + "i" + ili)))
        } else {
            listOf<Statement>()
        })
    }
}

class LMFFormatException(message : String) : RuntimeException(message)