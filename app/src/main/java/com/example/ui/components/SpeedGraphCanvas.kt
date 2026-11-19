package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.domain.model.SpeedPoint
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.MonoMetricSmall
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary

@Composable
fun SpeedGraphCanvas(
    history: List<SpeedPoint>,
    modifier: Modifier = Modifier,
    height: Dp = 156.dp,
    title: String = "ГРАФИК СКОРОСТИ (60 СЕК)"
) {
    val maxSpeed = (history.maxOfOrNull { maxOf(it.downloadBps, it.uploadBps) } ?: 0L)
        .coerceAtLeast(64L * 1024L)
    val currentDown = history.lastOrNull()?.downloadBps ?: 0L
    val currentUp = history.lastOrNull()?.uploadBps ?: 0L
    val peakDown = history.maxOfOrNull { it.downloadBps } ?: 0L

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CarbonSurface)
            .border(1.dp, HairlineBorder, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MonoMetricSmall,
                color = TextMuted
            )
            Text(
                text = "ПИК ${formatSpeed(peakDown)}",
                style = MonoMetricSmall,
                color = TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val topPad = 8.dp.toPx()
                val bottomPad = 6.dp.toPx()
                val usableH = (h - topPad - bottomPad).coerceAtLeast(1f)

                val gridColor = Color(0xFF1A2230)
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 8f), 0f)
                for (i in 0..3) {
                    val y = topPad + (usableH * i / 3f)
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = if (i < 3) dashEffect else null
                    )
                }

                val samples = if (history.size >= 2) {
                    history
                } else {
                    List(2) { SpeedPoint(0L, currentDown, currentUp) }
                }

                val slotCount = 59
                val startSlot = (slotCount + 1 - samples.size).coerceAtLeast(0)

                fun buildLinePath(selector: (SpeedPoint) -> Long): Pair<Path, Path> {
                    val line = Path()
                    val fill = Path()
                    val pts = samples.mapIndexed { idx, pt ->
                        val x = ((startSlot + idx).toFloat() / slotCount.toFloat()) * w
                        val ratio = (selector(pt).toFloat() / maxSpeed.toFloat()).coerceIn(0f, 1f)
                        val y = topPad + usableH * (1f - ratio)
                        Offset(x, y)
                    }

                    if (pts.isNotEmpty()) {
                        line.moveTo(pts.first().x, pts.first().y)
                        fill.moveTo(pts.first().x, topPad + usableH)
                        fill.lineTo(pts.first().x, pts.first().y)

                        for (i in 1 until pts.size) {
                            val prev = pts[i - 1]
                            val curr = pts[i]
                            val midX = (prev.x + curr.x) / 2f
                            line.cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
                            fill.cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
                        }
                        fill.lineTo(pts.last().x, topPad + usableH)
                        fill.close()
                    }
                    return line to fill
                }

                val (upLine, upFill) = buildLinePath { it.uploadBps }
                drawPath(
                    path = upFill,
                    brush = Brush.verticalGradient(
                        colors = listOf(NeonEmerald.copy(alpha = 0.16f), Color.Transparent),
                        startY = topPad,
                        endY = topPad + usableH
                    )
                )
                drawPath(
                    path = upLine,
                    color = NeonEmerald.copy(alpha = 0.85f),
                    style = Stroke(
                        width = 1.8.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                val (downLine, downFill) = buildLinePath { it.downloadBps }
                drawPath(
                    path = downFill,
                    brush = Brush.verticalGradient(
                        colors = listOf(NeonCyan.copy(alpha = 0.28f), Color.Transparent),
                        startY = topPad,
                        endY = topPad + usableH
                    )
                )
                // Soft glow understroke
                drawPath(
                    path = downLine,
                    color = NeonCyan.copy(alpha = 0.25f),
                    style = Stroke(
                        width = 6.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
                drawPath(
                    path = downLine,
                    color = NeonCyan,
                    style = Stroke(
                        width = 2.4.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendChip(
                color = NeonCyan,
                label = "↓ ${formatSpeed(currentDown)}"
            )
            LegendChip(
                color = NeonEmerald,
                label = "↑ ${formatSpeed(currentUp)}"
            )
        }
    }
}

@Composable
private fun LegendChip(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MonoMetricSmall,
            color = TextSecondary
        )
    }
}
