package com.naviify.app.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import android.widget.RemoteViews
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.naviify.app.MainActivity
import com.naviify.app.R
import com.naviify.app.core.image.CoverUrls
import com.naviify.app.domain.model.Track
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Peint les trois widgets de lecture (4x1, 2x2, 4x2) en style Spotify : fond
 * degrade tire de la couleur de la pochette, pochette ronde, controles minimaux.
 *
 * Deux regles en editant ce fichier :
 *  1. chaque [RemoteViews] voyage dans une transaction Binder (~1 Mo) : les
 *     bitmaps restent petits et la pochette part dans une mise a jour partielle
 *     separee du fond et des tuiles.
 *  2. tout ce qui est peint ici doit l'etre pour les trois familles -> apres un
 *     changement de morceau, passer par [updateAllWidgets], jamais par une mise
 *     a jour locale a une seule famille.
 */
object PlayerWidgetUpdater {

    private const val TAG = "PlayerWidgetUpdater"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private const val PREFS_NAME = "naviify_playback_prefs"
    private const val KEY_LAST_TRACK_ID = "last_track_id"
    private const val KEY_LAST_TRACK_TITLE = "last_track_title"
    private const val KEY_LAST_TRACK_ARTIST = "last_track_artist"
    private const val KEY_LAST_TRACK_COVER_ID = "last_track_cover_id"

    // Tailles de rendu (px), volontairement modestes : une RemoteViews trop
    // lourde fait echouer la mise a jour (transaction Binder ~1 Mo).
    private const val BAR_ART_PX = 112
    private const val BAR_ART_RADIUS_PX = 14f
    private const val BAR_BG_WIDTH_PX = 420
    private const val BAR_BG_HEIGHT_PX = 108
    private const val CARD_ART_PX = 288
    private const val CARD_ART_RADIUS_PX = 34f
    private const val WIDE_ART_PX = 176
    private const val WIDE_ART_RADIUS_PX = 14f
    private const val WIDE_BG_WIDTH_PX = 420
    private const val WIDE_BG_HEIGHT_PX = 210
    private const val TILE_PX = 96
    private const val TILE_RADIUS_PX = 12f
    private const val TILE_COUNT = 5

    /** Rayon des coins des fonds de widget, en px de la taille de reference. */
    private const val BAR_CORNER_PX = 36f
    private const val WIDE_CORNER_PX = 36f

    private const val REQUEST_BAR_OPEN = 10
    private const val REQUEST_BAR_PLAY_PAUSE = 11
    private const val REQUEST_BAR_NEXT = 12
    private const val REQUEST_BAR_PREV = 13
    private const val REQUEST_CARD_OPEN = 20
    private const val REQUEST_CARD_PLAY_PAUSE = 21
    private const val REQUEST_WIDE_OPEN = 30
    private const val REQUEST_WIDE_PLAY_PAUSE = 31
    private const val REQUEST_WIDE_NEXT = 32
    private const val REQUEST_WIDE_PREV = 33

    private val tileViewIds = intArrayOf(
        R.id.widget_wide_tile_1,
        R.id.widget_wide_tile_2,
        R.id.widget_wide_tile_3,
        R.id.widget_wide_tile_4,
        R.id.widget_wide_tile_5,
    )

    private data class WidgetPlaybackState(
        val trackId: String?,
        val title: String,
        val artist: String,
        val isPlaying: Boolean,
        val coverArtId: String?,
        /** Pochettes de la file d'attente, pour la ligne d'acces rapide du 4x2. */
        val quickCovers: List<String>,
    )

    private fun extractCurrentState(context: Context): WidgetPlaybackState {
        return try {
            val entry = EntryPointAccessors.fromApplication(
                context.applicationContext,
                WidgetEntryPoint::class.java,
            )
            val state = entry.playerQueueStore().state.value
            val currentTrack = state.currentTrack

            if (currentTrack != null) {
                WidgetPlaybackState(
                    trackId = currentTrack.id,
                    title = currentTrack.title.ifBlank { "ASAYS" },
                    artist = (currentTrack.artist ?: "").ifBlank { "Unknown Artist" },
                    isPlaying = state.isPlaying,
                    coverArtId = currentTrack.coverArtId,
                    quickCovers = quickCoversOf(state.queue, state.currentIndex),
                )
            } else {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val lastTitle = prefs.getString(KEY_LAST_TRACK_TITLE, null)
                if (!lastTitle.isNullOrBlank()) {
                    WidgetPlaybackState(
                        trackId = prefs.getString(KEY_LAST_TRACK_ID, null),
                        title = lastTitle,
                        artist = prefs.getString(KEY_LAST_TRACK_ARTIST, "").orEmpty().ifBlank { "ASAYS" },
                        isPlaying = false,
                        coverArtId = prefs.getString(KEY_LAST_TRACK_COVER_ID, null),
                        quickCovers = emptyList(),
                    )
                } else {
                    WidgetPlaybackState(
                        trackId = null,
                        title = "ASAYS",
                        artist = context.getString(R.string.widget_ready_to_play),
                        isPlaying = false,
                        coverArtId = null,
                        quickCovers = emptyList(),
                    )
                }
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Error resolving playback state for widgets, using fallback", e)
            WidgetPlaybackState(
                trackId = null,
                title = "ASAYS",
                artist = context.getString(R.string.widget_ready_to_play),
                isPlaying = false,
                coverArtId = null,
                quickCovers = emptyList(),
            )
        }
    }

    /** Les [TILE_COUNT] prochaines pochettes de la file, puis celles deja jouees. */
    private fun quickCoversOf(queue: List<Track>, currentIndex: Int): List<String> {
        if (queue.isEmpty()) return emptyList()
        val upcoming = queue.drop(currentIndex + 1).mapNotNull { it.coverArtId }
        val played = queue.take(currentIndex.coerceAtLeast(0)).mapNotNull { it.coverArtId }
        return (upcoming + played).distinct().take(TILE_COUNT)
    }

    fun updateAllWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
        val barIds = appWidgetManager.getAppWidgetIds(
            ComponentName(context, NowPlayingBarWidgetProvider::class.java),
        )
        val cardIds = appWidgetManager.getAppWidgetIds(
            ComponentName(context, NowPlayingCardWidgetProvider::class.java),
        )
        val wideIds = appWidgetManager.getAppWidgetIds(
            ComponentName(context, NowPlayingWideWidgetProvider::class.java),
        )

        if (barIds.isEmpty() && cardIds.isEmpty() && wideIds.isEmpty()) return

        val state = extractCurrentState(context)

        if (barIds.isNotEmpty()) {
            renderBarWidgets(context, appWidgetManager, barIds, state)
        }
        if (cardIds.isNotEmpty()) {
            renderCardWidgets(context, appWidgetManager, cardIds, state)
        }
        if (wideIds.isNotEmpty()) {
            renderWideWidgets(context, appWidgetManager, wideIds, state)
        }
    }

    fun updateBarWidgets(context: Context, appWidgetManager: AppWidgetManager, widgetIds: IntArray) {
        if (widgetIds.isEmpty()) return
        renderBarWidgets(context, appWidgetManager, widgetIds, extractCurrentState(context))
    }

    fun updateCardWidgets(context: Context, appWidgetManager: AppWidgetManager, widgetIds: IntArray) {
        if (widgetIds.isEmpty()) return
        renderCardWidgets(context, appWidgetManager, widgetIds, extractCurrentState(context))
    }

    fun updateWideWidgets(context: Context, appWidgetManager: AppWidgetManager, widgetIds: IntArray) {
        if (widgetIds.isEmpty()) return
        renderWideWidgets(context, appWidgetManager, widgetIds, extractCurrentState(context))
    }

    // ------------------------------------------------------------------ 4x1 barre

    private fun renderBarWidgets(
        context: Context,
        appWidgetManager: AppWidgetManager,
        widgetIds: IntArray,
        state: WidgetPlaybackState,
    ) {
        val openAppIntent = openAppPendingIntent(context, REQUEST_BAR_OPEN)
        val playPauseIntent = actionPendingIntent(context, REQUEST_BAR_PLAY_PAUSE, PlayerWidgetReceiver.ACTION_PLAY_PAUSE)
        val nextIntent = actionPendingIntent(context, REQUEST_BAR_NEXT, PlayerWidgetReceiver.ACTION_NEXT)
        val prevIntent = actionPendingIntent(context, REQUEST_BAR_PREV, PlayerWidgetReceiver.ACTION_PREV)

        val cachedArtwork = state.coverArtId?.let { artworkBitmap(it, BAR_ART_PX, BAR_ART_RADIUS_PX) }
        val artwork = cachedArtwork ?: WidgetBitmapUtils.getDefaultArtwork(context, BAR_ART_PX, BAR_ART_RADIUS_PX)
        val background = gradientBackground(cachedArtwork, BAR_BG_WIDTH_PX, BAR_BG_HEIGHT_PX, BAR_CORNER_PX)

        for (widgetId in widgetIds) {
            try {
                val views = RemoteViews(context.packageName, R.layout.widget_now_playing_bar).apply {
                    setTextViewText(R.id.widget_title, state.title)
                    setTextViewText(R.id.widget_artist, state.artist)
                    setPlayPauseIcon(state, R.id.widget_btn_play_pause)

                    background?.let { setImageViewBitmap(R.id.widget_bg, it) }
                    artwork?.let { setImageViewBitmap(R.id.widget_album_art, it) }

                    setOnClickPendingIntent(R.id.widget_root, openAppIntent)
                    setOnClickPendingIntent(R.id.widget_open_app_area, openAppIntent)
                    setOnClickPendingIntent(R.id.widget_album_art, openAppIntent)
                    setOnClickPendingIntent(R.id.widget_title, openAppIntent)
                    setOnClickPendingIntent(R.id.widget_artist, openAppIntent)

                    setOnClickPendingIntent(R.id.widget_btn_play_pause, playPauseIntent)
                    setOnClickPendingIntent(R.id.widget_btn_next, nextIntent)
                    setOnClickPendingIntent(R.id.widget_btn_prev, prevIntent)
                }
                appWidgetManager.updateAppWidget(widgetId, views)
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to update bar widget: $widgetId", e)
            }
        }

        if (state.coverArtId != null && cachedArtwork == null) {
            scope.launch(Dispatchers.IO) {
                val loadedArtwork = loadArtwork(context, state.coverArtId, BAR_ART_PX, BAR_ART_RADIUS_PX)
                    ?: return@launch
                val loadedBackground = gradientBackground(
                    loadedArtwork,
                    BAR_BG_WIDTH_PX,
                    BAR_BG_HEIGHT_PX,
                    BAR_CORNER_PX,
                ) ?: return@launch
                partiallyUpdateWidgets(context, widgetIds, R.layout.widget_now_playing_bar) {
                    setImageViewBitmap(R.id.widget_bg, loadedBackground)
                    setImageViewBitmap(R.id.widget_album_art, loadedArtwork)
                }
            }
        }
    }

    // ------------------------------------------------------------------ 2x2 tuile

    private fun renderCardWidgets(
        context: Context,
        appWidgetManager: AppWidgetManager,
        widgetIds: IntArray,
        state: WidgetPlaybackState,
    ) {
        val openAppIntent = openAppPendingIntent(context, REQUEST_CARD_OPEN)
        val playPauseIntent = actionPendingIntent(context, REQUEST_CARD_PLAY_PAUSE, PlayerWidgetReceiver.ACTION_PLAY_PAUSE)

        val cachedArtwork = state.coverArtId?.let { artworkBitmap(it, CARD_ART_PX, CARD_ART_RADIUS_PX) }
        val artwork = cachedArtwork ?: WidgetBitmapUtils.getDefaultArtwork(context, CARD_ART_PX, CARD_ART_RADIUS_PX)

        for (widgetId in widgetIds) {
            try {
                val views = RemoteViews(context.packageName, R.layout.widget_now_playing_card).apply {
                    setTextViewText(R.id.widget_card_title, state.title)
                    setTextViewText(R.id.widget_card_artist, state.artist)
                    setPlayPauseIcon(state, R.id.widget_card_btn_play_pause)

                    artwork?.let { setImageViewBitmap(R.id.widget_card_art, it) }

                    setOnClickPendingIntent(R.id.widget_card_root, openAppIntent)
                    setOnClickPendingIntent(R.id.widget_card_open_app_area, openAppIntent)
                    setOnClickPendingIntent(R.id.widget_card_art, openAppIntent)
                    setOnClickPendingIntent(R.id.widget_card_title, openAppIntent)
                    setOnClickPendingIntent(R.id.widget_card_artist, openAppIntent)

                    setOnClickPendingIntent(R.id.widget_card_btn_play_pause, playPauseIntent)
                }
                appWidgetManager.updateAppWidget(widgetId, views)
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to update card widget: $widgetId", e)
            }
        }

        if (state.coverArtId != null && cachedArtwork == null) {
            scope.launch(Dispatchers.IO) {
                val loadedArtwork = loadArtwork(context, state.coverArtId, CARD_ART_PX, CARD_ART_RADIUS_PX)
                    ?: return@launch
                partiallyUpdateWidgets(context, widgetIds, R.layout.widget_now_playing_card) {
                    setImageViewBitmap(R.id.widget_card_art, loadedArtwork)
                }
            }
        }
    }

    // -------------------------------------------------- 4x2 lecteur + acces rapide

    private fun renderWideWidgets(
        context: Context,
        appWidgetManager: AppWidgetManager,
        widgetIds: IntArray,
        state: WidgetPlaybackState,
    ) {
        val openAppIntent = openAppPendingIntent(context, REQUEST_WIDE_OPEN)
        val playPauseIntent = actionPendingIntent(context, REQUEST_WIDE_PLAY_PAUSE, PlayerWidgetReceiver.ACTION_PLAY_PAUSE)
        val nextIntent = actionPendingIntent(context, REQUEST_WIDE_NEXT, PlayerWidgetReceiver.ACTION_NEXT)
        val prevIntent = actionPendingIntent(context, REQUEST_WIDE_PREV, PlayerWidgetReceiver.ACTION_PREV)

        val coverArtId = state.coverArtId
        val cachedArtwork = coverArtId?.let { artworkBitmap(it, WIDE_ART_PX, WIDE_ART_RADIUS_PX) }
        val artwork = cachedArtwork ?: WidgetBitmapUtils.getDefaultArtwork(context, WIDE_ART_PX, WIDE_ART_RADIUS_PX)
        val background = gradientBackground(cachedArtwork, WIDE_BG_WIDTH_PX, WIDE_BG_HEIGHT_PX, WIDE_CORNER_PX)
        val placeholderTile = WidgetBitmapUtils.getDefaultArtwork(context, TILE_PX, TILE_RADIUS_PX)
        val tiles = Array(TILE_COUNT) { index ->
            state.quickCovers.getOrNull(index)?.let { tileBitmap(it) } ?: placeholderTile
        }

        for (widgetId in widgetIds) {
            try {
                val views = RemoteViews(context.packageName, R.layout.widget_now_playing_wide).apply {
                    setTextViewText(R.id.widget_wide_title, state.title)
                    setTextViewText(R.id.widget_wide_artist, state.artist)
                    setPlayPauseIcon(state, R.id.widget_wide_btn_play_pause)

                    background?.let { setImageViewBitmap(R.id.widget_wide_bg, it) }
                    artwork?.let { setImageViewBitmap(R.id.widget_wide_art, it) }

                    setOnClickPendingIntent(R.id.widget_wide_root, openAppIntent)
                    setOnClickPendingIntent(R.id.widget_wide_open_app_area, openAppIntent)
                    setOnClickPendingIntent(R.id.widget_wide_art, openAppIntent)
                    setOnClickPendingIntent(R.id.widget_wide_title, openAppIntent)
                    setOnClickPendingIntent(R.id.widget_wide_artist, openAppIntent)

                    setOnClickPendingIntent(R.id.widget_wide_btn_play_pause, playPauseIntent)
                    setOnClickPendingIntent(R.id.widget_wide_btn_next, nextIntent)
                    setOnClickPendingIntent(R.id.widget_wide_btn_prev, prevIntent)

                    tileViewIds.forEachIndexed { index, viewId ->
                        tiles.getOrNull(index)?.let { setImageViewBitmap(viewId, it) }
                        setOnClickPendingIntent(viewId, openAppIntent)
                    }
                }
                appWidgetManager.updateAppWidget(widgetId, views)
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to update wide widget: $widgetId", e)
            }
        }

        val missingTiles = state.quickCovers.any { tileBitmap(it) == null }
        val needsArtwork = coverArtId != null && cachedArtwork == null
        if (!needsArtwork && !missingTiles) return

        scope.launch(Dispatchers.IO) {
            val loadedArtwork = if (needsArtwork) {
                coverArtId?.let { loadArtwork(context, it, WIDE_ART_PX, WIDE_ART_RADIUS_PX) }
            } else {
                null
            }
            val loadedBackground = loadedArtwork?.let {
                gradientBackground(it, WIDE_BG_WIDTH_PX, WIDE_BG_HEIGHT_PX, WIDE_CORNER_PX)
            }
            val loadedTiles = state.quickCovers.take(TILE_COUNT).map { coverId -> loadTile(context, coverId) }

            if (loadedBackground != null || missingTiles) {
                partiallyUpdateWidgets(context, widgetIds, R.layout.widget_now_playing_wide) {
                    loadedBackground?.let { setImageViewBitmap(R.id.widget_wide_bg, it) }
                    loadedTiles.forEachIndexed { index, bitmap ->
                        if (bitmap != null && index < tileViewIds.size) {
                            setImageViewBitmap(tileViewIds[index], bitmap)
                        }
                    }
                }
            }
            if (loadedArtwork != null) {
                partiallyUpdateWidgets(context, widgetIds, R.layout.widget_now_playing_wide) {
                    setImageViewBitmap(R.id.widget_wide_art, loadedArtwork)
                }
            }
        }
    }

    // -------------------------------------------------------------------- outils

    private fun RemoteViews.setPlayPauseIcon(state: WidgetPlaybackState, viewId: Int) {
        setImageViewResource(
            viewId,
            if (state.isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play,
        )
    }

    private fun partiallyUpdateWidgets(
        context: Context,
        widgetIds: IntArray,
        layoutId: Int,
        bind: RemoteViews.() -> Unit,
    ) {
        val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
        for (widgetId in widgetIds) {
            try {
                val views = RemoteViews(context.packageName, layoutId).apply(bind)
                appWidgetManager.partiallyUpdateAppWidget(widgetId, views)
            } catch (e: Throwable) {
                Log.w(TAG, "Failed to partially update widget: $widgetId", e)
            }
        }
    }

    private fun artworkCacheKey(coverArtId: String, sizePx: Int, radiusPx: Float) =
        "art-$coverArtId-$sizePx-${radiusPx.toInt()}"

    private fun artworkBitmap(coverArtId: String, sizePx: Int, radiusPx: Float): Bitmap? =
        WidgetBitmapUtils.getCachedBitmap(artworkCacheKey(coverArtId, sizePx, radiusPx))

    private fun tileBitmap(coverArtId: String): Bitmap? = artworkBitmap(coverArtId, TILE_PX, TILE_RADIUS_PX)

    private suspend fun loadTile(context: Context, coverArtId: String): Bitmap? =
        tileBitmap(coverArtId) ?: loadArtwork(context, coverArtId, TILE_PX, TILE_RADIUS_PX)

    /**
     * Fond du widget, facon Spotify : degrade de la couleur dominante de la
     * pochette. Sur un cache miss il genere le bitmap (quelques ms) -> la version
     * non mise en cache est toujours appelee depuis un thread de fond.
     */
    private fun gradientBackground(
        artwork: Bitmap?,
        widthPx: Int,
        heightPx: Int,
        cornerRadiusPx: Float,
    ): Bitmap? {
        val color = WidgetBitmapUtils.dominantColor(artwork) ?: WidgetBitmapUtils.DEFAULT_WIDGET_COLOR
        return WidgetBitmapUtils.createGradientBackground(color, widthPx, heightPx, cornerRadiusPx)
    }

    /** Charge + arrondit une pochette (mise en cache), null si indisponible. */
    private suspend fun loadArtwork(context: Context, coverArtId: String, sizePx: Int, radiusPx: Float): Bitmap? {
        artworkBitmap(coverArtId, sizePx, radiusPx)?.let { return it }

        val url = CoverUrls.url(coverArtId, 512) ?: return null
        return try {
            val entry = EntryPointAccessors.fromApplication(
                context.applicationContext,
                WidgetEntryPoint::class.java,
            )
            val request = ImageRequest.Builder(context)
                .data(url)
                .size(sizePx, sizePx)
                .allowHardware(false)
                .build()
            val drawable = (entry.imageLoader().execute(request) as? SuccessResult)?.drawable
            val rawBitmap = (drawable as? BitmapDrawable)?.bitmap ?: return null
            val rounded = WidgetBitmapUtils.createSquareRoundedBitmap(rawBitmap, sizePx, radiusPx) ?: return null
            WidgetBitmapUtils.putCachedBitmap(artworkCacheKey(coverArtId, sizePx, radiusPx), rounded)
            rounded
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to load artwork for widget: $coverArtId", e)
            null
        }
    }

    private fun openAppPendingIntent(context: Context, requestCode: Int): PendingIntent =
        PendingIntent.getActivity(
            context,
            requestCode,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                setPackage(context.packageName)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun actionPendingIntent(context: Context, requestCode: Int, action: String): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, PlayerWidgetReceiver::class.java).apply {
                this.action = action
                setPackage(context.packageName)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
}
