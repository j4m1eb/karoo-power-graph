package com.sk0711.graph.graph

data class Sample(
    /** Moving ride time in ms — pause intervals are excluded, so the curve is continuous. */
    val timestampMs: Long,
    val value: Float,
    val zone: Int,
)
