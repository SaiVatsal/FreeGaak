package com.saivatsal.soundorbit.core.source.deezer.model

import com.saivatsal.soundorbit.core.model.Album
import com.saivatsal.soundorbit.core.model.Artist
import com.saivatsal.soundorbit.core.model.Playlist
import com.saivatsal.soundorbit.core.model.SourceId
import com.saivatsal.soundorbit.core.model.Track
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DeezerDataList<T>(
    @Json(name = "data") val data: List<T>? = null,
    @Json(name = "total") val total: Int = 0,
    @Json(name = "next") val next: String? = null
)

@JsonClass(generateAdapter = true)
data class DeezerTrackDto(
    @Json(name = "id") val id: Long = 0L,
    @Json(name = "title") val title: String = "",
    @Json(name = "title_short") val titleShort: String? = null,
    @Json(name = "duration") val duration: Long = 0L,
    @Json(name = "preview") val preview: String? = null,
    @Json(name = "link") val link: String? = null,
    @Json(name = "artist") val artist: DeezerArtistDto? = null,
    @Json(name = "album") val album: DeezerAlbumDto? = null
) {
    fun toDomainTrack(): Track = Track(
        sourceId = SourceId.DEEZER,
        sourceTrackId = id.toString(),
        title = title.ifBlank { titleShort ?: "Unknown Track" },
        titleSortKey = title.lowercase(),
        artistName = artist?.name ?: "Unknown Artist",
        albumName = album?.title,
        durationMs = if (duration > 0) duration * 1000L else 30_000L,
        artworkUrl = album?.coverBig ?: album?.coverMedium ?: album?.cover ?: artist?.pictureBig ?: artist?.pictureMedium,
        licenseName = "Deezer Standard",
        downloadAllowed = false
    )
}

@JsonClass(generateAdapter = true)
data class DeezerArtistDto(
    @Json(name = "id") val id: Long = 0L,
    @Json(name = "name") val name: String = "",
    @Json(name = "picture") val picture: String? = null,
    @Json(name = "picture_small") val pictureSmall: String? = null,
    @Json(name = "picture_medium") val pictureMedium: String? = null,
    @Json(name = "picture_big") val pictureBig: String? = null,
    @Json(name = "picture_xl") val pictureXl: String? = null,
    @Json(name = "nb_album") val nbAlbum: Int = 0,
    @Json(name = "nb_fan") val nbFan: Int = 0
) {
    fun toDomainArtist(): Artist = Artist(
        sourceId = SourceId.DEEZER,
        sourceArtistId = id.toString(),
        name = name.ifBlank { "Unknown Artist" },
        imageUrl = pictureBig ?: pictureMedium ?: picture
    )
}

@JsonClass(generateAdapter = true)
data class DeezerAlbumDto(
    @Json(name = "id") val id: Long = 0L,
    @Json(name = "title") val title: String = "",
    @Json(name = "cover") val cover: String? = null,
    @Json(name = "cover_small") val coverSmall: String? = null,
    @Json(name = "cover_medium") val coverMedium: String? = null,
    @Json(name = "cover_big") val coverBig: String? = null,
    @Json(name = "cover_xl") val coverXl: String? = null,
    @Json(name = "nb_tracks") val nbTracks: Int = 0,
    @Json(name = "artist") val artist: DeezerArtistDto? = null,
    @Json(name = "tracks") val tracks: DeezerDataList<DeezerTrackDto>? = null
) {
    fun toDomainAlbum(): Album = Album(
        sourceId = SourceId.DEEZER,
        sourceAlbumId = id.toString(),
        name = title.ifBlank { "Unknown Album" },
        artistName = artist?.name ?: "Unknown Artist",
        artworkUrl = coverBig ?: coverMedium ?: cover,
        trackCount = nbTracks
    )
}

@JsonClass(generateAdapter = true)
data class DeezerPlaylistDto(
    @Json(name = "id") val id: Long = 0L,
    @Json(name = "title") val title: String = "",
    @Json(name = "description") val description: String? = null,
    @Json(name = "picture") val picture: String? = null,
    @Json(name = "picture_small") val pictureSmall: String? = null,
    @Json(name = "picture_medium") val pictureMedium: String? = null,
    @Json(name = "picture_big") val pictureBig: String? = null,
    @Json(name = "picture_xl") val pictureXl: String? = null,
    @Json(name = "nb_tracks") val nbTracks: Int = 0,
    @Json(name = "tracks") val tracks: DeezerDataList<DeezerTrackDto>? = null
) {
    fun toDomainPlaylist(): Playlist = Playlist(
        sourceId = SourceId.DEEZER,
        sourcePlaylistId = id.toString(),
        name = title.ifBlank { "Unknown Playlist" },
        description = description,
        artworkUrl = pictureBig ?: pictureMedium ?: picture,
        trackCount = nbTracks
    )
}

@JsonClass(generateAdapter = true)
data class DeezerChartDto(
    @Json(name = "tracks") val tracks: DeezerDataList<DeezerTrackDto>? = null,
    @Json(name = "albums") val albums: DeezerDataList<DeezerAlbumDto>? = null,
    @Json(name = "artists") val artists: DeezerDataList<DeezerArtistDto>? = null,
    @Json(name = "playlists") val playlists: DeezerDataList<DeezerPlaylistDto>? = null
)
