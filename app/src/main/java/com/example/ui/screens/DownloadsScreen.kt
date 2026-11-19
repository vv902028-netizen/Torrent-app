package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.DownloadFilter
import com.example.domain.model.NetworkHealth
import com.example.domain.model.TorrentState
import com.example.domain.model.TorrentUiItem
import com.example.ui.components.NeonStatusBadge
import com.example.ui.components.OledCard
import com.example.ui.components.TelemetryPill
import com.example.ui.components.accentColor
import com.example.ui.components.formatBytes
import com.example.ui.components.formatCountdown
import com.example.ui.components.formatEta
import com.example.ui.components.formatLimitKbps
import com.example.ui.components.formatPercent
import com.example.ui.components.formatSpeed
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.ElevatedSlate
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.MonoMetricSmall
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCoral
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.OledBlack
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.DownloadsUiState

@Composable
fun DownloadsScreen(
    uiState: DownloadsUiState,
    onSelectFilter: (DownloadFilter) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onOpenTorrentDetails: (String) -> Unit,
    onTogglePauseResume: (TorrentUiItem) -> Unit,
    onDeleteTorrent: (String, Boolean) -> Unit,
    onPauseAll: () -> Unit,
    onResumeAll: () -> Unit,
    onOpenAddMagnet: () -> Unit,
    onPickTorrentFile: () -> Unit,
    onOpenCatalog: () -> Unit,
    onOpenControls: () -> Unit,
    onReconnectNetwork: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var pendingDelete by remember { mutableStateOf<TorrentUiItem?>(null) }
    var deleteWithFiles by remember { mutableStateOf(false) }
    var showSearchBar by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(OledBlack),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
                .padding(horizontal = 16.dp)
        ) {
            // Top Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "VoltTorrent",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "${uiState.globalTelemetry.networkTransportLabel} • DHT ${uiState.globalTelemetry.dhtNodes} • Без рекламы",
                        style = MonoMetricSmall,
                        color = TextMuted
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(
                        onClick = {
                            showSearchBar = !showSearchBar
                            if (!showSearchBar) onSearchQueryChange("")
                        },
                        modifier = Modifier.testTag("toggle_search_button")
                    ) {
                        Icon(
                            imageVector = if (showSearchBar) Icons.Outlined.Close else Icons.Outlined.Search,
                            contentDescription = "Поиск в списке",
                            tint = if (showSearchBar) NeonCyan else TextSecondary
                        )
                    }
                    if (uiState.torrents.isNotEmpty()) {
                        val hasActive = uiState.torrents.any {
                            it.state == TorrentState.DOWNLOADING || it.state == TorrentState.SEEDING || it.state == TorrentState.METADATA
                        }
                        IconButton(
                            onClick = { if (hasActive) onPauseAll() else onResumeAll() },
                            modifier = Modifier.testTag("pause_resume_all_button")
                        ) {
                            Icon(
                                imageVector = if (hasActive) Icons.Outlined.PauseCircle else Icons.Outlined.PlayCircle,
                                contentDescription = if (hasActive) "Пауза для всех" else "Возобновить все",
                                tint = if (hasActive) NeonAmber else NeonEmerald
                            )
                        }
                    }
                }
            }

            // Global Live Speed Telemetry Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TelemetryPill(
                    label = "СКАЧИВАНИЕ ↓",
                    value = formatSpeed(uiState.globalTelemetry.totalDownloadBps),
                    accent = NeonCyan,
                    modifier = Modifier.weight(1f)
                )
                TelemetryPill(
                    label = "ОТДАЧА ↑",
                    value = formatSpeed(uiState.globalTelemetry.totalUploadBps),
                    accent = NeonEmerald,
                    modifier = Modifier.weight(1f)
                )
            }

            // Active status pills (Wi-Fi wait, Speed Limit, Countdown Timer)
            val hasLimit = uiState.settings.downloadLimitKbps > 0 || uiState.settings.uploadLimitKbps > 0
            val showBannerRow = uiState.globalTelemetry.isWifiPaused || hasLimit || uiState.timerState.isActive
            if (showBannerRow) {
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (uiState.globalTelemetry.isWifiPaused) {
                        item {
                            StatusIndicatorChip(
                                icon = Icons.Outlined.WifiOff,
                                label = "Ожидание Wi-Fi",
                                accent = NeonAmber,
                                onClick = onOpenControls
                            )
                        }
                    }
                    if (uiState.timerState.isActive) {
                        item {
                            StatusIndicatorChip(
                                icon = Icons.Outlined.Timer,
                                label = "Таймер ${formatCountdown(uiState.timerState.remainingSeconds)}",
                                accent = NeonAmber,
                                onClick = onOpenControls
                            )
                        }
                    }
                    if (hasLimit) {
                        val dl = if (uiState.settings.downloadLimitKbps > 0) formatLimitKbps(uiState.settings.downloadLimitKbps) else "∞"
                        val ul = if (uiState.settings.uploadLimitKbps > 0) formatLimitKbps(uiState.settings.uploadLimitKbps) else "∞"
                        item {
                            StatusIndicatorChip(
                                icon = Icons.Outlined.Speed,
                                label = "Лимит ↓$dl / ↑$ul",
                                accent = NeonViolet,
                                onClick = onOpenControls
                            )
                        }
                    }
                }
            }

            val warningText = uiState.globalTelemetry.networkWarningMessage
            AnimatedVisibility(
                visible = uiState.globalTelemetry.networkHealth != NetworkHealth.ONLINE && !warningText.isNullOrBlank()
            ) {
                NetworkWarningBanner(
                    health = uiState.globalTelemetry.networkHealth,
                    message = warningText.orEmpty(),
                    onReconnect = onReconnectNetwork,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }

            // Optional Search Input
            AnimatedVisibility(visible = showSearchBar) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .testTag("downloads_search_input"),
                    placeholder = { Text("Поиск по названию или InfoHash", color = TextMuted) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = HairlineBorder,
                        focusedContainerColor = CarbonSurface,
                        unfocusedContainerColor = CarbonSurface,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(DownloadFilter.entries) { filter ->
                    val selected = uiState.filter == filter
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectFilter(filter) },
                        modifier = Modifier
                            .heightIn(min = 40.dp)
                            .testTag("filter_chip_${filter.name.lowercase()}"),
                        label = {
                            Text(
                                text = filter.label,
                                style = MaterialTheme.typography.labelLarge
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = CarbonSurface,
                            labelColor = TextSecondary,
                            selectedContainerColor = NeonCyan.copy(alpha = 0.14f),
                            selectedLabelColor = NeonCyan
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = HairlineBorder,
                            selectedBorderColor = NeonCyan.copy(alpha = 0.55f)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Torrent Queue or Empty State
            if (uiState.torrents.isEmpty()) {
                EmptyDownloadsState(
                    isFiltered = uiState.filter != DownloadFilter.ALL || uiState.searchQuery.isNotBlank(),
                    onResetFilter = {
                        onSelectFilter(DownloadFilter.ALL)
                        onSearchQueryChange("")
                    },
                    onAddMagnet = onOpenAddMagnet,
                    onPickTorrentFile = onPickTorrentFile,
                    onOpenCatalog = onOpenCatalog,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.torrents, key = { it.infoHash }) { item ->
                        TorrentQueueCard(
                            item = item,
                            onClick = { onOpenTorrentDetails(item.infoHash) },
                            onTogglePauseResume = { onTogglePauseResume(item) },
                            onRequestDelete = {
                                deleteWithFiles = false
                                pendingDelete = item
                            }
                        )
                    }
                }
            }
        }
    }

    pendingDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            containerColor = ObsidianCard,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = { Text("Удалить торрент?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { deleteWithFiles = !deleteWithFiles }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = deleteWithFiles,
                            onCheckedChange = { deleteWithFiles = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = NeonCoral,
                                checkmarkColor = TextPrimary,
                                uncheckedColor = TextMuted
                            )
                        )
                        Text(
                            text = "Также удалить скачанные файлы с диска",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteTorrent(item.infoHash, deleteWithFiles)
                        pendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonCoral,
                        contentColor = TextPrimary
                    ),
                    modifier = Modifier.testTag("confirm_delete_torrent_button")
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("Отмена", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun NetworkWarningBanner(
    health: NetworkHealth,
    message: String,
    onReconnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = when (health) {
        NetworkHealth.OFFLINE -> NeonCoral
        NetworkHealth.RECOVERING -> NeonCyan
        NetworkHealth.TRACKER_STALLED -> NeonAmber
        NetworkHealth.ONLINE -> NeonEmerald
    }
    val icon = when (health) {
        NetworkHealth.OFFLINE -> Icons.Outlined.WifiOff
        NetworkHealth.RECOVERING -> Icons.Outlined.Sync
        NetworkHealth.TRACKER_STALLED -> Icons.Outlined.WarningAmber
        NetworkHealth.ONLINE -> Icons.Outlined.Refresh
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("network_warning_banner"),
        shape = RoundedCornerShape(12.dp),
        color = accent.copy(alpha = 0.11f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            if (health == NetworkHealth.TRACKER_STALLED) {
                TextButton(
                    onClick = onReconnect,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("reconnect_network_button")
                ) {
                    Text(
                        text = "Обновить",
                        style = MaterialTheme.typography.labelLarge,
                        color = accent
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusIndicatorChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    accent: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = accent.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                style = MonoMetricSmall,
                color = accent
            )
        }
    }
}

@Composable
private fun TorrentQueueCard(
    item: TorrentUiItem,
    onClick: () -> Unit,
    onTogglePauseResume: () -> Unit,
    onRequestDelete: () -> Unit
) {
    val accent by animateColorAsState(targetValue = item.state.accentColor(), label = "card_accent")
    val isPausedOrStopped = item.state == TorrentState.PAUSED ||
        item.state == TorrentState.ERROR ||
        item.state == TorrentState.QUEUED

    OledCard(
        onClick = onClick,
        borderColor = if (item.state == TorrentState.DOWNLOADING) NeonCyan.copy(alpha = 0.35f) else HairlineBorder,
        modifier = Modifier.testTag("torrent_card_${item.infoHash}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NeonStatusBadge(state = item.state)
                    Text(
                        text = if (item.totalBytes > 0L) {
                            "${formatBytes(item.downloadedBytes)} / ${formatBytes(item.totalBytes)}"
                        } else {
                            "Получение размера..."
                        },
                        style = MonoMetricSmall,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onTogglePauseResume,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("toggle_torrent_${item.infoHash}")
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(accent.copy(alpha = 0.14f))
                            .border(1.dp, accent.copy(alpha = 0.45f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPausedOrStopped) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                            contentDescription = if (isPausedOrStopped) "Возобновить" else "Пауза",
                            tint = accent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                IconButton(
                    onClick = onRequestDelete,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("delete_torrent_${item.infoHash}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = "Удалить торрент",
                        tint = TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Crisp 4dp Neon Progress Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(ElevatedSlate)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(item.progress.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp))
                    .background(accent)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "↓ ${formatSpeed(item.downloadSpeedBps)}  ↑ ${formatSpeed(item.uploadSpeedBps)}",
                style = MonoMetricSmall,
                color = if (item.downloadSpeedBps > 0L) NeonCyan else TextSecondary
            )
            Text(
                text = "${formatPercent(item.progress)} • Пиры ${item.connectedPeers} • ETA ${formatEta(item.etaSeconds)}",
                style = MonoMetricSmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun EmptyDownloadsState(
    isFiltered: Boolean,
    onResetFilter: () -> Unit,
    onAddMagnet: () -> Unit,
    onPickTorrentFile: () -> Unit,
    onOpenCatalog: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(CarbonSurface)
                .border(1.dp, NeonCyan.copy(alpha = 0.35f), RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Download,
                contentDescription = null,
                tint = NeonCyan,
                modifier = Modifier.size(30.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (isFiltered) "Ничего не найдено" else "Очередь загрузок пуста",
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = if (isFiltered) {
                "Попробуйте сбросить фильтр или изменить поисковый запрос."
            } else {
                "Добавьте Magnet-ссылку, откройте локальный .torrent-файл или проверьте движок на открытых раздачах из каталога."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (isFiltered) {
            OutlinedButton(
                onClick = onResetFilter,
                modifier = Modifier.heightIn(min = 48.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
            ) {
                Text("Сбросить фильтры", color = NeonCyan)
            }
        } else {
            Column(
                modifier = Modifier.widthIn(max = 320.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onAddMagnet,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("empty_add_magnet_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonCyan,
                        contentColor = OledBlack
                    )
                ) {
                    Icon(Icons.Outlined.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Добавить Magnet-ссылку")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onPickTorrentFile,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("empty_pick_file_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, HairlineBorder)
                    ) {
                        Icon(
                            Icons.Outlined.Description,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = NeonEmerald
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(".torrent", color = TextPrimary, maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = onOpenCatalog,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("empty_open_catalog_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, HairlineBorder)
                    ) {
                        Icon(
                            Icons.Outlined.Explore,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = NeonCyan
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Каталог", color = TextPrimary, maxLines = 1)
                    }
                }
            }
        }
    }
}
