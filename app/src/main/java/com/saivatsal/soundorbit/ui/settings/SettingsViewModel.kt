package com.saivatsal.soundorbit.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saivatsal.soundorbit.core.backup.BackupImportSummary
import com.saivatsal.soundorbit.core.backup.BackupManager
import com.saivatsal.soundorbit.core.database.SoundOrbitDatabase
import com.saivatsal.soundorbit.core.datastore.SettingsDataStore
import com.saivatsal.soundorbit.core.datastore.UserSettings
import com.saivatsal.soundorbit.core.model.AudioQuality
import com.saivatsal.soundorbit.core.model.SourceId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import javax.inject.Inject

sealed interface BackupUiState {
    object Idle : BackupUiState
    object Loading : BackupUiState
    data class ExportReady(val jsonString: String) : BackupUiState
    data class ImportSuccess(val summary: BackupImportSummary) : BackupUiState
    data class Error(val message: String) : BackupUiState
}

data class DatabaseStats(
    val playlistCount: Int = 0,
    val favoriteCount: Int = 0
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val backupManager: BackupManager,
    private val database: SoundOrbitDatabase
) : ViewModel() {

    val settings: StateFlow<UserSettings> = settingsDataStore.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())

    private val _cacheSizeFormatted = MutableStateFlow("Calculating...")
    val cacheSizeFormatted: StateFlow<String> = _cacheSizeFormatted.asStateFlow()

    private val _backupState = MutableStateFlow<BackupUiState>(BackupUiState.Idle)
    val backupState: StateFlow<BackupUiState> = _backupState.asStateFlow()

    val databaseStats: StateFlow<DatabaseStats> = combine(
        database.playlistDao().getAllPlaylists(),
        database.favoriteDao().getFavoriteCount()
    ) { playlists, favCount ->
        DatabaseStats(
            playlistCount = playlists.size,
            favoriteCount = favCount
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DatabaseStats())

    init {
        refreshCacheSize()
    }

    fun setAudioQuality(quality: AudioQuality) {
        viewModelScope.launch {
            settingsDataStore.setAudioQuality(quality)
        }
    }

    fun setCrossfadeDuration(seconds: Int) {
        viewModelScope.launch {
            settingsDataStore.setCrossfadeDuration(seconds)
        }
    }

    fun setLoudnessNormalization(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setLoudnessNormalization(enabled)
        }
    }

    fun setOfflineMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setOfflineMode(enabled)
        }
    }

    fun toggleSource(sourceId: SourceId) {
        viewModelScope.launch {
            val currentSources = settings.value.enabledSources.toMutableSet()
            if (currentSources.contains(sourceId)) {
                if (currentSources.size > 1) { // Keep at least one source
                    currentSources.remove(sourceId)
                }
            } else {
                currentSources.add(sourceId)
            }
            settingsDataStore.setEnabledSources(currentSources)
        }
    }

    fun setThemeMode(themeMode: String) {
        viewModelScope.launch {
            settingsDataStore.setThemeMode(themeMode)
        }
    }

    fun refreshCacheSize() {
        viewModelScope.launch {
            val bytes = backupManager.getCacheSizeBytes()
            _cacheSizeFormatted.value = formatBytes(bytes)
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            backupManager.clearCache()
            refreshCacheSize()
        }
    }

    fun exportBackup() {
        viewModelScope.launch {
            _backupState.value = BackupUiState.Loading
            try {
                val json = backupManager.exportBackupJson()
                _backupState.value = BackupUiState.ExportReady(json)
            } catch (e: Exception) {
                _backupState.value = BackupUiState.Error(e.message ?: "Failed to generate backup JSON")
            }
        }
    }

    fun importBackup(jsonString: String) {
        viewModelScope.launch {
            _backupState.value = BackupUiState.Loading
            backupManager.importBackupFromJson(jsonString)
                .onSuccess { summary ->
                    _backupState.value = BackupUiState.ImportSuccess(summary)
                }
                .onFailure { error ->
                    _backupState.value = BackupUiState.Error(error.message ?: "Invalid backup file")
                }
        }
    }

    fun resetBackupState() {
        _backupState.value = BackupUiState.Idle
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val df = DecimalFormat("#,##0.#")
        return "${df.format(bytes / Math.pow(1024.0, digitGroups.toDouble()))} ${units[digitGroups]}"
    }
}
