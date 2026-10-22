package com.example.audio

import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

/**
 * Session-only streaming cache for Quran audio (YouTube / Instagram style).
 *
 * How it works:
 * 1. ExoPlayer reads the remote MP3 through a [CacheDataSource]. Playback starts as soon as
 *    ~1 second of audio is buffered - the file is NEVER downloaded completely first.
 * 2. Every byte that ExoPlayer downloads is written to this cache at the same time
 *    (one single download - no second parallel download competing for bandwidth).
 * 3. Seeking BACK (or replaying) is served straight from the cache, no network needed.
 * 4. Seeking FORWARD to a part that is not loaded yet makes ExoPlayer fetch just that part
 *    (HTTP range request), store it in the cache and start playing - no crash, no error.
 * 5. The cache is wiped when the app is closed from Recents (service onTaskRemoved /
 *    onDestroy) and also on every fresh app start.
 *
 * Threading: all public functions are synchronized on [lock].
 */
@OptIn(UnstableApi::class)
object QuranAudioStreamingCacheManager {
    private const val TAG = "QuranAudioCacheMgr"
    private const val TEMP_CACHE_DIR_NAME = "quran_audio_temp_cache"

    /** Upper bound for one session. Longest surah (Al-Baqarah) is ~100-120 MB at 128 kbps. */
    private const val MAX_CACHE_BYTES = 512L * 1024L * 1024L

    private val lock = Any()
    private var cache: SimpleCache? = null
    private var databaseProvider: StandaloneDatabaseProvider? = null
    private var startupWipeDone = false

    /** Called from Application.onCreate: removes anything left over from a previous session. */
    fun init(context: Context) {
        synchronized(lock) {
            if (startupWipeDone) return
            startupWipeDone = true
            wipeLocked(context.applicationContext)
        }
    }

    /**
     * Factory for REMOTE urls: network + progressive session cache.
     * Cache errors are ignored (falls back to plain network) so a cache problem can never crash playback.
     */
    fun createStreamingDataSourceFactory(context: Context): DataSource.Factory {
        val app = context.applicationContext
        val httpFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("AlDeenQuranPlayer/1.0")
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(20_000)
        val upstream = DefaultDataSource.Factory(app, httpFactory)
        val sessionCache = synchronized(lock) { getCacheLocked(app) }
        return CacheDataSource.Factory()
            .setCache(sessionCache)
            .setUpstreamDataSourceFactory(upstream)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    /** Factory for permanently downloaded local files: read directly, no cache involved. */
    fun createLocalDataSourceFactory(context: Context): DataSource.Factory {
        return DefaultDataSource.Factory(context.applicationContext)
    }

    /**
     * Completely wipes the temporary audio cache.
     * Any active player is shut down first, because a player must never read from a released cache.
     * Must be called on the main thread (ExoPlayer is released here).
     */
    fun clearTempCache(context: Context) {
        try {
            QuranAudioPlayer.shutdownIfCreated()
        } catch (e: Exception) {
            Log.w(TAG, "Error shutting down player before clearing cache", e)
        }
        synchronized(lock) {
            wipeLocked(context.applicationContext)
        }
    }

    // ---- internals -------------------------------------------------------------------------

    private fun getCacheLocked(app: Context): SimpleCache {
        cache?.let { return it }
        val dir = File(app.cacheDir, TEMP_CACHE_DIR_NAME).apply { if (!exists()) mkdirs() }
        val provider = databaseProvider
            ?: StandaloneDatabaseProvider(app).also { databaseProvider = it }
        return SimpleCache(dir, LeastRecentlyUsedCacheEvictor(MAX_CACHE_BYTES), provider)
            .also { cache = it }
    }

    private fun wipeLocked(app: Context) {
        try {
            cache?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing cache", e)
        }
        cache = null

        val dir = File(app.cacheDir, TEMP_CACHE_DIR_NAME)
        try {
            if (dir.exists()) {
                val provider = databaseProvider
                    ?: StandaloneDatabaseProvider(app).also { databaseProvider = it }
                // Deletes the files AND the cache index rows in the ExoPlayer database
                SimpleCache.delete(dir, provider)
            }
            Log.d(TAG, "Temporary audio cache cleared")
        } catch (e: Exception) {
            Log.w(TAG, "Error clearing temporary audio cache", e)
            try { dir.deleteRecursively() } catch (_: Exception) {}
        }
    }
}
