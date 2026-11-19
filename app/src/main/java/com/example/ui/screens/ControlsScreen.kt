package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.GlobalTelemetry
import com.example.domain.model.TimerAction
import com.example.domain.model.TimerState
import com.example.domain.model.TrafficSettings
import com.example.ui.components.OledCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.SpeedGraphCanvas
import com.example.ui.components.formatCountdown
import com.example.ui.components.formatLimitKbps
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.ElevatedSlate
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.MonoMetricLarge
import com.example.ui.theme.MonoMetricSmall
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCoral
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.OledBlack
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ControlsScreen(
    telemetry: GlobalTelemetry,
    settings: TrafficSettings,
    timerState: TimerState,
    downloadDirPath: String,
    onSetDownloadLimit: (Int) -> Unit,
    onSetUploadLimit: (Int) -> Unit,
    onStartTimer: (Int) -> Unit,
    onCancelTimer: () -> Unit,
    onSetTimerAction: (TimerAction) -> Unit,
    onSetStopOnComplete: (Boolean) -> Unit,
    onSetWifiOnly: (Boolean) -> Unit,
    onSetDhtEnabled: (Boolean) -> Unit,
    onSetPexEnabled: (Boolean) -> Unit,
    onSetLsdEnabled: (Boolean) -> Unit,
    onSetMaxConnections: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedMinutes by remember { mutableIntStateOf(30) }
    val speedPresets = listOf(0, 512, 2048, 5120, 10240)
    val timerPresets = listOf(15, 30, 60, 120)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(OledBlack),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column {
                    Text(
                        text = "Лимиты и таймер",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "Мониторинг канала, ограничение скорости и расписание",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            // Live Global Speed Graph
            item {
                SpeedGraphCanvas(
                    history = telemetry.speedHistory,
                    height = 160.dp,
                    title = "ОБЩАЯ СКОРОСТЬ СЕССИИ (60 СЕК)"
                )
            }

            // Download Timer Card
            item {
                OledCard(
                    borderColor = if (timerState.isActive) NeonAmber.copy(alpha = 0.5f) else HairlineBorder
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Timer,
                                contentDescription = null,
                                tint = NeonAmber
                            )
                            Text(
                                text = "Таймер загрузки",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                        }
                        if (timerState.isActive) {
                            Text(
                                text = formatCountdown(timerState.remainingSeconds),
                                style = MonoMetricLarge,
                                color = NeonAmber
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (timerState.isActive) {
                        Text(
                            text = if (timerState.action == TimerAction.PAUSE_ALL) {
                                "По истечении времени все загрузки будут поставлены на паузу."
                            } else {
                                "По истечении времени фоновый сервис будет остановлен."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = onCancelTimer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("cancel_timer_button"),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, NeonCoral.copy(alpha = 0.55f))
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Cancel,
                                contentDescription = null,
                                tint = NeonCoral,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Отменить таймер", color = NeonCoral)
                        }
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            timerPresets.forEach { mins ->
                                val isSelected = selectedMinutes == mins
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedMinutes = mins },
                                    modifier = Modifier.testTag("timer_preset_$mins"),
                                    label = {
                                        Text(
                                            text = if (mins >= 60) "${mins / 60} ч" else "$mins мин",
                                            style = MonoMetricSmall
                                        )
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = CarbonSurface,
                                        labelColor = TextSecondary,
                                        selectedContainerColor = NeonAmber.copy(alpha = 0.16f),
                                        selectedLabelColor = NeonAmber
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isSelected,
                                        borderColor = HairlineBorder,
                                        selectedBorderColor = NeonAmber.copy(alpha = 0.5f)
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Точное время: $selectedMinutes мин",
                            style = MonoMetricSmall,
                            color = TextSecondary
                        )
                        Slider(
                            value = selectedMinutes.toFloat(),
                            onValueChange = { selectedMinutes = it.roundToInt().coerceIn(5, 240) },
                            valueRange = 5f..240f,
                            colors = SliderDefaults.colors(
                                thumbColor = NeonAmber,
                                activeTrackColor = NeonAmber,
                                inactiveTrackColor = ElevatedSlate
                            ),
                            modifier = Modifier.testTag("timer_duration_slider")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TimerActionChip(
                                label = "Пауза всех",
                                selected = settings.timerAction == TimerAction.PAUSE_ALL,
                                onClick = { onSetTimerAction(TimerAction.PAUSE_ALL) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("timer_action_pause")
                            )
                            TimerActionChip(
                                label = "Стоп сервиса",
                                selected = settings.timerAction == TimerAction.STOP_SERVICE,
                                onClick = { onSetTimerAction(TimerAction.STOP_SERVICE) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("timer_action_stop")
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { onStartTimer(selectedMinutes) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("start_timer_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonAmber,
                                contentColor = OledBlack
                            )
                        ) {
                            Text("Запустить таймер ($selectedMinutes мин)")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    SettingToggleRow(
                        title = "Остановить по завершении всех",
                        subtitle = "Автоматически выключить движок, когда все файлы скачаны",
                        checked = settings.stopOnComplete,
                        onCheckedChange = onSetStopOnComplete,
                        testTag = "stop_on_complete_switch"
                    )
                }
            }

            // Speed Limits Card
            item {
                OledCard {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Speed,
                            contentDescription = null,
                            tint = NeonCyan
                        )
                        Text(
                            text = "Лимиты скорости",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    SectionHeader(
                        title = "Скачивание (Download)",
                        trailing = formatLimitKbps(settings.downloadLimitKbps)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        speedPresets.forEach { preset ->
                            val selected = settings.downloadLimitKbps == preset
                            FilterChip(
                                selected = selected,
                                onClick = { onSetDownloadLimit(preset) },
                                modifier = Modifier.testTag("dl_limit_preset_$preset"),
                                label = {
                                    Text(
                                        text = if (preset == 0) "∞" else formatLimitKbps(preset),
                                        style = MonoMetricSmall
                                    )
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = CarbonSurface,
                                    labelColor = TextSecondary,
                                    selectedContainerColor = NeonCyan.copy(alpha = 0.16f),
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
                    Slider(
                        value = settings.downloadLimitKbps.toFloat().coerceIn(0f, 20480f),
                        onValueChange = { raw ->
                            val snapped = (raw / 128f).roundToInt() * 128
                            onSetDownloadLimit(snapped)
                        },
                        valueRange = 0f..20480f,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonCyan,
                            activeTrackColor = NeonCyan,
                            inactiveTrackColor = ElevatedSlate
                        ),
                        modifier = Modifier.testTag("dl_limit_slider")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    SectionHeader(
                        title = "Отдача (Upload)",
                        trailing = formatLimitKbps(settings.uploadLimitKbps)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        speedPresets.forEach { preset ->
                            val selected = settings.uploadLimitKbps == preset
                            FilterChip(
                                selected = selected,
                                onClick = { onSetUploadLimit(preset) },
                                modifier = Modifier.testTag("ul_limit_preset_$preset"),
                                label = {
                                    Text(
                                        text = if (preset == 0) "∞" else formatLimitKbps(preset),
                                        style = MonoMetricSmall
                                    )
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = CarbonSurface,
                                    labelColor = TextSecondary,
                                    selectedContainerColor = NeonEmerald.copy(alpha = 0.16f),
                                    selectedLabelColor = NeonEmerald
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = selected,
                                    borderColor = HairlineBorder,
                                    selectedBorderColor = NeonEmerald.copy(alpha = 0.55f)
                                )
                            )
                        }
                    }
                    Slider(
                        value = settings.uploadLimitKbps.toFloat().coerceIn(0f, 20480f),
                        onValueChange = { raw ->
                            val snapped = (raw / 128f).roundToInt() * 128
                            onSetUploadLimit(snapped)
                        },
                        valueRange = 0f..20480f,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonEmerald,
                            activeTrackColor = NeonEmerald,
                            inactiveTrackColor = ElevatedSlate
                        ),
                        modifier = Modifier.testTag("ul_limit_slider")
                    )
                }
            }

            // Network & Protocol Card
            item {
                OledCard {
                    SectionHeader(
                        title = "Сеть и протокол",
                        trailing = "МАКС. ${settings.maxConnections} СОЕД."
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    SettingToggleRow(
                        title = "Только по Wi-Fi",
                        subtitle = "Ставить раздачи на паузу при переходе на мобильную сеть",
                        checked = settings.wifiOnly,
                        onCheckedChange = onSetWifiOnly,
                        testTag = "wifi_only_switch"
                    )
                    SettingToggleRow(
                        title = "DHT (Распределённая таблица)",
                        subtitle = "Поиск пиров без центральных трекеров",
                        checked = settings.dhtEnabled,
                        onCheckedChange = onSetDhtEnabled,
                        testTag = "dht_switch"
                    )
                    SettingToggleRow(
                        title = "PeX (Обмен пирами)",
                        subtitle = "Получение адресов новых пиров от уже подключённых",
                        checked = settings.pexEnabled,
                        onCheckedChange = onSetPexEnabled,
                        testTag = "pex_switch"
                    )
                    SettingToggleRow(
                        title = "LSD (Локальный поиск)",
                        subtitle = "Обнаружение пиров в локальной Wi-Fi сети",
                        checked = settings.lsdEnabled,
                        onCheckedChange = onSetLsdEnabled,
                        testTag = "lsd_switch"
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Максимум подключений: ${settings.maxConnections}",
                        style = MonoMetricSmall,
                        color = TextSecondary
                    )
                    Slider(
                        value = settings.maxConnections.toFloat(),
                        onValueChange = { onSetMaxConnections(it.roundToInt()) },
                        valueRange = 20f..300f,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonCyan,
                            activeTrackColor = NeonCyan,
                            inactiveTrackColor = ElevatedSlate
                        ),
                        modifier = Modifier.testTag("max_connections_slider")
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Folder,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Папка: $downloadDirPath",
                            style = MonoMetricSmall,
                            color = TextMuted,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimerActionChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 44.dp),
        shape = RoundedCornerShape(10.dp),
        color = if (selected) NeonAmber.copy(alpha = 0.16f) else CarbonSurface,
        border = BorderStroke(1.dp, if (selected) NeonAmber.copy(alpha = 0.6f) else HairlineBorder)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 10.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) NeonAmber else TextSecondary
            )
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag),
            colors = SwitchDefaults.colors(
                checkedThumbColor = OledBlack,
                checkedTrackColor = NeonCyan,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = ElevatedSlate,
                uncheckedBorderColor = HairlineBorder
            )
        )
    }
}
