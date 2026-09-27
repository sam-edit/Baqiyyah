package com.example.domain.util

import java.util.regex.Pattern

/**
 * Normalizes Arabic and Urdu text for diacritic-insensitive, variant-insensitive search matching.
 * Handles Uthmani Quranic script (e.g. dagger alef, waqf symbols, waw-dagger ligatures in Salat/Zakat).
 * Does NOT modify original displayed religious text.
 */
object ArabicNormalizationUtils {
    // Unicode ranges for Arabic & Urdu diacritics, Tashkeel, Quranic waqf/recitation marks, and special formatting
    private val TASHKEEL_REGEX = Regex("[\u0610-\u061A\u064B-\u065F\u0670\u06D6-\u06ED\u08D4-\u08FF\u200B-\u200F\uFEFF\u0640\u06DF\u06E0\u06E2\u06E3\u06E5\u06E6\u06EB\u06EC]")
    private val UTHMANI_WAW_ALEF_REGEX = Regex("و[\u0670\u08F0-\u08FF]*")

    fun normalize(text: String): String {
        if (text.isBlank()) return ""
        var normalized = text

        // 1. Replace Uthmani specific ligature combinations before stripping marks:
        // Waw + dagger alef (as in Salat ٱلصَّلَوٰةَ, Zakat ٱلزَّكَوٰةَ, Hayat ٱلْحَيَوٰةَ, Riba ٱلرِّبَوٰا۟) -> Alef
        normalized = UTHMANI_WAW_ALEF_REGEX.replace(normalized, "ا")

        // 2. Remove Tashkeel (harakat: fatha, damma, kasra, sukun, shaddah, tanween, dagger alef, waqf signs, etc.)
        normalized = TASHKEEL_REGEX.replace(normalized, "")

        // 3. Normalize Alef variants: أ, إ, آ, ٱ, ٲ, ٳ, ٵ, ء -> ا
        normalized = normalized
            .replace('\u0622', '\u0627') // Madda
            .replace('\u0623', '\u0627') // Hamza above
            .replace('\u0625', '\u0627') // Hamza below
            .replace('\u0671', '\u0627') // Wasla
            .replace('\u0672', '\u0627')
            .replace('\u0673', '\u0627')
            .replace('\u0675', '\u0627')
            .replace('\u0621', '\u0627') // Standalone Hamza

        // 4. Normalize Kaf variants: ك (Arabic) -> ک (Urdu)
        normalized = normalized.replace('\u0643', '\u06A9')

        // 5. Normalize Yeh variants: ي (Arabic), ى (Alef Maksura), ے (Bari Ye), ۓ, ئ -> ی (Urdu Yeh)
        normalized = normalized
            .replace('\u064A', '\u06CC')
            .replace('\u0649', '\u06CC')
            .replace('\u06D2', '\u06CC')
            .replace('\u06D3', '\u06CC')
            .replace('\u0626', '\u06CC')

        // 6. Normalize Heh variants: ه (Arabic), ة (Teh Marbuta), ھ (Do Chashmi He), ۂ, ۃ -> ہ (Urdu Gol He)
        normalized = normalized
            .replace('\u0647', '\u06C1')
            .replace('\u0629', '\u06C1')
            .replace('\u06BE', '\u06C1')
            .replace('\u06C2', '\u06C1')
            .replace('\u06C3', '\u06C1')

        // 7. Normalize Waw variants: ؤ, ٶ, ٷ -> و
        normalized = normalized
            .replace('\u0624', '\u0648')
            .replace('\u0676', '\u0648')
            .replace('\u0677', '\u0648')

        // 8. Normalize Noon Ghunna: ں -> ن
        normalized = normalized.replace('\u06BA', '\u0646')

        // 9. Remove tatweel/kashida (ـ)
        normalized = normalized.replace("\u0640", "")

        // 10. Normalize multiple spaces & trim
        return normalized.replace(Regex("\\s+"), " ").trim().lowercase()
    }

    /**
     * Checks if target string contains search query safely in Arabic, Urdu, or English
     */
    fun containsNormalized(source: String, query: String): Boolean {
        if (query.isBlank()) return true
        if (source.isBlank()) return false
        val normSource = normalize(source)
        val normQuery = normalize(query)
        if (normSource.contains(normQuery, ignoreCase = true)) return true

        // Check with definite article 'ال' stripped from query if query has it (e.g. searching 'الصلاة' matches 'صلاة')
        if (normQuery.startsWith("ال") && normQuery.length > 3) {
            val strippedQ = normQuery.removePrefix("ال")
            if (normSource.contains(strippedQ, ignoreCase = true)) return true
        }

        // Also try with alefs stripped (for words like الرحمن vs الرحمان or داود vs داوود or سموات vs سماوات)
        if (normQuery.length >= 2) {
            val sourceNoAlef = normSource.replace("\u0627", "")
            val queryNoAlef = normQuery.replace("\u0627", "").removePrefix("ال")
            if (queryNoAlef.length >= 2 && sourceNoAlef.contains(queryNoAlef)) return true
        }

        return source.contains(query, ignoreCase = true)
    }
}
