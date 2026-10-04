package com.saivatsal.soundorbit.core.repository

import com.saivatsal.soundorbit.core.database.dao.FavoriteDao
import com.saivatsal.soundorbit.core.database.dao.TrackDao
import com.saivatsal.soundorbit.core.database.entity.FavoriteEntity
import com.saivatsal.soundorbit.core.database.entity.toDomain
import com.saivatsal.soundorbit.core.database.entity.toEntity
import com.saivatsal.soundorbit.core.model.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface FavoritesRepository {
    fun getFavoriteTracks(): Flow<List<Track>>
    fun isFavorite(trackId: String): Flow<Boolean>
    suspend fun isFavoriteDirect(trackId: String): Boolean
    suspend fun toggleFavorite(track: Track)
    suspend fun addFavorite(track: Track)
    suspend fun removeFavorite(trackId: String)
    fun getFavoriteCount(): Flow<Int>
}

@Singleton
class FavoritesRepositoryImpl @Inject constructor(
    private val favoriteDao: FavoriteDao,
    private val trackDao: TrackDao
) : FavoritesRepository {

    override fun getFavoriteTracks(): Flow<List<Track>> {
        return favoriteDao.getFavoriteTracks().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun isFavorite(trackId: String): Flow<Boolean> {
        return favoriteDao.isFavoriteFlow(trackId)
    }

    override suspend fun isFavoriteDirect(trackId: String): Boolean {
        return favoriteDao.isFavorite(trackId)
    }

    override suspend fun toggleFavorite(track: Track) {
        val entity = track.toEntity()
        val trackId = entity.id
        if (favoriteDao.isFavorite(trackId)) {
            favoriteDao.deleteFavorite(trackId)
        } else {
            trackDao.insertOrUpdate(entity)
            favoriteDao.insertFavorite(FavoriteEntity(trackId = trackId))
        }
    }

    override suspend fun addFavorite(track: Track) {
        val entity = track.toEntity()
        trackDao.insertOrUpdate(entity)
        favoriteDao.insertFavorite(FavoriteEntity(trackId = entity.id))
    }

    override suspend fun removeFavorite(trackId: String) {
        favoriteDao.deleteFavorite(trackId)
    }

    override fun getFavoriteCount(): Flow<Int> {
        return favoriteDao.getFavoriteCount()
    }
}
