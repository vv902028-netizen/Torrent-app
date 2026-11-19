package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.ElevatedSlate
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.MonoMetricSmall
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary

@Composable
fun PieceHeatmapBar(
    segments: List<Float>,
    pieceCount: Int,
    piecesCompleted: Int,
    modifier: Modifier = Modifier
) {
    val displaySegments = if (segments.isEmpty()) List(32) { 0f } else segments

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
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "КАРТА БЛОКОВ (PIECES)",
                style = MonoMetricSmall,
                color = TextMuted
            )
            Text(
                text = if (pieceCount > 0) "$piecesCompleted / $pieceCount" else "Ожидание метаданных",
                style = MonoMetricSmall,
                color = TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
        ) {
            val count = displaySegments.size
            val gap = 2.dp.toPx()
            val segWidth = ((size.width - gap * (count - 1)) / count).coerceAtLeast(2f)
            val corner = CornerRadius(2.dp.toPx(), 2.dp.toPx())

            displaySegments.forEachIndexed { index, ratio ->
                val x = index * (segWidth + gap)
                val color: Color = when {
                    ratio >= 0.99f -> NeonEmerald
                    ratio > 0.05f -> NeonCyan.copy(alpha = 0.35f + 0.65f * ratio)
                    else -> ElevatedSlate
                }
                drawRoundRect(
                    color = color,
                    topLeft = Offset(x, 0f),
                    size = Size(segWidth, size.height),
                    cornerRadius = corner
                )
            }
        }
    }
}
