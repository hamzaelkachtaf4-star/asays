package com.naviify.app.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import android.widget.RemoteViews
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.naviify.app.MainActivity
import com.naviify.app.R
import com.naviify.app.core.image.CoverUrls
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object PlayerWidgetUpdater {

    private const val TAG = "PlayerWidgetUpdater"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private const val PREFS_NAME = "naviify_playback_prefs"
    private const val KEY_LAST_TRACK_ID = "last_track_id"
    private const val KEY_LAST_TRACK_TITLE = "last_track_title"
    private const val KEY_LAST_TRACK_ARTIST = "last_track_artist"
    private const val KEY_LAST_TRACK_COVER_ID = "last_track_cover_id"

    private data class WidgetPlaybackState(
        val trackId: String?,
        val title: String,
        val artist: String,
        val isPlaying: Boolean,
        val isFavorite: Boolean,
        val coverArtId: String?,
    )

    private fun extractCurrentState(context: Context): WidgetPlaybackState {
        return try {
            val entry = EntryPointAccessors.fromApplication(
                context.applicationContext,
                WidgetEntryPoint::class.java,
            )
            val state = entry.playerQueueStore().state.value
            val currentTrack = state.currentTrack
            val isPlaying = state.isPlaying

            if (currentTrack != null) {
                WidgetPlaybackState(
                    trackId = currentTrack.id,
                    title = currentTrack.title.ifBlank { "ASAYS" },
                    artist = (currentTrack.artist ?: "").ifBlank { "Unknown Artist" },
                    isPlaying = isPlaying,
                    isFavorite = currentTrack.isFavorite,
                    coverArtId = currentTrack.coverArtId,
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
                        isFavorite = false,
                        coverArtId = prefs.getString(KEY_LAST_TRACK_COVER_ID, null),
                    )
                } else {
                    WidgetPlaybackState(
                        trackId = null,
                        title = "ASAYS",
                        artist = context.getString(R.string.widget_ready_to_play),
                        isPlaying = false,
                        isFavorite = false,
                        coverArtId = null,
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
                isFavorite = false,
                coverArtId = null,
            )
        }
    }

    fun updateAllWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
        val barIds = appWidgetManager.getAppWidgetIds(
            ComponentName(context, NowPlayingBarWidgetProvider::class.java),
        )
        val cardIds = appWidgetManager.getAppWidgetIds(
            ComponentName(context, NowPlayingCardWidgetProvider::class.java),
        )

        if (barIds.isEmpty() && cardIds.isEmpty()) return

        val state = extractCurrentState(context)

        if (barIds.isNotEmpty()) {
            renderBarWidgets(context, appWidgetManager, barIds, state)
        }
        if (cardIds.isNotEmpty()) {
            renderCardWidgets(context, appWidgetManager, cardIds, state)
        }
    }

    fun updateBarWidgets(context: Context, appWidgetManager: AppWidgetManager, widgetIds: IntArray) {
        if (widgetIds.isEmpty()) return
        val state = extractCurrentState(context)
        renderBarWidgets(context, appWidgetManager, widgetIds, state)
    }

    fun updateCardWidgets(context: Context, appWidgetManager: AppWidgetManager, widgetIds: IntArray) {
        if (widgetIds.isEmpty()) return
        val state = extractCurrentState(context)
        renderCardWidgets(context, appWidgetManager, widgetIds, state)
    }

    private fun renderBarWidgets(
        context: Context,
        appWidgetManager: AppWidgetManager,
        widgetIds: IntArray,
        state: WidgetPlaybackState,
    ) {
        val openAppIntent = PendingIntent.getActivity(
            context,
            10,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                setPackage(context.packageName)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val playPauseIntent = PendingIntent.getBroadcast(
            context,
            11,
            Intent(context, PlayerWidgetReceiver::class.java).apply {
                action = PlayerWidgetReceiver.ACTION_PLAY_PAUSE
                setPackage(context.packageName)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val nextIntent = PendingIntent.getBroadcast(
            context,
            12,
            Intent(context, PlayerWidgetReceiver::class.java).apply {
                action = PlayerWidgetReceiver.ACTION_NEXT
                setPackage(context.packageName)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val prevIntent = PendingIntent.getBroadcast(
            context,
            13,
            Intent(context, PlayerWidgetReceiver::class.java).apply {
                action = PlayerWidgetReceiver.ACTION_PREV
                setPackage(context.packageName)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val favoriteIntent = PendingIntent.getBroadcast(
            context,
            14,
            Intent(context, PlayerWidgetReceiver::class.java).apply {
                action = PlayerWidgetReceiver.ACTION_FAVORITE
                setPackage(context.packageName)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val barArtSizePx = 140
        val barRadiusPx = 16f
        val cacheKey = "bar-${state.coverArtId}-$barArtSizePx"
        val cachedBitmap = if (state.coverArtId != null) WidgetBitmapUtils.getCachedBitmap(cacheKey) else null

        for (widgetId in widgetIds) {
            try {
                val views = RemoteViews(context.packageName, R.layout.widget_now_playing_bar).apply {
                    setTextViewText(R.id.widget_title, state.title)
                    setTextViewText(R.id.widget_artist, state.artist)

                    setImageViewResource(
                        R.id.widget_btn_play_pause,
                        if (state.isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play,
                    )
                    setImageViewResource(
                        R.id.widget_btn_favorite,
                        if (state.isFavorite) R.drawable.ic_widget_heart_filled else R.drawable.ic_widget_heart,
                    )

                    setOnClickPendingIntent(R.id.widget_root, openAppIntent)
                    setOnClickPendingIntent(R.id.widget_open_app_area, openAppIntent)
                    setOnClickPendingIntent(R.id.widget_album_art, openAppIntent)
                    setOnClickPendingIntent(R.id.widget_title, openAppIntent)
                    setOnClickPendingIntent(R.id.widget_artist, openAppIntent)

                    setOnClickPendingIntent(R.id.widget_btn_play_pause, playPauseIntent)
                    setOnClickPendingIntent(R.id.widget_btn_next, nextIntent)
                    setOnClickPendingIntent(R.id.widget_btn_prev, prevIntent)
                    setOnClickPendingIntent(R.id.widget_btn_favorite, favoriteIntent)

                    if (cachedBitmap != null) {
                        setImageViewBitmap(R.id.widget_album_art, cachedBitmap)
                    } else {
                        setImageViewResource(R.id.widget_album_art, R.drawable.app_logo)
                    }
                }
                appWidgetManager.updateAppWidget(widgetId, views)
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to update bar widget: $widgetId", e)
            }
        }

        // Asynchronously load artwork if not cached
        if (state.coverArtId != null && cachedBitmap == null) {
            loadArtworkAsync(
                context = context,
                coverArtId = state.coverArtId,
                targetSizePx = barArtSizePx,
                radiusPx = barRadiusPx,
                cacheKey = cacheKey,
                widgetIds = widgetIds,
                layoutId = R.layout.widget_now_playing_bar,
                imageViewId = R.id.widget_album_art,
            )
        }
    }

    private fun renderCardWidgets(
        context: Context,
        appWidgetManager: AppWidgetManager,
        widgetIds: IntArray,
        state: WidgetPlaybackState,
    ) {
        val openAppIntent = PendingIntent.getActivity(
            context,
            20,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                setPackage(context.packageName)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val playPauseIntent = PendingIntent.getBroadcast(
            context,
            21,
            Intent(context, PlayerWidgetReceiver::class.java).apply {
                action = PlayerWidgetReceiver.ACTION_PLAY_PAUSE
                setPackage(context.packageName)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val nextIntent = PendingIntent.getBroadcast(
            context,
            22,
            Intent(context, PlayerWidgetReceiver::class.java).apply {
                action = PlayerWidgetReceiver.ACTION_NEXT
                setPackage(context.packageName)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val prevIntent = PendingIntent.getBroadcast(
            context,
            23,
            Intent(context, PlayerWidgetReceiver::class.java).apply {
                action = PlayerWidgetReceiver.ACTION_PREV
                setPackage(context.packageName)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val favoriteIntent = PendingIntent.getBroadcast(
            context,
            24,
            Intent(context, PlayerWidgetReceiver::class.java).apply {
                action = PlayerWidgetReceiver.ACTION_FAVORITE
                setPackage(context.packageName)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val cardArtSizePx = 240
        val cardRadiusPx = 20f
        val cacheKey = "card-${state.coverArtId}-$cardArtSizePx"
        val cachedBitmap = if (state.coverArtId != null) WidgetBitmapUtils.getCachedBitmap(cacheKey) else null

        for (widgetId in widgetIds) {
            try {
                val views = RemoteViews(context.packageName, R.layout.widget_now_playing_card).apply {
                    setTextViewText(R.id.widget_card_title, state.title)
                    setTextViewText(R.id.widget_card_artist, state.artist)

                    setImageViewResource(
                        R.id.widget_card_btn_play_pause,
                        if (state.isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play,
                    )
                    setImageViewResource(
                        R.id.widget_card_btn_favorite,
                        if (state.isFavorite) R.drawable.ic_widget_heart_filled else R.drawable.ic_widget_heart,
                    )

                    setOnClickPendingIntent(R.id.widget_card_root, openAppIntent)
                    setOnClickPendingIntent(R.id.widget_card_art, openAppIntent)
                    setOnClickPendingIntent(R.id.widget_card_title, openAppIntent)
                    setOnClickPendingIntent(R.id.widget_card_artist, openAppIntent)

                    setOnClickPendingIntent(R.id.widget_card_btn_play_pause, playPauseIntent)
                    setOnClickPendingIntent(R.id.widget_card_btn_next, nextIntent)
                    setOnClickPendingIntent(R.id.widget_card_btn_prev, prevIntent)
                    setOnClickPendingIntent(R.id.widget_card_btn_favorite, favoriteIntent)

                    if (cachedBitmap != null) {
                        setImageViewBitmap(R.id.widget_card_art, cachedBitmap)
                    } else {
                        setImageViewResource(R.id.widget_card_art, R.drawable.app_logo)
                    }
                }
                appWidgetManager.updateAppWidget(widgetId, views)
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to update card widget: $widgetId", e)
            }
        }

        // Asynchronously load artwork if not cached
        if (state.coverArtId != null && cachedBitmap == null) {
            loadArtworkAsync(
                context = context,
                coverArtId = state.coverArtId,
                targetSizePx = cardArtSizePx,
                radiusPx = cardRadiusPx,
                cacheKey = cacheKey,
                widgetIds = widgetIds,
                layoutId = R.layout.widget_now_playing_card,
                imageViewId = R.id.widget_card_art,
            )
        }
    }

    private fun loadArtworkAsync(
        context: Context,
        coverArtId: String,
        targetSizePx: Int,
        radiusPx: Float,
        cacheKey: String,
        widgetIds: IntArray,
        layoutId: Int,
        imageViewId: Int,
    ) {
        val url = CoverUrls.url(coverArtId, 512) ?: return

        scope.launch(Dispatchers.IO) {
            try {
                val entry = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    WidgetEntryPoint::class.java,
                )
                val imageLoader = entry.imageLoader()
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .size(targetSizePx, targetSizePx)
                    .allowHardware(false)
                    .build()
                val result = (imageLoader.execute(request) as? SuccessResult)?.drawable
                val rawBitmap = (result as? BitmapDrawable)?.bitmap
                if (rawBitmap != null) {
                    val rounded = WidgetBitmapUtils.createSquareRoundedBitmap(rawBitmap, targetSizePx, radiusPx)
                    if (rounded != null) {
                        WidgetBitmapUtils.putCachedBitmap(cacheKey, rounded)

                        val appWidgetManager = AppWidgetManager.getInstance(context) ?: return@launch
                        for (id in widgetIds) {
                            try {
                                val views = RemoteViews(context.packageName, layoutId).apply {
                                    setImageViewBitmap(imageViewId, rounded)
                                }
                                appWidgetManager.partiallyUpdateAppWidget(id, views)
                            } catch (e: Throwable) {
                                Log.w(TAG, "Failed to partially update artwork for widget $id", e)
                            }
                        }
                    }
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Failed to load artwork for widget: $coverArtId", e)
            }
        }
    }
}
