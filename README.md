# Naviify

Naviify is a production-oriented Android music client for self-hosted
[Navidrome](https://www.navidrome.org) (and other Subsonic-compatible) servers.
It targets 120Hz-smooth Jetpack Compose UI, a Media3-based background playback
engine, and Android Auto support through `MediaLibraryService`.

## Current status

- **Phase 1 (done):** Project skeleton, Gradle build, Navidrome/Subsonic REST
  client (md5 token + salt auth, persistent cookies, dynamic server URL),
  DataStore server config, Room favorites cache, Hilt wiring, and a functional
  first-run screen that tests the server connection.
- **Phase 2 (done):** Home / Search / Library / Settings screens, Spotify-style
  navigation shell with persistent mini-player slot, MediaRepository +
  FavoritesRepository, debounced search, create-playlist flow, and Coil
  memory + disk caching.
- **Phase 3:** Media3 ExoPlayer engine, `MediaSessionService` +
  `MediaLibraryService`, Android Auto, stream caching.
- **Phase 3 (done):** Media3 `MediaLibraryService` engine with LRU `SimpleCache`
  streaming, Android Auto browsing tree (Favorites/Playlists/Albums/Artists),
  media notification + favorite custom action, and the `MediaController` bridge
  feeding the mini player and now-playing sheet (seek/shuffle/repeat).
- **Phase 4 (done):** Fullscreen player with lyrics + interactive queue,
  scrobbling, FLAC lossless streaming with per-network quality presets, live
  cache management, and a full setup guide (`SETUP_AND_RUN.md`).

## Build

Open the project in Android Studio (Ladybug or newer) and sync. Requirements:

- JDK 17+
- Android SDK 35 (`compileSdk = 35`, `minSdk = 26`)
- Gradle 8.11.1 via the wrapper properties (Studio bootstraps the wrapper jar)

```shell
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

## Architecture

```
com.naviify.app
├── core.network      Retrofit + OkHttp client, Subsonic DTOs, auth/cookie interceptors
├── core.storage      DataStore server config, Room database (favorites)
├── data.repository   AuthRepository (ping / connection state)
├── domain.model      Domain media models (Artist, Album, Track)
└── ui                Compose screens, theme, ViewModels
```

The Subsonic client authenticates every request with `u`, `t`, `s` where
`t = md5(secret + s)` and `s` is a per-request random salt, plus `v=1.16.1`,
`c=Naviify`, `f=json`. Both password auth and Navidrome API-token auth are
supported (token wins when both are set).
