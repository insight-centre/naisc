package org.insightcentre.uld.naisc.cili

import org.apache.jena.query.Query
import org.apache.jena.query.QueryExecution
import org.apache.jena.rdf.model.*
import org.apache.jena.rdf.model.impl.NodeIteratorImpl
import org.apache.jena.rdf.model.impl.ResIteratorImpl
import org.apache.jena.rdf.model.impl.StmtIteratorImpl
import org.insightcentre.uld.naisc.Dataset
import org.insightcentre.uld.naisc.util.None
import org.insightcentre.uld.naisc.util.Option
import java.io.Closeable
import java.io.File
import java.net.URL
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.util.function.Function
import java.util.function.Predicate

class CILISQLiteDataset(dbFile : File) : Dataset, Closeable {
    override fun close() {
        connection.close()
    }

    override fun id() = dbFile.toString()

    companion object {
        const val ILI = "http://ili.globalwordnet.org/ili/"
        const val RDFS_LABEL = "http://www.w3.org/2000/01/rdf-schema#label"
        const val WN = "http://wordnet-rdf.princeton.edu/ontology#"
        const val WN_POS = WN + "partOfSpeech"
        const val WN_EXAMPLE = WN + "example"
        const val WN_DEF = "http://www.w3.org/2004/02/skos/core#definition"
    }

    private val defs : Map<Int, String>
    private val model = ModelFactory.createDefaultModel()
    private val dbFile = dbFile
    private val connection = CILIConnection(DriverManager.getConnection("jdbc:sqlite:" + dbFile.absolutePath))

    init {
        var defsTemp = mutableMapOf<Int, String>()
            val stat = connection.createStatement()
            stat.use {
                val rs = stat.executeQuery("SELECT id, def FROM ili")
                rs.use {
                    while(rs.next()) {
                        defsTemp.put(rs.getInt(1), rs.getString(2))
                    }
                }
            }
        defs = defsTemp
    }

    //private fun conn() : CILIConnection = CILIConnection(DriverManager.getConnection("jdbc:sqlite:" + dbFile.absolutePath))

    private fun lemmas(connection : CILIConnection, ili : Int) : List<Lemma> {
        val stat = connection.lemmaStat
        var lemmas = mutableListOf<Lemma>()
        stat.setInt(1, ili);
        val rs = stat.executeQuery();
        rs.use {
            while(rs.next()) {
                lemmas.add(Lemma(rs.getString(1), rs.getString(2), rs.getString(3)))
            }
        }
        return lemmas
    }

    private fun ilisWithLemma(connection : CILIConnection, lemma : String, lang : String) : List<Int> {
        val stat = connection.lemmasByIliStat
        var ilis = mutableListOf<Int>()
            stat.setString(1, lemma)
            stat.setString(2, lang)
            val rs = stat.executeQuery();
            rs.use {
                while(rs.next()) {
                    ilis.add(rs.getInt(1))
                }
            }
        return ilis
    }

    private fun ilisWithPos(connection : CILIConnection, pos : String) : List<Int> {
        val stat = connection.ilisByPosStat
        var ilis = mutableListOf<Int>()
            stat.setString(1, pos)
            val rs = stat.executeQuery();
            rs.use {
                while(rs.next()) {
                    ilis.add(rs.getInt(1))
                }
            }
        return ilis
    }



    private fun links(connection : CILIConnection, ili : Int) : List<Relation> {
        val stat = connection.linksStat
        var links = mutableListOf<Relation>()
            stat.setInt(1, ili)
            val rs = stat.executeQuery()
            rs.use {
                while(rs.next()) {
                    links.add(Relation(rs.getInt(1), rs.getString(2)))
                }
            }
        return links
    }

    private fun iliWithLink(connection : CILIConnection, prop : String) : List<Int> {
        val stat = connection.iliWithLinkStat
        var ilis = mutableListOf<Int>()
            stat.setString(1, prop)
            val rs = stat.executeQuery()
            rs.use {
                while(rs.next()) {
                    ilis.add(rs.getInt(1))
                }
            }
        return ilis
    }

    private fun iliWithLinkTarget(connection : CILIConnection, prop : String, targ : Int) : List<Int> {
        val stat = connection.iliWithLinkTargetStat
        var ilis = mutableListOf<Int>()
            stat.setString(1, prop)
            stat.setInt(2, targ)
            val rs = stat.executeQuery()
            rs.use {
                while(rs.next()) {
                    ilis.add(rs.getInt(1))
                }
            }
        return ilis
    }

    private fun examples(connection : CILIConnection, ili : Int) : List<Text> {
        val stat = connection.examplesStat
        var examples = mutableListOf<Text>()
            stat.setInt(1, ili)
            val rs = stat.executeQuery()
            rs.use {
                while(rs.next()) {
                    examples.add(Text(rs.getString(1), rs.getString(2)))
                }
            }
        return examples
    }

    private fun ilisWithExample(connection : CILIConnection, example : Text) : List<Int> {
        val stat = connection.iliWithExamplesStat
        var ilis = mutableListOf<Int>()
            stat.setString(1, example.text)
            stat.setString(2, example.lang)
            val rs = stat.executeQuery()
            rs.use {
                while(rs.next()) {
                    ilis.add(rs.getInt(1))
                }
            }
        return ilis
    }

    private fun definitions(connection : CILIConnection, ili : Int) : List<Text> {
         val stat = connection.definitionsStat;
        var defs = mutableListOf<Text>()
            stat.setInt(1, ili)
            val rs = stat.executeQuery()
            rs.use {
                while(rs.next()) {
                    defs.add(Text(rs.getString(1), rs.getString(2)))
                }
            }
        return defs

    }
    private fun ilisWithDefinition(connection : CILIConnection, defn : Text) : List<Int> {
         val stat = connection.ilisWithDefinitionStat;
        var ilis = mutableListOf<Int>()
            stat.setString(1, defn.text)
            stat.setString(2, defn.lang)
            val rs = stat.executeQuery()
            rs.use {
                while(rs.next()) {
                    ilis.add(rs.getInt(1))
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
            return synchronized(connection, { ilisAsResources(iliWithLink(connection, prop)) })

        } else {
            return emptyRes()
        }
    }

    private fun emptyRes() = ResIteratorImpl(listOf<Resource>().iterator())

    override fun listSubjectsWithProperty(property: Property?, obj: RDFNode?): ResIterator {
        if(property == null || obj == null) {
            throw UnsupportedOperationException("Property or object cannot be null")
        }
            if(property.uri == RDFS_LABEL) {
                if(!obj.isLiteral) {
                    return emptyRes()
                }
                return ilisAsResources(synchronized(connection, { ilisWithLemma(connection, obj.asLiteral().lexicalForm, obj.asLiteral().language) }))
            } else if(property.uri == WN_POS) {
                if(!obj.isURIResource) {
                    return emptyRes()
                }
                return ilisAsResources(synchronized(connection, { ilisWithPos(connection, obj.asResource().uri.substring(WN.length)) }))
            } else if(property.uri == WN_EXAMPLE) {
                if(!obj.isLiteral) {
                    return emptyRes()
                }
                return ilisAsResources(synchronized(connection, { ilisWithExample(connection, Text(obj.asLiteral().lexicalForm, obj.asLiteral().language)) }))
            } else if(property.uri == WN_DEF) {
                if(!obj.isLiteral) {
                    return emptyRes()
                }
                return ilisAsResources(synchronized(connection, { ilisWithDefinition(connection, Text(obj.asLiteral().lexicalForm, obj.asLiteral().language)) }))
            } else if(property.uri.startsWith(WN)) {
                if(!obj.isURIResource || !obj.asResource().uri.startsWith(ILI + "i")) {
                    return emptyRes()
                }
                val targ = obj.asResource().uri.substring(ILI.length + 1).toInt()
                val prop = property.uri.substring(WN.length)
                return ilisAsResources(synchronized(connection, {iliWithLinkTarget(connection, prop, targ) }))
            } else {
                return emptyRes()
            }
    }

    override fun listObjectsOfProperty(r: Resource?, property: Property?): NodeIterator {
        if (r != null && property != null) {
            if (!r.isURIResource || !r.uri.startsWith(ILI + "i")) {
                return NodeIteratorImpl(listOf<RDFNode>().iterator(), null)
            } else {
                val ili = r.uri.substring(ILI.length + 1).toInt()
                if(property.uri == RDFS_LABEL || property.uri == WN_POS) {
                        return NodeIteratorImpl(lemmasAsStatements(synchronized(connection, { lemmas(connection, ili) }), ili).filter { s ->
                            s.predicate == property
                        }.map { s -> s.`object` }.iterator(), null)
                } else if (property.uri == WN_DEF) {
                        return NodeIteratorImpl(definitionsAsStatements(synchronized(connection, { definitions(connection, ili) }), ili).map { s ->
                            s.`object` }.iterator(), null)
                } else if(property.uri == WN_EXAMPLE) {
                        return NodeIteratorImpl(examplesAsStatements(synchronized(connection, { examples(connection, ili) }), ili).map { s ->
                            s.`object` }.iterator(), null)
                } else {
                        return NodeIteratorImpl(linksAsStatements(synchronized(connection, { links(connection, ili) }), ili).filter { s ->
                            s.predicate == property
                        }.map { s -> s.`object` }.iterator(), null)
                }
            }
        } else {
            throw UnsupportedOperationException("Cannot list with one argument null")
        }
    }

    override fun listStatements(source: Resource?, prop: Property?, rdfNode: RDFNode?): StmtIterator {
            val iter = defs.keys.asSequence().flatMap { ili ->
                (lemmasAsStatements(synchronized(connection, { lemmas(connection, ili) }), ili) +
                examplesAsStatements(synchronized(connection, { examples(connection, ili) }), ili) +
                linksAsStatements(synchronized(connection, { links(connection, ili) }), ili) +
                definitionsAsStatements(synchronized(connection, { definitions(connection, ili) }), ili)
                ).asSequence()
            }.filter { s ->
                (source == null || source == s.subject) &&
                        (prop == null || prop == s.predicate) &&
                        (rdfNode == null || rdfNode == s.`object`)
            }
            return StmtIteratorImpl(iter.iterator())
    }

    override fun listStatements(): StmtIterator {
        val iter = defs.keys.asSequence().flatMap { ili ->
            (lemmasAsStatements(synchronized(connection, { lemmas(connection, ili) }), ili) +
            examplesAsStatements(synchronized(connection, { examples(connection, ili) }), ili) +
            linksAsStatements(synchronized(connection, { links(connection, ili) }), ili) +
            definitionsAsStatements(synchronized(connection, { definitions(connection, ili) }), ili)
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

class SQLStmtIteratorImpl(private val i: StmtIterator, private val connection: CILIConnection) : StmtIterator {
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

class CILIConnection(private val connection : Connection) : Closeable {
    override fun close() {
        connection.close()
    }

    val lemmaStat : PreparedStatement by lazy {
        connection.prepareStatement(
            "SELECT DISTINCT f.lemma, lang.bcp47, pos.def FROM f " +
                    "JOIN s ON s.id = f.id " +
                    "JOIN ss on ss.id = s.ss_id " +
                    "JOIN lang on lang.id = f.lang_id " +
                    "JOIN pos on pos.id = f.pos_id " +
                    "WHERE ss.ili_id=?")
    }

    val lemmasByIliStat : PreparedStatement by lazy {
        connection.prepareStatement(
            "SELECT DISTINCT ss.ili_id FROM f " +
                    "JOIN s ON s.id = f.id " +
                    "JOIN ss on ss.id = s.ss_id " +
                    "JOIN lang on lang.id = f.lang_id " +
                    "JOIN pos on pos.id = f.pos_id " +
                    "WHERE f.lemma=? AND lang.bcp47=?")
    }

    val ilisByPosStat by lazy {
        connection.prepareStatement(
            "SELECT DISTINCT ss.ili_id FROM f " +
                    "JOIN s ON s.id = f.id " +
                    "JOIN ss on ss.id = s.ss_id " +
                    "JOIN lang on lang.id = f.lang_id " +
                    "JOIN pos on pos.id = f.pos_id " +
                    "WHERE pos.def=?")
    }

    val linksStat by lazy {
        connection.prepareStatement(
            "SELECT DISTINCT ss2.ili_id, ssrel.rel FROM sslink " +
                    "JOIN ss AS ss1 ON ss1.id = sslink.ss1_id " +
                    "JOIN ss AS ss2 ON ss2.id = sslink.ss2_id " +
                    "JOIN ssrel ON ssrel.id = sslink.ssrel_id " +
                    "WHERE ss1.ili_id=?")
    }

    val iliWithLinkStat by lazy {
        connection.prepareStatement(
            "SELECT DISTINCT ss1.ili_id FROM sslink " +
                    "JOIN ss AS ss1 ON ss1.id = sslink.ss1_id " +
                    "JOIN ssrel ON ssrel.id = sslink.ssrel_id " +
                    "WHERE ssrel.rel=?")
    }

    val iliWithLinkTargetStat by lazy {
        connection.prepareStatement(
            "SELECT DISTINCT ss1.ili_id FROM sslink " +
                    "JOIN ss AS ss1 ON ss1.id = sslink.ss1_id " +
                    "JOIN ss AS ss2 ON ss2.id = sslink.ss2_id " +
                    "JOIN ssrel ON ssrel.id = sslink.ssrel_id " +
                    "WHERE ssrel.rel=? AND ss2.ili_id=?")
    }

    val examplesStat by lazy {
        connection.prepareStatement(
            "SELECT DISTINCT ssexe.ssexe, lang.bcp47 FROM ssexe " +
                    "JOIN ss ON ss.id = ssexe.ss_id " +
                    "JOIN lang on lang.id = ssexe.lang_id " +
                    "WHERE ss.ili_id=?")
    }

    val iliWithExamplesStat by lazy {
        connection.prepareStatement(
            "SELECT DISTINCT ss.ili_id FROM ssexe " +
                    "JOIN ss ON ss.id = ssexe.ss_id " +
                    "JOIN lang on lang.id = ssexe.lang_id " +
                    "WHERE ssexe.ssexe=? AND lang.bcp47=?")
    }

    val definitionsStat by lazy {
        connection.prepareStatement(
            "SELECT DISTINCT def.def, lang.bcp47 FROM def " +
                    "JOIN ss ON ss.id = def.ss_id " +
                    "JOIN lang on lang.id = def.lang_id " +
                    "WHERE ss.ili_id=?")
    }

    val ilisWithDefinitionStat by lazy {
        connection.prepareStatement(
            "SELECT DISTINCT ss.ili_id FROM def " +
                    "JOIN ss ON ss.id = def.ss_id " +
                    "JOIN lang on lang.id = def.lang_id " +
                    "WHERE def.def=? AND lang.bcp47=?")
    }

    fun createStatement() = connection.createStatement()
}