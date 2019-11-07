package org.insightcentre.uld.naisc.cili

import org.apache.jena.query.Query
import org.apache.jena.query.QueryExecution
import org.apache.jena.rdf.model.*
import org.apache.jena.rdf.model.impl.NodeIteratorImpl
import org.apache.jena.rdf.model.impl.ResIteratorImpl
import org.apache.jena.rdf.model.impl.StmtIteratorImpl
import org.apache.jena.util.iterator.ExtendedIterator
import org.insightcentre.uld.naisc.Dataset
import org.insightcentre.uld.naisc.util.None
import org.insightcentre.uld.naisc.util.Option
import java.io.File
import java.net.URL
import java.sql.Connection
import java.sql.DriverManager
import java.util.function.Function
import java.util.function.Predicate

class CILISQLiteDataset(dbFile : File) : Dataset {
    companion object {
        const val ILI = "http://ili.globalwordnet.org/ili/"
        const val RDFS_LABEL = "http://www.w3.org/2000/01/rdf-schema#label"
        const val WN = "http://wordnet-rdf.princeton.edu/ontology#"
        const val WN_POS = WN + "partOfSpeech"
        const val WN_EXAMPLE = WN + "example"
        const val WN_DEF = "http://www.w3.org/2004/02/skos/core#"
    }

    private val defs : Map<Int, String>
    private val model = ModelFactory.createDefaultModel()
    private val dbFile = dbFile

    init {
        val connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.absolutePath)
        var defsTemp = mutableMapOf<Int, String>()
        connection.use {
            val stat = connection.createStatement()
            stat.use {
                val rs = stat.executeQuery("SELECT id, def FROM ili")
                rs.use {
                    while(rs.next()) {
                        defsTemp.put(rs.getInt(1), rs.getString(2))
                    }
                }
            }
        }
        defs = defsTemp
    }

    private fun conn() : Connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.absolutePath)

    private fun lemmas(connection : Connection, ili : Int) : List<Lemma> {
        val stat = connection.prepareStatement(
            "SELECT DISTINCT f.lemma, lang.bcp47, pos.def FROM f " +
                    "JOIN s ON s.id = f.id " +
                    "JOIN ss on ss.id = s.ss_id " +
                    "JOIN lang on lang.id = f.lang_id " +
                    "JOIN pos on pos.id = f.pos_id " +
                    "WHERE ss.ili_id=?");
        var lemmas = mutableListOf<Lemma>()
        stat.use {
            stat.setInt(1, ili);
            val rs = stat.executeQuery();
            rs.use {
                while(rs.next()) {
                    lemmas.add(Lemma(rs.getString(1), rs.getString(2), rs.getString(3)))
                }
            }
        }
        return lemmas
    }

    private fun ilisWithLemma(connection : Connection, lemma : String, lang : String) : List<Int> {
        val stat = connection.prepareStatement(
            "SELECT DISTINCT ss.ili_id FROM f " +
                    "JOIN s ON s.id = f.id " +
                    "JOIN ss on ss.id = s.ss_id " +
                    "JOIN lang on lang.id = f.lang_id " +
                    "JOIN pos on pos.id = f.pos_id " +
                    "WHERE f.lemma=? AND lang.bcp47=?");
        var ilis = mutableListOf<Int>()
        stat.use {
            stat.setString(1, lemma)
            stat.setString(2, lang)
            val rs = stat.executeQuery();
            rs.use {
                while(rs.next()) {
                    ilis.add(rs.getInt(1))
                }
            }
        }
        return ilis
    }

    private fun ilisWithPos(connection : Connection, pos : String) : List<Int> {
        val stat = connection.prepareStatement(
            "SELECT DISTINCT ss.ili_id FROM f " +
                    "JOIN s ON s.id = f.id " +
                    "JOIN ss on ss.id = s.ss_id " +
                    "JOIN lang on lang.id = f.lang_id " +
                    "JOIN pos on pos.id = f.pos_id " +
                    "WHERE pos.def=?")
        var ilis = mutableListOf<Int>()
        stat.use {
            stat.setString(1, pos)
            val rs = stat.executeQuery();
            rs.use {
                while(rs.next()) {
                    ilis.add(rs.getInt(1))
                }
            }
        }
        return ilis
    }



    private fun links(connection : Connection, ili : Int) : List<Relation> {
        val stat = connection.prepareStatement(
            "SELECT DISTINCT ss2.ili_id, ssrel.rel FROM sslink " + // TODO: Ask Francis about ssrel.rel
                    "JOIN ss AS ss1 ON ss1.id = sslink.ss1_id " +
                    "JOIN ss AS ss2 ON ss2.id = sslink.ss2_id " +
                    "JOIN ssrel ON ssrel.id = sslink.ssrel_id " +
                    "WHERE ss1.ili_id=?")
        var links = mutableListOf<Relation>()
        stat.use {
            stat.setInt(1, ili)
            val rs = stat.executeQuery()
            rs.use {
                while(rs.next()) {
                    links.add(Relation(rs.getInt(1), rs.getString(2)))
                }
            }
        }
        return links
    }

    private fun iliWithLink(connection : Connection, prop : String) : List<Int> {
        val stat = connection.prepareStatement(
            "SELECT DISTINCT ss1.ili_id FROM sslink " + // TODO: Ask Francis about ssrel.rel
                    "JOIN ss AS ss1 ON ss1.id = sslink.ss1_id " +
                    "JOIN ssrel ON ssrel.id = sslink.ssrel_id " +
                    "WHERE ssrel.rel=?")
        var ilis = mutableListOf<Int>()
        stat.use {
            stat.setString(1, prop)
            val rs = stat.executeQuery()
            rs.use {
                while(rs.next()) {
                    ilis.add(rs.getInt(1))
                }
            }
        }
        return ilis
    }

    private fun iliWithLinkTarget(connection : Connection, prop : String, targ : Int) : List<Int> {
        val stat = connection.prepareStatement(
            "SELECT DISTINCT ss1.ili_id FROM sslink " + // TODO: Ask Francis about ssrel.rel
                    "JOIN ss AS ss1 ON ss1.id = sslink.ss1_id " +
                    "JOIN ss AS ss2 ON ss2.id = sslink.ss2_id " +
                    "JOIN ssrel ON ssrel.id = sslink.ssrel_id " +
                    "WHERE ssrel.rel=? AND ss2.ili_id=?")
        var ilis = mutableListOf<Int>()
        stat.use {
            stat.setString(1, prop)
            stat.setInt(2, targ)
            val rs = stat.executeQuery()
            rs.use {
                while(rs.next()) {
                    ilis.add(rs.getInt(1))
                }
            }
        }
        return ilis
    }

    private fun examples(connection : Connection, ili : Int) : List<Text> {
        val stat = connection.prepareStatement(
            "SELECT DISTINCT ssexe.ssexe, lang.bcp47 FROM ssexe " +
                    "JOIN ss ON ss.id = ssexe.ss_id " +
                    "JOIN lang on lang.id = ssexe.lang_id " +
                    "WHERE ss.ili_id=?");
        var examples = mutableListOf<Text>()
        stat.use {
            stat.setInt(1, ili)
            val rs = stat.executeQuery()
            rs.use {
                while(rs.next()) {
                    examples.add(Text(rs.getString(1), rs.getString(2)))
                }
            }
        }
        return examples
    }

    private fun ilisWithExample(connection : Connection, example : Text) : List<Int> {
        val stat = connection.prepareStatement(
            "SELECT DISTINCT ss.ili_id FROM ssexe " +
                    "JOIN ss ON ss.id = ssexe.ss_id " +
                    "JOIN lang on lang.id = ssexe.lang_id " +
                    "WHERE ssexe.ssexe=? AND lang.bcp47=?");
        var ilis = mutableListOf<Int>()
        stat.use {
            stat.setString(1, example.text)
            stat.setString(2, example.lang)
            val rs = stat.executeQuery()
            rs.use {
                while(rs.next()) {
                    ilis.add(rs.getInt(1))
                }
            }
        }
        return ilis
    }

    private fun definitions(connection : Connection, ili : Int) : List<Text> {
         val stat = connection.prepareStatement(
            "SELECT DISTINCT def.def, lang.bcp47 FROM def " +
                    "JOIN ss ON ss.id = def.ss_id " +
                    "JOIN lang on lang.id = def.lang_id " +
                    "WHERE ss.ili_id=?");
        var defs = mutableListOf<Text>()
        stat.use {
            stat.setInt(1, ili)
            val rs = stat.executeQuery()
            rs.use {
                while(rs.next()) {
                    defs.add(Text(rs.getString(1), rs.getString(2)))
                }
            }
        }
        return defs

    }
    private fun ilisWithDefinition(connection : Connection, defn : Text) : List<Int> {
         val stat = connection.prepareStatement(
            "SELECT DISTINCT ss.ili_id FROM def " +
                    "JOIN ss ON ss.id = def.ss_id " +
                    "JOIN lang on lang.id = def.lang_id " +
                    "WHERE def.def=? AND lang.bcp47=?");
        var ilis = mutableListOf<Int>()
        stat.use {
            stat.setString(1, defn.text)
            stat.setString(2, defn.lang)
            val rs = stat.executeQuery()
            rs.use {
                while(rs.next()) {
                    ilis.add(rs.getInt(1))
                }
            }
        }
        return ilis
    }


    private fun lemmasAsStatements(lemmas : List<Lemma>, ili : Int) : List<Statement> {
        return lemmas.flatMap { l ->
            listOf(model.createStatement(
                    model.createResource(ILI + "i" + ili),
                    model.createProperty(RDFS_LABEL),
                    model.createLiteral(l.lemma, l.lang)),
                model.createStatement(
                    model.createResource(ILI + "i" + ili),
                    model.createProperty(WN_POS),
                    model.createResource(WN + l.pos))
                )
        }
    }

    private fun linksAsStatements(links : List<Relation>, ili : Int) : List<Statement> {
        return links.map { l ->
            model.createStatement(
                model.createResource(ILI + "i" + ili),
                model.createProperty(WN + l.type),
                model.createResource(ILI + "i" + l.targ))
        }
    }

    private fun examplesAsStatements(examples : List<Text>, ili : Int) : List<Statement> {
        return examples.map { x ->
            model.createStatement(
                model.createResource(ILI + "i" + ili),
                model.createProperty(WN_EXAMPLE),
                model.createLiteral(x.text, x.lang)
            )
        }
    }

    private fun definitionsAsStatements(definitions : List<Text>, ili : Int) : List<Statement> {
        return definitions.map { x ->
             model.createStatement(
                model.createResource(ILI + "i" + ili),
                model.createProperty(WN_DEF),
                model.createLiteral(x.text, x.lang)
            )
        }
    }

    private fun ilisAsResources(ilis : Collection<Int>) : ResIterator {
        return ResIteratorImpl(ilis.map { key:Int ->
            model.createResource(ILI + "i" + key)
        }.iterator())
    }

    override fun listSubjects(): ResIterator {
        return ilisAsResources(defs.keys)
    }

    override fun createProperty(uri: String?): Property {
        return model.createProperty(uri)
    }

    override fun createResource(uri: String?): Resource {
        return model.createResource(uri)
    }

    override fun listSubjectsWithProperty(property : Property?): ResIterator {
        if(property == null) {
            throw UnsupportedOperationException("Propert cannot be null")
        }
        if(property.uri == RDFS_LABEL || property.uri == WN_EXAMPLE || property.uri == WN_POS || property.uri == WN_DEF) {
            // Kind of a hack here... if we recognize the property we assume that the value is always set
            return listSubjects()
        } else if(property.uri.startsWith(WN)) {
            val prop = property.uri.substring(WN.length)
            val connection = conn()
            connection.use {
                return ilisAsResources(iliWithLink(connection, prop))
            }

        } else {
            return emptyRes()
        }
    }

    private fun emptyRes() = ResIteratorImpl(listOf<Resource>().iterator())

    override fun listSubjectsWithProperty(property: Property?, obj: RDFNode?): ResIterator {
        if(property == null || obj == null) {
            throw UnsupportedOperationException("Property or object cannot be null")
        }
        val connection = conn()
        connection.use {
            if(property.uri == RDFS_LABEL) {
                if(!obj.isLiteral) {
                    return emptyRes()
                }
                return ilisAsResources(ilisWithLemma(connection, obj.asLiteral().lexicalForm, obj.asLiteral().language))
            } else if(property.uri == WN_POS) {
                if(!obj.isURIResource) {
                    return emptyRes()
                }
                return ilisAsResources(ilisWithPos(connection, obj.asResource().uri.substring(WN.length)))
            } else if(property.uri == WN_EXAMPLE) {
                if(!obj.isLiteral) {
                    return emptyRes()
                }
                return ilisAsResources(ilisWithExample(connection, Text(obj.asLiteral().lexicalForm, obj.asLiteral().language)))
            } else if(property.uri == WN_DEF) {
                if(!obj.isLiteral) {
                    return emptyRes()
                }
                return ilisAsResources(ilisWithDefinition(connection, Text(obj.asLiteral().lexicalForm, obj.asLiteral().language)))
            } else if(property.uri.startsWith(WN)) {
                if(!obj.isURIResource || !obj.asResource().uri.startsWith(ILI + "i")) {
                    return emptyRes()
                }
                val targ = obj.asResource().uri.substring(ILI.length + 1).toInt()
                val prop = property.uri.substring(WN.length)
                return ilisAsResources(iliWithLinkTarget(connection, prop, targ))
            } else {
                return emptyRes()
            }
        }
    }

    override fun listObjectsOfProperty(r: Resource?, property: Property?): NodeIterator {
        if (r != null && property != null) {
            if (!r.isURIResource || !r.uri.startsWith(ILI + "i")) {
                return NodeIteratorImpl(listOf<RDFNode>().iterator(), null)
            } else {
                val ili = r.uri.substring(ILI.length + 1).toInt()
                if(property.uri == RDFS_LABEL || property.uri == WN_POS) {
                    val connection = conn();
                    connection.use {
                        return NodeIteratorImpl(lemmasAsStatements(lemmas(connection, ili), ili).filter { s ->
                            s.predicate == property
                        }.map { s -> s.`object` }.iterator(), null)
                    }
                } else if (property.uri == WN_DEF) {
                    val connection = conn();
                    connection.use {
                        return NodeIteratorImpl(definitionsAsStatements(definitions(connection, ili), ili).map { s ->
                            s.`object` }.iterator(), null)
                    }
                } else if(property.uri == WN_EXAMPLE) {
                    val connection = conn();
                    connection.use {
                        return NodeIteratorImpl(examplesAsStatements(examples(connection, ili), ili).map { s ->
                            s.`object` }.iterator(), null)
                    }
                } else {
                    val connection = conn();
                    connection.use {
                        return NodeIteratorImpl(linksAsStatements(links(connection, ili), ili).filter { s ->
                            s.predicate == property
                        }.map { s -> s.`object` }.iterator(), null)
                    }
                }
            }
        } else {
            throw UnsupportedOperationException("Cannot list with one argument null")
        }
    }

    override fun listStatements(source: Resource?, prop: Property?, rdfNode: RDFNode?): StmtIterator {
        val connection = conn()
        connection.use {
            val iter = defs.keys.asSequence().flatMap { ili ->
                (lemmasAsStatements(lemmas(connection, ili), ili) +
                examplesAsStatements(examples(connection, ili), ili) +
                linksAsStatements(links(connection, ili), ili) +
                definitionsAsStatements(definitions(connection, ili), ili)
                ).asSequence()
            }.filter { s ->
                (source == null || source == s.subject) &&
                        (prop == null || prop == s.predicate) &&
                        (rdfNode == null || rdfNode == s.`object`)
            }
            return StmtIteratorImpl(iter.iterator())
        }
    }

    override fun listStatements(): StmtIterator {
        val connection = conn()
        val iter = defs.keys.asSequence().flatMap { ili ->
            (lemmasAsStatements(lemmas(connection, ili), ili) +
            examplesAsStatements(examples(connection, ili), ili) +
            linksAsStatements(links(connection, ili), ili) +
            definitionsAsStatements(definitions(connection, ili), ili)
            ).asSequence()
        }
        return SQLStmtIteratorImpl(StmtIteratorImpl(iter.iterator()), connection)
    }

    override fun createQuery(query: Query?): QueryExecution {
        throw UnsupportedOperationException("Cannot execute SPARQL queries over CILI database, please do not use the module that calls this")
    }

    override fun asEndpoint(): Option<URL> {
        return None()
    }

}

data class Lemma(val lemma : String, val lang : String, val pos : String)

data class Relation(val targ : Int, val type : String)

data class Text(val text : String, val lang : String)

class SQLStmtIteratorImpl(val i: StmtIterator, val connection: Connection) : StmtIterator {
    override fun nextStatement() = i.nextStatement()
    override fun removeNext() = i.removeNext()
    override fun remove() = i.remove()
    override fun <U : Any?> mapWith(p0: Function<Statement, U>?) = i.mapWith(p0)
    override fun toSet() = i.toSet()
    override fun filterDrop(p0: Predicate<Statement>?) = i.filterDrop(p0)
    override fun next() = i.next()
    override fun filterKeep(p0: Predicate<Statement>?) = i.filterKeep(p0)
    override fun <X : Statement?> andThen(p0: MutableIterator<X>?) = i.andThen(p0)
    override fun toList() = i.toList()
    override fun hasNext() = i.hasNext()
    override fun close() {
        i.close()
        connection.close()
    }

}