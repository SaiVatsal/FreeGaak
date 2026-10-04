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
- 2026-10-04: Fixed stream playback issues: configured ExoPlayer with `DefaultHttpDataSource.Factory` enabling cross-protocol redirects for decentralized content nodes (Audius/Jamendo), added `onPlayerError` listener in `CrossfadePlayer`, added error message rendering in `NowPlayingScreen`, and added `MEDIA_PLAY_FROM_SEARCH` intent filter in `AndroidManifest.xml`.
- 2026-10-04: Implemented Deezer music source via RapidAPI (`DeezerSource`, Moshi DTO models, `SourceModule` Hilt multibinding, UI search/trending integration, `DeezerSourceTest`).
- 2026-10-04: Full test suite verification pass: 51/51 tests passing at 100%, lint clean, debug APK built successfully.
- 2026-10-04: SoundOrbit v1.1.0 release: Implemented RapidAPI Deezer multi-key rotation and automatic failover pool (4 keys with round-robin failover on HTTP 429/403), signed release APK with APK Signature Scheme v1, v2, and v3, configured R8 ProGuard keep rules for Moshi DTO serialization, fixed background coroutine test cleanup in `RepositoryTests`, 53/53 tests passing (100%), and generated `SoundOrbit-v1.1.0.apk` (5.1 MB).
- 2026-10-04: SoundOrbit v1.2.0 release: Integrated 2 additional RapidAPI keys into the 6-key failover pool (`local.properties`), designed and implemented animated splash screen with rotating vinyl CD, concentric grooves, specular sheen reflection, pulsing cosmic orbital rings, and "Developed by SaiVatsal" credit (`SplashScreen.kt`), connected smooth crossfade transition in `MainActivity.kt`, updated About section in `SettingsScreen.kt`, bumped app version to 1.2.0 (code 3), compiled and verified signed release APK with Signature Schemes v2 & v3, and deployed `SoundOrbit-v1.2.0.apk` (5.1 MB) to Desktop replacing v1.1.0.
- 2026-10-04: Fixed playback auto-pause bug and background execution: resolved audio focus self-collision by removing manual `AudioManager` listener in favor of ExoPlayer's native `AudioFocusManager`, fixed `Player.Listener` state transitions to evaluate `playWhenReady` across buffering states, enabled `C.WAKE_MODE_NETWORK` on ExoPlayer, added explicit foreground `PlaybackService` start command with `START_STICKY` lifecycle, and rebuilt and deployed updated signed `SoundOrbit-v1.2.0.apk` to Desktop.
- 2026-10-04: Integrated custom MP4 splash screen animation video (`Video Project 2.mp4` -> `splash_animation.mp4`) rendered via Jetpack Compose `AndroidView(PlayerView)` with fallback orbital glow, updated app branding to "Sound Orbit" across UI headers, Settings, and Android `strings.xml`, compiled release APK and deployed signed `SoundOrbit-v1.2.0.apk` (6.05 MB) to Desktop.
- 2026-10-04: Expanded song catalog with Option 1 (Deezer Top Charts, New Releases, Genre Channels: Pop, Bollywood/Hindi, Hip-Hop, Dance, Rock, R&B) and Option 2 (Spotify Top 50 Global, Top 50 India, Viral 50, New Music Friday discovery with cross-source stream resolution engine matching Deezer 320kbps MP3 streams), updated Home/Search/Settings UI with new carousels and filtering, resolved Robolectric in-memory SQLite connection test lifecycles (53/53 tests passing at 100%), and deployed updated signed release APK to Desktop.
- 2026-10-04: Updated developer branding and theme aesthetics: updated developer credit to "Grindokuu" styled in neon emerald green typography on both the animated video splash screen (`SplashScreen.kt`) and the About section (`SettingsScreen.kt`), polished dark-mode palette tokens with deep Obsidian surfaces (`#0B0D13`, `#10131A`, `#171B24`) and emerald green accents (`EmeraldGreenBright`), resolved Robolectric SQLite direct-executor lifecycle in `RepositoryTests.kt` (all 53/53 tests passing at 100%), verified lint and debug/release builds, and deployed fresh signed release APK `SoundOrbit-v1.2.0.apk` (6.37 MB) to Desktop.

