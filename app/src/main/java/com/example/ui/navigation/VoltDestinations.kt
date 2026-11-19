package com.example.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
data object DownloadsRoute

@Serializable
data object CatalogRoute

@Serializable
data object ControlsRoute

@Serializable
data class TorrentDetailsRoute(val infoHash: String)
