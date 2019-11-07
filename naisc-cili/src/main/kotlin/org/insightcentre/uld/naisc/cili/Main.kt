package org.insightcentre.uld.naisc.cili

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import joptsimple.OptionParser
import java.lang.Exception
import java.util.*

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
    return CILIAlignment(randString(r), r.nextInt(10000), r.nextDouble(), "http://www.w3.org/2004/02/skos/core#exactMatch")
}

fun main(args : Array<String>) {
    val p = OptionParser()
    p.accepts("d", "The location of the CILI database").withRequiredArg().ofType(String::class.java)
    p.nonOptions("The GWA-LMF file to align to")
    val os = try {
        p.parse(*args)
    } catch(x : Exception) {
        badOptions(p, x.message ?: "Unknown error")
        return
    }
    val alignment = mutableListOf<CILIAlignment>()
    for(i in 0..10) {
        alignment.add(randomAlignment());
    }
    val mapper = ObjectMapper()
    mapper.enable(SerializationFeature.INDENT_OUTPUT);
    mapper.writeValue(System.out, alignment);
    System.out.println()
}