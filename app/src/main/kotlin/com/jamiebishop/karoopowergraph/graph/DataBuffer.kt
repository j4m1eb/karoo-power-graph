package com.jamiebishop.karoopowergraph.graph

class DataBuffer(private val capacitySeconds: Int = 43_200) {

    private val samples = ArrayDeque<Sample>()

    fun add(s: Sample) {
        samples.addLast(s)
        val cutoff = s.timestampMs - capacitySeconds * 1000L
        while (samples.isNotEmpty() && samples.first().timestampMs < cutoff) {
            samples.removeFirst()
        }
    }

    fun snapshot(): List<Sample> = samples.toList()

    fun size(): Int = samples.size

    fun clear() = samples.clear()
}
