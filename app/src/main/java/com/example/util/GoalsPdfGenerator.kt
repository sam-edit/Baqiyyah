package com.example.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.domain.model.GoalCategory
import com.example.domain.model.GoalsReportData
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object GoalsPdfGenerator {

    private val fullDateFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy")
    private val shortDateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
    private val timeFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy  hh:mm a")

    fun generateAndOpenPdf(context: Context, report: GoalsReportData) {
        try {
            val pdfDocument = PdfDocument()
            val pageWidth = 595
            val pageHeight = 842

            // Colors
            val emeraldPrimary = Color.rgb(6, 95, 70) // #065F46
            val emeraldDark = Color.rgb(4, 120, 87) // #047857
            val emeraldLight = Color.rgb(240, 253, 244) // #F0FDF4
            val emeraldBorder = Color.rgb(167, 243, 208) // #A7F3D0
            val successGreen = Color.rgb(16, 185, 129) // #10B981
            val successBg = Color.rgb(220, 252, 231) // #DCFCE7
            val successBorder = Color.rgb(134, 239, 172) // #86EFAC
            val failBg = Color.rgb(243, 244, 246) // #F3F4F6
            val failBorder = Color.rgb(229, 231, 235) // #E5E7EB
            val failText = Color.rgb(156, 163, 175) // #9CA3AF
            val textDark = Color.rgb(17, 24, 39) // #111827
            val textSecondary = Color.rgb(75, 85, 99) // #4B5563
            val goldAccent = Color.rgb(217, 119, 6) // #D97706
            val goldBg = Color.rgb(254, 243, 199) // #FEF3C7

            val urduTypeface: Typeface = try {
                Typeface.createFromAsset(context.assets, "fonts/notonastaliqurdu-regular.ttf")
            } catch (_: Exception) {
                Typeface.DEFAULT
            }

            val arabicTypeface: Typeface = try {
                Typeface.createFromAsset(context.assets, "fonts/scheherazadenew-regular.ttf")
            } catch (_: Exception) {
                Typeface.DEFAULT
            }

            val defaultBoldTypeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val defaultNormalTypeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

            val bgPaint = Paint().apply { isAntiAlias = true }
            val strokePaint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
            }
            val textPaint = Paint().apply {
                isAntiAlias = true
                typeface = defaultNormalTypeface
            }

            var pageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas

            val margin = 36f
            val contentWidth = pageWidth - (margin * 2)

            fun drawFooter(c: Canvas, pNum: Int) {
                textPaint.color = textSecondary
                textPaint.textSize = 8.5f
                textPaint.typeface = defaultNormalTypeface
                c.drawText("Baqiyyah Spiritual Companion  •  Daily Worship Report  •  Page $pNum", margin, pageHeight - 22f, textPaint)
            }

            // Page 1 Header Banner
            bgPaint.color = emeraldPrimary
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), 85f, bgPaint)

            // Header Title
            textPaint.color = Color.WHITE
            textPaint.textSize = 18f
            textPaint.typeface = urduTypeface
            canvas.drawText("BAQIYYAH  |  بقیہ", margin, 34f, textPaint)

            textPaint.textSize = 11f
            textPaint.typeface = urduTypeface
            canvas.drawText("Daily Worship & Goals Progress Report (رپورٹ برائے عبادات و اعمال)", margin, 52f, textPaint)

            textPaint.textSize = 8.5f
            textPaint.color = Color.rgb(209, 250, 229)
            val genDateStr = LocalDateTime.now().format(timeFormatter)
            canvas.drawText("Generated: $genDateStr  •  Period: ${report.requestedDays} Days (${report.startDate.format(shortDateFormatter)} - ${report.endDate.format(shortDateFormatter)})", margin, 70f, textPaint)

            var currentY = 100f

            // Executive Summary Card
            val summaryCardRect = RectF(margin, currentY, margin + contentWidth, currentY + 84f)
            bgPaint.color = emeraldLight
            canvas.drawRoundRect(summaryCardRect, 8f, 8f, bgPaint)

            strokePaint.color = emeraldBorder
            strokePaint.strokeWidth = 1f
            canvas.drawRoundRect(summaryCardRect, 8f, 8f, strokePaint)

            textPaint.color = emeraldPrimary
            textPaint.textSize = 11f
            textPaint.typeface = urduTypeface
            canvas.drawText("EXECUTIVE SUMMARY (مجموعی کارکردگی کا خلاصہ)", margin + 14f, currentY + 20f, textPaint)

            // 4 Key Metrics
            val colWidth = contentWidth / 4f
            val metrics = listOf(
                Pair("Total Progress", "${report.overallPercentage}%"),
                Pair("Fard Salat", "${report.totalFardCompleted} / ${report.totalFardPossible}"),
                Pair("Daily Deeds", "${report.totalDeedsCompleted} / ${report.totalDeedsPossible}"),
                Pair("Custom Goals", "${report.totalCustomCompleted} / ${report.totalCustomPossible}")
            )

            metrics.forEachIndexed { index, (label, value) ->
                val colX = margin + 14f + (index * colWidth)
                textPaint.color = textSecondary
                textPaint.textSize = 8.5f
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                canvas.drawText(label, colX, currentY + 40f, textPaint)

                textPaint.color = emeraldPrimary
                textPaint.textSize = 15f
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText(value, colX, currentY + 60f, textPaint)

                textPaint.color = textDark
                textPaint.textSize = 7.5f
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                val subText = when (index) {
                    0 -> "Total completion"
                    1 -> "5 Prayers tracked"
                    2 -> "Ibadah deeds"
                    else -> "Personal targets"
                }
                canvas.drawText(subText, colX, currentY + 74f, textPaint)
            }

            currentY += 100f

            // Section Header
            textPaint.color = emeraldPrimary
            textPaint.textSize = 11.5f
            textPaint.typeface = urduTypeface
            canvas.drawText("DAILY WORSHIP BREAKDOWN (روزانہ کی بنیاد پر تفصیل)", margin, currentY + 12f, textPaint)
            currentY += 22f

            val today = LocalDate.now()

            // Draw Each Day's Card
            report.dailySummaries.forEach { daySummary ->
                val hasCustomGoals = daySummary.items.any { it.category == GoalCategory.CUSTOM }
                val cardHeight = if (hasCustomGoals) 146f else 126f

                // Check page break
                if (currentY + cardHeight > pageHeight - 35f) {
                    drawFooter(canvas, pageNumber)
                    pdfDocument.finishPage(page)

                    pageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas

                    // Mini page header on subsequent pages
                    bgPaint.color = emeraldPrimary
                    canvas.drawRect(0f, 0f, pageWidth.toFloat(), 34f, bgPaint)
                    textPaint.color = Color.WHITE
                    textPaint.textSize = 10f
                    textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas.drawText("BAQIYYAH  |  Daily Worship Report (Continued)  •  ${report.requestedDays} Days Period", margin, 21f, textPaint)

                    currentY = 46f
                }

                val cardRect = RectF(margin, currentY, margin + contentWidth, currentY + cardHeight)

                // Main card background
                bgPaint.color = Color.WHITE
                canvas.drawRoundRect(cardRect, 8f, 8f, bgPaint)

                // Main card stroke
                strokePaint.color = if (daySummary.date == today) emeraldPrimary else Color.rgb(229, 231, 235)
                strokePaint.strokeWidth = if (daySummary.date == today) 1.5f else 1f
                canvas.drawRoundRect(cardRect, 8f, 8f, strokePaint)

                // Top Box: Date & Day header inside card
                val headerRect = RectF(margin, currentY, margin + contentWidth, currentY + 28f)
                val headerPath = Path().apply {
                    addRoundRect(headerRect, floatArrayOf(8f, 8f, 8f, 8f, 0f, 0f, 0f, 0f), Path.Direction.CW)
                }
                bgPaint.color = when {
                    daySummary.date == today -> emeraldPrimary
                    daySummary.completionPercentage > 0 -> Color.rgb(236, 253, 245)
                    else -> Color.rgb(249, 250, 251)
                }
                canvas.drawPath(headerPath, bgPaint)

                // Border between header and content
                strokePaint.color = Color.rgb(229, 231, 235)
                strokePaint.strokeWidth = 0.8f
                canvas.drawLine(margin, currentY + 28f, margin + contentWidth, currentY + 28f, strokePaint)

                // Header Text: Date and Day
                textPaint.color = if (daySummary.date == today) Color.WHITE else textDark
                textPaint.textSize = 10.5f
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                val dateLabel = daySummary.date.format(fullDateFormatter)
                canvas.drawText(dateLabel, margin + 12f, currentY + 18f, textPaint)

                // If today, draw a "TODAY" pill
                if (daySummary.date == today) {
                    val todayPillRect = RectF(margin + textPaint.measureText(dateLabel) + 18f, currentY + 6f, margin + textPaint.measureText(dateLabel) + 68f, currentY + 22f)
                    bgPaint.color = goldAccent
                    canvas.drawRoundRect(todayPillRect, 4f, 4f, bgPaint)
                    textPaint.color = Color.WHITE
                    textPaint.textSize = 7.5f
                    textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas.drawText("TODAY (آج)", todayPillRect.left + 6f, currentY + 17f, textPaint)
                }

                // Completion Badge on Right of Header
                val pctStr = "${daySummary.completionPercentage}% (${daySummary.totalCompleted}/${daySummary.totalGoals} Goals)"
                textPaint.color = if (daySummary.date == today) Color.WHITE else if (daySummary.completionPercentage > 0) emeraldDark else textSecondary
                textPaint.textSize = 9.5f
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                val pctWidth = textPaint.measureText(pctStr)
                canvas.drawText(pctStr, margin + contentWidth - pctWidth - 12f, currentY + 18f, textPaint)

                // Interactive Form Progress Bar below Header
                val barTop = currentY + 35f
                val barHeight = 6f
                val barWidth = contentWidth - 24f
                val barRect = RectF(margin + 12f, barTop, margin + 12f + barWidth, barTop + barHeight)

                // Background track
                bgPaint.color = Color.rgb(229, 231, 235)
                canvas.drawRoundRect(barRect, 3f, 3f, bgPaint)

                // Fill progress
                val filledWidth = (barWidth * (daySummary.completionPercentage / 100f)).coerceIn(0f, barWidth)
                if (filledWidth > 0) {
                    val fillRect = RectF(margin + 12f, barTop, margin + 12f + filledWidth, barTop + barHeight)
                    bgPaint.color = when {
                        daySummary.completionPercentage >= 80 -> successGreen
                        daySummary.completionPercentage >= 50 -> goldAccent
                        else -> Color.rgb(59, 130, 246)
                    }
                    canvas.drawRoundRect(fillRect, 3f, 3f, bgPaint)
                }

                // Section 1: Fard Salat Breakdown
                val salatY = currentY + 54f
                textPaint.color = emeraldPrimary
                textPaint.textSize = 8.5f
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("FARD SALAT (فرض نمازیں):", margin + 12f, salatY, textPaint)

                // 5 Fard Prayers Badges
                val fardItems = daySummary.items.filter { it.category == GoalCategory.FARD_SALAT }
                val prayerNames = listOf(
                    "fajr" to "Fajr",
                    "dhuhr" to "Dhuhr",
                    "asr" to "Asr",
                    "maghrib" to "Maghrib",
                    "isha" to "Isha"
                )

                val badgeWidth = (contentWidth - 24f - 16f) / 5f
                prayerNames.forEachIndexed { idx, (id, label) ->
                    val isDone = fardItems.firstOrNull { it.goalId == id }?.isCompleted == true
                    val bLeft = margin + 12f + (idx * (badgeWidth + 4f))
                    val bTop = salatY + 4f
                    val bRect = RectF(bLeft, bTop, bLeft + badgeWidth, bTop + 18f)

                    bgPaint.color = if (isDone) successBg else failBg
                    canvas.drawRoundRect(bRect, 4f, 4f, bgPaint)

                    strokePaint.color = if (isDone) successBorder else failBorder
                    strokePaint.strokeWidth = 0.8f
                    canvas.drawRoundRect(bRect, 4f, 4f, strokePaint)

                    textPaint.color = if (isDone) emeraldDark else failText
                    textPaint.textSize = 8f
                    textPaint.typeface = Typeface.create(Typeface.DEFAULT, if (isDone) Typeface.BOLD else Typeface.NORMAL)
                    val iconMark = if (isDone) "[✓] " else "[  ] "
                    canvas.drawText(iconMark + label, bLeft + 6f, bTop + 13f, textPaint)
                }

                // Section 2: Daily Deeds Breakdown
                val deedsY = salatY + 30f
                textPaint.color = emeraldPrimary
                textPaint.textSize = 8.5f
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("DAILY DEEDS & IBADAH (روزمرہ اعمال):", margin + 12f, deedsY, textPaint)

                val deedItems = daySummary.items.filter { it.category == GoalCategory.DAILY_IBADAH }
                val deedNames = listOf(
                    "quran" to "Quran",
                    "good_deed" to "Good Deed",
                    "azkar" to "Azkar",
                    "dua" to "Dua",
                    "durood" to "Durood",
                    "istighfar" to "Istighfar",
                    "charity" to "Charity/Sadqah"
                )

                // Row 1: 4 deeds
                val deedWidth4 = (contentWidth - 24f - 12f) / 4f
                deedNames.take(4).forEachIndexed { idx, (id, label) ->
                    val isDone = deedItems.firstOrNull { it.goalId == id }?.isCompleted == true
                    val bLeft = margin + 12f + (idx * (deedWidth4 + 4f))
                    val bTop = deedsY + 4f
                    val bRect = RectF(bLeft, bTop, bLeft + deedWidth4, bTop + 16f)

                    bgPaint.color = if (isDone) successBg else failBg
                    canvas.drawRoundRect(bRect, 4f, 4f, bgPaint)

                    strokePaint.color = if (isDone) successBorder else failBorder
                    strokePaint.strokeWidth = 0.8f
                    canvas.drawRoundRect(bRect, 4f, 4f, strokePaint)

                    textPaint.color = if (isDone) emeraldDark else failText
                    textPaint.textSize = 7.5f
                    textPaint.typeface = Typeface.create(Typeface.DEFAULT, if (isDone) Typeface.BOLD else Typeface.NORMAL)
                    val iconMark = if (isDone) "[✓] " else "[  ] "
                    canvas.drawText(iconMark + label, bLeft + 5f, bTop + 11.5f, textPaint)
                }

                // Row 2: remaining 3 deeds
                val deedWidth3 = (contentWidth - 24f - 8f) / 3f
                deedNames.drop(4).forEachIndexed { idx, (id, label) ->
                    val isDone = deedItems.firstOrNull { it.goalId == id }?.isCompleted == true
                    val bLeft = margin + 12f + (idx * (deedWidth3 + 4f))
                    val bTop = deedsY + 23f
                    val bRect = RectF(bLeft, bTop, bLeft + deedWidth3, bTop + 16f)

                    bgPaint.color = if (isDone) successBg else failBg
                    canvas.drawRoundRect(bRect, 4f, 4f, bgPaint)

                    strokePaint.color = if (isDone) successBorder else failBorder
                    strokePaint.strokeWidth = 0.8f
                    canvas.drawRoundRect(bRect, 4f, 4f, strokePaint)

                    textPaint.color = if (isDone) emeraldDark else failText
                    textPaint.textSize = 7.5f
                    textPaint.typeface = Typeface.create(Typeface.DEFAULT, if (isDone) Typeface.BOLD else Typeface.NORMAL)
                    val iconMark = if (isDone) "[✓] " else "[  ] "
                    canvas.drawText(iconMark + label, bLeft + 6f, bTop + 11.5f, textPaint)
                }

                // Section 3: Custom Goals (if any)
                if (hasCustomGoals) {
                    val customY = deedsY + 45f
                    textPaint.color = emeraldPrimary
                    textPaint.textSize = 8.5f
                    textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas.drawText("CUSTOM GOALS (ذاتی اہداف):", margin + 12f, customY, textPaint)

                    val customItems = daySummary.items.filter { it.category == GoalCategory.CUSTOM }
                    val customBadgeWidth = (contentWidth - 24f - (4f * (customItems.size - 1).coerceAtLeast(0))) / customItems.size.coerceAtLeast(1)

                    customItems.take(4).forEachIndexed { idx, item ->
                        val bLeft = margin + 12f + (idx * (customBadgeWidth + 4f))
                        val bTop = customY + 4f
                        val bRect = RectF(bLeft, bTop, bLeft + customBadgeWidth, bTop + 16f)

                        bgPaint.color = if (item.isCompleted) successBg else failBg
                        canvas.drawRoundRect(bRect, 4f, 4f, bgPaint)

                        strokePaint.color = if (item.isCompleted) successBorder else failBorder
                        strokePaint.strokeWidth = 0.8f
                        canvas.drawRoundRect(bRect, 4f, 4f, strokePaint)

                        textPaint.color = if (item.isCompleted) emeraldDark else failText
                        textPaint.textSize = 7.5f
                        textPaint.typeface = Typeface.create(Typeface.DEFAULT, if (item.isCompleted) Typeface.BOLD else Typeface.NORMAL)
                        val iconMark = if (item.isCompleted) "[✓] " else "[  ] "
                        val cleanTitle = if (item.titleEnglish.length > 18) item.titleEnglish.take(16) + ".." else item.titleEnglish
                        canvas.drawText(iconMark + cleanTitle, bLeft + 5f, bTop + 11.5f, textPaint)
                    }
                }

                currentY += cardHeight + 10f
            }

            // Draw footer on final page
            drawFooter(canvas, pageNumber)
            pdfDocument.finishPage(page)

            // Save PDF to file
            val reportsDir = File(context.cacheDir, "reports")
            if (!reportsDir.exists()) reportsDir.mkdirs()

            val fileName = "Baqiyyah_Goals_Report_${report.requestedDays}Days_${System.currentTimeMillis()}.pdf"
            val file = File(reportsDir, fileName)
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            pdfDocument.close()
            outputStream.flush()
            outputStream.close()

            // Open with chooser intent
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)

            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val resInfoList = context.packageManager.queryIntentActivities(viewIntent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
            for (resolveInfo in resInfoList) {
                val pkg = resolveInfo.activityInfo.packageName
                context.grantUriPermission(pkg, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooserIntent = Intent.createChooser(viewIntent, "Open Goals Report PDF (پی ڈی ایف رپورٹ کھولیں)").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(chooserIntent)
            Toast.makeText(context, "Report generated! Opening PDF...", Toast.LENGTH_SHORT).show()

        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "No PDF viewer app found on device.", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error generating report: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}
