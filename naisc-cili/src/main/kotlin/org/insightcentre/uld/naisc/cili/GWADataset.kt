package org.insightcentre.uld.naisc.cili

import org.apache.jena.query.Query
import org.apache.jena.query.QueryExecution
import org.apache.jena.rdf.model.*
import org.insightcentre.uld.naisc.Dataset
import org.insightcentre.uld.naisc.util.None
import org.insightcentre.uld.naisc.util.Option
import org.w3c.dom.Element
import java.io.File
import java.net.URL
import javax.xml.parsers.DocumentBuilderFactory

class GWADataset(xmlFile : File) : Dataset {
    val lang : String
    init {
        val dbFactory = DocumentBuilderFactory.newInstance()
        val dBuilder = dbFactory.newDocumentBuilder()
        val doc = dBuilder.parse(xmlFile)
        val children = doc.documentElement.childNodes.item(0).childNodes
        var lang0 = ""
        var synset2lemmas = mutableMapOf<String, List<String>>()
        for(i in 0 until children.length) {
            val child = children.item(i)
            if(child is Element) {
                lang0 = child.getAttribute("language")
            }
            val children2 = child.childNodes
            for(j in 0 until children2.length) {
                val child2 = children.item(i)
                if(child2 is Element) {
                    if(child2.tagName == "LexicalEntry") {
                        processEntry(child2, synset2lemmas)
                    } else if(child2.tagName == "Synset") {
                        processSynset(child2)
                    } else {
                        throw UnsupportedOperationException("Unexpected tag name: " + child2.tagName)
                    }
                }
            }
        }
        lang = lang0
    }

    private fun processEntry(elem : Element, synset2lemmas : MutableMap<String, List<String>>) {
        val lemma = elem.getElementsByTagName("Lemma")
        if(lemma == null || lemma.length != 1) {
            throw LMFFormatException("LexicalEntry without Lemma")
        }
        val lemmaText : String = lemma.item(0).attributes.getNamedItem("writtenForm").textContent
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
        }
    }
    private fun processSynset(elem : Element) { }

    override fun asEndpoint(): Option<URL> {
        return None()
    }

    override fun listSubjects(): ResIterator {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createProperty(uri: String?): Property {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createResource(uri: String?): Resource {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun listSubjectsWithProperty(createProperty: Property?): ResIterator {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun listSubjectsWithProperty(createProperty: Property?, `object`: RDFNode?): ResIterator {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun listObjectsOfProperty(r: Resource?, createProperty: Property?): NodeIterator {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun listStatements(source: Resource?, prop: Property?, rdfNode: RDFNode?): StmtIterator {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun listStatements(): StmtIterator {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createQuery(query: Query?): QueryExecution {
        throw UnsupportedOperationException("Cannot query an XML file")
    }

}

data class Synset(val id : String, val lemmas : List<String>, val pos : String, val definitions : List<String>,
    val examples : List<String>, val relations : List<Pair<String, String>>)

class LMFFormatException(message : String) : RuntimeException(message)