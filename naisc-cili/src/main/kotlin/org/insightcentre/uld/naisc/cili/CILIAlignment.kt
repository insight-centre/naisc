package org.insightcentre.uld.naisc.cili

import it.unimi.dsi.fastutil.objects.Object2DoubleMap
import org.insightcentre.uld.naisc.Alignment

data class CILIAlignment(val sourceId : String, val ili : Int, val score : Double, val relationship : String, val features : Object2DoubleMap<String>?)

fun naisc2cili(align : Alignment, prefix : String) = CILIAlignment(
        align.entity1?.uri?.substring(prefix.length) ?: throw RuntimeException("null entity"),
        align.entity2?.uri?.substring(CILISQLiteDataset.ILI.length + 1)?.toInt() ?: throw RuntimeException("null entity"),
        align.score,
        align.relation ?: throw RuntimeException("null relation"),
        align.features)
