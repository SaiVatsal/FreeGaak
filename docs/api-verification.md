# API Verification Log

Verified on: 2026-04-10

## 1. Audius API
- **Docs URL:** `https://docs.audius.co` / `https://docs.openaudio.org`
- **Base URL:** `https://discoveryprovider.audius.co/v1/` (discovery node base) or `https://api.audius.co`
- **Auth:** `app_name` query parameter (e.g. `?app_name=SoundOrbit`). Optional Bearer token for developer accounts.
- **Key Endpoints:**
  - Search tracks: `GET /v1/tracks/search?query={q}&app_name={app}&limit={l}&offset={o}`
  - Trending tracks: `GET /v1/tracks/trending?app_name={app}&time={week|month|year|allTime}&genre={g}&limit={l}&offset={o}`
  - Track details: `GET /v1/tracks/{track_id}?app_name={app}`
  - Track stream: `GET /v1/tracks/{track_id}/stream?app_name={app}` (returns HTTP 302 redirect to content node CDN URL)
  - Artist / User: `GET /v1/users/{user_id}?app_name={app}`
  - Artist tracks: `GET /v1/users/{user_id}/tracks?app_name={app}`
  - Playlist details: `GET /v1/playlists/{playlist_id}?app_name={app}`
  - Playlist tracks: `GET /v1/playlists/{playlist_id}/tracks?app_name={app}`
- **Rate Limits:** ~100 req/min per IP per discovery node.
- **Attribution & Terms:** Open Audio Protocol. Must respect stream gating flags (`is_stream_gated`, `is_download_gated`).
- **Status:** Active (v1).
- **Conservative choices / rules:**
  - Map gated tracks to `SourceError.Gated`.
  - Declare `hasLanguageFilter = false`, `supportsSelectableQuality = false` in capabilities.

## 2. Jamendo API
- **Docs URL:** `https://developer.jamendo.com/v3.0`
- **Base URL:** `https://api.jamendo.com/v3.0/`
- **Auth:** `client_id` query parameter.
- **Key Endpoints:**
  - Search & Browse: `GET /v3.0/tracks/?client_id={id}&format=json&search={q}&type=single albumtrack&audioformat={fmt}&include=licenses+musicinfo+stats+lyrics`
  - Audio formats: `mp31` (96k), `mp32` (VBR high), `ogg`, `flac`.
  - Language filter: `lang={code}` or `lang[]={code}` (2-letter ISO code).
  - License info: `include=licenses` returns `license_ccurl`.
  - Download eligibility: check `audiodownload_allowed == true` and `audiodownload` URL is non-empty.
- **Rate Limits:** 35,000 requests/month on free tier.
- **Attribution & Terms:** Non-commercial use only. Must display CC license name + URL and artist credit. Must provide backlink to Jamendo. No caching of audio.
- **Status:** Active (v3.0).

## 3. LRCLIB (Synced Lyrics)
- **Docs URL:** `https://lrclib.net` / `github.com/tranxuanthang/lrclib`
- **Base URL:** `https://lrclib.net/api`
- **Auth:** None for read.
- **Key Endpoints:**
  - Exact match: `GET /api/get?track_name={title}&artist_name={artist}&album_name={album}&duration={sec}`
  - Fallback search: `GET /api/search?q={query}&track_name={title}&artist_name={artist}`
  - Response contains: `syncedLyrics` (LRC format: `[mm:ss.xx] text`), `plainLyrics`, `instrumental` (boolean).
- **Rate Limits:** Reasonable usage. Send descriptive `User-Agent: SoundOrbit/1.0.0 (contact@example.com)`.
- **Status:** Active.

## 4. MusicBrainz API (Metadata Enrichment)
- **Docs URL:** `https://musicbrainz.org/doc/MusicBrainz_API`
- **Base URL:** `https://musicbrainz.org/ws/2/`
- **Auth:** None for read queries.
- **Rate Limits:** Strictly 1 request/second for the whole app. Enforced via mutex rate limiter.
- **User-Agent:** Mandatory `SoundOrbit/1.0.0 ( {CONTACT_EMAIL} )`.
- **Rule:** If `CONTACT_EMAIL` is empty in `local.properties`, MusicBrainz enrichment is automatically DISABLED to prevent anonymous requests.
- **Status:** Active (ws/2).

## 5. Spotify Web API (Discovery & Charts) + Cross-Source Resolution
- **Docs URL:** `https://developer.spotify.com/documentation/web-api`
- **Base URL:** `https://api.spotify.com/v1`
- **Auth:** Client Credentials / Public anonymous token flow or PKCE token for public chart playlists (`/playlists/{id}/tracks`).
- **Key Endpoints:**
  - Search tracks: `GET /v1/search?q={query}&type=track&limit={l}&offset={o}`
  - Charts / Playlists: `GET /v1/playlists/{playlist_id}/tracks?limit=50` (Top 50 Global `37i9dQZEVXbMDoHDwVN2tF`, Top 50 India `37i9dQZEVXbLZ52XmNYSJg`, Viral 50 `37i9dQZEVXbLiRSasKsNU9`)
  - New Releases: `GET /v1/browse/new-releases?limit=20`
  - Track details: `GET /v1/tracks/{id}`
- **Cross-Source Stream Resolution:** When playing Spotify tracks, metadata (clean title & artist name) is queried against Deezer high-quality 320kbps MP3 preview stream, Audius, or Jamendo to resolve playable audio streams. In-memory stream cache prevents redundant resolution calls.
- **Status:** Active.

## 6. Deezer API (via RapidAPI Multi-Key Pool)
- **Docs URL:** `https://rapidapi.com/deezerdevs/api/deezerdevs-deezer` / `https://developers.deezer.com/api`
- **Base URL:** `https://deezerdevs-deezer.p.rapidapi.com`
- **Auth:** `x-rapidapi-key` and `x-rapidapi-host` headers. Multiple keys configured in `local.properties` (`RAPIDAPI_DEEZER_KEYS`) with automatic round-robin rotation and failover on HTTP 429 (Rate Limit) and HTTP 401/403 (Unauthorized/Forbidden).
- **Key Endpoints:**
  - Search tracks: `GET /search?q={query}&index={i}&limit={l}`
  - Search artists: `GET /search/artist?q={query}&index={i}&limit={l}`
  - Search albums: `GET /search/album?q={query}&index={i}&limit={l}`
  - Search playlists: `GET /search/playlist?q={query}&index={i}&limit={l}`
  - Top charts / Trending: `GET /chart/0/tracks` (with fallback to `GET /chart`)
  - Genre Channels & Editorial: `GET /genre/{genre_id}/artists`, `GET /chart/{genre_id}/tracks` (Pop: 132, Bollywood/Hindi: 116, Hip-Hop: 116, Dance/Electronic: 113, Rock: 152, R&B: 165)
  - Track details: `GET /track/{id}` (contains 30-second MP3 `preview` stream URL)
  - Artist details & top tracks: `GET /artist/{id}`, `GET /artist/{id}/top?limit=20`, `GET /artist/{id}/albums?limit=20`
  - Album details: `GET /album/{id}`
  - Playlist details: `GET /playlist/{id}`
- **Rate Limits:** RapidAPI rate-limited with automatic multi-key failover and `Retry-After` header parsing. App throttles via `RateLimiter(minIntervalMs = 300L)`.
- **Stream Playback:** High-quality MP3 preview stream playback supported. Downloads disabled (`allowsDownloads = false`).
- **Status:** Active.
