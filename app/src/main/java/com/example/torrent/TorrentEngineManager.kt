package com.example.torrent

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat
import com.example.data.db.TorrentDao
import com.example.data.db.TorrentEntity
import com.example.data.prefs.TrafficPreferencesStore
import com.example.domain.model.FilePriority
import com.example.domain.model.GlobalTelemetry
import com.example.domain.model.NetworkHealth
import com.example.domain.model.PeerStat
import com.example.domain.model.SpeedPoint
import com.example.domain.model.TimerAction
import com.example.domain.model.TimerState
import com.example.domain.model.TorrentFileItem
import com.example.domain.model.TorrentState
import com.example.domain.model.TorrentUiItem
import com.example.domain.model.TrackerStat
import com.example.domain.model.TrackerStatus
import com.example.domain.model.TrafficSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.libtorrent4j.AnnounceEntry
import org.libtorrent4j.PeerInfo
import org.libtorrent4j.Priority
import org.libtorrent4j.SessionManager
import org.libtorrent4j.SessionParams
import org.libtorrent4j.SettingsPack
import org.libtorrent4j.Sha1Hash
import org.libtorrent4j.TorrentFlags
import org.libtorrent4j.TorrentHandle
import org.libtorrent4j.TorrentInfo
import org.libtorrent4j.TorrentStatus
import org.libtorrent4j.swig.settings_pack
import org.libtorrent4j.swig.string_int_pair
import org.libtorrent4j.swig.torrent_flags_t
import java.io.File
import java.util.concurrent.ConcurrentHashMap

sealed interface EngineEvent {
    data class TimerExpired(val action: TimerAction) : EngineEvent
    data class TorrentCompleted(val name: String) : EngineEvent
    data class Message(val text: String) : EngineEvent
}

private data class LiveTelemetrySnapshot(
    val torrentsByHash: Map<String, LiveTorrentRuntime> = emptyMap(),
    val global: GlobalTelemetry = GlobalTelemetry()
)

private data class LiveTorrentRuntime(
    val name: String,
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
    val errorMessage: String?,
    val pieceCount: Int,
    val piecesCompleted: Int,
    val pieceSegments: List<Float>,
    val files: List<TorrentFileItem>,
    val peers: List<PeerStat>,
    val trackers: List<TrackerStat>,
    val speedHistory: List<SpeedPoint>
)

class TorrentEngineManager(
    private val context: Context,
    private val torrentDao: TorrentDao,
    private val preferencesStore: TrafficPreferencesStore
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sessionManager = SessionManager()
    private val lock = Any()

    @Volatile
    private var isStarted = false

    @Volatile
    private var currentSettings = TrafficSettings()

    @Volatile
    private var isWifiConnected = true

    @Volatile
    private var isNetworkAvailable = true

    @Volatile
    private var currentTransportLabel = "Сеть"

    @Volatile
    private var lastNetworkKey = ""

    @Volatile
    private var isRecoveringNetwork = false

    @Volatile
    private var lastNetworkChangeMs = 0L

    private var recoveryJob: Job? = null

    private val globalSpeedBuffer = ArrayDeque<SpeedPoint>(64)
    private val torrentSpeedBuffers = ConcurrentHashMap<String, ArrayDeque<SpeedPoint>>()
    private val torrentInfoCache = ConcurrentHashMap<String, TorrentInfo>()
    private val notifiedCompletedHashes = ConcurrentHashMap.newKeySet<String>()

    private val liveSnapshot = MutableStateFlow(LiveTelemetrySnapshot())
    private val _timerState = MutableStateFlow(TimerState())
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    private val _events = MutableSharedFlow<EngineEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<EngineEvent> = _events.asSharedFlow()

    private val _torrents = MutableStateFlow<List<TorrentUiItem>>(emptyList())
    val torrents: StateFlow<List<TorrentUiItem>> = _torrents.asStateFlow()

    private val _globalTelemetry = MutableStateFlow(GlobalTelemetry())
    val globalTelemetry: StateFlow<GlobalTelemetry> = _globalTelemetry.asStateFlow()

    private var pollingJob: Job? = null
    private var timerJob: Job? = null
    private var wifiCallbackRegistered = false

    val defaultDownloadDir: File by lazy {
        val base = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: File(context.filesDir, "downloads")
        File(base, "VoltTorrent").apply { mkdirs() }
    }

    private val torrentFilesCacheDir: File by lazy {
        File(context.filesDir, "torrents_meta").apply { mkdirs() }
    }

    init {
        observeCombinedState()
        observeSettings()
        registerNetworkMonitor()
        scope.launch {
            ensureSessionStarted()
            restoreSavedTorrents()
        }
    }

    private fun observeCombinedState() {
        scope.launch {
            combine(
                torrentDao.observeAll(),
                liveSnapshot
            ) { entities, snapshot ->
                _globalTelemetry.value = snapshot.global
                entities.map { entity ->
                    val live = snapshot.torrentsByHash[entity.infoHash]
                    if (live != null) {
                        TorrentUiItem(
                            infoHash = entity.infoHash,
                            name = live.name.ifBlank { entity.name },
                            sourceUri = entity.sourceUri,
                            saveDir = entity.saveDir,
                            totalBytes = if (live.totalBytes > 0L) live.totalBytes else entity.totalBytes,
                            downloadedBytes = live.downloadedBytes,
                            uploadedBytes = live.uploadedBytes,
                            progress = live.progress,
                            downloadSpeedBps = live.downloadSpeedBps,
                            uploadSpeedBps = live.uploadSpeedBps,
                            etaSeconds = live.etaSeconds,
                            connectedPeers = live.connectedPeers,
                            totalPeers = live.totalPeers,
                            connectedSeeds = live.connectedSeeds,
                            totalSeeds = live.totalSeeds,
                            ratio = live.ratio,
                            activeTimeSeconds = live.activeTimeSeconds,
                            state = live.state,
                            addedAt = entity.addedAt,
                            errorMessage = live.errorMessage ?: entity.errorMessage,
                            pieceCount = live.pieceCount,
                            piecesCompleted = live.piecesCompleted,
                            pieceSegments = live.pieceSegments,
                            files = live.files,
                            peers = live.peers,
                            trackers = live.trackers,
                            speedHistory = live.speedHistory
                        )
                    } else {
                        val savedState = runCatching { TorrentState.valueOf(entity.statusName) }
                            .getOrDefault(TorrentState.PAUSED)
                        TorrentUiItem(
                            infoHash = entity.infoHash,
                            name = entity.name,
                            sourceUri = entity.sourceUri,
                            saveDir = entity.saveDir,
                            totalBytes = entity.totalBytes,
                            downloadedBytes = entity.downloadedBytes,
                            uploadedBytes = entity.uploadedBytes,
                            progress = entity.progress,
                            downloadSpeedBps = 0L,
                            uploadSpeedBps = 0L,
                            etaSeconds = -1L,
                            connectedPeers = 0,
                            totalPeers = 0,
                            connectedSeeds = 0,
                            totalSeeds = 0,
                            ratio = if (entity.downloadedBytes > 0L) {
                                entity.uploadedBytes.toFloat() / entity.downloadedBytes.toFloat()
                            } else 0f,
                            activeTimeSeconds = 0L,
                            state = savedState,
                            addedAt = entity.addedAt,
                            errorMessage = entity.errorMessage,
                            speedHistory = torrentSpeedBuffers[entity.infoHash]?.toList().orEmpty()
                        )
                    }
                }
            }.collect { merged ->
                _torrents.value = merged
            }
        }
    }

    private fun observeSettings() {
        scope.launch {
            preferencesStore.settingsFlow.collect { settings ->
                currentSettings = settings
                _timerState.value = _timerState.value.copy(
                    action = settings.timerAction,
                    stopOnComplete = settings.stopOnComplete
                )
                applySessionSettings(settings)
            }
        }
    }

    private fun registerNetworkMonitor() {
        if (wifiCallbackRegistered) return
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
        val initialCaps = cm.activeNetwork?.let { cm.getNetworkCapabilities(it) }
        updateNetworkState(cm.activeNetwork, initialCaps, triggerRecovery = false)

        runCatching {
            cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    val caps = cm.getNetworkCapabilities(network)
                    updateNetworkState(network, caps, triggerRecovery = true)
                }

                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    updateNetworkState(network, networkCapabilities, triggerRecovery = true)
                }

                override fun onLost(network: Network) {
                    val active = cm.activeNetwork
                    val caps = active?.let { cm.getNetworkCapabilities(it) }
                    updateNetworkState(active, caps, triggerRecovery = true)
                }
            })
            wifiCallbackRegistered = true
        }
    }

    private fun updateNetworkState(
        network: Network?,
        caps: NetworkCapabilities?,
        triggerRecovery: Boolean
    ) {
        val hasInternet = network != null && caps != null &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val isWifi = caps != null && (
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            )
        val isVpn = caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
        val isCellular = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true

        isNetworkAvailable = hasInternet
        isWifiConnected = isWifi
        currentTransportLabel = when {
            !hasInternet -> "Нет сети"
            isVpn && isWifi -> "Wi-Fi + VPN"
            isVpn && isCellular -> "Мобильная + VPN"
            isVpn -> "VPN"
            isWifi -> "Wi-Fi"
            isCellular -> "Мобильная сеть"
            else -> "Сеть"
        }

        val newKey = if (!hasInternet) {
            "OFFLINE"
        } else {
            "${network?.networkHandle ?: 0}:$currentTransportLabel"
        }

        val changed = lastNetworkKey.isNotEmpty() && newKey != lastNetworkKey
        lastNetworkKey = newKey

        applyWifiConstraint()

        if (triggerRecovery && changed) {
            lastNetworkChangeMs = System.currentTimeMillis()
            if (hasInternet && isStarted && (!currentSettings.wifiOnly || isWifi)) {
                scheduleNetworkRecovery()
            }
        }
    }

    private fun scheduleNetworkRecovery() {
        recoveryJob?.cancel()
        recoveryJob = scope.launch {
            isRecoveringNetwork = true
            delay(650L)
            recoverNetworkAndTrackers(emitToast = false)
            delay(3500L)
            isRecoveringNetwork = false
        }
    }

    suspend fun recoverNetworkAndTrackers(emitToast: Boolean = true) = withContext(Dispatchers.IO) {
        if (!isStarted) return@withContext
        isRecoveringNetwork = true
        runCatching {
            sessionManager.reopenNetworkSockets()
            if (currentSettings.dhtEnabled && sessionManager.dhtNodes() <= 0L) {
                sessionManager.swig().add_dht_node(string_int_pair("router.bittorrent.com", 6881))
                sessionManager.swig().add_dht_node(string_int_pair("dht.transmissionbt.com", 6881))
                sessionManager.swig().add_dht_node(string_int_pair("router.utorrent.com", 6881))
            }
            getAllActiveHandles().forEach { th ->
                if (th.isValid) {
                    val isPaused = th.status().flags().and_(TorrentFlags.PAUSED).to_int() != 0
                    if (!isPaused) {
                        th.forceReannounce(0, -1, TorrentHandle.IGNORE_MIN_INTERVAL)
                        th.forceDHTAnnounce()
                    }
                }
            }
        }
        if (emitToast) {
            _events.tryEmit(EngineEvent.Message("Сокеты и трекеры переподключены"))
        }
        delay(2500L)
        isRecoveringNetwork = false
    }

    private fun applyWifiConstraint() {
        if (!isStarted) return
        if (currentSettings.wifiOnly && !isWifiConnected) {
            runCatching { sessionManager.pause() }
        } else {
            runCatching { sessionManager.resume() }
        }
    }

    fun ensureSessionStarted() {
        synchronized(lock) {
            if (isStarted) return
            val sp = buildSettingsPack(currentSettings)
            val params = SessionParams(sp)
            sessionManager.start(params)
            runCatching {
                sessionManager.swig().add_dht_node(string_int_pair("router.bittorrent.com", 6881))
                sessionManager.swig().add_dht_node(string_int_pair("dht.transmissionbt.com", 6881))
                sessionManager.swig().add_dht_node(string_int_pair("router.utorrent.com", 6881))
                sessionManager.swig().add_dht_node(string_int_pair("dht.aelitis.com", 6881))
            }
            isStarted = true
            applyWifiConstraint()
            startPollingLoop()
        }
    }

    private fun buildSettingsPack(settings: TrafficSettings): SettingsPack {
        val sp = SettingsPack()
        sp.downloadRateLimit(if (settings.downloadLimitKbps > 0) settings.downloadLimitKbps * 1024 else 0)
        sp.uploadRateLimit(if (settings.uploadLimitKbps > 0) settings.uploadLimitKbps * 1024 else 0)
        sp.setBoolean(settings_pack.bool_types.enable_dht.swigValue(), settings.dhtEnabled)
        sp.setBoolean(settings_pack.bool_types.enable_lsd.swigValue(), settings.lsdEnabled)
        sp.connectionsLimit(settings.maxConnections)
        return sp
    }

    private fun applySessionSettings(settings: TrafficSettings) {
        if (!isStarted) return
        runCatching {
            val sp = buildSettingsPack(settings)
            sessionManager.applySettings(sp)
            sessionManager.downloadRateLimit(if (settings.downloadLimitKbps > 0) settings.downloadLimitKbps * 1024 else 0)
            sessionManager.uploadRateLimit(if (settings.uploadLimitKbps > 0) settings.uploadLimitKbps * 1024 else 0)
            sessionManager.maxConnections(settings.maxConnections)
            if (settings.dhtEnabled && !sessionManager.isDhtRunning) {
                sessionManager.startDht()
            } else if (!settings.dhtEnabled && sessionManager.isDhtRunning) {
                sessionManager.stopDht()
            }
            getAllActiveHandles().forEach { th ->
                if (th.isValid) {
                    if (settings.pexEnabled) {
                        th.unsetFlags(TorrentFlags.DISABLE_PEX)
                    } else {
                        th.setFlags(TorrentFlags.DISABLE_PEX)
                    }
                }
            }
        }
        applyWifiConstraint()
    }

    private suspend fun restoreSavedTorrents() {
        val saved = torrentDao.getAllOnce()
        saved.forEach { entity ->
            val wasPaused = entity.statusName == TorrentState.PAUSED.name
            val flags = if (wasPaused) TorrentFlags.PAUSED else torrent_flags_t()
            runCatching {
                if (entity.isTorrentFile && !entity.torrentFilePath.isNullOrBlank()) {
                    val f = File(entity.torrentFilePath)
                    if (f.exists()) {
                        val ti = TorrentInfo(f)
                        sessionManager.download(ti, File(entity.saveDir), null, null, null, flags)
                    }
                } else if (entity.sourceUri.startsWith("magnet:", ignoreCase = true)) {
                    sessionManager.download(entity.sourceUri, File(entity.saveDir), flags)
                }
            }
        }
        if (saved.any { it.statusName != TorrentState.PAUSED.name }) {
            ensureForegroundServiceRunning()
        }
    }

    suspend fun addMagnet(magnetUri: String, customName: String? = null): Result<String> =
        withContext(Dispatchers.IO) {
            val parsed = MagnetParser.parse(magnetUri)
                ?: return@withContext Result.failure(IllegalArgumentException("Некорректная Magnet-ссылка"))

            ensureSessionStarted()
            val displayName = customName?.takeIf { it.isNotBlank() }
                ?: parsed.displayName
                ?: "Magnet (${parsed.infoHash.take(8)})"

            val existing = torrentDao.getAllOnce().firstOrNull { it.infoHash == parsed.infoHash }
            if (existing == null) {
                torrentDao.upsert(
                    TorrentEntity(
                        infoHash = parsed.infoHash,
                        name = displayName,
                        sourceUri = parsed.rawUri,
                        isTorrentFile = false,
                        torrentFilePath = null,
                        saveDir = defaultDownloadDir.absolutePath,
                        totalBytes = 0L,
                        downloadedBytes = 0L,
                        uploadedBytes = 0L,
                        progress = 0f,
                        statusName = TorrentState.METADATA.name,
                        addedAt = System.currentTimeMillis(),
                        completedAt = null,
                        errorMessage = null
                    )
                )
            }

            runCatching {
                sessionManager.download(parsed.rawUri, defaultDownloadDir, torrent_flags_t())
            }.onFailure { err ->
                torrentDao.updateStatus(parsed.infoHash, TorrentState.ERROR.name)
                return@withContext Result.failure(err)
            }

            ensureForegroundServiceRunning()
            Result.success(parsed.infoHash)
        }

    suspend fun previewTorrentFile(uri: Uri): Result<TorrentFilePreview> =
        withContext(Dispatchers.IO) {
            runCatching {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: error("Не удалось открыть файл")
                val ti = TorrentInfo.bdecode(bytes)
                val files = (0 until ti.numFiles()).map { idx ->
                    TorrentFileItem(
                        index = idx,
                        path = ti.files().filePath(idx),
                        name = ti.files().fileName(idx),
                        sizeBytes = ti.files().fileSize(idx),
                        downloadedBytes = 0L,
                        priority = FilePriority.NORMAL
                    )
                }
                TorrentFilePreview(
                    infoHash = ti.infoHash().toHex().lowercase(),
                    name = ti.name(),
                    totalBytes = ti.totalSize(),
                    files = files,
                    rawBytes = bytes
                )
            }
        }

    suspend fun addTorrentFileBytes(
        rawBytes: ByteArray,
        selectedIndices: Set<Int>? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            ensureSessionStarted()
            val ti = TorrentInfo.bdecode(rawBytes)
            val hash = ti.infoHash().toHex().lowercase()
            val cachedFile = File(torrentFilesCacheDir, "$hash.torrent")
            cachedFile.writeBytes(rawBytes)

            val totalWanted = if (selectedIndices != null && selectedIndices.isNotEmpty()) {
                selectedIndices.sumOf { idx ->
                    if (idx in 0 until ti.numFiles()) ti.files().fileSize(idx) else 0L
                }
            } else {
                ti.totalSize()
            }

            torrentDao.upsert(
                TorrentEntity(
                    infoHash = hash,
                    name = ti.name(),
                    sourceUri = cachedFile.absolutePath,
                    isTorrentFile = true,
                    torrentFilePath = cachedFile.absolutePath,
                    saveDir = defaultDownloadDir.absolutePath,
                    totalBytes = totalWanted,
                    downloadedBytes = 0L,
                    uploadedBytes = 0L,
                    progress = 0f,
                    statusName = TorrentState.DOWNLOADING.name,
                    addedAt = System.currentTimeMillis(),
                    completedAt = null,
                    errorMessage = null
                )
            )

            val priorities = if (selectedIndices != null) {
                Array(ti.numFiles()) { idx ->
                    if (idx in selectedIndices) Priority.DEFAULT else Priority.IGNORE
                }
            } else {
                null
            }

            sessionManager.download(
                ti,
                defaultDownloadDir,
                null,
                priorities,
                null,
                torrent_flags_t()
            )
            ensureForegroundServiceRunning()
            hash
        }
    }

    suspend fun pauseTorrent(infoHash: String) = withContext(Dispatchers.IO) {
        findHandle(infoHash)?.let { handle ->
            if (handle.isValid) {
                handle.unsetFlags(TorrentFlags.AUTO_MANAGED)
                handle.pause()
            }
        }
        torrentDao.updateStatus(infoHash, TorrentState.PAUSED.name)
    }

    suspend fun resumeTorrent(infoHash: String) = withContext(Dispatchers.IO) {
        ensureSessionStarted()
        val handle = findHandle(infoHash)
        if (handle != null && handle.isValid) {
            handle.setFlags(TorrentFlags.AUTO_MANAGED)
            handle.resume()
        } else {
            val entity = torrentDao.getAllOnce().firstOrNull { it.infoHash == infoHash }
            if (entity != null) {
                runCatching {
                    if (entity.isTorrentFile && !entity.torrentFilePath.isNullOrBlank()) {
                        val f = File(entity.torrentFilePath)
                        if (f.exists()) {
                            sessionManager.download(
                                TorrentInfo(f),
                                File(entity.saveDir),
                                null,
                                null,
                                null,
                                torrent_flags_t()
                            )
                        }
                    } else {
                        sessionManager.download(
                            entity.sourceUri,
                            File(entity.saveDir),
                            torrent_flags_t()
                        )
                    }
                }
            }
        }
        torrentDao.updateStatus(infoHash, TorrentState.DOWNLOADING.name)
        ensureForegroundServiceRunning()
    }

    suspend fun pauseAll() = withContext(Dispatchers.IO) {
        getAllActiveHandles().forEach { th ->
            if (th.isValid) {
                th.unsetFlags(TorrentFlags.AUTO_MANAGED)
                th.pause()
            }
        }
        torrentDao.getAllOnce().forEach { entity ->
            if (entity.statusName != TorrentState.COMPLETED.name) {
                torrentDao.updateStatus(entity.infoHash, TorrentState.PAUSED.name)
            }
        }
    }

    suspend fun resumeAll() = withContext(Dispatchers.IO) {
        ensureSessionStarted()
        getAllActiveHandles().forEach { th ->
            if (th.isValid) {
                th.setFlags(TorrentFlags.AUTO_MANAGED)
                th.resume()
            }
        }
        torrentDao.getAllOnce().forEach { entity ->
            if (entity.statusName == TorrentState.PAUSED.name) {
                torrentDao.updateStatus(entity.infoHash, TorrentState.DOWNLOADING.name)
            }
        }
        ensureForegroundServiceRunning()
    }

    suspend fun removeTorrent(infoHash: String, deleteFiles: Boolean) = withContext(Dispatchers.IO) {
        val entity = torrentDao.getAllOnce().firstOrNull { it.infoHash == infoHash }
        val handle = findHandle(infoHash)
        if (handle != null && handle.isValid) {
            if (deleteFiles) {
                sessionManager.remove(handle, org.libtorrent4j.swig.session_handle.delete_files)
            } else {
                sessionManager.remove(handle)
            }
        }
        if (entity?.torrentFilePath != null) {
            runCatching { File(entity.torrentFilePath).delete() }
        }
        torrentInfoCache.remove(infoHash)
        torrentSpeedBuffers.remove(infoHash)
        torrentDao.deleteByHash(infoHash)
    }

    suspend fun setFilePriority(infoHash: String, fileIndex: Int, priority: FilePriority) =
        withContext(Dispatchers.IO) {
            val handle = findHandle(infoHash) ?: return@withContext
            if (!handle.isValid) return@withContext
            val ltPriority = when (priority) {
                FilePriority.SKIP -> Priority.IGNORE
                FilePriority.NORMAL -> Priority.DEFAULT
                FilePriority.HIGH -> Priority.TOP_PRIORITY
            }
            handle.filePriority(fileIndex, ltPriority)
        }

    suspend fun forceReannounce(infoHash: String) = withContext(Dispatchers.IO) {
        val handle = findHandle(infoHash) ?: return@withContext
        if (handle.isValid) {
            runCatching { sessionManager.reopenNetworkSockets() }
            handle.forceReannounce(0, -1, TorrentHandle.IGNORE_MIN_INTERVAL)
            handle.forceDHTAnnounce()
            _events.tryEmit(EngineEvent.Message("Сокеты обновлены, запрос трекерам отправлен"))
        }
    }

    suspend fun addTracker(infoHash: String, trackerUrl: String): Boolean =
        withContext(Dispatchers.IO) {
            val cleanUrl = trackerUrl.trim()
            if (cleanUrl.isEmpty()) return@withContext false
            val handle = findHandle(infoHash) ?: return@withContext false
            if (!handle.isValid) return@withContext false
            runCatching {
                handle.addTracker(AnnounceEntry(cleanUrl))
                handle.forceReannounce()
                true
            }.getOrDefault(false)
        }

    fun startTimer(durationMinutes: Int, action: TimerAction) {
        timerJob?.cancel()
        val totalSec = (durationMinutes * 60).coerceAtLeast(60)
        _timerState.value = TimerState(
            isActive = true,
            totalSeconds = totalSec,
            remainingSeconds = totalSec,
            action = action,
            stopOnComplete = currentSettings.stopOnComplete
        )
        timerJob = scope.launch {
            var remaining = totalSec
            while (isActive && remaining > 0) {
                delay(1000L)
                remaining--
                _timerState.value = _timerState.value.copy(remainingSeconds = remaining)
            }
            if (isActive && remaining <= 0) {
                _timerState.value = _timerState.value.copy(isActive = false, remainingSeconds = 0)
                when (action) {
                    TimerAction.PAUSE_ALL -> pauseAll()
                    TimerAction.STOP_SERVICE -> {
                        pauseAll()
                        stopForegroundService()
                    }
                }
                _events.tryEmit(EngineEvent.TimerExpired(action))
            }
        }
    }

    fun cancelTimer() {
        timerJob?.cancel()
        timerJob = null
        _timerState.value = _timerState.value.copy(isActive = false, remainingSeconds = 0)
    }

    private fun getAllActiveHandles(): List<TorrentHandle> {
        if (!isStarted) return emptyList()
        return runCatching {
            val vec = sessionManager.swig().get_torrents()
            val count = vec.size
            (0 until count).map { idx -> TorrentHandle(vec[idx]) }
        }.getOrDefault(emptyList())
    }

    private fun findHandle(infoHash: String): TorrentHandle? {
        if (!isStarted) return null
        val target = infoHash.lowercase()
        val direct = runCatching { sessionManager.find(Sha1Hash.parseHex(target)) }.getOrNull()
        if (direct != null && direct.isValid) return direct
        return getAllActiveHandles().firstOrNull {
            it.isValid && it.infoHash().toHex().equals(target, ignoreCase = true)
        }
    }

    private fun startPollingLoop() {
        pollingJob?.cancel()
        pollingJob = scope.launch {
            var dbSyncCounter = 0
            while (isActive) {
                delay(1000L)
                if (!isStarted) continue
                pollEngineTick(persistToDb = (dbSyncCounter % 3 == 0))
                dbSyncCounter++
            }
        }
    }

    private suspend fun pollEngineTick(persistToDb: Boolean) {
        val now = System.currentTimeMillis()
        val globalDown = runCatching { sessionManager.downloadRate() }.getOrDefault(0L)
        val globalUp = runCatching { sessionManager.uploadRate() }.getOrDefault(0L)
        val dhtNodes = runCatching { sessionManager.dhtNodes() }.getOrDefault(0L)

        synchronized(globalSpeedBuffer) {
            if (globalSpeedBuffer.size >= 60) globalSpeedBuffer.removeFirst()
            globalSpeedBuffer.addLast(SpeedPoint(now, globalDown, globalUp))
        }

        val handles = getAllActiveHandles()
        val runtimeMap = HashMap<String, LiveTorrentRuntime>(handles.size)
        var activeCount = 0
        var allCompleted = handles.isNotEmpty()

        for (th in handles) {
            if (!th.isValid) continue
            val hash = th.infoHash().toHex().lowercase()
            val status = th.status()
            val ti = if (status.hasMetadata()) {
                torrentInfoCache[hash] ?: th.torrentFile()?.also { torrentInfoCache[hash] = it }
            } else null

            val totalWanted = status.totalWanted().coerceAtLeast(0L)
            val totalDone = status.totalWantedDone().coerceAtLeast(0L)
            val downRate = status.downloadPayloadRate().toLong().coerceAtLeast(0L)
            val upRate = status.uploadPayloadRate().toLong().coerceAtLeast(0L)
            val progress = status.progress().coerceIn(0f, 1f)
            val isPaused = status.flags().and_(TorrentFlags.PAUSED).to_int() != 0

            val mappedState = mapTorrentState(status, isPaused, progress)
            if (mappedState == TorrentState.DOWNLOADING || mappedState == TorrentState.METADATA || mappedState == TorrentState.CHECKING) {
                activeCount++
                allCompleted = false
            } else if (mappedState != TorrentState.COMPLETED && mappedState != TorrentState.SEEDING) {
                allCompleted = false
            }

            if ((mappedState == TorrentState.COMPLETED || mappedState == TorrentState.SEEDING) &&
                totalWanted > 0L && progress >= 0.999f && notifiedCompletedHashes.add(hash)
            ) {
                _events.tryEmit(EngineEvent.TorrentCompleted(status.name().ifBlank { ti?.name() ?: hash }))
            }

            val eta = if (downRate > 0L && totalWanted > totalDone) {
                (totalWanted - totalDone) / downRate
            } else -1L

            val buf = torrentSpeedBuffers.getOrPut(hash) { ArrayDeque(64) }
            synchronized(buf) {
                if (buf.size >= 60) buf.removeFirst()
                buf.addLast(SpeedPoint(now, downRate, upRate))
            }

            val files = extractFiles(th, ti)
            val peers = extractPeers(th)
            val trackers = extractTrackers(th)
            val pieceSegments = extractPieceSegments(status, ti)

            val totalUploaded = status.allTimeUpload()
            val totalDownloaded = status.allTimeDownload().coerceAtLeast(totalDone)
            val ratio = if (totalDownloaded > 0L) totalUploaded.toFloat() / totalDownloaded.toFloat() else 0f

            val displayName = status.name().ifBlank { ti?.name().orEmpty() }
            val errMsg = if (status.errorCode().isError) status.errorCode().message else null
            val runtime = LiveTorrentRuntime(
                name = displayName,
                totalBytes = totalWanted,
                downloadedBytes = totalDone,
                uploadedBytes = totalUploaded,
                progress = progress,
                downloadSpeedBps = downRate,
                uploadSpeedBps = upRate,
                etaSeconds = eta,
                connectedPeers = status.numPeers(),
                totalPeers = status.listPeers(),
                connectedSeeds = status.numSeeds(),
                totalSeeds = status.listSeeds(),
                ratio = ratio,
                activeTimeSeconds = status.activeDuration() / 1000L,
                state = mappedState,
                errorMessage = errMsg,
                pieceCount = ti?.numPieces() ?: 0,
                piecesCompleted = status.numPieces(),
                pieceSegments = pieceSegments,
                files = files,
                peers = peers,
                trackers = trackers,
                speedHistory = synchronized(buf) { buf.toList() }
            )
            runtimeMap[hash] = runtime

            if (persistToDb) {
                val completedAt = if (progress >= 0.999f) now else null
                torrentDao.updateProgress(
                    infoHash = hash,
                    name = displayName.ifBlank { "Torrent (${hash.take(8)})" },
                    totalBytes = totalWanted,
                    downloadedBytes = totalDone,
                    uploadedBytes = totalUploaded,
                    progress = progress,
                    statusName = mappedState.name,
                    completedAt = completedAt,
                    errorMessage = runtime.errorMessage
                )
            }
        }

        if (currentSettings.stopOnComplete && allCompleted && handles.isNotEmpty() && activeCount == 0) {
            val hasFinished = runtimeMap.values.any { it.progress >= 0.999f }
            if (hasFinished) {
                preferencesStore.updateStopOnComplete(false)
                pauseAll()
                stopForegroundService()
                _events.tryEmit(EngineEvent.Message("Все загрузки завершены — сервис остановлен"))
            }
        }

        val wifiPaused = currentSettings.wifiOnly && !isWifiConnected
        val runningUnpaused = runtimeMap.values.filter {
            it.state == TorrentState.DOWNLOADING ||
                it.state == TorrentState.SEEDING ||
                it.state == TorrentState.METADATA
        }
        val hasTrackerIssues = runningUnpaused.isNotEmpty() && runningUnpaused.any { rt ->
            rt.connectedPeers == 0 && (
                rt.trackers.any { it.fails > 0 || it.status == TrackerStatus.ERROR } ||
                    (lastNetworkChangeMs > 0L && now - lastNetworkChangeMs < 180_000L && rt.activeTimeSeconds > 10L)
                )
        }

        val (networkHealth, warningMessage) = when {
            !isNetworkAvailable -> NetworkHealth.OFFLINE to
                "Нет подключения к интернету — трекеры и раздача остановлены до появления сети."
            isRecoveringNetwork -> NetworkHealth.RECOVERING to
                "Сеть изменилась ($currentTransportLabel) — переподключаем сокеты и анонсируем трекеры..."
            !wifiPaused && hasTrackerIssues -> NetworkHealth.TRACKER_STALLED to
                "Трекеры не отвечают или подвисли после смены сети. Нажмите «Обновить», чтобы переоткрыть сокеты."
            else -> NetworkHealth.ONLINE to null
        }

        val globalSnapshot = GlobalTelemetry(
            totalDownloadBps = globalDown,
            totalUploadBps = globalUp,
            dhtNodes = dhtNodes,
            activeCount = activeCount,
            isWifiPaused = wifiPaused,
            networkHealth = networkHealth,
            networkTransportLabel = currentTransportLabel,
            networkWarningMessage = warningMessage,
            speedHistory = synchronized(globalSpeedBuffer) { globalSpeedBuffer.toList() }
        )
        liveSnapshot.value = LiveTelemetrySnapshot(runtimeMap, globalSnapshot)
    }

    private fun mapTorrentState(
        status: TorrentStatus,
        isPaused: Boolean,
        progress: Float
    ): TorrentState {
        if (status.errorCode().isError) return TorrentState.ERROR
        if (isPaused) {
            return if (progress >= 0.999f) TorrentState.COMPLETED else TorrentState.PAUSED
        }
        return when (status.state()) {
            TorrentStatus.State.CHECKING_FILES,
            TorrentStatus.State.CHECKING_RESUME_DATA -> TorrentState.CHECKING
            TorrentStatus.State.DOWNLOADING_METADATA -> TorrentState.METADATA
            TorrentStatus.State.DOWNLOADING -> TorrentState.DOWNLOADING
            TorrentStatus.State.FINISHED -> TorrentState.COMPLETED
            TorrentStatus.State.SEEDING -> TorrentState.SEEDING
            else -> TorrentState.QUEUED
        }
    }

    private fun extractFiles(th: TorrentHandle, ti: TorrentInfo?): List<TorrentFileItem> {
        if (ti == null) return emptyList()
        val count = ti.numFiles()
        val fileProgress = runCatching { th.fileProgress() }.getOrNull()
        val priorities = runCatching { th.filePriorities() }.getOrNull()
        val storage = ti.files()

        return (0 until count).map { idx ->
            val ltPriority = priorities?.getOrNull(idx) ?: Priority.DEFAULT
            val mappedPriority = when (ltPriority) {
                Priority.IGNORE -> FilePriority.SKIP
                Priority.TOP_PRIORITY, Priority.SIX -> FilePriority.HIGH
                else -> FilePriority.NORMAL
            }
            TorrentFileItem(
                index = idx,
                path = storage.filePath(idx),
                name = storage.fileName(idx),
                sizeBytes = storage.fileSize(idx),
                downloadedBytes = fileProgress?.getOrNull(idx) ?: 0L,
                priority = mappedPriority
            )
        }
    }

    private fun extractPeers(th: TorrentHandle): List<PeerStat> {
        val rawPeers = runCatching { th.peerInfo() }.getOrDefault(emptyList())
        return rawPeers.map { p: PeerInfo ->
            val ipStr = p.ip()
            val hostPart = ipStr.substringBeforeLast(':', ipStr)
            val portPart = ipStr.substringAfterLast(':', "0").toIntOrNull() ?: 0
            val isUtp = p.connectionType() == PeerInfo.ConnectionType.WEB_SEED ||
                (p.flags() and 0x20000) != 0
            val proto = if ((p.flags() and 0x20000) != 0) "uTP" else "TCP"
            val flagTags = buildList {
                if ((p.flags() and 0x01) != 0) add("I")
                if ((p.flags() and 0x10) != 0) add("E")
                add(proto)
            }.joinToString("·")

            PeerStat(
                ip = hostPart,
                port = portPart,
                client = p.client().ifBlank { "BitTorrent Peer" },
                protocol = if (isUtp) "uTP" else "TCP",
                downSpeedBps = p.downSpeed().toLong().coerceAtLeast(0L),
                upSpeedBps = p.upSpeed().toLong().coerceAtLeast(0L),
                progress = p.progress().coerceIn(0f, 1f),
                flags = flagTags
            )
        }.sortedByDescending { it.downSpeedBps + it.upSpeedBps }
    }

    private fun extractTrackers(th: TorrentHandle): List<TrackerStat> {
        val entries = runCatching { th.trackers() }.getOrDefault(emptyList())
        return entries.map { entry ->
            val endpoints = entry.endpoints()
            val firstEp = endpoints.firstOrNull()
            val v1 = firstEp?.infohashV1()
            val isWorking = v1?.isWorking == true
            val isUpdating = v1?.updating() == true
            val fails = (v1?.fails() ?: 0).toInt()
            val message = v1?.message().orEmpty()
            val swigV1 = v1?.swig()
            val seeds = swigV1?.scrape_complete ?: 0
            val peers = swigV1?.scrape_incomplete ?: 0
            val nextAnnounce = swigV1?._next_announce?.let {
                ((it - System.currentTimeMillis() / 1000L).toInt()).coerceAtLeast(0)
            } ?: 0

            val trackerStatus = when {
                isUpdating -> TrackerStatus.UPDATING
                isWorking -> TrackerStatus.WORKING
                fails > 0 || message.isNotBlank() -> TrackerStatus.ERROR
                else -> TrackerStatus.UPDATING
            }

            TrackerStat(
                url = entry.url(),
                tier = entry.tier().toInt(),
                status = trackerStatus,
                seeds = seeds.coerceAtLeast(0),
                peers = peers.coerceAtLeast(0),
                fails = fails,
                nextAnnounceSeconds = nextAnnounce,
                message = message
            )
        }
    }

    private fun extractPieceSegments(status: TorrentStatus, ti: TorrentInfo?): List<Float> {
        val numPieces = ti?.numPieces() ?: 0
        if (numPieces <= 0) return emptyList()
        val pieces = status.pieces()
        val buckets = 32
        val result = MutableList(buckets) { 0f }
        if (pieces == null || pieces.size() <= 0) {
            val fill = status.progress().coerceIn(0f, 1f)
            return List(buckets) { idx ->
                val threshold = (idx + 1).toFloat() / buckets.toFloat()
                if (fill >= threshold) 1f else if (fill > idx.toFloat() / buckets) 0.5f else 0f
            }
        }
        for (b in 0 until buckets) {
            val start = (b * numPieces) / buckets
            val end = (((b + 1) * numPieces) / buckets).coerceAtLeast(start + 1).coerceAtMost(numPieces)
            var done = 0
            for (p in start until end) {
                if (pieces.getBit(p)) done++
            }
            result[b] = done.toFloat() / (end - start).toFloat()
        }
        return result
    }

    private fun ensureForegroundServiceRunning() {
        val intent = Intent(context, TorrentForegroundService::class.java).apply {
            action = TorrentForegroundService.ACTION_START
        }
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(context, intent)
            } else {
                context.startService(intent)
            }
        }
    }

    private fun stopForegroundService() {
        val intent = Intent(context, TorrentForegroundService::class.java).apply {
            action = TorrentForegroundService.ACTION_STOP
        }
        runCatching { context.startService(intent) }
    }

    fun shutdown() {
        pollingJob?.cancel()
        timerJob?.cancel()
        if (isStarted) {
            runCatching { sessionManager.stop() }
            isStarted = false
        }
    }
}

data class TorrentFilePreview(
    val infoHash: String,
    val name: String,
    val totalBytes: Long,
    val files: List<TorrentFileItem>,
    val rawBytes: ByteArray
)
