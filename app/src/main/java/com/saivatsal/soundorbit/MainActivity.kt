package com.saivatsal.soundorbit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.saivatsal.soundorbit.core.datastore.SettingsDataStore
import com.saivatsal.soundorbit.core.datastore.UserSettings
import com.saivatsal.soundorbit.ui.navigation.SoundOrbitApp
import com.saivatsal.soundorbit.ui.splash.SplashScreen
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
            var showSplash by remember { mutableStateOf(true) }

            SoundOrbitTheme(oledBlack = isOled) {
                Crossfade(
                    targetState = showSplash,
                    animationSpec = tween(durationMillis = 450),
                    label = "SplashCrossfade"
                ) { isSplashing ->
                    if (isSplashing) {
                        SplashScreen(
                            onSplashFinished = { showSplash = false }
                        )
                    } else {
                        SoundOrbitApp()
                    }
                }
            }
        }
    }
}
