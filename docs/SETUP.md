# SoundOrbit Setup and Distribution Guide

## 1. Keystore and Signing

The app uses a single shared release keystore so all builds (debug and release) can update over each other without uninstallation.

- **Keystore file:** `soundorbit-release.jks` (local only, git-ignored)
- **Alias:** `soundorbit`
- **SHA-1 Fingerprint:** `34:F7:54:E0:79:8A:4F:EB:CD:D1:7C:4F:77:80:31:77:3E:20:AF:B3`

### Back up your keystore!
**CRITICAL:** Back up `soundorbit-release.jks` and `keystore.properties` to a safe location (e.g., password manager or secure cloud drive). If you lose this keystore, you will never be able to update the app on your devices without uninstalling first and losing local data!

### GitHub Actions Secrets (for CI)
If you push to GitHub, add these 4 Repository Secrets:

1. `KEYSTORE_BASE64` - Base64 encoded keystore file:
   - On Linux/macOS: `base64 -w 0 soundorbit-release.jks`
   - On Windows PowerShell: `[Convert]::ToBase64String([IO.File]::ReadAllBytes("soundorbit-release.jks")) | Set-Clipboard`
2. `KEYSTORE_PASSWORD` - Keystore password (see `keystore.properties`)
3. `KEY_ALIAS` - `soundorbit`
4. `KEY_PASSWORD` - Key password (see `keystore.properties`)

## 2. API Keys Configuration

Copy `local.properties.example` to `local.properties` and fill in your keys:

```properties
AUDIUS_API_KEY=your_audius_api_key
AUDIUS_BEARER_TOKEN=your_audius_bearer_token
JAMENDO_CLIENT_ID=your_jamendo_client_id
SPOTIFY_CLIENT_ID=your_spotify_client_id
CONTACT_EMAIL=your_email@example.com
```

### Where to get keys:
- **Audius:** Register at [audius.co](https://audius.co) developer portal.
- **Jamendo:** Create a free account at [developer.jamendo.com](https://developer.jamendo.com) and create an application.
- **Spotify (Optional):** Create an app in the [Spotify Developer Dashboard](https://developer.spotify.com/dashboard).
  - Add SHA-1 fingerprint: `34:F7:54:E0:79:8A:4F:EB:CD:D1:7C:4F:77:80:31:77:3E:20:AF:B3`
  - Package name: `com.saivatsal.soundorbit`
  - Redirect URI: `soundorbit://spotify-callback`
- **MusicBrainz:** No key needed, but set `CONTACT_EMAIL` to identify your app in the User-Agent header (required by MusicBrainz policy, 1 req/sec rate limit).

**Note:** A missing key never breaks the build. The corresponding source will safely disable itself with a friendly message in the UI.

## 3. How to Build and Install

### Build Debug APK:
```bash
./gradlew assembleDebug
```
Output: `app/build/outputs/apk/debug/app-debug.apk`

### Install directly to a connected phone:
```bash
./gradlew installDebug
```

### Build Release APK:
```bash
./gradlew assembleRelease
```
Output: `app/build/outputs/apk/release/app-release.apk`

## 4. How to Distribute to Friends and Family

Since this is a private app (no Play Store):

1. Build the APK: `./gradlew assembleDebug`
2. Send the APK file (`app/build/outputs/apk/debug/app-debug.apk`) directly to your sister and friends via:
   - Nearby Share / Quick Share
   - Signal / WhatsApp / Telegram (as a file)
   - Local file transfer (USB)
3. On their phones:
   - Tap the received APK to install
   - If prompted, allow "Install unknown apps" from the app they received it through
   - Future APK updates will install seamlessly over the existing version because they share the same signing key

## 5. Android Auto Setup

For sideloaded apps on Android Auto:
1. Open the **Android Auto** app on your phone.
2. Scroll to the bottom and tap **Version** repeatedly (10 times) to unlock Developer Mode.
3. Tap the 3-dot menu > **Developer settings**.
4. Check **Unknown sources**.
5. SoundOrbit will now appear in your car's Android Auto media apps list.

## 6. Battery Optimization Note

Some Android devices (especially Samsung, Xiaomi, Huawei) aggressively kill background audio services. If playback stops when the screen is off:
1. Go to **Settings > Apps > SoundOrbit > Battery**.
2. Select **Unrestricted** (or turn off "Battery optimization").
