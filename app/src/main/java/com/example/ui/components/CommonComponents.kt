package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.TorrentState
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.MonoMetricMedium
import com.example.ui.theme.MonoMetricSmall
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonAmberDim
import com.example.ui.theme.NeonCoral
import com.example.ui.theme.NeonCoralDim
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonCyanDim
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonEmeraldDim
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale
import kotlin.math.roundToInt

fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 Б"
    val kb = 1024.0
    val mb = kb * 1024.0
    val gb = mb * 1024.0
    return when {
        bytes >= gb -> String.format(Locale.US, "%.2f ГБ", bytes / gb)
        bytes >= mb -> String.format(Locale.US, "%.1f МБ", bytes / mb)
        bytes >= kb -> String.format(Locale.US, "%.0f КБ", bytes / kb)
        else -> "$bytes Б"
    }
}

fun formatSpeed(bytesPerSec: Long): String {
    if (bytesPerSec <= 0L) return "0 КБ/с"
    val kb = 1024.0
    val mb = kb * 1024.0
    return when {
        bytesPerSec >= mb -> String.format(Locale.US, "%.2f МБ/с", bytesPerSec / mb)
        bytesPerSec >= kb -> String.format(Locale.US, "%.0f КБ/с", bytesPerSec / kb)
        else -> "$bytesPerSec Б/с"
    }
}

fun formatLimitKbps(kbps: Int): String {
    if (kbps <= 0) return "Без лимита"
    return if (kbps >= 1024) {
        String.format(Locale.US, "%.1f МБ/с", kbps / 1024f)
    } else {
        "$kbps КБ/с"
    }
}

fun formatEta(seconds: Long): String {
    if (seconds <= 0L) return "—"
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return when {
        h > 99 -> ">99 ч"
        h > 0 -> "${h}ч ${m}м"
        m > 0 -> "${m}м ${s}с"
        else -> "${s}с"
    }
}

fun formatCountdown(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    val h = safe / 3600
    val m = (safe % 3600) / 60
    val s = safe % 60
    return if (h > 0) {
        String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
    } else {
        String.format(Locale.US, "%02d:%02d", m, s)
    }
}

fun formatPercent(progress: Float): String {
    val pct = (progress.coerceIn(0f, 1f) * 1000f).roundToInt() / 10f
    return if (pct >= 99.9f && progress >= 0.999f) "100%" else String.format(Locale.US, "%.1f%%", pct)
}

fun TorrentState.accentColor(): Color = when (this) {
    TorrentState.DOWNLOADING -> NeonCyan
    TorrentState.SEEDING, TorrentState.COMPLETED -> NeonEmerald
    TorrentState.METADATA, TorrentState.CHECKING -> NeonAmber
    TorrentState.PAUSED, TorrentState.QUEUED -> TextSecondary
    TorrentState.ERROR -> NeonCoral
}

fun TorrentState.containerColor(): Color = when (this) {
    TorrentState.DOWNLOADING -> NeonCyanDim.copy(alpha = 0.45f)
    TorrentState.SEEDING, TorrentState.COMPLETED -> NeonEmeraldDim.copy(alpha = 0.45f)
    TorrentState.METADATA, TorrentState.CHECKING -> NeonAmberDim.copy(alpha = 0.45f)
    TorrentState.PAUSED, TorrentState.QUEUED -> CarbonSurface
    TorrentState.ERROR -> NeonCoralDim.copy(alpha = 0.5f)
}

fun TorrentState.displayLabel(): String = when (this) {
    TorrentState.DOWNLOADING -> "Загрузка"
    TorrentState.SEEDING -> "Раздача"
    TorrentState.PAUSED -> "Пауза"
    TorrentState.METADATA -> "Поиск пиров"
    TorrentState.CHECKING -> "Проверка"
    TorrentState.QUEUED -> "Очередь"
    TorrentState.COMPLETED -> "Готово"
    TorrentState.ERROR -> "Ошибка"
}

@Composable
fun OledCard(
    modifier: Modifier = Modifier,
    borderColor: Color = HairlineBorder,
    containerColor: Color = ObsidianCard,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = containerColor,
            border = BorderStroke(1.dp, borderColor),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                content = content
            )
        }
    } else {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = containerColor,
            border = BorderStroke(1.dp, borderColor),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                content = content
            )
        }
    }
}

@Composable
fun NeonStatusBadge(
    state: TorrentState,
    modifier: Modifier = Modifier
) {
    val accent by animateColorAsState(
        targetValue = state.accentColor(),
        animationSpec = tween(220),
        label = "status_accent"
    )
    val bg by animateColorAsState(
        targetValue = state.containerColor(),
        animationSpec = tween(220),
        label = "status_bg"
    )

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = bg,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(accent)
            )
            Text(
                text = state.displayLabel(),
                style = MaterialTheme.typography.labelSmall,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun TelemetryPill(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = CarbonSurface,
        border = BorderStroke(1.dp, HairlineBorder)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MonoMetricSmall,
                color = TextMuted,
                maxLines = 1
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(accent)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = value,
                    style = MonoMetricMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    trailing: String? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary
        )
        if (!trailing.isNullOrBlank()) {
            Text(
                text = trailing,
                style = MonoMetricSmall,
                color = NeonViolet
            )
        }
    }
}
