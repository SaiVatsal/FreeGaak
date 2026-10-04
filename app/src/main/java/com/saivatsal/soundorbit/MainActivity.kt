package com.saivatsal.soundorbit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.saivatsal.soundorbit.core.datastore.SettingsDataStore
import com.saivatsal.soundorbit.core.datastore.UserSettings
import com.saivatsal.soundorbit.ui.navigation.SoundOrbitApp
import com.saivatsal.soundorbit.ui.theme.SoundOrbitTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsDataStore: SettingsDataStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val settings by settingsDataStore.settings.collectAsState(initial = UserSettings())
            val isOled = settings.activeThemeMode == "OLED_BLACK"

            SoundOrbitTheme(oledBlack = isOled) {
                SoundOrbitApp()
            }
        }
    }
}
