package com.saivatsal.soundorbit.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saivatsal.soundorbit.BuildConfig
import com.saivatsal.soundorbit.core.model.AudioQuality
import com.saivatsal.soundorbit.core.model.SourceId
import com.saivatsal.soundorbit.ui.theme.CosmicTeal
import com.saivatsal.soundorbit.ui.theme.DarkSurface
import com.saivatsal.soundorbit.ui.theme.DarkSurfaceVariant
import com.saivatsal.soundorbit.ui.theme.OledBlack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onOpenEqualizer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val cacheSize by viewModel.cacheSizeFormatted.collectAsState()
    val databaseStats by viewModel.databaseStats.collectAsState()
    val backupState by viewModel.backupState.collectAsState()

    var showQualityDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }
    var showClearCacheConfirm by remember { mutableStateOf(false) }

    // Handle backup state events
    LaunchedEffect(backupState) {
        when (val state = backupState) {
            is BackupUiState.ExportReady -> {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("SoundOrbit_Backup.json", state.jsonString)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "Backup JSON copied to clipboard!", Toast.LENGTH_LONG).show()
                viewModel.resetBackupState()
            }
            is BackupUiState.ImportSuccess -> {
                Toast.makeText(
                    context,
                    "Imported ${state.summary.playlistsImported} playlists & ${state.summary.favoritesImported} favorites successfully!",
                    Toast.LENGTH_LONG
                ).show()
                viewModel.resetBackupState()
            }
            is BackupUiState.Error -> {
                Toast.makeText(context, "Error: ${state.message}", Toast.LENGTH_LONG).show()
                viewModel.resetBackupState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = OledBlack
                )
            )
        },
        containerColor = OledBlack,
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(OledBlack)
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp)
        ) {
            // Audio & Playback Section
            item {
                SettingsSectionHeader(title = "Audio & Playback", icon = Icons.Default.Audiotrack)
            }

            item {
                Surface(
                    color = DarkSurface,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Streaming Audio Quality
                        SettingsClickableRow(
                            title = "Streaming Audio Quality",
                            subtitle = when (settings.audioQuality) {
                                AudioQuality.LOW -> "Low (96 kbps - Data Saver)"
                                AudioQuality.NORMAL -> "Normal (160 kbps - Standard)"
                                AudioQuality.HIGH -> "High (320 kbps - Studio Quality)"
                                AudioQuality.AUTO -> "Auto (Adaptive)"
                            },
                            icon = Icons.Default.GraphicEq,
                            onClick = { showQualityDialog = true }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = DarkSurfaceVariant
                        )

                        // Crossfade Duration Slider
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = null,
                                        tint = CosmicTeal,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Crossfade Duration",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White
                                    )
                                }
                                Text(
                                    text = if (settings.crossfadeDurationSec == 0) "Off" else "${settings.crossfadeDurationSec}s",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = CosmicTeal
                                )
                            }
                            Slider(
                                value = settings.crossfadeDurationSec.toFloat(),
                                onValueChange = { viewModel.setCrossfadeDuration(it.toInt()) },
                                valueRange = 0f..12f,
                                steps = 11,
                                colors = SliderDefaults.colors(
                                    thumbColor = CosmicTeal,
                                    activeTrackColor = CosmicTeal,
                                    inactiveTrackColor = DarkSurfaceVariant
                                ),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = DarkSurfaceVariant
                        )

                        // Loudness Normalization
                        SettingsToggleRow(
                            title = "Loudness Normalization",
                            subtitle = "Equalize volume across different audio sources",
                            icon = Icons.Default.VolumeUp,
                            checked = settings.loudnessNormalization,
                            onCheckedChange = { viewModel.setLoudnessNormalization(it) }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = DarkSurfaceVariant
                        )

                        // Hardware Audio Equalizer Shortcut
                        SettingsClickableRow(
                            title = "Hardware Audio Equalizer",
                            subtitle = "Multi-band frequency tuning, bass boost & virtualizer",
                            icon = Icons.Default.Equalizer,
                            onClick = onOpenEqualizer
                        )
                    }
                }
            }

            // Theme & Appearance Section
            item {
                SettingsSectionHeader(title = "Appearance", icon = Icons.Default.Palette)
            }

            item {
                Surface(
                    color = DarkSurface,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SettingsToggleRow(
                            title = "OLED Pure Black Mode",
                            subtitle = "True #000000 background for battery savings on OLED screens",
                            icon = Icons.Default.DarkMode,
                            checked = settings.activeThemeMode == "OLED_BLACK",
                            onCheckedChange = { isOled ->
                                viewModel.setThemeMode(if (isOled) "OLED_BLACK" else "SYSTEM")
                            }
                        )
                    }
                }
            }

            // Music Ecosystem & Sources
            item {
                SettingsSectionHeader(title = "Music Sources & Offline", icon = Icons.Default.CloudSync)
            }

            item {
                Surface(
                    color = DarkSurface,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Offline Mode Switch
                        SettingsToggleRow(
                            title = "Offline-Only Mode",
                            subtitle = "Play only downloaded and cached tracks",
                            icon = Icons.Default.CloudOff,
                            checked = settings.offlineMode,
                            onCheckedChange = { viewModel.setOfflineMode(it) }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = DarkSurfaceVariant
                        )

                        Text(
                            text = "Enabled Sources",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = CosmicTeal,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // Audius Source
                        SettingsToggleRow(
                            title = "Audius Music",
                            subtitle = "Decentralized streaming audio ecosystem",
                            icon = Icons.Default.Radio,
                            checked = settings.enabledSources.contains(SourceId.AUDIUS),
                            onCheckedChange = { viewModel.toggleSource(SourceId.AUDIUS) }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = DarkSurfaceVariant
                        )

                        // Jamendo Source
                        SettingsToggleRow(
                            title = "Jamendo Music",
                            subtitle = "Independent, Creative Commons licensed music",
                            icon = Icons.Default.MusicNote,
                            checked = settings.enabledSources.contains(SourceId.JAMENDO),
                            onCheckedChange = { viewModel.toggleSource(SourceId.JAMENDO) }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = DarkSurfaceVariant
                        )

                        // Deezer Source
                        SettingsToggleRow(
                            title = "Deezer Music",
                            subtitle = "Stream music previews and discover tracks via RapidAPI Deezer",
                            icon = Icons.Default.Album,
                            checked = settings.enabledSources.contains(SourceId.DEEZER),
                            onCheckedChange = { viewModel.toggleSource(SourceId.DEEZER) }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = DarkSurfaceVariant
                        )

                        // Local Files Source
                        SettingsToggleRow(
                            title = "Local Device Media",
                            subtitle = "Scan device storage for MP3, FLAC, WAV, and AAC",
                            icon = Icons.Default.Folder,
                            checked = settings.enabledSources.contains(SourceId.LOCAL),
                            onCheckedChange = { viewModel.toggleSource(SourceId.LOCAL) }
                        )
                    }
                }
            }

            // Data & Storage Management
            item {
                SettingsSectionHeader(title = "Data & Storage", icon = Icons.Default.Storage)
            }

            item {
                Surface(
                    color = DarkSurface,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Cached Audio & Images",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White
                                )
                                Text(
                                    text = "Current storage usage: $cacheSize",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                            OutlinedButton(
                                onClick = { showClearCacheConfirm = true },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = CosmicTeal
                                ),
                                border = ButtonDefaults.outlinedButtonBorder.copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(CosmicTeal.copy(alpha = 0.5f))
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Clear")
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = DarkSurfaceVariant
                        )

                        // Export Backup
                        SettingsClickableRow(
                            title = "Export Backup (JSON)",
                            subtitle = "Export ${databaseStats.playlistCount} playlists and ${databaseStats.favoriteCount} favorites",
                            icon = Icons.Default.FileDownload,
                            onClick = { viewModel.exportBackup() }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = DarkSurfaceVariant
                        )

                        // Import Backup
                        SettingsClickableRow(
                            title = "Import Backup (JSON)",
                            subtitle = "Restore playlists and settings from JSON",
                            icon = Icons.Default.FileUpload,
                            onClick = { showImportDialog = true }
                        )
                    }
                }
            }

            // About & Privacy
            item {
                SettingsSectionHeader(title = "About SoundOrbit", icon = Icons.Default.Info)
            }

            item {
                Surface(
                    color = DarkSurface,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SoundOrbit v${BuildConfig.VERSION_NAME}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "by SaiVatsal",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = CosmicTeal
                            )
                        }
                        Text(
                            text = "Developed by SaiVatsal • An offline-first, ad-free, 100% private music player built for open audio ecosystems.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.LightGray
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            PrivacyBadge(label = "Zero Tracking")
                            PrivacyBadge(label = "Zero Ads")
                            PrivacyBadge(label = "100% Private")
                            PrivacyBadge(label = "Local-Only")
                        }
                    }
                }
            }
        }
    }

    // Audio Quality Dialog
    if (showQualityDialog) {
        AlertDialog(
            onDismissRequest = { showQualityDialog = false },
            title = { Text("Audio Streaming Quality", color = Color.White) },
            containerColor = DarkSurface,
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AudioQuality.values().forEach { quality ->
                        val isSelected = settings.audioQuality == quality
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    viewModel.setAudioQuality(quality)
                                    showQualityDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    viewModel.setAudioQuality(quality)
                                    showQualityDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = CosmicTeal)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = when (quality) {
                                        AudioQuality.LOW -> "Low (96 kbps)"
                                        AudioQuality.NORMAL -> "Normal (160 kbps)"
                                        AudioQuality.HIGH -> "High (320 kbps)"
                                        AudioQuality.AUTO -> "Auto (Adaptive)"
                                    },
                                    color = if (isSelected) CosmicTeal else Color.White,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                Text(
                                    text = when (quality) {
                                        AudioQuality.LOW -> "Optimized for minimal data consumption"
                                        AudioQuality.NORMAL -> "Balanced quality and network speed"
                                        AudioQuality.HIGH -> "Best audio clarity and stereo fidelity"
                                        AudioQuality.AUTO -> "Dynamically adjusts to network conditions"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQualityDialog = false }) {
                    Text("Close", color = CosmicTeal)
                }
            }
        )
    }

    // Import JSON Dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import JSON Backup", color = Color.White) },
            containerColor = DarkSurface,
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Paste your exported SoundOrbit JSON backup below:",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray
                    )
                    OutlinedTextField(
                        value = importText,
                        onValueChange = { importText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        placeholder = { Text("{\"version\": 1, \"playlists\": [...]}") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CosmicTeal,
                            unfocusedBorderColor = DarkSurfaceVariant,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.LightGray
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (importText.isNotBlank()) {
                            viewModel.importBackup(importText)
                            showImportDialog = false
                            importText = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CosmicTeal)
                ) {
                    Text("Restore", color = OledBlack, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }

    // Clear Cache Confirmation Dialog
    if (showClearCacheConfirm) {
        AlertDialog(
            onDismissRequest = { showClearCacheConfirm = false },
            title = { Text("Clear Audio & Image Cache?", color = Color.White) },
            text = {
                Text(
                    "This will delete temporarily cached album art and audio files ($cacheSize). Your saved playlists and favorites will remain untouched.",
                    color = Color.LightGray
                )
            },
            containerColor = DarkSurface,
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearCache()
                        showClearCacheConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f))
                ) {
                    Text("Clear Cache", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheConfirm = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = CosmicTeal,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = CosmicTeal
        )
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (checked) CosmicTeal else Color.Gray,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = CosmicTeal,
                checkedTrackColor = CosmicTeal.copy(alpha = 0.35f),
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = DarkSurfaceVariant
            )
        )
    }
}

@Composable
private fun SettingsClickableRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = CosmicTeal,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun PrivacyBadge(label: String) {
    Surface(
        color = DarkSurfaceVariant,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.border(0.5.dp, CosmicTeal.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = CosmicTeal,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
