package com.saivatsal.soundorbit.core.source.audius.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AudiusResponse<T>(
    val data: T
)

@JsonClass(generateAdapter = true)
data class AudiusArtworkDto(
    @Json(name = "150x150") val small: String? = null,
    @Json(name = "480x480") val medium: String? = null,
    @Json(name = "1000x1000") val large: String? = null
)

@JsonClass(generateAdapter = true)
data class AudiusUserDto(
    val id: String,
    val name: String,
    val handle: String? = null,
    val bio: String? = null,
    @Json(name = "profile_picture") val profilePicture: AudiusArtworkDto? = null,
    @Json(name = "follower_count") val followerCount: Int = 0,
    @Json(name = "is_verified") val isVerified: Boolean = false
)

@JsonClass(generateAdapter = true)
data class AudiusTrackDto(
    val id: String,
    val title: String,
    val duration: Long = 0,
    val artwork: AudiusArtworkDto? = null,
    val genre: String? = null,
    val mood: String? = null,
    val tags: String? = null,
    val user: AudiusUserDto? = null,
    val description: String? = null,
    @Json(name = "play_count") val playCount: Long = 0,
    @Json(name = "is_stream_gated") val isStreamGated: Boolean = false,
    @Json(name = "is_download_gated") val isDownloadGated: Boolean = false,
    @Json(name = "downloadable") val downloadable: Boolean = false
)

@JsonClass(generateAdapter = true)
data class AudiusPlaylistDto(
    val id: String,
    @Json(name = "playlist_name") val playlistName: String,
    val description: String? = null,
    val artwork: AudiusArtworkDto? = null,
    val user: AudiusUserDto? = null,
    @Json(name = "total_play_count") val totalPlayCount: Long = 0,
    @Json(name = "track_count") val trackCount: Int = 0
)
