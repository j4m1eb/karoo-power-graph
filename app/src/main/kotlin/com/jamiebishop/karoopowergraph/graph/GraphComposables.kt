package com.jamiebishop.karoopowergraph.graph

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap

@Composable
fun ZoneGraph(
    samples: List<Sample>,
    timeWindowSec: Int?,
    nowMs: Long,
    kind: GraphRenderer.Kind,
    modifier: Modifier = Modifier,
    windowLabel: String? = null,
    isDark: Boolean = isSystemInDarkTheme(),
) {
    BoxWithConstraints(modifier = modifier) {
        val stats = remember(samples) {
            if (samples.isEmpty()) {
                0 to 0
            } else {
                var sum = 0.0
                var max = Float.NEGATIVE_INFINITY
                for (s in samples) {
                    sum += s.value
                    if (s.value > max) max = s.value
                }
                (sum / samples.size).toInt() to max.toInt()
            }
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val bitmap = GraphRenderer.render(
                samples = samples,
                avg = stats.first,
                max = stats.second,
                widthPx = size.width.toInt(),
                heightPx = size.height.toInt(),
                timeWindowSec = timeWindowSec,
                nowMs = nowMs,
                kind = kind,
                isDark = isDark,
                windowLabel = windowLabel,
            )
            drawImage(bitmap.asImageBitmap())
        }
    }
}
