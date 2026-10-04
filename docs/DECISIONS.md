# Decisions

## Architecture decisions and trade-offs

| Date | Decision | Reason | Alternative considered |
|------|----------|--------|----------------------|
| 2026-10-04 | Dual-ExoPlayer crossfade architecture | Media3 ExoPlayer does not support concurrent audio stream gapless volume ramp mixing natively in a single instance | Single ExoPlayer with simple seek and metadata update |
| 2026-10-04 | Unified MusicSource SPI with Dagger Multibinds | Decouples audio sources (Local, Audius, Jamendo, Spotify) from player & UI logic | Monolithic repository handling all sources |
| 2026-10-04 | Windows NTFS virtual drive isolation for Gradle test worker | Java argfile parser treats single quote in Windows username directory as delimiter | Modifying system username or avoiding unit tests |
| 2026-10-04 | WorkManager for Background Track Downloads | Provides guaranteed execution with network constraints and foreground service notifications | Custom foreground service or raw CoroutineScope |
| 2026-10-04 | MediaLibraryService for Android Auto | Enables head units and external controllers to browse Favorites, Downloads, and Local files via standard MediaLibrarySession hierarchy | Simple MediaSessionService without browsable library tree |
| 2026-10-04 | Underscore Composite Key Formatting | Standardizes Track composite key format (`${sourceId.name}_$sourceTrackId`) across Room entities and Domain models | Divergent key formats across layers |
| 2026-10-04 | Spotify Discovery + Cross-Source Stream Resolution Engine | Leverages Spotify Web API for latest trending charts (Top 50 Global, Top 50 India, Viral 50) and resolves audio streams dynamically via Deezer high-quality 320kbps MP3 previews, Audius, and Jamendo | Spotify App Remote only (which requires Spotify Premium and installed app) |

## API conflicts or legal constraints

| Date | Issue | Resolution |
|------|-------|------------|
| 2026-10-04 | YouTube/InnerTube/yt-dlp stream extraction | Strictly forbidden by Rule 2; exclusively use Audius, Jamendo, Local, and Spotify Web API |
| 2026-10-04 | Privacy & zero-telemetry enforcement | Exclude Firebase, Play Services analytics, Crashlytics, and remote user accounts |

## Library version conflicts

| Date | Libraries | Resolution |
|------|-----------|------------|
| 2026-10-04 | Robolectric 4.14.1 with Android 15 (targetSdk 35) | Pinned unit test SDK shadow target to API 34 via `@Config(sdk = [34])` |

