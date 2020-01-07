package org.insightcentre.uld.naisc.cili

import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.Resource
import org.insightcentre.uld.naisc.cili.CILISQLiteDataset.Companion.ILI
import org.insightcentre.uld.naisc.cili.CILISQLiteDataset.Companion.RDFS_LABEL
import org.insightcentre.uld.naisc.cili.CILISQLiteDataset.Companion.WN
import org.insightcentre.uld.naisc.cili.CILISQLiteDataset.Companion.WN_DEF
import org.insightcentre.uld.naisc.cili.CILISQLiteDataset.Companion.WN_EXAMPLE
import org.insightcentre.uld.naisc.cili.CILISQLiteDataset.Companion.WN_POS
import org.junit.Assert.*;
import java.io.File
import java.sql.DriverManager

class CILISQLiteDatasetTest {
    val file : File
    val model = ModelFactory.createDefaultModel()
    init {
        file = File.createTempFile("cilisql", ".db")
        file.deleteOnExit()
        val connection = DriverManager.getConnection("jdbc:sqlite:" + file.absolutePath)
        connection.use {
            val stat = connection.createStatement()
            stat.use {
                stat.execute("CREATE TABLE lang\n" +
                        "       -- English will always be 1 (and is often the fallback)\n" +
                        "       -- language names are in lang_name\n" +
                        "       (id INTEGER PRIMARY KEY ASC,   -- 1\n" +
                        "        bcp47 TEXT NOT NULL,          -- 'en'\n" +
                        "        iso639 TEXT,                  -- 'eng'    \n" +
                        "        u INTEGER NOT NULL, \n" +
                        "        t TIMESTAMP DEFAULT CURRENT_TIMESTAMP);")
                stat.execute("INSERT INTO lang VALUES (1,\"en\",\"eng\",\"foo\",date(\"now\"))")
                stat.execute("CREATE TABLE ili\n" +
                        "       (id INTEGER PRIMARY KEY ASC,\n" +
                        "        kind_id INTEGER NOT NULL,\n" +
                        "        def TEXT NOT NULL,\n" +
                        "        status_id INTEGER NOT NULL,\n" +
                        "        superseded_by_id INTEGER,\n" +
                        "        origin_src_id INTEGER NOT NULL,\n" +
                        "        src_key TEXT NOT NULL,\n" +
                        "        u INTEGER NOT NULL,\n" +
                        "        t TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\n" +
                        //"        FOREIGN KEY(origin_src_id) REFERENCES src(id),\n" +
                        //"        FOREIGN KEY(kind_id) REFERENCES kind(id),\n" +
                        "        FOREIGN KEY(superseded_by_id) REFERENCES ili(id))")
                        //"        FOREIGN KEY(status_id) REFERENCES status(id));")
                stat.execute("INSERT INTO ili VALUES (1,1,\"definition\",1,NULL,1,\"00001740-a\",\"foo\",date(\"now\"))")
                stat.execute("CREATE TABLE pos\n" +
                        "       (id INTEGER PRIMARY KEY ASC,\n" +
                        "        tag TEXT NOT NULL,\n" +
                        "        def TEXT NOT NULL,\n" +
                        "        u INTEGER NOT NULL,\n" +
                        "        t TIMESTAMP DEFAULT CURRENT_TIMESTAMP);")
                stat.execute("INSERT INTO pos VALUES (1,\"n\",\"noun\",\"foo\",date(\"now\"))")
                stat.execute("CREATE TABLE ssrel\n" +
                        "       (id INTEGER PRIMARY KEY ASC,\n" +
                        "        rel TEXT NOT NULL,\n" +
                        "        def TEXT NOT NULL,\n" +
                        "        u INTEGER NOT NULL,\n" +
                        "        t TIMESTAMP DEFAULT CURRENT_TIMESTAMP);\n")
                stat.execute("INSERT INTO ssrel VALUES (1,\"hypernym\",\"hypernym\",\"foo\",date(\"now\"))")
                stat.execute("CREATE TABLE ss\n" +
                        "       (id INTEGER PRIMARY KEY ASC,\n" +
                        "        ili_id INTEGER,\n" +
                        "        pos_id INTEGER NOT NULL,\n" +
                        "        u INTEGER NOT NULL,\n" +
                        "        t TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\n" +
                        "        FOREIGN KEY(pos_id) REFERENCES pos(id));\n")
                stat.execute("INSERT INTO ss VALUES(1, 1, 1, \"foo\", date(\"now\"))")
                stat.execute("CREATE TABLE f\n" +
                        "       (id INTEGER PRIMARY KEY ASC,\n" +
                        "        lang_id INTEGER NOT NULL,\n" +
                        "        pos_id INTEGER NOT NULL,\n" +
                        "        lemma TEXT NOT NULL,\n" +
                        "        u INTEGER NOT NULL,\n" +
                        "        t TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\n" +
                        "        FOREIGN KEY(lang_id) REFERENCES lang(id),\n" +
                        "        FOREIGN KEY(pos_id) REFERENCES pos(id));\n")
                stat.execute("INSERT INTO f VALUES (1,1,1,\"able\",\"foo\",date(\"now\"))")
                stat.execute("CREATE TABLE s\n" +
                        "       (id INTEGER PRIMARY KEY ASC,\n" +
                        "        ss_id INTEGER NOT NULL,  -- synset id\n" +
                        "        w_id INTEGER NOT NULL,   -- word id\n" +
                        "        freq INTEGER DEFAULT 0,  -- total frequency of lemma\n" +
                        "                                 -- summed from sense_meta\n" +
                        "        u INTEGER NOT NULL,      -- who added it    \n" +
                        "        t TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\n" +
                        "        FOREIGN KEY(ss_id) REFERENCES ss(id))")
                        //"        FOREIGN KEY(w_id) REFERENCES w(id));\n")
                stat.execute("INSERT INTO s VALUES (1,1,1,0,\"foo\",date(\"now\"))")
                stat.execute("CREATE TABLE def\n" +
                        "       (id INTEGER PRIMARY KEY ASC,\n" +
                        "        ss_id INTEGER NOT NULL,\n" +
                        "        lang_id INTEGER NOT NULL,\n" +
                        "        def TEXT NOT NULL,\n" +
                        "        u INTEGER NOT NULL,\n" +
                        "        t TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\n" +
                        "        FOREIGN KEY(ss_id) REFERENCES ss(id),\n" +
                        "        FOREIGN KEY(lang_id) REFERENCES lang(id));\n")
                stat.execute("INSERT INTO def VALUES (1,1,1,\"definition\",\"foo\",date(\"now\"))")
                stat.execute("CREATE TABLE ssexe\n" +
                        "       (id INTEGER PRIMARY KEY ASC,\n" +
                        "        ss_id INTEGER NOT NULL,\n" +
                        "        lang_id INTEGER NOT NULL,\n" +
                        "        ssexe TEXT NOT NULL,\n" +
                        "        u INTEGER NOT NULL,\n" +
                        "        t TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\n" +
                        "        FOREIGN KEY(ss_id) REFERENCES ss(id),\n" +
                        "        FOREIGN KEY(lang_id) REFERENCES lang(id));\n")
                stat.execute("INSERT INTO ssexe VALUES (1,1,1,\"example\",\"foo\",date(\"now\"))")
                stat.execute("CREATE TABLE sslink\n" +
                        "       (id INTEGER PRIMARY KEY ASC,\n" +
                        "        ss1_id INTEGER NOT NULL,\n" +
                        "        ssrel_id INTEGER NOT NULL,\n" +
                        "        ss2_id INTEGER NOT NULL,\n" +
                        "        u INTEGER NOT NULL,\n" +
                        "        t TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\n" +
                        "        FOREIGN KEY(ss1_id) REFERENCES ss(id),\n" +
                        "        FOREIGN KEY(ssrel_id) REFERENCES ssrel(id),\n" +
                        "        FOREIGN KEY(ss2_id) REFERENCES ss(id));\n")
                stat.execute("INSERT INTO sslink VALUES (1,1,1,1,\"foo\",date(\"now\"))")
            }
        }
    }

    @org.junit.Test
    fun listSubjects() {
        val dataset = CILISQLiteDataset(file)
        val result = dataset.listSubjects().toList()
        val expResult = listOf(model.createResource(CILISQLiteDataset.ILI + "i1"))
        assertEquals(expResult, result)
        dataset.close()
    }


    @org.junit.Test
    fun listSubjectsWithProperty() {
        val dataset = CILISQLiteDataset(file)
        val result = dataset.listSubjectsWithProperty(model.createProperty(WN + "hypernym")).toList()
        val expResult = listOf(model.createResource(CILISQLiteDataset.ILI + "i1"))
        assertEquals(expResult, result)
        val result2 = dataset.listSubjectsWithProperty(model.createProperty(WN + "hyponym")).toList()
        val expResult2 = listOf<Resource>()
        assertEquals(expResult2, result2)
        dataset.close()
    }

    @org.junit.Test
    fun listSubjectsWithProperty1() {
        val dataset = CILISQLiteDataset(file)
        val result1 = dataset.listSubjectsWithProperty(model.createProperty(WN + "hypernym"),
            model.createResource(ILI + "i1")).toList()
        val expResult = listOf(model.createResource(CILISQLiteDataset.ILI + "i1"))
        assertEquals(expResult, result1)
        val result2 = dataset.listSubjectsWithProperty(model.createProperty(RDFS_LABEL),
                model.createLiteral("able","en")).toList()
        assertEquals(expResult, result2)
        val result3 = dataset.listSubjectsWithProperty(model.createProperty(WN_POS),
                model.createResource(WN + "noun")).toList()
        assertEquals(expResult, result3)
        val result4 = dataset.listSubjectsWithProperty(model.createProperty(WN_DEF),
                model.createLiteral("definition","en")).toList()
        assertEquals(expResult, result4)
        val result5 = dataset.listSubjectsWithProperty(model.createProperty(WN_EXAMPLE),
                model.createLiteral("example","en")).toList()
        assertEquals(expResult, result5)
        val result6 = dataset.listSubjectsWithProperty(model.createProperty(WN + "hypernym"),
                model.createResource(ILI + "i2")).toList()
        assertEquals(listOf<Resource>(), result6)
        dataset.close()
     }

    @org.junit.Test
    fun listObjectsOfProperty() {
        val dataset = CILISQLiteDataset(file)
        val subj = model.createResource(ILI + "i1")
        val result1 = dataset.listSubjectsWithProperty(model.createProperty(WN + "hypernym"), subj).toList()
        assertEquals(listOf(subj), result1)
        dataset.close()
    }

    @org.junit.Test
    fun listStatements() {
        val dataset = CILISQLiteDataset(file)
        val result = dataset.listStatements().toSet()
        val subj = model.createResource(ILI + "i1")
        val expResult = setOf(
            model.createStatement(subj, model.createProperty(RDFS_LABEL), model.createLiteral("able","en")),
            model.createStatement(subj, model.createProperty(WN_POS), model.createResource(WN + "noun")),
            model.createStatement(subj, model.createProperty(WN_DEF), model.createLiteral("definition","en")),
            model.createStatement(subj, model.createProperty(WN_EXAMPLE), model.createLiteral("example","en")),
            model.createStatement(subj, model.createProperty(WN + "hypernym"), subj))
        assertEquals(expResult, result)
        dataset.close()
    }
}