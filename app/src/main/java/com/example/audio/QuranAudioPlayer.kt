package com.example.audio

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.extractor.DefaultExtractorsFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

enum class AudioRepeatMode {
    OFF,
    REPEAT_ONE,
    REPEAT_ALL
}

sealed class AudioPlaybackState {
    object Idle : AudioPlaybackState()
    data class Loading(val trackId: String) : AudioPlaybackState()
    data class Playing(
        val trackId: String,
        val progress: Float = 0f,
        val currentPositionMs: Long = 0L,
        val durationMs: Long = 0L
    ) : AudioPlaybackState()
    data class Paused(
        val trackId: String,
        val progress: Float = 0f,
        val currentPositionMs: Long = 0L,
        val durationMs: Long = 0L
    ) : AudioPlaybackState()
    data class Error(val message: String) : AudioPlaybackState()
}

data class CurrentTrackMetadata(
    val surahNumber: Int,
    val surahEnglishName: String,
    val surahArabicName: String,
    val surahUrduName: String,
    val reciterName: String,
    val reciterId: String,
    val numberOfAyahs: Int = 0
)

/**
 * Quran audio player built on Media3 ExoPlayer.
 *
 * - Remote audio is STREAMED: playback starts after ~1s of buffering while the rest keeps loading
 *   in the background (like social-media video). Everything that is loaded is also kept in a
 *   session cache, so seeking back is instant and offline-safe; seeking forward only loads the
 *   part that is needed.
 * - Downloaded (local) files are played directly from disk.
 * - Mid-playback network problems are retried silently at the same position instead of failing.
 *
 * Must be used from the main thread (as before).
 */
@OptIn(UnstableApi::class)
class QuranAudioPlayer(private val context: Context) {
    private var exoPlayer: ExoPlayer? = null
    private var isPrepared: Boolean = false
    private var currentTrackId: String? = null
    private var tickerJob: Job? = null
    private var retryJob: Job? = null
    private var errorRetryCount: Int = 0
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _currentTrackMetadata = MutableStateFlow<CurrentTrackMetadata?>(null)
    val currentTrackMetadata: StateFlow<CurrentTrackMetadata?> = _currentTrackMetadata.asStateFlow()

    var onNextTrackListener: (() -> Unit)? = null
    var onPreviousTrackListener: (() -> Unit)? = null

    private val _playbackState = MutableStateFlow<AudioPlaybackState>(AudioPlaybackState.Idle)
    val playbackState: StateFlow<AudioPlaybackState> = _playbackState.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _repeatMode = MutableStateFlow(AudioRepeatMode.OFF)
    val repeatMode: StateFlow<AudioRepeatMode> = _repeatMode.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private var onCompletionCallback: (() -> Unit)? = null

    fun setOnCompletionListener(callback: () -> Unit) {
        onCompletionCallback = callback
    }

    fun toggleMute() {
        _isMuted.value = !_isMuted.value
        applyVolume()
    }

    fun setMuted(muted: Boolean) {
        _isMuted.value = muted
        applyVolume()
    }

    private fun applyVolume() {
        val vol = if (_isMuted.value) 0f else 1f
        try {
            exoPlayer?.volume = vol
        } catch (_: Exception) {}
    }

    fun restartTrack() {
        seekTo(0L)
        if (!_isPlaying.value) {
            resume()
        }
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
            pause()
        } else {
            resume()
        }
    }

    private fun internalResetPlayer() {
        retryJob?.cancel()
        retryJob = null
        stopProgressTicker()
        _isPlaying.value = false
        isPrepared = false
        val exo = exoPlayer
        exoPlayer = null // listeners ignore callbacks of a player that is no longer current
        if (exo != null) {
            try { exo.stop() } catch (_: Exception) {}
            try { exo.release() } catch (_: Exception) {}
        }
    }

    private fun isLocalPath(urlOrPath: String) = urlOrPath.startsWith("/")

    private fun safeDuration(exo: ExoPlayer): Long {
        val d = try { exo.duration } catch (_: Exception) { C.TIME_UNSET }
        return if (d == C.TIME_UNSET || d < 0L) 0L else d
    }

    private fun safePosition(exo: ExoPlayer?): Long {
        return try {
            exo?.currentPosition?.coerceAtLeast(0L) ?: _currentPositionMs.value
        } catch (_: Exception) {
            _currentPositionMs.value
        }
    }

    fun play(
        urlOrPath: String,
        trackId: String,
        metadata: CurrentTrackMetadata? = null,
        fallbackUrl: String? = null,
        startPositionMs: Long = 0L
    ) {
        if (metadata != null) {
            _currentTrackMetadata.value = metadata
        }
        // Same track already loaded -> behave as a play/pause toggle
        if (currentTrackId == trackId && exoPlayer != null && isPrepared) {
            if (_isPlaying.value) {
                pause()
            } else {
                resume()
            }
            return
        }

        internalResetPlayer()
        currentTrackId = trackId
        errorRetryCount = 0
        _currentPositionMs.value = startPositionMs.coerceAtLeast(0L)
        _durationMs.value = 0L
        _playbackState.value = AudioPlaybackState.Loading(trackId)
        QuranAudioService.startOrUpdate(context)

        try {
            val file = File(urlOrPath)
            val isLocalFile = isLocalPath(urlOrPath) && file.exists() && file.length() > 0
            val uri = if (isLocalFile) Uri.fromFile(file) else Uri.parse(urlOrPath)

            // Local file: straight from disk. Remote: network + progressive session cache.
            val dataSourceFactory = if (isLocalFile) {
                QuranAudioStreamingCacheManager.createLocalDataSourceFactory(context)
            } else {
                QuranAudioStreamingCacheManager.createStreamingDataSourceFactory(context)
            }
            // Constant-bitrate seeking lets users jump forward even in MP3s without a seek table
            val extractorsFactory = DefaultExtractorsFactory().setConstantBitrateSeekingEnabled(true)
            val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory, extractorsFactory)
                .createMediaSource(MediaItem.fromUri(uri))

            // Small start threshold = playback begins almost immediately; large look-ahead = smooth.
            val loadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    /* minBufferMs = */ 15_000,
                    /* maxBufferMs = */ 120_000,
                    /* bufferForPlaybackMs = */ 1_000,
                    /* bufferForPlaybackAfterRebufferMs = */ 3_000
                )
                .setPrioritizeTimeOverSizeThresholds(true)
                .build()

            val exo = ExoPlayer.Builder(context)
                .setLoadControl(loadControl)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .setUsage(C.USAGE_MEDIA)
                        .build(),
                    /* handleAudioFocus = */ true
                )
                .setHandleAudioBecomingNoisy(true)
                .setWakeMode(C.WAKE_MODE_NETWORK)
                .build()

            exoPlayer = exo
            exo.addListener(createListener(exo, trackId, urlOrPath, metadata, fallbackUrl))
            applyVolume()
            applyPlaybackSpeed()
            exo.setMediaSource(mediaSource, startPositionMs.coerceAtLeast(0L))
            exo.playWhenReady = true
            exo.prepare()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to start playback", e)
            failOrFallback(
                trackId = trackId,
                urlOrPath = urlOrPath,
                metadata = metadata,
                fallbackUrl = fallbackUrl,
                message = e.message ?: "Failed to play audio",
                resumeAtMs = 0L
            )
        }
    }

    private fun createListener(
        exo: ExoPlayer,
        trackId: String,
        urlOrPath: String,
        metadata: CurrentTrackMetadata?,
        fallbackUrl: String?
    ): Player.Listener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (exoPlayer !== exo) return
            when (playbackState) {
                Player.STATE_READY -> onReady(exo, trackId)
                Player.STATE_ENDED -> onEnded(exo)
                else -> Unit // BUFFERING: keep current UI state, ExoPlayer resumes by itself
            }
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            if (exoPlayer !== exo || !isPrepared) return
            // Changes that did not come from our own pause()/resume() (audio focus, headset unplugged)
            if (!playWhenReady && _isPlaying.value) {
                pause()
            } else if (playWhenReady && !_isPlaying.value) {
                resume()
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            if (exoPlayer !== exo) return
            Log.w(TAG, "Player error: ${error.errorCodeName}", error)
            handlePlayerError(exo, trackId, urlOrPath, metadata, fallbackUrl, error)
        }
    }

    private fun onReady(exo: ExoPlayer, trackId: String) {
        val duration = safeDuration(exo)
        if (duration > 0L) _durationMs.value = duration

        if (isPrepared) return // just recovered from buffering / a retry

        isPrepared = true
        errorRetryCount = 0
        applyVolume()
        applyPlaybackSpeed()
        if (exo.playWhenReady) {
            _isPlaying.value = true
            updateStatePlaying(trackId)
            startProgressTicker()
        } else {
            // user paused while it was still loading
            val pos = safePosition(exo)
            val dur = _durationMs.value
            val prog = if (dur > 0) (pos.toFloat() / dur.toFloat()).coerceIn(0f, 1f) else 0f
            _playbackState.value = AudioPlaybackState.Paused(trackId, prog, pos, dur)
        }
        QuranAudioService.startOrUpdate(context)
    }

    private fun onEnded(exo: ExoPlayer) {
        _isPlaying.value = false
        stopProgressTicker()
        if (_repeatMode.value == AudioRepeatMode.REPEAT_ONE && isPrepared) {
            try {
                exo.seekTo(0L)
                exo.playWhenReady = true
                exo.play()
                _isPlaying.value = true
                currentTrackId?.let { updateStatePlaying(it) }
                startProgressTicker()
                QuranAudioService.startOrUpdate(context)
                return
            } catch (_: Exception) {}
        }
        _playbackState.value = AudioPlaybackState.Idle
        _currentPositionMs.value = 0L
        currentTrackId = null
        internalResetPlayer()
        QuranAudioService.startOrUpdate(context)
        onCompletionCallback?.invoke()
    }

    private fun handlePlayerError(
        exo: ExoPlayer,
        trackId: String,
        urlOrPath: String,
        metadata: CurrentTrackMetadata?,
        fallbackUrl: String?,
        error: PlaybackException
    ) {
        val resumeAt = if (isPrepared) safePosition(exo) else 0L

        // 1) Network hiccup while playing: retry the SAME source at the SAME position.
        //    Already loaded parts come from the cache, so the user just hears a short pause.
        if (isPrepared && errorRetryCount < MAX_SAME_SOURCE_RETRIES) {
            errorRetryCount++
            val attempt = errorRetryCount
            retryJob?.cancel()
            retryJob = scope.launch {
                delay(1_500L * attempt)
                if (exoPlayer === exo && currentTrackId == trackId) {
                    try {
                        exo.seekTo(resumeAt)
                        exo.prepare()
                    } catch (e: Exception) {
                        failOrFallback(trackId, urlOrPath, metadata, fallbackUrl, e.message ?: "Playback error", resumeAt)
                    }
                }
            }
            return
        }

        // 2) Retry exhausted (or failed before start): try the fallback mirror, else show error.
        failOrFallback(
            trackId = trackId,
            urlOrPath = urlOrPath,
            metadata = metadata,
            fallbackUrl = fallbackUrl,
            message = "Playback error (${error.errorCodeName})",
            resumeAtMs = resumeAt
        )
    }

    private fun failOrFallback(
        trackId: String,
        urlOrPath: String,
        metadata: CurrentTrackMetadata?,
        fallbackUrl: String?,
        message: String,
        resumeAtMs: Long
    ) {
        internalResetPlayer()
        if (fallbackUrl != null && fallbackUrl != urlOrPath && !isLocalPath(urlOrPath)) {
            scope.launch {
                // Only continue if the user did not start something else in the meantime
                if (currentTrackId == trackId && exoPlayer == null) {
                    play(fallbackUrl, trackId, metadata, fallbackUrl = null, startPositionMs = resumeAtMs)
                }
            }
        } else {
            _isPlaying.value = false
            _playbackState.value = AudioPlaybackState.Error(message)
            currentTrackId = null
            QuranAudioService.startOrUpdate(context)
        }
    }

    fun pause() {
        val exo = exoPlayer ?: return
        _isPlaying.value = false // set first so the listener does not treat it as an external change
        stopProgressTicker()
        try { exo.pause() } catch (_: Exception) {}
        val currentPos = safePosition(exo)
        val dur = _durationMs.value
        val prog = if (dur > 0) (currentPos.toFloat() / dur.toFloat()).coerceIn(0f, 1f) else 0f
        currentTrackId?.let { id ->
            _playbackState.value = AudioPlaybackState.Paused(
                trackId = id,
                progress = prog,
                currentPositionMs = currentPos,
                durationMs = dur
            )
        }
        QuranAudioService.startOrUpdate(context)
    }

    fun resume() {
        val exo = exoPlayer ?: return
        if (_isPlaying.value) return
        try {
            if (isPrepared) {
                _isPlaying.value = true // set first, see pause()
                exo.play()
                applyPlaybackSpeed()
                currentTrackId?.let { updateStatePlaying(it) }
                startProgressTicker()
            } else {
                // still loading: just let it start as soon as it is ready
                exo.playWhenReady = true
                currentTrackId?.let { _playbackState.value = AudioPlaybackState.Loading(it) }
            }
            QuranAudioService.startOrUpdate(context)
        } catch (_: Exception) {
            _isPlaying.value = false
        }
    }

    fun stop() {
        internalResetPlayer()
        currentTrackId = null
        _currentPositionMs.value = 0L
        _playbackState.value = AudioPlaybackState.Idle
        QuranAudioService.stop(context)
    }

    /**
     * Fully releases the player WITHOUT touching the service. Used right before the session cache
     * is wiped (a player must never read from a released cache).
     */
    fun shutdown() {
        internalResetPlayer()
        currentTrackId = null
        _currentPositionMs.value = 0L
        _durationMs.value = 0L
        _playbackState.value = AudioPlaybackState.Idle
    }

    fun seekTo(positionMs: Long) {
        val exo = exoPlayer ?: return
        var target = positionMs.coerceAtLeast(0L)
        val dur = _durationMs.value
        if (dur > 0L) target = target.coerceAtMost(dur)
        try {
            // Forward seek into a not-yet-loaded part: ExoPlayer fetches just that part into the
            // cache and resumes playing as soon as ~1s is buffered. Backward seek: served from cache.
            exo.seekTo(target)
            _currentPositionMs.value = target
            currentTrackId?.let { trackId ->
                val prog = if (dur > 0) (target.toFloat() / dur.toFloat()).coerceIn(0f, 1f) else 0f
                if (_isPlaying.value) {
                    _playbackState.value = AudioPlaybackState.Playing(trackId, prog, target, dur)
                } else if (isPrepared) {
                    _playbackState.value = AudioPlaybackState.Paused(trackId, prog, target, dur)
                }
            }
            QuranAudioService.startOrUpdate(context)
        } catch (_: Exception) {}
    }

    fun seekForward(deltaMs: Long = 10000L) {
        val cur = if (exoPlayer != null) safePosition(exoPlayer) else _currentPositionMs.value
        val dur = if (_durationMs.value > 0) _durationMs.value else cur + deltaMs
        val target = (cur + deltaMs).coerceAtMost(dur)
        seekTo(target)
    }

    fun seekBackward(deltaMs: Long = 10000L) {
        val cur = if (exoPlayer != null) safePosition(exoPlayer) else _currentPositionMs.value
        val target = (cur - deltaMs).coerceAtLeast(0L)
        seekTo(target)
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        applyPlaybackSpeed()
        QuranAudioService.startOrUpdate(context)
    }

    fun setRepeatMode(mode: AudioRepeatMode) {
        _repeatMode.value = mode
    }

    fun toggleRepeatMode() {
        _repeatMode.value = when (_repeatMode.value) {
            AudioRepeatMode.OFF -> AudioRepeatMode.REPEAT_ONE
            AudioRepeatMode.REPEAT_ONE -> AudioRepeatMode.REPEAT_ALL
            AudioRepeatMode.REPEAT_ALL -> AudioRepeatMode.OFF
        }
    }

    private fun applyPlaybackSpeed() {
        try {
            exoPlayer?.playbackParameters = PlaybackParameters(_playbackSpeed.value)
        } catch (_: Exception) {}
    }

    private fun updateStatePlaying(trackId: String) {
        val pos = safePosition(exoPlayer)
        val dur = _durationMs.value
        val prog = if (dur > 0) (pos.toFloat() / dur.toFloat()).coerceIn(0f, 1f) else 0f
        _playbackState.value = AudioPlaybackState.Playing(
            trackId = trackId,
            progress = prog,
            currentPositionMs = pos,
            durationMs = dur
        )
    }

    private fun startProgressTicker() {
        stopProgressTicker()
        tickerJob = scope.launch {
            while (isActive && _isPlaying.value) {
                try {
                    val exo = exoPlayer
                    if (exo != null && isPrepared) {
                        val pos = exo.currentPosition.coerceAtLeast(0L)
                        val dur = safeDuration(exo)
                        _currentPositionMs.value = pos
                        if (dur > 0) _durationMs.value = dur
                        val knownDur = _durationMs.value
                        currentTrackId?.let { trackId ->
                            val prog = if (knownDur > 0) (pos.toFloat() / knownDur.toFloat()).coerceIn(0f, 1f) else 0f
                            _playbackState.value = AudioPlaybackState.Playing(trackId, prog, pos, knownDur)
                        }
                    }
                } catch (_: Exception) {}
                delay(250L)
            }
        }
    }

    private fun stopProgressTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    fun release() {
        stop()
    }

    companion object {
        private const val TAG = "QuranAudioPlayer"
        private const val MAX_SAME_SOURCE_RETRIES = 2

        @Volatile
        private var instance: QuranAudioPlayer? = null

        fun getInstance(context: Context): QuranAudioPlayer {
            return instance ?: synchronized(this) {
                instance ?: QuranAudioPlayer(context.applicationContext).also { instance = it }
            }
        }

        /** Shuts the player down only if it was ever created (never creates one). */
        fun shutdownIfCreated() {
            instance?.shutdown()
        }
    }
}
