<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xs="http://www.w3.org/2001/XMLSchema"
                exclude-result-prefixes="xs" version="1.0" xmlns="http://lari-datasets.ilc.cnr.it/nenu_sample#"
                xml:base="http://lari-datasets.ilc.cnr.it/nenu_sample" xmlns:void="http://rdfs.org/ns/void#"
                xmlns:owl="http://www.w3.org/2002/07/owl#" xmlns:ns="http://creativecommons.org/ns#"
                xmlns:lime="http://www.w3.org/ns/lemon/lime" xmlns:xsd="http://www.w3.org/2001/XMLSchema#"
                xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#" xmlns:lexinfo="http://www.lexinfo.net/ontology/3.0/lexinfo#"
                xmlns:lexicog="http://www.w3.org/ns/lemon/lexicog#" xmlns:dct="http://purl.org/dc/terms/"
                xmlns:bibo="http://purl.org/ontology/bibo/" xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                xmlns:terms="http://purl.org/dc/terms/" xmlns:xml="http://www.w3.org/XML/1998/namespace"
                xmlns:ontolex="http://www.w3.org/ns/lemon/ontolex#" xmlns:vann="http://purl.org/vocab/vann/"
                xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:lime1="http://www.w3.org/ns/lemon/lime#"
                xmlns:tei="http://www.tei-c.org/ns/1.0" xmlns:skos="http://www.w3.org/2004/02/skos/core#">

    <xsl:variable name="LexiconURI" select="'http://www.mylexica.perso/PLI1906'"/>

    <xsl:output indent="yes" method="xml"/>

    <xsl:template match="/">
        <rdf:RDF>
            <xsl:apply-templates select="descendant::tei:entry"/>
        </rdf:RDF>
    </xsl:template>

    <xsl:template match="tei:entry">
        <xsl:choose>
            <xsl:when test="tei:entry | tei:re">
                <lexicog:Entry>
                    <lexicog:describes>
                        <ontolex:LexicalEntry rdf:ID="{@xml:id}">
                            <xsl:apply-templates select="*[not(self::tei:entry or self::tei:re)]"/>
                        </ontolex:LexicalEntry>
                    </lexicog:describes>
                    <xsl:apply-templates select="tei:entry | tei:re"/>
                </lexicog:Entry>
            </xsl:when>
            <xsl:when test="tei:gramGrp/tei:pos/@expand = 'locution'">
                <owl:NamedIndividual rdf:about="{$LexiconURI}#{@xml:id}">
                    <rdf:type rdf:resource="http://www.w3.org/ns/lemon/ontolex#MultiwordExpression"/>
                    <xsl:apply-templates/>
                </owl:NamedIndividual>
            </xsl:when>
            <xsl:otherwise>
                <ontolex:LexicalEntry rdf:ID="{@xml:id}">
                    <xsl:apply-templates/>
                </ontolex:LexicalEntry>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <xsl:template
            match="tei:entry/tei:form[@type = 'lemma'] | tei:entry/tei:form[not(@type)] | tei:entry/tei:re/tei:form | tei:entry/tei:entry/tei:form">
        <ontolex:canonicalForm>
            <rdf:Description>
                <xsl:apply-templates/>
            </rdf:Description>
        </ontolex:canonicalForm>
    </xsl:template>

    <xsl:template match="tei:entry/tei:form[@type = 'inflected'] | tei:form/tei:form[@type = 'inflected']">
        <ontolex:otherForm>
            <rdf:Description>
                <xsl:apply-templates/>
            </rdf:Description>
        </ontolex:otherForm>
    </xsl:template>


    <xsl:template match="tei:orth">
        <xsl:variable name="workingLanguage" select="ancestor-or-self::*[@xml:lang][1]/@xml:lang"/>
        <ontolex:writtenRep xml:lang="{$workingLanguage}">
            <xsl:apply-templates/>
        </ontolex:writtenRep>
    </xsl:template>

    <xsl:template match="tei:orth/tei:seg | tei:orth/tei:pc">
        <xsl:apply-templates/>
    </xsl:template>

    <xsl:template match="tei:pron">
        <xsl:variable name="workingLanguage" select="ancestor-or-self::*[@xml:lang][1]/@xml:lang"/>
        <xsl:variable name="languageWithNotation">
            <xsl:choose>
                <xsl:when test="@notation">
                    <xsl:value-of select="concat($workingLanguage, '-fon', @notation)"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="$workingLanguage"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <ontolex:phoneticRep xml:lang="{$languageWithNotation}">
            <xsl:apply-templates/>
        </ontolex:phoneticRep>
    </xsl:template>

    <xsl:template match="tei:pron/tei:seg | tei:pron/tei:pc">
        <xsl:apply-templates/>
    </xsl:template>

    <xsl:template
            match="tei:form/text() | tei:orth/text()[normalize-space() = ''] | tei:pron/text()[normalize-space() = '']"/>

    <xsl:template match="tei:form[@type = 'lemma']/tei:form[@type = 'variant']">
        <xsl:apply-templates/>
    </xsl:template>

    <xsl:template match="tei:gramGrp">
        <xsl:apply-templates/>
    </xsl:template>

    <!-- e.g. "et" in French in PLI ("adj. et n.")-->
    <xsl:template match="tei:gramGrp/tei:lbl | tei:gramGrp/text()"/>

    <!-- Note the hack concerning 'proper' due to a bad example that remains to be corrected -->
    <xsl:template match="tei:pos | tei:gram[@type = 'pos'] | tei:gram[@type = 'proper']">
        <xsl:if test="not(@expan = 'locution')">
            <xsl:variable name="sourceReference">
                <xsl:choose>
                    <xsl:when test="@norm">
                        <xsl:value-of select="@norm"/>
                    </xsl:when>
                    <xsl:when test="@expand">
                        <xsl:value-of select="@expand"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="."/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:variable>
            <xsl:variable name="lexinfoCategory">
                <xsl:choose>
                    <xsl:when test="$sourceReference = 'nom' or $sourceReference = 'noun' or $sourceReference = 'NOUN'"
                    >commonNoun</xsl:when>
                    <xsl:when
                            test="$sourceReference = 'adjectif' or $sourceReference = 'adjective' or $sourceReference = 'ADJ'"
                    >adjective</xsl:when>
                    <xsl:when
                            test="$sourceReference = 'verbe' or $sourceReference = 'verb' or $sourceReference = 'VERB'"
                    >verb</xsl:when>
                    <xsl:when
                            test="$sourceReference = 'adverbe' or $sourceReference = 'adverb' or $sourceReference = 'ADV'"
                    >adverb</xsl:when>
                    <xsl:when
                            test="$sourceReference = 'pronom' or $sourceReference = 'pronoun' or $sourceReference = 'PRON'"
                    >pronoun</xsl:when>
                    <xsl:when
                            test="$sourceReference = 'article' or $sourceReference = 'determiner' or $sourceReference = 'DET'"
                    >determiner</xsl:when>
                    <xsl:when test="$sourceReference = 'interjection' or $sourceReference = 'INTJ'"
                    >interjection</xsl:when>
                    <xsl:when
                            test="$sourceReference = 'nombre' or $sourceReference = 'number' or $sourceReference = 'NUM'"
                    >numeral</xsl:when>
                    <xsl:when
                            test="$sourceReference = 'particule' or $sourceReference = 'particle' or $sourceReference = 'PART'"
                    >particle</xsl:when>

                    <xsl:when test="$sourceReference = 'préfixe' or $sourceReference = 'prefix'">prefix</xsl:when>
                    <xsl:when
                            test="$sourceReference = 'conjonction de coordination' or $sourceReference = 'coordinating conjunction' or $sourceReference = 'CCONJ'"
                    >coordinatingConjunction</xsl:when>
                    <xsl:when
                            test="$sourceReference = 'auxiliaire' or $sourceReference = 'auxiliary' or $sourceReference = 'AUX'"
                    >auxiliary</xsl:when>
                    <xsl:when
                            test="$sourceReference = 'préposition' or $sourceReference = 'preposition' or $sourceReference = 'ADP'"
                    >adposition</xsl:when>
                    <xsl:when test="$sourceReference = 'PROPN'">properNoun</xsl:when>
                    <xsl:when test="$sourceReference = 'PUNCT'">punctuation</xsl:when>
                    <xsl:when test="$sourceReference = 'SCONJ'">subordinatingConjunction</xsl:when>
                    <xsl:when test="$sourceReference = 'SYM'">symbol</xsl:when>
                    <xsl:when test="$sourceReference = 'X'">unknown</xsl:when>
                    <xsl:otherwise>
                        <xsl:message>CategoryRemainsToBeDetermined: <xsl:value-of select="$sourceReference"
                        /></xsl:message>
                        <xsl:text>unknown</xsl:text>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:variable>
            <xsl:if test="not($lexinfoCategory = 'unknown')">
                <lexinfo:partOfSpeech rdf:resource="http://www.lexinfo.net/ontology/3.0/lexinfo#{$lexinfoCategory}"/>
            </xsl:if>
        </xsl:if>
    </xsl:template>

    <xsl:template match="tei:gen | tei:gram[@type = 'gen']">
        <xsl:variable name="sourceReference">
            <xsl:choose>
                <xsl:when test="@norm">
                    <xsl:value-of select="@norm"/>
                </xsl:when>
                <xsl:when test="@expand">
                    <xsl:value-of select="@expand"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="."/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:variable name="lexinfoGender">
            <xsl:choose>
                <xsl:when test="$sourceReference = 'masculin' or $sourceReference = 'masculine'">masculine</xsl:when>
                <xsl:when test="$sourceReference = 'féminin' or $sourceReference = 'feminine'">feminine</xsl:when>
                <xsl:when test="$sourceReference = 'neutre' or $sourceReference = 'neuter'">neuter</xsl:when>
                <xsl:otherwise>GenderValueRemainsToBeDetermined for: <xsl:value-of select="$sourceReference"
                /></xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <lexinfo:gender rdf:resource="http://www.lexinfo.net/ontology/3.0/lexinfo#{$lexinfoGender}"/>
    </xsl:template>


    <xsl:template match="tei:number | tei:gram[@type = 'num'] | tei:gram[@type = 'number']">
        <xsl:variable name="sourceReference">
            <xsl:choose>
                <xsl:when test="@norm">
                    <xsl:value-of select="@norm"/>
                </xsl:when>
                <xsl:when test="@expand">
                    <xsl:value-of select="@expand"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="."/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:variable name="lexinfoNumber">
            <xsl:choose>
                <xsl:when test="$sourceReference = 'singulier' or $sourceReference = 'singular'">singular</xsl:when>
                <xsl:when test="$sourceReference = 'pluriel' or $sourceReference = 'plural'">plural</xsl:when>
                <xsl:when test="$sourceReference = 'dual'">dual</xsl:when>
                <xsl:otherwise>GenderValueRemainsToBeDetermined for: <xsl:value-of select="$sourceReference"
                /></xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <lexinfo:number rdf:resource="http://www.lexinfo.net/ontology/3.0/lexinfo#{$lexinfoNumber}"/>
    </xsl:template>

    <xsl:template match="tei:tns | tei:gram[@type = 'tns'] | tei:gram[@type = 'tense']">
        <xsl:variable name="sourceReference">
            <xsl:choose>
                <xsl:when test="@norm">
                    <xsl:value-of select="@norm"/>
                </xsl:when>
                <xsl:when test="@expand">
                    <xsl:value-of select="@expand"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="."/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:variable name="lexinfoTense">
            <xsl:choose>
                <xsl:when test="$sourceReference = 'présent' or $sourceReference = 'present'">present</xsl:when>
                <xsl:when test="$sourceReference = 'futur' or $sourceReference = 'future'">future</xsl:when>
                <xsl:when test="$sourceReference = 'passé' or $sourceReference = 'past'">past</xsl:when>
                <xsl:when test="$sourceReference = 'prétérite' or $sourceReference = 'preterite'">preterite</xsl:when>
                <xsl:otherwise>TenseValueRemainsToBeDetermined for: <xsl:value-of select="$sourceReference"
                /></xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <lexinfo:tense rdf:resource="http://www.lexinfo.net/ontology/3.0/lexinfo#{$lexinfoTense}"/>
    </xsl:template>

    <xsl:template match="tei:gram[@type = 'animate'] | tei:gram[@type = 'animacy']">
        <xsl:variable name="sourceReference">
            <xsl:choose>
                <xsl:when test="@norm">
                    <xsl:value-of select="@norm"/>
                </xsl:when>
                <xsl:when test="@expand">
                    <xsl:value-of select="@expand"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="."/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <xsl:variable name="lexinfoAnimacy">
            <xsl:choose>
                <xsl:when test="$sourceReference = 'animé' or $sourceReference = 'animate'">animate</xsl:when>
                <xsl:when test="$sourceReference = 'inanimé' or $sourceReference = 'inanimate'">inanimate</xsl:when>
                <xsl:otherwise>AnimacyValueRemainsToBeDetermined for: <xsl:value-of select="$sourceReference"
                /></xsl:otherwise>
            </xsl:choose>
        </xsl:variable>
        <lexinfo:animacy rdf:resource="http://www.lexinfo.net/ontology/3.0/lexinfo#{$lexinfoAnimacy}"/>
    </xsl:template>

    <xsl:template match="tei:subc"/>


    <!-- Punctuations are not kept in Ontolex -->

    <xsl:template match="tei:pc"/>

    <!-- Sense related transformation in two ways: a) reference within an entry and b) creation of the actual LexicalSense node -->

    <xsl:template match="tei:sense">
        <ontolex:sense>
            <rdf:Description>
                <xsl:apply-templates/>
            </rdf:Description>
        </ontolex:sense>
    </xsl:template>

    <xsl:template match="tei:sense/text()"/>

    <!-- Dealing with the general <usg> values and mapping them to possible lexinfo SenseContext information types -->

    <!-- Note (LR): the  official value for this category in TEI Lex 0 is frequency (opening source values to deal with legacy data) -->
    <xsl:template match="tei:usg[@type = 'plev' or @type = 'frequency']">
        <lexinfo:frequency>
            <rdf:Description>
                <rdf:value>
                    <xsl:apply-templates/>
                </rdf:value>
            </rdf:Description>
        </lexinfo:frequency>
    </xsl:template>

    <!-- Note (LR): the  official value for this category in TEI Lex 0 is socioCultural (opening source values to deal with legacy data) -->
    <xsl:template match="tei:usg[@type = 'register' or @type = 'reg' or @type = 'socioCultural']">
        <lexinfo:socioCultural>
            <rdf:Description>
                <rdf:value>
                    <xsl:apply-templates/>
                </rdf:value>
            </rdf:Description>
        </lexinfo:socioCultural>
    </xsl:template>

    <!-- Note (LR): the  official value for this category in TEI Lex 0 is temporal (opening source values to deal with legacy data) -->
    <xsl:template match="tei:usg[@type = 'time' or @type = 'temporal']">
        <lexinfo:temporalQualifier>
            <rdf:Description>
                <rdf:value>
                    <xsl:apply-templates/>
                </rdf:value>
            </rdf:Description>
        </lexinfo:temporalQualifier>
    </xsl:template>

    <!-- Note (LR): the  official value for this category in TEI Lex 0 is geographic (opening source values to deal with legacy data) -->
    <xsl:template match="tei:usg[@type = 'geo' or @type = 'geographic']">
        <lexinfo:geographic>
            <rdf:Description>
                <rdf:value>
                    <xsl:apply-templates/>
                </rdf:value>
            </rdf:Description>
        </lexinfo:geographic>
    </xsl:template>

    <!-- Note (LR): the  official value for this category in TEI Lex 0 is domain (opening source values to deal with legacy data) -->
    <xsl:template match="tei:usg[@type = 'dom' or @type = 'domain']">
        <lexinfo:domain>
            <rdf:Description>
                <rdf:value>
                    <xsl:apply-templates/>
                </rdf:value>
            </rdf:Description>
        </lexinfo:domain>
    </xsl:template>

    <xsl:template match="tei:usg[@type = 'attitude']">
        <lexinfo:attitude>
            <rdf:Description>
                <rdf:value>
                    <xsl:apply-templates/>
                </rdf:value>
            </rdf:Description>
        </lexinfo:attitude>
    </xsl:template>

    <xsl:template match="tei:usg[@type = 'normativity']">
        <lexinfo:normativity>
            <rdf:Description>
                <rdf:value>
                    <xsl:apply-templates/>
                </rdf:value>
            </rdf:Description>
        </lexinfo:normativity>
    </xsl:template>

    <!-- Note (LR): the  official value for this category in TEI Lex 0 is meaningType (opening source values to deal with legacy data) -->
    <xsl:template match="tei:usg[@type = 'style' or @type = 'meaningType']">
        <lexinfo:meaningType>
            <rdf:Description>
                <rdf:value>
                    <xsl:apply-templates/>
                </rdf:value>
            </rdf:Description>
        </lexinfo:meaningType>
    </xsl:template>

    <xsl:template match="tei:usg[@type = 'hint']">
        <lexinfo:hint>
            <rdf:Description>
                <rdf:value>
                    <xsl:apply-templates/>
                </rdf:value>
            </rdf:Description>
        </lexinfo:hint>
    </xsl:template>

    <xsl:template match="tei:usg[@type = 'textType']">
        <lexinfo:textType>
            <rdf:Description>
                <rdf:value>
                    <xsl:apply-templates/>
                </rdf:value>
            </rdf:Description>
        </lexinfo:textType>
    </xsl:template>

    <xsl:template match="tei:def">
        <xsl:variable name="workingLanguage" select="ancestor-or-self::*[@xml:lang][1]/@xml:lang"/>
        <skos:definition xml:lang="{$workingLanguage}">
            <xsl:apply-templates/>
        </skos:definition>
    </xsl:template>

    <xsl:template match="tei:cit[@type = 'example' or @type = 'quote']">
        <lexicog:usageExample>
            <rdf:Description>
                <xsl:apply-templates/>
            </rdf:Description>
        </lexicog:usageExample>
    </xsl:template>

    <xsl:template match="tei:cit[@type = 'example' or @type = 'quote']/tei:quote">
        <xsl:variable name="workingLanguage" select="ancestor-or-self::*[@xml:lang][1]/@xml:lang"/>
        <rdf:value xml:lang="{$workingLanguage}">
            <xsl:apply-templates/>
        </rdf:value>
    </xsl:template>

    <!-- Very basic flattening of translations et large -->
    <xsl:template match="tei:cit[@type = 'trans' or @type = 'translationEquivalent']">
        <xsl:apply-templates select="tei:quote"/>
    </xsl:template>

    <xsl:template match="tei:cit[@type = 'trans' or @type = 'translationEquivalent']/tei:quote">
        <xsl:variable name="workingLanguage" select="ancestor-or-self::*[@xml:lang][1]/@xml:lang"/>
        <lexinfo:senseTranslation xml:lang="{$workingLanguage}">
            <xsl:apply-templates/>
        </lexinfo:senseTranslation>
    </xsl:template>

    <xsl:template match="tei:quote/tei:mentioned | tei:def/tei:mentioned | tei:note/tei:mentioned">
        <xsl:apply-templates/>
    </xsl:template>

    <xsl:template match="tei:etym">
        <xsl:variable name="flattenEtym">
            <xsl:apply-templates/>
        </xsl:variable>
        <xsl:variable name="workingLanguage" select="ancestor-or-self::*[@xml:lang][1]/@xml:lang"/>
        <xsl:if test="normalize-space($flattenEtym)">
            <lexinfo:etymology>
                <rdf:Description>
                    <rdf:value xml:lang="{$workingLanguage}">
                        <xsl:value-of select="normalize-space($flattenEtym)"/>
                    </rdf:value>
                </rdf:Description>
            </lexinfo:etymology>
        </xsl:if>
    </xsl:template>

    <!-- Highest priority here to make sure that <etym> is always flattened, cf. e.g. overriding the precise transformation of <usg> -->
    <xsl:template match="tei:etym/*" priority="5">
        <xsl:apply-templates/>
    </xsl:template>

    <!-- The following template ensures that while flattening, the various strings remains white-space separated. -->
    <xsl:template match="tei:etym/text()[normalize-space() = '']">
        <xsl:text> </xsl:text>
    </xsl:template>

    <xsl:template match="tei:cit[@type = 'etymon']/tei:lang">
        <xsl:apply-templates/>
    </xsl:template>

    <xsl:template match="tei:cit[@type = 'etymon']/text()[normalize-space() = '']">
        <xsl:text> </xsl:text>
    </xsl:template>


    <xsl:template match="tei:ref[@type = 'bibl']">
        <xsl:text>[</xsl:text>
        <xsl:apply-templates/>
        <xsl:text>]</xsl:text>
    </xsl:template>

    <xsl:template match="tei:bibl">
        <dct:source>
            <xsl:apply-templates/>
        </dct:source>
    </xsl:template>

    <xsl:template match="tei:bibl/*">
        <xsl:apply-templates/>
    </xsl:template>

    <xsl:template match="tei:bibl/text()[normalize-space() = '']">
        <xsl:text> </xsl:text>
    </xsl:template>

    <xsl:template match="tei:author">
        <dc:creator>
            <xsl:apply-templates/>
        </dc:creator>
    </xsl:template>

    <xsl:template match="tei:title">
        <dc:title>
            <xsl:apply-templates/>
        </dc:title>
    </xsl:template>

    <xsl:template match="tei:date">
        <dc:date>
            <xsl:apply-templates/>
        </dc:date>
    </xsl:template>

    <xsl:template match="tei:publisher">
        <dc:publisher>
            <xsl:apply-templates/>
        </dc:publisher>
    </xsl:template>

    <!-- <xr> construct -->

    <xsl:template match="tei:xr[@type = 'related' or @type = 'renvoi']">
        <lexinfo:relatedTerm>
            <rdf:Description rdf:about="{tei:ref/@target}">
                <xsl:apply-templates/>
            </rdf:Description>
        </lexinfo:relatedTerm>
    </xsl:template>

    <xsl:template match="tei:xr[@type = 'synonymy' or @type = 'synonym']">
        <lexinfo:synonym>
            <rdf:Description rdf:about="{tei:ref/@target}">
                <xsl:apply-templates/>
            </rdf:Description>
        </lexinfo:synonym>
    </xsl:template>

    <xsl:template match="tei:xr[@type = 'hyponymy']">
        <lexinfo:hyponym>
            <rdf:Description rdf:about="{tei:ref/@target}">
                <xsl:apply-templates/>
            </rdf:Description>
        </lexinfo:hyponym>
    </xsl:template>

    <xsl:template match="tei:xr[@type = 'hypernymy']">
        <lexinfo:hypernym>
            <rdf:Description rdf:about="{tei:ref/@target}">
                <xsl:apply-templates/>
            </rdf:Description>
        </lexinfo:hypernym>
    </xsl:template>

    <xsl:template match="tei:xr[@type = 'meronymy']">
        <lexinfo:meronymTerm>
            <rdf:Description rdf:about="{tei:ref/@target}">
                <xsl:apply-templates/>
            </rdf:Description>
        </lexinfo:meronymTerm>
    </xsl:template>

    <xsl:template match="tei:xr[@type = 'antonymy' or @type = 'antonym']">
        <lexinfo:antonym>
            <rdf:Description rdf:about="{tei:ref/@target}">
                <xsl:apply-templates/>
            </rdf:Description>
        </lexinfo:antonym>
    </xsl:template>

    <xsl:template match="tei:xr/tei:ref">
        <xsl:if test="normalize-space()">
            <xsl:variable name="workingLanguage" select="ancestor-or-self::*[@xml:lang][1]/@xml:lang"/>
            <rdf:value xml:lang="{$workingLanguage}">
                <xsl:value-of select="."/>
            </rdf:value>
        </xsl:if>
    </xsl:template>

    <xsl:template match="tei:xr/text()"/>

    <xsl:template match="tei:note">
        <rdfs:comment>
            <xsl:apply-templates/>
        </rdfs:comment>
    </xsl:template>


    <!-- And we drop <lbl> in <xr> -->
    <xsl:template match="tei:xr/tei:lbl"/>

    <xsl:template match="tei:entry/tei:re | tei:entry/tei:entry | tei:re/tei:re">
        <lexicog:subComponent>
            <lexicog:Entry>
                <lexicog:describes>
                    <ontolex:LexicalEntry>
                        <xsl:apply-templates/>
                    </ontolex:LexicalEntry>
                </lexicog:describes>
                <xsl:if test="@type = 'prov' or @type='proverb'">
                    <lexinfo:termType rdf:resource="http://www.lexinfo.net/ontology/3.0/lexinfo#proverb"/>
                </xsl:if>
                <xsl:if test="@type = 'expr' or @type='expression'">
                    <lexinfo:termType rdf:resource="http://www.lexinfo.net/ontology/3.0/lexinfo#phraseologicalUnit"/>
                </xsl:if>
                <xsl:if test="@type = 'loc' or @type='locution'">
                    <lexinfo:termType rdf:resource="http://www.lexinfo.net/ontology/3.0/lexinfo#setPhrase"/>
                </xsl:if>
            </lexicog:Entry>
        </lexicog:subComponent>
    </xsl:template>

    <!-- Small annotation elements or intermediate text that disappear in Ontolex -->

    <xsl:template match="tei:emph | tei:hi">
        <xsl:apply-templates/>
    </xsl:template>

    <xsl:template match="tei:pb"/>

    <!-- We flatten all <choice> constructs to reflect the original source -->
    <xsl:template match="tei:choice[tei:abbr]" priority="6">
        <xsl:value-of select="tei:abbr"/>
    </xsl:template>

    <xsl:template match="tei:choice[tei:sic]" priority="6">
        <xsl:value-of select="tei:sic"/>
    </xsl:template>

    <xsl:template match="tei:choice[tei:orig]" priority="6">
        <xsl:value-of select="tei:orig"/>
    </xsl:template>


    <!-- Copy all template to account for possible missed elements -->
    <xsl:template match="@* | node()">
        <xsl:choose>
            <xsl:when test="name()">
                <xsl:message>
                    <xsl:value-of select="name()"/> - <xsl:value-of select="."/>
                </xsl:message>
            </xsl:when>
            <!--  <xsl:when test="attribute()">
                <xsl:message>
                    <xsl:value-of select="concat('@', name())"/>
                </xsl:message>
            </xsl:when>-->
        </xsl:choose>

        <xsl:copy>
            <xsl:apply-templates select="@* | node()"/>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>