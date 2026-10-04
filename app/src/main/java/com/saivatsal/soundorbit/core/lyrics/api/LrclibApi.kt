package com.saivatsal.soundorbit.core.lyrics.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface LrclibApi {

    @GET("api/get")
    suspend fun getLyrics(
        @Query("track_name") trackName: String,
        @Query("artist_name") artistName: String,
        @Query("album_name") albumName: String? = null,
        @Query("duration") durationSec: Int? = null
    ): Response<LrclibResponse>

    @GET("api/search")
    suspend fun searchLyrics(
        @Query("track_name") trackName: String,
        @Query("artist_name") artistName: String
    ): Response<List<LrclibResponse>>

    @GET("api/search")
    suspend fun searchByQuery(
        @Query("q") query: String
    ): Response<List<LrclibResponse>>
}
