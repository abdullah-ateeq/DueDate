package com.example.duedate

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.duedate.data.SharedPreferencesStore
import com.example.duedate.model.ThemeMode
import com.example.duedate.repository.DueDateHistoryRepository
import com.example.duedate.repository.SettingsRepository
import com.example.duedate.ui.DueDateApp
import com.example.duedate.ui.DueDateViewModel
import com.example.duedate.ui.theme.DueDateTheme

class MainActivity : ComponentActivity() {

    private val viewModel: DueDateViewModel by viewModels {
        DueDateViewModel.Factory(
            historyRepository = DueDateHistoryRepository(SharedPreferencesStore(this, "duedate_history")),
            settingsRepository = SettingsRepository(SharedPreferencesStore(this, "duedate_settings"))
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val darkTheme = when (state.settings.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            // Keep system bar icon contrast in sync with the in-app theme choice.
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(LightScrim, DarkScrim) { darkTheme }
                )
                onDispose {}
            }

            DueDateTheme(darkTheme = darkTheme, dynamicColor = state.settings.dynamicColor) {
                DueDateApp(viewModel = viewModel, state = state)
            }
        }
    }

    private companion object {
        val LightScrim = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
        val DarkScrim = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
    }
}
