# SakibStream v1.1

A lightweight Android source launcher built with Kotlin, Jetpack Compose and Android WebView.

## What's changed
- Opens the last selected source on app startup. First launch defaults to `https://7movies.ws/`.
- Separate Movies and Live TV & Sports source lists, with quick switching inside the browser screen.
- Source selection is saved with Android SharedPreferences and survives app restarts.
- No in-app browser Back or Forward buttons. The close button returns to SakibStream; Android Back also closes the source view.
- Embedded WebView with JavaScript and DOM storage enabled for modern sites.
- On main-page network errors, HTTP errors (400+) or SSL errors, tries the next source in the same category.
- Attempts a cosmetic cleanup of common website footer/copyright elements. This is best-effort and cannot remove every site's branding or layout.
- GitHub Actions workflow builds a debug APK without Android Studio.

## Important limitations
- The source entries are website shortcuts, not verified content providers. A site may be online while its video player or individual stream is unavailable.
- Automatic fallback detects main-frame load/network/HTTP/SSL errors. It cannot reliably detect a site that loads successfully but has no playable video.
- Android WebView is not a Chrome extension host. Browser extensions cannot be installed in this build.
- Vega providers are JavaScript modules designed for Vega's own provider contract and sandbox bridge. The old ZIP's Provider Manager was only a demo form; this build does not claim Vega provider compatibility. A separate compatible provider runtime and API bridge must be implemented and tested before Vega extensions can run.
- No ad-blocking engine, DRM bypass, paywall bypass, or content-hosting feature is included.
- Third-party sites control their own content, availability, cookies, login requirements, and playback restrictions. Use only content you are authorized to access.

## Build APK without Android Studio
1. Create a GitHub repository.
2. Upload the contents of this folder to the repository root (keep `.github/`).
3. Open Actions → Build SakibStream APK → Run workflow.
4. Download the `SakibStream-debug-apk` artifact and extract it.
5. Install `app-debug.apk` on an Android device.

The output is a debug APK for testing, not a Play Store release-signed APK.
