package com.example.domain.model

data class QuranReciter(
    val id: String,
    val name: String,
    val englishName: String,
    val style: String = "Murattal",
    val bitrate: Int = 128
) {
    fun getAudioUrl(surahNumber: Int): String {
        return "https://cdn.islamic.network/quran/audio-surah/$bitrate/$id/$surahNumber.mp3"
    }
}

val SUPPORTED_RECITERS = listOf(
    QuranReciter(
        id = "ar.alafasy",
        name = "مشاري راشد العفاسي",
        englishName = "Mishary Rashid Alafasy",
        style = "Murattal"
    ),
    QuranReciter(
        id = "ar.abdulbasitmurattal",
        name = "عبد الباسط عبد الصمد",
        englishName = "Abdul Basit (Murattal)",
        style = "Murattal"
    ),
    QuranReciter(
        id = "ar.abdulbasitmujawwad",
        name = "عبد الباسط عبد الصمد (مجوّد)",
        englishName = "Abdul Basit (Mujawwad)",
        style = "Mujawwad"
    ),
    QuranReciter(
        id = "ar.saudalshuraim",
        name = "سعود الشريم",
        englishName = "Saud ash-Shuraim",
        style = "Haramain"
    ),
    QuranReciter(
        id = "ar.abdullahalmatrood",
        name = "عبد الله المطرود",
        englishName = "Abdullah al-Matrood",
        style = "Murattal"
    ),
    QuranReciter(
        id = "ar.abdullahawadaljuhani",
        name = "عبد الله عواد الجهني",
        englishName = "Abdullah Awad al-Juhani",
        style = "Haramain"
    ),
    QuranReciter(
        id = "ar.abdulazizazzahrani",
        name = "عبد العزيز الزهراني",
        englishName = "Abdul Aziz az-Zahrani",
        style = "Murattal"
    ),
    QuranReciter(
        id = "ar.abdulbariaththubaity",
        name = "عبد الباري الثبيتي",
        englishName = "Abdul Bari ath-Thubaity",
        style = "Madinah"
    ),
    QuranReciter(
        id = "ar.abdulkareemalhazmi",
        name = "عبد الكريم الحازمي",
        englishName = "Abdul Kareem al-Hazmi",
        style = "Murattal"
    ),
    QuranReciter(
        id = "ar.abdulwadoodhaneef",
        name = "عبد الودود حنيف",
        englishName = "Abdul Wadood Haneef",
        style = "Murattal"
    )
)
