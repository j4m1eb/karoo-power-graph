package com.jamiebishop.karoopowergraph.graph

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface

object GraphRenderer {

    private const val LINE_WIDTH_PX = 2.5f
    private const val FILL_ALPHA = 0xFF
    private const val FILL_OVERLAP_PX = 1.0f
    private val HR_ICON = Color.WHITE
    private val POWER_ICON = Color.WHITE
    private val KAROO_TYPEFACE: Typeface = Typeface.MONOSPACE

    enum class Kind { HR, POWER }

    fun render(
        samples: List<Sample>,
        avg: Int,
        max: Int,
        widthPx: Int,
        heightPx: Int,
        timeWindowSec: Int?,
        nowMs: Long,
        kind: Kind,
        isDark: Boolean,
        windowLabel: String? = null,
        maxLabel: String = "MAX",
        reuse: Bitmap? = null,
    ): Bitmap {
        val textColor = if (isDark) Color.WHITE else Color.BLACK
        val w = widthPx.coerceAtLeast(1)
        val h = heightPx.coerceAtLeast(1)
        // Reuse a caller-supplied bitmap when its dimensions still match, instead of
        // allocating a fresh ARGB_8888 bitmap every frame (memory pressure, #5).
        val bmp = if (reuse != null && !reuse.isRecycled && reuse.width == w && reuse.height == h) {
            reuse.also { it.eraseColor(Color.TRANSPARENT) }
        } else {
            Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        }
        val canvas = Canvas(bmp)
        val fieldClip = Path().apply {
            val radius = minOf(w, h) * 0.08f
            addRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), radius, radius, Path.Direction.CW)
        }
        canvas.clipPath(fieldClip)

        val padPx = h * 0.05f
        val compactHeight = h < 260
        val compactPillCenterX = w * 0.385f
        val compactStat1X = w * 0.505f
        val compactStat2X = w * 0.760f
        val statTextSize = if (compactHeight) {
            maxOf(h * 0.170f, 28f)
        } else {
            h * 0.132f
        }
        val windowTextSize = if (compactHeight) {
            maxOf(h * 0.130f, 22f)
        } else {
            h * 0.082f
        }

        val statPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            typeface = KAROO_TYPEFACE
            textSize = statTextSize
            style = Paint.Style.FILL
        }
        val windowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = KAROO_TYPEFACE
            textSize = windowTextSize
            style = Paint.Style.FILL
        }
        val windowStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#6B7280")
            style = Paint.Style.STROKE
            strokeWidth = maxOf(1f, h * 0.008f)
        }

        val currentText = samples.lastOrNull()?.value?.toInt()?.toString().orEmpty()
        val avgText = "AVG $avg"
        val maxText = "$maxLabel $max"
        val statsInlineText = "$avgText  $maxText"

        if (compactHeight) {
            val statAllowed = minOf(
                compactStat2X - compactStat1X - padPx * 2f,
                w - padPx - compactStat2X,
            )
            val widestStat = maxOf(statPaint.measureText(avgText), statPaint.measureText(maxText))
            if (statAllowed > 0f && widestStat > statAllowed) {
                statPaint.textSize *= statAllowed / widestStat
            }
        }

        val avgBounds = Rect().also { statPaint.getTextBounds(avgText, 0, avgText.length, it) }
        val maxBounds = Rect().also { statPaint.getTextBounds(maxText, 0, maxText.length, it) }
        val statsInlineBounds = Rect().also {
            statPaint.getTextBounds(statsInlineText, 0, statsInlineText.length, it)
        }
        val avgWidth = statPaint.measureText(avgText)
        val maxWidth = statPaint.measureText(maxText)
        val statsInlineWidth = statPaint.measureText(statsInlineText)
        val rightColWidth = maxOf(avgWidth, maxWidth)
        val rightColLeft = if (compactHeight) compactStat1X else w - padPx - rightColWidth

        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            typeface = KAROO_TYPEFACE
            textSize = h * 0.335f
            style = Paint.Style.FILL
        }

        val minCenterGap = padPx * 1.2f
        val maxLeftColRight = if (compactHeight) {
            compactPillCenterX - padPx * 1.8f
        } else {
            rightColLeft - minCenterGap
        }

        var valueWidth = 0f
        var valueHeight = 0f
        var iconSize = 0f
        if (currentText.isNotEmpty()) {
            valueWidth = valuePaint.measureText(currentText)
            val vb = Rect().also { valuePaint.getTextBounds(currentText, 0, currentText.length, it) }
            valueHeight = vb.height().toFloat()
            iconSize = valueHeight * iconScale(kind)
            val leftUsed = padPx + iconSize + valueGap(kind, padPx) + valueWidth
            if (leftUsed > maxLeftColRight) {
                val scale = (maxLeftColRight - padPx) / (iconSize + valueGap(kind, padPx) + valueWidth)
                valuePaint.textSize *= scale
                valueWidth = valuePaint.measureText(currentText)
                valuePaint.getTextBounds(currentText, 0, currentText.length, vb)
                valueHeight = vb.height().toFloat()
                iconSize = valueHeight * iconScale(kind)
            }
        }

        val iconX = padPx * 0.65f
        val iconTopAlign = padPx + (valueHeight - iconSize) / 2f
        drawIcon(canvas, kind, iconX, iconTopAlign, iconSize)

        val valueTextX = iconX + iconSize + valueGap(kind, padPx)
        val valueBaseline = padPx + valueHeight
        if (currentText.isNotEmpty()) {
            canvas.drawText(currentText, valueTextX, valueBaseline, valuePaint)
        }

        val avgBaseline = padPx + avgBounds.height()
        val statLineStep = maxOf(avgBounds.height(), maxBounds.height()) * 1.2f
        val maxBaseline: Float
        val rightBottom: Float
        if (compactHeight) {
            canvas.drawText(avgText, compactStat1X, avgBaseline, statPaint)
            canvas.drawText(maxText, compactStat2X, avgBaseline, statPaint)
            maxBaseline = avgBaseline
            rightBottom = avgBaseline + statsInlineBounds.bottom.coerceAtLeast(0)
        } else {
            val avgX = w - padPx - avgWidth
            canvas.drawText(avgText, avgX, avgBaseline, statPaint)

            maxBaseline = avgBaseline + statLineStep
            val maxX = w - padPx - maxWidth
            canvas.drawText(maxText, maxX, maxBaseline, statPaint)
            rightBottom = maxBaseline + maxBounds.bottom.coerceAtLeast(0)
        }

        val compactWindowLabel = windowLabel?.compactWindowLabel()
        if (!compactWindowLabel.isNullOrEmpty()) {
            val leftColRight = valueTextX + valueWidth
            val centerLeft = if (compactHeight) {
                compactPillCenterX - w * 0.055f
            } else {
                leftColRight + padPx * 0.8f
            }
            val centerRight = if (compactHeight) {
                compactPillCenterX + w * 0.055f
            } else {
                rightColLeft - padPx * 0.8f
            }
            val available = centerRight - centerLeft
            if (available > 0f) {
                var labelWidth = windowPaint.measureText(compactWindowLabel)
                if (labelWidth > available) {
                    windowPaint.textSize *= available / labelWidth
                    labelWidth = windowPaint.measureText(compactWindowLabel)
                }
                if (windowPaint.textSize >= h * 0.06f) {
                    val windowBounds = Rect().also {
                        windowPaint.getTextBounds(compactWindowLabel, 0, compactWindowLabel.length, it)
                    }
                    val tagPadX = padPx * if (compactHeight) 0.85f else 0.65f
                    val tagPadY = padPx * if (compactHeight) 0.40f else 0.28f
                    val tagW = labelWidth + tagPadX * 2f
                    val tagH = windowBounds.height() + tagPadY * 2f
                    val tagLeft = centerLeft + (available - tagW) / 2f
                    val tagTop = padPx + h * 0.015f
                    val tagRect = RectF(tagLeft, tagTop, tagLeft + tagW, tagTop + tagH)
                    val radius = tagH * 0.35f
                    canvas.drawRoundRect(tagRect, radius, radius, windowStrokePaint)
                    val wx = tagRect.left + tagPadX
                    val wy = tagRect.top + tagPadY + windowBounds.height()
                    canvas.drawText(compactWindowLabel, wx, wy, windowPaint)
                }
            }
        }

        val leftBottom = padPx + valueHeight
        val curveTop = maxOf(leftBottom, rightBottom) + padPx * 0.5f
        val curveH = (h - curveTop).coerceAtLeast(1f)

        val frame = GraphGeometry.compute(
            samples = samples,
            widthPx = w,
            heightPx = curveH.toInt(),
            timeWindowSec = timeWindowSec,
            nowMs = nowMs,
            minYSpan = if (kind == Kind.POWER) 400f else 60f,
            floorAtZero = kind == Kind.POWER,
        )

        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = LINE_WIDTH_PX
            strokeCap = Paint.Cap.ROUND
        }
        val fillPaint = Paint().apply { style = Paint.Style.FILL }
        val path = Path()
        val totalH = h.toFloat()
        for (seg in frame.segments) {
            val baseColor = when (kind) {
                Kind.HR -> ZoneColors.hr(seg.zone)
                Kind.POWER -> ZoneColors.power(seg.zone)
            }
            fillPaint.color = (baseColor and 0x00FFFFFF) or (FILL_ALPHA shl 24)
            val y0 = seg.y0 + curveTop
            val y1 = seg.y1 + curveTop
            val x0 = (seg.x0 - FILL_OVERLAP_PX).coerceAtLeast(0f)
            val x1 = (seg.x1 + FILL_OVERLAP_PX).coerceAtMost(w.toFloat())
            path.reset()
            path.moveTo(x0, y0)
            path.lineTo(x1, y1)
            path.lineTo(x1, totalH)
            path.lineTo(x0, totalH)
            path.close()
            canvas.drawPath(path, fillPaint)

            linePaint.color = baseColor
            canvas.drawLine(seg.x0, y0, seg.x1, y1, linePaint)
        }

        return bmp
    }

    private fun drawIcon(canvas: Canvas, kind: Kind, x: Float, y: Float, s: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = if (kind == Kind.HR) HR_ICON else POWER_ICON
        }
        val p = Path()
        when (kind) {
            Kind.HR -> {
                p.moveTo(x + 0.50f * s, y + 0.30f * s)
                p.cubicTo(x + 0.50f * s, y + 0.00f * s, x + 0.00f * s, y + 0.00f * s, x + 0.00f * s, y + 0.30f * s)
                p.cubicTo(x + 0.00f * s, y + 0.55f * s, x + 0.25f * s, y + 0.78f * s, x + 0.50f * s, y + 1.00f * s)
                p.cubicTo(x + 0.75f * s, y + 0.78f * s, x + 1.00f * s, y + 0.55f * s, x + 1.00f * s, y + 0.30f * s)
                p.cubicTo(x + 1.00f * s, y + 0.00f * s, x + 0.50f * s, y + 0.00f * s, x + 0.50f * s, y + 0.30f * s)
                p.close()
            }
            Kind.POWER -> {
                p.moveTo(x + 0.60f * s, y + 0.00f * s)
                p.lineTo(x + 0.10f * s, y + 0.58f * s)
                p.lineTo(x + 0.42f * s, y + 0.58f * s)
                p.lineTo(x + 0.30f * s, y + 1.00f * s)
                p.lineTo(x + 0.90f * s, y + 0.40f * s)
                p.lineTo(x + 0.55f * s, y + 0.40f * s)
                p.lineTo(x + 0.78f * s, y + 0.00f * s)
                p.close()
            }
        }
        canvas.drawPath(p, paint)
    }

    private fun iconScale(kind: Kind): Float = 0.62f

    private fun valueGap(kind: Kind, padPx: Float): Float =
        padPx * 0.38f

    private fun String.compactWindowLabel(): String =
        when (this) {
            "1 min" -> "1m"
            "5 min" -> "5m"
            "20 min" -> "20m"
            else -> this
        }
}
