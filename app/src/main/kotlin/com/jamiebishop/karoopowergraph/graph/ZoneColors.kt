package com.jamiebishop.karoopowergraph.graph

object ZoneColors {
    val HR = intArrayOf(
        0xFF00A3FF.toInt(),
        0xFF00E676.toInt(),
        0xFFFFEA00.toInt(),
        0xFFFF8A00.toInt(),
        0xFFFF1744.toInt(),
    )

    val POWER = intArrayOf(
        0xFFB0BEC5.toInt(),
        0xFF00A3FF.toInt(),
        0xFF00E676.toInt(),
        0xFFFFEA00.toInt(),
        0xFFFF8A00.toInt(),
        0xFFFF1744.toInt(),
        0xFFD500F9.toInt(),
    )

    const val FALLBACK = 0xFFB0BEC5.toInt()

    fun hr(zone: Int): Int = pick(HR, zone)

    fun power(zone: Int): Int = pick(POWER, zone)

    private fun pick(palette: IntArray, zone: Int): Int {
        if (zone < 1) return FALLBACK
        val idx = (zone - 1).coerceAtMost(palette.size - 1)
        return palette[idx]
    }
}
