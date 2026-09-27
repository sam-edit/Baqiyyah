package com.example.domain.model

data class Surah(
    val number: Int,
    val englishName: String,
    val urduName: String,
    val arabicName: String,
    val englishTranslation: String,
    val revelationType: String, // "Meccan" or "Medinan"
    val numberOfAyahs: Int,
    val rukuCount: Int,
    val audioUrl: String? = null
)

data class QuranWord(
    val id: Long = 0,
    val position: Int = 0,
    val textUthmani: String = "",
    val urduTranslation: String = "",
    val englishTranslation: String = "",
    val activeTranslation: String = "",
    val isVerseEndMarker: Boolean = false
)

data class Ayah(
    val number: Int,
    val surahNumber: Int,
    val numberInSurah: Int,
    val textArabic: String,
    val textTranslation: String, // English translation
    val urduTranslation: String = "", // Exact Urdu translation
    val audioUrl: String? = null,
    val words: List<QuranWord> = emptyList() // Word-by-word data
)

data class JuzInfo(
    val juzNumber: Int,
    val arabicName: String,
    val urduName: String,
    val surahs: List<SurahInJuz>
)

data class SurahInJuz(
    val surahNumber: Int,
    val name: String,
    val startAyah: Int,
    val endAyah: Int
)

enum class QuranSearchMode(val label: String, val hint: String) {
    BY_WORD("By Word", "Search word e.g. patience, نور, mercy, رحمت..."),
    BY_AYAH("By Ayah", "Search Ayah e.g. 2:255, 36:1, Baqarah 255...")
}
