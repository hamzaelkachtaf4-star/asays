# Setup and Run Guide

This guide covers building Naviify in Android Studio, connecting it to a
self-hosted Navidrome server, and testing the Android Auto integration with the
Desktop Head Unit (DHU).

## Prerequisites

- macOS / Linux / Windows with Android Studio installed (Ladybug or newer)
- JDK 17+
- Android SDK Platform 35 + Build Tools (Studio prompts to install these)
- A physical Android phone on Android 8.0+ (API 26+), or an emulator
- A running Navidrome instance (or any Subsonic-compatible server)

## Building

1. Open the project root (the folder containing `settings.gradle.kts`) in
   Android Studio. Studio reads `gradle/wrapper/gradle-wrapper.properties` and
   bootstraps the Gradle wrapper.
2. Let the Gradle sync finish (it downloads AGP, Kotlin, Compose, Media3, etc.
   on the first run).
3. Build the debug APK:

   ```shell
   ./gradlew :app:assembleDebug
   ```

4. Run the JVM test suite (auth, DTO parsing, repository, queue, quality,
   scrobble and lyrics logic):

   ```shell
   ./gradlew :app:testDebugUnitTest
   ```

## Connecting to Navidrome

1. Enable Developer Options and USB debugging on the phone/emulator.
2. Launch the app. The first screen asks for your server.
3. Enter:
   - **Server URL**: e.g. `http://192.168.1.20:4533` or
     `https://music.example.com` (the app adds `/rest` itself; a trailing
     `/rest` you typed is stripped automatically).
   - **Username**: your Navidrome user.
   - **Password** or **API token**: Navidrome shows a Subsonic API token under
     Settings > Users > your user. Token auth is preferred.
4. Tap **Connect**. The app pings `ping.view` and shows server type/version.

> Plain HTTP on a local network works because cleartext traffic is enabled.
> Use a reverse proxy with HTTPS for anything remote or untrusted.

## Streaming quality (FLAC)

- Open **Settings > Streaming quality**.
- Pick a quality for **Wi-Fi** and one for **Mobile data**.
- **Lossless** omits `maxBitRate`, so Navidrome serves the original file
  bit-for-bit (FLAC stays FLAC).
- The fullscreen player shows a **FLAC · LOSSLESS** badge on lossless sources;
  Settings shows which preset is active on the current network.

Playback uses a 1 GB LRU `SimpleCache` with generous ExoPlayer buffers so
30-80 MB FLAC files stream without stutter. Settings shows live bytes used by
the audio cache and Coil image cache, with a one-tap **Clear all caches**.

## Lyrics, queue and scrobbling

- Tap the queue icon in Now Playing to jump to or remove upcoming songs.
- Toggle **Show lyrics** for synced OpenSubsonic lyrics (falls back to legacy
  `getLyrics.view`; synced lines auto-scroll with playback).
- Incoming tracks report **now playing** (`scrobble` submission=false) and are
  scrobbled (`submission=true`) after 50% of the track or 4 minutes.

## Android Auto with Desktop Head Unit (DHU)

1. **On the phone**: install "Android Auto for phone screens", open its settings,
   tap **Version** ~10 times to enable developer mode, then enable **Start head
   unit server**.
2. **On the computer**: Android Studio > SDK Manager > SDK Tools > **Desktop
   Head Unit** > install. Launch it when available via Studio's Tools menu or
   directly:

   ```shell
   # macOS example; adjust for your SDK path
   $ANDROID_HOME/extras/google/auto/desktop-head-unit/dhu
   ```

3. Connect the phone over USB and allow USB debugging. Android Auto opens on
   the desktop head unit.
4. Naviify appears under media apps (it exposes a `MediaLibraryService`). On
   the dashboard browse **Favorites / Playlists / Albums / Artists**; browsing
   and "Play" stream directly through the playback service.

### Troubleshooting

- **App not listed in AA**: verify the merged manifest contains the service
  (`adb shell dumpsys package com.naviify.app`), and that head unit server mode
  is enabled on the phone.
- **No playback**: test in-app playback first, then verify Wi-Fi/mobile quality
  presets and that the phone can reach the Navidrome host.
- **Server unreachable from the car**: the phone must reach the server (same
  LAN, or HTTPS over the internet).
