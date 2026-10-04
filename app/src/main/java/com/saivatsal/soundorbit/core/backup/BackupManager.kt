package com.saivatsal.soundorbit.core.backup

import android.content.Context
import androidx.media3.common.util.UnstableApi
import com.saivatsal.soundorbit.core.database.SoundOrbitDatabase
import com.saivatsal.soundorbit.core.database.entity.FavoriteEntity
import com.saivatsal.soundorbit.core.database.entity.PlaylistEntity
import com.saivatsal.soundorbit.core.database.entity.PlaylistTrackCrossRef
import com.saivatsal.soundorbit.core.database.entity.TrackEntity
import com.saivatsal.soundorbit.core.datastore.SettingsDataStore
import com.saivatsal.soundorbit.core.datastore.UserSettings
import com.saivatsal.soundorbit.core.model.AudioQuality
import com.saivatsal.soundorbit.core.model.SourceId
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

data class BackupImportSummary(
    val playlistsImported: Int,
    val tracksImported: Int,
    val favoritesImported: Int,
    val settingsRestored: Boolean
)

@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: SoundOrbitDatabase,
    private val settingsDataStore: SettingsDataStore
) {

    suspend fun exportBackupJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("version", 1)
        root.put("app", "SoundOrbit")
        root.put("exportedAt", System.currentTimeMillis())

        // 1. Settings
        val userSettings = settingsDataStore.settings.first()
        val settingsObj = JSONObject().apply {
            put("audioQuality", userSettings.audioQuality.name)
            put("crossfadeDurationSec", userSettings.crossfadeDurationSec)
            put("loudnessNormalization", userSettings.loudnessNormalization)
            put("offlineMode", userSettings.offlineMode)
            put("enabledSources", JSONArray(userSettings.enabledSources.map { it.name }))
            put("equalizerEnabled", userSettings.equalizerEnabled)
            put("equalizerPreset", userSettings.equalizerPreset)
            put("equalizerCustomBands", userSettings.equalizerCustomBands)
            put("bassBoostStrength", userSettings.bassBoostStrength)
            put("virtualizerStrength", userSettings.virtualizerStrength)
            put("activeThemeMode", userSettings.activeThemeMode)
        }
        root.put("settings", settingsObj)

        // 2. Playlists with Tracks
        val playlists = database.playlistDao().getAllPlaylists().first()
        val playlistsArray = JSONArray()
        for (pl in playlists) {
            val plObj = JSONObject().apply {
                put("id", pl.id)
                put("name", pl.name)
                put("description", pl.description ?: "")
                put("artworkUrl", pl.coverArtworkUri ?: "")
                put("createdAt", pl.createdAt)
                put("updatedAt", pl.updatedAt)
            }
            val tracks = database.playlistDao().getTracksForPlaylist(pl.id).first()
            val tracksArray = JSONArray()
            for (t in tracks) {
                tracksArray.put(trackEntityToJson(t))
            }
            plObj.put("tracks", tracksArray)
            playlistsArray.put(plObj)
        }
        root.put("playlists", playlistsArray)

        // 3. Favorites with Tracks
        val favorites = database.favoriteDao().getFavoriteTracks().first()
        val favoritesArray = JSONArray()
        for (favTrack in favorites) {
            favoritesArray.put(trackEntityToJson(favTrack))
        }
        root.put("favorites", favoritesArray)

        root.toString(2)
    }

    suspend fun exportBackupToFile(outputStream: OutputStream) = withContext(Dispatchers.IO) {
        val json = exportBackupJson()
        outputStream.bufferedWriter().use { it.write(json) }
    }

    suspend fun importBackupFromJson(jsonString: String): Result<BackupImportSummary> = withContext(Dispatchers.IO) {
        runCatching {
            val root = JSONObject(jsonString)
            var playlistsCount = 0
            var tracksCount = 0
            var favoritesCount = 0
            var settingsRestored = false

            // Restore Settings
            if (root.has("settings")) {
                val settingsObj = root.getJSONObject("settings")
                val quality = try {
                    AudioQuality.valueOf(settingsObj.optString("audioQuality", "HIGH"))
                } catch (_: Exception) { AudioQuality.HIGH }
                settingsDataStore.setAudioQuality(quality)
                settingsDataStore.setCrossfadeDuration(settingsObj.optInt("crossfadeDurationSec", 0))
                settingsDataStore.setLoudnessNormalization(settingsObj.optBoolean("loudnessNormalization", false))
                settingsDataStore.setOfflineMode(settingsObj.optBoolean("offlineMode", false))

                if (settingsObj.has("enabledSources")) {
                    val sourcesArr = settingsObj.getJSONArray("enabledSources")
                    val sources = mutableSetOf<SourceId>()
                    for (i in 0 until sourcesArr.length()) {
                        try {
                            sources.add(SourceId.valueOf(sourcesArr.getString(i)))
                        } catch (_: Exception) {}
                    }
                    if (sources.isNotEmpty()) {
                        settingsDataStore.setEnabledSources(sources)
                    }
                }

                settingsDataStore.setEqualizerEnabled(settingsObj.optBoolean("equalizerEnabled", false))
                settingsDataStore.setEqualizerPreset(settingsObj.optInt("equalizerPreset", 0))
                settingsDataStore.setEqualizerCustomBands(settingsObj.optString("equalizerCustomBands", ""))
                settingsDataStore.setBassBoostStrength(settingsObj.optInt("bassBoostStrength", 0))
                settingsDataStore.setVirtualizerStrength(settingsObj.optInt("virtualizerStrength", 0))
                settingsDataStore.setThemeMode(settingsObj.optString("activeThemeMode", "SYSTEM"))
                settingsRestored = true
            }

            // Restore Playlists
            if (root.has("playlists")) {
                val playlistsArray = root.getJSONArray("playlists")
                for (i in 0 until playlistsArray.length()) {
                    val plObj = playlistsArray.getJSONObject(i)
                    val playlistEntity = PlaylistEntity(
                        id = plObj.getString("id"),
                        name = plObj.getString("name"),
                        description = plObj.optString("description").takeIf { it.isNotEmpty() },
                        coverArtworkUri = plObj.optString("artworkUrl").takeIf { it.isNotEmpty() },
                        createdAt = plObj.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = plObj.optLong("updatedAt", System.currentTimeMillis())
                    )
                    database.playlistDao().insertPlaylist(playlistEntity)
                    playlistsCount++

                    if (plObj.has("tracks")) {
                        val tracksArray = plObj.getJSONArray("tracks")
                        val crossRefs = mutableListOf<PlaylistTrackCrossRef>()
                        for (j in 0 until tracksArray.length()) {
                            val trackObj = tracksArray.getJSONObject(j)
                            val trackEntity = jsonToTrackEntity(trackObj)
                            database.trackDao().insertOrUpdate(trackEntity)
                            crossRefs.add(
                                PlaylistTrackCrossRef(
                                    playlistId = playlistEntity.id,
                                    trackId = trackEntity.id,
                                    position = j
                                )
                            )
                            tracksCount++
                        }
                        database.playlistDao().insertTracksToPlaylist(crossRefs)
                    }
                }
            }

            // Restore Favorites
            if (root.has("favorites")) {
                val favoritesArray = root.getJSONArray("favorites")
                for (i in 0 until favoritesArray.length()) {
                    val trackObj = favoritesArray.getJSONObject(i)
                    val trackEntity = jsonToTrackEntity(trackObj)
                    database.trackDao().insertOrUpdate(trackEntity)
                    database.favoriteDao().insertFavorite(
                        FavoriteEntity(
                            trackId = trackEntity.id,
                            favoritedAt = System.currentTimeMillis()
                        )
                    )
                    favoritesCount++
                }
            }

            BackupImportSummary(
                playlistsImported = playlistsCount,
                tracksImported = tracksCount,
                favoritesImported = favoritesCount,
                settingsRestored = settingsRestored
            )
        }
    }

    suspend fun importBackupFromFile(inputStream: InputStream): Result<BackupImportSummary> = withContext(Dispatchers.IO) {
        val json = inputStream.bufferedReader().use { it.readText() }
        importBackupFromJson(json)
    }

    suspend fun getCacheSizeBytes(): Long = withContext(Dispatchers.IO) {
        var totalSize = 0L
        try {
            val cacheDir = context.cacheDir
            totalSize += calculateDirSize(cacheDir)
            val externalCache = context.externalCacheDir
            if (externalCache != null) {
                totalSize += calculateDirSize(externalCache)
            }
        } catch (_: Exception) {}
        totalSize
    }

    suspend fun clearCache(): Boolean = withContext(Dispatchers.IO) {
        try {
            // Clear local cache dirs
            deleteDirContents(context.cacheDir)
            context.externalCacheDir?.let { deleteDirContents(it) }
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun calculateDirSize(dir: File): Long {
        var size = 0L
        val files = dir.listFiles() ?: return 0L
        for (file in files) {
            size += if (file.isDirectory) calculateDirSize(file) else file.length()
        }
        return size
    }

    private fun deleteDirContents(dir: File) {
        val files = dir.listFiles() ?: return
        for (file in files) {
            if (file.isDirectory) {
                deleteDirContents(file)
                file.delete()
            } else {
                file.delete()
            }
        }
    }

    private fun trackEntityToJson(t: TrackEntity): JSONObject {
        return JSONObject().apply {
            put("id", t.id)
            put("sourceId", t.sourceId)
            put("sourceTrackId", t.sourceTrackId)
            put("title", t.title)
            put("titleSortKey", t.titleSortKey)
            put("artistName", t.artistName)
            put("albumName", t.albumName ?: "")
            put("durationMs", t.durationMs)
            put("artworkUrl", t.artworkUrl ?: "")
            put("licenseName", t.licenseName ?: "")
            put("licenseUrl", t.licenseUrl ?: "")
            put("downloadAllowed", t.downloadAllowed)
            put("cachedAt", t.cachedAt)
        }
    }

    private fun jsonToTrackEntity(obj: JSONObject): TrackEntity {
        return TrackEntity(
            id = obj.getString("id"),
            sourceId = obj.getString("sourceId"),
            sourceTrackId = obj.getString("sourceTrackId"),
            title = obj.getString("title"),
            titleSortKey = obj.optString("titleSortKey", obj.getString("title").lowercase()),
            artistName = obj.getString("artistName"),
            albumName = obj.optString("albumName").takeIf { it.isNotEmpty() },
            durationMs = obj.getLong("durationMs"),
            artworkUrl = obj.optString("artworkUrl").takeIf { it.isNotEmpty() },
            licenseName = obj.optString("licenseName").takeIf { it.isNotEmpty() },
            licenseUrl = obj.optString("licenseUrl").takeIf { it.isNotEmpty() },
            downloadAllowed = obj.optBoolean("downloadAllowed", false),
            cachedAt = obj.optLong("cachedAt", System.currentTimeMillis())
        )
    }
}
