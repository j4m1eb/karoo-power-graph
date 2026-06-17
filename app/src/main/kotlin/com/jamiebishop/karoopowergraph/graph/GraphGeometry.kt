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
        val plotted = bucketSamplesForDrawing(smoothed, drawingBucketMsFor(timeWindowSec, windowMs))

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

    private fun drawingBucketMsFor(timeWindowSec: Int?, windowMs: Long): Long =
        when (timeWindowSec) {
            60 -> 1_000L
            300 -> 2_000L
            1200 -> 5_000L
            null -> maxOf(5_000L, windowMs / 480L)
            else -> 1_000L
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

    private fun bucketSamplesForDrawing(samples: List<Sample>, bucketMs: Long): List<Sample> {
        if (samples.size < 3 || bucketMs <= 1_000L) return samples

        val result = ArrayList<Sample>()
        var groupStart = 0
        var groupBucket = bucketKey(samples.first().timestampMs, bucketMs)
        var groupZone = samples.first().zone
        var timestampSum = 0L
        var valueSum = 0.0
        var count = 0

        fun flush() {
            if (count == 0) return
            if (count == 1) {
                result.add(samples[groupStart])
            } else {
                result.add(
                    Sample(
                        timestampMs = timestampSum / count,
                        value = (valueSum / count).toFloat(),
                        zone = groupZone,
                    ),
                )
            }
        }

        for (i in samples.indices) {
            val sample = samples[i]
            val bucket = bucketKey(sample.timestampMs, bucketMs)
            if (count > 0 && (bucket != groupBucket || sample.zone != groupZone)) {
                flush()
                groupStart = i
                groupBucket = bucket
                groupZone = sample.zone
                timestampSum = 0L
                valueSum = 0.0
                count = 0
            }
            timestampSum += sample.timestampMs
            valueSum += sample.value
            count += 1
        }
        flush()
        return result
    }

    private fun bucketKey(timestampMs: Long, bucketMs: Long): Long =
        Math.floorDiv(timestampMs, bucketMs)
}
