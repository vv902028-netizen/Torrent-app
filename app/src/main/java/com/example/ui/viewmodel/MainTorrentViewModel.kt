package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.VoltTorrentApp
import com.example.data.repo.TorrentRepository
import com.example.domain.model.CatalogCategory
import com.example.domain.model.CatalogEntry
import com.example.domain.model.DownloadFilter
import com.example.domain.model.FilePriority
import com.example.domain.model.GlobalTelemetry
import com.example.domain.model.TimerAction
import com.example.domain.model.TimerState
import com.example.domain.model.TorrentState
import com.example.domain.model.TorrentUiItem
import com.example.domain.model.TrafficSettings
import com.example.torrent.EngineEvent
import com.example.torrent.MagnetParser
import com.example.torrent.ParsedMagnet
import com.example.torrent.TorrentFilePreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DownloadsUiState(
    val torrents: List<TorrentUiItem> = emptyList(),
    val filter: DownloadFilter = DownloadFilter.ALL,
    val searchQuery: String = "",
    val globalTelemetry: GlobalTelemetry = GlobalTelemetry(),
    val timerState: TimerState = TimerState(),
    val settings: TrafficSettings = TrafficSettings(),
    val allInfoHashes: Set<String> = emptySet()
)

data class AddSheetState(
    val isVisible: Boolean = false,
    val magnetInput: String = "",
    val parsedMagnet: ParsedMagnet? = null,
    val filePreview: TorrentFilePreview? = null,
    val selectedFileIndices: Set<Int> = emptySet(),
    val errorText: String? = null,
    val isSubmitting: Boolean = false
)

class MainTorrentViewModel(
    application: Application,
    private val repository: TorrentRepository
) : AndroidViewModel(application) {

    private val _selectedFilter = MutableStateFlow(DownloadFilter.ALL)
    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow(CatalogCategory.ALL)
    private val _addSheetState = MutableStateFlow(AddSheetState())
    val addSheetState: StateFlow<AddSheetState> = _addSheetState.asStateFlow()

    val selectedCategory: StateFlow<CatalogCategory> = _selectedCategory.asStateFlow()

    private val _snackbarMessages = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val snackbarMessages: SharedFlow<String> = _snackbarMessages.asSharedFlow()

    val settings: StateFlow<TrafficSettings> = repository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TrafficSettings()
    )

    val globalTelemetry: StateFlow<GlobalTelemetry> = repository.globalTelemetry
    val timerState: StateFlow<TimerState> = repository.timerState

    val downloadsUiState: StateFlow<DownloadsUiState> = combine(
        repository.torrents,
        _selectedFilter,
        _searchQuery,
        repository.globalTelemetry,
        repository.timerState,
        settings
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val allTorrents = values[0] as List<TorrentUiItem>
        val filter = values[1] as DownloadFilter
        val query = values[2] as String
        val telemetry = values[3] as GlobalTelemetry
        val timer = values[4] as TimerState
        val trafficSettings = values[5] as TrafficSettings

        val filtered = allTorrents.filter { item ->
            val matchesFilter = when (filter) {
                DownloadFilter.ALL -> true
                DownloadFilter.ACTIVE -> item.state == TorrentState.DOWNLOADING ||
                    item.state == TorrentState.SEEDING ||
                    item.state == TorrentState.METADATA ||
                    item.state == TorrentState.CHECKING
                DownloadFilter.COMPLETED -> item.state == TorrentState.COMPLETED ||
                    item.state == TorrentState.SEEDING ||
                    item.progress >= 0.999f
                DownloadFilter.PAUSED -> item.state == TorrentState.PAUSED ||
                    item.state == TorrentState.QUEUED ||
                    item.state == TorrentState.ERROR
            }
            val matchesQuery = query.isBlank() ||
                item.name.contains(query, ignoreCase = true) ||
                item.infoHash.contains(query, ignoreCase = true)
            matchesFilter && matchesQuery
        }

        DownloadsUiState(
            torrents = filtered,
            filter = filter,
            searchQuery = query,
            globalTelemetry = telemetry,
            timerState = timer,
            settings = trafficSettings,
            allInfoHashes = allTorrents.map { it.infoHash.lowercase() }.toSet()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DownloadsUiState()
    )

    val catalogItems: List<CatalogEntry> = repository.catalogEntries
    val downloadDirectoryPath: String = repository.downloadDirectoryPath

    init {
        viewModelScope.launch {
            repository.engineEvents.collect { event ->
                when (event) {
                    is EngineEvent.TimerExpired -> {
                        val msg = if (event.action == TimerAction.PAUSE_ALL) {
                            "Таймер истёк: все загрузки поставлены на паузу"
                        } else {
                            "Таймер истёк: фоновый сервис остановлен"
                        }
                        _snackbarMessages.tryEmit(msg)
                    }
                    is EngineEvent.TorrentCompleted -> {
                        _snackbarMessages.tryEmit("Загрузка завершена: ${event.name}")
                    }
                    is EngineEvent.Message -> {
                        _snackbarMessages.tryEmit(event.text)
                    }
                }
            }
        }
    }

    fun observeTorrent(infoHash: String) = repository.observeTorrent(infoHash)

    fun setFilter(filter: DownloadFilter) {
        _selectedFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCatalogCategory(category: CatalogCategory) {
        _selectedCategory.value = category
    }

    fun openAddSheet(initialMagnet: String = "") {
        val parsed = if (initialMagnet.isNotBlank()) MagnetParser.parse(initialMagnet) else null
        _addSheetState.value = AddSheetState(
            isVisible = true,
            magnetInput = initialMagnet,
            parsedMagnet = parsed
        )
    }

    fun closeAddSheet() {
        _addSheetState.value = AddSheetState(isVisible = false)
    }

    fun onMagnetInputChanged(value: String) {
        val parsed = MagnetParser.parse(value)
        _addSheetState.value = _addSheetState.value.copy(
            magnetInput = value,
            parsedMagnet = parsed,
            filePreview = null,
            errorText = null
        )
    }

    fun onTorrentFileSelected(uri: Uri) {
        viewModelScope.launch {
            repository.previewTorrentFile(uri).onSuccess { preview ->
                _addSheetState.value = _addSheetState.value.copy(
                    filePreview = preview,
                    selectedFileIndices = preview.files.map { it.index }.toSet(),
                    errorText = null
                )
            }.onFailure {
                _addSheetState.value = _addSheetState.value.copy(
                    errorText = "Не удалось прочитать .torrent-файл"
                )
            }
        }
    }

    fun togglePreviewFileSelection(index: Int) {
        val current = _addSheetState.value.selectedFileIndices
        val updated = if (index in current) current - index else current + index
        _addSheetState.value = _addSheetState.value.copy(selectedFileIndices = updated)
    }

    fun submitAddSheet() {
        val current = _addSheetState.value
        if (current.isSubmitting) return

        viewModelScope.launch {
            _addSheetState.value = current.copy(isSubmitting = true, errorText = null)
            val preview = current.filePreview
            if (preview != null) {
                if (current.selectedFileIndices.isEmpty()) {
                    _addSheetState.value = current.copy(
                        isSubmitting = false,
                        errorText = "Выберите хотя бы один файл для загрузки"
                    )
                    return@launch
                }
                repository.addTorrentFileBytes(preview.rawBytes, current.selectedFileIndices)
                    .onSuccess {
                        _addSheetState.value = AddSheetState(isVisible = false)
                        _snackbarMessages.tryEmit("Добавлен торрент: ${preview.name}")
                    }
                    .onFailure { err ->
                        _addSheetState.value = current.copy(
                            isSubmitting = false,
                            errorText = err.message ?: "Ошибка добавления файла"
                        )
                    }
                return@launch
            }

            val parsed = MagnetParser.parse(current.magnetInput)
            if (parsed == null) {
                _addSheetState.value = current.copy(
                    isSubmitting = false,
                    errorText = "Некорректная Magnet-ссылка (ожидается magnet:?xt=urn:btih:...)"
                )
                return@launch
            }

            repository.addMagnet(parsed.rawUri, parsed.displayName)
                .onSuccess {
                    _addSheetState.value = AddSheetState(isVisible = false)
                    _snackbarMessages.tryEmit("Торрент добавлен в очередь")
                }
                .onFailure { err ->
                    _addSheetState.value = current.copy(
                        isSubmitting = false,
                        errorText = err.message ?: "Ошибка запуска загрузки"
                    )
                }
        }
    }

    fun addFromCatalog(entry: CatalogEntry) {
        viewModelScope.launch {
            repository.addMagnet(entry.magnetUri, entry.title)
                .onSuccess {
                    _snackbarMessages.tryEmit("Запущено: ${entry.title}")
                }
                .onFailure { err ->
                    _snackbarMessages.tryEmit("Ошибка: ${err.message ?: "не удалось добавить"}")
                }
        }
    }

    fun handleIncomingMagnet(magnetUri: String) {
        openAddSheet(magnetUri)
    }

    fun handleIncomingTorrentUri(uri: Uri) {
        openAddSheet()
        onTorrentFileSelected(uri)
    }

    fun togglePauseResume(item: TorrentUiItem) {
        viewModelScope.launch {
            when (item.state) {
                TorrentState.PAUSED, TorrentState.ERROR, TorrentState.QUEUED ->
                    repository.resumeTorrent(item.infoHash)
                else ->
                    repository.pauseTorrent(item.infoHash)
            }
        }
    }

    fun pauseAll() {
        viewModelScope.launch {
            repository.pauseAll()
            _snackbarMessages.tryEmit("Все загрузки приостановлены")
        }
    }

    fun resumeAll() {
        viewModelScope.launch {
            repository.resumeAll()
            _snackbarMessages.tryEmit("Все загрузки возобновлены")
        }
    }

    fun removeTorrent(infoHash: String, deleteFiles: Boolean) {
        viewModelScope.launch {
            repository.removeTorrent(infoHash, deleteFiles)
            _snackbarMessages.tryEmit(
                if (deleteFiles) "Торрент и файлы удалены" else "Торрент удалён из списка"
            )
        }
    }

    fun setFilePriority(infoHash: String, fileIndex: Int, priority: FilePriority) {
        viewModelScope.launch {
            repository.setFilePriority(infoHash, fileIndex, priority)
        }
    }

    fun forceReannounce(infoHash: String) {
        viewModelScope.launch {
            repository.forceReannounce(infoHash)
        }
    }

    fun reconnectNetworkAndTrackers() {
        viewModelScope.launch {
            repository.reconnectNetworkAndTrackers()
        }
    }

    fun addTracker(infoHash: String, url: String) {
        viewModelScope.launch {
            val ok = repository.addTracker(infoHash, url)
            _snackbarMessages.tryEmit(
                if (ok) "Трекер добавлен и анонсирован" else "Некорректный адрес трекера"
            )
        }
    }

    fun setDownloadLimit(kbps: Int) {
        viewModelScope.launch { repository.setDownloadLimit(kbps) }
    }

    fun setUploadLimit(kbps: Int) {
        viewModelScope.launch { repository.setUploadLimit(kbps) }
    }

    fun setWifiOnly(enabled: Boolean) {
        viewModelScope.launch { repository.setWifiOnly(enabled) }
    }

    fun setDhtEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setDhtEnabled(enabled) }
    }

    fun setPexEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setPexEnabled(enabled) }
    }

    fun setLsdEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setLsdEnabled(enabled) }
    }

    fun setMaxConnections(count: Int) {
        viewModelScope.launch { repository.setMaxConnections(count) }
    }

    fun setStopOnComplete(enabled: Boolean) {
        viewModelScope.launch { repository.setStopOnComplete(enabled) }
    }

    fun setTimerAction(action: TimerAction) {
        viewModelScope.launch { repository.setTimerAction(action) }
    }

    fun startTimer(minutes: Int) {
        repository.startTimer(minutes, settings.value.timerAction)
        _snackbarMessages.tryEmit("Таймер запущен на $minutes мин")
    }

    fun cancelTimer() {
        repository.cancelTimer()
        _snackbarMessages.tryEmit("Таймер отменён")
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as VoltTorrentApp
                MainTorrentViewModel(app, app.container.repository)
            }
        }
    }
}
