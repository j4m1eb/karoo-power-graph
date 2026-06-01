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
        minYSpan: Float = 1f,
        floorAtZero: Boolean = false,
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

        val smoothed = smoothSamplesWithinZones(visible, smoothingMsFor(timeWindowSec))
        val plotted = downsampleSamples(smoothed, maxPointsFor(timeWindowSec, widthPx))

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
        var yMin = if (floorAtZero) 0f else vMin - pad
        var yMax = vMax + pad
        if (yMax - yMin < minYSpan) {
            if (floorAtZero) {
                yMax = yMin + minYSpan
            } else {
                val extra = (minYSpan - (yMax - yMin)) / 2f
                yMin -= extra
                yMax += extra
            }
        }
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

    private fun smoothingMsFor(timeWindowSec: Int?): Long =
        when (timeWindowSec) {
            60 -> 3_000L
            300 -> 5_000L
            1200 -> 10_000L
            null -> 15_000L
            else -> 5_000L
        }

    private fun maxPointsFor(timeWindowSec: Int?, widthPx: Int): Int =
        when (timeWindowSec) {
            60 -> Int.MAX_VALUE
            300 -> (widthPx / 3).coerceIn(100, 220)
            1200 -> widthPx.coerceIn(240, 520)
            null -> widthPx.coerceIn(240, 520)
            else -> Int.MAX_VALUE
        }

    private fun smoothSamplesWithinZones(samples: List<Sample>, smoothingMs: Long): List<Sample> {
        if (samples.size < 3 || smoothingMs <= 1_000L) return samples

        val result = ArrayList<Sample>(samples.size)
        var runStart = 0
        while (runStart < samples.size) {
            val zone = samples[runStart].zone
            var runEnd = runStart + 1
            while (runEnd < samples.size && samples[runEnd].zone == zone) {
                runEnd += 1
            }
            smoothRun(samples, runStart, runEnd, smoothingMs, result)
            runStart = runEnd
        }
        return result
    }

    private fun smoothRun(
        samples: List<Sample>,
        runStart: Int,
        runEnd: Int,
        smoothingMs: Long,
        result: MutableList<Sample>,
    ) {
        var start = runStart
        var sum = 0.0
        for (end in runStart until runEnd) {
            val current = samples[end]
            sum += current.value
            val cutoff = current.timestampMs - smoothingMs
            while (start < end && samples[start].timestampMs < cutoff) {
                sum -= samples[start].value
                start += 1
            }
            val count = end - start + 1
            result.add(current.copy(value = (sum / count).toFloat()))
        }
    }

    private fun downsampleSamples(samples: List<Sample>, maxPoints: Int): List<Sample> {
        if (samples.size <= maxPoints) return samples
        val result = ArrayList<Sample>(maxPoints)
        val stride = ((samples.size - 1).toDouble() / (maxPoints - 1).toDouble()).coerceAtLeast(1.0)
        var next = 0.0
        while (next < samples.lastIndex) {
            result.add(samples[next.toInt()])
            next += stride
        }
        if (result.lastOrNull() != samples.last()) {
            result.add(samples.last())
        }
        return result
    }
}
