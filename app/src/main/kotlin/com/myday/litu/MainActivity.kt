package com.myday.litu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.viewModels
import com.myday.litu.core.designsystem.theme.LituTheme
import com.myday.litu.core.designsystem.theme.isDarkTheme
import com.myday.litu.navigation.LituApp
import dagger.hilt.android.AndroidEntryPoint

/** Single activity (spec section 6). */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        // S01 lasts under a second: keep the system splash only until settings are read.
        splash.setKeepOnScreenCondition { viewModel.state.value.loading }
        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()
            val dark = isDarkTheme(state.theme)
            LaunchedEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(LIGHT_SCRIM, DARK_SCRIM) { dark },
                )
            }
            // Theme changes apply instantly without restarting or losing the current screen.
            LituTheme(state.theme) {
                if (!state.loading) LituApp(state)
            }
        }
    }

    private companion object {
        val LIGHT_SCRIM = android.graphics.Color.argb(0xe6, 0xFB, 0xF8, 0xF2)
        val DARK_SCRIM = android.graphics.Color.argb(0x80, 0x10, 0x14, 0x14)
    }
}
