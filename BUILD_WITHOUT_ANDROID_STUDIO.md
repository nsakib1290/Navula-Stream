# Build SakibStream without Android Studio

1. Create/sign in to GitHub.
2. Create a repository named `SakibStream`.
3. Extract this ZIP and upload the **contents** of `SakibStream_v2` to the repository root. Keep `.github/`, `app/`, and the Gradle files.
4. Open Actions → **Build SakibStream APK** → **Run workflow**.
5. Wait for the workflow to complete successfully.
6. Download `SakibStream-debug-apk`, extract it, and install `app-debug.apk`.

The first launch opens `https://7movies.ws/`. Later launches reopen the last selected source.

The app switches between Movies and Live TV & Sports sources and can move to the next source when the current site's main page returns a network, HTTP 400+ or SSL error. It cannot detect every playback failure.

Vega-compatible provider extensions are not implemented in this build. Android WebView does not support Chrome extensions, and Vega providers require a separate sandbox runtime and provider API bridge. See `provider-repository/PROVIDER_SPEC.md`.
