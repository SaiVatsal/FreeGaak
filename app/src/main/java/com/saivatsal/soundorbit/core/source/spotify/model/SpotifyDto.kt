package com.saivatsal.soundorbit.core.source.spotify.model

import com.saivatsal.soundorbit.core.model.Album
import com.saivatsal.soundorbit.core.model.Artist
import com.saivatsal.soundorbit.core.model.Playlist
import com.saivatsal.soundorbit.core.model.SourceId
import com.saivatsal.soundorbit.core.model.Track
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SpotifyPaging<T>(
    @Json(name = "items") val items: List<T>? = null,
    @Json(name = "total") val total: Int = 0,
    @Json(name = "limit") val limit: Int = 20,
    @Json(name = "offset") val offset: Int = 0,
    @Json(name = "next") val next: String? = null
)

@JsonClass(generateAdapter = true)
data class SpotifyImageDto(
    @Json(name = "url") val url: String = "",
    @Json(name = "height") val height: Int? = null,
    @Json(name = "width") val width: Int? = null
)

@JsonClass(generateAdapter = true)
data class SpotifyArtistDto(
    @Json(name = "id") val id: String = "",
    @Json(name = "name") val name: String = "",
    @Json(name = "images") val images: List<SpotifyImageDto>? = null,
    @Json(name = "genres") val genres: List<String>? = null
) {
    fun toDomainArtist(): Artist = Artist(
        sourceId = SourceId.SPOTIFY,
        sourceArtistId = id,
        name = name.ifBlank { "Unknown Artist" },
        imageUrl = images?.firstOrNull()?.url
    )
}

@JsonClass(generateAdapter = true)
data class SpotifyAlbumDto(
    @Json(name = "id") val id: String = "",
    @Json(name = "name") val name: String = "",
    @Json(name = "images") val images: List<SpotifyImageDto>? = null,
    @Json(name = "artists") val artists: List<SpotifyArtistDto>? = null,
    @Json(name = "total_tracks") val totalTracks: Int = 0,
    @Json(name = "tracks") val tracks: SpotifyPaging<SpotifyTrackDto>? = null
) {
    fun toDomainAlbum(): Album = Album(
        sourceId = SourceId.SPOTIFY,
        sourceAlbumId = id,
        name = name.ifBlank { "Unknown Album" },
        artistName = artists?.joinToString(", ") { it.name } ?: "Unknown Artist",
        artworkUrl = images?.firstOrNull()?.url,
        trackCount = totalTracks
    )
}

@JsonClass(generateAdapter = true)
data class SpotifyTrackDto(
    @Json(name = "id") val id: String = "",
    @Json(name = "name") val name: String = "",
    @Json(name = "duration_ms") val durationMs: Long = 0L,
    @Json(name = "preview_url") val previewUrl: String? = null,
    @Json(name = "artists") val artists: List<SpotifyArtistDto>? = null,
    @Json(name = "album") val album: SpotifyAlbumDto? = null
) {
    fun toDomainTrack(): Track = Track(
        sourceId = SourceId.SPOTIFY,
        sourceTrackId = id,
        title = name.ifBlank { "Unknown Track" },
        titleSortKey = name.lowercase(),
        artistName = artists?.joinToString(", ") { it.name }?.ifBlank { "Unknown Artist" } ?: "Unknown Artist",
        albumName = album?.name,
        durationMs = if (durationMs > 0) durationMs else 180_000L,
        artworkUrl = album?.images?.firstOrNull()?.url,
        licenseName = "Spotify Discovery",
        downloadAllowed = false
    )
}

@JsonClass(generateAdapter = true)
data class SpotifyPlaylistTrackItem(
    @Json(name = "track") val track: SpotifyTrackDto? = null
)

@JsonClass(generateAdapter = true)
data class SpotifyPlaylistDto(
    @Json(name = "id") val id: String = "",
    @Json(name = "name") val name: String = "",
    @Json(name = "description") val description: String? = null,
    @Json(name = "images") val images: List<SpotifyImageDto>? = null,
    @Json(name = "tracks") val tracks: SpotifyPaging<SpotifyPlaylistTrackItem>? = null
) {
    fun toDomainPlaylist(): Playlist = Playlist(
        sourceId = SourceId.SPOTIFY,
        sourcePlaylistId = id,
        name = name.ifBlank { "Unknown Playlist" },
        description = description,
        artworkUrl = images?.firstOrNull()?.url,
        trackCount = tracks?.total ?: 0
    )
}

@JsonClass(generateAdapter = true)
data class SpotifySearchResponse(
    @Json(name = "tracks") val tracks: SpotifyPaging<SpotifyTrackDto>? = null,
    @Json(name = "artists") val artists: SpotifyPaging<SpotifyArtistDto>? = null,
    @Json(name = "albums") val albums: SpotifyPaging<SpotifyAlbumDto>? = null,
    @Json(name = "playlists") val playlists: SpotifyPaging<SpotifyPlaylistDto>? = null
)

@JsonClass(generateAdapter = true)
data class SpotifyNewReleasesResponse(
    @Json(name = "albums") val albums: SpotifyPaging<SpotifyAlbumDto>? = null
)

@JsonClass(generateAdapter = true)
data class SpotifyTokenResponse(
    @Json(name = "accessToken") val accessToken: String? = null,
    @Json(name = "access_token") val altAccessToken: String? = null,
    @Json(name = "accessTokenExpirationTimestampMs") val expirationMs: Long = 0L,
    @Json(name = "expires_in") val expiresInSec: Long = 3600L
)
