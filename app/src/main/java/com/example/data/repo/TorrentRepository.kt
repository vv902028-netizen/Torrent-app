package com.example.data.repo

import android.net.Uri
import com.example.data.catalog.DemoCatalog
import com.example.data.prefs.TrafficPreferencesStore
import com.example.domain.model.CatalogEntry
import com.example.domain.model.FilePriority
import com.example.domain.model.GlobalTelemetry
import com.example.domain.model.TimerAction
import com.example.domain.model.TimerState
import com.example.domain.model.TorrentUiItem
import com.example.domain.model.TrafficSettings
import com.example.torrent.EngineEvent
import com.example.torrent.TorrentEngineManager
import com.example.torrent.TorrentFilePreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map

class TorrentRepository(
    private val engineManager: TorrentEngineManager,
    private val preferencesStore: TrafficPreferencesStore
) {
    val torrents: StateFlow<List<TorrentUiItem>> = engineManager.torrents
    val globalTelemetry: StateFlow<GlobalTelemetry> = engineManager.globalTelemetry
    val timerState: StateFlow<TimerState> = engineManager.timerState
    val settings: Flow<TrafficSettings> = preferencesStore.settingsFlow
    val engineEvents: SharedFlow<EngineEvent> = engineManager.events

    val catalogEntries: List<CatalogEntry> = DemoCatalog.entries

    val downloadDirectoryPath: String
        get() = engineManager.defaultDownloadDir.absolutePath

    fun observeTorrent(infoHash: String): Flow<TorrentUiItem?> =
        torrents.map { list -> list.firstOrNull { it.infoHash.equals(infoHash, ignoreCase = true) } }

    suspend fun addMagnet(magnetUri: String, customName: String? = null): Result<String> =
        engineManager.addMagnet(magnetUri, customName)

    suspend fun previewTorrentFile(uri: Uri): Result<TorrentFilePreview> =
        engineManager.previewTorrentFile(uri)

    suspend fun addTorrentFileBytes(rawBytes: ByteArray, selectedIndices: Set<Int>?): Result<String> =
        engineManager.addTorrentFileBytes(rawBytes, selectedIndices)

    suspend fun pauseTorrent(infoHash: String) = engineManager.pauseTorrent(infoHash)

    suspend fun resumeTorrent(infoHash: String) = engineManager.resumeTorrent(infoHash)

    suspend fun pauseAll() = engineManager.pauseAll()

    suspend fun resumeAll() = engineManager.resumeAll()

    suspend fun removeTorrent(infoHash: String, deleteFiles: Boolean) =
        engineManager.removeTorrent(infoHash, deleteFiles)

    suspend fun setFilePriority(infoHash: String, fileIndex: Int, priority: FilePriority) =
        engineManager.setFilePriority(infoHash, fileIndex, priority)

    suspend fun forceReannounce(infoHash: String) = engineManager.forceReannounce(infoHash)

    suspend fun reconnectNetworkAndTrackers() = engineManager.recoverNetworkAndTrackers(emitToast = true)

    suspend fun addTracker(infoHash: String, url: String): Boolean =
        engineManager.addTracker(infoHash, url)

    fun startTimer(minutes: Int, action: TimerAction) =
        engineManager.startTimer(minutes, action)

    fun cancelTimer() = engineManager.cancelTimer()

    suspend fun setDownloadLimit(kbps: Int) = preferencesStore.updateDownloadLimit(kbps)

    suspend fun setUploadLimit(kbps: Int) = preferencesStore.updateUploadLimit(kbps)

    suspend fun setWifiOnly(enabled: Boolean) = preferencesStore.updateWifiOnly(enabled)

    suspend fun setDhtEnabled(enabled: Boolean) = preferencesStore.updateDhtEnabled(enabled)

    suspend fun setPexEnabled(enabled: Boolean) = preferencesStore.updatePexEnabled(enabled)

    suspend fun setLsdEnabled(enabled: Boolean) = preferencesStore.updateLsdEnabled(enabled)

    suspend fun setMaxConnections(count: Int) = preferencesStore.updateMaxConnections(count)

    suspend fun setStopOnComplete(enabled: Boolean) = preferencesStore.updateStopOnComplete(enabled)

    suspend fun setTimerAction(action: TimerAction) = preferencesStore.updateTimerAction(action)
}
