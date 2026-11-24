# SakibStream source and provider architecture

## Website source shortcuts
The app includes user-requested website shortcuts in two groups:
- Movies
- Live TV & Sports

These entries are opened in Android WebView. The selected source ID and category are stored locally so the app reopens the last selected source. On main-frame network errors, HTTP status 400+, or SSL errors, the app tries the next source in that category.

This is a website launcher, not a catalog scraper or stream resolver. A successful page load does not guarantee that the site's embedded player or an individual stream works.

## Vega provider compatibility
Vega providers are not generic website links. They use Vega's JavaScript provider contract, provider files, and a sandbox bridge. A compatible implementation requires:
- Fetching and validating a Vega provider source manifest
- Downloading and verifying provider bundle files
- A sandboxed JavaScript runtime with a strict message protocol
- Implementing the Vega provider API and storage/cookie scope
- Mapping provider catalog, metadata, episode, and stream results into SakibStream UI
- Domain/permission controls and safe lifecycle management

The previous starter's repository form did not implement any of those steps. This release therefore does not pretend that Vega extensions are installed or running. Do not execute arbitrary provider code directly in the app process.

## Security and usage
- Only HTTPS websites are listed.
- WebView file/content access is disabled.
- No DRM, paywall, access-control, or geographic restriction bypass is implemented.
- Third-party websites may block WebView, require sign-in, or change their layout without notice.
