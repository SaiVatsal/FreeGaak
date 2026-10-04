# Progress

## Milestones

- [x] M1: Foundation — project setup, theme, navigation, empty screens, CI, signing, docs
- [x] M2: Player — Media3 service, PlaybackController, mini player, Now Playing, queue, LocalFilesSource
- [x] M3: Audius + Search + Home — AudiusSource, search, Home rows
- [x] M4: Jamendo + Browse — JamendoSource, genre/tag/language, A-Z index, licence display
- [x] M5: Library — Room tables, likes, playlists, history, stats, Your Orbit
- [x] M6: Sound and Lyrics — LRCLIB lyrics, sleep timer, equalizer, normalization, crossfade, speed, quality
- [x] M7: Downloads and Backup — licence-allowed downloads, WorkManager downloader, JSON backup/restore
- [x] M8: Android Auto and Notifications — browse tree, search, playback, notification polish
- [x] M9: Spotify (optional architecture) — PKCE models, browse endpoints, App Remote playback controller interface
- [x] M10: Hardening and Launch — full test pass (47/47 passing), release/debug builds, architecture verification, docs

## Current phase: All Milestones Complete & Verified

## Work log
- 2026-10-03: Completed Milestone 1 foundation: Material 3 tokens, Dark/OLED Black themes, Navigation with 4 tabs, SettingsDataStore, RateLimiter, SourceRegistry, zero-telemetry configuration, debug APK built, 8/8 unit tests passing, report generated in `docs/reports/M1.md`.
- 2026-10-04: Completed Milestone 2: Dual ExoPlayer crossfade audio engine, AndroidX Media3 PlaybackService, PlaybackController, LocalFilesSource, MediaStore query system, NowPlaying and MiniPlayer UI.
- 2026-10-04: Completed Milestones 3 & 4: Audius and Jamendo API music sources, RateLimiting, SearchScreen with multi-source filtering, BrowseScreen with CC license tagging and artist/album/tag discovery.
- 2026-10-04: Completed Milestone 5: Room database (TrackDao, FavoriteDao, PlaylistDao, ListeningHistoryDao, DownloadDao), Favorites, Playlists, Listening history, and "Your Orbit" personalized local stats & discovery mix.
- 2026-10-04: Completed Milestone 6: LRCLIB synchronized lyrics engine, LrcParser, hardware audio effects (Equalizer, BassBoost, Virtualizer), sleep timer, and audio settings.
- 2026-10-04: Completed Milestones 7 & 8: WorkManager background TrackDownloadWorker with atomic file persistence, DownloadRepository, MediaLibraryService Android Auto media browsing tree, and local JSON backup/restore.
- 2026-10-04: Completed Milestone 10: Full unit test suite (47 tests passing at 100%), verified Architecture tests, zero secrets, zero tracking, assembleDebug output (26.5 MB APK).
