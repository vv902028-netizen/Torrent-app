package com.example

import android.app.Application
import com.example.data.db.AppDatabase
import com.example.data.prefs.TrafficPreferencesStore
import com.example.data.repo.TorrentRepository
import com.example.torrent.TorrentEngineManager

class AppContainer(application: Application) {
    val database: AppDatabase by lazy { AppDatabase.create(application) }
    val preferencesStore: TrafficPreferencesStore by lazy { TrafficPreferencesStore(application) }
    val engineManager: TorrentEngineManager by lazy {
        TorrentEngineManager(
            context = application,
            torrentDao = database.torrentDao(),
            preferencesStore = preferencesStore
        )
    }
    val repository: TorrentRepository by lazy {
        TorrentRepository(
            engineManager = engineManager,
            preferencesStore = preferencesStore
        )
    }
}

class VoltTorrentApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
