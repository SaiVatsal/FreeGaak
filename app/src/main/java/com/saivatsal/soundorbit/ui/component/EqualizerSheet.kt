package com.saivatsal.soundorbit.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.saivatsal.soundorbit.core.audio.EqualizerState
import com.saivatsal.soundorbit.ui.theme.CosmicTeal
import com.saivatsal.soundorbit.ui.theme.DarkSurface
import com.saivatsal.soundorbit.ui.theme.DarkSurfaceVariant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerSheet(
    state: EqualizerState,
    onDismiss: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onSelectPreset: (Short) -> Unit,
    onBandChange: (Short, Short) -> Unit,
    onBassBoostChange: (Short) -> Unit,
    onVirtualizerChange: (Short) -> Unit
) {
    var presetMenuExpanded by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(scrollState)
        ) {
            // Header with Master Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Equalizer,
                        contentDescription = null,
                        tint = CosmicTeal,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Equalizer & Effects",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                Switch(
                    checked = state.isEnabled,
                    onCheckedChange = onToggleEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = CosmicTeal
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!state.isAvailable) {
                Text(
                    text = "Hardware audio equalizer is not supported on this audio output session.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                // Preset Dropdown
                if (state.presets.isNotEmpty()) {
                    val currentPresetName = if (state.currentPreset >= 0 && state.currentPreset < state.presets.size) {
                        state.presets[state.currentPreset.toInt()]
                    } else {
                        "Custom"
                    }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { if (state.isEnabled) presetMenuExpanded = true },
                            enabled = state.isEnabled,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Preset: $currentPresetName", fontWeight = FontWeight.SemiBold)
                        }

                        DropdownMenu(
                            expanded = presetMenuExpanded,
                            onDismissRequest = { presetMenuExpanded = false }
                        ) {
                            state.presets.forEachIndexed { index, name ->
                                DropdownMenuItem(
                                    text = { Text(name) },
                                    onClick = {
                                        presetMenuExpanded = false
                                        onSelectPreset(index.toShort())
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Band Sliders
                Text(
                    text = "Frequency Bands",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                state.bands.forEach { band ->
                    val minLevel = band.minLevelMb.toFloat()
                    val maxLevel = band.maxLevelMb.toFloat()
                    val currentLevel = band.currentLevelMb.toFloat()
                    val freqLabel = formatFreq(band.centerFreqHz)
                    val dbLabel = String.format("%.1f dB", band.currentLevelMb / 100f)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = freqLabel,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = dbLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (band.currentLevelMb != 0.toShort()) CosmicTeal else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Slider(
                            value = currentLevel,
                            onValueChange = { newLevel ->
                                onBandChange(band.index, newLevel.toInt().toShort())
                            },
                            valueRange = minLevel..maxLevel,
                            enabled = state.isEnabled,
                            colors = SliderDefaults.colors(
                                thumbColor = CosmicTeal,
                                activeTrackColor = CosmicTeal,
                                inactiveTrackColor = DarkSurfaceVariant
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bass Boost
                if (state.isBassBoostSupported) {
                    Text(
                        text = "Bass Boost (${(state.bassBoostStrength / 10)}%)",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = state.bassBoostStrength.toFloat(),
                        onValueChange = { onBassBoostChange(it.toInt().toShort()) },
                        valueRange = 0f..1000f,
                        enabled = state.isEnabled,
                        colors = SliderDefaults.colors(
                            thumbColor = CosmicTeal,
                            activeTrackColor = CosmicTeal,
                            inactiveTrackColor = DarkSurfaceVariant
                        )
                    )
                }

                // Virtualizer
                if (state.isVirtualizerSupported) {
                    Text(
                        text = "Virtualizer / Surround (${(state.virtualizerStrength / 10)}%)",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = state.virtualizerStrength.toFloat(),
                        onValueChange = { onVirtualizerChange(it.toInt().toShort()) },
                        valueRange = 0f..1000f,
                        enabled = state.isEnabled,
                        colors = SliderDefaults.colors(
                            thumbColor = CosmicTeal,
                            activeTrackColor = CosmicTeal,
                            inactiveTrackColor = DarkSurfaceVariant
                        )
                    )
                }
            }
        }
    }
}

private fun formatFreq(freqHz: Int): String {
    return if (freqHz >= 1000) {
        val kHz = freqHz / 1000f
        if (kHz % 1 == 0f) "${kHz.toInt()} kHz" else String.format("%.1f kHz", kHz)
    } else {
        "$freqHz Hz"
    }
}
