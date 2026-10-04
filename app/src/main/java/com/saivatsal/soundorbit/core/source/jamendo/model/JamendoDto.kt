package com.saivatsal.soundorbit.core.source.jamendo.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class JamendoResponse<T>(
    @Json(name = "headers") val headers: JamendoHeadersDto? = null,
    @Json(name = "results") val results: T? = null
)

@JsonClass(generateAdapter = true)
data class JamendoHeadersDto(
    @Json(name = "status") val status: String = "",
    @Json(name = "code") val code: Int = 0,
    @Json(name = "error_message") val errorMessage: String? = null,
    @Json(name = "results_count") val resultsCount: Int = 0,
    @Json(name = "next") val next: String? = null
)

@JsonClass(generateAdapter = true)
data class JamendoTrackDto(
    @Json(name = "id") val id: String = "",
    @Json(name = "name") val name: String = "",
    @Json(name = "duration") val duration: Long = 0L,
    @Json(name = "artist_id") val artistId: String = "",
    @Json(name = "artist_name") val artistName: String = "",
    @Json(name = "album_name") val albumName: String? = null,
    @Json(name = "album_id") val albumId: String? = null,
    @Json(name = "license_ccurl") val licenseCcUrl: String? = null,
    @Json(name = "position") val position: Int = 0,
    @Json(name = "releasedate") val releaseDate: String? = null,
    @Json(name = "album_image") val albumImage: String? = null,
    @Json(name = "image") val image: String? = null,
    @Json(name = "audio") val audio: String? = null,
    @Json(name = "audiodl") val audioDl: String? = null,
    @Json(name = "shorturl") val shortUrl: String? = null,
    @Json(name = "shareurl") val shareUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class JamendoArtistDto(
    @Json(name = "id") val id: String = "",
    @Json(name = "name") val name: String = "",
    @Json(name = "website") val website: String? = null,
    @Json(name = "joindate") val joinDate: String? = null,
    @Json(name = "image") val image: String? = null,
    @Json(name = "shorturl") val shortUrl: String? = null,
    @Json(name = "shareurl") val shareUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class JamendoAlbumDto(
    @Json(name = "id") val id: String = "",
    @Json(name = "name") val name: String = "",
    @Json(name = "releasedate") val releaseDate: String? = null,
    @Json(name = "artist_id") val artistId: String = "",
    @Json(name = "artist_name") val artistName: String = "",
    @Json(name = "image") val image: String? = null,
    @Json(name = "shorturl") val shortUrl: String? = null,
    @Json(name = "shareurl") val shareUrl: String? = null,
    @Json(name = "tracks") val tracks: List<JamendoTrackDto>? = null
)

@JsonClass(generateAdapter = true)
data class JamendoPlaylistDto(
    @Json(name = "id") val id: String = "",
    @Json(name = "name") val name: String = "",
    @Json(name = "creationdate") val creationDate: String? = null,
    @Json(name = "user_id") val userId: String? = null,
    @Json(name = "user_name") val userName: String? = null,
    @Json(name = "tracks") val tracks: List<JamendoTrackDto>? = null
)
