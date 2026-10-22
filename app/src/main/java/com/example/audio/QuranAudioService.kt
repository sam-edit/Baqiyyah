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
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class QuranAudioService : Service() {

    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var notificationManager: NotificationManager
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var tickerJob: Job? = null
    private var albumArtBitmap: Bitmap? = null

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()

        mediaSession = MediaSessionCompat(this, "QuranAudioSession").apply {
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
            )
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    QuranAudioPlayer.getInstance(this@QuranAudioService).resume()
                }

                override fun onPause() {
                    QuranAudioPlayer.getInstance(this@QuranAudioService).pause()
                }

                override fun onSkipToNext() {
                    QuranAudioPlayer.getInstance(this@QuranAudioService).onNextTrackListener?.invoke()
                }

                override fun onSkipToPrevious() {
                    QuranAudioPlayer.getInstance(this@QuranAudioService).onPreviousTrackListener?.invoke()
                }

                override fun onFastForward() {
                    QuranAudioPlayer.getInstance(this@QuranAudioService).seekForward(10000L)
                }

                override fun onRewind() {
                    QuranAudioPlayer.getInstance(this@QuranAudioService).seekBackward(10000L)
                }

                override fun onSeekTo(pos: Long) {
                    QuranAudioPlayer.getInstance(this@QuranAudioService).seekTo(pos)
                }

                override fun onStop() {
                    stopPlaybackAndService()
                }
            })
            isActive = true
        }

        try {
            albumArtBitmap = BitmapFactory.decodeResource(resources, R.drawable.al_deen_icon)
        } catch (_: Exception) {}

        observePlayerState()
    }

    private fun observePlayerState() {
        val player = QuranAudioPlayer.getInstance(this)
        serviceScope.launch {
            player.playbackState.collectLatest { state ->
                when (state) {
                    is AudioPlaybackState.Playing -> {
                        startProgressTicker()
                        updateNotificationAndSession(isPlaying = true)
                    }
                    is AudioPlaybackState.Paused -> {
                        stopProgressTicker()
                        updateNotificationAndSession(isPlaying = false)
                    }
                    is AudioPlaybackState.Loading -> {
                        updateNotificationAndSession(isPlaying = true)
                    }
                    is AudioPlaybackState.Idle -> {
                        stopProgressTicker()
                        updateNotificationAndSession(isPlaying = false)
                    }
                    is AudioPlaybackState.Error -> {
                        stopProgressTicker()
                        updateNotificationAndSession(isPlaying = false)
                    }
                }
            }
        }
    }

    private fun startProgressTicker() {
        tickerJob?.cancel()
        tickerJob = serviceScope.launch {
            val player = QuranAudioPlayer.getInstance(this@QuranAudioService)
            while (isActive && player.isPlaying.value) {
                // Update media session playback state every second so system seekbar stays synced
                updateSessionPlaybackState(
                    isPlaying = true,
                    positionMs = player.currentPositionMs.value,
                    speed = player.playbackSpeed.value
                )
                delay(1000L)
            }
        }
    }

    private fun stopProgressTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action != ACTION_STOP) stopped = false
        val player = QuranAudioPlayer.getInstance(this)

        when (action) {
            ACTION_PLAY -> player.resume()
            ACTION_PAUSE -> player.pause()
            ACTION_PLAY_PAUSE -> player.togglePlayPause()
            ACTION_NEXT -> player.onNextTrackListener?.invoke()
            ACTION_PREVIOUS -> player.onPreviousTrackListener?.invoke()
            ACTION_FORWARD_10 -> player.seekForward(10000L)
            ACTION_REWIND_10 -> player.seekBackward(10000L)
            ACTION_STOP -> {
                stopPlaybackAndService()
                return START_NOT_STICKY
            }
        }

        val metadata = player.currentTrackMetadata.value
        if (metadata != null) {
            updateNotificationAndSession(isPlaying = player.isPlaying.value)
        } else {
            stopPlaybackAndService()
        }

        return START_NOT_STICKY
    }

    private fun updateNotificationAndSession(isPlaying: Boolean) {
        if (stopped) return
        val player = QuranAudioPlayer.getInstance(this)
        val metadata = player.currentTrackMetadata.value ?: return
        val currentPos = player.currentPositionMs.value
        val duration = player.durationMs.value
        val speed = player.playbackSpeed.value

        // 1. Update MediaSession Metadata
        val title = "${metadata.surahNumber}. ${metadata.surahEnglishName} (${metadata.surahArabicName})"
        val artist = metadata.reciterName
        val album = "Al-Deen Holy Quran"

        val metadataBuilder = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
            .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
            .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, artist)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, album)
            .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION, "Surah ${metadata.surahNumber} • ${metadata.numberOfAyahs} Verses")
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)

        albumArtBitmap?.let {
            metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, it)
            metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, it)
            metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, it)
        }
        mediaSession.setMetadata(metadataBuilder.build())

        // 2. Update MediaSession PlaybackState
        updateSessionPlaybackState(isPlaying, currentPos, speed)

        // 3. Build Notification
        val notification = buildNotification(title, artist, metadata.surahNumber, isPlaying)

        // 4. Start / Update Foreground
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (_: Exception) {
            try {
                notificationManager.notify(NOTIFICATION_ID, notification)
            } catch (_: Exception) {}
        }
    }

    private fun updateSessionPlaybackState(isPlaying: Boolean, positionMs: Long, speed: Float) {
        val playbackStateBuilder = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PAUSE or
                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                PlaybackStateCompat.ACTION_FAST_FORWARD or
                PlaybackStateCompat.ACTION_REWIND or
                PlaybackStateCompat.ACTION_SEEK_TO or
                PlaybackStateCompat.ACTION_STOP
            )
            .setState(
                if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
                positionMs,
                speed
            )
        mediaSession.setPlaybackState(playbackStateBuilder.build())
    }

    private fun buildNotification(
        title: String,
        artist: String,
        surahNumber: Int,
        isPlaying: Boolean
    ): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "quran")
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            1001,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val prevPendingIntent = createActionPendingIntent(ACTION_PREVIOUS, 1002)
        val rewindPendingIntent = createActionPendingIntent(ACTION_REWIND_10, 1003)
        val playPausePendingIntent = createActionPendingIntent(ACTION_PLAY_PAUSE, 1004)
        val forwardPendingIntent = createActionPendingIntent(ACTION_FORWARD_10, 1005)
        val nextPendingIntent = createActionPendingIntent(ACTION_NEXT, 1006)
        val stopPendingIntent = createActionPendingIntent(ACTION_STOP, 1007)

        val mediaStyle = MediaStyle()
            .setMediaSession(mediaSession.sessionToken)
            // In compact view show: Previous (0), Play/Pause (2), Next (4)
            .setShowActionsInCompactView(0, 2, 4)
            .setShowCancelButton(true)
            .setCancelButtonIntent(stopPendingIntent)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setLargeIcon(albumArtBitmap)
            .setContentTitle(title)
            .setContentText(artist)
            .setSubText("Surah $surahNumber of 114")
            .setContentIntent(contentPendingIntent)
            .setDeleteIntent(stopPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(isPlaying)
            .setOnlyAlertOnce(true)
            .setStyle(mediaStyle)
            // Action 0: Previous Surah
            .addAction(
                android.R.drawable.ic_media_previous,
                "Previous",
                prevPendingIntent
            )
            // Action 1: Rewind 10s
            .addAction(
                android.R.drawable.ic_media_rew,
                "-10s",
                rewindPendingIntent
            )
            // Action 2: Play/Pause Toggle
            .addAction(
                if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (isPlaying) "Pause" else "Play",
                playPausePendingIntent
            )
            // Action 3: Forward 10s
            .addAction(
                android.R.drawable.ic_media_ff,
                "+10s",
                forwardPendingIntent
            )
            // Action 4: Next Surah
            .addAction(
                android.R.drawable.ic_media_next,
                "Next",
                nextPendingIntent
            )
            // Action 5: Close / Stop
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Close",
                stopPendingIntent
            )
            .build()
    }

    private fun createActionPendingIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, QuranAudioService::class.java).apply {
            this.action = action
        }
        return PendingIntent.getService(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Quran Audio Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Media controls and playback for Holy Quran recitations"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Requirement: "jab app recent sy clear kryn to band ho jaye or uper sy bhi chala jaye"
     * When user clears app from Android Recent Apps, onTaskRemoved is immediately called by system.
     */
    private var isStopping = false

    /** True after the service was told to stop; blocks late player-state emissions from re-posting the notification. */
    @Volatile
    private var stopped = false

    private fun stopPlaybackAndService() {
        if (isStopping) return
        isStopping = true
        stopped = true
        stopProgressTicker()
        try {
            val player = QuranAudioPlayer.getInstance(this)
            player.pause()
        } catch (_: Exception) {}

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
            notificationManager.cancel(NOTIFICATION_ID)
        } catch (_: Exception) {}

        try {
            mediaSession.isActive = false
        } catch (_: Exception) {}

        stopSelf()
        isStopping = false
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // App swiped away from Recents: stop playback + notification first, then
        // shut the player down and wipe the streaming cache immediately.
        stopPlaybackAndService()
        QuranAudioStreamingCacheManager.clearTempCache(applicationContext)
    }

    override fun onDestroy() {
        stopped = true
        stopProgressTicker()
        serviceScope.cancel()
        // clearTempCache() releases the ExoPlayer first, then deletes the cache - never the other way round
        QuranAudioStreamingCacheManager.clearTempCache(applicationContext)
        try {
            mediaSession.isActive = false
            mediaSession.release()
        } catch (_: Exception) {}
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL_ID = "quran_audio_playback_channel"
        const val NOTIFICATION_ID = 4001

        const val ACTION_START_OR_UPDATE = "com.example.audio.ACTION_START_OR_UPDATE"
        const val ACTION_PLAY = "com.example.audio.ACTION_PLAY"
        const val ACTION_PAUSE = "com.example.audio.ACTION_PAUSE"
        const val ACTION_PLAY_PAUSE = "com.example.audio.ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "com.example.audio.ACTION_NEXT"
        const val ACTION_PREVIOUS = "com.example.audio.ACTION_PREVIOUS"
        const val ACTION_FORWARD_10 = "com.example.audio.ACTION_FORWARD_10"
        const val ACTION_REWIND_10 = "com.example.audio.ACTION_REWIND_10"
        const val ACTION_STOP = "com.example.audio.ACTION_STOP"

        fun startOrUpdate(context: Context) {
            val intent = Intent(context, QuranAudioService::class.java).apply {
                action = ACTION_START_OR_UPDATE
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (_: Exception) {}
        }

        fun stop(context: Context) {
            val intent = Intent(context, QuranAudioService::class.java).apply {
                action = ACTION_STOP
            }
            try {
                context.startService(intent)
            } catch (_: Exception) {}
        }
    }
}
