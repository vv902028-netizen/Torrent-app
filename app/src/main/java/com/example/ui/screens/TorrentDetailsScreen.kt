package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.FilePriority
import com.example.domain.model.PeerStat
import com.example.domain.model.TorrentFileItem
import com.example.domain.model.TorrentState
import com.example.domain.model.TorrentUiItem
import com.example.domain.model.TrackerStat
import com.example.domain.model.TrackerStatus
import com.example.ui.components.NeonStatusBadge
import com.example.ui.components.OledCard
import com.example.ui.components.PieceHeatmapBar
import com.example.ui.components.SpeedGraphCanvas
import com.example.ui.components.TelemetryPill
import com.example.ui.components.accentColor
import com.example.ui.components.formatBytes
import com.example.ui.components.formatEta
import com.example.ui.components.formatPercent
import com.example.ui.components.formatSpeed
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.ElevatedSlate
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.MonoMetricMedium
import com.example.ui.theme.MonoMetricSmall
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCoral
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.OledBlack
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun TorrentDetailsScreen(
    torrent: TorrentUiItem?,
    onBack: () -> Unit,
    onTogglePauseResume: (TorrentUiItem) -> Unit,
    onDeleteTorrent: (String, Boolean) -> Unit,
    onSetFilePriority: (String, Int, FilePriority) -> Unit,
    onForceReannounce: (String) -> Unit,
    onAddTracker: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddTrackerDialog by remember { mutableStateOf(false) }
    var trackerUrlInput by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val tabs = listOf("Обзор", "Файлы", "Пиры", "Трекеры")
    val clipboard = LocalClipboardManager.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(OledBlack),
        contentAlignment = Alignment.TopCenter
    ) {
        if (torrent == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Торрент не найден или удалён", color = TextSecondary)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(onClick = onBack) {
                    Text("Назад", color = NeonCyan)
                }
            }
            return@Box
        }

        val isPaused = torrent.state == TorrentState.PAUSED ||
            torrent.state == TorrentState.ERROR ||
            torrent.state == TorrentState.QUEUED

        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("details_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Назад",
                        tint = TextPrimary
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = torrent.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NeonStatusBadge(state = torrent.state)
                        Text(
                            text = "${formatPercent(torrent.progress)} • ${formatBytes(torrent.downloadedBytes)} / ${formatBytes(torrent.totalBytes)}",
                            style = MonoMetricSmall,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(
                    onClick = { onTogglePauseResume(torrent) },
                    modifier = Modifier.testTag("details_pause_resume_button")
                ) {
                    Icon(
                        imageVector = if (isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                        contentDescription = if (isPaused) "Возобновить" else "Пауза",
                        tint = torrent.state.accentColor()
                    )
                }

                IconButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.testTag("details_delete_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = "Удалить",
                        tint = NeonCoral
                    )
                }
            }

            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = CarbonSurface,
                contentColor = NeonCyan,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        height = 2.dp,
                        color = NeonCyan
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    val badge = when (index) {
                        1 -> if (torrent.files.isNotEmpty()) " (${torrent.files.size})" else ""
                        2 -> " (${torrent.peers.size})"
                        3 -> " (${torrent.trackers.size})"
                        else -> ""
                    }
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("details_tab_$index"),
                        text = {
                            Text(
                                text = "$title$badge",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selectedTab == index) NeonCyan else TextSecondary,
                                maxLines = 1
                            )
                        }
                    )
                }
            }

            when (selectedTab) {
                0 -> OverviewTab(
                    torrent = torrent,
                    onCopyHash = {
                        clipboard.setText(AnnotatedString(torrent.infoHash))
                    },
                    modifier = Modifier.weight(1f)
                )
                1 -> FilesTab(
                    files = torrent.files,
                    onChangePriority = { idx, prio ->
                        onSetFilePriority(torrent.infoHash, idx, prio)
                    },
                    modifier = Modifier.weight(1f)
                )
                2 -> PeersTab(
                    peers = torrent.peers,
                    modifier = Modifier.weight(1f)
                )
                3 -> TrackersTab(
                    trackers = torrent.trackers,
                    onForceReannounce = { onForceReannounce(torrent.infoHash) },
                    onOpenAddTracker = { showAddTrackerDialog = true },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    if (showAddTrackerDialog && torrent != null) {
        AlertDialog(
            onDismissRequest = { showAddTrackerDialog = false },
            containerColor = ObsidianCard,
            titleContentColor = TextPrimary,
            title = { Text("Добавить трекер") },
            text = {
                OutlinedTextField(
                    value = trackerUrlInput,
                    onValueChange = { trackerUrlInput = it },
                    placeholder = { Text("udp://tracker.opentrackr.org:1337/announce", color = TextMuted) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_tracker_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = HairlineBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAddTracker(torrent.infoHash, trackerUrlInput)
                        trackerUrlInput = ""
                        showAddTrackerDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = OledBlack),
                    modifier = Modifier.testTag("confirm_add_tracker_button")
                ) {
                    Text("Добавить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTrackerDialog = false }) {
                    Text("Отмена", color = TextSecondary)
                }
            }
        )
    }

    if (showDeleteDialog && torrent != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = ObsidianCard,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = { Text("Удалить торрент?") },
            text = { Text("Выберите, оставить ли загруженные файлы на устройстве.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteTorrent(torrent.infoHash, true)
                        showDeleteDialog = false
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCoral, contentColor = TextPrimary)
                ) {
                    Text("С файлами")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        onDeleteTorrent(torrent.infoHash, false)
                        showDeleteDialog = false
                        onBack()
                    }
                ) {
                    Text("Только из списка", color = NeonCyan)
                }
            }
        )
    }
}

@Composable
private fun OverviewTab(
    torrent: TorrentUiItem,
    onCopyHash: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 14.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SpeedGraphCanvas(
                history = torrent.speedHistory,
                height = 150.dp,
                title = "СКОРОСТЬ ТОРРЕНТА (60 СЕК)"
            )
        }

        item {
            PieceHeatmapBar(
                segments = torrent.pieceSegments,
                pieceCount = torrent.pieceCount,
                piecesCompleted = torrent.piecesCompleted
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TelemetryPill(
                    label = "ПИРЫ (СИДЫ)",
                    value = "${torrent.connectedPeers} (${torrent.connectedSeeds})",
                    accent = NeonCyan,
                    modifier = Modifier.weight(1f)
                )
                TelemetryPill(
                    label = "ОТДАЧА / RATIO",
                    value = String.format(Locale.US, "%.2f", torrent.ratio),
                    accent = NeonEmerald,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TelemetryPill(
                    label = "ETA",
                    value = formatEta(torrent.etaSeconds),
                    accent = NeonAmber,
                    modifier = Modifier.weight(1f)
                )
                TelemetryPill(
                    label = "ОТДАНО",
                    value = formatBytes(torrent.uploadedBytes),
                    accent = NeonEmerald,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            OledCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "INFO HASH (SHA-1)",
                            style = MonoMetricSmall,
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = torrent.infoHash,
                            style = MonoMetricSmall,
                            color = NeonCyan
                        )
                    }
                    IconButton(
                        onClick = onCopyHash,
                        modifier = Modifier.testTag("copy_infohash_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = "Копировать InfoHash",
                            tint = TextSecondary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "СОХРАНЕНИЕ",
                    style = MonoMetricSmall,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = torrent.saveDir,
                    style = MonoMetricSmall,
                    color = TextSecondary
                )
                if (!torrent.errorMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Ошибка: ${torrent.errorMessage}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NeonCoral
                    )
                }
            }
        }
    }
}

@Composable
private fun FilesTab(
    files: List<TorrentFileItem>,
    onChangePriority: (Int, FilePriority) -> Unit,
    modifier: Modifier = Modifier
) {
    if (files.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Ожидание метаданных файлов от пиров...",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 14.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(files, key = { it.index }) { file ->
            OledCard {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${formatBytes(file.downloadedBytes)} / ${formatBytes(file.sizeBytes)} (${formatPercent(file.progress)})",
                    style = MonoMetricSmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(ElevatedSlate)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(file.progress)
                            .fillMaxHeight()
                            .background(if (file.priority == FilePriority.SKIP) TextMuted else NeonCyan)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PriorityChip(
                        label = "Пропустить",
                        selected = file.priority == FilePriority.SKIP,
                        accent = TextMuted,
                        onClick = { onChangePriority(file.index, FilePriority.SKIP) },
                        modifier = Modifier.weight(1f)
                    )
                    PriorityChip(
                        label = "Обычный",
                        selected = file.priority == FilePriority.NORMAL,
                        accent = NeonCyan,
                        onClick = { onChangePriority(file.index, FilePriority.NORMAL) },
                        modifier = Modifier.weight(1f)
                    )
                    PriorityChip(
                        label = "Высокий",
                        selected = file.priority == FilePriority.HIGH,
                        accent = NeonEmerald,
                        onClick = { onChangePriority(file.index, FilePriority.HIGH) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun PriorityChip(
    label: String,
    selected: Boolean,
    accent: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 40.dp),
        shape = RoundedCornerShape(8.dp),
        color = if (selected) accent.copy(alpha = 0.16f) else CarbonSurface,
        border = BorderStroke(1.dp, if (selected) accent.copy(alpha = 0.6f) else HairlineBorder)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 6.dp)) {
            Text(
                text = label,
                style = MonoMetricSmall,
                color = if (selected) accent else TextSecondary,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun PeersTab(
    peers: List<PeerStat>,
    modifier: Modifier = Modifier
) {
    if (peers.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Нет активных подключений к пирам",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 14.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(peers, key = { "${it.ip}:${it.port}" }) { peer ->
            OledCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${peer.ip}:${peer.port}",
                        style = MonoMetricMedium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = CarbonSurface,
                        border = BorderStroke(1.dp, HairlineBorder)
                    ) {
                        Text(
                            text = peer.flags,
                            style = MonoMetricSmall,
                            color = NeonCyan,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = peer.client,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = formatPercent(peer.progress),
                        style = MonoMetricSmall,
                        color = NeonEmerald
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "↓ ${formatSpeed(peer.downSpeedBps)}   ↑ ${formatSpeed(peer.upSpeedBps)}",
                    style = MonoMetricSmall,
                    color = if (peer.downSpeedBps > 0L) NeonCyan else TextMuted
                )
            }
        }
    }
}

@Composable
private fun TrackersTab(
    trackers: List<TrackerStat>,
    onForceReannounce: () -> Unit,
    onOpenAddTracker: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onForceReannounce,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("force_reannounce_button"),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
            ) {
                Icon(Icons.Outlined.Refresh, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Обновить", color = NeonCyan)
            }
            OutlinedButton(
                onClick = onOpenAddTracker,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("open_add_tracker_button"),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, HairlineBorder)
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null, tint = NeonEmerald, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Добавить", color = TextPrimary)
            }
        }

        if (trackers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Трекеры отсутствуют (используется DHT / PeX)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(top = 6.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(trackers, key = { "${it.tier}_${it.url}" }) { tracker ->
                    val statusColor = when (tracker.status) {
                        TrackerStatus.WORKING -> NeonEmerald
                        TrackerStatus.UPDATING -> NeonAmber
                        TrackerStatus.ERROR -> NeonCoral
                        TrackerStatus.DISABLED -> TextMuted
                    }
                    val statusText = when (tracker.status) {
                        TrackerStatus.WORKING -> "Работает"
                        TrackerStatus.UPDATING -> "Анонс..."
                        TrackerStatus.ERROR -> "Ошибка"
                        TrackerStatus.DISABLED -> "Отключён"
                    }

                    OledCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TIER ${tracker.tier}",
                                style = MonoMetricSmall,
                                color = TextMuted
                            )
                            Text(
                                text = statusText,
                                style = MonoMetricSmall,
                                color = statusColor
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = tracker.url,
                            style = MonoMetricSmall,
                            color = TextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Сиды: ${tracker.seeds} • Пиры: ${tracker.peers} • Ошибки: ${tracker.fails}",
                            style = MonoMetricSmall,
                            color = TextSecondary
                        )
                        if (tracker.message.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = tracker.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = NeonCoral,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
