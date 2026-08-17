package com.example.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import coil.ImageLoader
import coil.request.ImageRequest
import com.example.MainActivity
import com.example.R
import com.example.data.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MusicPlaybackService : Service() {

    private val binder = LocalBinder()
    private var isForegroundRunning = false
    private var lastSong: Song? = null
    private var lastPlayingState: Boolean = false
    private var lastPositionMs: Long = 0L
    private var lastDurationMs: Long = 210000L

    private var mediaSession: MediaSessionCompat? = null
    private var currentAlbumBitmap: Bitmap? = null
    private var lastCoverUrl: String = ""
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    inner class LocalBinder : Binder() {
        fun getService(): MusicPlaybackService = this@MusicPlaybackService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        ServiceAudioBridge.currentService = this
        createNotificationChannel()
        initMediaSession()
        try {
            val initialNotification = buildNotification(lastSong, lastPlayingState, lastPositionMs, lastDurationMs)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    initialNotification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(NOTIFICATION_ID, initialNotification)
            }
            isForegroundRunning = true
        } catch (e: Exception) {
            Log.w("MusicPlaybackService", "Error in initial startForeground: ${e.message}")
        }
    }

    private fun initMediaSession() {
        try {
            mediaSession = MediaSessionCompat(this, "SKMusicPlaybackSession").apply {
                setCallback(object : MediaSessionCompat.Callback() {
                    override fun onPlay() {
                        ServiceAudioBridge.onTogglePlayPause?.invoke()
                    }

                    override fun onPause() {
                        ServiceAudioBridge.onTogglePlayPause?.invoke()
                    }

                    override fun onSkipToNext() {
                        ServiceAudioBridge.onNext?.invoke()
                    }

                    override fun onSkipToPrevious() {
                        ServiceAudioBridge.onPrevious?.invoke()
                    }

                    override fun onStop() {
                        ServiceAudioBridge.onStopPlayback?.invoke()
                        stopForegroundService()
                    }

                    override fun onSeekTo(pos: Long) {
                        ServiceAudioBridge.onSeekTo?.invoke(pos)
                    }
                })
                isActive = true
            }
        } catch (e: Exception) {
            Log.e("MusicPlaybackService", "Error initializing MediaSession", e)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            when (intent?.action) {
                ACTION_PLAY_PAUSE -> {
                    ServiceAudioBridge.onTogglePlayPause?.invoke()
                }
                ACTION_NEXT -> {
                    ServiceAudioBridge.onNext?.invoke()
                }
                ACTION_PREVIOUS -> {
                    ServiceAudioBridge.onPrevious?.invoke()
                }
                ACTION_STOP -> {
                    ServiceAudioBridge.onStopPlayback?.invoke()
                    stopForegroundService()
                }
                ACTION_SEEK -> {
                    val pos = intent.getLongExtra(EXTRA_SEEK_POSITION, 0L)
                    ServiceAudioBridge.onSeekTo?.invoke(pos)
                }
            }

            // Ensure foreground status is maintained on Android 8+
            if (!isForegroundRunning) {
                val notif = buildNotification(lastSong, lastPlayingState, lastPositionMs, lastDurationMs)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(NOTIFICATION_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
                } else {
                    startForeground(NOTIFICATION_ID, notif)
                }
                isForegroundRunning = true
            }
        } catch (e: Exception) {
            Log.w("MusicPlaybackService", "Error handling onStartCommand: ${e.message}")
        }
        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d("MusicPlaybackService", "App swiped away from recents box, stopping playback and service immediately")
        try {
            ServiceAudioBridge.onStopPlayback?.invoke()
            stopForegroundService()
        } catch (e: Exception) {
            Log.w("MusicPlaybackService", "Error in onTaskRemoved: ${e.message}")
        }
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SK Music Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows interactive playback timeline, controls, and skip actions"
                setShowBadge(false)
                setSound(null, null)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun updateNotification(
        song: Song?,
        isPlaying: Boolean,
        positionMs: Long = lastPositionMs,
        durationMs: Long = lastDurationMs
    ) {
        lastSong = song
        lastPlayingState = isPlaying
        lastPositionMs = positionMs
        lastDurationMs = durationMs

        if (song == null) {
            stopForegroundService()
            return
        }

        // Fetch artwork if changed
        if (song.imageUrl != lastCoverUrl && song.imageUrl.isNotBlank()) {
            lastCoverUrl = song.imageUrl
            loadArtwork(song.imageUrl)
        }

        updateMediaSessionState(song, isPlaying, positionMs, durationMs)

        try {
            val notification = buildNotification(song, isPlaying, positionMs, durationMs)
            if (!isForegroundRunning) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
                isForegroundRunning = true
            } else {
                val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                manager?.notify(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.w("MusicPlaybackService", "updateNotification failed: ${e.message}")
        }
    }

    private fun loadArtwork(url: String) {
        scope.launch {
            try {
                val loader = ImageLoader(this@MusicPlaybackService)
                val request = ImageRequest.Builder(this@MusicPlaybackService)
                    .data(url)
                    .allowHardware(false)
                    .build()
                val result = loader.execute(request).drawable
                if (result is BitmapDrawable) {
                    currentAlbumBitmap = result.bitmap
                    val song = lastSong
                    if (song != null) {
                        updateNotification(song, lastPlayingState, lastPositionMs, lastDurationMs)
                    }
                }
            } catch (_: Exception) {
                // Ignore artwork loading issues gracefully
            }
        }
    }

    private fun updateMediaSessionState(song: Song, isPlaying: Boolean, positionMs: Long, durationMs: Long) {
        try {
            val state = if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
            val actions = PlaybackStateCompat.ACTION_PLAY_PAUSE or
                    PlaybackStateCompat.ACTION_PLAY or
                    PlaybackStateCompat.ACTION_PAUSE or
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                    PlaybackStateCompat.ACTION_STOP or
                    PlaybackStateCompat.ACTION_SEEK_TO

            val playbackState = PlaybackStateCompat.Builder()
                .setActions(actions)
                .setState(state, positionMs, 1.0f, SystemClock.elapsedRealtime())
                .build()

            mediaSession?.setPlaybackState(playbackState)

            val metadataBuilder = MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, song.title)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, song.artist)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, song.album)
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, "${song.artist} • ${song.category}")
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, durationMs)

            currentAlbumBitmap?.let {
                metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, it)
                metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, it)
            }

            mediaSession?.setMetadata(metadataBuilder.build())
        } catch (e: Exception) {
            Log.w("MusicPlaybackService", "Error updating MediaSession state: ${e.message}")
        }
    }

    private fun buildNotification(
        song: Song?,
        isPlaying: Boolean,
        positionMs: Long,
        durationMs: Long
    ): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Previous Action (Skip Previous)
        val prevIntent = Intent(this, MusicPlaybackService::class.java).apply { action = ACTION_PREVIOUS }
        val prevPendingIntent = PendingIntent.getService(this, 1, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        // Play/Pause Action
        val playPauseIntent = Intent(this, MusicPlaybackService::class.java).apply { action = ACTION_PLAY_PAUSE }
        val playPausePendingIntent = PendingIntent.getService(this, 2, playPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        // Next Action (Skip Next)
        val nextIntent = Intent(this, MusicPlaybackService::class.java).apply { action = ACTION_NEXT }
        val nextPendingIntent = PendingIntent.getService(this, 3, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        // Stop / Close Action
        val stopIntent = Intent(this, MusicPlaybackService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(this, 4, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val playPauseIcon = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        val playPauseTitle = if (isPlaying) "Pause" else "Play"

        val title = song?.title ?: "SK Edz Player"
        val subtitle = if (song != null) "${song.artist} • ${song.category}" else "Ready to Play"

        val mediaStyle = androidx.media.app.NotificationCompat.MediaStyle()
            .setShowActionsInCompactView(0, 1, 2)
            .setShowCancelButton(true)
            .setCancelButtonIntent(stopPendingIntent)

        mediaSession?.sessionToken?.let {
            mediaStyle.setMediaSession(it)
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setSubText(if (song?.isVipOnly == true) "👑 VIP HD Audio" else "SK EDZ")
            .setContentIntent(openAppPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(isPlaying)
            .setOnlyAlertOnce(true)
            .setStyle(mediaStyle)
            .addAction(android.R.drawable.ic_media_previous, "Previous", prevPendingIntent)
            .addAction(playPauseIcon, playPauseTitle, playPausePendingIntent)
            .addAction(android.R.drawable.ic_media_next, "Next", nextPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)

        currentAlbumBitmap?.let {
            builder.setLargeIcon(it)
        }

        return builder.build()
    }

    private fun stopForegroundService() {
        isForegroundRunning = false
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
        } catch (e: Exception) {
            Log.w("MusicPlaybackService", "Error in stopForeground: ${e.message}")
        }
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (ServiceAudioBridge.currentService == this) {
            ServiceAudioBridge.currentService = null
        }
        try {
            mediaSession?.isActive = false
            mediaSession?.release()
            mediaSession = null
        } catch (e: Exception) {
            Log.w("MusicPlaybackService", "Error releasing MediaSession: ${e.message}")
        }
        isForegroundRunning = false
    }

    companion object {
        const val CHANNEL_ID = "sk_media_playback_channel"
        const val NOTIFICATION_ID = 9001

        const val ACTION_PLAY_PAUSE = "com.example.action.PLAY_PAUSE"
        const val ACTION_NEXT = "com.example.action.NEXT"
        const val ACTION_PREVIOUS = "com.example.action.PREVIOUS"
        const val ACTION_STOP = "com.example.action.STOP"
        const val ACTION_SEEK = "com.example.action.SEEK"
        const val EXTRA_SEEK_POSITION = "extra_seek_position"
    }
}

object ServiceAudioBridge {
    var currentService: MusicPlaybackService? = null
    var onTogglePlayPause: (() -> Unit)? = null
    var onNext: (() -> Unit)? = null
    var onPrevious: (() -> Unit)? = null
    var onStopPlayback: (() -> Unit)? = null
    var onSeekTo: ((Long) -> Unit)? = null
}

