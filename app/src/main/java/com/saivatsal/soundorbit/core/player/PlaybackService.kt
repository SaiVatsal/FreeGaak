package com.saivatsal.soundorbit.core.player

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.saivatsal.soundorbit.MainActivity
import com.saivatsal.soundorbit.R
import com.saivatsal.soundorbit.core.datastore.SettingsDataStore
import com.saivatsal.soundorbit.core.model.Track
import com.saivatsal.soundorbit.core.repository.FavoritesRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(UnstableApi::class, ExperimentalCoroutinesApi::class)
@AndroidEntryPoint
class PlaybackService : MediaLibraryService() {

    @Inject lateinit var crossfadePlayer: CrossfadePlayer
    @Inject lateinit var settingsDataStore: SettingsDataStore
    @Inject lateinit var favoritesRepository: FavoritesRepository

    private var mediaLibrarySession: MediaLibrarySession? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var isCurrentTrackFavorite = false

    companion object {
        const val ACTION_TOGGLE_FAVORITE = "com.saivatsal.soundorbit.ACTION_TOGGLE_FAVORITE"
    }

    private fun buildFavoriteButton(isFavorite: Boolean): CommandButton {
        val customCommand = SessionCommand(ACTION_TOGGLE_FAVORITE, Bundle.EMPTY)
        return CommandButton.Builder()
            .setDisplayName(if (isFavorite) "Favorite" else "Unfavorite")
            .setIconResId(if (isFavorite) R.drawable.ic_favorite_filled else R.drawable.ic_favorite_border)
            .setSessionCommand(customCommand)
            .build()
    }

    private val librarySessionCallback = object : MediaLibrarySession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                .add(SessionCommand(ACTION_TOGGLE_FAVORITE, Bundle.EMPTY))
                .build()

            val favoriteButton = buildFavoriteButton(isCurrentTrackFavorite)

            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(sessionCommands)
                .setCustomLayout(ImmutableList.of(favoriteButton))
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            if (customCommand.customAction == ACTION_TOGGLE_FAVORITE) {
                val currentTrack = crossfadePlayer.snapshot.value.currentTrack
                if (currentTrack != null) {
                    serviceScope.launch {
                        favoritesRepository.toggleFavorite(currentTrack)
                    }
                }
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            return super.onCustomCommand(session, controller, customCommand, args)
        }

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val rootItem = MediaItem.Builder()
                .setMediaId("ROOT")
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle("SoundOrbit")
                        .setIsPlayable(false)
                        .setIsBrowsable(true)
                        .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                        .build()
                )
                .build()
            return Futures.immediateFuture(LibraryResult.ofItem(rootItem, params))
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            val items = when (parentId) {
                "ROOT" -> listOf(
                    buildFolderItem("FAVORITES", "Favorites"),
                    buildFolderItem("DOWNLOADS", "Downloads"),
                    buildFolderItem("LOCAL", "Local Media")
                )
                else -> emptyList()
            }
            return Futures.immediateFuture(LibraryResult.ofItemList(ImmutableList.copyOf(items), params))
        }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String
        ): ListenableFuture<LibraryResult<MediaItem>> {
            val item = MediaItem.Builder()
                .setMediaId(mediaId)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(mediaId)
                        .build()
                )
                .build()
            return Futures.immediateFuture(LibraryResult.ofItem(item, null))
        }
    }

    private fun buildFolderItem(mediaId: String, title: String): MediaItem {
        return MediaItem.Builder()
            .setMediaId(mediaId)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setIsPlayable(false)
                    .setIsBrowsable(true)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                    .build()
            )
            .build()
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Create initial session with the currently active player instance
        mediaLibrarySession = MediaLibrarySession.Builder(
            this,
            crossfadePlayer.getActivePlayerInstance(),
            librarySessionCallback
        )
            .setSessionActivity(pendingIntent)
            .build()

        serviceScope.launch {
            settingsDataStore.settings.collectLatest { settings ->
                crossfadePlayer.setCrossfadeDuration(settings.crossfadeDurationSec)
            }
        }

        serviceScope.launch {
            crossfadePlayer.snapshot
                .map { it.currentTrack }
                .distinctUntilChanged()
                .flatMapLatest { track ->
                    if (track != null) {
                        favoritesRepository.isFavorite(track.compositeKey)
                    } else {
                        flowOf(false)
                    }
                }
                .collectLatest { isFav ->
                    isCurrentTrackFavorite = isFav
                    mediaLibrarySession?.let { session ->
                        val button = buildFavoriteButton(isFav)
                        session.setCustomLayout(ImmutableList.of(button))
                    }
                }
        }

        // Observe CrossfadePlayer's active player changes and sync MediaLibrarySession
        serviceScope.launch {
            crossfadePlayer.activePlayerInstance
                .collectLatest { activePlayer ->
                    mediaLibrarySession?.let { session ->
                        // Update the MediaLibrarySession's player to match the active crossfade player
                        session.player = activePlayer
                    }
                }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? {
        return mediaLibrarySession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Only stop the service if no media is playing or queued
        // Keep the service alive for background playback when user swipes away the app
        val player = mediaLibrarySession?.player
        val shouldStop = player == null || (!player.playWhenReady && player.mediaItemCount == 0)
        if (shouldStop) {
            stopSelf()
        }
        // Otherwise keep service running for background playback
    }

    override fun onDestroy() {
        mediaLibrarySession?.run {
            player.release()
            release()
            mediaLibrarySession = null
        }
        serviceScope.cancel()
        crossfadePlayer.release()
        super.onDestroy()
    }
}
