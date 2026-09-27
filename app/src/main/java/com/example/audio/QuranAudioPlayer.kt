package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class AudioPlaybackState {
    object Idle : AudioPlaybackState()
    data class Loading(val trackId: String) : AudioPlaybackState()
    data class Playing(val trackId: String, val progress: Float = 0f) : AudioPlaybackState()
    data class Paused(val trackId: String) : AudioPlaybackState()
    data class Error(val message: String) : AudioPlaybackState()
}

class QuranAudioPlayer(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private var currentTrackId: String? = null

    private val _playbackState = MutableStateFlow<AudioPlaybackState>(AudioPlaybackState.Idle)
    val playbackState: StateFlow<AudioPlaybackState> = _playbackState.asStateFlow()

    fun play(url: String, trackId: String) {
        if (currentTrackId == trackId && mediaPlayer != null) {
            if (mediaPlayer?.isPlaying == true) {
                pause()
                return
            } else {
                mediaPlayer?.start()
                _playbackState.value = AudioPlaybackState.Playing(trackId)
                return
            }
        }

        stop()
        currentTrackId = trackId
        _playbackState.value = AudioPlaybackState.Loading(trackId)

        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(url)
                setOnPreparedListener { mp ->
                    mp.start()
                    _playbackState.value = AudioPlaybackState.Playing(trackId)
                }
                setOnCompletionListener {
                    _playbackState.value = AudioPlaybackState.Idle
                    currentTrackId = null
                }
                setOnErrorListener { _, what, extra ->
                    _playbackState.value = AudioPlaybackState.Error("Playback error ($what, $extra)")
                    currentTrackId = null
                    true
                }
                prepareAsync()
            }
            mediaPlayer = player
        } catch (e: Exception) {
            _playbackState.value = AudioPlaybackState.Error(e.message ?: "Failed to play audio")
            currentTrackId = null
        }
    }

    fun pause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                currentTrackId?.let { id ->
                    _playbackState.value = AudioPlaybackState.Paused(id)
                }
            }
        }
    }

    fun stop() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.reset()
            mediaPlayer?.release()
        } catch (e: Exception) {
            // ignore
        } finally {
            mediaPlayer = null
            currentTrackId = null
            _playbackState.value = AudioPlaybackState.Idle
        }
    }

    fun release() {
        stop()
    }
}
