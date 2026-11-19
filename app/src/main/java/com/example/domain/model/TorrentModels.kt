package com.example.domain.model

enum class TorrentState {
    DOWNLOADING,
    SEEDING,
    PAUSED,
    METADATA,
    CHECKING,
    QUEUED,
    COMPLETED,
    ERROR
}

enum class FilePriority {
    SKIP,
    NORMAL,
    HIGH
}

enum class TrackerStatus {
    WORKING,
    UPDATING,
    ERROR,
    DISABLED
}

enum class TimerAction {
    PAUSE_ALL,
    STOP_SERVICE
}

data class TorrentFileItem(
    val index: Int,
    val path: String,
    val name: String,
    val sizeBytes: Long,
    val downloadedBytes: Long,
    val priority: FilePriority
) {
    val progress: Float
        get() = if (sizeBytes > 0L) (downloadedBytes.toFloat() / sizeBytes.toFloat()).coerceIn(0f, 1f) else 0f
}

data class PeerStat(
    val ip: String,
    val port: Int,
    val client: String,
    val protocol: String,
    val downSpeedBps: Long,
    val upSpeedBps: Long,
    val progress: Float,
    val flags: String
)

data class TrackerStat(
    val url: String,
    val tier: Int,
    val status: TrackerStatus,
    val seeds: Int,
    val peers: Int,
    val fails: Int,
    val nextAnnounceSeconds: Int,
    val message: String
)

data class SpeedPoint(
    val timestampMs: Long,
    val downloadBps: Long,
    val uploadBps: Long
)

data class TorrentUiItem(
    val infoHash: String,
    val name: String,
    val sourceUri: String,
    val saveDir: String,
    val totalBytes: Long,
    val downloadedBytes: Long,
    val uploadedBytes: Long,
    val progress: Float,
    val downloadSpeedBps: Long,
    val uploadSpeedBps: Long,
    val etaSeconds: Long,
    val connectedPeers: Int,
    val totalPeers: Int,
    val connectedSeeds: Int,
    val totalSeeds: Int,
    val ratio: Float,
    val activeTimeSeconds: Long,
    val state: TorrentState,
    val addedAt: Long,
    val errorMessage: String? = null,
    val pieceCount: Int = 0,
    val piecesCompleted: Int = 0,
    val pieceSegments: List<Float> = emptyList(),
    val files: List<TorrentFileItem> = emptyList(),
    val peers: List<PeerStat> = emptyList(),
    val trackers: List<TrackerStat> = emptyList(),
    val speedHistory: List<SpeedPoint> = emptyList()
)

data class TrafficSettings(
    val downloadLimitKbps: Int = 0,
    val uploadLimitKbps: Int = 0,
    val wifiOnly: Boolean = false,
    val dhtEnabled: Boolean = true,
    val pexEnabled: Boolean = true,
    val lsdEnabled: Boolean = true,
    val maxConnections: Int = 120,
    val stopOnComplete: Boolean = false,
    val timerAction: TimerAction = TimerAction.PAUSE_ALL
)

data class TimerState(
    val isActive: Boolean = false,
    val totalSeconds: Int = 0,
    val remainingSeconds: Int = 0,
    val action: TimerAction = TimerAction.PAUSE_ALL,
    val stopOnComplete: Boolean = false
)

enum class NetworkHealth {
    ONLINE,
    RECOVERING,
    OFFLINE,
    TRACKER_STALLED
}

data class GlobalTelemetry(
    val totalDownloadBps: Long = 0L,
    val totalUploadBps: Long = 0L,
    val dhtNodes: Long = 0L,
    val activeCount: Int = 0,
    val isWifiPaused: Boolean = false,
    val networkHealth: NetworkHealth = NetworkHealth.ONLINE,
    val networkTransportLabel: String = "Сеть",
    val networkWarningMessage: String? = null,
    val speedHistory: List<SpeedPoint> = emptyList()
)

data class CatalogEntry(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: CatalogCategory,
    val sizeLabel: String,
    val sizeBytes: Long,
    val magnetUri: String,
    val infoHash: String,
    val license: String,
    val description: String,
    val trackerCount: Int
)

enum class CatalogCategory(val label: String) {
    ALL("Все"),
    OS_SOFTWARE("ОС и софт"),
    VIDEO_CC("Видео CC"),
    ARCHIVES("Архивы")
}

enum class DownloadFilter(val label: String) {
    ALL("Все"),
    ACTIVE("Активные"),
    COMPLETED("Завершённые"),
    PAUSED("Пауза")
}
