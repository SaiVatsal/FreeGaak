package com.saivatsal.soundorbit.core.lyrics.repository

import android.content.Context
import androidx.collection.LruCache
import com.saivatsal.soundorbit.core.lyrics.api.LrclibApi
import com.saivatsal.soundorbit.core.lyrics.model.Lyrics
import com.saivatsal.soundorbit.core.lyrics.parser.LrcParser
import com.saivatsal.soundorbit.core.model.Track
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

interface LyricsRepository {
    suspend fun getLyrics(track: Track): Result<Lyrics>
    suspend fun saveCustomLyrics(trackId: String, lrcContent: String): Result<Lyrics>
}

@Singleton
class LyricsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val lrclibApi: LrclibApi
) : LyricsRepository {

    private val memoryCache = LruCache<String, Lyrics>(100)
    private val lyricsDir by lazy {
        File(context.cacheDir, "lyrics").apply { if (!exists()) mkdirs() }
    }

    override suspend fun getLyrics(track: Track): Result<Lyrics> = withContext(Dispatchers.IO) {
        val cacheKey = "${track.sourceId.name}_${track.sourceTrackId}"

        // 1. Check in-memory cache
        memoryCache.get(cacheKey)?.let { return@withContext Result.success(it) }

        // 2. Check disk cache
        val diskFile = File(lyricsDir, "$cacheKey.lrc")
        if (diskFile.exists() && diskFile.length() > 0) {
            try {
                val content = diskFile.readText()
                val parsed = LrcParser.parse(content)
                val lyrics = Lyrics(
                    trackId = cacheKey,
                    plainLyrics = content,
                    syncedLyrics = parsed,
                    isInstrumental = false
                )
                memoryCache.put(cacheKey, lyrics)
                return@withContext Result.success(lyrics)
            } catch (e: Exception) {
                // Corrupt file, continue to network fetch
                diskFile.delete()
            }
        }

        // 3. Network fetch from LRCLIB
        val cleanTitle = cleanTrackTitle(track.title)
        val cleanArtist = cleanArtistName(track.artistName)
        val durationSec = (track.durationMs / 1000).toInt()

        try {
            val response = lrclibApi.getLyrics(
                trackName = cleanTitle,
                artistName = cleanArtist,
                albumName = track.albumName,
                durationSec = if (durationSec > 0) durationSec else null
            )

            val body = if (response.isSuccessful && response.body() != null) {
                response.body()
            } else {
                // Fallback to search endpoint
                val searchResponse = lrclibApi.searchLyrics(
                    trackName = cleanTitle,
                    artistName = cleanArtist
                )
                searchResponse.body()?.firstOrNull()
            }

            if (body != null) {
                val synced = LrcParser.parse(body.syncedLyrics)
                val lyrics = Lyrics(
                    trackId = cacheKey,
                    plainLyrics = body.plainLyrics,
                    syncedLyrics = synced,
                    isInstrumental = body.instrumental ?: false
                )

                // Cache to memory
                memoryCache.put(cacheKey, lyrics)

                // Cache to disk if synced or plain lyrics exist
                val lrcToSave = body.syncedLyrics ?: body.plainLyrics
                if (!lrcToSave.isNullOrBlank()) {
                    try {
                        diskFile.writeText(lrcToSave)
                    } catch (_: Exception) {}
                }

                Result.success(lyrics)
            } else {
                Result.failure(NoSuchElementException("No lyrics found for $cleanTitle by $cleanArtist"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveCustomLyrics(trackId: String, lrcContent: String): Result<Lyrics> = withContext(Dispatchers.IO) {
        try {
            val diskFile = File(lyricsDir, "$trackId.lrc")
            diskFile.writeText(lrcContent)
            val parsed = LrcParser.parse(lrcContent)
            val lyrics = Lyrics(
                trackId = trackId,
                plainLyrics = lrcContent,
                syncedLyrics = parsed,
                isInstrumental = false
            )
            memoryCache.put(trackId, lyrics)
            Result.success(lyrics)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun cleanTrackTitle(title: String): String {
        return title
            .replace(Regex("""\s*[\(\[](?:feat\.?|ft\.?|official|video|audio|remastered|explicit|clean|live|deluxe|version|edit).*?[\)\]]""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s*-\s*(?:feat\.?|ft\.?|official|remastered|live).*$""", RegexOption.IGNORE_CASE), "")
            .trim()
    }

    private fun cleanArtistName(artist: String): String {
        return artist
            .replace(Regex("""\s*(?:feat\.?|ft\.?|featuring|,|\&|x|\/).*$""", RegexOption.IGNORE_CASE), "")
            .trim()
    }
}
