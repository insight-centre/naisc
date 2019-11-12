package org.insightcentre.uld.naisc.cili

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import joptsimple.OptionParser
import org.insightcentre.uld.naisc.main.Configuration
import org.insightcentre.uld.naisc.main.ExecuteListeners
import org.insightcentre.uld.naisc.util.None
import java.io.File
import java.lang.Exception
import java.util.*
import org.insightcentre.uld.naisc.main.Main as NaiscMain

fun badOptions(p : OptionParser, msg : String) {
    System.err.println("Error: " + msg);
    p.printHelpOn(System.err);
    System.exit(-1);

}

fun randString(r : Random) : String {
    var x = ""
    for(i in 0..10) {
        x += (r.nextInt(26) + 97).toChar()
    }
    return x
}


fun randomAlignment() : CILIAlignment {
    val r = Random()
    return CILIAlignment(randString(r), r.nextInt(10000), r.nextDouble(), "http://www.w3.org/2004/02/skos/core#exactMatch", null)
}

fun main(args : Array<String>) {
    val p = OptionParser()
    val dbOpt = p.accepts("d", "The location of the CILI database").withRequiredArg().ofType(String::class.java)
    val configOpt = p.accepts("c", "The Naisc configuration to use").withRequiredArg().ofType(String::class.java)
    p.accepts("random", "Generate random testing results")
    val xmlOpt = p.nonOptions("The GWA-LMF file to align to")
    val os = try {
        p.parse(*args)
    } catch(x : Exception) {
        badOptions(p, x.message ?: "Unknown error")
        return
    }
   if(os.has("random")) {
        val alignment = mutableListOf<CILIAlignment>()
        for(i in 0..10) {
            alignment.add(randomAlignment());
        }
        val mapper = ObjectMapper()
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.writeValue(System.out, alignment);
        System.out.println()
    } else {
        if(os.nonOptionArguments().size != 1) {
            badOptions(p, "Wrong number of arguments. Please specify at least one argument")
            return
        }
        val dbFile = File(os.valueOf(dbOpt) ?: "omw/db/omw.db")
        val xmlFile = File(os.valueOf(xmlOpt) ?: "unreachable")
        if(!dbFile.exists()) {
            badOptions(p, dbFile.path + " does not exist")
            return
        }
        if(!xmlFile.exists()) {
            badOptions(p, xmlFile.path + " does not exist")
        }
        val xmlDataset = GWADataset(xmlFile)
        val ciliDataset = CILISQLiteDataset(dbFile)

        val listener = ExecuteListeners.STDERR

        val configFilename = os.valueOf(configOpt) ?: "configs/cili.json"
        val mapper = ObjectMapper()
        val config = mapper.readValue(File(configFilename),  Configuration::class.java)

        val alignment = NaiscMain.execute("cili", xmlDataset, ciliDataset, config, None(), listener, null, null, null)
        ciliDataset.close()
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.writeValue(System.out, alignment.map { a -> naisc2cili(a, xmlDataset.prefix) })
        System.out.println()

    }
}