package com.saivatsal.soundorbit.core.audio

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import com.saivatsal.soundorbit.core.datastore.SettingsDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

data class EqualizerBand(
    val index: Short,
    val centerFreqHz: Int,
    val minLevelMb: Short,
    val maxLevelMb: Short,
    val currentLevelMb: Short
)

data class EqualizerState(
    val isAvailable: Boolean = false,
    val isEnabled: Boolean = false,
    val numBands: Short = 0,
    val bands: List<EqualizerBand> = emptyList(),
    val presets: List<String> = emptyList(),
    val currentPreset: Short = -1, // -1 means custom
    val bassBoostStrength: Short = 0,
    val virtualizerStrength: Short = 0,
    val isBassBoostSupported: Boolean = false,
    val isVirtualizerSupported: Boolean = false
)

@Singleton
class EqualizerController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsDataStore: SettingsDataStore
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var currentSessionId: Int = 0

    private val _state = MutableStateFlow(EqualizerState())
    val state: StateFlow<EqualizerState> = _state.asStateFlow()

    fun attachAudioSession(audioSessionId: Int) {
        if (audioSessionId == 0 || audioSessionId == currentSessionId) return
        release()
        currentSessionId = audioSessionId

        try {
            val eq = Equalizer(0, audioSessionId)
            val bb = try { BassBoost(0, audioSessionId) } catch (_: Exception) { null }
            val virt = try { Virtualizer(0, audioSessionId) } catch (_: Exception) { null }

            equalizer = eq
            bassBoost = bb
            virtualizer = virt

            val numBands = eq.numberOfBands
            val minEqLevel = eq.bandLevelRange.getOrNull(0) ?: -1500
            val maxEqLevel = eq.bandLevelRange.getOrNull(1) ?: 1500

            val bands = (0 until numBands).map { bandIdx ->
                val sIdx = bandIdx.toShort()
                val centerFreq = eq.getCenterFreq(sIdx) / 1000 // mHz to Hz
                val currentLevel = eq.getBandLevel(sIdx)
                EqualizerBand(
                    index = sIdx,
                    centerFreqHz = centerFreq,
                    minLevelMb = minEqLevel,
                    maxLevelMb = maxEqLevel,
                    currentLevelMb = currentLevel
                )
            }

            val numPresets = eq.numberOfPresets
            val presets = (0 until numPresets).map { presetIdx ->
                try {
                    eq.getPresetName(presetIdx.toShort()) ?: "Preset $presetIdx"
                } catch (_: Exception) {
                    "Preset $presetIdx"
                }
            }

            val currentPreset = try { eq.currentPreset } catch (_: Exception) { (-1).toShort() }
            val bbStrength = bb?.roundedStrength ?: 0
            val virtStrength = virt?.roundedStrength ?: 0

            _state.update {
                EqualizerState(
                    isAvailable = true,
                    isEnabled = eq.enabled,
                    numBands = numBands,
                    bands = bands,
                    presets = presets,
                    currentPreset = currentPreset,
                    bassBoostStrength = bbStrength,
                    virtualizerStrength = virtStrength,
                    isBassBoostSupported = bb?.strengthSupported ?: false,
                    isVirtualizerSupported = virt?.strengthSupported ?: false
                )
            }

            // Restore saved settings
            scope.launch {
                restoreSavedSettings()
            }
        } catch (e: Exception) {
            _state.update { it.copy(isAvailable = false) }
        }
    }

    private suspend fun restoreSavedSettings() {
        val settings = settingsDataStore.settings.first()
        val eq = equalizer ?: return

        try {
            eq.enabled = settings.equalizerEnabled
            bassBoost?.enabled = settings.equalizerEnabled
            virtualizer?.enabled = settings.equalizerEnabled

            if (settings.equalizerPreset >= 0 && settings.equalizerPreset < eq.numberOfPresets) {
                eq.usePreset(settings.equalizerPreset.toShort())
            } else if (settings.equalizerCustomBands.isNotBlank()) {
                val bandLevels = settings.equalizerCustomBands.split(",").mapNotNull { it.toShortOrNull() }
                bandLevels.forEachIndexed { index, level ->
                    if (index < eq.numberOfBands) {
                        eq.setBandLevel(index.toShort(), level)
                    }
                }
            }

            bassBoost?.let {
                if (it.strengthSupported && settings.bassBoostStrength in 0..1000) {
                    it.setStrength(settings.bassBoostStrength.toShort())
                }
            }

            virtualizer?.let {
                if (it.strengthSupported && settings.virtualizerStrength in 0..1000) {
                    it.setStrength(settings.virtualizerStrength.toShort())
                }
            }

            refreshState()
        } catch (_: Exception) {}
    }

    fun setEnabled(enabled: Boolean) {
        val eq = equalizer ?: return
        try {
            eq.enabled = enabled
            bassBoost?.enabled = enabled
            virtualizer?.enabled = enabled
            _state.update { it.copy(isEnabled = enabled) }
            scope.launch { settingsDataStore.setEqualizerEnabled(enabled) }
        } catch (_: Exception) {}
    }

    fun setBandLevel(bandIndex: Short, levelMb: Short) {
        val eq = equalizer ?: return
        try {
            eq.setBandLevel(bandIndex, levelMb)
            refreshState()
            scope.launch {
                val updatedBands = _state.value.bands.joinToString(",") { it.currentLevelMb.toString() }
                settingsDataStore.setEqualizerCustomBands(updatedBands)
                settingsDataStore.setEqualizerPreset(-1)
            }
        } catch (_: Exception) {}
    }

    fun usePreset(presetIndex: Short) {
        val eq = equalizer ?: return
        try {
            if (presetIndex in 0 until eq.numberOfPresets) {
                eq.usePreset(presetIndex)
                refreshState()
                scope.launch {
                    settingsDataStore.setEqualizerPreset(presetIndex.toInt())
                }
            }
        } catch (_: Exception) {}
    }

    fun setBassBoostStrength(strength: Short) {
        val bb = bassBoost ?: return
        try {
            if (bb.strengthSupported) {
                bb.setStrength(strength)
                _state.update { it.copy(bassBoostStrength = strength) }
                scope.launch { settingsDataStore.setBassBoostStrength(strength.toInt()) }
            }
        } catch (_: Exception) {}
    }

    fun setVirtualizerStrength(strength: Short) {
        val virt = virtualizer ?: return
        try {
            if (virt.strengthSupported) {
                virt.setStrength(strength)
                _state.update { it.copy(virtualizerStrength = strength) }
                scope.launch { settingsDataStore.setVirtualizerStrength(strength.toInt()) }
            }
        } catch (_: Exception) {}
    }

    private fun refreshState() {
        val eq = equalizer ?: return
        val numBands = eq.numberOfBands
        val minEqLevel = eq.bandLevelRange.getOrNull(0) ?: -1500
        val maxEqLevel = eq.bandLevelRange.getOrNull(1) ?: 1500

        val bands = (0 until numBands).map { bandIdx ->
            val sIdx = bandIdx.toShort()
            EqualizerBand(
                index = sIdx,
                centerFreqHz = eq.getCenterFreq(sIdx) / 1000,
                minLevelMb = minEqLevel,
                maxLevelMb = maxEqLevel,
                currentLevelMb = eq.getBandLevel(sIdx)
            )
        }
        val currentPreset = try { eq.currentPreset } catch (_: Exception) { (-1).toShort() }

        _state.update {
            it.copy(
                isEnabled = eq.enabled,
                bands = bands,
                currentPreset = currentPreset,
                bassBoostStrength = bassBoost?.roundedStrength ?: 0,
                virtualizerStrength = virtualizer?.roundedStrength ?: 0
            )
        }
    }

    fun release() {
        try {
            equalizer?.release()
            bassBoost?.release()
            virtualizer?.release()
        } catch (_: Exception) {}
        equalizer = null
        bassBoost = null
        virtualizer = null
        currentSessionId = 0
        _state.update { EqualizerState() }
    }
}
