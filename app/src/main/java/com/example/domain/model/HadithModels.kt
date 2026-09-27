package com.example.domain.model

data class HadithBook(
    val slug: String,
    val englishName: String,
    val urduName: String,
    val arabicName: String,
    val totalHadithCount: Int,
    val totalChaptersCount: Int
) {
    val name: String get() = englishName
}

data class HadithChapter(
    val id: Int,
    val bookSlug: String,
    val chapterNumber: Int,
    val arabicTitle: String,
    val urduTitle: String,
    val englishTitle: String
)

data class Hadith(
    val id: Long,
    val bookSlug: String,
    val bookName: String,
    val chapterNumber: Int,
    val chapterNameArabic: String,
    val chapterNameUrdu: String,
    val hadithNumber: String,
    val arabicText: String,
    val urduTranslation: String,
    val englishTranslation: String,
    val activeTranslation: String = "",
    val bookRefNumber: Int = -1,
    val hadithRefNumber: Int = -1,
    val arabicNumber: String = "",
    val indexInBook: Int = -1
)

enum class HadithSearchMode(val label: String, val hint: String) {
    BY_WORD("By Word", "Search narration e.g. intention, نیت, prayer, عمل..."),
    BY_NUMBER("By Hadith #", "Search Hadith number e.g. 1, 42, 100...")
}

enum class SearchLanguage(val code: String, val englishName: String, val nativeName: String) {
    URDU("ur", "Urdu", "اردو"),
    ENGLISH("en", "English", "English"),
    BENGALI("bn", "Bengali", "বাংলা"),
    FRENCH("fr", "French", "Français"),
    INDONESIAN("id", "Indonesian", "Bahasa Indonesia"),
    TURKISH("tr", "Turkish", "Türkçe"),
    RUSSIAN("ru", "Russian", "Русский"),
    TAMIL("ta", "Tamil", "தமிழ்"),
    HINDI("hi", "Hindi", "हिन्दी"),
    ARABIC("ar", "Arabic", "العربية"),
    SPANISH("es", "Spanish", "Español"),
    GERMAN("de", "German", "Deutsch"),
    PERSIAN("fa", "Persian", "فارسی"),
    CHINESE("zh", "Chinese", "中文"),
    MALAY("ms", "Malay", "Bahasa Melayu"),
    PASHTO("ps", "Pashto", "پښتو"),
    SINDHI("sd", "Sindhi", "سنڌي"),
    BOSNIAN("bs", "Bosnian", "Bosanski"),
    DUTCH("nl", "Dutch", "Nederlands"),
    ITALIAN("it", "Italian", "Italiano"),
    PORTUGUESE("pt", "Portuguese", "Português"),
    ALBANIAN("sq", "Albanian", "Shqip"),
    SWEDISH("sv", "Swedish", "Svenska"),
    SOMALI("so", "Somali", "Soomaali"),
    SWAHILI("sw", "Swahili", "Kiswahili"),
    HAUSA("ha", "Hausa", "Hausa"),
    JAPANESE("ja", "Japanese", "日本語"),
    KOREAN("ko", "Korean", "한국어"),
    UZBEK("uz", "Uzbek", "Oʻzbekcha"),
    KURDISH("ku", "Kurdish", "Kurdî"),
    AZERI("az", "Azeri", "Azərbaycan");

    companion object {
        val hadithLanguages = listOf(
            URDU, ENGLISH, BENGALI, FRENCH, INDONESIAN, TURKISH, RUSSIAN, TAMIL
        )
        val quranLanguages = listOf(
            URDU, ENGLISH, HINDI, BENGALI, INDONESIAN, TURKISH, FRENCH, SPANISH,
            RUSSIAN, GERMAN, PERSIAN, CHINESE, MALAY, TAMIL, PASHTO, SINDHI,
            BOSNIAN, DUTCH, ITALIAN, PORTUGUESE, ALBANIAN, SWEDISH, SOMALI,
            SWAHILI, HAUSA, JAPANESE, KOREAN, UZBEK, KURDISH, AZERI, ARABIC
        )
    }
}
