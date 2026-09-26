package com.naviify.app.ui.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.naviify.app.MainActivity
import com.naviify.app.domain.model.FavoriteType
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PlayerWidgetReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val pendingResult = goAsync()

        receiverScope.launch(Dispatchers.Main) {
            try {
                handleActionInternal(context, action)
            } catch (e: Throwable) {
                Log.e(TAG, "Error handling widget action $action", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "PlayerWidgetReceiver"
        private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

        const val ACTION_PLAY_PAUSE = "com.naviify.app.widget.ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "com.naviify.app.widget.ACTION_NEXT"
        const val ACTION_PREV = "com.naviify.app.widget.ACTION_PREV"
        const val ACTION_FAVORITE = "com.naviify.app.widget.ACTION_FAVORITE"

        fun handleAction(context: Context, action: String) {
            receiverScope.launch(Dispatchers.Main) {
                try {
                    handleActionInternal(context, action)
                } catch (e: Throwable) {
                    Log.e(TAG, "Error in handleAction for $action", e)
                }
            }
        }

        private suspend fun handleActionInternal(context: Context, action: String) {
            val entry = try {
                EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    WidgetEntryPoint::class.java,
                )
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to get WidgetEntryPoint", e)
                return
            }

            val playbackController = entry.playbackController()
            val queueStore = entry.playerQueueStore()
            val favoritesRepository = entry.favoritesRepository()

            when (action) {
                ACTION_PLAY_PAUSE -> {
                    val current = queueStore.state.value
                    if (current.currentTrack == null && current.queue.isEmpty()) {
                        val launchIntent = Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        }
                        context.startActivity(launchIntent)
                    } else {
                        playbackController.togglePlayPause()
                        PlayerWidgetUpdater.updateAllWidgets(context)
                    }
                }

                ACTION_NEXT -> {
                    playbackController.next()
                    PlayerWidgetUpdater.updateAllWidgets(context)
                }

                ACTION_PREV -> {
                    playbackController.previous()
                    PlayerWidgetUpdater.updateAllWidgets(context)
                }

                ACTION_FAVORITE -> {
                    val currentTrack = queueStore.state.value.currentTrack
                    if (currentTrack != null) {
                        try {
                            favoritesRepository.toggleFavorite(
                                id = currentTrack.id,
                                type = FavoriteType.TRACK,
                                name = currentTrack.title,
                                secondaryText = currentTrack.artist,
                                coverArtId = currentTrack.coverArtId,
                            )
                            PlayerWidgetUpdater.updateAllWidgets(context)
                        } catch (e: Throwable) {
                            Log.e(TAG, "Failed to toggle favorite from widget", e)
                        }
                    }
                }
            }
        }
    }
}
