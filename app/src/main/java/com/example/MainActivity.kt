package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.navigation.VoltTorrentRoot
import com.example.ui.theme.VoltTorrentTheme
import com.example.ui.viewmodel.MainTorrentViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainTorrentViewModel by viewModels { MainTorrentViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        handleIncomingIntent(intent)
        setContent {
            VoltTorrentTheme {
                VoltTorrentRoot(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        val dataUri = intent?.data ?: return
        when (dataUri.scheme?.lowercase()) {
            "magnet" -> viewModel.handleIncomingMagnet(dataUri.toString())
            "content", "file" -> viewModel.handleIncomingTorrentUri(dataUri)
        }
    }
}
