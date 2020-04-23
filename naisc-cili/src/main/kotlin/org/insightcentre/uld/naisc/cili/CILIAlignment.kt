package org.insightcentre.uld.naisc.cili

import it.unimi.dsi.fastutil.objects.Object2DoubleMap
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap
import org.insightcentre.uld.naisc.Alignment

data class CILIAlignment(val sourceId : String, val ili : Int, val score : Double, val relationship : String, val features : Object2DoubleMap<String>?)

fun naisc2cili(align : Alignment, prefix : String) = CILIAlignment(
        align.entity1?.uri?.substring(prefix.length) ?: throw RuntimeException("null entity"),
        align.entity2?.uri?.substring(CILISQLiteDataset.ILI.length + 1)?.toInt() ?: throw RuntimeException("null entity"),
        align.probability,
        align.property ?: throw RuntimeException("null property"),
        if(align.features is Object2DoubleMap<String>) {
            align.features as Object2DoubleMap<String>
        } else {
            Object2DoubleOpenHashMap<String>(align.features)
        })
