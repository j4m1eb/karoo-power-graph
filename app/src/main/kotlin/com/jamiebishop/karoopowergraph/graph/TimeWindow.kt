package com.jamiebishop.karoopowergraph.graph

enum class TimeWindow(val seconds: Int?, val label: String) {
    ONE_MIN(60, "1 min"),
    FIVE_MIN(300, "5 min"),
    TWENTY_MIN(1200, "20 min"),
    FULL(null, "Full");

    fun next(): TimeWindow = when (this) {
        ONE_MIN -> FIVE_MIN
        FIVE_MIN -> TWENTY_MIN
        TWENTY_MIN -> FULL
        FULL -> ONE_MIN
    }
}
