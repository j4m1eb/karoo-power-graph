package com.jamiebishop.karoopowergraph.graph

object GraphGeometry {

    data class Segment(
        val x0: Float,
        val y0: Float,
        val x1: Float,
        val y1: Float,
        val zone: Int,
    )

    data class Frame(
        val segments: List<Segment>,
        val widthPx: Int,
        val heightPx: Int,
        val yMin: Float,
        val yMax: Float,
    )

    fun compute(
        samples: List<Sample>,
        widthPx: Int,
        heightPx: Int,
        timeWindowSec: Int?,
        nowMs: Long,
    ): Frame {
        if (samples.isEmpty() || widthPx <= 0 || heightPx <= 0) {
            return Frame(emptyList(), widthPx, heightPx, 0f, 0f)
        }

        val windowStart: Long
        val windowMs: Long
        if (timeWindowSec == null) {
            windowStart = samples.first().timestampMs
            val span = samples.last().timestampMs - windowStart
            windowMs = if (span > 0) span else 1L
        } else {
            windowMs = timeWindowSec * 1000L
            windowStart = nowMs - windowMs
        }

        val anchorIdx = samples.indexOfLast { it.timestampMs < windowStart }
        val visible: List<Sample> = when {
            anchorIdx < 0 -> samples
            anchorIdx == samples.lastIndex -> listOf(samples[anchorIdx])
            else -> samples.subList(anchorIdx, samples.size)
        }

        val plotted = bucketSamples(visible, windowStart, bucketMsFor(timeWindowSec, windowMs))

        if (plotted.size < 2) {
            val v = plotted.firstOrNull()?.value ?: 0f
            return Frame(emptyList(), widthPx, heightPx, v, v)
        }

        var vMin = plotted.first().value
        var vMax = plotted.first().value
        for (s in plotted) {
            if (s.value < vMin) vMin = s.value
            if (s.value > vMax) vMax = s.value
        }
        val range = vMax - vMin
        val pad = if (range > 0f) range * 0.05f else maxOf(vMax * 0.05f, 1f)
        val yMin = vMin - pad
        val yMax = vMax + pad
        val ySpan = yMax - yMin

        val w = widthPx.toFloat()
        val h = heightPx.toFloat()

        fun xAt(tMs: Long): Float = (tMs - windowStart).toFloat() / windowMs.toFloat() * w
        fun yAt(v: Float): Float = h - ((v - yMin) / ySpan * h)

        val segments = ArrayList<Segment>(plotted.size - 1)
        for (i in 1 until plotted.size) {
            val a = plotted[i - 1]
            val b = plotted[i]
            segments.add(
                Segment(
                    x0 = xAt(a.timestampMs),
                    y0 = yAt(a.value),
                    x1 = xAt(b.timestampMs),
                    y1 = yAt(b.value),
                    zone = b.zone,
                ),
            )
        }
        return Frame(segments, widthPx, heightPx, yMin, yMax)
    }

    private fun bucketMsFor(timeWindowSec: Int?, windowMs: Long): Long =
        when (timeWindowSec) {
            60 -> 5_000L
            300 -> 5_000L
            1200 -> 10_000L
            null -> if (windowMs <= 5_000L) 1L else maxOf(5_000L, windowMs / 240L)
            else -> 5_000L
        }

    private fun bucketSamples(samples: List<Sample>, windowStart: Long, bucketMs: Long): List<Sample> {
        if (samples.size < 3 || bucketMs <= 1L) return samples

        val result = ArrayList<Sample>()
        val bucketable = if (samples.first().timestampMs < windowStart) {
            result.add(samples.first())
            samples.drop(1)
        } else {
            samples
        }

        val buckets = linkedMapOf<Long, MutableList<Sample>>()
        for (sample in bucketable) {
            val bucket = ((sample.timestampMs - windowStart).coerceAtLeast(0L)) / bucketMs
            buckets.getOrPut(bucket) { ArrayList() }.add(sample)
        }

        for (group in buckets.values) {
            if (group.size == 1) {
                result.add(group.first())
                continue
            }
            val timestamp = group.sumOf { it.timestampMs } / group.size
            val value = (group.sumOf { it.value.toDouble() } / group.size).toFloat()
            val zone = group
                .groupingBy { it.zone }
                .eachCount()
                .maxByOrNull { it.value }
                ?.key
                ?: group.last().zone
            result.add(Sample(timestamp, value, zone))
        }

        return result
    }
}
