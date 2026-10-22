package com.example.ui.quran16

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.LruCache
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

internal data class PdfStatus(val pages: Int = 0, val busy: Boolean = false, val progress: Float? = null, val message: String? = null, val checking: Boolean = false)

/** Application scoped; never holds an Activity. Only download() starts network I/O. */
internal class Quran16Store private constructor(context: Context) {
    private val app = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Mutex()
    private val directory = File(app.filesDir, "quran16")
    val file = File(directory, "quran-16-line.pdf")
    private val partial = File(directory, "quran-16-line.part")
    private val prefs = app.getSharedPreferences("quran16_reading", Context.MODE_PRIVATE)
    private val mutableStatus = MutableStateFlow(PdfStatus(checking = true))
    val status = mutableStatus.asStateFlow()
    private val mutableRecent = MutableStateFlow(readPages("recent").take(10))
    val recent = mutableRecent.asStateFlow()
    private val mutableBookmarks = MutableStateFlow(readPages("bookmarks"))
    val bookmarks = mutableBookmarks.asStateFlow()
    private var readers = 0
    private var downloadJob: Job? = null
    @Volatile private var activeCall: okhttp3.Call? = null
    private val client = OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS).callTimeout(10, TimeUnit.MINUTES)
        .followRedirects(true).followSslRedirects(false).build()
    private fun readPages(key: String) = prefs.getString(key, "").orEmpty().split(',').mapNotNull { it.toIntOrNull()?.takeIf { p -> p > 0 } }.distinct()
    val lastPage get() = prefs.getInt("last", 1).coerceAtLeast(1)
    @Synchronized fun visit(page: Int) {
        val pages = (listOf(page) + mutableRecent.value.filter { it != page }).take(10)
        prefs.edit().putInt("last", page).putString("recent", pages.joinToString(",")).apply()
        mutableRecent.value = pages
    }
    @Synchronized fun bookmark(page: Int) {
        val old = mutableBookmarks.value
        val pages = if (page in old) old - page else (old + page).sorted()
        prefs.edit().putString("bookmarks", pages.joinToString(",")).apply()
        mutableBookmarks.value = pages
    }
    @Synchronized fun clearRecent() {
        prefs.edit().remove("recent").apply(); mutableRecent.value = emptyList()
    }
    fun refresh() { scope.launch { lock.withLock {
        if (!mutableStatus.value.busy) mutableStatus.value = try {
            PdfStatus(pages = if (file.isFile) validate(file, false) else 0)
        } catch (_: Exception) { PdfStatus(message = "The local PDF is damaged. Tap Download to replace it.") }
    } } }
    @Synchronized fun download() {
        if (downloadJob?.isActive == true || mutableStatus.value.busy || mutableStatus.value.checking) return
        mutableStatus.value = PdfStatus(busy = true)
        downloadJob = scope.launch {
            try { lock.withLock {
                check(readers == 0) { "Close the reader before replacing the PDF." }
                directory.mkdirs(); partial.delete()
                downloadToPartial()
                mutableStatus.value = PdfStatus(busy = true, message = "Validating PDF…")
                val count = validate(partial, true)
                currentCoroutineContext().ensureActive()
                check(partial.renameTo(file)) { "Unable to save PDF. Check free storage and retry." }
                mutableStatus.value = PdfStatus(pages = count)
            } } catch (e: Exception) {
                partial.delete()
                mutableStatus.value = PdfStatus(message = if (e is CancellationException || !currentCoroutineContext().isActive) "Download cancelled. Tap Download to retry." else (e.message ?: "Download failed. Check your connection and retry."))
            }
        }.also { job ->
            // Cancellation can arrive before the IO dispatcher starts the coroutine.
            job.invokeOnCompletion { cause ->
                if (cause is CancellationException && mutableStatus.value.busy)
                    mutableStatus.value = PdfStatus(message = "Download cancelled. Tap Download to retry.")
            }
        }
    }
    fun cancelDownload() { downloadJob?.cancel(); activeCall?.cancel() }
    private suspend fun downloadToPartial() {
        var url = "https://drive.google.com/uc?export=download&id=1sZT2kaIX_87aanXR4Deg217r-uC1XMDj"
        repeat(3) {
            currentCoroutineContext().ensureActive()
            val call = client.newCall(Request.Builder().url(url).build())
            activeCall = call
            try {
                call.execute().use { response ->
                    if (!response.isSuccessful) throw IOException("Download server returned ${response.code}. Please retry.")
                    val body = response.body ?: throw IOException("Empty download response.")
                    val source = body.source()
                    source.request(5)
                    val header = source.buffer.clone().readUtf8(minOf(5L, source.buffer.size))
                    if (header != "%PDF-") {
                        // Drive large-file confirmation. Never save HTML login/error responses.
                        val html = response.peekBody(262144L).string()
                        val action = Regex("<form[^>]*action=\"([^\"]+)\"", RegexOption.IGNORE_CASE).find(html)?.groupValues?.get(1)
                            ?: throw IOException("Google Drive did not return a PDF. Check public access or retry later.")
                        val target = action.replace("&amp;", "&").toHttpUrl()
                        if (target.scheme != "https" || target.host !in setOf("drive.google.com", "drive.usercontent.google.com")) throw IOException("Unexpected download confirmation host.")
                        val builder = target.newBuilder()
                        Regex("<input[^>]+>", RegexOption.IGNORE_CASE).findAll(html).forEach { tag ->
                            val attrs = Regex("([a-zA-Z]+)=\"([^\"]*)\"").findAll(tag.value).associate { it.groupValues[1] to it.groupValues[2].replace("&amp;", "&") }
                            attrs["name"]?.let { name -> builder.addQueryParameter(name, attrs["value"].orEmpty()) }
                        }
                        url = builder.build().toString()
                    } else {
                        val total = body.contentLength()
                        var done = 0L
                        partial.outputStream().use { output ->
                            val buffer = ByteArray(65536)
                            var lastUpdate = 0L
                            while (true) {
                                currentCoroutineContext().ensureActive()
                                val n = source.read(buffer)
                                if (n == -1) break
                                output.write(buffer, 0, n); done += n
                                if (done > 512L * 1024 * 1024) throw IOException("PDF exceeds the 512 MB download limit.")
                                if (done - lastUpdate >= 262144) {
                                    mutableStatus.value = PdfStatus(busy = true, progress = if (total > 0) (done.toFloat() / total).coerceIn(0f, 1f) else null)
                                    lastUpdate = done
                                }
                            }
                            output.fd.sync()
                        }
                        if (total >= 0 && done != total) throw IOException("Download interrupted. Tap Download to retry.")
                        return
                    }
                }
            } finally { activeCall = null }
        }
        throw IOException("Google Drive confirmation failed. Please retry later.")
    }
    private suspend fun validate(target: File, full: Boolean): Int {
        java.io.RandomAccessFile(target, "r").use { input ->
            require(input.length() >= 8) { "The PDF is incomplete. Download it again." }
            val header = ByteArray(5); input.readFully(header)
            require(String(header, Charsets.US_ASCII) == "%PDF-") { "The file is not a PDF." }
            val tailSize = minOf(input.length(), 4096L).toInt()
            input.seek(input.length() - tailSize)
            val tail = ByteArray(tailSize); input.readFully(tail)
            require(String(tail, Charsets.ISO_8859_1).contains("%%EOF")) { "The PDF is incomplete. Download it again." }
        }
        val descriptor = ParcelFileDescriptor.open(target, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = try { PdfRenderer(descriptor) } catch (e: Exception) { descriptor.close(); throw e }
        renderer.use { pdf ->
            require(pdf.pageCount > 0) { "The downloaded PDF has no pages." }
            val bitmap = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
            try {
                for (index in if (full) 0 until pdf.pageCount else 0..0) {
                    currentCoroutineContext().ensureActive()
                    pdf.openPage(index).use { it.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY) }
                }
            } finally { bitmap.recycle() }
            return pdf.pageCount
        }
    }
    fun delete() { scope.launch { lock.withLock {
        if (readers != 0) { mutableStatus.value = mutableStatus.value.copy(message = "Close the reader before deleting the PDF."); return@withLock }
        try {
            check(!file.exists() || file.delete()) { "Unable to delete the PDF. Please retry." }
            partial.delete(); File(app.cacheDir, "quran16").deleteRecursively()
            mutableStatus.value = PdfStatus()
        } catch (e: Exception) { mutableStatus.value = mutableStatus.value.copy(message = e.message) }
    } } }
    suspend fun open(): PdfSession = withContext(Dispatchers.IO) { lock.withLock {
        check(file.isFile && !mutableStatus.value.busy) { "Download the PDF first from the Quran screen." }
        val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = try { PdfRenderer(descriptor) } catch (e: Exception) { descriptor.close(); throw e }
        readers++
        PdfSession(renderer) { scope.launch { lock.withLock { readers-- } } }
    } }
    companion object {
        @Volatile private var instance: Quran16Store? = null
        fun get(context: Context) = instance ?: synchronized(this) { instance ?: Quran16Store(context).also { instance = it } }
    }
}

/** Serial native calls, bounded cache, and no recycling of bitmaps still displayed by Compose. */
internal class PdfSession(private val renderer: PdfRenderer, private val release: () -> Unit) {
    val count = renderer.pageCount
    private val mutex = Mutex()
    private var closed = false
    private val cache = object : LruCache<String, Bitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.allocationByteCount
    }
    suspend fun render(index: Int, width: Int): Bitmap = withContext(Dispatchers.IO) { mutex.withLock {
        check(!closed) { "Reader closed. Please reopen it." }
        require(index in 0 until count) { "Page outside document." }
        val safeWidth = width.coerceIn(80, 2200)
        val key = "$index:$safeWidth"
        cache.get(key) ?: renderer.openPage(index).use { page ->
            val factor = minOf(safeWidth.toFloat() / page.width, 3200f / page.height, kotlin.math.sqrt(3_200_000f / (page.width.toFloat() * page.height)))
            Bitmap.createBitmap((page.width * factor).toInt().coerceAtLeast(1), (page.height * factor).toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888).also { bitmap ->
                bitmap.eraseColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                cache.put(key, bitmap)
            }
        }
    } }
    suspend fun close() = withContext(NonCancellable + Dispatchers.IO) { mutex.withLock {
        if (!closed) { closed = true; cache.evictAll(); try { renderer.close() } finally { release() } }
    } }
}
