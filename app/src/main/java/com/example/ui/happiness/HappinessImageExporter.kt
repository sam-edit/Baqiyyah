package com.example.ui.happiness

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.domain.util.ArabicNormalizationUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object HappinessImageExporter {

    private const val TAG = "HappinessImageExporter"

    suspend fun exportAndShare(
        context: Context,
        template: HappinessTemplate,
        textItems: List<HappinessTextItem>
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // High definition export dimension respecting the chosen image aspect ratio
            val targetWidth = 1200
            val targetHeight = (targetWidth / template.aspectRatio).toInt().coerceIn(600, 2400)

            val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // 1. Draw the background image / drawable onto the canvas
            val drawable = ContextCompat.getDrawable(context, template.drawableResId)
            if (drawable != null) {
                drawable.setBounds(0, 0, targetWidth, targetHeight)
                drawable.draw(canvas)
            } else {
                canvas.drawColor(android.graphics.Color.DKGRAY)
            }

            // 2. Load typefaces for authentic typography
            val urduTypeface = try {
                Typeface.createFromAsset(context.assets, "fonts/notonastaliqurdu-regular.ttf")
            } catch (_: Exception) {
                try {
                    Typeface.createFromAsset(context.assets, "notonastaliqurdu-regular.ttf")
                } catch (_: Exception) {
                    Typeface.DEFAULT_BOLD
                }
            }

            val arabicTypeface = try {
                Typeface.createFromAsset(context.assets, "fonts/scheherazadenew-regular.ttf")
            } catch (_: Exception) {
                try {
                    Typeface.createFromAsset(context.assets, "scheherazadenew-regular.ttf")
                } catch (_: Exception) {
                    Typeface.DEFAULT_BOLD
                }
            }

            // 3. Draw each text overlay accurately (only pristine text, NO editing borders or UI handles)
            textItems.forEach { item ->
                if (item.text.isBlank()) return@forEach

                val scaleFactor = targetWidth / 400f
                val maxTextWidth = (targetWidth * 0.88f).toInt()
                val alignment = Layout.Alignment.ALIGN_CENTER

                // Build StaticLayouts for the item (supports dual-script for Quran/Hadith)
                data class BlockLayout(val layout: StaticLayout, val height: Int)
                val blocks = mutableListOf<BlockLayout>()
                var totalBlockHeight = 0
                var maxBlockWidth = 0

                val paragraphs = item.text.split("\n")
                // Group consecutive paragraphs if same style or process per non-blank line
                val cleanParagraphs = if (paragraphs.all { it.isNotBlank() }) paragraphs else {
                    // Normalize excessive blank lines to clean gaps
                    item.text.replace(Regex("\n{3,}"), "\n\n").split("\n\n")
                }

                cleanParagraphs.forEach { para ->
                    if (para.isBlank()) return@forEach

                    val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.argb(
                            (item.color.alpha * 255).toInt(),
                            (item.color.red * 255).toInt(),
                            (item.color.green * 255).toInt(),
                            (item.color.blue * 255).toInt()
                        )

                        // Select authentic font and proportionate size
                        when (item.sourceType) {
                            HappinessSourceType.CUSTOM -> {
                                typeface = if (item.fontFamilyType == HappinessFontFamily.URDU) urduTypeface else arabicTypeface
                                textSize = item.fontSizeSp * scaleFactor
                            }
                            HappinessSourceType.QURAN, HappinessSourceType.HADITH -> {
                                val script = com.example.ui.theme.detectScriptLanguage(para)
                                if (script == com.example.ui.theme.ScriptLanguage.ARABIC) {
                                    typeface = arabicTypeface
                                    textSize = item.fontSizeSp * scaleFactor
                                } else {
                                    typeface = urduTypeface
                                    textSize = (item.fontSizeSp * 0.82f) * scaleFactor
                                }
                            }
                        }

                        if (item.hasShadow) {
                            setShadowLayer(
                                8f * scaleFactor,
                                2f * scaleFactor,
                                3f * scaleFactor,
                                android.graphics.Color.argb(220, 0, 0, 0)
                            )
                        }
                    }

                    val staticLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        StaticLayout.Builder.obtain(para, 0, para.length, textPaint, maxTextWidth)
                            .setAlignment(alignment)
                            .setLineSpacing(0f, 1.2f)
                            .setIncludePad(true)
                            .build()
                    } else {
                        @Suppress("DEPRECATION")
                        StaticLayout(
                            para,
                            textPaint,
                            maxTextWidth,
                            alignment,
                            1.2f,
                            0f,
                            true
                        )
                    }

                    blocks.add(BlockLayout(staticLayout, staticLayout.height))
                    totalBlockHeight += staticLayout.height + (8f * scaleFactor).toInt()
                    if (staticLayout.width > maxBlockWidth) {
                        maxBlockWidth = staticLayout.width
                    }
                }

                if (blocks.isEmpty()) return@forEach

                val centerX = item.xPercent * targetWidth
                val centerY = item.yPercent * targetHeight
                val layoutLeft = (centerX - maxBlockWidth / 2f).coerceIn(10f, targetWidth - maxBlockWidth - 10f)
                val layoutTop = (centerY - totalBlockHeight / 2f).coerceIn(10f, targetHeight - totalBlockHeight - 10f)

                // Optional text background: ONE connected shape that follows the text boundaries
                val bgPaint = item.backgroundColor?.let { c ->
                    Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.argb(
                            (c.alpha * 255).toInt(),
                            (c.red * 255).toInt(),
                            (c.green * 255).toInt(),
                            (c.blue * 255).toInt()
                        )
                        style = Paint.Style.FILL
                    }
                }

                var blockY = layoutTop
                val origins = blocks.map { b ->
                    val bl = (centerX - b.layout.width / 2f).coerceIn(10f, targetWidth - b.layout.width - 10f)
                    val o = floatArrayOf(bl, blockY)
                    blockY += b.height + (8f * scaleFactor).toInt()
                    o
                }

                if (bgPaint != null) {
                    val lines = ArrayList<FloatArray>()
                    blocks.forEachIndexed { bi, b ->
                        for (i in 0 until b.layout.lineCount) {
                            val l = b.layout.getLineLeft(i)
                            val r = b.layout.getLineRight(i)
                            if (r - l <= 0.5f) continue
                            lines.add(
                                floatArrayOf(
                                    origins[bi][0] + l,
                                    origins[bi][0] + r,
                                    origins[bi][1] + b.layout.getLineTop(i),
                                    origins[bi][1] + b.layout.getLineBottom(i)
                                )
                            )
                        }
                    }
                    val corners = buildHighlightCorners(lines, 10f * scaleFactor, 2f * scaleFactor, 12f * scaleFactor)
                    if (corners.size >= 3) {
                        val path = android.graphics.Path()
                        val last = corners.last()
                        path.moveTo(last[4], last[5])
                        for (c in corners) {
                            path.lineTo(c[0], c[1])
                            path.quadTo(c[2], c[3], c[4], c[5])
                        }
                        path.close()
                        canvas.drawPath(path, bgPaint)
                    }
                }

                blocks.forEachIndexed { bi, b ->
                    canvas.save()
                    canvas.translate(origins[bi][0], origins[bi][1])
                    b.layout.draw(canvas)
                    canvas.restore()
                }
            }

            // 4. Save to temporary cache file
            val exportDir = File(context.cacheDir, "happiness_shared")
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }
            val file = File(exportDir, "happiness_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            // 5. Open Android ShareSheet
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            withContext(Dispatchers.Main) {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    putExtra(Intent.EXTRA_TEXT, "Shared via Baqiyyah\nApp Link: https://play.google.com/store/apps/details?id=com.ruleno1.baqiyyahproject")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(shareIntent, "Share Islamic Card (خوشیاں بانٹیں)").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed exporting happiness image", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Error sharing image: ${e.message}", Toast.LENGTH_SHORT).show()
            }
            false
        }
    }
}

/**
 * Builds ONE connected outline around all text lines (like a single highlighted block
 * that follows the text boundaries). Each line is [left, right, top, bottom].
 * Returns rounded corners as [startX, startY, cornerX, cornerY, endX, endY]:
 * draw with lineTo(start) + quadTo(corner, end) for each entry, then close.
 */
internal fun buildHighlightCorners(
    lines: List<FloatArray>,
    padX: Float,
    padY: Float,
    radius: Float
): List<FloatArray> {
    val n = lines.size
    if (n == 0) return emptyList()
    val l = FloatArray(n) { lines[it][0] - padX }
    val r = FloatArray(n) { lines[it][1] + padX }

    // Snap nearly-equal neighbouring edges so tiny steps don't create ugly notches
    val minStep = radius * 2f
    repeat(2) {
        for (i in 1 until n) {
            if (kotlin.math.abs(r[i] - r[i - 1]) < minStep) { val m = kotlin.math.max(r[i], r[i - 1]); r[i] = m; r[i - 1] = m }
            if (kotlin.math.abs(l[i] - l[i - 1]) < minStep) { val m = kotlin.math.min(l[i], l[i - 1]); l[i] = m; l[i - 1] = m }
        }
        for (i in n - 1 downTo 1) {
            if (kotlin.math.abs(r[i] - r[i - 1]) < minStep) { val m = kotlin.math.max(r[i], r[i - 1]); r[i] = m; r[i - 1] = m }
            if (kotlin.math.abs(l[i] - l[i - 1]) < minStep) { val m = kotlin.math.min(l[i], l[i - 1]); l[i] = m; l[i - 1] = m }
        }
    }

    // Horizontal boundaries: each line extends down to the next line's top (no gaps)
    val y = FloatArray(n + 1)
    y[0] = lines[0][2] - padY
    for (i in 1 until n) y[i] = lines[i][2]
    y[n] = lines[n - 1][3] + padY

    // Outline: down the right side, then back up the left side
    var pts: List<FloatArray> = buildList {
        for (i in 0 until n) {
            add(floatArrayOf(r[i], y[i]))
            add(floatArrayOf(r[i], y[i + 1]))
        }
        for (i in n - 1 downTo 0) {
            add(floatArrayOf(l[i], y[i + 1]))
            add(floatArrayOf(l[i], y[i]))
        }
    }

    // Remove duplicate and collinear points
    var changed = true
    while (changed && pts.size > 3) {
        changed = false
        val out = ArrayList<FloatArray>()
        for (k in pts.indices) {
            val prev = pts[(k + pts.size - 1) % pts.size]
            val cur = pts[k]
            val next = pts[(k + 1) % pts.size]
            val dup = kotlin.math.abs(cur[0] - prev[0]) < 0.01f && kotlin.math.abs(cur[1] - prev[1]) < 0.01f
            val cross = (cur[0] - prev[0]) * (next[1] - cur[1]) - (cur[1] - prev[1]) * (next[0] - cur[0])
            if (dup || kotlin.math.abs(cross) < 0.01f) changed = true else out.add(cur)
        }
        pts = out
    }
    val m = pts.size
    if (m < 3) return emptyList()

    return List(m) { k ->
        val prev = pts[(k + m - 1) % m]
        val cur = pts[k]
        val next = pts[(k + 1) % m]
        val lp = kotlin.math.hypot(prev[0] - cur[0], prev[1] - cur[1])
        val ln = kotlin.math.hypot(next[0] - cur[0], next[1] - cur[1])
        val rr = kotlin.math.min(radius, kotlin.math.min(lp, ln) / 2f)
        floatArrayOf(
            cur[0] + (prev[0] - cur[0]) / lp * rr, cur[1] + (prev[1] - cur[1]) / lp * rr,
            cur[0], cur[1],
            cur[0] + (next[0] - cur[0]) / ln * rr, cur[1] + (next[1] - cur[1]) / ln * rr
        )
    }
}
