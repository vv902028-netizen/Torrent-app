package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.data.catalog.DemoCatalog
import com.example.domain.model.CatalogCategory
import com.example.domain.model.DownloadFilter
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
import com.example.domain.model.FilePriority
import com.example.torrent.MagnetParser
import com.example.ui.screens.CatalogScreen
import com.example.ui.screens.ControlsScreen
import com.example.ui.screens.DownloadsScreen
import com.example.ui.screens.TorrentDetailsScreen
import com.example.ui.theme.VoltTorrentTheme
import com.example.ui.viewmodel.DownloadsUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class VoltTorrentUiAndCatalogTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun demoCatalogEntries_allHaveValidMagnetLinksAndMatchingHashes() {
        assertTrue(DemoCatalog.entries.isNotEmpty())
        DemoCatalog.entries.forEach { entry ->
            val parsed = MagnetParser.parse(entry.magnetUri)
            assertNotNull("Invalid magnet for ${entry.id}", parsed)
            assertEquals(entry.infoHash.lowercase(), parsed!!.infoHash)
        }
    }

    @Test
    fun downloadsScreen_emptyStateAndCardInteraction_renderCorrectly() {
        var clickedHash = ""
        val sampleTorrent = TorrentUiItem(
            infoHash = "dd8255ecdc7ca55fb0bbf81323d87062db1f6d1c",
            name = "Big Buck Bunny 1080p",
            sourceUri = "magnet:?xt=urn:btih:dd8255ecdc7ca55fb0bbf81323d87062db1f6d1c",
            saveDir = "/downloads/VoltTorrent",
            totalBytes = 289_406_976L,
            downloadedBytes = 144_703_488L,
            uploadedBytes = 12_000_000L,
            progress = 0.5f,
            downloadSpeedBps = 3_200_000L,
            uploadSpeedBps = 250_000L,
            etaSeconds = 45L,
            connectedPeers = 18,
            totalPeers = 40,
            connectedSeeds = 9,
            totalSeeds = 22,
            ratio = 0.08f,
            activeTimeSeconds = 60L,
            state = TorrentState.DOWNLOADING,
            addedAt = 1_000L
        )

        composeRule.setContent {
            VoltTorrentTheme {
                DownloadsScreen(
                    uiState = DownloadsUiState(
                        torrents = listOf(sampleTorrent),
                        filter = DownloadFilter.ALL,
                        globalTelemetry = GlobalTelemetry(
                            totalDownloadBps = 3_200_000L,
                            totalUploadBps = 250_000L,
                            dhtNodes = 142L,
                            activeCount = 1
                        )
                    ),
                    onSelectFilter = {},
                    onSearchQueryChange = {},
                    onOpenTorrentDetails = { clickedHash = it },
                    onTogglePauseResume = {},
                    onDeleteTorrent = { _, _ -> },
                    onPauseAll = {},
                    onResumeAll = {},
                    onOpenAddMagnet = {},
                    onPickTorrentFile = {},
                    onOpenCatalog = {},
                    onOpenControls = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Big Buck Bunny 1080p").assertIsDisplayed()
        composeRule.onNodeWithTag("torrent_card_dd8255ecdc7ca55fb0bbf81323d87062db1f6d1c").performClick()
        assertEquals("dd8255ecdc7ca55fb0bbf81323d87062db1f6d1c", clickedHash)
    }

    @Test
    fun controlsScreen_rendersLimitsAndStartsTimer() {
        var startedMinutes = 0
        composeRule.setContent {
            VoltTorrentTheme {
                ControlsScreen(
                    telemetry = GlobalTelemetry(
                        speedHistory = listOf(
                            SpeedPoint(1000L, 1_000_000L, 100_000L),
                            SpeedPoint(2000L, 2_000_000L, 200_000L)
                        )
                    ),
                    settings = TrafficSettings(downloadLimitKbps = 2048, uploadLimitKbps = 512),
                    timerState = TimerState(isActive = false, action = TimerAction.PAUSE_ALL),
                    downloadDirPath = "/storage/emulated/0/VoltTorrent",
                    onSetDownloadLimit = {},
                    onSetUploadLimit = {},
                    onStartTimer = { startedMinutes = it },
                    onCancelTimer = {},
                    onSetTimerAction = {},
                    onSetStopOnComplete = {},
                    onSetWifiOnly = {},
                    onSetDhtEnabled = {},
                    onSetPexEnabled = {},
                    onSetLsdEnabled = {},
                    onSetMaxConnections = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("timer_preset_60").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("start_timer_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(60, startedMinutes)
    }

    @Test
    fun torrentDetailsScreen_switchesTabs_andShowsPeersAndTrackers() {
        val item = TorrentUiItem(
            infoHash = "dd8255ecdc7ca55fb0bbf81323d87062db1f6d1c",
            name = "Ubuntu 24.04.1 LTS",
            sourceUri = "magnet:?xt=urn:btih:dd8255ecdc7ca55fb0bbf81323d87062db1f6d1c",
            saveDir = "/downloads/VoltTorrent",
            totalBytes = 100_000_000L,
            downloadedBytes = 50_000_000L,
            uploadedBytes = 5_000_000L,
            progress = 0.5f,
            downloadSpeedBps = 1_500_000L,
            uploadSpeedBps = 120_000L,
            etaSeconds = 33L,
            connectedPeers = 1,
            totalPeers = 5,
            connectedSeeds = 1,
            totalSeeds = 5,
            ratio = 0.1f,
            activeTimeSeconds = 120L,
            state = TorrentState.DOWNLOADING,
            addedAt = 1000L,
            files = listOf(TorrentFileItem(0, "ubuntu.iso", "ubuntu.iso", 100_000_000L, 50_000_000L, FilePriority.NORMAL)),
            peers = listOf(PeerStat("192.168.1.50", 51413, "qBittorrent 4.6", "uTP", 1_500_000L, 0L, 1.0f, "uTP")),
            trackers = listOf(TrackerStat("udp://tracker.opentrackr.org:1337/announce", 0, TrackerStatus.WORKING, 42, 12, 0, 120, ""))
        )

        composeRule.setContent {
            VoltTorrentTheme {
                TorrentDetailsScreen(
                    torrent = item,
                    onBack = {},
                    onTogglePauseResume = {},
                    onDeleteTorrent = { _, _ -> },
                    onSetFilePriority = { _, _, _ -> },
                    onForceReannounce = {},
                    onAddTracker = { _, _ -> }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("details_tab_2").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("192.168.1.50:51413").assertIsDisplayed()

        composeRule.onNodeWithTag("details_tab_3").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("udp://tracker.opentrackr.org:1337/announce").assertIsDisplayed()
    }

    @Test
    fun catalogScreen_displaysCatalogEntries() {
        composeRule.setContent {
            VoltTorrentTheme {
                CatalogScreen(
                    entries = DemoCatalog.entries,
                    selectedCategory = CatalogCategory.ALL,
                    existingHashes = emptySet(),
                    onSelectCategory = {},
                    onAddCatalogEntry = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Ubuntu 24.04.1 LTS Desktop").assertIsDisplayed()
    }

    @Test
    fun networkStallWarningBanner_rendersAndTriggersReconnect() {
        var reconnected = false
        composeRule.setContent {
            VoltTorrentTheme {
                DownloadsScreen(
                    uiState = DownloadsUiState(
                        globalTelemetry = GlobalTelemetry(
                            networkHealth = NetworkHealth.TRACKER_STALLED,
                            networkTransportLabel = "Мобильная сеть",
                            networkWarningMessage = "Трекеры не отвечают или подвисли после смены сети. Нажмите «Обновить», чтобы переоткрыть сокеты."
                        )
                    ),
                    onSelectFilter = {},
                    onSearchQueryChange = {},
                    onOpenTorrentDetails = {},
                    onTogglePauseResume = {},
                    onDeleteTorrent = { _, _ -> },
                    onPauseAll = {},
                    onResumeAll = {},
                    onOpenAddMagnet = {},
                    onPickTorrentFile = {},
                    onOpenCatalog = {},
                    onOpenControls = {},
                    onReconnectNetwork = { reconnected = true }
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("network_warning_banner").assertIsDisplayed()
        composeRule.onNodeWithTag("reconnect_network_button").performClick()
        composeRule.waitForIdle()
        assertTrue(reconnected)
    }
}
