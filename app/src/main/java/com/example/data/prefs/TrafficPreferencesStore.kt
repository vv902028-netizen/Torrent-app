package com.example.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.TimerAction
import com.example.domain.model.TrafficSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.trafficDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "volt_traffic_settings"
)

class TrafficPreferencesStore(private val context: Context) {

    private object Keys {
        val DOWNLOAD_LIMIT_KBPS = intPreferencesKey("download_limit_kbps")
        val UPLOAD_LIMIT_KBPS = intPreferencesKey("upload_limit_kbps")
        val WIFI_ONLY = booleanPreferencesKey("wifi_only")
        val DHT_ENABLED = booleanPreferencesKey("dht_enabled")
        val PEX_ENABLED = booleanPreferencesKey("pex_enabled")
        val LSD_ENABLED = booleanPreferencesKey("lsd_enabled")
        val MAX_CONNECTIONS = intPreferencesKey("max_connections")
        val STOP_ON_COMPLETE = booleanPreferencesKey("stop_on_complete")
        val TIMER_ACTION = stringPreferencesKey("timer_action")
    }

    val settingsFlow: Flow<TrafficSettings> = context.trafficDataStore.data.map { prefs ->
        val actionName = prefs[Keys.TIMER_ACTION] ?: TimerAction.PAUSE_ALL.name
        val timerAction = runCatching { TimerAction.valueOf(actionName) }
            .getOrDefault(TimerAction.PAUSE_ALL)

        TrafficSettings(
            downloadLimitKbps = prefs[Keys.DOWNLOAD_LIMIT_KBPS] ?: 0,
            uploadLimitKbps = prefs[Keys.UPLOAD_LIMIT_KBPS] ?: 0,
            wifiOnly = prefs[Keys.WIFI_ONLY] ?: false,
            dhtEnabled = prefs[Keys.DHT_ENABLED] ?: true,
            pexEnabled = prefs[Keys.PEX_ENABLED] ?: true,
            lsdEnabled = prefs[Keys.LSD_ENABLED] ?: true,
            maxConnections = prefs[Keys.MAX_CONNECTIONS] ?: 120,
            stopOnComplete = prefs[Keys.STOP_ON_COMPLETE] ?: false,
            timerAction = timerAction
        )
    }

    suspend fun updateDownloadLimit(kbps: Int) {
        context.trafficDataStore.edit { it[Keys.DOWNLOAD_LIMIT_KBPS] = kbps.coerceAtLeast(0) }
    }

    suspend fun updateUploadLimit(kbps: Int) {
        context.trafficDataStore.edit { it[Keys.UPLOAD_LIMIT_KBPS] = kbps.coerceAtLeast(0) }
    }

    suspend fun updateWifiOnly(enabled: Boolean) {
        context.trafficDataStore.edit { it[Keys.WIFI_ONLY] = enabled }
    }

    suspend fun updateDhtEnabled(enabled: Boolean) {
        context.trafficDataStore.edit { it[Keys.DHT_ENABLED] = enabled }
    }

    suspend fun updatePexEnabled(enabled: Boolean) {
        context.trafficDataStore.edit { it[Keys.PEX_ENABLED] = enabled }
    }

    suspend fun updateLsdEnabled(enabled: Boolean) {
        context.trafficDataStore.edit { it[Keys.LSD_ENABLED] = enabled }
    }

    suspend fun updateMaxConnections(count: Int) {
        context.trafficDataStore.edit { it[Keys.MAX_CONNECTIONS] = count.coerceIn(20, 400) }
    }

    suspend fun updateStopOnComplete(enabled: Boolean) {
        context.trafficDataStore.edit { it[Keys.STOP_ON_COMPLETE] = enabled }
    }

    suspend fun updateTimerAction(action: TimerAction) {
        context.trafficDataStore.edit { it[Keys.TIMER_ACTION] = action.name }
    }
}
