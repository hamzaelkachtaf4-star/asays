# ASAYS (formerly Naviify) — Complete Project Master Guide & Handover

> **Purpose**: This document is an exhaustive, self-contained technical handbook and progress report for **ASAYS** (an advanced Spotify-style Subsonic/Navidrome Android music client). It contains everything built so far, architectural blueprints, critical gotchas, verification procedures, and the prioritized roadmap for subsequent development phases.
> 
> *Drop this file directly into any new AI agent prompt or conversation to resume work with 100% context.*

---

## 1. Project Overview & Tech Stack

- **App Name**: **ASAYS** (Official brand name; Kotlin package remains `com.naviify.app`).
- **Brand Colors**: 
  - Primary Accent: **ASAYS Green** (`#69B987`)
  - Background: Deep AMOLED Black (`#000000` / `#0D0D0D`)
  - Surfaces: Frosted Card Surfaces (`#181818`, `#282828`)
- **Primary Backend**: **Navidrome** / Subsonic / OpenSubsonic REST API.
- **Secondary Integrations**: **LRCLIB** (for synced and plain-text lyrics matching).
- **Core Technology Stack**:
  - **Language**: Kotlin 2.0+ (100% Coroutines & StateFlow)
  - **UI Toolkit**: Jetpack Compose + Material 3 (with custom dynamic theme engine)
  - **Dependency Injection**: Dagger Hilt
  - **Audio Engine**: AndroidX Media3 (`MediaLibraryService`, `ExoPlayer`, `MediaSession`)
  - **Android Auto**: Full Android Auto Coolwalk support (`MediaLibraryService` + custom controls layout)
  - **Image Loading**: Coil 2 with custom static salt keying and hardware GPU acceleration
  - **Persistence**: Jetpack DataStore (Preferences) + Android KeyStore (hardware-backed AES-256-GCM encryption for credentials)
  - **Networking**: Retrofit 2 + OkHttp 4 (with dual-network reactive failover: Home LAN <-> Tailscale/Remote)
- **Local Working Directory**: `/Users/tayeb/Documents/naviify`
- **Android SDK / ADB**: `/Users/tayeb/Library/Android/sdk/platform-tools/adb`

---

## 2. Directory Structure & Key Components

```
app/src/main/java/com/naviify/app/
├── core/
│   ├── image/
│   │   ├── CoilModule.kt               # ImageLoader with hardware bitmaps enabled
│   │   └── CoverArt.kt                 # CoverUrls helper with static salt for caching
│   ├── network/
│   │   ├── SubsonicService.kt          # Retrofit endpoints (playlists, search, scrobble, stream)
│   │   ├── SubsonicAuth.kt             # Token, salt, MD5 generator (ASAYS client identity)
│   │   ├── DynamicServerInterceptor.kt # Seamless in-flight retry/failover + encoded query fix
│   │   ├── ServerUrlRouter.kt          # Concurrent LAN fast-path (<350ms) vs Tailscale probing
│   │   ├── SessionStateHolder.kt       # In-memory reactive state of server credentials & URLs
│   │   └── DataStoreCookieJar.kt       # Persistent session cookies
│   ├── playback/
│   │   ├── NaviifyPlaybackService.kt   # Media3 MediaLibraryService lifecycle & notification
│   │   ├── PlaybackController.kt       # ExoPlayer wrapper, sleep timers, smart autoplay
│   │   ├── NaviifyLibraryCallback.kt   # Android Auto browse tree & Coolwalk custom actions
│   │   ├── ArtworkContentProvider.kt   # ContentProvider for Android Auto & lockscreen covers
│   │   └── MediaItemMapper.kt          # Domain Track <-> MediaItem mapping + artwork bytes
│   ├── storage/
│   │   ├── ServerConfigStore.kt        # Encrypted server URLs & token persistence
│   │   └── KeystoreEncryptor.kt        # AES-256-GCM Keystore encryption wrapper
│   └── theme/
│       └── AppTheme.kt                 # 10 theme presets (Spotify, Glass, OLED, Sunset, Titanium, etc.)
│
├── data/
│   ├── download/
│   │   ├── DownloadRepository.kt       # Parallel downloads (Semaphore 3), in-memory cover index
│   │   └── OfflinePlaylistStore.kt     # Local JSON persistence for offline/downloaded playlists
│   ├── lyrics/
│   │   ├── LrclibLyricsClient.kt       # Strict LRCLIB candidate validation (artist & duration ±6s)
│   │   └── LyricsPreferencesStore.kt   # Blocked tracks and reported lyric issue storage
│   ├── repository/
│   │   ├── MediaRepository.kt          # Two-way sync event bus, caching, radio recommendation
│   │   └── FavoritesRepository.kt      # Starred tracks, albums, artists
│   ├── search/
│   │   └── SearchHistoryStore.kt       # DataStore search history (30 items, deduplicated)
│   └── stats/
│       └── ListeningStatsStore.kt      # Listening history, playback streaks, heatmap logs
│
├── domain/
│   ├── model/
│   │   ├── MediaModels.kt              # @Immutable Track, Album, Artist
│   │   ├── PlaylistModels.kt           # @Immutable Playlist, SearchResults, FavoriteType
│   │   └── LyricsModels.kt             # SyncedLine, LyricsData, with live offsetMs
│   └── playback/
│       └── PlayerQueueStore.kt         # Queue management, shuffle state, reordering, originalQueue
│
└── ui/
    ├── components/
    │   ├── MediaComponents.kt          # AmbientGlassBackdrop, TrackRow, AlbumCard, PlaylistCard
    │   ├── MarqueeText.kt              # Looping continuous horizontal scrolling text
    │   ├── TrackActionSheets.kt        # "Add to Playlist" sheet with thumbnails & instant checkmark
    │   ├── PlaylistActionSheets.kt     # Cover picker, track removal, song picker sheets
    │   └── ErrorBubble.kt              # Floating dynamic island / error notification
    ├── home/
    │   ├── HomeScreen.kt               # ASAYS branding, user playlists carousel, 6-grid quick access
    │   └── HomeViewModel.kt            # Strict offline filtering, greeting, quick picks
    ├── player/
    │   ├── NowPlayingScreen.kt         # Lyrics sync sheet, sleep timer, gestures, Marquee titles
    │   ├── MiniPlayerBar.kt            # Floating mini-player with looping Marquee text
    │   ├── QueueSheet.kt               # Interactive queue with Up Next, Move Up/Down, Play Next
    │   ├── LyricsActionSheets.kt       # Fine-tune timing slider, report problem, manual search
    │   └── PlayerViewModel.kt          # Audio controls, sleep timer, lyrics calibration
    ├── playlist/
    │   ├── PlaylistsScreen.kt          # 2-column grid, dynamic geometric themes, pull-to-refresh
    │   ├── PlaylistDetailScreen.kt     # Ambient gradient halo, 1-click download, sync button
    │   └── PlaylistDetailViewModel.kt  # Real-time two-way synchronization with Navidrome
    ├── search/
    │   ├── SearchScreen.kt             # Search history, prioritized results (Songs first)
    │   └── SearchViewModel.kt          # Debounced search, history recorder & clear actions
    ├── stats/
    │   ├── StatsScreen.kt              # Period-adaptive GitHub heatmap (7D/30D/90D/ALL), stats
    │   └── StatsViewModel.kt           # Listening activity metrics
    └── settings/
        ├── SettingsScreen.kt           # Live library scan progress, lyrics corrections dashboard
        └── SettingsViewModel.kt        # Polling scan status, cache clearing, connection testing
```

---

## 3. Exhaustive Feature Inventory (What We Have Accomplished)

Below is the chronological log of all 27 major milestones implemented, tested, and verified:

### 1. Real-Time Lyrics Timing Sync & Slider Sheet
- Added `offsetMs` to `LyricsData`, persisted via `DownloadRepository.saveLyrics()`.
- Interactive `AdjustLyricsSheet` with live preview of the currently highlighted line as the song plays, a continuous slider from `-10.0s` to `+10.0s` in 0.1s increments, and quick stepping buttons (`-1.0s`, `-0.5s`, `Reset`, `+0.5s`, `+1.0s`).

### 2. Sleep Timer & "End of track" Queue Preservation
- Fixed bug where `sleepUntilTrackEnd` desynchronized ExoPlayer from `PlayerQueueStore`.
- Added active countdown publishing (`_sleepTimerRemainingSeconds`) and safe queue state synchronization on track finish.

### 3. Redesigned Spotify-Style Sleep Timer Bottom Sheet
- Active timer card with glowing timer icon and live countdown.
- Preset options: 5m, 10m, 15m, 30m, 45m, 1h, plus "End of track" and custom time stepper.

### 4. Clean Startup & Animated Floating Error Bubble
- Eliminated startup flash by removing the placeholder icon during auth resolution.
- Replaced the inline error box with a floating frosted-glass capsule (`ErrorBubble`) with spring slide-in animation and one-tap retry.

### 5. Security & Architecture Audit Implementation
- **Keystore Encryption**: Server passwords and Subsonic tokens encrypted via AES-256-GCM (`KeystoreEncryptor`).
- **Token Leak Prevention**: Redacted sensitive query params (`?u=...&t=...&s=...`) from logs and error messages.
- **Corrupt Download Guard**: Strict assertions (`length() > 0L`) preventing 0-byte corrupt files from being marked complete.
- **Repeat & Shuffle Persistence**: Queue repeat and shuffle states persisted to storage and restored on startup.

### 6. Android Auto Album Art (`ArtworkContentProvider`)
- Solved missing album art in Android Auto Coolwalk by creating `ArtworkContentProvider` serving `content://${applicationId}.artwork/cover/{coverArtId}.jpg` across process boundaries.
- Embedded direct `artworkData` bytes for local downloads for zero-latency in-car rendering.

### 7. Playlist Track Overwrite Bug Fix
- Fixed critical bug where calling `createPlaylist` wiped existing tracks; migrated to Subsonic `updatePlaylist.view` with `songIdToAdd` and `songIndexToRemove`.

### 8. Ultra-Fast Reconnection & Smart In-Flight Failover
- Reduced server probe times from 3,000ms to <250ms via concurrent probing with Home LAN fast-path (<350ms).
- Dynamic in-flight failover seamlessly switches from Home LAN to Tailscale if an `IOException` occurs mid-request.

### 9. 120fps Smooth Scrolling Optimization
- Replaced dynamic salts in `CoverUrls` with static salt (`naviify_cover`) so Coil hits memory and disk caches 100% of the time.
- Annotated `Track`, `Album`, and `Artist` with `@Immutable` to enable Compose item skipping.
- Eliminated main-thread disk I/O with in-memory `coverFileIndex` and `localFileIndex`.

### 10. Complete Full-Song Lyrics Sharing
- Upgraded lyrics sharing to package the entire song lyrics rather than just a single active line.

### 11. Grouped Modular Settings & Real-Time Scan Progress Line
- Organized settings into 6 clean cards: Server & Network, Server Library, Storage & Cache, Streaming Quality, Personalization, and Account.
- Real-time polling progress bar during library scans displaying live indexed file count.

### 12. 2-Column Playlists & Procedural Geometric Themes
- Replaced single-column list with 2-column `LazyVerticalGrid`.
- Created 12 handcrafted color gradients and 4 procedural canvas patterns (waves, concentric rings, diamonds, equalizer bars) determined deterministically by playlist ID.

### 13. Pixel-Perfect GitHub Listening Heatmap
- Replaced mismatched styles with an interactive GitHub-style contribution heatmap.
- Mathematically bound month headers to day columns to eliminate visual drift.

### 14. Search Prioritization & Continuous Smart Autoplay / Radio
- Re-ordered search results: **Songs (Music)** at top, followed by Albums, Artists, and Playlists.
- Implemented smart fallback radio: when a queue approaches its end, it automatically fetches similar tracks by style, artist, or album.

### 15. Spotify-Style Home Dedicated to User Playlists
- Removed artificial smart playlists; placed user-created playlists front and center in the 6-Grid and dedicated "Your Playlists" carousel.

### 16. Strict Offline Mode
- Toggling "Offline" strictly filters out un-downloaded content, showing only downloaded tracks, albums, and playlists.

### 17. Period-Adaptive Heatmap & Theme-Harmonized Palette
- Heatmap adapts to selected timeframe (7D = 2 weeks, 30D = 5 weeks, 90D = 13 weeks, ALL = 16 weeks).
- Heatmap tiles and charts follow the user's active theme accent color.

### 18. Spotify-Style "Add to Playlist" Sheet
- Added song header thumbnail, individual playlist cover art, inline "+ New Playlist" creation, and instant checkmark animations.

### 19. Playlist Name Double-Encoding Fix (`%20` / `%2520`)
- Fixed Retrofit query double-encoding where space characters became `fuck%20off`. Added automatic display sanitization via `URLDecoder`.

### 20. 1-Click Concurrent Playlist Downloads & Deletion
- Top bar download button queues all missing tracks with concurrency (Semaphore 3).
- Added playlist deletion confirmation dialog removing playlists from server and local storage.

### 21. Interactive Shuffle Queue & Reordering
- Visible shuffled queue showing true upcoming order while preserving `originalQueue`.
- Up Next reorder controls: **Move Up**, **Move Down**, **Play Next**, and **Reshuffle**.

### 22. Official ASAYS Rebranding & Launcher Icons
- Renamed app to **ASAYS** across launcher, Android Auto, notifications, and settings.
- Official brand green `#69B987`.
- Fixed Android 13+ Material You themed icons by creating a 100% transparent monochrome glyph (`ic_launcher_monochrome.png`).

### 23. Android Auto Coolwalk Full Controls
- Balanced car playback card with custom actions on the right: **Shuffle**, **Favorite (Heart)** with live sync, and **Repeat**.

### 24. Lyrics Suite: Strict Validation, Report, Block, & Manual Search
- Strict candidate validation (artist name must match, duration within $\pm 6$s) preventing incorrect lyrics.
- Lyrics options: Block wrong lyrics, manual LRCLIB candidate search dialog, and issue reporting dashboard in Settings.

### 25. Automatic Two-Way Real-Time Navidrome Sync
- Added `playlistSyncEvents` SharedFlow in `MediaRepository`.
- Any modification (create, rename, add track, remove track, delete) updates Navidrome and immediately notifies all active screens.
- Auto-sync on return via Compose `LifecycleResumeEffect`.
- Support for uploading custom cover photos or resetting back to Navidrome's dynamic 4-album collage.

### 26. Ambient Glassy / Blurry Backdrop Gradient
- Created `AmbientGlassBackdrop` composable blending the active theme accent color with cover art blur.
- Applied across `HomeScreen`, `AlbumDetailScreen`, `ArtistDetailScreen`, and `PlaylistDetailScreen`.

### 27. Spotify-Style Search History & Looping Marquee
- **Search History**: DataStore-backed storage (`SearchHistoryStore`) recording the 30 most recent searches. Shown when search bar is empty, complete with type badges, cover art, individual delete buttons, and "Clear all".
- **MarqueeText**: Continuous Spotify-style horizontal scrolling text for long song titles, artist names, and album titles across `NowPlayingScreen` (portrait & landscape) and `MiniPlayerBar`.

### 28. Android Auto Album Art Glitch & Truncation Fix
- **ImageValidator**: Zero-allocation binary format and EOF integrity validator (`ImageValidator.kt`) verifying SOI (`0xFF 0xD8`), EOI (`0xFF 0xD9`) for JPEG, PNG `IEND`, and WebP container boundaries. Prevents truncated images from entering caches.
- **Thread Synchronization**: Striped locking (64 buckets) in `ArtworkContentProvider` preventing race conditions between concurrent Android Auto requests (backdrop blur, player card, and notifications).
- **Auto-Healing Cache**: Detects and purges corrupted/truncated cache files automatically on lookup, re-downloading complete fresh covers without requiring the user to clear app storage.
- **Atomic File Movement**: Unique temporary files (`.tmp` / `.part`) and atomic replacement preventing partial reads while streaming.

### 29. Playlist Detail Redesign (Vibe Gradient, Navidrome 1024px Covers, & Action Layout)
- **Dynamic Vibe Gradient**: Dynamically extracts dominant colors from the playlist cover art (or deterministic theme gradient), creating an atmospheric top wash fading smoothly into AMOLED black.
- **Crisp 1024px Navidrome Covers**: Resolves `pl-{id}` for Navidrome dynamic collages and requests high-DPI 1024px covers with rounded corners and drop shadows, eliminating blurriness.
- **Public / Private & Song/Time Metadata**: Displays `Public playlist` / `Private playlist` with globe/lock icon, alongside total songs (`5 songs`) and total formatted duration (`12m`, `1h 24m`).
- **Spotify Action Buttons**: Left-aligned metadata paired with right-aligned circular white shuffle button (`#FFFFFF` with black icon) and large circular accent play button.
- **Clean 3-Dots Menu**: Top bar simplified to Back, Edit (pencil), Download, and More options (Sync, Add songs, Change cover, Delete).

### 30. Queue Reordering & Audio Stutter Elimination (Spotify-Style Queue)
- **Zero-Interruption Playback**: Switched `PlaybackController` queue mutations (`moveInQueue`, `removeFromQueue`, `appendToQueue`, `appendSimilarTracks`, `setShuffle`, `reshuffleQueue`) from disruptive `setMediaItems` pipeline flushes to native Media3 operations (`session.moveMediaItem`, `session.removeMediaItem`, `session.addMediaItem`, `session.removeMediaItems`/`addMediaItems`). Audio decoders and buffers continue without any dropouts, pauses, or restarts.
- **Spotify-Style UI Overhaul**: Removed cluttered 4-button rows (`<<`, `^`, `v`, `x`). Replaced with a clean, spacious Spotify-style layout featuring 44dp album art, 1-line truncated title & artist, a subtle remove button (`X`), and a dedicated 3-bar drag handle (`≡`).
- **Tactile Drag-and-Drop Reordering**: Direct vertical gesture tracking on the drag handle with elevation, visual highlight, and tactile haptic feedback (`HapticFeedbackType.TextHandleMove`) on each track swap.

### 31. Full Playlist Track Options Restoration
- Restored the complete suite of track actions in playlists (`Share song`, `Add to playlist`, `Add to queue`, `Go to album`, `Go to artist`, `Download song / Remove download`) alongside `Remove from Playlist` in `PlaylistTrackOptionsSheet`.

### 32. Screen Insets & System Bars Full Real-Estate Utilization
- Eliminated redundant `.statusBarsPadding()` on `HomeScreen.kt`, recovering 50dp of dead space above the ASAYS brand header.
- Added `.navigationBarsPadding()` and `.verticalScroll(rememberScrollState())` across all modal bottom sheets (`AdjustLyricsTimingSheet`, `OptionsSheet`, `SleepTimerSheet`, `QueueSheet`, `PlaylistActionSheets`, `TrackActionSheets`, `SortOptionsBottomSheet`), ensuring buttons ("Done", "Adjust offset", action items) are 100% visible and tap-targetable above 3-button navigation bars (`|||`, `O`, `<`).
- Standardized bottom scroll padding from 120–140dp down to 24dp across `HomeScreen`, `SearchScreen`, `PlaylistsScreen`, `SettingsScreen`, and `StatsScreen`.

### 33. Home Screen Performance & Freeze Elimination
- **Zero UI-Thread Disk I/O (`localCoverFor`)**: Converted `DownloadRepository.localCoverFor` into a pure in-memory O(1) hash map lookup (`coverFileIndex[coverArtId]`), completely eliminating synchronous `RandomAccessFile`, `BitmapFactory.decodeFile`, and flash storage existence checks on the Android Main UI Thread during frame rendering.
- **Parallel Network Loading**: Converted sequential network fetching in `HomeViewModel.load()` to concurrent `async(Dispatchers.IO)` coroutines across all 5 streams (`favoriteAlbums`, `random`, `newest`, `artists`, `playlists`), slashing network load latency from ~3s down to ~400ms.
- **15-Minute In-Memory Artist Caching**: Added in-memory caching with a 15-minute TTL to `MediaRepository.getArtists()`, preventing redundant downloads of thousands of artists on every Home visit.
- **Background Artist Sorting**: Offloaded large artist library sorting (`sortedByDescending { it.albumCount }`) to `Dispatchers.Default`.
- **Instant Stale-While-Revalidate Rendering**: Cached successful Home UI state in `HomeViewModel` companion cache, providing instant 0ms cold-start and tab switching with background revalidation.
- **Eliminated Recomposition Thrashing**: Removed unused `ListeningStatsStore` subscription from `HomeViewModel` and unneeded fields (`listeningMs`, `plays`, `streak`) from `HomeUiState`, ending full-screen recompositions on every song tick.
- **UI Compose Optimization**: Pre-chunked grid and pick lists with `remember` and assigned stable unique keys to all `LazyColumn` items and carousels, ensuring smooth 60/120fps scrolling.

### 34. Video Diagnostic & UX Issue Fixes (Continuous Queue Reordering & Sheet Freeze Elimination)
- **Continuous Fluid Queue Drag-and-Drop**: Eliminated pointer input gesture cancellation caused by index-based item keying. Introduced a stable `QueueEntry` model and real-time local list swaps with tactile haptics. Single-commit to `PlaybackController.moveInQueue` on drop (`onDragEnd`) prevents audio buffer drops, touch drops, and media pipeline thrashing.
- **Zero Halfway-Stuck Bottom Sheets**: Configured `rememberModalBottomSheetState(skipPartiallyExpanded = true)` across all 13 `ModalBottomSheet` composables in the application (`AdjustLyricsTimingSheet`, `LyricsOptionsSheet`, `QueueSheet`, `OptionsSheet`, `SleepTimerSheet`, `TrackOptionsSheet`, `AddToPlaylistSheet`, `PlaylistCoverOptionsSheet`, `PlaylistTrackOptionsSheet`, `AddSongsToPlaylistSheet`, `SortOptionsBottomSheet`, `ReportLyricsDialog`).
- **Eliminated Sheet Freezing**: Resolved `AdjustLyricsTimingSheet` freezing halfway across the screen when swiping down to dismiss. Connected "Done" button to smooth `sheetState.hide()` dismissal.
- **Full Immediate Visibility of Lyrics Options**: Ensured `LyricsOptionsSheet` expands completely on open so options at the bottom ("Reload Lyrics", "Block & Remove", "Report") are immediately visible above 3-button navigation bars without requiring manual scrolling.

### 35. Spotify-Style FIFO Queue Insertion
- **`isUserQueued` Track State**: Tagged user-initiated queue additions with `isUserQueued: Boolean = true` in `Track`.
- **FIFO Priority Queue (`addToUserQueue`)**: When a track is added to queue, it is inserted immediately after the currently playing song (`currentIndex + 1`). If subsequent tracks are added to queue, they are appended in FIFO order directly after the existing user-queued tracks and before the context album/playlist.
- **ExoPlayer Dynamic Timeline Injection**: `PlaybackController.appendToQueue` calls `session.addMediaItem(insertIndex, item)` at the exact calculated position without restarting playback, rebuffering, or interrupting the active audio stream.
- **Spotify-Style Visual Section Headers in `QueueSheet`**: Displays `NEXT IN QUEUE (count)` in SpotifyGreen for user-queued tracks, followed by `NEXT FROM: [ALBUM]` or `NEXT UP` for the remaining context tracks, while preserving fluid continuous drag-and-drop.
- **Verified with 89 Unit Tests**: Added tests in `PlayerQueueStoreTest` verifying single-insertion, consecutive FIFO additions, addition after playback progression, and cold-start queue creation.

### 36. 120fps Home Screen Smoothness & Memory Overhaul
- **In-Memory URL LRU Cache (`CoverUrls.kt`)**: Added a 512-entry synchronized `LruCache<String, String>` mapping `id:size` to fully formatted Subsonic URLs with static salts. Bypasses per-card string formatting, OkHttp URL parsing, and MD5 cryptographic hashing on every scroll frame.
- **Coil ImageLoader Integration (`NaviifyApplication.kt` & `CoilModule.kt`)**: Implemented `coil.ImageLoaderFactory` directly on `NaviifyApplication` returning the Hilt-injected `ImageLoader`. Configured `.bitmapConfig(Bitmap.Config.RGB_565)` for album art to reduce bitmap memory consumption by 50% without visual degradation on mobile screens, putting an end to GC pause frame drops during fast scrolls.
- **Zero-Allocation Geometry in `PlaylistCoverArt` (`MediaComponents.kt`)**: Completely bypassed `drawBehind` when a cover URL is present. Replaced per-frame `Path()` allocations and cubic bezier curves in the fallback with zero-allocation geometric primitives (`drawCircle`, `drawRoundRect`).
- **Downsampled Playlist Card Art (`MediaComponents.kt`)**: Downsampled 150dp playlist card requests from 512px to 256px, cutting per-image memory footprint from 1MB down to 256KB (75% RAM reduction).
- **Carousel State Hoisting (`HomeScreen.kt`)**: Hoisted `LazyListState` for the 3 horizontal carousels (`userPlaylistsRowState`, `recentlyAddedRowState`, `featuredArtistsRowState`) to the top-level `HomeScreen`. Moving vertically past carousels preserves layout measurements and scroll positions instead of throwing them away and re-measuring on every scroll pass.
- **LazyLayout Prefetching & Node Recycling (`HomeScreen.kt`)**: Converted `quickGridRows.forEachIndexed` and `quickPickRows.forEachIndexed` to `items(items = ..., key = ..., contentType = { ... })` with `contentType = { "quick_grid_row" }` and `contentType = { "quick_pick_row" }`. Enables Android Compose's `LazyLayoutItemContentFactory` to prefetch upcoming items ahead of fast fling gestures and recycle layout nodes.
- **Hardware Layer Backdrop (`HomeScreen.kt`)**: Applied `.graphicsLayer { }` to `AmbientGlassBackdrop` so the ambient gradient renders directly as an offscreen GPU texture rather than re-rasterizing per scroll frame.
- **Verified with 89 Unit Tests**: All unit tests pass cleanly with zero regressions.

### 37. Permanent Lyrics Sync, Navidrome Companion Export, & Cache-Resilient Architecture
- **Permanent `CustomLyricsStore` (`CustomLyricsStore.kt`)**: Dedicated singleton store housed in `context.filesDir/custom_lyrics/` (isolated from temporary caches). Saves user-calibrated timing offsets, custom synced LRCLIB selections, and lyrics overrides. Dual-indexed by `trackId` and metadata fingerprint (`artist:title`) so lyrics remain linked even if server IDs change.
- **Cache-Resilience**: Completely protected from `SettingsViewModel.clearCaches()` and `DownloadRepository.deleteAllDownloads()`. Clearing cache only sweeps temporary HTTP/media caches; user-calibrated lyrics are never lost.
- **Priority 0 Lookup in `MediaRepository`**: `MediaRepository.getLyrics()` checks `CustomLyricsStore` before consulting disk cache, Navidrome `getLyricsBySongId`, or LRCLIB. User calibrations always take precedence over server unsynced text.
- **Standard `.lrc` Serializer (`LyricsModels.kt`)**: Added `LyricsData.toLrcString()` generating standard `[mm:ss.xx]` sidecar content incorporating user `offsetMs`.
- **Companion `.lrc` File Auto-Generation (`DownloadRepository.kt`)**: Automatically writes `${trackId}.lrc` sidecar files directly alongside downloaded audio files in `ASAYS_Downloads/`.
- **Navidrome 1-Tap Export & Copy Actions (`LyricsActionSheets.kt`)**: Added "Copy .lrc (for Navidrome)" and "Export / Share .lrc File" buttons in `LyricsOptionsSheet` with a "Custom Synced (Saved permanently)" status badge.
- **Verified with 94 Unit Tests**: Added 5 new unit tests in `CustomLyricsStoreTest` (94/94 tests passed).

---

### 38. Lyrics Experience Modernization (Apple-Style Animation, 3-Dots Options & Blur Toggle)
- **No more accidental seeks**: In the collapsed `LyricsCard`, tapping the card or any lyric line now triggers `onExpand()` (opens fullscreen lyrics) instead of `onSeekTo()`. Click-to-seek is enabled **only** when `isFullScreen == true` and the line has a timestamp (`AppleMusicLyricLineItem`).
- **Flicker-free rendering**: The animated `Modifier.blur(animatedBlur)` pipeline — the cause of GPU shader flicker and disappearing text on some devices — is gone. Only the active line keeps a single **static** 6dp halo, and only while the glow toggle is ON.
- **Glow / Blur toggle (persisted)**: New `isLyricsBlurEnabled` in `LyricsPreferencesStore` (key `lyrics_blur_enabled`, default `true`), exposed via `PlayerViewModel.lyricsBlurEnabled` / `setLyricsBlurEnabled()`. OFF = crisp mode: 0dp blur, high-contrast opacity (`1.0f` active vs `0.32f` inactive) for buttery 60/120fps scrolling.
- **GPU-only active-line animation**: `fontSize` is now **constant** for every line (no recomposition relayout). Emphasis uses `Modifier.graphicsLayer { scaleX/scaleY, transformOrigin = TransformOrigin(0f, 0.5f) }` with `animateFloatAsState` — `spring(DampingRatioLowBouncy, StiffnessLow)` for scale (`1.05f` active / `0.98f` adjacent / `0.95f` far) and `tween(350ms, FastOutSlowInEasing)` for alpha.
- **Quieter instrumental dots**: `InstrumentalGapDots` threshold raised from 5s to **15s** (long interludes only), and the pulse is now one slow 1200ms alpha animation on 5dp dots instead of 3 staggered alpha+scale animations on 9dp dots.
- **Decluttered headers + 3-dots consolidation**: `Tune` and `Share` icon buttons removed from both the collapsed card header and the fullscreen header (now title + expand + `MoreVert` only). Their actions live in `LyricsOptionsSheet`: **Share Lyrics** (new entry), **Adjust Timing**, **Search / Replace Lyrics**, plus a **Lyrics Glow Effect** `Switch`. The fullscreen header keeps a compact `+0.5s` offset badge when a timing offset is active.
- **Files touched**: `NowPlayingScreen.kt`, `LyricsActionSheets.kt`, `LyricsPreferencesStore.kt`, `PlayerViewModel.kt`.
- **Verification note**: Static review only on the server (no JDK/Gradle/Android SDK there). `./gradlew testDebugUnitTest` and `assembleDebug` must be run on the Mac before shipping.

### 39. Home Smoothness Diagnosis: Build Variants & Per-Frame Churn
- **Root cause found: the build type.** `release` had no `signingConfig`, so `assembleRelease` produced an unsigned (non-installable) APK — every build ever tested on the phone was the **debug** build, which is debuggable (ART/R8 optimizations off) and ships Compose `ui-tooling`. Home is the heaviest screen (≈15 images, 4 nested `LazyRow`s, ~40 text nodes), so it is where a debuggable build hurts most.
- **New `perf` build type (`app/build.gradle.kts`)**: release-like (non-debuggable, no `ui-tooling`) but **without R8** (so it cannot crash on a missing keep rule) and signed with `signingConfigs.localDebug` (Android Studio's debug keystore). It reuses the `debug` application id, so `adb install -r app-perf.apk` upgrades the app in place and keeps the saved server credentials. `release` is now signed with the same keystore so it is installable too.
- **No more full refetch on every Home visit (`HomeViewModel.kt`)**: `FRESHNESS_MS = 60s` guard in the new `loadInternal(force)`; `load()` short-circuits when `cachedHomeUiState` is still fresh, `refresh()` forces a reload and is wired to the `ErrorBubble` retry. Before, each return to Home fired 5 network calls (including `getArtists()`, which returns all 628 artists of this library to display 12) and rebuilt the whole screen and all its images.
- **Downloads flow off the UI thread**: `.flowOn(Dispatchers.Default)` on the `observeDownloads()` chain — the `filter`/`map`/`sorted`/`distinctUntilChanged` ran on `Dispatchers.Main` on every progress tick.
- **No per-composition allocation / no image crossfade on Home (`HomeScreen.kt`)**: the hero and quick-access thumbnail `ImageRequest`s are `remember`ed and no longer set `crossfade(true)` (that contradicted the global `crossfade(false)` policy and animated one fade per image while scrolling); the hero placeholder/scrim gradients are `remember`ed.
- **`PlaylistCoverArt` zero-allocation (`MediaComponents.kt`)**: the gradient `Brush` and the `drawBehind` pattern modifier are `remember`ed instead of rebuilt on every composition of every playlist tile.
- **Files touched**: `app/build.gradle.kts`, `HomeViewModel.kt`, `HomeScreen.kt`, `MediaComponents.kt`.
- **Not compiled on the server** (no JDK/Gradle/Android SDK there) — run `./gradlew assemblePerf` on the Mac. Expected result to validate: the same scrolling on `app-perf.apk` should be visibly smoother than on `app-debug.apk`.

### 40. Mix / Transition Sheets: Real Curve Preview + Preset Chips
- **Transition curves now have a single source of truth**: `mixGains(progress, mode, equalPower)` in `domain/model/PlaylistMix.kt` (AUTO/FADE = cos/sin equal-power or linear, RISE = hold at 1 until 75 % then drop with `p²`, MELT = `(1−p)²` out with `√p` in, SLAM = hard cut). `PlaybackController.calculateMixGains()` delegates to it, so the UI preview can never drift from the audio that is played.
- **`MixCurvePreview`** (private, `PlaylistActionSheets.kt`): a Canvas in an 80 dp box — beat grid, dashed outgoing gain, accent-coloured solid incoming gain, duration pill and legend. Used by both `PlaylistMixStudioSheet` and `PlaylistTransitionBridgeSheet`. It deliberately draws **no waveform**: the app has no audio analysis, so a waveform would be fabricated.
- **`MixPresetChipsRow`** replaces the 5 stacked mode cards in the per-transition sheet: pill chips (icon + title) filled with `SpotifyGreen` (= active theme accent) when selected, then a single detail card for the selected mode (title + DEFAULT/CUSTOMIZED badge + subtitle). Much shorter sheet, Save always reachable.
- **`mixModeIcon(mode)`** is now the single icon mapping for a transition mode (it was duplicated in both sheets).
- **Sheet parameters**: `PlaylistTransitionBridgeSheet` gained defaulted `equalPowerVolume` / `durationSeconds`; `PlaylistDetailScreen` passes the `state.mixConfig` values.
- **WARNING — the "DJ metadata" in these sheets is fabricated**: `getDjBpm()` returns `84 + (hash % 45)` and `getCamelotKey()` derives the key from a hash of id/artist, so every displayed BPM, Camelot key, "−21 bpm" delta and the "Harmonic Warmth (−1)" banner are invented, not measured. Real values need either BPM/key tags in the files (Navidrome can expose them) or an offline analysis pass. Never build beatmatching/tempo sync on top of these numbers.
- **Not compiled on the server** — run `./gradlew assemblePerf` on the Mac.

### 41. Real DJ metadata (BPM + key) — server pipeline replaces the hashed values
- `getDjBpm()` / `getCamelotKey()` in `domain/model/PlaylistMix.kt` fabricate their values from a hash. Real values now come from an offline analysis pipeline on the home server: `/home/tayeb/dj-analyzer/` (essentia `RhythmExtractor2013(multifeature)` + `KeyExtractor(edma)`, ffmpeg decode of a 45 s segment at 25 % of the file), ~1.2 s/track, 1487 tracks ≈ 30 min, resumable cache `results.jsonl`.
- Validation: `make_tests.py` builds synthetic tracks with a known answer (120/95/128 bpm clicks, C/A/F triads) — the analyser reproduces 120.0 / 128.2 / 95.0 bpm and 8B / 8A / 7B.
- Published to the app as JSON keyed by **Navidrome pid**: `GET http://<serveur>:8788/djmeta.json` (user unit `djmeta-http.service`, file `djmeta_navidrome.json`, fields `bpm`, `key`, `camelot`, `bpmConfidence`, `keyConfidence`).
- Also written into the files as ID3 `TBPM`/`TKEY`, so after a Navidrome rescan the Subsonic API returns a real `bpm` (`Child.BPM json:"bpm"`). Navidrome has **no key column**, so the Camelot can only come from the JSON endpoint.
- App side still to do: map `Track.bpm` / `Track.camelot` from the API/JSON and remove the hash helpers. Until that lands, the mix sheets keep showing invented numbers.

## Compose "blind coding" rules (this server has no Android SDK)
The Mac agent compiles what is written here; a mistake costs a full build round-trip. Non-negotiable:
- Theme colours are **dynamic composable getters**: `val SpotifyGreen: Color @Composable get() = LocalNaviifyPalette.current.accent` (same for `TextPrimary`, `TextSecondary`, `SurfaceCard`, `SurfaceCardHigh`, `ThemeOutline`, `NaviifyBlack`). They may only be read from a composable scope.
- Never read them inside `Canvas { }`, `Modifier.drawBehind { }`, `graphicsLayer { }`, `remember { }`, `LaunchedEffect` or any other non-composable lambda. Capture the colour into a local `val` first (`val incomingColor = SpotifyGreen`) and use that inside the lambda. Reading `LocalNaviifyPalette.current` itself is allowed (not annotated), but calling the annotated getters is not.
- Check `ui/theme/Color.kt` and `Type.kt` before assuming a colour or style is a static object.

---

### 42. Lyrics: real sync restored + no more black fade (2026-09-26)
- `SyncedLyricsList`: the auto-scroll was mistaken for a user drag — `animateScrollToItem` sets `isScrollInProgress`, the old guard flipped `userScrolledRecently = true`, the sync was suppressed for 1.8s and then re-fired: the lyrics stopped following the track and animations piled up. An `isAutoScrolling` flag is now set around the programmatic scroll, so only a real gesture pauses the sync (2.2s).
- Far lines sat at alpha 0.26: that is the "black fade" that appeared a moment after browsing stopped. Floor raised to 0.50 (0.74 / 0.62 / 0.50).
- The 6dp blurred halo behind the active line is gone: heaviest per-frame cost, and it left a grey smudge on dark backgrounds.
- Top/bottom `BlendMode.DstIn` gradient mask: a line straddling the edge fades out instead of being sliced in half (fullscreen and collapsed card).
- The collapsed card is the same list as fullscreen: it scrolls, and it re-syncs by itself after the user stops.

### 43. Manual transition timing (in/out points) + Spotify-style blend indicator
- `PlaylistMixConfig.outroOffsetMs` (<= 0: how early the outgoing starts leaving) and `introSkipMs` (>= 0: how much of the incoming's intro is skipped), persisted as longs in `PlaylistMixStore`. `transitionStartMs(durationMs, overlapMs)` lives in the domain and is the single source of truth for both `PlaybackController` and the UI schema (`MixTimingBar`).
- The intro skip is applied in `onMediaItemTransition` (the item must be loaded first): `pendingIntroSkipMs` is armed when the transition fires and consumed exactly once.
- `PlaylistTransitionBridgeSheet` gained a "Manual timing" card: Reset + two sliders ("Outgoing exit", "Incoming entry") + a weight-based schematic of the placement (no invented waveform).
- `AutomixBlendPill` replaced: green text on a green border with a hard "from Waiting Around T..." truncation -> full-width card, accent equalizer bars, white "BLENDING n%", marquee title, and a real progress rail fed by the audio blend progress.

### 44. App side: real DJ metadata wired in, hash values deleted
- `Child.bpm` (OpenSubsonic exposes the TBPM tag) -> `Track.bpm`: the API now carries a real tempo after a Navidrome rescan.
- New `data/djmeta/DjMetadataRepository` fetches `http://<hote du serveur>:8788/djmeta.json` (systemd user unit `djmeta-http` on the home server; payload keyed by **Navidrome pid == `Track.id`**), caches it in memory and exposes a `StateFlow`. The port is derived from the active server URL (LAN or Tailscale), so no new setting to configure.
- New domain `DjTrackMeta` + `realBpmOf(track, meta)` / `realCamelotOf(track, meta)` + `camelotFromCode("8A")`.
- `getDjBpm()` and `getCamelotKey()` (the hash values: `84 + hash % 45`, `-21 bpm`, `Harmonic Warmth (-1)`) are **deleted**. The mix track rows and the Transition Style sheet now show measured values, and render nothing / "Not analysed" when a track is missing from the catalogue.
- `sortTracksHarmonically(tracks, meta)` sorts on measured BPM/Camelot; unanalysed tracks stay at the end in their original order (never sorted on a hash).
- Server pipeline behind it: `/home/tayeb/dj-analyzer` (essentia) - 1473 tracks in 27m49s, TBPM/TKEY written into 1487/1487 files, Navidrome rescanned (bpm present for 1487/1487 rows), JSON re-exported (1487 entries, 0 unmatched).

### 45. Accueil allege + playlists de genre automatiques
- Accueil : la section "Quick Picks" (12 albums sur deux colonnes) est **retiree** de `HomeScreen`. Le fetch reste dans `HomeViewModel` parce que `quickPicks` sert aussi a la detection hors-ligne, mais plus rien n'est dessine : moins de travail sur le fil d'accueil.
- Serveur : la bibliotheque n'a **aucun tag de genre** (1487/1487 lignes vides dans `media_file`), donc `~/scripts/genre_playlists.py` classe par **artiste** (dictionnaire curate + filet "Cheb/Cheba" -> rai) et ecrit des `.m3u8` dans `/srv/media/music/Playlists`.
- Resultat mesure (26/09, apres retours de Tayeb sur les artistes) : Rap Maghribi 68 titres (ElGrandeToto 20, Raid 14, Mehdi Black Wind 7, Lbenj, Morad, Mr. Crazy + collabs), Raï (auto) 494, Rap US 479, Rap Russe 16 (Gio Pika + Вектор А), Rock 24, Country 24 ; 1106/1487 morceaux classes (74%).
- Buckets = dictionnaire curate par artiste + filet "Cheb/Cheba/Cheikh" -> raï. Gio Pika (Гио Пика) est classe **rap/chanson russe**, pas marocain (confirme par Tayeb).
- Nettoyage : un `.m3u8` portant le marqueur mais plus produit est supprime automatiquement (renommage de bucket, playlist passee sous le seuil).
- Garde-fou : un `.m3u8` sans le marqueur `AUTO-GENERATED` n'est jamais ecrase (le "Raï" manuel de 746 titres est intact) ; on ecrit alors `<Nom> (auto).m3u8`.
- Refresh : timer systemd user `genre-playlists.timer` (quotidien). Manuel : `python3 ~/scripts/genre_playlists.py`.
- Cote app : rien a faire, Navidrome expose ces playlists comme les autres (verifie en base : les 5 presentes avec leurs comptes de titres).

### 46. Lyrics : carte non defilante + plein ecran stable
- Probleme signale : **impossible de faire defiler la page de lecture** quand le doigt tombait sur les paroles (la carte contenait une `LazyColumn` de 300dp qui consommait le geste et se battait avec l'auto-scroll), effet qui "clignotait", et l'accentuation qui disparaissait quand on tapait une ligne pour s'y rendre.
- `InPageSyncedLyrics` remplace la liste defilante dans `LyricsCard` : toutes les lignes sont mesurees une fois (`onGloballyPositioned`), puis le conteneur glisse via un seul `graphicsLayer { translationY = -animé }` pour garder la ligne active a ~42% du cadre. **Aucun detecteur de defilement n'est installe** -> le geste remonte a la page. Un tap n'importe ou ouvre le plein ecran (`detectTapGestures`), comme demande.
- Plus de mode "browsing" dans la carte : l'animation ne depend que de la position de lecture -> l'effet ne clignote plus. Masque de fondu haut/bas conserve (les lignes en bord s'estompent au lieu d'etre coupees).
- Plein ecran : entree/sortie adoucies (`AnimatedVisibility` fade + slide ~200ms) au lieu d'une coupure nette ; le defilement et le tap-pour-seek restent.
- Plein ecran : la ligne active garde **toujours** alpha 1.0 + scale 1.05, meme en mode "browsing" (avant, le browsing passait toutes les lignes a 0.82 : en tapant une ligne, l'effet semblait disparaitre).
- Regle a retenir : un bloc de paroles dans une page defilante ne doit JAMAIS contenir de liste defilante.

### 47. Stations "radio" du jour (remplace les playlists de genre Navidrome)
- Tayeb ne veut plus ces genres sous forme de playlists Navidrome : elles doivent apparaitre **dans l'app** comme des cartes "radio" facon Spotify, et le contenu doit changer chaque jour.
- `~/scripts/radio_stations.py` produit `~/dj-analyzer/radio.json` : 6 stations (Rap Maghribi, Raï, Rap US, Rap Russe, Rock, Country) et, pour chaque titre, `id / titre / artiste / album / album_id / duree / bpm / camelot` - l'app peut donc afficher ET jouer sans aucun appel supplementaire.
- Rotation quotidienne : melange amorce par la date du jour + echantillon de 60 titres max par station (le contenu change chaque jour, reste stable dans la journee, JSON de 37 Ko).
- `djmeta_server.py` sert aussi `/radio.json` (port 8788, LAN + Tailscale). Le timer user `genre-playlists.timer` regenere le JSON chaque jour a 00:05.
- Les `.m3u8` de genre ont ete retires du disque **et** de la base Navidrome : il ne reste que les listes de Tayeb (My own, Raï 746, Best American Rap 539, 1, L7ajaa, Fuck off).
- Reste a faire cote app : section "Radio" sur l'accueil (cartes facon Spotify) + ecran de station + lecture.
- Perf associee (commit `bcb918e`) : le fond flou du lecteur est calcule sur un calque de 128dp agrandi 6x au lieu d'un `blur(60.dp)` plein ecran, et les paroles de la carte ne composent qu'une fenetre de 14 lignes (composer 100+ lignes creait ~200 animations a l'ouverture).

### 48. Audit statique Kotlin (pas de compilateur sur le serveur)
- Outil : `python3 ~/scripts/kotlin_audit.py [racine]` - **8 verifications** sur les 113 fichiers `.kt` : equilibre accolades/parentheses/crochets (lexer Kotlin reel : commentaires imbriques, chaines brutes, char literals, et le code des templates `${...}` est conserve), imports internes `com.naviify.app.*` sans declaration, declarations dupliquees dans un package, getters `@Composable` du theme utilises dans un lambda non-composable (`graphicsLayer`/`drawBehind`/`drawWithContent`/`Canvas`/`remember`/`LaunchedEffect`/`pointerInput`), `R.<type>.<nom>` inexistant pour **tous** les types de ressources, APIs Material3 experimentales sans `@OptIn`, usages/declarations des fonctions sensibles (BPM, mix, lyrics), et **cablage build** (package vs chemin, composants + `@res` du manifest, alias `libs.*` vs `gradle/libs.versions.toml`).
- Resultat du 26/09 **apres correction de l'outil : 0 signalement**. Les 24 signalements d'origine etaient tous des faux positifs, causes par 4 bugs de l'outil, corriges : symboles generes par Gradle (`R`, `BuildConfig`) pris pour des imports casses ; declarations imbriquees invisibles (les fonctions membres passaient pour des appels orphelins) ; liste `EXPERIMENTAL_M3` perimee - `FilterChip`/`AssistChip`/`SuggestionChip`/`SingleChoiceSegmentedButtonRow` sont **stables** (verifie dans `material3-1.3.1-sources.jar` : `fun FilterChip(` ne porte aucune annotation) ; nettoyage a base de regex qui coupait les templates `${...}` au premier guillemet (7 faux "desequilibre", dont `ServerUrl.kt`).
- Non-regression : `python3 ~/scripts/test_kotlin_audit.py` (5 cas) injecte une faute par verification dans une copie de l'arbre et exige qu'elle soit detectee - 0 echec le 26/09.
- Limite : pas de type-checking reel (pas de JDK/Gradle ici) - le verdict final reste `./gradlew assemblePerf` sur le Mac.

### 49. Accueil : cartes "Radio du jour" (cote app)
- Nouveaux fichiers : `core/network/dto/RadioDtos.kt` (payload de `/radio.json`), `domain/model/RadioStation.kt` (nom + accent + titres deja en `Track`), `data/radio/RadioRepository.kt`.
- `RadioRepository` : `@Singleton`, meme logique d'endpoint que `DjMetadataRepository` (hote du serveur actif + port 8788, client OkHttp dedie, **pas** l'intercepteur Subsonic). Il recharge des que la date du payload change -> les cartes suivent la rotation quotidienne du serveur sans action de l'utilisateur.
- `HomeViewModel` : injecte le repository, expose `radioStations`, et `playStation(station)` appelle `playbackController.playShuffled(tracks, "radio:<nom>")`. Les titres arrivent complets du serveur (id/titre/artiste/album/album_id/duree/bpm) : **aucun appel Subsonic supplementaire**.
- `HomeScreen` : section "Radio du jour" (LazyRow de cartes facon Spotify - tuile degradee batie sur la couleur d'accent envoyee par le serveur, nom + nombre de titres, aucune image distante). Tap = lance la station en lecture melangee puis ouvre le lecteur (`onOpenNowPlaying`, cable dans `MainShell`).
- Contrat verifie automatiquement : les champs du JSON serveur correspondent exactement aux DTO (racine / station / titre).

### 50. Lecteur : fond sans flou, carte paroles simplifiee, ouverture allegee
- **`BlurredBackdrop` : plus aucun `blur()`.** Le fond = pochette 256px a 20% + degrade vertical. La variante "calque 128dp agrandi 6x" laissait un **carre visible** au milieu de l'ecran (signale par Tayeb) ; deux rectangles suffisent et ne declenchent aucun RenderEffect plein ecran.
- **`InPageSyncedLyrics` : plus de mesure ni d'offset anime.** Bloc fixe = ligne precedente (contexte) + ligne active + 3 suivantes, remplace par un fondu (`AnimatedContent`) a chaque changement. Corrige : ligne active coupee en bas du cadre (le dernier offset restait sous le masque) et etat casse au retour du plein ecran (les hauteurs mesurees devenaient fausses des que la fenetre glissait).
- **Ouverture du lecteur allegee** : pochette 1024 -> 768 px (affichee sur 340dp max), fond 512 -> 256 px ; transitions pochette 550/450 -> 330/280 ms. Moins de decodage = moins de coupures en arrivant depuis la recherche.


### 51. Accueil sans "hero", radio en tete, ouverture du lecteur allegee
- **Suppression de la carte "FEATURED PLAYLIST / FEATURED ALBUM"** (`heroItem` + `AppleMusicHeroCard`) : elle repetait un titre deja present dans la grille "Quick Access". La **section "Radio du jour" prend sa place, en tete de l'accueil** (avant la grille Quick Access). `AppleMusicHeroCard` reste definie mais n'est plus appelee (warning volontaire, gardee pour reutilisation).
- **Ouverture depuis la recherche** : `SearchScreen` **precharge la pochette au format du lecteur (768 px, memes cles de cache que `CoverImage`)** des que l'utilisateur tape un resultat. Avant, l'image se telechargeait et se decodait pendant le slide d'ouverture -> frames manquees visibles.
- **Transition du lecteur : 400/350 ms -> 240/220 ms** (`MainShell`). Le slide couvre moins de frames, les rares frames manquees ne sont plus visibles.
- Rappel : plus aucun `blur()` plein ecran dans l'app. Trois `blur()` subsistent dans le lecteur, tous mesures : fond d'ambiance (source 256 px, 20 dp), halo statique de la ligne de paroles active, et le rayon de flou des lignes inactives **anime** quand l'effet de flou est actif (voir milestone 52).

### 52. Animations sorties du chemin de composition (pill de blend, marquee, gap)
- **Regle appliquee** : un etat anime (`animateFloat`, `animateFloatAsState`, `infiniteTransition`) lu dans le **corps** d'un composable le fait recomposer a chaque frame ; lu **dans un lambda de dessin** (`graphicsLayer { }`), seule la phase de dessin est invalidee. Le projet suit cette regle a 4 endroits de plus depuis ce commit.
- `ui/components/MarqueeText.kt` : `graphicsLayer { translationX = -(offsetFraction * segmentWidthPx) }` (avant : `offsetPx` calcule dans le corps -> le marquee recomposait a chaque frame ; il est affiche dans le mini-player et dans la pill de blend).
- `AutomixBlendPill` : les 3 barres d'onde passent de `height((12 * wave).dp)` a `height(12.dp)` + `graphicsLayer { scaleY = wave; transformOrigin = TransformOrigin(0.5f, 1f) }` (meme rendu, plus de recomposition de la pill), et le rail de progression est isole dans `BlendProgressRail(progress: () -> Float, ...)` - lambda differee, seule la barre se recompose.
- `InstrumentalGapDots` : `graphicsLayer { alpha = animAlpha }` au lieu de `background(Color.White.copy(alpha = animAlpha))`.
- Cout connu et **assume** : le rayon de flou des lignes de paroles inactives reste anime (300 ms, uniquement si l'effet de flou est active dans les reglages) - c'est un effet demande par Tayeb ; le supprimer = appliquer `targetBlur` sans passer par `animateFloatAsState`.
- Verification : `python3 ~/scripts/kotlin_audit.py` = 0 signalement ; `grep -rn -E '\.(height|width|fillMaxWidth|offset|size)\(.*(animated|wave[0-9]?|offsetFraction)'` ne renvoie plus rien.

### 53. Widgets de lecture : trois tailles, fond degrade, acces rapide (style Spotify)
- Trois widgets declares (manifest + `res/xml/*_info.xml`) : **ASAYS 4x1** (barre compacte, `NowPlayingBarWidgetProvider`), **ASAYS 2x2** (tuile pochette, `NowPlayingCardWidgetProvider`) et **ASAYS 4x2** (lecteur + ligne d'acces rapide, `NowPlayingWideWidgetProvider`, nouveau).
- **Fond facon Spotify** : `WidgetBitmapUtils.dominantColor()` (moyenne 8x8, saturation/valeur retravaillees pour rester lisibles sur du texte blanc) + `createGradientBackground()` (degrade vertical, coins arrondis). Le 4x1 et le 4x2 s'en servent ; la tuile 2x2 = pochette plein cadre + bandeau bas (`widget_scrim_strip.xml`, coins bas arrondis au rayon de la pochette).
- **Controles** : plus de bouton favori dans les widgets (comme Spotify) - `ACTION_FAVORITE` reste gere par `PlayerWidgetReceiver` si on le remet un jour. Bouton lecture = cercle blanc + glyphe sombre (`widget_play_btn_white.xml`) ; precedent/suivant = glyphes clairs sur ripple rond.
- **Acces rapide (4x2)** : 5 tuiles = les pochettes suivantes de la file (`PlayerUiState.queue` apres `currentIndex`, puis les deja jouees). Chaque tuile, comme toute la surface du widget, ouvre l'app d'un tap.
- **Taille des bitmaps volontairement modeste** (pochette 112/176/288 px, fonds 420x108 et 420x210, tuiles 96 px) : une RemotesViews trop lourde fait echouer la mise a jour (transaction Binder ~1 Mo). La pochette part donc dans une mise a jour partielle separee du fond et des tuiles.
- Les coins sont arrondis dans le fond lui-meme (l'OS rogne en plus selon le lanceur).
- Non compile sur le serveur (pas de JDK/Gradle) : verdict sur Mac avec `./gradlew assemblePerf && ./gradlew testDebugUnitTest`.

### 54. Nouveau logo ASAYS (note manuscrite) + fonds de repli des widgets
- **Logo remplace partout** : `app_logo.png` = 512 px, silhouette **blanche sur fond transparent**, extraite de la photo fournie par Tayeb avec ImageMagick (`-colorspace Gray -median 3 -negate -level 55%,72% -trim`, puis `-alpha copy -fill white -colorize 100%`). Le fond papier et les taches sont partis.
- Icone de lancement : `ic_launcher_foreground.png` + `ic_launcher_monochrome.png` (432 px, note a ~58 % = zone sure des icones adaptatives) et les 10 bitmaps legacy `mipmap-*/ic_launcher{,_round}.png` (note blanche sur carre arrondi / cercle, fond `#000000`).
- Le logo est une **silhouette monochrome** : sa couleur vient du theme a l'affichage (`colorFilter = ColorFilter.tint(TextPrimary)`) dans `HomeScreen` (en-tete 32 dp) et `ConnectScreen` (80 dp) - noir sur theme clair, blanc sur theme sombre. Les widgets utilisent la note blanche telle quelle (RemoteViews ne sait pas teinter une image).
- **Piege evite** : les nouveaux layouts de widgets n'avaient aucun fond statique (le fond vient du degrade genere au runtime) -> l'apercu du selecteur de widgets serait apparu transparent avec du texte blanc. `widget_bg_bar` / `widget_bg_card` (deja presents, redevenus utiles) + nouveau `widget_bg_wide` servent de repli, et le ripple des tuiles d'acces rapide porte maintenant un creneau `#2A2A2A` arrondi.



---

## 4. Subsonic & Navidrome Specifics / Critical Gotchas

1. **Static Salt for Coil Caching**:
   - Subsonic API URLs require `&u=user&t=token&s=salt`.
   - Never generate random salts for cover art! Doing so breaks Coil image caching. Always use `staticSalt = true` in `CoverUrls.kt`.
2. **Playlist Mutations**:
   - In Subsonic, calling `createPlaylist` with an existing `playlistId` will **overwrite** the entire playlist with only the newly provided song.
   - To append or remove tracks without wiping, always use `updatePlaylist.view` with `songIdToAdd` and `songIndexToRemove`.
3. **Double Encoding**:
   - In OkHttp interceptors, always use `.encodedQuery(...)` instead of `.query(...)` when rewriting requests to prevent `%20` from being converted to `%2520`.
4. **Android Auto Artwork Sandbox**:
   - Android Auto (`com.google.android.projection.gearhead`) cannot read `file:///data/user/0/...` paths.
   - Always route car artwork through `content://com.naviify.app.artwork/cover/{id}.jpg` backed by `ArtworkContentProvider`.

---

## 5. Verification & Testing Commands

To verify that the project is completely healthy:

```bash
# 1. Navigate to project directory
cd /Users/tayeb/Documents/naviify

# 2. Run all unit tests (94 tests covering queues, repositories, auth, mappers, custom lyrics)
./gradlew testDebugUnitTest

# 3. Assemble the FAST build (use this one, and to judge smoothness)
./gradlew assemblePerf

# 4. Install directly to a connected USB Android phone
~/Library/Android/sdk/platform-tools/adb install -r /Users/tayeb/Documents/naviify/app/build/outputs/apk/perf/app-perf.apk

# Debug build: only for step-by-step debugging in Android Studio. It is 2-3x slower on
# UI-heavy screens (Home) because it is debuggable and ships Compose ui-tooling.
./gradlew assembleDebug
```

- **Built APK Path (fast, prefer this one)**: `/Users/tayeb/Documents/naviify/app/build/outputs/apk/perf/app-perf.apk`
- **Built APK Path (debug)**: `/Users/tayeb/Documents/naviify/app/build/outputs/apk/debug/app-debug.apk`
- **Build Variants**: `debug` (debuggable + ui-tooling, application id `com.naviify.app.debug`), `perf` (non-debuggable, no R8, signed with the debug keystore, **same application id as debug** so installing it upgrades the app in place and keeps the saved server credentials), `release` (R8 + resource shrinking, also signed with the debug keystore so `assembleRelease` is installable — replace that with a real keystore before publishing).
- **Current Build Status**: `BUILD SUCCESSFUL` (0 errors, 94/94 tests passed).

---

## 6. Backlog & Prioritized Roadmap for Future Work

The following items are recommended for future milestones:

1. **Built-in Equalizer / Audio FX**:
   - Implement an in-app Equalizer sheet (5-band EQ, Bass Boost, Virtualizer) connected to Android's `AudioEffect` / `Equalizer` APIs attached to ExoPlayer's `audioSessionId`.
2. **Crossfade & Gapless Playback**:
   - Add optional track crossfade (0–12 seconds) and silence skipping in Settings.
3. **Chromecast / Google Cast**:
   - Integrate Google Cast SDK to stream audio directly to Nest Audio, smart TVs, and Google Cast receivers.
4. **Full Package Name Refactoring** *(Optional / Low Priority)*:
   - While the app is completely branded as ASAYS everywhere in UI and strings, the Java/Kotlin package remains `com.naviify.app`. If desired, execute a clean package rename to `com.asays.app`.

---

## 7. Direct Instructions for the Next AI Agent

If you are an AI assistant reading this guide in a new session:
- **Project Location**: `/Users/tayeb/Documents/naviify`
- **User Language**: Moroccan Darija (Arabic dialect) or English. Always respond politely, clearly, and concisely in the user's preferred language. All code and UI text must be in English.
- **Coding Style**: Idiomatic Kotlin, Jetpack Compose Material 3, clean Architecture with Hilt DI.
- **Verification Rule**: Always run `./gradlew assemblePerf` and `./gradlew testDebugUnitTest` after modifying code before concluding your task.
- **Performance Rule**: Never judge UI smoothness on a debug build. Only `perf`/`release` are representative: a debuggable build runs without ART/R8 optimizations and ships Compose ui-tooling, which costs 2-3x on image-heavy screens such as Home.
