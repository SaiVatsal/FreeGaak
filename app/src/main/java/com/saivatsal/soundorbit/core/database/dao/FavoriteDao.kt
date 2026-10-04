package com.saivatsal.soundorbit.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.saivatsal.soundorbit.core.database.entity.FavoriteEntity
import com.saivatsal.soundorbit.core.database.entity.TrackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE trackId = :trackId")
    suspend fun deleteFavorite(trackId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE trackId = :trackId)")
    fun isFavoriteFlow(trackId: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE trackId = :trackId)")
    suspend fun isFavorite(trackId: String): Boolean

    @Transaction
    @Query("""
        SELECT t.* FROM tracks t
        INNER JOIN favorites f ON t.id = f.trackId
        ORDER BY f.favoritedAt DESC
    """)
    fun getFavoriteTracks(): Flow<List<TrackEntity>>

    @Query("SELECT COUNT(*) FROM favorites")
    fun getFavoriteCount(): Flow<Int>

    @Query("SELECT trackId FROM favorites")
    suspend fun getAllFavoriteTrackIds(): List<String>
}
