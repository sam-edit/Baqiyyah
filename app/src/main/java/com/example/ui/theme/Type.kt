package com.example.ui.theme

import android.content.Context
import android.graphics.Typeface
import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

/**
 * Script language classifications for accurate typography mapping.
 */
enum class ScriptLanguage {
    ARABIC,
    URDU,
    ENGLISH
}

/**
 * Al Deen Centralized Typography Architecture
 * Manages exact custom fonts for Urdu (notonastaliqurdu-regular.ttf),
 * Arabic (scheherazadenew-regular.ttf), and English (default system font).
 */
object AlDeenFontRegistry {
    val EnglishFontFamily: FontFamily = FontFamily.Default
    var UrduFontFamily: FontFamily = FontFamily.Default
        private set
    var ArabicFontFamily: FontFamily = FontFamily.Default
        private set
    var QuranFontFamily: FontFamily = FontFamily.Default
        private set
    var HadithArabicFontFamily: FontFamily = FontFamily.Default
        private set
    val SurahGlyphFontFamily: FontFamily = FontFamily.Default

    @Volatile
    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        synchronized(this) {
            if (isInitialized) return

            // Load Urdu font (notonastaliqurdu-regular.ttf)
            try {
                UrduFontFamily = try {
                    FontFamily(Font("fonts/notonastaliqurdu-regular.ttf", context.assets))
                } catch (_: Throwable) {
                    FontFamily(Font("notonastaliqurdu-regular.ttf", context.assets))
                }
            } catch (_: Throwable) {
                try {
                    val tf = try {
                        Typeface.createFromAsset(context.assets, "fonts/notonastaliqurdu-regular.ttf")
                    } catch (_: Throwable) {
                        Typeface.createFromAsset(context.assets, "notonastaliqurdu-regular.ttf")
                    }
                    UrduFontFamily = FontFamily(tf)
                } catch (_: Throwable) {}
            }

            // Load Arabic font (scheherazadenew-regular.ttf)
            try {
                val arFont = try {
                    FontFamily(Font("fonts/scheherazadenew-regular.ttf", context.assets))
                } catch (_: Throwable) {
                    FontFamily(Font("scheherazadenew-regular.ttf", context.assets))
                }
                ArabicFontFamily = arFont
                QuranFontFamily = arFont
                HadithArabicFontFamily = arFont
            } catch (_: Throwable) {
                try {
                    val tf = try {
                        Typeface.createFromAsset(context.assets, "fonts/scheherazadenew-regular.ttf")
                    } catch (_: Throwable) {
                        Typeface.createFromAsset(context.assets, "scheherazadenew-regular.ttf")
                    }
                    val arFont = FontFamily(tf)
                    ArabicFontFamily = arFont
                    QuranFontFamily = arFont
                    HadithArabicFontFamily = arFont
                } catch (_: Throwable) {}
            }

            isInitialized = true
        }
    }
}

/**
 * Character and script classification helpers.
 */
fun isArabicScriptChar(c: Char): Boolean {
    val code = c.code
    return (code in 0x0600..0x06FF) ||
           (code in 0x0750..0x077F) ||
           (code in 0x08A0..0x08FF) ||
           (code in 0xFB50..0xFDFF) ||
           (code in 0xFE70..0xFEFF)
}

fun isUrduUniqueChar(c: Char): Boolean {
    val code = c.code
    return c in "ٹڈڑںےۓچپژگھ۔ۂۃ؍؎؏ؒؓؑؐﷺء" ||
           (code in 0x06F0..0x06F9) || // Urdu/Persian digits
           c == 'ک' || // Urdu Kaf (0x06A9)
           c == 'ی' || // Urdu Yeh (0x06CC)
           c == 'ہ'    // Urdu He (0x06C1)
}

fun isLatinChar(c: Char): Boolean {
    val code = c.code
    return (code in 0x0041..0x005A) ||
           (code in 0x0061..0x007A) ||
           (code in 0x00C0..0x024F)
}

val URDU_COMMON_WORDS = setOf(
    "ہے", "ہیں", "تھا", "تھی", "تھے", "اور", "سے", "کا", "کی", "کے", "کو", "میں", "پر", "نے", "یہ", "وہ", "آپ",
    "کیا", "نہیں", "ہو", "ہوا", "ہوئی", "گے", "گی", "گیا", "گئے", "کر", "کرنے", "والا", "والے", "والی", "کہ", "بھی",
    "یا", "تک", "اب", "جب", "سب", "ہم", "تم", "مجھ", "تجھ", "اپنے", "اپنی", "اپنا", "اس", "ان", "جس", "جن", "کس", "کن",
    "بات", "کہا", "کہتے", "فرمایا", "روایت", "ترجمہ", "تفسیر", "حدیث", "سورت", "آیات", "پارہ", "رکوع", "مکی", "مدنی",
    "نماز", "روزہ", "زکوۃ", "حج", "دعا", "ذکر", "تسبیح", "فضیلت", "جلد", "صفحہ", "شروع", "ختم", "کل", "آج", "پڑھنا",
    "پڑھیں", "شائع", "ناشر", "مصنف", "مترجم", "مجموعہ", "شمار", "حصہ", "نیت", "اعمال", "دارومدار", "نیتوں", "سنن",
    "جامع", "صحیح", "ابواب", "مسند", "معجم", "فجر", "ظہر", "عصر", "مغرب", "عشاء", "اشراق", "چاشت", "تہجد", "منتخب",
    "کریں", "ڈاؤن", "لوڈ", "اہداف", "ذاتی", "ہدف", "تجویز", "کردہ", "تبدیلی", "ممکن", "پچھلے", "دن", "رپورٹ", "تاریخ",
    "شہر", "موجودہ", "رخ", "سمجھ", "گئے", "صفر", "مرضی", "اختیاری", "مثلاً", "صفحات", "شامل", "طریقۂ", "حسابِ",
    "وغیرہ", "چھپائیں", "دیگر", "اوقات", "ٹیسٹ", "نوٹیفکیشن", "حاصل", "موبائل", "آن", "باب", "کتاب", "تلاوت", "قاری",
    "لفظ", "بہ", "بامحاورہ", "سحری", "اختتام", "طلوع", "آفتاب", "نصف", "النہار", "غروب", "لمحات", "فکر", "کوئی",
    "براہ", "کرم", "معنی", "اسماء", "مقدسہ", "کاؤنٹر", "کیلنڈر", "محفوظ", "تلاش", "نما", "فاصلہ", "سیدھا", "رکھیں",
    "دستی", "گھمائیں", "آواز", "صرف", "ملاحظہ", "صلوات", "الأذكار", "والأعمال", "فون", "سطح", "ہموار", "سیدھی", "قبلہ", "توجہ", "درست",
    "نام", "ساتھ", "تعریف", "تعریفیں", "پروردگار", "جہانوں", "مالک", "جزا", "سزا", "بدلہ", "تیری", "عبادت",
    "مدد", "مانگتے", "دکھا", "ہمیں", "راستہ", "لوگوں", "انعام", "نہ", "غضب", "گمراہ", "گمراہوں", "ایک", "بےنیاز",
    "اولاد", "پیدا", "برابر", "پناہ", "صبح", "شر", "رات", "اندھیری", "چھا", "پھونکنے", "گرہوں", "حسد", "لوگ",
    "بادشاہ", "معبود", "وسوسہ", "پیچھے", "سینوں", "جنات", "انسان", "کافر", "کافرو", "پوجتے", "تمہارا", "میرا",
    "دین", "عطا", "کوثر", "قربانی", "دشمن", "بےنام", "فتح", "فوج", "مغفرت", "توبہ", "قبول", "فرمانے", "زمانہ",
    "قسم", "خسارے", "خسارہ", "سوائے", "ایمان", "لائے", "نیک", "نصیحت", "حق", "صبر", "بہت", "مہربان", "نہایت",
    "رحم", "بار", "جو", "لیے", "وہ", "کہہ", "دیجیے"
)

fun CharSequence.containsArabicScript(): Boolean {
    for (i in 0 until length) {
        if (isArabicScriptChar(this[i])) return true
    }
    return false
}

fun CharSequence.containsLatinScript(): Boolean {
    for (i in 0 until length) {
        if (isLatinChar(this[i])) return true
    }
    return false
}

/**
 * Accurately detects whether Arabic-script content is Urdu or Classical/Quranic Arabic.
 */
fun detectScriptLanguage(text: CharSequence): ScriptLanguage {
    if (!text.containsArabicScript()) return ScriptLanguage.ENGLISH

    // Check for explicit Urdu characters
    for (i in 0 until text.length) {
        if (isUrduUniqueChar(text[i])) return ScriptLanguage.URDU
    }

    // Check for explicit Urdu vocabulary
    val tokens = text.split(Regex("[\\s\\p{Punct}]+"))
    for (token in tokens) {
        if (token in URDU_COMMON_WORDS) return ScriptLanguage.URDU
    }

    return ScriptLanguage.ARABIC
}

/**
 * Builds an AnnotatedString that enforces:
 * 1. Arabic characters -> Scheherazade New
 * 2. Urdu characters -> Noto Nastaliq Urdu
 * 3. English/Latin characters -> Existing English font (AlDeenFontRegistry.EnglishFontFamily / unstyled)
 */
fun String.toScriptAnnotatedString(
    baseStyle: TextStyle = TextStyle.Default,
    targetScript: ScriptLanguage? = null
): AnnotatedString {
    if (this.isEmpty()) return AnnotatedString("")
    if (!containsArabicScript()) return AnnotatedString(this)

    val builder = AnnotatedString.Builder(this)
    val overallScript = targetScript
        ?: if (baseStyle.fontFamily == AlDeenFontRegistry.UrduFontFamily) {
            ScriptLanguage.URDU
        } else if (baseStyle.fontFamily == AlDeenFontRegistry.ArabicFontFamily ||
            baseStyle.fontFamily == AlDeenFontRegistry.QuranFontFamily ||
            baseStyle.fontFamily == AlDeenFontRegistry.HadithArabicFontFamily) {
            ScriptLanguage.ARABIC
        } else {
            detectScriptLanguage(this)
        }

    var runStart = -1
    for (i in indices) {
        val c = this[i]
        if (isArabicScriptChar(c)) {
            if (runStart == -1) {
                runStart = i
            }
        } else {
            if (runStart != -1) {
                // If it's a space or neutral punctuation between Arabic words, let's look ahead
                var j = i
                while (j < length && (this[j] == ' ' || this[j] == '\t' || this[j] == '،' || this[j] == '؛' || this[j] == '۔')) {
                    j++
                }
                if (j < length && isArabicScriptChar(this[j])) {
                    // Middle of Arabic/Urdu phrase, continue run
                } else {
                    val runSegment = substring(runStart, i)
                    val font = resolveFontFamilyForRun(runSegment, overallScript)
                    builder.addStyle(SpanStyle(fontFamily = font), runStart, i)
                    runStart = -1
                }
            }
        }
    }
    if (runStart != -1) {
        val runSegment = substring(runStart, length)
        val font = resolveFontFamilyForRun(runSegment, overallScript)
        builder.addStyle(SpanStyle(fontFamily = font), runStart, length)
    }

    return builder.toAnnotatedString()
}

private fun resolveFontFamilyForRun(runSegment: String, overallScript: ScriptLanguage): FontFamily {
    if (overallScript == ScriptLanguage.URDU) return AlDeenFontRegistry.UrduFontFamily
    if (overallScript == ScriptLanguage.ARABIC) return AlDeenFontRegistry.ArabicFontFamily

    // Mixed/unknown context - inspect the run itself
    for (c in runSegment) {
        if (isUrduUniqueChar(c)) return AlDeenFontRegistry.UrduFontFamily
    }
    val words = runSegment.split(Regex("[\\s\\p{Punct}]+"))
    for (w in words) {
        if (w in URDU_COMMON_WORDS) return AlDeenFontRegistry.UrduFontFamily
    }
    return AlDeenFontRegistry.ArabicFontFamily
}

/**
 * Al Deen Centralized Typography Definition
 * Uses dynamic getters so that custom fonts (notonastaliqurdu-regular.ttf &
 * scheherazadenew-regular.ttf) are immediately used across all screens.
 */
object AlDeenTypography {
    // English Text (Preserves system/Roboto styling)
    val EnglishHeadingLarge: TextStyle
        get() = TextStyle(
            fontFamily = AlDeenFontRegistry.EnglishFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            color = Color.Unspecified
        )

    val EnglishHeadingMedium: TextStyle
        get() = TextStyle(
            fontFamily = AlDeenFontRegistry.EnglishFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            lineHeight = 24.sp,
            color = Color.Unspecified
        )

    val EnglishBodyLarge: TextStyle
        get() = TextStyle(
            fontFamily = AlDeenFontRegistry.EnglishFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            color = Color.Unspecified
        )

    val EnglishBodyMedium: TextStyle
        get() = TextStyle(
            fontFamily = AlDeenFontRegistry.EnglishFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = Color.Unspecified
        )

    val EnglishLabelSmall: TextStyle
        get() = TextStyle(
            fontFamily = AlDeenFontRegistry.EnglishFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            color = Color.Unspecified
        )

    // Urdu Text (Noto Nastaliq Urdu: notonastaliqurdu-regular.ttf)
    val UrduHeading: TextStyle
        get() = TextStyle(
            fontFamily = AlDeenFontRegistry.UrduFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            lineHeight = 34.sp,
            color = Color.Unspecified
        )

    val UrduBody: TextStyle
        get() = TextStyle(
            fontFamily = AlDeenFontRegistry.UrduFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 30.sp,
            color = Color.Unspecified
        )

    val UrduCaption: TextStyle
        get() = TextStyle(
            fontFamily = AlDeenFontRegistry.UrduFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 22.sp,
            color = Color.Unspecified
        )

    // Arabic Text (Scheherazade New: scheherazadenew-regular.ttf)
    val ArabicHeading: TextStyle
        get() = TextStyle(
            fontFamily = AlDeenFontRegistry.ArabicFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            lineHeight = 38.sp,
            color = Color.Unspecified
        )

    val ArabicBody: TextStyle
        get() = TextStyle(
            fontFamily = AlDeenFontRegistry.ArabicFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 18.sp,
            lineHeight = 34.sp,
            color = Color.Unspecified
        )

    val ArabicCaption: TextStyle
        get() = TextStyle(
            fontFamily = AlDeenFontRegistry.ArabicFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            lineHeight = 24.sp,
            color = Color.Unspecified
        )

    // Quran Text (Scheherazade New)
    val QuranAyahText: TextStyle
        get() = TextStyle(
            fontFamily = AlDeenFontRegistry.QuranFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 24.sp,
            lineHeight = 46.sp,
            textAlign = TextAlign.Right,
            color = Color.Unspecified
        )

    // Hadith Arabic Text (Scheherazade New)
    val HadithArabicText: TextStyle
        get() = TextStyle(
            fontFamily = AlDeenFontRegistry.HadithArabicFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 20.sp,
            lineHeight = 40.sp,
            textAlign = TextAlign.Right,
            color = Color.Unspecified
        )

    val ArabicHadithSmall: TextStyle
        get() = TextStyle(
            fontFamily = AlDeenFontRegistry.HadithArabicFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 20.sp,
            color = Color.Unspecified
        )

    // Surah Glyph / Logo Placeholder Style
    val SurahGlyphText: TextStyle
        get() = TextStyle(
            fontFamily = AlDeenFontRegistry.SurahGlyphFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            lineHeight = 22.sp,
            color = Color.Unspecified
        )

    // Countdown Display
    val CountdownDisplay: TextStyle
        get() = TextStyle(
            fontFamily = AlDeenFontRegistry.EnglishFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            lineHeight = 36.sp,
            letterSpacing = 1.sp,
            color = Color.White
        )
}

val Typography = Typography(
    headlineLarge = AlDeenTypography.EnglishHeadingLarge,
    headlineMedium = AlDeenTypography.EnglishHeadingMedium,
    bodyLarge = AlDeenTypography.EnglishBodyLarge,
    bodyMedium = AlDeenTypography.EnglishBodyMedium,
    labelSmall = AlDeenTypography.EnglishLabelSmall
)
