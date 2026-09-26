package com.naviify.app.core.playback

import android.util.Log
import com.naviify.app.BuildConfig

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.naviify.app.data.repository.MediaRepository
import com.naviify.app.data.stats.ListeningStatsStore
import com.naviify.app.domain.model.Track
import com.naviify.app.domain.playback.PlaybackRepeatMode
import com.naviify.app.domain.playback.ScrobblePolicy
import com.naviify.app.domain.playback.PlayerQueueStore
import com.naviify.app.domain.playback.PlayerUiState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

import android.os.SystemClock
import com.naviify.app.core.storage.PlaylistMixStore
import com.naviify.app.domain.model.PlaylistMixConfig
import com.naviify.app.domain.model.PlaylistMixMode
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory

/**
 * App-side bridge to the [NaviifyPlaybackService] session. UI actions echo
 * locally into [PlayerQueueStore] for zero-lag feedback, then apply to the
 * active [MediaController]; engine events write the authoritative state back.
 */
@Singleton
class PlaybackController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val store: PlayerQueueStore,
    private val itemMapper: MediaItemMapper,
    private val streamQualityProvider: StreamQualityProvider,
    private val mediaRepository: MediaRepository,
    private val listeningStatsStore: ListeningStatsStore,
    private val playlistMixStore: PlaylistMixStore,
    private val cacheDataSourceFactory: CacheDataSource.Factory,
) {

    val state: StateFlow<PlayerUiState> = store.state

    private var controller: MediaController? = null
    private var connectRequested = false
    private var tickerJob: Job? = null
    private val scrobbledTrackIds = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    private var sleepJob: Job? = null
    private val _isSleepUntilTrackEnd = MutableStateFlow(false)
    val isSleepUntilTrackEnd: StateFlow<Boolean> = _isSleepUntilTrackEnd.asStateFlow()
    private val _sleepTimerRemainingSeconds = MutableStateFlow<Long?>(null)
    val sleepTimerRemainingSeconds: StateFlow<Long?> = _sleepTimerRemainingSeconds.asStateFlow()

    fun startSleepTimer(minutes: Int) {
        cancelSleepTimer()
        sleepJob = scope.launch {
            var remaining = minutes * 60L
            _sleepTimerRemainingSeconds.value = remaining
            while (isActive && remaining > 0) {
                delay(1000)
                remaining--
                _sleepTimerRemainingSeconds.value = remaining
            }
            _sleepTimerRemainingSeconds.value = null
            pausePlayback()
        }
    }

    fun startSleepTimerEndOfTrack() {
        cancelSleepTimer()
        _isSleepUntilTrackEnd.value = true
        sleepJob = scope.launch {
            while (isActive && _isSleepUntilTrackEnd.value) {
                val track = store.state.value.currentTrack
                val pos = controller?.currentPosition ?: store.state.value.positionMs
                val dur = (track?.duration ?: 0) * 1000L
                val remainingSec = if (dur > 0L) {
                    ((dur - pos) / 1000L).coerceAtLeast(0L)
                } else 0L
                _sleepTimerRemainingSeconds.value = remainingSec
                if (dur > 0L && pos >= (dur - 400L) && pos > 1000L) {
                    pausePlayback()
                    cancelSleepTimer()
                    break
                }
                delay(500)
            }
        }
    }

    fun cancelSleepTimer() {
        sleepJob?.cancel()
        sleepJob = null
        _isSleepUntilTrackEnd.value = false
        _sleepTimerRemainingSeconds.value = null
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var tailPlayer: ExoPlayer? = null
    private var transitionJob: Job? = null
    private var isMixTransitionTriggered = false

    private fun getOrCreateTailPlayer(): ExoPlayer {
        tailPlayer?.let { return it }
        val newPlayer = ExoPlayer.Builder(context)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                false,
            )
            .setMediaSourceFactory(DefaultMediaSourceFactory(cacheDataSourceFactory))
            .build()
        tailPlayer = newPlayer
        return newPlayer
    }

    private fun cancelTransition() {
        transitionJob?.cancel()
        transitionJob = null
        tailPlayer?.let { tp ->
            runCatching {
                tp.stop()
                tp.clearMediaItems()
            }
        }
        controller?.volume = 1.0f
        isMixTransitionTriggered = false
    }

    init {
        // Re-signed URLs when the effective quality changes (network switch or
        // settings change), preserving position without stopping playback.
        scope.launch {
            streamQualityProvider.effectiveQuality
                .drop(1)
                .collectLatest { reapplyWithCurrentQuality() }
        }
    }

    private val controllerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (BuildConfig.DEBUG) Log.d(TAG, "onIsPlayingChanged: isPlaying=$isPlaying")
            store.setIsPlaying(isPlaying)
            if (isPlaying) startTicker() else stopTicker()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (transitionJob?.isActive != true) {
                isMixTransitionTriggered = false
                controller?.volume = 1.0f
            }
            val shouldSleep = _isSleepUntilTrackEnd.value
            if (shouldSleep) {
                cancelSleepTimer()
                pausePlayback()
            }
            val mediaId = mediaItem?.mediaId
            val sessionIndex = controller?.currentMediaItemIndex ?: -1
            val index = if (sessionIndex in store.state.value.queue.indices &&
                mediaId?.endsWith(store.state.value.queue[sessionIndex].id) == true) {
                sessionIndex
            } else {
                store.state.value.queue.indexOfFirst { track ->
                    mediaId?.endsWith(track.id) == true
                }
            }
            syncPosition()
            if (index >= 0) store.setCurrentIndex(index)
            val trackId = mediaId?.removePrefix("track/")
            if (trackId != null && !shouldSleep) {
                scope.launch { mediaRepository.reportNowPlaying(trackId) }
            }

            // Continuous playback / Smart shuffle pre-fetch:
            if (!shouldSleep && index >= 0 && index >= store.state.value.queue.size - 2) {
                val currentTrack = store.state.value.queue.getOrNull(index)
                if (currentTrack != null && (store.state.value.isShuffleEnabled || store.state.value.repeatMode == PlaybackRepeatMode.OFF)) {
                    scope.launch { appendSimilarTracks(currentTrack) }
                }
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (BuildConfig.DEBUG) Log.d(TAG, "onPlaybackStateChanged: ${stateLabel(playbackState)}")
            if (playbackState == Player.STATE_ENDED) {
                if (_isSleepUntilTrackEnd.value) {
                    cancelSleepTimer()
                    pausePlayback()
                    stopTicker()
                    store.setIsPlaying(false)
                    return
                }
                val current = store.state.value
                val lastTrack = current.currentTrack ?: current.queue.lastOrNull()
                if (lastTrack != null && current.repeatMode == PlaybackRepeatMode.OFF) {
                    scope.launch {
                        val added = appendSimilarTracks(lastTrack)
                        if (added && store.state.value.currentIndex < store.state.value.queue.lastIndex) {
                            next()
                        } else {
                            stopTicker()
                            store.setIsPlaying(false)
                        }
                    }
                    return
                }
                stopTicker()
                store.setIsPlaying(false)
            }
            syncPosition()
        }

        override fun onEvents(player: Player, events: Player.Events) {
            if (player.isPlaying) {
                startTicker()
            } else if (player.playbackState == Player.STATE_ENDED || !player.playWhenReady) {
                stopTicker()
            }
            if (events.containsAny(
                    Player.EVENT_PLAY_WHEN_READY_CHANGED,
                    Player.EVENT_POSITION_DISCONTINUITY,
                    Player.EVENT_PLAYBACK_STATE_CHANGED,
                )
            ) {
                syncPosition()
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            if (BuildConfig.DEBUG) {
                val sanitized = error.message?.replace(Regex("""([?&])(t|s|p|u)=[^&]+"""), "$1$2=REDACTED")
                Log.e(TAG, "onPlayerError: ${error.errorCodeName} $sanitized")
            }
            store.setError(friendlyPlaybackErrorMessage(error))
            store.setIsPlaying(false)
        }
    }

    /** Tears down the session binding and listener. */
    fun disconnect() {
        cancelTransition()
        tailPlayer?.release()
        tailPlayer = null
        controller?.let { session ->
            session.removeListener(controllerListener)
            session.release()
        }
        controller = null
        connectRequested = false
        stopTicker()
    }

    /** Connects to the playback service; safe to call repeatedly and from any screen. */
    fun connect() {
        if (controller != null || connectRequested) return
        connectRequested = true
        val future = MediaController.Builder(
            context,
            SessionToken(context, ComponentName(context, NaviifyPlaybackService::class.java)),
        ).buildAsync()
        future.addListener({
            connectRequested = false
            controller = runCatching { future.get() }.getOrNull()?.also { session ->
                session.addListener(controllerListener)
                applyStoredStateTo(session)
                if (session.isPlaying) startTicker()
            }
            if (controller == null) {
                runCatching { future.cancel(true) }
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun play(
        queue: List<Track>,
        startIndex: Int = 0,
        startShuffled: Boolean = false,
        playlistId: String? = null,
    ) {
        if (queue.isEmpty()) {
            store.setError("This playlist is empty")
            return
        }
        scrobbledTrackIds.clear()
        store.play(queue, startIndex, startShuffled, playlistId)
        controller?.volume = 1.0f
        controller?.let(::applyStoredStateTo) ?: connect()
    }

    fun playShuffled(queue: List<Track>, playlistId: String? = null) {
        val randomStart = if (queue.isNotEmpty()) queue.indices.random() else 0
        play(queue, startIndex = randomStart, startShuffled = true, playlistId = playlistId)
    }

    fun pausePlayback() {
        cancelTransition()
        controller?.pause() ?: store.setIsPlaying(false)
    }

    fun togglePlayPause() {
        val session = controller
        if (session == null) {
            store.togglePlayPause()
            connect()
            return
        }
        if (session.mediaItemCount == 0 && store.state.value.queue.isNotEmpty()) {
            applyStoredStateTo(session)
            return
        }
        if (session.isPlaying) {
            cancelTransition()
            session.pause()
        } else {
            session.play()
        }
    }

    fun next() {
        cancelTransition()
        val session = controller
        if (session != null) {
            session.seekToNextMediaItem()
        } else {
            store.next()
            connect()
        }
    }

    fun previous() {
        cancelTransition()
        val session = controller
        if (session != null) {
            session.seekToPreviousMediaItem()
        } else {
            store.previous()
            connect()
        }
    }

    fun select(trackIndex: Int) {
        cancelTransition()
        store.select(trackIndex)
        val session = controller
        if (session != null) {
            if (session.mediaItemCount > 0) {
                session.seekTo(trackIndex, 0L)
                session.play()
            } else {
                applyStoredStateTo(session)
            }
        } else {
            connect()
        }
    }

    fun seekTo(positionMs: Long) {
        cancelTransition()
        store.seekTo(positionMs)
        controller?.seekTo(positionMs)
    }

    private var isFetchingSimilar = false
    private var prepareRetryCount = 0

    suspend fun appendSimilarTracks(seedTrack: Track): Boolean {
        if (isFetchingSimilar) return false
        isFetchingSimilar = true
        return try {
            val similar = mediaRepository.getSimilarOrRelatedTracks(seedTrack, count = 25)
            val existingIds = store.state.value.queue.map { it.id }.toSet()
            val newTracks = similar.filter { it.id !in existingIds }
            if (newTracks.isNotEmpty()) {
                val session = controller
                if (session != null) {
                    val mediaItems = newTracks.map { itemMapper.trackItem(it) }
                    session.addMediaItems(mediaItems)
                }
                newTracks.forEach { store.append(it) }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.w(TAG, "appendSimilarTracks error", e)
            false
        } finally {
            isFetchingSimilar = false
        }
    }

    fun playSimilarRadio(seedTrack: Track) {
        scrobbledTrackIds.clear()
        store.play(listOf(seedTrack), 0)
        setShuffle(true)
        controller?.let(::applyStoredStateTo) ?: connect()
        scope.launch {
            appendSimilarTracks(seedTrack)
        }
    }

    fun setShuffle(enabled: Boolean) {
        store.setShuffle(enabled)
        val session = controller
        if (session != null) {
            val updated = store.state.value
            replaceUpcomingItems(session, updated.queue, updated.currentIndex)
        }
        if (enabled) {
            val current = store.state.value
            val currentTrack = current.currentTrack
            if (currentTrack != null && (current.queue.size <= 3 || current.currentIndex >= current.queue.size - 2)) {
                scope.launch {
                    appendSimilarTracks(currentTrack)
                }
            }
        }
    }

    fun reshuffleQueue() {
        store.reshuffle()
        val session = controller
        if (session != null) {
            val updated = store.state.value
            replaceUpcomingItems(session, updated.queue, updated.currentIndex)
        }
    }

    private fun replaceUpcomingItems(session: MediaController, newQueue: List<Track>, currentIndex: Int) {
        val nextIndex = currentIndex + 1
        if (session.currentMediaItemIndex == currentIndex && nextIndex <= session.mediaItemCount) {
            val count = session.mediaItemCount
            if (count > nextIndex) {
                session.removeMediaItems(nextIndex, count)
            }
            val upcomingTracks = newQueue.drop(nextIndex)
            if (upcomingTracks.isNotEmpty()) {
                session.addMediaItems(itemMapper.queueItems(upcomingTracks))
            }
            session.setShuffleModeEnabled(false)
        } else {
            val position = session.currentPosition.coerceAtLeast(0)
            val shouldPlay = session.playWhenReady
            session.setMediaItems(
                itemMapper.queueItems(newQueue),
                currentIndex.coerceAtLeast(0),
                position,
            )
            session.setShuffleModeEnabled(false)
            if (shouldPlay) session.play()
        }
    }

    fun moveInQueue(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        store.move(fromIndex, toIndex)
        val session = controller ?: return
        if (fromIndex in 0 until session.mediaItemCount && toIndex in 0 until session.mediaItemCount) {
            session.moveMediaItem(fromIndex, toIndex)
        } else {
            val updated = store.state.value
            val position = session.currentPosition.coerceAtLeast(0)
            val shouldPlay = session.playWhenReady
            session.setMediaItems(
                itemMapper.queueItems(updated.queue),
                updated.currentIndex.coerceAtLeast(0),
                position,
            )
            if (shouldPlay) session.play()
        }
    }

    fun setRepeat(mode: PlaybackRepeatMode) {
        store.setRepeatMode(mode)
        controller?.setRepeatMode(mode.toMedia3Mode())
    }

    fun appendToQueue(track: Track) {
        val hadQueue = store.state.value.queue.isNotEmpty()
        val insertIndex = store.addToUserQueue(track)
        val session = controller
        if (session != null) {
            if (hadQueue && session.mediaItemCount > 0) {
                val clampedIndex = insertIndex.coerceIn(0, session.mediaItemCount)
                session.addMediaItem(clampedIndex, itemMapper.trackItem(track))
                if (BuildConfig.DEBUG) Log.d(TAG, "appendToQueue: inserted at $clampedIndex, track=${track.id}, queue=${store.state.value.queue.size}")
            } else {
                applyStoredStateTo(session)
            }
        } else {
            connect()
        }
    }

    fun retryCurrent() {
        controller?.let(::applyStoredStateTo)
    }

    fun clear() {
        stopTicker()
        scrobbledTrackIds.clear()
        store.clear()
        controller?.let { session ->
            session.stop()
            session.clearMediaItems()
        }
    }

    private fun applyStoredStateTo(session: MediaController) {
        try {
            val current = store.state.value
            if (current.queue.isEmpty()) return
            if (BuildConfig.DEBUG) Log.d(TAG, "applyStoredStateTo: queue=${current.queue.size} index=${current.currentIndex} playing=${current.isPlaying}")
            val items = itemMapper.queueItems(current.queue)
            if (items.isEmpty()) {
                // Stream URLs can only be signed once the server config has been
                // published, so a cold start can map to nothing. Retry briefly
                // instead of leaving ExoPlayer with an empty playlist, which the
                // user experiences as "play does nothing".
                if (prepareRetryCount < MAX_PREPARE_RETRIES) {
                    prepareRetryCount++
                    if (BuildConfig.DEBUG) Log.d(TAG, "applyStoredStateTo: no playable items yet (retry $prepareRetryCount)")
                    store.setError("Connecting to your server...")
                    scope.launch {
                        delay(PREPARE_RETRY_DELAY_MS)
                        if (controller === session) applyStoredStateTo(session)
                    }
                } else {
                    store.setError("Could not reach your server")
                }
                return
            }
            prepareRetryCount = 0
            store.setError(null)
            session.setMediaItems(items, current.currentIndex.coerceAtLeast(0), 0L)
            session.setShuffleModeEnabled(false)
            session.setRepeatMode(current.repeatMode.toMedia3Mode())
            session.playWhenReady = true
            session.prepare()
            if (current.isPlaying) session.play()
            if (session.playWhenReady || session.isPlaying) startTicker()
            if (BuildConfig.DEBUG) Log.d(TAG, "applyStoredStateTo: prepared + playWhenReady=true")
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.e(TAG, "applyStoredStateTo failed", e)
            store.setError("Playback setup failed")
        }
    }

    private fun syncPosition() {
        val session = controller ?: return
        val positionMs = session.currentPosition.coerceAtLeast(0)
        val sessionDuration = session.duration.coerceAtLeast(0)
        val fallbackDuration = (store.state.value.currentTrack?.duration?.toLong() ?: 0L) * 1000L
        val durationMs = if (sessionDuration > 0) sessionDuration else fallbackDuration
        store.setPosition(positionMs, durationMs)
        maybeScrobble()
        applyMixTransitionIfNeeded(session, positionMs, durationMs)
    }

    private fun applyMixTransitionIfNeeded(session: MediaController, positionMs: Long, durationMs: Long) {
        val playlistId = store.state.value.activePlaylistId ?: return
        val config = playlistMixStore.getConfig(playlistId)
        if (!config.isEnabled) return

        val currentIndex = store.state.value.currentIndex
        val queue = store.state.value.queue
        val queueSize = queue.size
        val hasNext = currentIndex in 0 until (queueSize - 1) || store.state.value.repeatMode == PlaybackRepeatMode.ALL
        if (!hasNext) {
            if (session.volume < 1.0f && transitionJob?.isActive != true) session.volume = 1.0f
            return
        }

        val overlapMs = (config.durationSeconds * 1000L).toLong().coerceIn(2000L, 12000L)
        if (durationMs <= overlapMs || durationMs < 6000L) return

        val transitionStartMs = durationMs - overlapMs
        if (positionMs >= transitionStartMs && !isMixTransitionTriggered && transitionJob?.isActive != true) {
            val fromTrack = queue.getOrNull(currentIndex) ?: return
            val nextIndex = if (currentIndex in 0 until (queueSize - 1)) currentIndex + 1 else 0
            val toTrack = queue.getOrNull(nextIndex) ?: return
            val transitionMode = config.transitionModeFor(fromTrack.id, toTrack.id)

            isMixTransitionTriggered = true
            executeMixTransition(session, fromTrack, toTrack, positionMs, overlapMs, transitionMode, config)
        }
    }

    private fun executeMixTransition(
        session: MediaController,
        fromTrack: Track,
        toTrack: Track,
        currentPosMs: Long,
        overlapMs: Long,
        transitionMode: PlaylistMixMode,
        config: PlaylistMixConfig,
    ) {
        transitionJob?.cancel()
        transitionJob = scope.launch {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Starting DJ mix transition: '${fromTrack.title}' -> '${toTrack.title}' ($transitionMode, ${overlapMs}ms)")
            }

            if (transitionMode == PlaylistMixMode.SLAM) {
                // Slam: Instant drop on the beat with zero silence
                session.seekToNextMediaItem()
                session.volume = 1.0f
                isMixTransitionTriggered = false
                return@launch
            }

            val tailStarted = runCatching {
                val tail = getOrCreateTailPlayer()
                val fromMediaItem = itemMapper.trackItem(fromTrack)
                tail.stop()
                tail.setMediaItem(fromMediaItem, currentPosMs)
                tail.volume = 1.0f
                tail.prepare()
                tail.playWhenReady = true
                tail.play()
                true
            }.getOrDefault(false)

            if (!tailStarted) {
                session.seekToNextMediaItem()
                session.volume = 1.0f
                isMixTransitionTriggered = false
                return@launch
            }

            val tail = getOrCreateTailPlayer()
            // Main player immediately seeks and starts incoming track
            session.volume = 0.05f
            session.seekToNextMediaItem()

            val startTime = SystemClock.elapsedRealtime()
            val totalSpanMs = overlapMs.toFloat()

            while (isActive) {
                val elapsed = (SystemClock.elapsedRealtime() - startTime).coerceAtLeast(0L)
                val progress = (elapsed.toFloat() / totalSpanMs).coerceIn(0f, 1f)

                val (outGain, inGain) = calculateMixGains(progress, transitionMode, config.equalPowerVolume)

                tail.volume = outGain.coerceIn(0f, 1f)
                session.volume = inGain.coerceIn(0.01f, 1f)

                if (progress >= 1f || tail.playbackState == Player.STATE_ENDED || tail.playbackState == Player.STATE_IDLE) {
                    break
                }
                delay(35L)
            }

            runCatching {
                tail.stop()
                tail.clearMediaItems()
            }
            session.volume = 1.0f
            isMixTransitionTriggered = false
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "DJ mix transition completed: now playing '${toTrack.title}' at 1.0 volume")
            }
        }
    }

    private fun calculateMixGains(
        progress: Float,
        mode: PlaylistMixMode,
        equalPower: Boolean,
    ): Pair<Float, Float> {
        return when (mode) {
            PlaylistMixMode.AUTO, PlaylistMixMode.FADE -> {
                if (equalPower) {
                    val outGain = kotlin.math.cos(progress * Math.PI / 2.0).toFloat()
                    val inGain = kotlin.math.sin(progress * Math.PI / 2.0).toFloat()
                    Pair(outGain, inGain)
                } else {
                    Pair(1f - progress, progress)
                }
            }
            PlaylistMixMode.RISE -> {
                val outGain = if (progress < 0.75f) 1.0f else ((1f - progress) / 0.25f)
                val inGain = (progress * progress).coerceIn(0f, 1f)
                Pair(outGain, inGain)
            }
            PlaylistMixMode.MELT -> {
                val outGain = (1f - progress) * (1f - progress)
                val inGain = kotlin.math.sqrt(progress.toDouble()).toFloat()
                Pair(outGain, inGain)
            }
            PlaylistMixMode.SLAM -> {
                Pair(0f, 1f)
            }
        }
    }

    private fun maybeScrobble() {
        val current = store.state.value
        val trackId = current.currentTrack?.id ?: return
        if (!ScrobblePolicy.shouldScrobble(current.positionMs, current.durationMs)) return
        if (!scrobbledTrackIds.add(trackId)) return
        val playedAtMs = System.currentTimeMillis() - current.positionMs
        listeningStatsStore.recordPlayback(
            (current.durationMs.coerceAtLeast(0) / 1000).toInt(),
            current.currentTrack,
        )
        scope.launch { mediaRepository.submitScrobble(trackId, playedAtMs) }
    }

    private suspend fun reapplyWithCurrentQuality() {
        val session = controller ?: return
        val current = store.state.value
        if (current.queue.isEmpty() || session.mediaItemCount == 0) return
        runCatching {
            val position = session.currentPosition.coerceAtLeast(0)
            val shouldPlay = session.playWhenReady
            session.setMediaItems(itemMapper.queueItems(current.queue), current.currentIndex.coerceAtLeast(0), position)
            if (shouldPlay) session.play()
        }
    }

    /** Removes a track from the interactive queue, resuming at the same position. */
    fun removeFromQueue(index: Int) {
        val session = controller
        if (session == null) {
            store.removeAt(index)
            return
        }
        val current = store.state.value
        if (index !in current.queue.indices) return
        if (current.queue.size <= 1) {
            clear()
            return
        }
        store.removeAt(index)
        if (index in 0 until session.mediaItemCount) {
            session.removeMediaItem(index)
        } else {
            val updated = store.state.value
            if (updated.queue.isEmpty()) {
                clear()
                return
            }
            if (updated.currentIndex < 0) return
            val position = session.currentPosition.coerceAtLeast(0)
            session.setMediaItems(itemMapper.queueItems(updated.queue), updated.currentIndex.coerceAtLeast(0), position)
            if (session.playWhenReady) session.play()
        }
    }

    private fun startTicker() {
        if (tickerJob?.isActive == true) return
        tickerJob = scope.launch {
            while (isActive) {
                syncPosition()
                val playlistId = store.state.value.activePlaylistId
                val isMix = playlistId != null && playlistMixStore.getConfig(playlistId).isEnabled
                val curPos = store.state.value.positionMs
                val curDur = store.state.value.durationMs
                val isNearTransition = isMix && curDur > 6000L && curPos >= (curDur - 15000L)
                delay(if (isNearTransition || transitionJob?.isActive == true) 40L else 400L)
            }
        }
    }

    private fun stopTicker() {
        syncPosition()
        tickerJob?.cancel()
        tickerJob = null
    }

    private fun PlaybackRepeatMode.toMedia3Mode(): Int = when (this) {
        PlaybackRepeatMode.OFF -> Player.REPEAT_MODE_OFF
        PlaybackRepeatMode.ALL -> Player.REPEAT_MODE_ALL
        PlaybackRepeatMode.ONE -> Player.REPEAT_MODE_ONE
    }

    private fun stateLabel(state: Int): String = when (state) {
        Player.STATE_IDLE -> "IDLE"
        Player.STATE_BUFFERING -> "BUFFERING"
        Player.STATE_READY -> "READY"
        Player.STATE_ENDED -> "ENDED"
        else -> state.toString()
    }

    private fun friendlyPlaybackErrorMessage(error: PlaybackException): String {
        return when (error.errorCode) {
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> "Network connection failed. Check your connection."
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> "Track not found on server."
            PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
            PlaybackException.ERROR_CODE_DECODING_FAILED -> "Audio decoding error."
            else -> "Playback error occurred. Tap to retry."
        }
    }

    companion object {
        private const val TAG = "AsaysAudio"
        private const val MAX_PREPARE_RETRIES = 6
        private const val PREPARE_RETRY_DELAY_MS = 750L
    }
}
