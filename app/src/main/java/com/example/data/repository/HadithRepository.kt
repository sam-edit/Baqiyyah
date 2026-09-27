package com.example.data.repository

import android.content.Context
import com.example.data.hadith.HadithDownloadManager
import com.example.domain.model.Hadith
import com.example.domain.model.HadithBook
import com.example.domain.model.HadithChapter
import com.example.domain.util.ArabicNormalizationUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.json.JSONObject
import java.io.File
import java.util.concurrent.ConcurrentHashMap

data class HadithSearchBatch(
    val results: List<Hadith>,
    val progress: Int,
    val isComplete: Boolean = false
)

class HadithRepository(
    private val context: Context? = null,
    private val downloadManager: HadithDownloadManager? = null
) {
    private val parsedBooksCache = ConcurrentHashMap<String, ParsedHadithBookData>()
    // Cache for language translations: Map<"${bookSlug}_${langCode}", Map<hadithNumber, translationText>>
    private val translationsCache = ConcurrentHashMap<String, Map<String, String>>()

    data class ParsedHadithBookData(
        val chapters: List<HadithChapter>,
        val hadithsByChapter: Map<Int, List<Hadith>>
    )

    fun getAllBooks(): List<HadithBook> = AuthoritativeContentProvider.hadithBooks

    companion object {
        fun normalizeHadithNumber(raw: String): String {
            val trimmed = raw.trim()
            val asDouble = trimmed.toDoubleOrNull()
            return if (asDouble != null && asDouble == asDouble.toLong().toDouble()) {
                asDouble.toLong().toString()
            } else {
                trimmed
            }
        }

        fun cleanTranslationText(
            rawText: String,
            lang: String = "urd",
            bookSlug: String = "",
            hadithNumber: String = ""
        ): String {
            val normNum = normalizeHadithNumber(hadithNumber)

            // If empty, check UrduHadithSupplement for authentic Urdu translation
            val effectiveText = if (rawText.isBlank() && (lang == "urd" || lang == "ur")) {
                UrduHadithSupplement.getTranslation(bookSlug, normNum)
                    ?: UrduHadithSupplement.getTranslation(bookSlug, hadithNumber)
                    ?: ""
            } else {
                rawText
            }

            if (effectiveText.isBlank()) return ""
            var cleaned = effectiveText.trim()

            // Remove leading stray punctuation / artifacts from OCR or scraped JSON
            while (cleaned.isNotEmpty() && (cleaned.startsWith("۔") || cleaned.startsWith(".") ||
                    cleaned.startsWith("،") || cleaned.startsWith("-") || cleaned.startsWith(":") ||
                    cleaned.startsWith("“") || cleaned.startsWith("”") || cleaned.startsWith("\"") ||
                    cleaned.startsWith("'") || cleaned.startsWith("’") || cleaned.startsWith("‘") ||
                    cleaned.startsWith(" "))) {
                cleaned = cleaned.drop(1).trim()
            }

            if (lang == "urd" || lang == "ur") {
                // Fix known truncation in Urdu translation from API:
                // Bukhari #1 in Urdu API starts with "کو حمیدی نے" instead of "ہم کو حمیدی نے"
                if (cleaned.startsWith("کو حمیدی نے")) {
                    cleaned = "ہم " + cleaned
                } else if (cleaned.startsWith("کو ") && (bookSlug.contains("bukhari") || normNum == "1")) {
                    cleaned = "ہم " + cleaned
                }

                // Muwatta Malik Hadith 2 Urdu fallback if empty or missing in API
                if (cleaned.isBlank() && (bookSlug.contains("malik") || bookSlug.contains("muwatta")) && normNum == "2") {
                    cleaned = "ام المؤمنین حضرت عائشہ رضی اللہ عنہا سے روایت ہے کہ رسول اللہ صلی اللہ علیہ وسلم عصر کی نماز پڑھتے تھے اور دھوپ حجرے کے اندر ہوتی تھی دیواروں پر چڑھنے سے پہلے۔"
                }

                if (cleaned.isBlank()) {
                    val supplement = UrduHadithSupplement.getTranslation(bookSlug, normNum)
                        ?: UrduHadithSupplement.getTranslation(bookSlug, hadithNumber)
                    if (!supplement.isNullOrBlank()) {
                        cleaned = supplement
                    }
                }

                // Remove dangling opening bracket if OCR artifact
                if (cleaned.startsWith("]")) cleaned = cleaned.drop(1).trim()
                if (cleaned.startsWith("[")) {
                    val closeIdx = cleaned.indexOf(']')
                    if (closeIdx in 1..20 && !cleaned.substring(0, closeIdx).contains(" ")) {
                        cleaned = cleaned.substring(closeIdx + 1).trim()
                    }
                }
            }

            return cleaned
        }
    }

    private fun hasArabicText(str: String): Boolean {
        return str.any { it in '\u0600'..'\u06FF' || it in '\u0750'..'\u077F' || it in '\u08A0'..'\u08FF' }
    }

    private fun getParsedBookData(bookSlug: String): ParsedHadithBookData? {
        parsedBooksCache[bookSlug]?.let { return it }

        val hadithsDir = downloadManager?.hadithDir
            ?: downloadManager?.getBookFile(bookSlug)?.parentFile
            ?: context?.let { File(it.filesDir, "hadiths") }

        if (hadithsDir != null && !hadithsDir.exists()) {
            hadithsDir.mkdirs()
        }

        if (hadithsDir != null && (bookSlug in listOf("arbaeen-nawawi", "hadith-qudsi", "nawawi", "qudsi"))) {
            val assetName = if (bookSlug.contains("nawawi")) "nawawi" else "qudsi"
            val targetSlug = if (bookSlug.contains("nawawi")) "arbaeen-nawawi" else "hadith-qudsi"
            val targetAra = File(hadithsDir, "${targetSlug}_ara.json")
            if (!targetAra.exists() || targetAra.length() < 1000) {
                try {
                    context?.assets?.open("hadiths/ara-$assetName.json.gz")?.use { input ->
                        java.util.zip.GZIPInputStream(input).use { gz ->
                            targetAra.outputStream().use { out -> gz.copyTo(out) }
                        }
                    }
                } catch (_: Exception) {}
            }
            val targetUrd = File(hadithsDir, "${targetSlug}_urd.json")
            val targetUrdu = File(hadithsDir, "${targetSlug}_urdu.json")
            if (!targetUrd.exists() || targetUrd.length() < 1000) {
                try {
                    context?.assets?.open("hadiths/urd-$assetName.json.gz")?.use { input ->
                        java.util.zip.GZIPInputStream(input).use { gz ->
                            targetUrd.outputStream().use { out -> gz.copyTo(out) }
                        }
                    }
                    if (targetUrd.exists()) {
                        targetUrd.copyTo(targetUrdu, overwrite = true)
                    }
                } catch (_: Exception) {}
            } else if (!targetUrdu.exists()) {
                try { targetUrd.copyTo(targetUrdu, overwrite = true) } catch (_: Exception) {}
            }
        }

        if (hadithsDir == null || !hadithsDir.exists()) return null

        val araFile = File(hadithsDir, "${bookSlug}_ara.json").takeIf { it.exists() && it.length() > 500 }
        val urdFile = File(hadithsDir, "${bookSlug}_urdu.json").takeIf { it.exists() && it.length() > 500 }
            ?: File(hadithsDir, "${bookSlug}_urd.json").takeIf { it.exists() && it.length() > 500 }
        val engFile = File(hadithsDir, "${bookSlug}_english.json").takeIf { it.exists() && it.length() > 500 }
            ?: File(hadithsDir, "${bookSlug}_eng.json").takeIf { it.exists() && it.length() > 500 }

        val primaryFile = araFile ?: urdFile ?: engFile ?: return null

        try {
            val root = JSONObject(primaryFile.readText(Charsets.UTF_8))
            val metadata = root.optJSONObject("metadata")
            val sections = metadata?.optJSONObject("sections")
            val sectionDetails = metadata?.optJSONObject("section_details")
            val book = getAllBooks().firstOrNull { it.slug == bookSlug }
            val bookName = book?.englishName ?: "Hadith Book"

            // Also check urdu/english sections for bilingual titles
            var urdSections: JSONObject? = null
            if (urdFile != null && urdFile.exists()) {
                try {
                    val uRoot = JSONObject(urdFile.readText(Charsets.UTF_8))
                    urdSections = uRoot.optJSONObject("metadata")?.optJSONObject("sections")
                } catch (_: Exception) {}
            }
            var engSections: JSONObject? = null
            if (engFile != null && engFile.exists()) {
                try {
                    val eRoot = JSONObject(engFile.readText(Charsets.UTF_8))
                    engSections = eRoot.optJSONObject("metadata")?.optJSONObject("sections")
                } catch (_: Exception) {}
            }

            val isIntroBook = bookSlug.contains("muslim") || bookSlug.contains("ibnmajah") || bookSlug.contains("ibn-majah")
            val fallbackChapters = AuthoritativeContentProvider.getChaptersForBook(bookSlug)
            val chaptersList = mutableListOf<HadithChapter>()
            if (sections != null) {
                val keys = sections.keys()
                val sortedKeys = mutableListOf<Int>()
                while (keys.hasNext()) {
                    val k = keys.next()
                    k.toIntOrNull()?.let { if (it >= 0) sortedKeys.add(it) }
                }
                sortedKeys.sort()
                if (sortedKeys.contains(0) && sortedKeys.size > 1 && sections.optString("0", "").isBlank() && !isIntroBook) {
                    sortedKeys.remove(0)
                }
                if (isIntroBook && !sortedKeys.contains(0)) {
                    sortedKeys.add(0, 0)
                }

                for (chNum in sortedKeys) {
                    val rawTitle = sections.optString(chNum.toString(), "")
                    val fallbackCh = fallbackChapters.firstOrNull { it.chapterNumber == chNum }
                    val uTitle = fallbackCh?.urduTitle?.takeIf { it.isNotBlank() }
                        ?: urdSections?.optString(chNum.toString(), "")?.takeIf { it.isNotBlank() }
                        ?: if (chNum == 0 && isIntroBook) (if (bookSlug.contains("muslim")) "مقدمہ صحیح مسلم" else "مقدمہ سنن ابن ماجہ")
                        else (if (primaryFile != araFile && !hasArabicText(rawTitle)) rawTitle else "باب $chNum")
                    val eTitle = fallbackCh?.englishTitle?.takeIf { it.isNotBlank() }
                        ?: engSections?.optString(chNum.toString(), "")?.takeIf { it.isNotBlank() }
                        ?: if (chNum == 0 && isIntroBook) "The Book of the Introduction"
                        else (if (primaryFile == engFile || (!hasArabicText(rawTitle) && rawTitle.isNotBlank())) rawTitle else "Chapter $chNum")
                    val aTitle = fallbackCh?.arabicTitle?.takeIf { it.isNotBlank() && hasArabicText(it) }
                        ?: if (chNum == 0 && isIntroBook) "كتاب المقدمة"
                        else if (primaryFile == araFile && rawTitle.isNotBlank() && hasArabicText(rawTitle)) {
                            if (rawTitle.startsWith("كتاب") || rawTitle.startsWith("المقدمة") || rawTitle.startsWith("الأحاديث")) rawTitle else "كتاب $rawTitle"
                        } else "كتاب $chNum"

                    chaptersList.add(
                        HadithChapter(
                            id = if (chNum > 0) chNum else 1000,
                            bookSlug = bookSlug,
                            chapterNumber = chNum,
                            arabicTitle = aTitle,
                            urduTitle = uTitle,
                            englishTitle = eTitle
                        )
                    )
                }
            }

            // Load translations if Urdu file also exists
            val urdMap = mutableMapOf<String, String>()
            var urdArrayLength = -1
            if (urdFile != null && urdFile.exists()) {
                try {
                    val urdRoot = JSONObject(urdFile.readText(Charsets.UTF_8))
                    val uArray = urdRoot.optJSONArray("hadiths")
                    if (uArray != null) {
                        urdArrayLength = uArray.length()
                        for (i in 0 until uArray.length()) {
                            val uObj = uArray.getJSONObject(i)
                            val num = uObj.optString("hadithnumber").ifBlank {
                                uObj.optString("hadithNumber").ifBlank {
                                    ""
                                }
                            }.trim()
                            val rawTxt = uObj.optString("text", "")
                            val norm = normalizeHadithNumber(num)
                            val supplement = UrduHadithSupplement.getTranslation(bookSlug, norm)
                                ?: UrduHadithSupplement.getTranslation(bookSlug, num)
                            val txt = cleanTranslationText(rawTxt.ifBlank { supplement ?: "" }, "urd", bookSlug, norm)
                            if (txt.isNotBlank()) {
                                urdMap["idx_$i"] = txt
                                if (num.isNotBlank()) urdMap[num] = txt
                                if (norm.isNotBlank()) urdMap[norm] = txt
                                val ref = uObj.optJSONObject("reference")
                                if (ref != null) {
                                    val b = ref.optInt("book", -1)
                                    val h = ref.optInt("hadith", -1)
                                    if (b >= 0 && h > 0) {
                                        urdMap["ref_${b}_${h}"] = txt
                                    }
                                }
                                if (!uObj.isNull("arabicnumber")) {
                                    val aNum = normalizeHadithNumber(uObj.optString("arabicnumber"))
                                    if (aNum.isNotBlank()) {
                                        urdMap["ara_$aNum"] = txt
                                    }
                                }
                            }
                        }
                    }
                } catch (_: Exception) {}
            }

            // Load translations if English file exists
            val engMap = mutableMapOf<String, String>()
            if (engFile != null && engFile.exists()) {
                try {
                    val engRoot = JSONObject(engFile.readText(Charsets.UTF_8))
                    val eArray = engRoot.optJSONArray("hadiths")
                    if (eArray != null) {
                        for (i in 0 until eArray.length()) {
                            val eObj = eArray.getJSONObject(i)
                            val num = eObj.optString("hadithnumber").ifBlank {
                                eObj.optString("hadithNumber").ifBlank {
                                    ""
                                }
                            }.trim()
                            val rawTxt = eObj.optString("text", "")
                            val norm = normalizeHadithNumber(num)
                            val txt = cleanTranslationText(rawTxt, "eng", bookSlug, norm)
                            if (txt.isNotBlank()) {
                                engMap["idx_$i"] = txt
                                if (num.isNotBlank()) engMap[num] = txt
                                if (norm.isNotBlank()) engMap[norm] = txt
                                val ref = eObj.optJSONObject("reference")
                                if (ref != null) {
                                    val b = ref.optInt("book", -1)
                                    val h = ref.optInt("hadith", -1)
                                    if (b >= 0 && h > 0) {
                                        engMap["ref_${b}_${h}"] = txt
                                    }
                                }
                                if (!eObj.isNull("arabicnumber")) {
                                    val aNum = normalizeHadithNumber(eObj.optString("arabicnumber"))
                                    if (aNum.isNotBlank()) {
                                        engMap["ara_$aNum"] = txt
                                    }
                                }
                            }
                        }
                    }
                } catch (_: Exception) {}
            }

            val hadithsArray = root.optJSONArray("hadiths")
            val hadithMap = mutableMapOf<Int, MutableList<Hadith>>()

            if (hadithsArray != null) {
                var lastAssignedChapter = if (isIntroBook) 0 else 1

                for (i in 0 until hadithsArray.length()) {
                    val hObj = hadithsArray.getJSONObject(i)
                    val rawNumber = hObj.optString("hadithnumber", (i + 1).toString())
                    val hadithNumber = normalizeHadithNumber(rawNumber)
                    val text = hObj.optString("text", "")
                    val refObj = hObj.optJSONObject("reference")

                    val refBook = if (refObj != null && refObj.has("book")) refObj.optInt("book", -1) else -1
                    val refHadith = if (refObj != null && refObj.has("hadith")) refObj.optInt("hadith", -1) else -1
                    val refChapter = if (refObj != null && refObj.has("chapter")) refObj.optInt("chapter", -1) else -1
                    val araNumber = if (!hObj.isNull("arabicnumber")) normalizeHadithNumber(hObj.optString("arabicnumber")) else ""

                    var chapterNum = -1
                    if (refBook > 0) {
                        chapterNum = refBook
                    } else if (refChapter > 0) {
                        chapterNum = refChapter
                    } else if (refBook == 0 || refChapter == 0) {
                        if (isIntroBook) {
                            chapterNum = 0
                        }
                    }

                    if (chapterNum < 0) {
                        // Attempt lookup from section_details range
                        val numInt = hadithNumber.toIntOrNull() ?: (i + 1)
                        if (sectionDetails != null) {
                            val sKeys = sectionDetails.keys()
                            while (sKeys.hasNext()) {
                                val sKey = sKeys.next()
                                val sObj = sectionDetails.optJSONObject(sKey)
                                if (sObj != null) {
                                    val first = sObj.optInt("hadithnumber_first", -1)
                                    val last = sObj.optInt("hadithnumber_last", -1)
                                    if (first in 1..numInt && numInt <= last) {
                                        chapterNum = sKey.toIntOrNull() ?: -1
                                        if (chapterNum >= 0) break
                                    }
                                }
                            }
                        }
                    }

                    // Fallback to previous chapter if transition/unspecified reference
                    if (chapterNum < 0) {
                        chapterNum = if (lastAssignedChapter >= 0) lastAssignedChapter else (if (isIntroBook) 0 else 1)
                    }
                    lastAssignedChapter = chapterNum

                    val chapter = chaptersList.firstOrNull { it.chapterNumber == chapterNum }
                    val isArabicPrimary = (primaryFile == araFile)

                    val isMuslim = bookSlug.contains("muslim")

                    // Urdu: match strictly by the hadith's own identity (number / reference);
                    // position in the file is used only as a last resort and only if both files line up.
                    val urduIdxAligned = urdArrayLength >= 0 && urdArrayLength == hadithsArray.length()
                    val urduSupplement = UrduHadithSupplement.getTranslation(bookSlug, hadithNumber)
                        ?: UrduHadithSupplement.getTranslation(bookSlug, rawNumber)
                    val urduText = urduSupplement?.takeIf { it.isNotBlank() } ?: if (isMuslim) {
                        (if (refBook >= 0 && refHadith > 0) urdMap["ref_${refBook}_${refHadith}"] else null)
                            ?: urdMap[hadithNumber]
                            ?: urdMap[rawNumber]
                            ?: (if (araNumber.isNotBlank()) urdMap["ara_${normalizeHadithNumber(araNumber)}"] else null)
                            ?: (if (araNumber.isNotBlank()) urdMap["ara_$araNumber"] else null)
                            ?: (if (urduIdxAligned) urdMap["idx_$i"] else null)
                            ?: if (primaryFile == urdFile) cleanTranslationText(text, "urd", bookSlug, hadithNumber) else ""
                    } else {
                        urdMap[hadithNumber]
                            ?: urdMap[rawNumber]
                            ?: (if (refBook >= 0 && refHadith > 0) urdMap["ref_${refBook}_${refHadith}"] else null)
                            ?: (if (araNumber.isNotBlank()) urdMap["ara_${normalizeHadithNumber(araNumber)}"] else null)
                            ?: (if (araNumber.isNotBlank()) urdMap["ara_$araNumber"] else null)
                            ?: (if (urduIdxAligned) urdMap["idx_$i"] else null)
                            ?: if (primaryFile == urdFile) cleanTranslationText(text, "urd", bookSlug, hadithNumber) else ""
                    }

                    val englishText = if (isMuslim) {
                        (if (refBook >= 0 && refHadith > 0) engMap["ref_${refBook}_${refHadith}"] else null)
                            ?: engMap[hadithNumber]
                            ?: engMap[rawNumber]
                            ?: engMap["idx_$i"]
                            ?: (if (araNumber.isNotBlank()) engMap["ara_${normalizeHadithNumber(araNumber)}"] else null)
                            ?: (if (araNumber.isNotBlank()) engMap["ara_$araNumber"] else null)
                            ?: if (primaryFile == engFile) cleanTranslationText(text, "eng", bookSlug, hadithNumber) else ""
                    } else {
                        engMap["idx_$i"]
                            ?: engMap[hadithNumber]
                            ?: engMap[rawNumber]
                            ?: (if (refBook >= 0 && refHadith > 0) engMap["ref_${refBook}_${refHadith}"] else null)
                            ?: (if (araNumber.isNotBlank()) engMap["ara_${normalizeHadithNumber(araNumber)}"] else null)
                            ?: (if (araNumber.isNotBlank()) engMap["ara_$araNumber"] else null)
                            ?: if (primaryFile == engFile) cleanTranslationText(text, "eng", bookSlug, hadithNumber) else ""
                    }

                    // Discard phantom indexing gaps where all texts are empty and refBook is 0/invalid
                    val isPhantomGap = text.isBlank() && urduText.isBlank() && englishText.isBlank() && (refBook <= 0 && refChapter <= 0)
                    if (isPhantomGap) {
                        continue
                    }

                    val arabicText = if (isArabicPrimary) text.trim() else ""

                    val hadithItem = Hadith(
                        id = hObj.optLong("hadithnumber", (i + 1).toLong()),
                        bookSlug = bookSlug,
                        bookName = bookName,
                        chapterNumber = chapterNum,
                        chapterNameArabic = chapter?.arabicTitle ?: "باب $chapterNum",
                        chapterNameUrdu = chapter?.urduTitle ?: "باب $chapterNum",
                        hadithNumber = hadithNumber,
                        arabicText = arabicText,
                        urduTranslation = urduText,
                        englishTranslation = englishText,
                        activeTranslation = "",
                        bookRefNumber = refBook,
                        hadithRefNumber = refHadith,
                        arabicNumber = araNumber,
                        indexInBook = i
                    )

                    hadithMap.getOrPut(chapterNum) { mutableListOf() }.add(hadithItem)
                }
            }

            val finalChapters = if (chaptersList.isNotEmpty()) {
                val populated = chaptersList.filter { hadithMap[it.chapterNumber]?.isNotEmpty() == true }
                if (populated.isNotEmpty()) populated else chaptersList
            } else {
                hadithMap.keys.sorted().map { chNum ->
                    val fallback = fallbackChapters.firstOrNull { it.chapterNumber == chNum }
                    HadithChapter(
                        id = chNum,
                        bookSlug = bookSlug,
                        chapterNumber = chNum,
                        arabicTitle = fallback?.arabicTitle ?: "الباب $chNum",
                        urduTitle = fallback?.urduTitle ?: "باب $chNum",
                        englishTitle = fallback?.englishTitle ?: "Chapter $chNum"
                    )
                }
            }

            val parsed = ParsedHadithBookData(chapters = finalChapters, hadithsByChapter = hadithMap)
            parsedBooksCache[bookSlug] = parsed
            return parsed
        } catch (_: Exception) {
            return null
        }
    }

    fun normalizeLang(code: String): String {
        return when (code.lowercase().trim()) {
            "ur", "urdu", "urd" -> "urd"
            "en", "english", "eng" -> "eng"
            "bn", "bengali", "ben" -> "ben"
            "fr", "french", "fra" -> "fra"
            "id", "indonesian", "ind" -> "ind"
            "tr", "turkish", "tur" -> "tur"
            "ru", "russian", "rus" -> "rus"
            "ta", "tamil", "tam" -> "tam"
            else -> code.lowercase().trim()
        }
    }

    private fun getTranslationMap(bookSlug: String, langCode: String): Map<String, String> {
        val normLang = normalizeLang(langCode)
        val cacheKey = "${bookSlug}_$normLang"
        translationsCache[cacheKey]?.let { return it }

        val hadithsDir = downloadManager?.hadithDir
            ?: downloadManager?.getBookFile(bookSlug)?.parentFile
            ?: context?.let { File(it.filesDir, "hadiths") }

        if (hadithsDir == null || !hadithsDir.exists()) return emptyMap()

        if (normLang == "urd" && (bookSlug in listOf("arbaeen-nawawi", "hadith-qudsi", "nawawi", "qudsi"))) {
            val assetName = if (bookSlug.contains("nawawi")) "nawawi" else "qudsi"
            val targetSlug = if (bookSlug.contains("nawawi")) "arbaeen-nawawi" else "hadith-qudsi"
            val targetUrd = File(hadithsDir, "${targetSlug}_urd.json")
            val targetUrdu = File(hadithsDir, "${targetSlug}_urdu.json")
            if (!targetUrd.exists() || targetUrd.length() < 1000) {
                try {
                    context?.assets?.open("hadiths/urd-$assetName.json.gz")?.use { input ->
                        java.util.zip.GZIPInputStream(input).use { gz ->
                            targetUrd.outputStream().use { out -> gz.copyTo(out) }
                        }
                    }
                    if (targetUrd.exists()) {
                        targetUrd.copyTo(targetUrdu, overwrite = true)
                    }
                } catch (_: Exception) {}
            }
        }

        val langFile = File(hadithsDir, "${bookSlug}_${normLang}.json").takeIf { it.exists() && it.length() > 500 }
            ?: if (normLang == "urd") {
                File(hadithsDir, "${bookSlug}_urd.json").takeIf { it.exists() && it.length() > 500 }
                    ?: File(hadithsDir, "${bookSlug}_urdu.json").takeIf { it.exists() && it.length() > 500 }
            } else if (normLang == "eng") {
                File(hadithsDir, "${bookSlug}_eng.json").takeIf { it.exists() && it.length() > 500 }
                    ?: File(hadithsDir, "${bookSlug}_english.json").takeIf { it.exists() && it.length() > 500 }
            } else null

        if (langFile == null) return emptyMap()

        val map = mutableMapOf<String, String>()
        try {
            val content = langFile.readText(Charsets.UTF_8)
            val root = JSONObject(content)
            val hadithsArray = root.optJSONArray("hadiths")
            if (hadithsArray != null) {
                for (i in 0 until hadithsArray.length()) {
                    val hObj = hadithsArray.getJSONObject(i)
                    val rawNum = hObj.optString("hadithnumber").ifBlank {
                        hObj.optString("hadithNumber").ifBlank {
                            hObj.optString("arabicnumber").ifBlank {
                                ""
                            }
                        }
                    }.trim()
                    val rawTxt = hObj.optString("text", "")
                    val norm = normalizeHadithNumber(rawNum)
                    val supplement = if (normLang == "urd") {
                        UrduHadithSupplement.getTranslation(bookSlug, norm)
                            ?: UrduHadithSupplement.getTranslation(bookSlug, rawNum)
                    } else null
                    val txt = cleanTranslationText(rawTxt.ifBlank { supplement ?: "" }, normLang, bookSlug, norm)
                    if (txt.isNotBlank()) {
                        map["idx_$i"] = txt
                        if (rawNum.isNotBlank()) map[rawNum] = txt
                        if (norm.isNotBlank()) map[norm] = txt

                        val ref = hObj.optJSONObject("reference")
                        if (ref != null) {
                            val b = ref.optInt("book", -1)
                            val h = ref.optInt("hadith", -1)
                            if (b >= 0 && h > 0) {
                                map["ref_${b}_${h}"] = txt
                            }
                        }

                        if (!hObj.isNull("arabicnumber")) {
                            val aNum = normalizeHadithNumber(hObj.optString("arabicnumber"))
                            if (aNum.isNotBlank()) {
                                map["ara_$aNum"] = txt
                            }
                        }
                    }
                }
            }
            if (map.isNotEmpty()) {
                translationsCache[cacheKey] = map
            }
        } catch (_: Throwable) {}

        return map
    }

    fun invalidateTranslationCache(bookSlug: String? = null, langCode: String? = null) {
        if (bookSlug != null && langCode != null) {
            val normLang = normalizeLang(langCode)
            translationsCache.remove("${bookSlug}_$normLang")
            translationsCache.remove("${bookSlug}_$langCode")
        } else if (bookSlug != null) {
            translationsCache.keys.filter { it.startsWith(bookSlug) }.forEach { translationsCache.remove(it) }
        } else {
            translationsCache.clear()
        }
    }

    fun getChaptersForBook(bookSlug: String): List<HadithChapter> {
        val authoritative = AuthoritativeContentProvider.getChaptersForBook(bookSlug)
        val parsed = getParsedBookData(bookSlug)
        if (parsed != null && parsed.chapters.isNotEmpty()) {
            return parsed.chapters.map { ch ->
                val authCh = authoritative.firstOrNull { it.chapterNumber == ch.chapterNumber }
                if (authCh != null && authCh.arabicTitle.isNotBlank()) {
                    ch.copy(
                        arabicTitle = authCh.arabicTitle,
                        englishTitle = if (ch.englishTitle.isNotBlank() && !ch.englishTitle.startsWith("Chapter ")) ch.englishTitle else authCh.englishTitle,
                        urduTitle = if (ch.urduTitle.isNotBlank() && !ch.urduTitle.startsWith("باب ")) ch.urduTitle else authCh.urduTitle
                    )
                } else {
                    ch
                }
            }
        }
        return authoritative
    }

    fun getChapterForHadith(bookSlug: String, chapterNumber: Int, hadithNumber: String): HadithChapter {
        val normNum = normalizeHadithNumber(hadithNumber)
        val parsed = getParsedBookData(bookSlug)

        // 1. If parsed book exists, look for which chapter contains this hadithNumber
        if (parsed != null) {
            for ((chNum, hadithsList) in parsed.hadithsByChapter) {
                val found = hadithsList.any {
                    it.hadithNumber == hadithNumber ||
                    normalizeHadithNumber(it.hadithNumber) == normNum
                }
                if (found) {
                    val matchingCh = parsed.chapters.firstOrNull { it.chapterNumber == chNum }
                    if (matchingCh != null) return matchingCh
                    val fallback = AuthoritativeContentProvider.getChaptersForBook(bookSlug).firstOrNull { it.chapterNumber == chNum }
                    return HadithChapter(
                        id = if (chNum > 0) chNum else 1000,
                        bookSlug = bookSlug,
                        chapterNumber = chNum,
                        arabicTitle = fallback?.arabicTitle ?: "الباب $chNum",
                        urduTitle = fallback?.urduTitle ?: "باب $chNum",
                        englishTitle = fallback?.englishTitle ?: "Chapter $chNum"
                    )
                }
            }
        }

        // 2. Try by chapterNumber in getChaptersForBook
        val allChapters = getChaptersForBook(bookSlug)
        val byNum = allChapters.firstOrNull { it.chapterNumber == chapterNumber }
        if (byNum != null) return byNum

        // 3. Try sample hadiths from AuthoritativeContentProvider
        val fallbackChapters = AuthoritativeContentProvider.getChaptersForBook(bookSlug)
        for (ch in fallbackChapters) {
            val sampleHadiths = AuthoritativeContentProvider.getHadithsForChapter(bookSlug, ch.chapterNumber)
            if (sampleHadiths.any { it.hadithNumber == hadithNumber || normalizeHadithNumber(it.hadithNumber) == normNum }) {
                return ch
            }
        }

        // 4. Default / synthesize valid chapter so reader never fails or goes back to book list
        val safeNum = if (chapterNumber > 0) chapterNumber else 1
        val fallback = fallbackChapters.firstOrNull { it.chapterNumber == safeNum }
        return fallback ?: HadithChapter(
            id = safeNum,
            bookSlug = bookSlug,
            chapterNumber = safeNum,
            arabicTitle = "الباب $safeNum",
            urduTitle = "باب $safeNum",
            englishTitle = "Chapter $safeNum"
        )
    }

    fun getTranslationForHadith(
        bookSlug: String,
        hadithNumber: String,
        chapterNumber: Int = 1,
        langCode: String = "urd",
        fallbackUrdu: String = "",
        fallbackEnglish: String = ""
    ): String {
        val normLang = normalizeLang(langCode)
        val translationMap = getTranslationMap(bookSlug, normLang)
        val normNum = normalizeHadithNumber(hadithNumber)

        val supplement = if (normLang == "urd") {
            UrduHadithSupplement.getTranslation(bookSlug, normNum)
                ?: UrduHadithSupplement.getTranslation(bookSlug, hadithNumber)
        } else null

        val found = supplement
            ?: translationMap[normNum]
            ?: translationMap[hadithNumber]
            ?: translationMap["ref_${chapterNumber}_${hadithNumber}"]
            ?: translationMap["ref_${chapterNumber}_$normNum"]

        if (!found.isNullOrBlank()) {
            return cleanTranslationText(found, normLang, bookSlug, normNum)
        }
        val fallback = when (normLang) {
            "urd" -> fallbackUrdu.takeIf { UrduHadithSupplement.hasUrduScript(it) } ?: ""
            "eng" -> fallbackEnglish.ifBlank { fallbackUrdu }
            else -> fallbackUrdu.ifBlank { fallbackEnglish }
        }
        return cleanTranslationText(fallback, normLang, bookSlug, normNum)
    }

    fun getHadithsForChapter(bookSlug: String, chapterNumber: Int, langCode: String = "urd"): List<Hadith> {
        val normLang = normalizeLang(langCode)
        val parsed = getParsedBookData(bookSlug)
        if (parsed != null) {
            val list = parsed.hadithsByChapter[chapterNumber]
                ?: if (bookSlug in listOf("arbaeen-nawawi", "hadith-qudsi", "nawawi", "qudsi")) {
                    parsed.hadithsByChapter[1] ?: parsed.hadithsByChapter.values.firstOrNull()
                } else null
            if (!list.isNullOrEmpty()) {
                val translationMap = getTranslationMap(bookSlug, normLang)
                val isMuslim = bookSlug.contains("muslim")
                return list.map { hadith ->
                    val normNum = normalizeHadithNumber(hadith.hadithNumber)
                    val rawTranslation = if (normLang == "urd") {
                        val supplement = UrduHadithSupplement.getTranslation(bookSlug, normNum)
                            ?: UrduHadithSupplement.getTranslation(bookSlug, hadith.hadithNumber)
                        val byNumber = translationMap[normNum] ?: translationMap[hadith.hadithNumber]
                        val byRef = if (hadith.bookRefNumber >= 0 && hadith.hadithRefNumber > 0) translationMap["ref_${hadith.bookRefNumber}_${hadith.hadithRefNumber}"] else null
                        val byAra = if (hadith.arabicNumber.isNotBlank()) {
                            translationMap["ara_${normalizeHadithNumber(hadith.arabicNumber)}"] ?: translationMap["ara_${hadith.arabicNumber}"]
                        } else null
                        val byPos = if (hadith.indexInBook >= 0 && !isMuslim && !bookSlug.contains("malik")) translationMap["idx_${hadith.indexInBook}"] else null
                        supplement ?: byNumber ?: byRef ?: byAra ?: byPos ?: hadith.urduTranslation
                    } else if (isMuslim) {
                        (if (hadith.bookRefNumber >= 0 && hadith.hadithRefNumber > 0) translationMap["ref_${hadith.bookRefNumber}_${hadith.hadithRefNumber}"] else null)
                            ?: translationMap[normNum]
                            ?: translationMap[hadith.hadithNumber]
                            ?: (if (hadith.indexInBook >= 0) translationMap["idx_${hadith.indexInBook}"] else null)
                            ?: (if (hadith.arabicNumber.isNotBlank()) translationMap["ara_${normalizeHadithNumber(hadith.arabicNumber)}"] else null)
                            ?: (if (hadith.arabicNumber.isNotBlank()) translationMap["ara_${hadith.arabicNumber}"] else null)
                            ?: if (normLang == "urd") hadith.urduTranslation
                            else if (normLang == "eng") hadith.englishTranslation
                            else ""
                    } else {
                        (if (hadith.indexInBook >= 0) translationMap["idx_${hadith.indexInBook}"] else null)
                            ?: translationMap[normNum]
                            ?: translationMap[hadith.hadithNumber]
                            ?: (if (hadith.bookRefNumber >= 0 && hadith.hadithRefNumber > 0) translationMap["ref_${hadith.bookRefNumber}_${hadith.hadithRefNumber}"] else null)
                            ?: (if (hadith.arabicNumber.isNotBlank()) translationMap["ara_${normalizeHadithNumber(hadith.arabicNumber)}"] else null)
                            ?: (if (hadith.arabicNumber.isNotBlank()) translationMap["ara_${hadith.arabicNumber}"] else null)
                            ?: if (normLang == "urd") hadith.urduTranslation
                            else if (normLang == "eng") hadith.englishTranslation
                            else ""
                    }
                    var translation = cleanTranslationText(rawTranslation, normLang, bookSlug, normNum)

                    // Strict authentic handling: NEVER fallback to English when Urdu is requested
                    if (normLang == "urd") {
                        if (translation.isBlank()) {
                            val supplement = UrduHadithSupplement.getTranslation(bookSlug, normNum)
                                ?: UrduHadithSupplement.getTranslation(bookSlug, hadith.hadithNumber)
                            if (!supplement.isNullOrBlank()) {
                                translation = supplement
                            } else if (hadith.urduTranslation.isNotBlank() && UrduHadithSupplement.hasUrduScript(hadith.urduTranslation)) {
                                translation = hadith.urduTranslation
                            }
                        } else if (!UrduHadithSupplement.hasUrduScript(translation)) {
                            translation = if (hadith.urduTranslation.isNotBlank() && UrduHadithSupplement.hasUrduScript(hadith.urduTranslation)) {
                                hadith.urduTranslation
                            } else {
                                ""
                            }
                        }
                    } else if (normLang == "eng") {
                        if (translation.isBlank() && hadith.englishTranslation.isNotBlank()) {
                            translation = hadith.englishTranslation
                        }
                    } else {
                        if (translation.isBlank()) {
                            if (hadith.urduTranslation.isNotBlank()) translation = hadith.urduTranslation
                            else if (hadith.englishTranslation.isNotBlank()) translation = hadith.englishTranslation
                        }
                    }
                    hadith.copy(activeTranslation = translation)
                }
            }
            if (bookSlug !in listOf("arbaeen-nawawi", "hadith-qudsi", "nawawi", "qudsi")) {
                return emptyList()
            }
        }
        return AuthoritativeContentProvider.getHadithsForChapter(bookSlug, chapterNumber).map { hadith ->
            val normNum = normalizeHadithNumber(hadith.hadithNumber)
            val supplement = if (normLang == "urd") {
                UrduHadithSupplement.getTranslation(bookSlug, normNum)
                    ?: UrduHadithSupplement.getTranslation(bookSlug, hadith.hadithNumber)
            } else null
            val rawTranslation = supplement ?: when (normLang) {
                "eng" -> hadith.englishTranslation
                "urd" -> hadith.urduTranslation
                else -> hadith.urduTranslation.ifBlank { hadith.englishTranslation }
            }
            val translation = cleanTranslationText(rawTranslation, normLang, bookSlug, hadith.hadithNumber)
            hadith.copy(activeTranslation = translation)
        }
    }

    fun clearCacheForBook(bookSlug: String) {
        parsedBooksCache.remove(bookSlug)
        translationsCache.keys.filter { it.startsWith(bookSlug) }.forEach { translationsCache.remove(it) }
    }

    private fun readJsonStringOrNumber(reader: android.util.JsonReader): String {
        return try {
            when (reader.peek()) {
                android.util.JsonToken.STRING -> reader.nextString()
                android.util.JsonToken.NUMBER -> reader.nextString()
                android.util.JsonToken.BOOLEAN -> reader.nextBoolean().toString()
                android.util.JsonToken.NULL -> {
                    reader.nextNull()
                    ""
                }
                else -> {
                    reader.skipValue()
                    ""
                }
            }
        } catch (_: Exception) {
            ""
        }
    }

    private fun readSingleHadithTranslationStreaming(
        file: File,
        targetHadithNumber: String,
        targetNormNum: String,
        targetRefBook: Int = -1,
        targetRefHadith: Int = -1,
        targetArabicNum: String = "",
        lang: String,
        bookSlug: String
    ): String {
        if (!file.exists() || file.length() < 500) return ""
        try {
            java.io.FileInputStream(file).bufferedReader(Charsets.UTF_8).use { br ->
                val reader = android.util.JsonReader(br)
                reader.beginObject()
                while (reader.hasNext()) {
                    if (reader.nextName() == "hadiths") {
                        reader.beginArray()
                        var index = 0
                        while (reader.hasNext()) {
                            reader.beginObject()
                            var hNum = ""
                            var aNum = ""
                            var text = ""
                            var rBook = -1
                            var rHadith = -1
                            while (reader.hasNext()) {
                                when (reader.nextName()) {
                                    "hadithnumber", "hadithNumber" -> hNum = readJsonStringOrNumber(reader)
                                    "arabicnumber" -> aNum = readJsonStringOrNumber(reader)
                                    "text" -> text = try { reader.nextString() } catch (_: Exception) { "" }
                                    "reference" -> {
                                        try {
                                            reader.beginObject()
                                            while (reader.hasNext()) {
                                                when (reader.nextName()) {
                                                    "book" -> rBook = try { reader.nextInt() } catch (_: Exception) { -1 }
                                                    "hadith" -> rHadith = try { reader.nextInt() } catch (_: Exception) { -1 }
                                                    else -> reader.skipValue()
                                                }
                                            }
                                            reader.endObject()
                                        } catch (_: Exception) {
                                            try { reader.skipValue() } catch (_: Exception) {}
                                        }
                                    }
                                    else -> reader.skipValue()
                                }
                            }
                            reader.endObject()
                            val normH = normalizeHadithNumber(hNum)
                            val normA = normalizeHadithNumber(aNum)

                            val matches = (normH.isNotBlank() && normH == targetNormNum) ||
                                    hNum == targetHadithNumber ||
                                    (targetArabicNum.isNotBlank() && (normA == targetArabicNum || aNum == targetArabicNum)) ||
                                    (targetRefBook > 0 && targetRefHadith > 0 && rBook == targetRefBook && rHadith == targetRefHadith)

                            if (matches && text.isNotBlank()) {
                                return cleanTranslationText(text, lang, bookSlug, targetNormNum)
                            }
                            index++
                        }
                        reader.endArray()
                    } else {
                        reader.skipValue()
                    }
                }
                reader.endObject()
            }
        } catch (_: Throwable) {}
        return ""
    }

    private fun getFastSingleTranslation(
        bookSlug: String,
        hadithNumber: String,
        normNumber: String,
        chapterNumber: Int,
        refBook: Int,
        refHadith: Int,
        arabicNumber: String,
        langCode: String
    ): String {
        val normLang = normalizeLang(langCode)
        if (normLang == "urd") {
            val supplement = UrduHadithSupplement.getTranslation(bookSlug, normNumber)
                ?: UrduHadithSupplement.getTranslation(bookSlug, hadithNumber)
            if (!supplement.isNullOrBlank()) {
                return cleanTranslationText(supplement, "urd", bookSlug, normNumber)
            }
        }

        val cacheKey = "${bookSlug}_$normLang"
        translationsCache[cacheKey]?.let { cachedMap ->
            cachedMap[normNumber]?.let { return cleanTranslationText(it, normLang, bookSlug, normNumber) }
            cachedMap[hadithNumber]?.let { return cleanTranslationText(it, normLang, bookSlug, normNumber) }
        }

        val hadithsDir = downloadManager?.hadithDir
            ?: downloadManager?.getBookFile(bookSlug)?.parentFile
            ?: context?.let { File(it.filesDir, "hadiths") }

        if (hadithsDir != null && hadithsDir.exists()) {
            val langFile = File(hadithsDir, "${bookSlug}_${normLang}.json").takeIf { it.exists() && it.length() > 500 }
                ?: if (normLang == "urd") {
                    File(hadithsDir, "${bookSlug}_urd.json").takeIf { it.exists() && it.length() > 500 }
                        ?: File(hadithsDir, "${bookSlug}_urdu.json").takeIf { it.exists() && it.length() > 500 }
                } else if (normLang == "eng") {
                    File(hadithsDir, "${bookSlug}_eng.json").takeIf { it.exists() && it.length() > 500 }
                        ?: File(hadithsDir, "${bookSlug}_english.json").takeIf { it.exists() && it.length() > 500 }
                } else null

            if (langFile != null) {
                val found = readSingleHadithTranslationStreaming(
                    langFile,
                    hadithNumber,
                    normNumber,
                    refBook,
                    refHadith,
                    arabicNumber,
                    normLang,
                    bookSlug
                )
                if (found.isNotBlank()) {
                    return found
                }
            }
        }
        return ""
    }

    private suspend fun searchBookForHadithNumberStreaming(
        file: File,
        bookSlug: String,
        bookName: String,
        targetNumber: String,
        normTargetNumber: String,
        mappedHadithLang: String,
        chapters: List<HadithChapter>,
        onMatchFound: suspend (Hadith) -> Unit
    ) {
        if (!file.exists() || file.length() < 500) return

        try {
            java.io.FileInputStream(file).bufferedReader(Charsets.UTF_8).use { br ->
                val reader = android.util.JsonReader(br)
                reader.beginObject()
                while (reader.hasNext()) {
                    if (reader.nextName() == "hadiths") {
                        reader.beginArray()
                        var index = 0
                        while (reader.hasNext()) {
                            currentCoroutineContext().ensureActive()
                            reader.beginObject()
                            var hNum = ""
                            var aNum = ""
                            var text = ""
                            var rBook = -1
                            var rHadith = -1
                            var rChapter = -1
                            while (reader.hasNext()) {
                                when (reader.nextName()) {
                                    "hadithnumber", "hadithNumber" -> hNum = readJsonStringOrNumber(reader)
                                    "arabicnumber" -> aNum = readJsonStringOrNumber(reader)
                                    "text" -> text = try { reader.nextString() } catch (_: Exception) { "" }
                                    "reference" -> {
                                        try {
                                            reader.beginObject()
                                            while (reader.hasNext()) {
                                                when (reader.nextName()) {
                                                    "book" -> rBook = try { reader.nextInt() } catch (_: Exception) { -1 }
                                                    "hadith" -> rHadith = try { reader.nextInt() } catch (_: Exception) { -1 }
                                                    "chapter" -> rChapter = try { reader.nextInt() } catch (_: Exception) { -1 }
                                                    else -> reader.skipValue()
                                                }
                                            }
                                            reader.endObject()
                                        } catch (_: Exception) {
                                            try { reader.skipValue() } catch (_: Exception) {}
                                        }
                                    }
                                    else -> reader.skipValue()
                                }
                            }
                            reader.endObject()

                            val normH = normalizeHadithNumber(hNum)
                            val normA = normalizeHadithNumber(aNum)

                            val isExactMatch = (normH.isNotBlank() && normH == normTargetNumber) ||
                                    hNum == targetNumber ||
                                    (normA.isNotBlank() && normA == normTargetNumber)

                            if (isExactMatch) {
                                val chNum = when {
                                    rBook > 0 -> rBook
                                    rChapter > 0 -> rChapter
                                    rBook == 0 && (bookSlug.contains("muslim") || bookSlug.contains("ibnmajah")) -> 0
                                    else -> 1
                                }
                                val ch = chapters.firstOrNull { it.chapterNumber == chNum }
                                val chNameUrdu = ch?.urduTitle ?: "باب $chNum"
                                val chNameArabic = ch?.arabicTitle ?: "باب $chNum"

                                val activeTrans = getFastSingleTranslation(
                                    bookSlug, hNum, normH, chNum, rBook, rHadith, normA, mappedHadithLang
                                )
                                val urdTrans = if (mappedHadithLang == "urd") activeTrans else getFastSingleTranslation(
                                    bookSlug, hNum, normH, chNum, rBook, rHadith, normA, "urd"
                                )
                                val engTrans = if (mappedHadithLang == "eng") activeTrans else getFastSingleTranslation(
                                    bookSlug, hNum, normH, chNum, rBook, rHadith, normA, "eng"
                                )

                                val isAraFile = file.name.contains("_ara")
                                val arabicText = if (isAraFile) text.trim() else ""

                                val hadith = Hadith(
                                    id = (normH.toLongOrNull() ?: (index + 1).toLong()),
                                    bookSlug = bookSlug,
                                    bookName = bookName,
                                    chapterNumber = chNum,
                                    chapterNameArabic = chNameArabic,
                                    chapterNameUrdu = chNameUrdu,
                                    hadithNumber = if (normH.isNotBlank()) normH else hNum,
                                    arabicText = arabicText,
                                    urduTranslation = urdTrans,
                                    englishTranslation = engTrans,
                                    activeTranslation = activeTrans.ifBlank { urdTrans.ifBlank { engTrans } },
                                    bookRefNumber = rBook,
                                    hadithRefNumber = rHadith,
                                    arabicNumber = aNum,
                                    indexInBook = index
                                )
                                onMatchFound(hadith)
                            }
                            index++
                        }
                        reader.endArray()
                    } else {
                        reader.skipValue()
                    }
                }
                reader.endObject()
            }
        } catch (_: Throwable) {}
    }

    private suspend fun searchBookForWordStreaming(
        file: File,
        bookSlug: String,
        bookName: String,
        query: String,
        mappedHadithLang: String,
        chapters: List<HadithChapter>,
        onMatchFound: suspend (Hadith) -> Unit
    ) {
        if (!file.exists() || file.length() < 500) return

        try {
            java.io.FileInputStream(file).bufferedReader(Charsets.UTF_8).use { br ->
                val reader = android.util.JsonReader(br)
                reader.beginObject()
                while (reader.hasNext()) {
                    if (reader.nextName() == "hadiths") {
                        reader.beginArray()
                        var index = 0
                        while (reader.hasNext()) {
                            currentCoroutineContext().ensureActive()
                            reader.beginObject()
                            var hNum = ""
                            var aNum = ""
                            var text = ""
                            var rBook = -1
                            var rHadith = -1
                            var rChapter = -1
                            while (reader.hasNext()) {
                                when (reader.nextName()) {
                                    "hadithnumber", "hadithNumber" -> hNum = readJsonStringOrNumber(reader)
                                    "arabicnumber" -> aNum = readJsonStringOrNumber(reader)
                                    "text" -> text = try { reader.nextString() } catch (_: Exception) { "" }
                                    "reference" -> {
                                        try {
                                            reader.beginObject()
                                            while (reader.hasNext()) {
                                                when (reader.nextName()) {
                                                    "book" -> rBook = try { reader.nextInt() } catch (_: Exception) { -1 }
                                                    "hadith" -> rHadith = try { reader.nextInt() } catch (_: Exception) { -1 }
                                                    "chapter" -> rChapter = try { reader.nextInt() } catch (_: Exception) { -1 }
                                                    else -> reader.skipValue()
                                                }
                                            }
                                            reader.endObject()
                                        } catch (_: Exception) {
                                            try { reader.skipValue() } catch (_: Exception) {}
                                        }
                                    }
                                    else -> reader.skipValue()
                                }
                            }
                            reader.endObject()

                            val normH = normalizeHadithNumber(hNum)
                            val isTextMatch = text.contains(query, ignoreCase = true) ||
                                    ArabicNormalizationUtils.containsNormalized(text, query)
                            val isNumMatch = normH.contains(query) || hNum.contains(query)

                            if (isTextMatch || isNumMatch) {
                                val chNum = when {
                                    rBook > 0 -> rBook
                                    rChapter > 0 -> rChapter
                                    rBook == 0 && (bookSlug.contains("muslim") || bookSlug.contains("ibnmajah")) -> 0
                                    else -> 1
                                }
                                val ch = chapters.firstOrNull { it.chapterNumber == chNum }
                                val chNameUrdu = ch?.urduTitle ?: "باب $chNum"
                                val chNameArabic = ch?.arabicTitle ?: "باب $chNum"

                                val isAraFile = file.name.contains("_ara")
                                val isUrdFile = file.name.contains("_urd") || file.name.contains("_urdu")
                                val isEngFile = file.name.contains("_eng") || file.name.contains("_english")

                                val urdTrans = if (isUrdFile) text.trim() else getFastSingleTranslation(
                                    bookSlug, hNum, normH, chNum, rBook, rHadith, aNum, "urd"
                                )
                                val engTrans = if (isEngFile) text.trim() else getFastSingleTranslation(
                                    bookSlug, hNum, normH, chNum, rBook, rHadith, aNum, "eng"
                                )
                                val activeTrans = when (mappedHadithLang) {
                                    "urd" -> urdTrans
                                    "eng" -> engTrans
                                    else -> if (file.name.contains("_${mappedHadithLang}")) text.trim()
                                    else getFastSingleTranslation(
                                        bookSlug, hNum, normH, chNum, rBook, rHadith, aNum, mappedHadithLang
                                    )
                                }
                                val arabicText = if (isAraFile) text.trim() else ""

                                val hadith = Hadith(
                                    id = (normH.toLongOrNull() ?: (index + 1).toLong()),
                                    bookSlug = bookSlug,
                                    bookName = bookName,
                                    chapterNumber = chNum,
                                    chapterNameArabic = chNameArabic,
                                    chapterNameUrdu = chNameUrdu,
                                    hadithNumber = if (normH.isNotBlank()) normH else hNum,
                                    arabicText = arabicText,
                                    urduTranslation = urdTrans,
                                    englishTranslation = engTrans,
                                    activeTranslation = activeTrans.ifBlank { urdTrans.ifBlank { engTrans } },
                                    bookRefNumber = rBook,
                                    hadithRefNumber = rHadith,
                                    arabicNumber = aNum,
                                    indexInBook = index
                                )
                                onMatchFound(hadith)
                            }
                            index++
                        }
                        reader.endArray()
                    } else {
                        reader.skipValue()
                    }
                }
                reader.endObject()
            }
        } catch (_: Throwable) {}
    }

    /**
     * Flow-based streaming search across downloaded Hadith books.
     * Batches emissions (e.g. 1st match, then every 5 matches) in real-time
     * while running 100% on background thread without any UI lag.
     */
    fun searchDownloadedHadithsFlow(
        query: String,
        searchByNumber: Boolean,
        langCode: String,
        bookSlug: String? = null
    ): Flow<HadithSearchBatch> = flow {
        if (query.isBlank()) {
            emit(HadithSearchBatch(results = emptyList(), progress = 100, isComplete = true))
            return@flow
        }
        val trimmed = query.trim()

        val allBooks = getAllBooks()
        val downloadedBooks = allBooks.filter { book ->
            val matchesBook = bookSlug == null || book.slug == bookSlug
            matchesBook && downloadManager?.isBookDownloaded(book.slug) == true
        }

        val isNumericQuery = trimmed.toIntOrNull() != null
        val mappedHadithLang = when (langCode.lowercase()) {
            "en", "eng" -> "eng"
            "ur", "urd" -> "urd"
            "hi", "hin" -> "hin"
            "bn", "ben" -> "ben"
            "ar", "ara" -> "ara"
            "fr", "fra" -> "fra"
            "id", "ind" -> "ind"
            "tr", "tur" -> "tur"
            "ru", "rus" -> "rus"
            "ta", "tam" -> "tam"
            else -> langCode.lowercase()
        }

        val hadithsDir = downloadManager?.hadithDir
            ?: context?.let { File(it.filesDir, "hadiths") }

        val accumulatedResults = mutableListOf<Hadith>()
        var unemittedCount = 0
        val isNumberSearch = searchByNumber || isNumericQuery
        val normTarget = normalizeHadithNumber(trimmed)

        if (downloadedBooks.isNotEmpty() && hadithsDir != null && hadithsDir.exists()) {
            val totalBooks = downloadedBooks.size

            for ((bookIndex, book) in downloadedBooks.withIndex()) {
                currentCoroutineContext().ensureActive()

                val araFile = File(hadithsDir, "${book.slug}_ara.json").takeIf { it.exists() && it.length() > 500 }
                val urdFile = File(hadithsDir, "${book.slug}_urdu.json").takeIf { it.exists() && it.length() > 500 }
                    ?: File(hadithsDir, "${book.slug}_urd.json").takeIf { it.exists() && it.length() > 500 }
                val engFile = File(hadithsDir, "${book.slug}_english.json").takeIf { it.exists() && it.length() > 500 }
                    ?: File(hadithsDir, "${book.slug}_eng.json").takeIf { it.exists() && it.length() > 500 }

                val primaryFile = when (mappedHadithLang) {
                    "urd" -> urdFile ?: araFile ?: engFile
                    "eng" -> engFile ?: urdFile ?: araFile
                    "ara" -> araFile ?: urdFile ?: engFile
                    else -> {
                        val langSpecific = File(hadithsDir, "${book.slug}_${mappedHadithLang}.json").takeIf { it.exists() && it.length() > 500 }
                        langSpecific ?: urdFile ?: engFile ?: araFile
                    }
                } ?: continue

                val chapters = AuthoritativeContentProvider.getChaptersForBook(book.slug)
                val bookProgress = (((bookIndex + 1) * 100) / totalBooks).coerceIn(10, 95)

                if (isNumberSearch) {
                    searchBookForHadithNumberStreaming(
                        file = primaryFile,
                        bookSlug = book.slug,
                        bookName = book.englishName,
                        targetNumber = trimmed,
                        normTargetNumber = normTarget,
                        mappedHadithLang = mappedHadithLang,
                        chapters = chapters
                    ) { match ->
                        accumulatedResults.add(match)
                        emit(HadithSearchBatch(results = accumulatedResults.toList(), progress = bookProgress))
                    }
                } else {
                    searchBookForWordStreaming(
                        file = primaryFile,
                        bookSlug = book.slug,
                        bookName = book.englishName,
                        query = trimmed,
                        mappedHadithLang = mappedHadithLang,
                        chapters = chapters
                    ) { match ->
                        accumulatedResults.add(match)
                        unemittedCount++
                        // Instant 1st match, and every 5 matches
                        if (accumulatedResults.size == 1 || unemittedCount >= 5) {
                            emit(HadithSearchBatch(results = accumulatedResults.toList(), progress = bookProgress))
                            unemittedCount = 0
                        }
                    }
                    if (unemittedCount > 0) {
                        emit(HadithSearchBatch(results = accumulatedResults.toList(), progress = bookProgress))
                        unemittedCount = 0
                    }
                }
            }

            emit(HadithSearchBatch(results = accumulatedResults.toList(), progress = 100, isComplete = true))
        } else {
            // Fallback: search sample built-in hadiths
            for (book in allBooks) {
                if (bookSlug != null && book.slug != bookSlug) continue
                val chapters = AuthoritativeContentProvider.getChaptersForBook(book.slug)
                for (ch in chapters) {
                    val hadiths = AuthoritativeContentProvider.getHadithsForChapter(book.slug, ch.chapterNumber)
                    for (h in hadiths) {
                        val normH = normalizeHadithNumber(h.hadithNumber)
                        val isMatch = if (isNumberSearch) {
                            normH == normTarget || h.hadithNumber.equals(trimmed, ignoreCase = true)
                        } else {
                            h.hadithNumber.contains(trimmed) ||
                                    h.urduTranslation.contains(trimmed, ignoreCase = true) ||
                                    h.englishTranslation.contains(trimmed, ignoreCase = true) ||
                                    ArabicNormalizationUtils.containsNormalized(h.arabicText, trimmed)
                        }
                        if (isMatch) {
                            val activeTrans = if (mappedHadithLang == "urd") h.urduTranslation else h.englishTranslation
                            accumulatedResults.add(h.copy(activeTranslation = activeTrans))
                            unemittedCount++
                            if (accumulatedResults.size == 1 || unemittedCount >= 5) {
                                emit(HadithSearchBatch(results = accumulatedResults.toList(), progress = 50))
                                unemittedCount = 0
                            }
                        }
                    }
                }
            }
            emit(HadithSearchBatch(results = accumulatedResults.toList(), progress = 100, isComplete = true))
        }
    }.flowOn(Dispatchers.IO)

    fun searchHadiths(query: String): List<Hadith> {
        if (query.isBlank()) return emptyList()
        val trimmed = query.trim()
        val isNumber = trimmed.toIntOrNull() != null
        val (results, _) = searchDownloadedHadiths(trimmed, searchByNumber = isNumber, langCode = "ur")
        return results
    }

    /**
     * Synchronous search helper for backward compatibility.
     */
    fun searchDownloadedHadiths(
        query: String,
        searchByNumber: Boolean,
        langCode: String,
        bookSlug: String? = null
    ): Pair<List<Hadith>, Int> {
        if (query.isBlank()) return Pair(emptyList(), 0)
        var lastBatch = HadithSearchBatch(emptyList(), 0)
        kotlinx.coroutines.runBlocking {
            searchDownloadedHadithsFlow(query, searchByNumber, langCode, bookSlug).collect {
                lastBatch = it
            }
        }
        val downloadedCount = getAllBooks().count { downloadManager?.isBookDownloaded(it.slug) == true }
        return Pair(lastBatch.results, downloadedCount)
    }
}
