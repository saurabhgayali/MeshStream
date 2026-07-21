package com.meshstream.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.meshstream.app.ui.MeshStreamNavHost
import com.meshstream.app.ui.theme.MeshStreamTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * The single Activity entry point for MeshStream.
 *
 * Navigation between screens is handled by [MeshStreamNavHost] using
 * Jetpack Compose Navigation. All screens are Composable functions.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MeshStreamTheme {
                MeshStreamNavHost()
            }
        }
    }
}
