# SoundOrbit Standing Orders

A private, offline-first, ad-free Android music app for a 4-person group. Built with Jetpack Compose, Material 3, Media3, Room, and Hilt. No accounts, no backend server, no analytics. All user data stays on device.

## Golden Rules

1. **No tracking:** No ads, ad SDKs, analytics, tracking, or crash-reporting SDKs (no Firebase, Crashlytics). No accounts or login to open the app. All user data stays on device.
2. **Legal sources only:** No extracting, scraping, or downloading from YouTube / YouTube Music (no InnerTube, NewPipe extractor, yt-dlp). No unofficial or reverse-engineered APIs.
3. **Original branding:** Never copy Spotify's (or any other service's) name, logo, or assets. Original branding only.
4. **Verified APIs:** Check every API, library version, and Android behavior against official docs before use. Record in `docs/api-verification.md`.
5. **No backend server:** The data layer is local (Room + repositories + source adapters).
6. **No secrets in commits:** Never commit keys, tokens, keystores, or passwords. Never log secrets.
7. **Never silence problems:** No deleting failing tests, no `@Suppress` for real bugs, no `!!`, no `GlobalScope`, no disabling lint.
8. **Local git only:** Never push to any remote or create a remote repository.
9. **Document conflicts:** If a requirement clashes with an API limit or legal rule, record it in `docs/DECISIONS.md` and choose the safest legal alternative.

## Commands

- Run all checks: `./gradlew lintDebug testDebugUnitTest assembleDebug`
- Unit tests: `./gradlew test`
- Build debug APK: `./gradlew assembleDebug`
- Install to connected device: `./gradlew installDebug`
- Keys live in `local.properties` (git-ignored, never committed). See `local.properties.example`.

## Loop for Any Future Task

1. Read `docs/ARCHITECTURE.md`, `docs/DECISIONS.md`, and `docs/PROGRESS.md` first.
2. Make the smallest change that solves the task. Keep the layering rules (`feature` -> `core`; `core` never imports `feature`).
3. Add or update tests in the same change. Never delete or weaken a test to pass.
4. Run `./gradlew lintDebug test assembleDebug`. All must pass before committing.
5. Re-verify any source API touched and update `docs/api-verification.md`.
6. Update affected docs and log in `docs/PROGRESS.md` ("Future work log").
7. Commit with a clear imperative message. Never push, never commit secrets.

Any future request must obey every rule in this file exactly like the original build did. If a request conflicts with a Golden rule, say so, explain why, and do not break the rule.

See `docs/ARCHITECTURE.md`, `docs/DECISIONS.md`, `docs/DESIGN.md`, and `docs/SETUP.md` for full specifications.
