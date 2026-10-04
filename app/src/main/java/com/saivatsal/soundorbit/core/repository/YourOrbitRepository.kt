package com.saivatsal.soundorbit.core.repository

import com.saivatsal.soundorbit.core.database.dao.FavoriteDao
import com.saivatsal.soundorbit.core.database.dao.ListeningHistoryDao
import com.saivatsal.soundorbit.core.database.dao.TrackDao
import com.saivatsal.soundorbit.core.database.entity.ArtistPlayCount
import com.saivatsal.soundorbit.core.database.entity.TrackEntity
import com.saivatsal.soundorbit.core.database.entity.toDomain
import com.saivatsal.soundorbit.core.model.Track
import com.saivatsal.soundorbit.core.model.TrendingWindow
import com.saivatsal.soundorbit.core.source.PageRequest
import com.saivatsal.soundorbit.core.source.SourceRegistry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

data class OrbitSummary(
    val topArtists: List<String>,
    val totalTracksPlayed: Int,
    val favoriteTracksCount: Int,
    val heavyRotationTracks: List<Track>,
    val dailyMixTracks: List<Track>,
    val rediscoveryTracks: List<Track>
)

interface YourOrbitRepository {
    fun getOrbitSummary(): Flow<OrbitSummary>
    fun getDailyMix(): Flow<List<Track>>
    fun getHeavyRotation(): Flow<List<Track>>
    fun getDiscoveryMix(): Flow<List<Track>>
}

@Singleton
class YourOrbitRepositoryImpl @Inject constructor(
    private val listeningHistoryDao: ListeningHistoryDao,
    private val favoriteDao: FavoriteDao,
    private val trackDao: TrackDao,
    private val sourceRegistry: SourceRegistry
) : YourOrbitRepository {

    override fun getOrbitSummary(): Flow<OrbitSummary> {
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        val topArtistsFlow = listeningHistoryDao.getTopArtists(sinceTimestamp = thirtyDaysAgo, limit = 5)
        val mostPlayedFlow = listeningHistoryDao.getMostPlayedTracks(sinceTimestamp = thirtyDaysAgo, limit = 10)
        val recentFlow = listeningHistoryDao.getRecentlyPlayedTracks(limit = 30)
        val favoritesFlow = favoriteDao.getFavoriteTracks()
        val totalPlaysFlow = listeningHistoryDao.getTotalPlayCount()
        val favoriteCountFlow = favoriteDao.getFavoriteCount()

        val historyCombined = combine(
            topArtistsFlow,
            mostPlayedFlow,
            recentFlow
        ) { artists: List<ArtistPlayCount>, mostPlayed: List<TrackEntity>, recents: List<TrackEntity> ->
            Triple(artists, mostPlayed.map { it.toDomain() }, recents.map { it.toDomain() })
        }

        val metaCombined = combine(
            favoritesFlow,
            totalPlaysFlow,
            favoriteCountFlow
        ) { favorites: List<TrackEntity>, totalPlays: Int, favCount: Int ->
            Triple(favorites.map { it.toDomain() }, totalPlays, favCount)
        }

        return combine(historyCombined, metaCombined) { historyData, metaData ->
            val (topArtists, mostPlayed, recents) = historyData
            val (favorites, totalPlays, favCount) = metaData

            val topArtistNames = topArtists.map { it.artistName }

            // Daily Mix: deterministic seed based on day of year
            val daySeed = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
            val pool = (favorites + mostPlayed + recents).distinctBy { it.compositeKey }
            val dailyMix = if (pool.isNotEmpty()) {
                val rng = Random(daySeed)
                pool.shuffled(rng).take(15)
            } else {
                emptyList()
            }

            // Rediscovery: tracks played previously but not in the most recent 10 plays
            val recentKeys = recents.take(10).map { it.compositeKey }.toSet()
            val rediscovery = mostPlayed
                .filter { it.compositeKey !in recentKeys }
                .take(10)

            OrbitSummary(
                topArtists = topArtistNames,
                totalTracksPlayed = totalPlays,
                favoriteTracksCount = favCount,
                heavyRotationTracks = mostPlayed.take(10),
                dailyMixTracks = dailyMix,
                rediscoveryTracks = rediscovery
            )
        }
    }

    override fun getDailyMix(): Flow<List<Track>> = flow {
        getOrbitSummary().collect { summary ->
            emit(summary.dailyMixTracks)
        }
    }

    override fun getHeavyRotation(): Flow<List<Track>> = flow {
        getOrbitSummary().collect { summary ->
            emit(summary.heavyRotationTracks)
        }
    }

    override fun getDiscoveryMix(): Flow<List<Track>> = flow {
        val resultTracks = mutableListOf<Track>()
        val sources = sourceRegistry.allSources()
        for (source in sources) {
            if (source.capabilities.hasTrending) {
                val trendingResult = source.trending(
                    genre = null,
                    window = TrendingWindow.WEEK,
                    page = PageRequest(limit = 10)
                )
                trendingResult.getOrNull()?.items?.let { tracks ->
                    resultTracks.addAll(tracks)
                }
            }
        }
        emit(resultTracks.distinctBy { it.compositeKey }.shuffled().take(20))
    }
}
