package org.insightcentre.uld.naisc.cili

import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.Statement
import org.insightcentre.uld.naisc.cili.CILISQLiteDataset.Companion.WN_DEF
import org.junit.Test

import org.junit.Assert.*
import java.io.File
import java.io.PrintWriter

class GWADatasetTest {
    val data = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<!DOCTYPE LexicalResource SYSTEM \"http://globalwordnet.github.io/schemas/WN-LMF-1.0.dtd\">\n" +
            "<LexicalResource xmlns:dc=\"http://purl.org/dc/elements/1.1/\">\n" +
            "    <!-- The first three lines must always be as above -->\n" +
            "\n" +
            "    <!-- A file may contain multiple WordNets in different languages:\n" +
            "         The following information is required:\n" +
            "            id: A short name for the resource\n" +
            "            label: The full name for the resources\n" +
            "            language: Please follow BCP-47, i.e., use a two-letter code if \n" +
            "                      available else a three-letter code\n" +
            "            email: Please give a contact email address\n" +
            "            license: The license of your resource (please provide URL)\n" +
            "            version: A string identifying this version (preferably follow \n" +
            "                     major.minor format)\n" +
            "            url: A URL for your project homepage\n" +
            "            citation: The paper to cite for this resource\n" +
            "         Extra properties may be included from Dublin core and in addition\n" +
            "            status: The status of the resource, e.g., \"valid\", \"checked\", \"unchecked\"\n" +
            "            confidenceScore: A numeric value between 0 and 1 giving the \n" +
            "                             confidence in the correctness of the element.                             \n" +
            "    -->\n" +
            "    <Lexicon id=\"example_en\"\n" +
            "             label=\"Example wordnet (English)\"\n" +
            "             language=\"en\" \n" +
            "             email=\"john@mccr.ae\"\n" +
            "             license=\"https://creativecommons.org/publicdomain/zero/1.0/\"\n" +
            "             version=\"1.0\"\n" +
            "             citation=\"CILI: the Collaborative Interlingual Index. Francis Bond, Piek Vossen, John P. McCrae and Christiane Fellbaum, Proceedings of the Global WordNet Conference 2016, (2016).\"\n" +
            "             url=\"http://globalwordnet.github.io/schemas/\"\n" +
            "             dc:publisher=\"Global Wordnet Association\">\n" +
            "        <!-- The list of lexical entries (words) in your wordnet -->\n" +
            "        <LexicalEntry id=\"w1\">\n" +
            "            <!-- The part of speech values are as follows:\n" +
            "                 n: Noun\n" +
            "                 v: Verb\n" +
            "                 a: Adjective\n" +
            "                 r: Adverb\n" +
            "                 s: Adjective Satellite\n" +
            "                 z: Multiword expression (inc. phrase, idiom)\n" +
            "                 c: Conjunction\n" +
            "                 p: Adposition (Preposition, postposition, etc.)\n" +
            "                 x: Other (inc. particle, classifier, bound morpheme, determiner)\n" +
            "                 u: Unknown -->\n" +
            "            <Lemma writtenForm=\"grandfather\" partOfSpeech=\"n\"/>\n" +
            "            <Sense id=\"example-en-10161911-n-1\" synset=\"example-en-10161911-n\">\n" +
            "            </Sense>\n" +
            "        </LexicalEntry>\n" +
            "        <LexicalEntry id=\"w2\">\n" +
            "            <Lemma writtenForm=\"paternal grandfather\" partOfSpeech=\"n\"/>\n" +
            "            <Sense id=\"example-en-1-n-1\" synset=\"example-en-1-n\">\n" +
            "                <!-- The set of relations between senses is limited to the following\n" +
            "                     antonym: An opposite and inherently incompatible word\n" +
            "                     also: See also, a reference of weak meaning\n" +
            "                     verb_group: Verb senses that similar in meaning and have been manually grouped together.\n" +
            "                     participle: An adjective that is a participle form a verb\n" +
            "                     pertainym: A relational adjective. Adjectives that are pertainyms are usually defined by such phrases as \"of or pertaining to\" and do not have antonyms. A pertainym can point to a noun or another pertainym\n" +
            "                     derivation: A word that is derived from some other word\n" +
            "                     domain_category: Indicates the category of this word\n" +
            "                     domain_member_category: Indicates a word involved in this category described by this word\n" +
            "                     domain_region: Indicates the region of this word\n" +
            "                     domain_member_region: Indicates a word involved in the region described by this word\n" +
            "                     exemplifies: Indicates the usage of this word\n" +
            "                     is_exemplified_by: Indicates a word involved in the usage described by this word\n" +
            "                -->\n" +
            "                <SenseRelation relType=\"derivation\" target=\"example-en-10161911-n-1\"/>\n" +
            "            </Sense>\n" +
            "        </LexicalEntry>\n" +
            "        <LexicalEntry id=\"w3\">\n" +
            "            <Lemma writtenForm=\"pay\" partOfSpeech=\"v\"/>\n" +
            "            <!-- Syntactic Behaviour is given as in Princeton WordNet -->\n" +
            "            <SyntacticBehaviour subcategorizationFrame=\"Sam cannot %s Sue \"/>\n" +
            "            <SyntacticBehaviour subcategorizationFrame=\"Sam and Sue %s\"/>\n" +
            "            <SyntacticBehaviour subcategorizationFrame=\"The banks %s the check\"/>\n" +
            "        </LexicalEntry>\n" +
            "        <!-- If a synset is already mapped to the ILI please give the ID here -->\n" +
            "        <Synset id=\"example-en-10161911-n\" ili=\"i90287\" partOfSpeech=\"n\">\n" +
            "            <Definition>the father of your father or mother</Definition>\n" +
            "            <!-- The set of relations between synsets is limited to the following:\n" +
            "                    hypernym: A concept with a broader meaning\n" +
            "                    hyponym: A concept with a narrower meaning\n" +
            "                    instance_hypernym: The class of objects to which this instance belongs\n" +
            "                    instance_hyponym: An individual instance of this class\n" +
            "                    part_holonym: A larger whole that this concept is part of\n" +
            "                    part_meronym: A part of this concept\n" +
            "                    member_holonym: A group that this concept is a member of\n" +
            "                    member_meronym: A member of this concept\n" +
            "                    substance_holonym: Something where a constituent material is this concept\n" +
            "                    substance_meronym: A constituent material of this concept\n" +
            "                    entail: A verb X entails Y if X cannot be done unless Y is, or has been, done.\n" +
            "                    cause: A verb that causes another\n" +
            "                    similar: Similar, though not necessarily interchangeable\n" +
            "                    also: See also, a reference of weak meaning\n" +
            "                    attribute: A noun for which adjectives express values. The noun weight is an attribute, for which the adjectives light and heavy express values.\n" +
            "                    verb_group: Verb senses that similar in meaning and have been manually grouped together.\n" +
            "                    domain_category: Indicates the category of this word\n" +
            "                    domain_member_category: Indicates a word involved in this category described by this word\n" +
            "                    domain_region: Indicates the region of this word\n" +
            "                    domain_member_region: Indicates a word involved in the region described by this word\n" +
            "                    exemplifies: Indicates the usage of this word\n" +
            "                    is_exemplified_by: Indicates a word involved in the usage described by this word\n" +
            "                -->\n" +
            "            <SynsetRelation relType=\"hypernym\" target=\"example-en-10162692-n\"/>\n" +
            "        </Synset>\n" +
            "        <!-- If you wish to define a new concept call the concept \"in\" (ILI New) -->\n" +
            "        <Synset id=\"example-en-1-n\" ili=\"in\" partOfSpeech=\"n\">\n" +
            "            <Definition>A father&apos;s father; a paternal grandfather</Definition>\n" +
            "            <!-- You can include metadata (such as source) at many points -->\n" +
            "            <!-- The ILI Definition must be at least 20 characters or five words -->\n" +
            "            <ILIDefinition dc:source=\"https://en.wiktionary.org/wiki/farfar\">A father&apos;s father; a paternal grandfather</ILIDefinition>\n" +
            "        </Synset>\n" +
            "        <!-- You must include all targets of relations -->\n" +
            "        <Synset id=\"example-en-10162692-n\" ili=\"i90292\" partOfSpeech=\"n\">\n" +
            "           <Example>This is an example</Example>\n" +
            "        </Synset>\n" +
            "    </Lexicon>\n" +
            "    <Lexicon id=\"example_sv\"\n" +
            "             label=\"Example wordnet (Swedish)\"\n" +
            "             language=\"sv\" \n" +
            "             email=\"john@mccr.ae\"\n" +
            "             license=\"https://creativecommons.org/publicdomain/zero/1.0/\"\n" +
            "             version=\"1.0\"\n" +
            "             citation=\"CILI: the Collaborative Interlingual Index. Francis Bond, Piek Vossen, John P. McCrae and Christiane Fellbaum, Proceedings of the Global WordNet Conference 2016, (2016).\"\n" +
            "             url=\"http://globalwordnet.github.io/schemas/\"\n" +
            "             dc:publisher=\"Global Wordnet Association\">\n" +
            "        <!-- The list of lexical entries (words) in your wordnet -->\n" +
            "        <LexicalEntry id=\"w4\">\n" +
            "            <Lemma writtenForm=\"farfar\" partOfSpeech=\"n\"/>\n" +
            "            <Form  writtenForm=\"farfäder\">\n" +
            "                <Tag category=\"penn\">NNS</Tag>\n" +
            "            </Form>\n" +
            "            <!-- Synsets need not be language-specific but senses must be -->\n" +
            "            <Sense id=\"example-sv-2-n-1\" synset=\"example-en-1-n\">\n" +
            "                <Example dc:source=\"Europarl Corpus\">Jag vill berätta för er att min farfar var svensk beredskapssoldat vid norska gränsen under andra världskriget, ett krig som Sverige stod utanför</Example>\n" +
            "            </Sense>\n" +
            "        </LexicalEntry>\n" +
            "    </Lexicon>\n" +
            "</LexicalResource>"
    val file = File.createTempFile("lmf",".xml")
    val prefix = "file:" + file.absolutePath + "#"
    init {
        file.deleteOnExit()
        val out = PrintWriter(file)
        out.use {
            out.println(data)
        }
    }

    @Test
    fun listSubjects() {
        val dataset = GWADataset(file)
        val model = ModelFactory.createDefaultModel()
        val expResult = setOf(
                model.createResource(prefix + "example-en-10161911-n"),
                model.createResource(prefix + "example-en-1-n"),
            model.createResource(prefix + "example-en-10162692-n"))
        val result = dataset.listSubjects().toSet()
        assertEquals(expResult, result)
    }

    @Test
    fun listSubjectsWithProperty() {
        val dataset = GWADataset(file)
        val model = ModelFactory.createDefaultModel()
        val expResult = setOf(
                model.createResource(prefix + "example-en-10161911-n"),
                model.createResource(prefix + "example-en-1-n"))
        val result = dataset.listSubjectsWithProperty(model.createProperty(WN_DEF)).toSet()
        assertEquals(expResult, result)

    }

    @Test
    fun listSubjectsWithProperty1() {
        val dataset = GWADataset(file)
        val model = ModelFactory.createDefaultModel()
        val expResult = setOf(
                model.createResource(prefix + "example-en-10161911-n"))
        val result = dataset.listSubjectsWithProperty(model.createProperty(WN_DEF),
            model.createLiteral("the father of your father or mother", "en")).toSet()
        assertEquals(expResult, result)
    }

    @Test
    fun listObjectsOfProperty() {
        val dataset = GWADataset(file)
        val model = ModelFactory.createDefaultModel()
        val expResult = setOf(
            model.createLiteral("the father of your father or mother", "en"))
        val result = dataset.listObjectsOfProperty(
            model.createResource(prefix + "example-en-10161911-n"),
            model.createProperty(WN_DEF)).toSet()
        assertEquals(expResult, result)
    }

    @Test
    fun listStatements() {
        val dataset = GWADataset(file)
        val model = ModelFactory.createDefaultModel()
        val expResult = buildSynsets().flatMap { ss -> ss.statements(model, prefix) }.toMutableSet()
        val expResult2 = mutableSetOf<Statement>() + expResult
        val result = dataset.listStatements().toSet()
        expResult.removeAll(result)
        println("Not generated")
        println(expResult.joinToString("\n"))
        result.removeAll(expResult2)
        println("Over generated")
        println(result.joinToString("\n"))
        assert(expResult.isEmpty())
        assert(result.isEmpty())
    }

    fun buildSynsets() : List<Synset> {
        return listOf(
            Synset("example-en-10161911-n", "en", listOf("grandfather"), "noun", listOf("the father of your father or mother"),
                listOf(), listOf(Pair("hypernym","example-en-10162692-n")), 90287),
            Synset("example-en-1-n", "en", listOf("paternal grandfather"), "noun", listOf("A father's father; a paternal grandfather"),
                listOf(), listOf(), 0),
                Synset("example-en-10162692-n", "en", listOf(), "noun", listOf(), listOf("This is an example"), listOf(), 90292))
    }


}