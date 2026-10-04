package com.saivatsal.soundorbit.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.saivatsal.soundorbit.core.model.AudioQuality
import com.saivatsal.soundorbit.core.model.SourceId
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "soundorbit_settings")

data class UserSettings(
    val audioQuality: AudioQuality = AudioQuality.HIGH,
    val crossfadeDurationSec: Int = 0,
    val loudnessNormalization: Boolean = false,
    val offlineMode: Boolean = false,
    val enabledSources: Set<SourceId> = setOf(SourceId.AUDIUS, SourceId.JAMENDO, SourceId.DEEZER, SourceId.LOCAL),
    val equalizerEnabled: Boolean = false,
    val equalizerPreset: Int = 0,
    val equalizerCustomBands: String = "",
    val bassBoostStrength: Int = 0,
    val virtualizerStrength: Int = 0,
    val activeThemeMode: String = "SYSTEM" // SYSTEM, DARK, OLED_BLACK
)

@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val AUDIO_QUALITY = stringPreferencesKey("audio_quality")
        val CROSSFADE_DURATION = intPreferencesKey("crossfade_duration_sec")
        val LOUDNESS_NORMALIZATION = booleanPreferencesKey("loudness_normalization")
        val OFFLINE_MODE = booleanPreferencesKey("offline_mode")
        val ENABLED_SOURCES = stringPreferencesKey("enabled_sources")
        val EQUALIZER_ENABLED = booleanPreferencesKey("equalizer_enabled")
        val EQUALIZER_PRESET = intPreferencesKey("equalizer_preset")
        val EQUALIZER_CUSTOM_BANDS = stringPreferencesKey("equalizer_custom_bands")
        val BASS_BOOST_STRENGTH = intPreferencesKey("bass_boost_strength")
        val VIRTUALIZER_STRENGTH = intPreferencesKey("virtualizer_strength")
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }

    val settings: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        val qualityStr = prefs[Keys.AUDIO_QUALITY] ?: AudioQuality.HIGH.name
        val audioQuality = try { AudioQuality.valueOf(qualityStr) } catch (_: Exception) { AudioQuality.HIGH }

        val sourcesStr = prefs[Keys.ENABLED_SOURCES]
        val enabledSources = if (sourcesStr != null) {
            sourcesStr.split(",")
                .mapNotNull { name -> try { SourceId.valueOf(name) } catch (_: Exception) { null } }
                .toSet()
        } else {
            setOf(SourceId.AUDIUS, SourceId.JAMENDO, SourceId.DEEZER, SourceId.LOCAL)
        }

        UserSettings(
            audioQuality = audioQuality,
            crossfadeDurationSec = prefs[Keys.CROSSFADE_DURATION] ?: 0,
            loudnessNormalization = prefs[Keys.LOUDNESS_NORMALIZATION] ?: false,
            offlineMode = prefs[Keys.OFFLINE_MODE] ?: false,
            enabledSources = enabledSources,
            equalizerEnabled = prefs[Keys.EQUALIZER_ENABLED] ?: false,
            equalizerPreset = prefs[Keys.EQUALIZER_PRESET] ?: 0,
            equalizerCustomBands = prefs[Keys.EQUALIZER_CUSTOM_BANDS] ?: "",
            bassBoostStrength = prefs[Keys.BASS_BOOST_STRENGTH] ?: 0,
            virtualizerStrength = prefs[Keys.VIRTUALIZER_STRENGTH] ?: 0,
            activeThemeMode = prefs[Keys.THEME_MODE] ?: "SYSTEM"
        )
    }

    suspend fun setAudioQuality(quality: AudioQuality) {
        context.dataStore.edit { it[Keys.AUDIO_QUALITY] = quality.name }
    }

    suspend fun setCrossfadeDuration(seconds: Int) {
        context.dataStore.edit { it[Keys.CROSSFADE_DURATION] = seconds }
    }

    suspend fun setLoudnessNormalization(enabled: Boolean) {
        context.dataStore.edit { it[Keys.LOUDNESS_NORMALIZATION] = enabled }
    }

    suspend fun setOfflineMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.OFFLINE_MODE] = enabled }
    }

    suspend fun setEnabledSources(sources: Set<SourceId>) {
        context.dataStore.edit { it[Keys.ENABLED_SOURCES] = sources.joinToString(",") { s -> s.name } }
    }

    suspend fun setEqualizerEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.EQUALIZER_ENABLED] = enabled }
    }

    suspend fun setEqualizerPreset(presetIndex: Int) {
        context.dataStore.edit { it[Keys.EQUALIZER_PRESET] = presetIndex }
    }

    suspend fun setEqualizerCustomBands(bands: String) {
        context.dataStore.edit { it[Keys.EQUALIZER_CUSTOM_BANDS] = bands }
    }

    suspend fun setBassBoostStrength(strength: Int) {
        context.dataStore.edit { it[Keys.BASS_BOOST_STRENGTH] = strength }
    }

    suspend fun setVirtualizerStrength(strength: Int) {
        context.dataStore.edit { it[Keys.VIRTUALIZER_STRENGTH] = strength }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode }
    }
}
