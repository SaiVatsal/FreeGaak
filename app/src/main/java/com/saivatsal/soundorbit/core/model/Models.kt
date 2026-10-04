package com.saivatsal.soundorbit.core.model

enum class SourceId {
    AUDIUS,
    JAMENDO,
    LOCAL,
    SPOTIFY
}

data class Track(
    val sourceId: SourceId,
    val sourceTrackId: String,
    val title: String,
    val titleSortKey: String,
    val artistName: String,
    val albumName: String? = null,
    val durationMs: Long,
    val artworkUrl: String? = null,
    val licenseName: String? = null,
    val licenseUrl: String? = null,
    val downloadAllowed: Boolean = false
) {
    val compositeKey: String get() = "${sourceId.name}_$sourceTrackId"
}

data class Artist(
    val sourceId: SourceId,
    val sourceArtistId: String,
    val name: String,
    val imageUrl: String? = null
) {
    val compositeKey: String get() = "${sourceId.name}_$sourceArtistId"
}

data class ArtistDetails(
    val artist: Artist,
    val bio: String? = null,
    val topTracks: List<Track> = emptyList(),
    val albums: List<Album> = emptyList()
)

data class Album(
    val sourceId: SourceId,
    val sourceAlbumId: String,
    val name: String,
    val artistName: String,
    val artworkUrl: String? = null,
    val trackCount: Int = 0
) {
    val compositeKey: String get() = "${sourceId.name}_$sourceAlbumId"
}

data class AlbumDetails(
    val album: Album,
    val tracks: List<Track> = emptyList()
)

data class Playlist(
    val sourceId: SourceId,
    val sourcePlaylistId: String,
    val name: String,
    val description: String? = null,
    val artworkUrl: String? = null,
    val trackCount: Int = 0
) {
    val compositeKey: String get() = "${sourceId.name}_$sourcePlaylistId"
}

data class PlaylistDetails(
    val playlist: Playlist,
    val tracks: List<Track> = emptyList()
)

enum class AudioQuality {
    LOW,
    NORMAL,
    HIGH,
    AUTO
}

data class ResolvedStream(
    val uri: String,
    val mimeType: String? = null,
    val headers: Map<String, String> = emptyMap(),
    val expiresAtMs: Long? = null
)

enum class SearchFilterType {
    ALL,
    SONGS,
    ARTISTS,
    ALBUMS,
    PLAYLISTS
}

data class SearchFilter(
    val type: SearchFilterType = SearchFilterType.ALL,
    val language: String? = null
)

enum class TrendingWindow {
    WEEK,
    MONTH,
    ALL_TIME
}

enum class BrowseKind {
    GENRE,
    TAG,
    LANGUAGE,
    MOOD
}
