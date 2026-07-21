package com.meshstream.app.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.meshstream.app.ui.screen.RecordingScreen
import com.meshstream.app.ui.screen.PeerStatusScreen
import com.meshstream.app.ui.screen.SettingsScreen

/** Top-level navigation destinations within MeshStream. */
internal object Destinations {
    const val RECORDING = "recording"
    const val PEER_STATUS = "peer_status"
    const val SETTINGS = "settings"
}

/**
 * Root [NavHost] for MeshStream.
 *
 * The recording screen is the start destination. Users can navigate to the peer
 * status screen and settings from there.
 */
@Composable
fun MeshStreamNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Destinations.RECORDING,
    ) {
        composable(Destinations.RECORDING) {
            RecordingScreen(
                onNavigateToPeerStatus = { navController.navigate(Destinations.PEER_STATUS) },
                onNavigateToSettings = { navController.navigate(Destinations.SETTINGS) },
            )
        }
        composable(Destinations.PEER_STATUS) {
            PeerStatusScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
        composable(Destinations.SETTINGS) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }
    }
}
