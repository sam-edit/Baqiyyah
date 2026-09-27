package com.example.domain.util

/**
 * Maps common Roman Urdu and English Islamic transliterations to their Arabic and Urdu keywords
 * so user queries like "namaz", "sabr", "noor", "iman", "jannah", "dua" immediately find
 * relevant verses across Arabic text and all translations.
 */
object IslamicSearchKeywords {
    private val ROMAN_SYNONYMS = mapOf(
        "namaz" to listOf("نماز", "صلاة", "صلاۃ", "prayer"),
        "salah" to listOf("صلاة", "صلاۃ", "نماز", "prayer"),
        "salat" to listOf("صلاة", "صلاۃ", "نماز", "prayer"),
        "salaat" to listOf("صلاة", "صلاۃ", "نماز", "prayer"),
        "roza" to listOf("روزہ", "صيام", "صوم", "fasting", "fast"),
        "roze" to listOf("روزہ", "روزوں", "صيام", "fasting"),
        "sawm" to listOf("صوم", "صيام", "روزہ", "fasting"),
        "saum" to listOf("صوم", "صيام", "روزہ", "fasting"),
        "siyyam" to listOf("صيام", "صوم", "fasting"),
        "sabr" to listOf("صبر", "صابر", "patience", "patient"),
        "sabar" to listOf("صبر", "صابر", "patience"),
        "zakat" to listOf("زكاة", "زکوۃ", "charity", "alms"),
        "zakah" to listOf("زكاة", "زکوۃ", "charity"),
        "zakaat" to listOf("زكاة", "زکوۃ", "charity"),
        "sadqa" to listOf("صدقہ", "صدقات", "charity", "alms"),
        "sadaqah" to listOf("صدقة", "صدقہ", "charity"),
        "hajj" to listOf("حج", "pilgrimage"),
        "umrah" to listOf("عمرة", "عمرہ", "pilgrimage"),
        "jannat" to listOf("جنت", "جنة", "جنات", "paradise", "garden"),
        "jannah" to listOf("جنة", "جنت", "paradise", "gardens"),
        "jahannum" to listOf("جہنم", "نار", "hell", "hellfire", "fire"),
        "jahannam" to listOf("جہنم", "نار", "hell", "hellfire"),
        "dozakh" to listOf("دوزخ", "جہنم", "hell"),
        "noor" to listOf("نور", "منور", "light"),
        "nur" to listOf("نور", "light"),
        "iman" to listOf("ایمان", "مومن", "مؤمن", "faith", "belief", "believe"),
        "eeman" to listOf("ایمان", "faith", "belief"),
        "ilm" to listOf("علم", "عالم", "knowledge"),
        "dua" to listOf("دعا", "دعاء", "supplication", "pray", "call upon"),
        "duaa" to listOf("دعاء", "دعا", "supplication"),
        "taqwa" to listOf("تقوی", "تقوى", "متقین", "righteous", "piety", "mindful"),
        "rizq" to listOf("رزق", "روزی", "provision", "sustenance"),
        "hidayat" to listOf("ہدایت", "ہدی", "ہدا", "هدى", "guidance", "guide"),
        "rehmat" to listOf("رحمت", "رحمة", "رحمان", "رحیم", "mercy", "merciful"),
        "rahmat" to listOf("رحمت", "رحمة", "mercy"),
        "maghfirat" to listOf("مغفرت", "بخش", "غفور", "forgiveness", "forgive"),
        "bakhshish" to listOf("بخش", "مغفرت", "forgiveness"),
        "tawbah" to listOf("توبہ", "توبة", "تواب", "repent", "repentance"),
        "tobah" to listOf("توبہ", "توبة", "repentance"),
        "shukr" to listOf("شکر", "شاکر", "شاكر", "grateful", "gratitude", "thanks"),
        "khauf" to listOf("خوف", "ڈر", "fear"),
        "momin" to listOf("مومن", "مؤمن", "مؤمنین", "مومنین", "believer", "believers"),
        "momineen" to listOf("مومنین", "مؤمنين", "believers"),
        "kafir" to listOf("کافر", "کفار", "disbeliever", "disbelievers"),
        "kuffar" to listOf("کفار", "کافرین", "disbelievers"),
        "munafiq" to listOf("منافق", "منافقین", "hypocrite", "hypocrites"),
        "shaitan" to listOf("شیطان", "ابلیس", "satan", "devil"),
        "iblis" to listOf("ابلیس", "devil", "satan"),
        "farishte" to listOf("فرشتے", "فرشتہ", "ملائکہ", "ملائكة", "angels", "angel"),
        "malaika" to listOf("ملائكة", "ملائکہ", "angels"),
        "jinn" to listOf("جن", "جنات", "jinn"),
        "insan" to listOf("انسان", "آدم", "mankind", "human", "man"),
        "qayamat" to listOf("قیامت", "ساعت", "hour", "resurrection", "day of judgement"),
        "qiyamah" to listOf("قيامة", "قیامت", "resurrection"),
        "akhirat" to listOf("آخرت", "آخرة", "hereafter"),
        "dunya" to listOf("دنیا", "دنيا", "world"),
        "duniya" to listOf("دنیا", "دنيا", "world"),
        "nabi" to listOf("نبی", "انبیاء", "prophet", "prophets"),
        "rasool" to listOf("رسول", "مرسل", "messenger", "messengers"),
        "allah" to listOf("اللہ", "الله", "allah", "god"),
        "khuda" to listOf("خدا", "اللہ", "الله", "god", "allah"),
        "rab" to listOf("رب", "پروردگار", "lord"),
        "rabb" to listOf("رب", "پروردگار", "lord"),
        "quran" to listOf("قرآن", "قران", "quran", "book"),
        "deen" to listOf("دین", "دين", "religion"),
        "haq" to listOf("حق", "truth"),
        "batil" to listOf("باطل", "falsehood"),
        "zulm" to listOf("ظلم", "ظالم", "injustice", "wrongdoing", "oppression"),
        "zalim" to listOf("ظالم", "ظالمین", "wrongdoers", "unjust"),
        "adal" to listOf("عدل", "انصاف", "justice"),
        "insaf" to listOf("انصاف", "عدل", "justice"),
        "fitnah" to listOf("فتنہ", "فتنة", "trial", "tribulation", "persecution"),
        "shirk" to listOf("شرک", "polytheism", "partners"),
        "tawhid" to listOf("توحید", "oneness", "one god"),
        "shifa" to listOf("شفا", "شفاء", "cure", "healing"),
        "astaghfirullah" to listOf("استغفر", "استغفار", "forgiveness"),
        "alhamdulillah" to listOf("الحمد", "شکر", "praise"),
        "bismillah" to listOf("بسم", "اللہ", "name of allah"),
        "subhanallah" to listOf("سبحان", "glory")
    )

    fun getExpandedQueries(query: String): List<String> {
        val trimmed = query.trim().lowercase()
        val list = mutableListOf(query.trim())
        val synonyms = ROMAN_SYNONYMS[trimmed]
        if (synonyms != null) {
            list.addAll(synonyms)
        }
        return list.distinct()
    }
}
