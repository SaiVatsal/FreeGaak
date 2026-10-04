# SoundOrbit Architecture

## Overview
SoundOrbit is a private, offline-first Android music player built with Jetpack Compose, Material 3, Media3, Room, and Hilt. No remote backend server; all data lives locally in Room and DataStore.

## Layering and Module Structure

Single module `:app` with strict package-by-feature and one-way dependencies:
`feature -> core`; `core` never imports `feature`.

```
app/src/main/java/com/saivatsal/soundorbit/
├── core/
│   ├── audio/         # Media3 service, player controllers, effects, sleep timer, crossfade
│   ├── common/        # Dispatchers, Result/Error types, RateLimiter, Flow helpers
│   ├── database/      # Room database, DAOs, entities, migrations, database views
│   ├── datastore/     # Settings DataStore (theme, quality, audio effects)
│   ├── di/            # Hilt dependency injection modules
│   ├── model/         # Domain models (Track, Artist, Album, Playlist, Stream)
│   ├── network/       # Retrofit APIs (Audius, Jamendo, LRCLIB, MusicBrainz)
│   └── source/        # MusicSource contract, AudiusSource, JamendoSource, LocalFilesSource, SpotifySource
├── feature/
│   ├── details/       # Artist, Album, Playlist detail screens
│   ├── home/          # Quick picks, Trending, Charts, Mood chips, Genre grid, Your Orbit
│   ├── library/       # Liked songs, Playlists, Downloads, History, Stats
│   ├── player/        # Mini player, Now Playing, Synced lyrics, Equalizer, Queue
│   ├── search/        # Live search, Suggestions, Recent searches, A-Z browse
│   └── settings/      # Audio quality, Equalizer, Backup/Restore, Themes, About
├── ui/
│   ├── navigation/    # SoundOrbitNavigation, NavHost, Routes
│   └── theme/         # Material 3 Theme, Color, Type, Shapes (Dark-first, OLED)
├── MainActivity.kt
└── SoundOrbitApplication.kt
```

## Core Contracts

### 1. `MusicSource`
Polymorphic interface implemented by:
- `AudiusSource` (Remote, decentralized streaming)
- `JamendoSource` (Remote, Creative Commons streaming + downloads)
- `LocalFilesSource` (On-device MediaStore audio)
- `SpotifySource` (Optional, Spotify App Remote playback + Web API browse)

UI reads `SourceCapabilities` to dynamically show/hide features (e.g., quality picker, downloads, language filter).

### 2. Playback Architecture
- `PlaybackController` interface decouples UI from Media3 internals.
- `Media3PlaybackController` binds to `MediaLibraryService` (`PlaybackService`).
- Lazy stream resolution via custom `ResolvingDataSource`.
- Persistent cache using ExoPlayer `SimpleCache` with custom cache key: `${sourceId}:${trackId}:${quality}`.
- Dual-ExoPlayer crossfade sharing one `audioSessionId` for platform audio effects.

### 3. Room Database
Composite key for all tracks: `${sourceId}:${sourceTrackId}`.
Tables:
- `tracks`: Cached track metadata with `titleSortKey`
- `playlists`: User-created playlists
- `playlist_entries`: Ordered tracks in playlists
- `likes`: Liked tracks
- `playback_history`: Played tracks with listening duration and streak calculation
- `track_language`: Inferred or user-set language metadata
- `downloads`: Offline downloaded files tracking
- `search_history`: Recent search queries
- `lyrics_cache`: Cached LRC lyrics (including negative/not-found cache)
- `queue_state`: Last active playback queue and position for seamless resume

### 4. Privacy & Network Security
- `allowBackup="false"` and restrictive data extraction rules.
- Local JSON backup/restore only (sanitized, versioned schema).
- No analytics, tracking, or crash-reporting SDKs.
- Network traffic strictly limited to verified API hosts. Cleartext traffic disabled.
