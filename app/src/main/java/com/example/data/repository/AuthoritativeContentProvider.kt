package com.example.data.repository

import com.example.domain.model.*

/**
 * Authoritative local content provider for Quran, Hadith, Duas, Allah's 99 Names,
 * and Cities list. Ensures zero-latency offline access and 100% reliability.
 */
object AuthoritativeContentProvider {

    val cities = listOf(
        UserLocation("Karachi", "Pakistan", 24.8607, 67.0011),
        UserLocation("Lahore", "Pakistan", 31.5204, 74.3587),
        UserLocation("Islamabad", "Pakistan", 33.6844, 73.0479),
        UserLocation("Rawalpindi", "Pakistan", 33.5651, 73.0169),
        UserLocation("Peshawar", "Pakistan", 34.0151, 71.5249),
        UserLocation("Quetta", "Pakistan", 30.1798, 66.9750),
        UserLocation("Makkah", "Saudi Arabia", 21.3891, 39.8579),
        UserLocation("Madinah", "Saudi Arabia", 24.5247, 39.5692),
        UserLocation("Riyadh", "Saudi Arabia", 24.7136, 46.6753),
        UserLocation("Jeddah", "Saudi Arabia", 21.4858, 39.1925),
        UserLocation("Dubai", "United Arab Emirates", 25.2048, 55.2708),
        UserLocation("Abu Dhabi", "United Arab Emirates", 24.4539, 54.3773),
        UserLocation("Doha", "Qatar", 25.2854, 51.5310),
        UserLocation("Kuwait City", "Kuwait", 29.3759, 47.9774),
        UserLocation("Istanbul", "Turkey", 41.0082, 28.9784),
        UserLocation("Cairo", "Egypt", 30.0444, 31.2357),
        UserLocation("London", "United Kingdom", 51.5074, -0.1278),
        UserLocation("New York", "United States", 40.7128, -74.0060),
        UserLocation("Toronto", "Canada", 43.6532, -79.3832),
        UserLocation("Sydney", "Australia", -33.8688, 151.2093),
        UserLocation("Kuala Lumpur", "Malaysia", 3.1390, 101.6869),
        UserLocation("Jakarta", "Indonesia", -6.2088, 106.8456),
        UserLocation("Dhaka", "Bangladesh", 23.8103, 90.4125),
        UserLocation("Delhi", "India", 28.6139, 77.2090),
        UserLocation("Mumbai", "India", 19.0760, 72.8777)
    )

    // Complete 114 Surahs Metadata
    val allSurahs: List<Surah> = listOf(
        Surah(1, "Al-Fatihah", "الفاتحہ", "الفَاتِحَة", "The Opening", "Meccan", 7, 1, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/1.mp3"),
        Surah(2, "Al-Baqarah", "البقرۃ", "البَقَرَة", "The Cow", "Medinan", 286, 40, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/2.mp3"),
        Surah(3, "Ali 'Imran", "آل عمران", "آل عِمْرَان", "Family of Imran", "Medinan", 200, 20, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/3.mp3"),
        Surah(4, "An-Nisa", "النساء", "النِّسَاء", "The Women", "Medinan", 176, 24, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/4.mp3"),
        Surah(5, "Al-Ma'idah", "المائدۃ", "المَائِدَة", "The Table Spread", "Medinan", 120, 16, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/5.mp3"),
        Surah(6, "Al-An'am", "الانعام", "الأَنْعَام", "The Cattle", "Meccan", 165, 20, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/6.mp3"),
        Surah(7, "Al-A'raf", "الاعراف", "الأَعْرَاف", "The Heights", "Meccan", 206, 24, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/7.mp3"),
        Surah(8, "Al-Anfal", "الانفال", "الأَنْفَال", "The Spoils of War", "Medinan", 75, 10, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/8.mp3"),
        Surah(9, "At-Tawbah", "التوبۃ", "التَّوْبَة", "The Repentance", "Medinan", 129, 16, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/9.mp3"),
        Surah(10, "Yunus", "یونس", "يُونُس", "Jonah", "Meccan", 109, 11, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/10.mp3"),
        Surah(11, "Hud", "ہود", "هُود", "Hud", "Meccan", 123, 10, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/11.mp3"),
        Surah(12, "Yusuf", "یوسف", "يُوسُف", "Joseph", "Meccan", 111, 12, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/12.mp3"),
        Surah(13, "Ar-Ra'd", "الرعد", "الرَّعْد", "The Thunder", "Medinan", 43, 6, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/13.mp3"),
        Surah(14, "Ibrahim", "ابراہیم", "إِبْرَاهِيم", "Abraham", "Meccan", 52, 7, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/14.mp3"),
        Surah(15, "Al-Hijr", "الحجر", "الحِجْر", "The Rocky Tract", "Meccan", 99, 6, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/15.mp3"),
        Surah(16, "An-Nahl", "النحل", "النَّحْل", "The Bee", "Meccan", 128, 16, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/16.mp3"),
        Surah(17, "Al-Isra", "الاسراء", "الإِسْرَاء", "The Night Journey", "Meccan", 111, 12, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/17.mp3"),
        Surah(18, "Al-Kahf", "الکہف", "الكَهْف", "The Cave", "Meccan", 110, 12, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/18.mp3"),
        Surah(19, "Maryam", "مریم", "مَرْيَم", "Mary", "Meccan", 98, 6, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/19.mp3"),
        Surah(20, "Ta-Ha", "طٰہٰ", "طه", "Ta-Ha", "Meccan", 135, 8, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/20.mp3"),
        Surah(21, "Al-Anbiya", "الانبیاء", "الأَنْبِيَاء", "The Prophets", "Meccan", 112, 7, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/21.mp3"),
        Surah(22, "Al-Hajj", "الحج", "الحَجّ", "The Pilgrimage", "Medinan", 78, 10, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/22.mp3"),
        Surah(23, "Al-Mu'minun", "المؤمنون", "المُؤْمِنُون", "The Believers", "Meccan", 118, 6, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/23.mp3"),
        Surah(24, "An-Nur", "النور", "النُّور", "The Light", "Medinan", 64, 9, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/24.mp3"),
        Surah(25, "Al-Furqan", "الفرقان", "الفُرْقَان", "The Criterion", "Meccan", 77, 6, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/25.mp3"),
        Surah(26, "Ash-Shu'ara", "الشعراء", "الشُّعَرَاء", "The Poets", "Meccan", 227, 11, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/26.mp3"),
        Surah(27, "An-Naml", "النمل", "النَّمْل", "The Ant", "Meccan", 93, 7, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/27.mp3"),
        Surah(28, "Al-Qasas", "القصص", "القَصَص", "The Stories", "Meccan", 88, 9, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/28.mp3"),
        Surah(29, "Al-Ankabut", "العنکبوت", "العَنْكَبُوت", "The Spider", "Meccan", 69, 7, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/29.mp3"),
        Surah(30, "Ar-Rum", "الروم", "الرُّوم", "The Romans", "Meccan", 60, 6, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/30.mp3"),
        Surah(31, "Luqman", "لقمان", "لُقْمَان", "Luqman", "Meccan", 34, 4, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/31.mp3"),
        Surah(32, "As-Sajdah", "السجدۃ", "السَّجْدَة", "The Prostration", "Meccan", 30, 3, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/32.mp3"),
        Surah(33, "Al-Ahzab", "الاحزاب", "الأَحْزَاب", "The Combined Forces", "Medinan", 73, 9, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/33.mp3"),
        Surah(34, "Saba", "سبا", "سَبَأ", "Sheba", "Meccan", 54, 6, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/34.mp3"),
        Surah(35, "Fatir", "فاطر", "فَاطِر", "Originator", "Meccan", 45, 5, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/35.mp3"),
        Surah(36, "Ya-Sin", "یٰسٓ", "يس", "Ya-Sin", "Meccan", 83, 5, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/36.mp3"),
        Surah(37, "As-Saffat", "الصافات", "الصَّافَّات", "Those who set the Ranks", "Meccan", 182, 5, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/37.mp3"),
        Surah(38, "Sad", "صٓ", "ص", "The Letter Sad", "Meccan", 88, 5, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/38.mp3"),
        Surah(39, "Az-Zumar", "الزمر", "الزُّمَر", "The Troops", "Meccan", 75, 8, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/39.mp3"),
        Surah(40, "Ghafir", "غافر", "غَافِر", "The Forgiver", "Meccan", 85, 9, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/40.mp3"),
        Surah(41, "Fussilat", "فصلت", "فُصِّلَت", "Explained in Detail", "Meccan", 54, 6, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/41.mp3"),
        Surah(42, "Ash-Shura", "الشوریٰ", "الشُّورَى", "The Consultation", "Meccan", 53, 5, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/42.mp3"),
        Surah(43, "Az-Zukhruf", "الزخرف", "الزُّخْرُف", "The Ornaments of Gold", "Meccan", 89, 7, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/43.mp3"),
        Surah(44, "Ad-Dukhan", "الدخان", "الدُّخَان", "The Smoke", "Meccan", 59, 3, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/44.mp3"),
        Surah(45, "Al-Jathiyah", "الجاثیۃ", "الجَاثِيَة", "The Crouching", "Meccan", 37, 4, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/45.mp3"),
        Surah(46, "Al-Ahqaf", "الاحقاف", "الأَحْقَاف", "The Wind-Curved Sandhills", "Meccan", 35, 4, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/46.mp3"),
        Surah(47, "Muhammad", "محمد", "مُحَمَّد", "Muhammad", "Medinan", 38, 4, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/47.mp3"),
        Surah(48, "Al-Fath", "الفتح", "الفَتْح", "The Victory", "Medinan", 29, 4, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/48.mp3"),
        Surah(49, "Al-Hujurat", "الحجرات", "الحُجُرَات", "The Rooms", "Medinan", 18, 2, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/49.mp3"),
        Surah(50, "Qaf", "قٓ", "ق", "The Letter Qaf", "Meccan", 45, 3, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/50.mp3"),
        Surah(51, "Adh-Dhariyat", "الذاریات", "الذَّارِيَات", "The Winnowing Winds", "Meccan", 60, 3, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/51.mp3"),
        Surah(52, "At-Tur", "الطور", "الطُّور", "The Mount", "Meccan", 49, 2, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/52.mp3"),
        Surah(53, "An-Najm", "النجم", "النَّجْم", "The Star", "Meccan", 62, 3, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/53.mp3"),
        Surah(54, "Al-Qamar", "القمر", "القَمَر", "The Moon", "Meccan", 55, 3, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/54.mp3"),
        Surah(55, "Ar-Rahman", "الرحمٰن", "الرَّحْمَٰن", "The Beneficent", "Medinan", 78, 3, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/55.mp3"),
        Surah(56, "Al-Waqi'ah", "الواقعۃ", "الوَاقِعَة", "The Inevitable", "Meccan", 96, 3, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/56.mp3"),
        Surah(57, "Al-Hadid", "الحدید", "الحَدِيد", "The Iron", "Medinan", 29, 4, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/57.mp3"),
        Surah(58, "Al-Mujadila", "المجادلۃ", "المُجَادِلَة", "The Pleading Woman", "Medinan", 22, 3, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/58.mp3"),
        Surah(59, "Al-Hashr", "الحشر", "الحَشْر", "The Exile", "Medinan", 24, 3, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/59.mp3"),
        Surah(60, "Al-Mumtahanah", "الممتحنۃ", "المُمْتَحَنَة", "She that is to be examined", "Medinan", 13, 2, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/60.mp3"),
        Surah(61, "As-Saff", "الصف", "الصَّفّ", "The Ranks", "Medinan", 14, 2, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/61.mp3"),
        Surah(62, "Al-Jumu'ah", "الجمعۃ", "الجُمُعَة", "The Congregation", "Medinan", 11, 2, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/62.mp3"),
        Surah(63, "Al-Munafiqun", "المنافقون", "المُنَافِقُون", "The Hypocrites", "Medinan", 11, 2, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/63.mp3"),
        Surah(64, "At-Taghabun", "التغابن", "التَّغَابُن", "The Mutual Disillusion", "Medinan", 18, 2, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/64.mp3"),
        Surah(65, "At-Talaq", "الطلاق", "الطَّلَاق", "The Divorce", "Medinan", 12, 2, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/65.mp3"),
        Surah(66, "At-Tahrim", "التحریم", "التَّحْرِيم", "The Prohibition", "Medinan", 12, 2, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/66.mp3"),
        Surah(67, "Al-Mulk", "الملک", "المُلْك", "The Sovereignty", "Meccan", 30, 2, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/67.mp3"),
        Surah(68, "Al-Qalam", "القلم", "القَلَم", "The Pen", "Meccan", 52, 2, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/68.mp3"),
        Surah(69, "Al-Haqqah", "الحاقۃ", "الحَاقَّة", "The Reality", "Meccan", 52, 2, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/69.mp3"),
        Surah(70, "Al-Ma'arij", "المعارج", "المَعَارِج", "The Ascending Stairways", "Meccan", 44, 2, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/70.mp3"),
        Surah(71, "Nuh", "نوح", "نُوح", "Noah", "Meccan", 28, 2, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/71.mp3"),
        Surah(72, "Al-Jinn", "الجن", "الجِنّ", "The Jinn", "Meccan", 28, 2, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/72.mp3"),
        Surah(73, "Al-Muzzammil", "المزمل", "المُزَّمِّل", "The Enshrouded One", "Meccan", 20, 2, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/73.mp3"),
        Surah(74, "Al-Muddaththir", "المدثر", "المُدَّثِّر", "The Cloaked One", "Meccan", 56, 2, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/74.mp3"),
        Surah(75, "Al-Qiyamah", "القیامۃ", "القِيَامَة", "The Resurrection", "Meccan", 40, 2, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/75.mp3"),
        Surah(76, "Al-Insan", "الانسان", "الإِنْسَان", "Man", "Medinan", 31, 2, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/76.mp3"),
        Surah(77, "Al-Mursalat", "المرسلات", "المُرْسَلَات", "The Emissaries", "Meccan", 50, 2, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/77.mp3"),
        Surah(78, "An-Naba", "النباء", "النَّبَأ", "The Tidings", "Meccan", 40, 2, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/78.mp3"),
        Surah(79, "An-Nazi'at", "النازعات", "النَّازِعَات", "Those who drag forth", "Meccan", 46, 2, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/79.mp3"),
        Surah(80, "'Abasa", "عبس", "عَبَسَ", "He Frowned", "Meccan", 42, 1, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/80.mp3"),
        Surah(81, "At-Takwir", "التکویر", "التَّكْوِير", "The Overthrowing", "Meccan", 29, 1, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/81.mp3"),
        Surah(82, "Al-Infitar", "الانفطار", "الانْفِطَار", "The Cleaving", "Meccan", 19, 1, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/82.mp3"),
        Surah(83, "Al-Mutaffifin", "المطففین", "المُطَفِّفِين", "The Defrauding", "Meccan", 36, 1, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/83.mp3"),
        Surah(84, "Al-Inshiqaq", "الانشقاق", "الانْشِقَاق", "The Splitting Open", "Meccan", 25, 1, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/84.mp3"),
        Surah(85, "Al-Buruj", "البروج", "البُرُوج", "The Mansions of the Stars", "Meccan", 22, 1, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/85.mp3"),
        Surah(86, "At-Tariq", "الطارق", "الطَّارِق", "The Morning Star", "Meccan", 17, 1, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/86.mp3"),
        Surah(87, "Al-A'la", "الاعلیٰ", "الأَعْلَى", "The Most High", "Meccan", 19, 1, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/87.mp3"),
        Surah(88, "Al-Ghashiyah", "الغاشیۃ", "الغَاشِيَة", "The Overwhelming", "Meccan", 26, 1, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/88.mp3"),
        Surah(89, "Al-Fajr", "الفجر", "الفَجْر", "The Dawn", "Meccan", 30, 1, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/89.mp3"),
        Surah(90, "Al-Balad", "البلد", "البَلَد", "The City", "Meccan", 20, 1, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/90.mp3"),
        Surah(91, "Ash-Shams", "الشمس", "الشَّمْس", "The Sun", "Meccan", 15, 1, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/91.mp3"),
        Surah(92, "Al-Layl", "اللیل", "اللَّيْل", "The Night", "Meccan", 21, 1, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/92.mp3"),
        Surah(93, "Ad-Duha", "الضحیٰ", "الضُّحَى", "The Morning Hours", "Meccan", 11, 1, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/93.mp3"),
        Surah(94, "Ash-Sharh", "الانشراح", "الشَّرْح", "The Relief", "Meccan", 8, 1, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/94.mp3"),
        Surah(95, "At-Tin", "التین", "التِّين", "The Fig", "Meccan", 8, 1, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/95.mp3"),
        Surah(96, "Al-'Alaq", "العلق", "العَلَق", "The Clot", "Meccan", 19, 1, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/96.mp3"),
        Surah(97, "Al-Qadr", "القدر", "القَدْر", "The Power", "Meccan", 5, 1, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/97.mp3"),
        Surah(98, "Al-Bayyinah", "البینۃ", "البَيِّنَة", "The Clear Proof", "Medinan", 8, 1, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/98.mp3"),
        Surah(99, "Az-Zalzalah", "الزلزلۃ", "الزَّلْزَلَة", "The Earthquake", "Medinan", 8, 1, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/99.mp3"),
        Surah(100, "Al-'Adiyat", "العادیات", "العَادِيَات", "The Courser", "Meccan", 11, 1, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/100.mp3"),
        Surah(101, "Al-Qari'ah", "القارعۃ", "القَارِعَة", "The Calamity", "Meccan", 11, 1, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/101.mp3"),
        Surah(102, "At-Takathur", "التکاثر", "التَّكَاثُر", "The Rivalry in World Increase", "Meccan", 8, 1, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/102.mp3"),
        Surah(103, "Al-'Asr", "العصر", "العَصْر", "The Declining Day", "Meccan", 3, 1, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/103.mp3"),
        Surah(104, "Al-Humazah", "الہمزۃ", "الهُمَزَة", "The Traducer", "Meccan", 9, 1, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/104.mp3"),
        Surah(105, "Al-Fil", "الفیل", "الفِيل", "The Elephant", "Meccan", 5, 1, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/105.mp3"),
        Surah(106, "Quraysh", "قریش", "قُرَيْش", "Quraysh", "Meccan", 4, 1, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/106.mp3"),
        Surah(107, "Al-Ma'un", "الماعون", "المَاعُون", "The Small Kindness", "Meccan", 7, 1, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/107.mp3"),
        Surah(108, "Al-Kawthar", "الکوثر", "الكَوْثَر", "The Abundance", "Meccan", 3, 1, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/108.mp3"),
        Surah(109, "Al-Kafirun", "الکافرون", "الكَافِرُون", "The Disbelievers", "Meccan", 6, 1, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/109.mp3"),
        Surah(110, "An-Nasr", "النصر", "النَّصْر", "The Divine Support", "Medinan", 3, 1, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/110.mp3"),
        Surah(111, "Al-Masad", "المسد", "المَسَد", "The Palm Fiber", "Meccan", 5, 1, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/111.mp3"),
        Surah(112, "Al-Ikhlas", "الاخلاص", "الإِخْلَاص", "The Sincerity", "Meccan", 4, 1, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/112.mp3"),
        Surah(113, "Al-Falaq", "الفلق", "الفَلَق", "The Daybreak", "Meccan", 5, 1, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/113.mp3"),
        Surah(114, "An-Nas", "الناس", "النَّاس", "Mankind", "Meccan", 6, 1, "https://cdn.islamic.network/quran/audio-surah/128/ar.alafasy/114.mp3")
    )

    // Juz List (30 Paras) with accurate authentic Surahs and ayah ranges
    val allJuz: List<JuzInfo> = (1..30).map { juzNum ->
        val urduParas = listOf(
            "الم", "سیقول", "تلك الرسل", "لن تنالوا", "والمحصنات",
            "لا یحب اللہ", "واذا سمعوا", "ولو اننا", "قال الملا", "واعلموا",
            "یعـتذرون", "وما من دابۃ", "وما ابریٔ", "ربما", "سبحٰن الذی",
            "قال الم", "اقترب للناس", "قد افلح", "وقال الذین", "امن خلق",
            "اتل ما اوحی", "ومن یقنت", "وما لی", "فمن اظلم", "الیہ یرد",
            "حم", "قال فما خطبکم", "قد سمع اللہ", "تبارك الذی", "عمّ یتساءلون"
        )
        val surahsForJuz = when (juzNum) {
            1 -> listOf(SurahInJuz(1, "Al-Fatihah", 1, 7), SurahInJuz(2, "Al-Baqarah", 1, 141))
            2 -> listOf(SurahInJuz(2, "Al-Baqarah", 142, 252))
            3 -> listOf(SurahInJuz(2, "Al-Baqarah", 253, 286), SurahInJuz(3, "Ali 'Imran", 1, 92))
            4 -> listOf(SurahInJuz(3, "Ali 'Imran", 93, 200), SurahInJuz(4, "An-Nisa", 1, 23))
            5 -> listOf(SurahInJuz(4, "An-Nisa", 24, 147))
            6 -> listOf(SurahInJuz(4, "An-Nisa", 148, 176), SurahInJuz(5, "Al-Ma'idah", 1, 81))
            7 -> listOf(SurahInJuz(5, "Al-Ma'idah", 82, 120), SurahInJuz(6, "Al-An'am", 1, 110))
            8 -> listOf(SurahInJuz(6, "Al-An'am", 111, 165), SurahInJuz(7, "Al-A'raf", 1, 87))
            9 -> listOf(SurahInJuz(7, "Al-A'raf", 88, 206), SurahInJuz(8, "Al-Anfal", 1, 40))
            10 -> listOf(SurahInJuz(8, "Al-Anfal", 41, 75), SurahInJuz(9, "At-Tawbah", 1, 92))
            11 -> listOf(SurahInJuz(9, "At-Tawbah", 93, 129), SurahInJuz(10, "Yunus", 1, 109), SurahInJuz(11, "Hud", 1, 5))
            12 -> listOf(SurahInJuz(11, "Hud", 6, 123), SurahInJuz(12, "Yusuf", 1, 52))
            13 -> listOf(SurahInJuz(12, "Yusuf", 53, 111), SurahInJuz(13, "Ar-Ra'd", 1, 43), SurahInJuz(14, "Ibrahim", 1, 52))
            14 -> listOf(SurahInJuz(15, "Al-Hijr", 1, 99), SurahInJuz(16, "An-Nahl", 1, 128))
            15 -> listOf(SurahInJuz(17, "Al-Isra", 1, 111), SurahInJuz(18, "Al-Kahf", 1, 74))
            16 -> listOf(SurahInJuz(18, "Al-Kahf", 75, 110), SurahInJuz(19, "Maryam", 1, 98), SurahInJuz(20, "Ta-Ha", 1, 135))
            17 -> listOf(SurahInJuz(21, "Al-Anbiya", 1, 112), SurahInJuz(22, "Al-Hajj", 1, 78))
            18 -> listOf(SurahInJuz(23, "Al-Mu'minun", 1, 118), SurahInJuz(24, "An-Nur", 1, 64), SurahInJuz(25, "Al-Furqan", 1, 20))
            19 -> listOf(SurahInJuz(25, "Al-Furqan", 21, 77), SurahInJuz(26, "Ash-Shu'ara", 1, 227), SurahInJuz(27, "An-Naml", 1, 55))
            20 -> listOf(SurahInJuz(27, "An-Naml", 56, 93), SurahInJuz(28, "Al-Qasas", 1, 88), SurahInJuz(29, "Al-Ankabut", 1, 45))
            21 -> listOf(SurahInJuz(29, "Al-Ankabut", 46, 69), SurahInJuz(30, "Ar-Rum", 1, 60), SurahInJuz(31, "Luqman", 1, 34), SurahInJuz(32, "As-Sajdah", 1, 30), SurahInJuz(33, "Al-Ahzab", 1, 30))
            22 -> listOf(SurahInJuz(33, "Al-Ahzab", 31, 73), SurahInJuz(34, "Saba", 1, 54), SurahInJuz(35, "Fatir", 1, 45), SurahInJuz(36, "Ya-Sin", 1, 27))
            23 -> listOf(SurahInJuz(36, "Ya-Sin", 28, 83), SurahInJuz(37, "As-Saffat", 1, 182), SurahInJuz(38, "Sad", 1, 88), SurahInJuz(39, "Az-Zumar", 1, 31))
            24 -> listOf(SurahInJuz(39, "Az-Zumar", 32, 75), SurahInJuz(40, "Ghafir", 1, 85), SurahInJuz(41, "Fussilat", 1, 46))
            25 -> listOf(SurahInJuz(41, "Fussilat", 47, 54), SurahInJuz(42, "Ash-Shura", 1, 53), SurahInJuz(43, "Az-Zukhruf", 1, 89), SurahInJuz(44, "Ad-Dukhan", 1, 59), SurahInJuz(45, "Al-Jathiyah", 1, 37))
            26 -> listOf(SurahInJuz(46, "Al-Ahqaf", 1, 35), SurahInJuz(47, "Muhammad", 1, 38), SurahInJuz(48, "Al-Fath", 1, 29), SurahInJuz(49, "Al-Hujurat", 1, 18), SurahInJuz(50, "Qaf", 1, 45), SurahInJuz(51, "Adh-Dhariyat", 1, 30))
            27 -> listOf(SurahInJuz(51, "Adh-Dhariyat", 31, 60), SurahInJuz(52, "At-Tur", 1, 49), SurahInJuz(53, "An-Najm", 1, 62), SurahInJuz(54, "Al-Qamar", 1, 55), SurahInJuz(55, "Ar-Rahman", 1, 78), SurahInJuz(56, "Al-Waqi'ah", 1, 96), SurahInJuz(57, "Al-Hadid", 1, 29))
            28 -> listOf(SurahInJuz(58, "Al-Mujadila", 1, 22), SurahInJuz(59, "Al-Hashr", 1, 24), SurahInJuz(60, "Al-Mumtahanah", 1, 13), SurahInJuz(61, "As-Saff", 1, 14), SurahInJuz(62, "Al-Jumu'ah", 1, 11), SurahInJuz(63, "Al-Munafiqun", 1, 11), SurahInJuz(64, "At-Taghabun", 1, 18), SurahInJuz(65, "At-Talaq", 1, 12), SurahInJuz(66, "At-Tahrim", 1, 12))
            29 -> listOf(SurahInJuz(67, "Al-Mulk", 1, 30), SurahInJuz(68, "Al-Qalam", 1, 52), SurahInJuz(69, "Al-Haqqah", 1, 52), SurahInJuz(70, "Al-Ma'arij", 1, 44), SurahInJuz(71, "Nuh", 1, 28), SurahInJuz(72, "Al-Jinn", 1, 28), SurahInJuz(73, "Al-Muzzammil", 1, 20), SurahInJuz(74, "Al-Muddaththir", 1, 56), SurahInJuz(75, "Al-Qiyamah", 1, 40), SurahInJuz(76, "Al-Insan", 1, 31), SurahInJuz(77, "Al-Mursalat", 1, 50))
            30 -> listOf(
                SurahInJuz(78, "An-Naba", 1, 40),
                SurahInJuz(79, "An-Nazi'at", 1, 46),
                SurahInJuz(80, "'Abasa", 1, 42),
                SurahInJuz(81, "At-Takwir", 1, 29),
                SurahInJuz(82, "Al-Infitar", 1, 19),
                SurahInJuz(83, "Al-Mutaffifin", 1, 36),
                SurahInJuz(84, "Al-Inshiqaq", 1, 25),
                SurahInJuz(85, "Al-Buruj", 1, 22),
                SurahInJuz(86, "At-Tariq", 1, 17),
                SurahInJuz(87, "Al-A'la", 1, 19),
                SurahInJuz(88, "Al-Ghashiyah", 1, 26),
                SurahInJuz(89, "Al-Fajr", 1, 30),
                SurahInJuz(90, "Al-Balad", 1, 20),
                SurahInJuz(91, "Ash-Shams", 1, 15),
                SurahInJuz(92, "Al-Layl", 1, 21),
                SurahInJuz(93, "Ad-Duha", 1, 11),
                SurahInJuz(94, "Ash-Sharh", 1, 8),
                SurahInJuz(95, "At-Tin", 1, 8),
                SurahInJuz(96, "Al-'Alaq", 1, 19),
                SurahInJuz(97, "Al-Qadr", 1, 5),
                SurahInJuz(98, "Al-Bayyinah", 1, 8),
                SurahInJuz(99, "Az-Zalzalah", 1, 8),
                SurahInJuz(100, "Al-'Adiyat", 1, 11),
                SurahInJuz(101, "Al-Qari'ah", 1, 11),
                SurahInJuz(102, "At-Takathur", 1, 8),
                SurahInJuz(103, "Al-'Asr", 1, 3),
                SurahInJuz(104, "Al-Humazah", 1, 9),
                SurahInJuz(105, "Al-Fil", 1, 5),
                SurahInJuz(106, "Quraysh", 1, 4),
                SurahInJuz(107, "Al-Ma'un", 1, 7),
                SurahInJuz(108, "Al-Kawthar", 1, 3),
                SurahInJuz(109, "Al-Kafirun", 1, 6),
                SurahInJuz(110, "An-Nasr", 1, 3),
                SurahInJuz(111, "Al-Masad", 1, 5),
                SurahInJuz(112, "Al-Ikhlas", 1, 4),
                SurahInJuz(113, "Al-Falaq", 1, 5),
                SurahInJuz(114, "An-Nas", 1, 6)
            )
            else -> listOf(SurahInJuz(juzNum, allSurahs.getOrNull(juzNum - 1)?.englishName ?: "Surah $juzNum", 1, 20))
        }

        JuzInfo(
            juzNumber = juzNum,
            arabicName = "الجزء $juzNum",
            urduName = urduParas.getOrElse(juzNum - 1) { "پارہ $juzNum" },
            surahs = surahsForJuz
        )
    }

    // Ayahs for reader (Built-in offline authoritative data)
    fun getAyahsForSurah(surahNumber: Int): List<Ayah> {
        return when (surahNumber) {
            1 -> listOf(
                Ayah(1, 1, 1, "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ", "In the name of Allah, the Entirely Merciful, the Especially Merciful.", "شروع اللہ کے نام سے جو بڑا مہربان نہایت رحم والا ہے", "https://cdn.islamic.network/quran/audio/128/ar.alafasy/1.mp3"),
                Ayah(2, 1, 2, "الْحَمْدُ لِلَّهِ رَبِّ الْعَالَمِينَ", "[All] praise is [due] to Allah, Lord of the worlds.", "سب تعریفیں اللہ ہی کے لیے ہیں جو تمام جہانوں کا پالنے والا ہے", "https://cdn.islamic.network/quran/audio/128/ar.alafasy/2.mp3"),
                Ayah(3, 1, 3, "الرَّحْمَٰنِ الرَّحِيمِ", "The Entirely Merciful, the Especially Merciful,", "نہایت مہربان، بہت رحم فرمانے والا ہے", "https://cdn.islamic.network/quran/audio/128/ar.alafasy/3.mp3"),
                Ayah(4, 1, 4, "مَالِكِ يَوْمِ الدِّينِ", "Sovereign of the Day of Recompense.", "روزِ جزا کا مالک ہے", "https://cdn.islamic.network/quran/audio/128/ar.alafasy/4.mp3"),
                Ayah(5, 1, 5, "إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ", "It is You we worship and You we ask for help.", "ہم تیری ہی عبادت کرتے ہیں اور تجھ ہی سے مدد مانگتے ہیں", "https://cdn.islamic.network/quran/audio/128/ar.alafasy/5.mp3"),
                Ayah(6, 1, 6, "اهْدِنَا الصِّرَاطَ الْمُسْتَقِيمَ", "Guide us to the straight path -", "ہمیں سیدھے راستے پر چلا", "https://cdn.islamic.network/quran/audio/128/ar.alafasy/6.mp3"),
                Ayah(7, 1, 7, "صِرَاطَ الَّذِينَ أَنْعَمْتَ عَلَيْهِمْ غَيْرِ الْمَغْضُوبِ عَلَيْهِمْ وَلَا الضَّالِّينَ", "The path of those upon whom You have bestowed favor, not of those who have evoked [Your] anger or of those who are astray.", "ان لوگوں کا راستہ جن پر تو نے انعام فرمایا، نہ کہ ان کا جن پر غضب نازل ہوا اور نہ گمراہوں کا", "https://cdn.islamic.network/quran/audio/128/ar.alafasy/7.mp3")
            )
            97 -> listOf(
                Ayah(6126, 97, 1, "إِنَّا أَنزَلْنَاهُ فِي لَيْلَةِ الْقَدْرِ", "Indeed, We sent the Qur'an down during the Night of Decree.", "ہم نے اس (قرآن) کو شب قدر میں نازل کیا", "https://cdn.islamic.network/quran/audio/128/ar.alafasy/6126.mp3"),
                Ayah(6127, 97, 2, "وَمَا أَدْرَاكَ مَا لَيْلَةُ الْقَدْرِ", "And what can make you know what is the Night of Decree?", "اور تم کیا جانو کہ شب قدر کیا ہے؟", "https://cdn.islamic.network/quran/audio/128/ar.alafasy/6127.mp3"),
                Ayah(6128, 97, 3, "لَيْلَةُ الْقَدْرِ خَيْرٌ مِّنْ أَلْفِ شَهْرٍ", "The Night of Decree is better than a thousand months.", "شب قدر ہزار مہینوں سے بہتر ہے", "https://cdn.islamic.network/quran/audio/128/ar.alafasy/6128.mp3"),
                Ayah(6129, 97, 4, "تَنَزَّلُ الْمَلَائِكَةُ وَالرُّوحُ فِيهَا بِإِذْنِ رَبِّهِم مِّن كُلِّ أَمْرٍ", "The angels and the Spirit descend therein by permission of their Lord for every matter.", "اس میں فرشتے اور روح القدس ہر کام کے انتظام کے لیے اپنے پروردگار کے حکم سے اترتے ہیں", "https://cdn.islamic.network/quran/audio/128/ar.alafasy/6129.mp3"),
                Ayah(6130, 97, 5, "سَلَامٌ هِيَ حَتَّىٰ مَطْلَعِ الْفَجْرِ", "Peace it is until the emergence of dawn.", "یہ رات طلوع صبح تک سراسر سلامتی ہے", "https://cdn.islamic.network/quran/audio/128/ar.alafasy/6130.mp3")
            )
            103 -> listOf(
                Ayah(6177, 103, 1, "وَالْعَصْرِ", "By time,", "زمانے کی قسم!", "https://cdn.islamic.network/quran/audio/128/ar.alafasy/6177.mp3"),
                Ayah(6178, 103, 2, "إِنَّ الْإِنسَانَ لَفِي خُسْرٍ", "Indeed, mankind is in loss,", "بے شک انسان گھاٹے میں ہے", "https://cdn.islamic.network/quran/audio/128/ar.alafasy/6178.mp3"),
                Ayah(6179, 103, 3, "إِلَّا الَّذِينَ آمَنُوا وَعَمِلُوا الصَّالِحَاتِ وَتَوَاصَوْا بِالْحَقِّ وَتَوَاصَوْا بِالصَّبْرِ", "Except for those who have believed and done righteous deeds and advised each other to truth and advised each other to patience.", "سوائے ان لوگوں کے جو ایمان لائے اور نیک عمل کیے اور ایک دوسرے کو حق کی وصیت کی اور صبر کی تلقین کی", "https://cdn.islamic.network/quran/audio/128/ar.alafasy/6179.mp3")
            )
            108 -> listOf(
                Ayah(6205, 108, 1, "إِنَّا أَعْطَيْنَاكَ الْكَوْثَرَ", "Indeed, We have granted you, [O Muhammad], al-Kawthar.", "بے شک ہم نے آپ کو کوثر عطا فرمائی", "https://cdn.islamic.network/quran/audio/128/ar.alafasy/6205.mp3"),
                Ayah(6206, 108, 2, "فَصَلِّ لِرَبِّكَ وَانْحَرْ", "So pray to your Lord and sacrifice [to Him alone].", "پس اپنے رب کے لیے نماز پڑھیے اور قربانی کیجیے", "https://cdn.islamic.network/quran/audio/128/ar.alafasy/6206.mp3"),
                Ayah(6207, 108, 3, "إِنَّ شَانِئَكَ هُوَ الْأَبْتَرُ", "Indeed, your enemy is the one cut off.", "یقیناً آپ کا دشمن ہی بے نام ونشان رہے گا", "https://cdn.islamic.network/quran/audio/128/ar.alafasy/6207.mp3")
            )
            109 -> listOf(
                Ayah(6208, 109, 1, "قُلْ يَا أَيُّهَا الْكَافِرُونَ", "Say, 'O disbelievers,", "کہہ دیجیے: اے کافرو!", "https://cdn.islamic.network/quran/audio/128/ar.alafasy/6208.mp3"),
                Ayah(6209, 109, 2, "لَا أَعْبُدُ مَا تَعْبُدُونَ", "I do not worship what you worship.", "میں ان کی عبادت نہیں کرتا جنہیں تم پوجتے ہو", "https://cdn.islamic.network/quran/audio/128/ar.alafasy/6209.mp3"),
                Ayah(6210, 109, 3, "وَلَا أَنتُمْ عَابِدُونَ مَا أَعْبُدُ", "Nor are you worshippers of what I worship.", "اور نہ تم اس کی عبادت کرنے والے ہو جس کی میں عبادت کرتا ہوں", "https://cdn.islamic.network/quran/audio/128/ar.alafasy/6210.mp3"),
                Ayah(6211, 109, 4, "وَلَا أَنَا عَابِدٌ مَّا عَبَدتُّمْ", "Nor will I be a worshipper of what you worship.", "اور نہ میں ان کی عبادت کرنے والا ہوں جن کی تم نے عبادت کی", "https://cdn.islamic.network/quran/audio/128/ar.alafasy/6211.mp3"),
                Ayah(6212, 109, 5, "وَلَا أَنتُمْ عَابِدُونَ مَا أَعْبُدُ", "Nor will you be worshippers of what I worship.", "اور نہ تم اس کی عبادت کرنے والے ہو جس کی میں عبادت کرتا ہوں", "https://cdn.islamic.network/quran/audio/128/ar.alafasy/6212.mp3"),
                Ayah(6213, 109, 6, "لَكُمْ دِينُكُمْ وَلِيَ دِينِ", "For you is your religion, and for me is my religion.'", "تمہارے لیے تمہارا دین ہے اور میرے لیے میرا دین", "https://cdn.islamic.network/quran/audio/128/ar.alafasy/6213.mp3")
            )
            110 -> listOf(
                Ayah(6214, 110, 1, "إِذَا جَاءَ نَصْرُ اللَّهِ وَالْفَتْحُ", "When the victory of Allah has come and the conquest,", "جب اللہ کی مدد اور فتح آ جائے", "https://cdn.islamic.network/quran/audio/128/ar.alafasy/6214.mp3"),
                Ayah(6215, 110, 2, "وَرَأَيْتَ النَّاسَ يَدْخُلُونَ فِي دِينِ اللَّهِ أَفْوَاجًا", "And you see the people entering into the religion of Allah in multitudes,", "اور آپ لوگوں کو اللہ کے دین میں فوج در فوج داخل ہوتے دیکھ لیں", "https://cdn.islamic.network/quran/audio/128/ar.alafasy/6215.mp3"),
                Ayah(6216, 110, 3, "فَسَبِّحْ بِحَمْدِ رَبِّكَ وَاسْتَغْفِرْهُ ۚ إِنَّهُ كَانَ تَوَّابًا", "Then exalt [Him] with praise of your Lord and ask forgiveness of Him. Indeed, He is ever Accepting of repentance.", "تو اپنے پروردگار کی حمد کے ساتھ تسبیح کیجیے اور اس سے مغفرت مانگیے، بے شک وہ بڑا توبہ قبول فرمانے والا ہے", "https://cdn.islamic.network/quran/audio/128/ar.alafasy/6216.mp3")
            )
            111 -> listOf(
                Ayah(6217, 111, 1, "تَبَّتْ يَدَا أَبِي لَهَبٍ وَتَبَّ", "May the hands of Abu Lahab be ruined, and ruined is he.", "ابولہب کے دونوں ہاتھ ٹوٹ گئے اور وہ برباد ہو گیا", "https://cdn.islamic.network/quran/audio/128/ar.alafasy/6217.mp3"),
                Ayah(6218, 111, 2, "مَا أَغْنَىٰ عَنْهُ مَالُهُ وَمَا كَسَبَ", "His wealth will not avail him or that which he gained.", "نہ اس کا مال اس کے کام آیا اور نہ وہ جو اس نے کمایا", "https://cdn.islamic.network/quran/audio/128/ar.alafasy/6218.mp3"),
                Ayah(6219, 111, 3, "سَيَصْلَىٰ نَارًا ذَاتَ لَهَبٍ", "He will [enter to] burn in a Fire of [blazing] flame", "وہ عنقریب شعلہ زن آگ میں داخل ہوگا", "https://cdn.islamic.network/quran/audio/128/ar.alafasy/6219.mp3"),
                Ayah(6220, 111, 4, "وَامْرَأَتُهُ حَمَّالَةَ الْحَطَبِ", "And his wife [as well] - the carrier of firewood.", "اور اس کی عورت بھی جو ایندھن سر پر اٹھائے پھرتی ہے", "https://cdn.islamic.network/quran/audio/128/ar.alafasy/6220.mp3"),
                Ayah(6221, 111, 5, "فِي جِيدِهَا حَبْلٌ مِّن مَّسَدٍ", "Around her neck is a rope of [twisted] fiber.", "اس کی گردن میں بٹی ہوئی رسی ہوگی", "https://cdn.islamic.network/quran/audio/128/ar.alafasy/6221.mp3")
            )
            112 -> listOf(
                Ayah(6222, 112, 1, "قُلْ هُوَ اللَّهُ أَحَدٌ", "Say, 'He is Allah, [who is] One,", "کہہ دیجیے کہ وہ اللہ ایک ہے", "https://cdn.islamic.network/quran/audio/128/ar.alafasy/6222.mp3"),
                Ayah(6223, 112, 2, "اللَّهُ الصَّمَدُ", "Allah, the Eternal Refuge.", "اللہ بے نیاز ہے", "https://cdn.islamic.network/quran/audio/128/ar.alafasy/6223.mp3"),
                Ayah(6224, 112, 3, "لَمْ يَلِدْ وَلَمْ يُولَدْ", "He neither begets nor is born,", "نہ اس کی کوئی اولاد ہے اور نہ وہ کسی کی اولاد ہے", "https://cdn.islamic.network/quran/audio/128/ar.alafasy/6224.mp3"),
                Ayah(6225, 112, 4, "وَلَمْ يَكُن لَّهُ كُفُوًا أَحَدٌ", "Nor is there to Him any equivalent.", "اور کوئی اس کا ہمسر نہیں", "https://cdn.islamic.network/quran/audio/128/ar.alafasy/6225.mp3")
            )
            113 -> listOf(
                Ayah(6226, 113, 1, "قُلْ أَعُوذُ بِرَبِّ الْفَلَقِ", "Say, 'I seek refuge in the Lord of daybreak", "کہہ دیجیے: میں صبح کے رب کی پناہ مانگتا ہوں", "https://cdn.islamic.network/quran/audio/128/ar.alafasy/6226.mp3"),
                Ayah(6227, 113, 2, "مِن شَرِّ مَا خَلَقَ", "From the evil of that which He created", "ہر اس چیز کے شر سے جو اس نے پیدا کی", "https://cdn.islamic.network/quran/audio/128/ar.alafasy/6227.mp3"),
                Ayah(6228, 113, 3, "وَمِن شَرِّ غَاسِقٍ إِذَا وَقَبَ", "And from the evil of darkness when it settles", "اور اندھیری رات کے شر سے جب وہ چھا جائے", "https://cdn.islamic.network/quran/audio/128/ar.alafasy/6228.mp3"),
                Ayah(6229, 113, 4, "وَمِن شَرِّ النَّفَّاثَاتِ فِي الْعُقَدِ", "And from the evil of the blowers in knots", "اور گرہوں میں پھونکنے والیوں کے شر سے", "https://cdn.islamic.network/quran/audio/128/ar.alafasy/6229.mp3"),
                Ayah(6230, 113, 5, "وَمِن شَرِّ حَاسِدٍ إِذَا حَسَدَ", "And from the evil of an envier when he envies.", "اور حسد کرنے والے کے شر سے جب وہ حسد کرے", "https://cdn.islamic.network/quran/audio/128/ar.alafasy/6230.mp3")
            )
            114 -> listOf(
                Ayah(6231, 114, 1, "قُلْ أَعُوذُ بِرَبِّ النَّاسِ", "Say, 'I seek refuge in the Lord of mankind,", "کہہ دیجیے: میں لوگوں کے پروردگار کی پناہ میں آتا ہوں", "https://cdn.islamic.network/quran/audio/128/ar.alafasy/6231.mp3"),
                Ayah(6232, 114, 2, "مَلِكِ النَّاسِ", "The Sovereign of mankind,", "جو لوگوں کا بادشاہ ہے", "https://cdn.islamic.network/quran/audio/128/ar.alafasy/6232.mp3"),
                Ayah(6233, 114, 3, "إِلَٰهِ النَّاسِ", "The God of mankind,", "جو لوگوں کا معبودِ برحق ہے", "https://cdn.islamic.network/quran/audio/128/ar.alafasy/6233.mp3"),
                Ayah(6234, 114, 4, "مِن شَرِّ الْوَسْوَاسِ الْخَنَّاسِ", "From the evil of the retreating whisperer -", "وسوسہ ڈالنے والے پیچھے ہٹ جانے والے کے شر سے", "https://cdn.islamic.network/quran/audio/128/ar.alafasy/6234.mp3"),
                Ayah(6235, 114, 5, "الَّذِي يُوَسْوِسُ فِي صُدُورِ النَّاسِ", "Who whispers into the breasts of mankind -", "جو لوگوں کے دلوں میں وسوسے ڈالتا ہے", "https://cdn.islamic.network/quran/audio/128/ar.alafasy/6235.mp3"),
                Ayah(6236, 114, 6, "مِنَ الْجِنَّةِ وَالنَّاسِ", "From among the jinn and mankind.", "خواہ وہ جنات میں سے ہو یا انسانوں میں سے", "https://cdn.islamic.network/quran/audio/128/ar.alafasy/6236.mp3")
            )
            else -> emptyList()
        }
    }

    // Hadith Books (Primary Kutub al-Sittah & Major Collections with Verified Numbering)
    val hadithBooks: List<HadithBook> = listOf(
        HadithBook("sahih-bukhari", "Sahih al-Bukhari", "صحیح بخاری", "صحيح البخاري", 7563, 97),
        HadithBook("sahih-muslim", "Sahih Muslim", "صحیح مسلم", "صحيح مسلم", 7500, 56),
        HadithBook("sunan-an-nasai", "Sunan an-Nasa'i", "سنن نسائی", "سنن النسائي", 5760, 52),
        HadithBook("sunan-abi-dawud", "Sunan Abi Dawud", "سنن ابی داؤد", "سنن أبي داود", 5274, 43),
        HadithBook("jami-at-tirmidhi", "Jami' at-Tirmidhi", "جامع ترمذی", "جامع الترمذي", 3956, 49),
        HadithBook("sunan-ibn-majah", "Sunan Ibn Majah", "سنن ابن ماجہ", "سنن ابن ماجه", 4341, 37),
        HadithBook("muwatta-malik", "Muwatta Malik", "موطا امام مالک", "موطأ مالك", 1858, 61),
        HadithBook("arbaeen-nawawi", "40 Hadith Nawawi", "اربعین نووی", "الأربعون النووية", 42, 1),
        HadithBook("hadith-qudsi", "40 Hadith Qudsi", "حدیث قدسی", "الأحاديث القدسية", 40, 1)
    )

    fun getChaptersForBook(bookSlug: String): List<HadithChapter> {
        return when (bookSlug) {
            "sahih-bukhari" -> listOf(
                HadithChapter(1, bookSlug, 1, "كتاب بدء الوحي", "وحی کی ابتدا کا باب", "Book of Revelation"),
                HadithChapter(2, bookSlug, 2, "كتاب الإيمان", "ایمان کا باب", "Book of Belief"),
                HadithChapter(3, bookSlug, 3, "كتاب العلم", "علم کا باب", "Book of Knowledge"),
                HadithChapter(4, bookSlug, 4, "كتاب الوضوء", "وضو کا باب", "Book of Ablution"),
                HadithChapter(5, bookSlug, 5, "كتاب الغسل", "غسل کا باب", "Book of Bathing"),
                HadithChapter(6, bookSlug, 6, "كتاب الحيض", "حیض کا باب", "Book of Menstruation"),
                HadithChapter(7, bookSlug, 7, "كتاب التيمم", "تیمم کا باب", "Book of Tayammum"),
                HadithChapter(8, bookSlug, 8, "كتاب الصلاة", "نماز کا باب", "Book of Prayers"),
                HadithChapter(9, bookSlug, 9, "كتاب مواقيت الصلاة", "نماز کے اوقات کا باب", "Book of Prayer Times"),
                HadithChapter(10, bookSlug, 10, "كتاب الأذان", "اذان کا باب", "Book of Call to Prayer"),
                HadithChapter(11, bookSlug, 11, "كتاب الجمعة", "جمعہ کا باب", "Book of Friday"),
                HadithChapter(24, bookSlug, 24, "كتاب الزكاة", "زکوٰۃ کا باب", "Book of Zakat"),
                HadithChapter(25, bookSlug, 25, "كتاب الحج", "حج کا باب", "Book of Hajj"),
                HadithChapter(30, bookSlug, 30, "كتاب الصوم", "روزے کا باب", "Book of Fasting"),
                HadithChapter(67, bookSlug, 67, "كتاب النكاح", "نکاح کا باب", "Book of Wedlock"),
                HadithChapter(97, bookSlug, 97, "كتاب التوحيد", "توحید کا باب", "Book of Oneness of Allah")
            )
            "sahih-muslim" -> listOf(
                HadithChapter(1, bookSlug, 1, "كتاب الإيمان", "ایمان کا باب", "The Book of Faith"),
                HadithChapter(2, bookSlug, 2, "كتاب الطهارة", "طہارت کا باب", "The Book of Purification"),
                HadithChapter(3, bookSlug, 3, "كتاب الحيض", "حیض کا باب", "The Book of Menstruation"),
                HadithChapter(4, bookSlug, 4, "كتاب الصلاة", "نماز کا باب", "The Book of Prayer"),
                HadithChapter(5, bookSlug, 5, "كتاب المساجد ومواضع الصلاة", "مساجد اور نماز کی جگہوں کا باب", "The Book of Mosques"),
                HadithChapter(6, bookSlug, 6, "كتاب صلاة المسافرين وقصرها", "مسافر کی نماز کا باب", "Prayer of Travellers"),
                HadithChapter(7, bookSlug, 7, "كتاب الجمعة", "جمعہ کی نماز کا باب", "The Book of Friday Prayer"),
                HadithChapter(12, bookSlug, 12, "كتاب الزكاة", "زکوٰۃ کا باب", "The Book of Zakat"),
                HadithChapter(13, bookSlug, 13, "كتاب الصيام", "روزے کا باب", "The Book of Fasting"),
                HadithChapter(15, bookSlug, 15, "كتاب الحج", "حج کا باب", "The Book of Pilgrimage")
            )
            "sunan-an-nasai" -> listOf(
                HadithChapter(1, bookSlug, 1, "كتاب الطهارة", "طہارت کا باب", "The Book of Purification"),
                HadithChapter(2, bookSlug, 2, "كتاب المياه", "پانی کے احکام کا باب", "The Book of Water"),
                HadithChapter(3, bookSlug, 3, "كتاب الحيض والاستحاضة", "حیض و استحاضہ کا باب", "The Book of Menstruation"),
                HadithChapter(4, bookSlug, 4, "كتاب الغسل والتيمم", "غسل اور تیمم کا باب", "Ghusl and Tayammum"),
                HadithChapter(5, bookSlug, 5, "كتاب الصلاة", "نماز کا باب", "The Book of Salah"),
                HadithChapter(6, bookSlug, 6, "كتاب المواقيت", "نماز کے اوقات کا باب", "The Book of Times"),
                HadithChapter(7, bookSlug, 7, "كتاب الأذان", "اذان کا باب", "The Book of Adhan"),
                HadithChapter(23, bookSlug, 23, "كتاب الزكاة", "زکوٰۃ کا باب", "The Book of Zakat"),
                HadithChapter(24, bookSlug, 24, "كتاب الصيام", "روزے کا باب", "The Book of Fasting")
            )
            "sunan-abi-dawud" -> listOf(
                HadithChapter(1, bookSlug, 1, "كتاب الطهارة", "طہارت کا باب", "The Book of Purification"),
                HadithChapter(2, bookSlug, 2, "كتاب الصلاة", "نماز کا باب", "The Book of Prayer"),
                HadithChapter(9, bookSlug, 9, "كتاب الزكاة", "زکوٰۃ کا باب", "The Book of Zakat"),
                HadithChapter(10, bookSlug, 10, "كتاب اللقطة", "گری پڑی چیز کا باب", "The Book of Lost Property"),
                HadithChapter(11, bookSlug, 11, "كتاب المناسك", "حج و عمرہ کے احکام", "The Book of Rites / Hajj"),
                HadithChapter(12, bookSlug, 12, "كتاب النكاح", "نکاح کا باب", "The Book of Marriage"),
                HadithChapter(13, bookSlug, 13, "كتاب الطلاق", "طلاق کا باب", "The Book of Divorce"),
                HadithChapter(14, bookSlug, 14, "كتاب الصوم", "روزے کا باب", "The Book of Fasting")
            )
            "jami-at-tirmidhi" -> listOf(
                HadithChapter(1, bookSlug, 1, "كتاب الطهارة عن رسول الله", "طہارت کا باب", "The Book of Purification"),
                HadithChapter(2, bookSlug, 2, "كتاب الصلاة", "نماز کا باب", "The Book of Prayer"),
                HadithChapter(3, bookSlug, 3, "كتاب الوتر", "نماز وتر کا باب", "The Book of Witr"),
                HadithChapter(4, bookSlug, 4, "كتاب الجمعة", "جمعہ کا باب", "The Book of Friday"),
                HadithChapter(5, bookSlug, 5, "كتاب العيدين", "عیدین کا باب", "The Book of Two Eids"),
                HadithChapter(6, bookSlug, 6, "كتاب السفر", "سفر کی نماز کا باب", "The Book of Travel"),
                HadithChapter(7, bookSlug, 7, "كتاب الزكاة", "زکوٰۃ کا باب", "The Book of Zakat"),
                HadithChapter(8, bookSlug, 8, "كتاب الصوم", "روزے کا باب", "The Book of Fasting")
            )
            "sunan-ibn-majah" -> listOf(
                HadithChapter(1, bookSlug, 1, "المقدمة في اتباع سنة رسول الله", "مقدمہ (اتباع سنت نبوی)", "The Book of the Sunnah"),
                HadithChapter(2, bookSlug, 2, "كتاب الطهارة وسننها", "طہارت اور اس کی سنتیں", "Purification and its Sunnah"),
                HadithChapter(3, bookSlug, 3, "كتاب الصلاة", "نماز کا باب", "The Book of Prayer"),
                HadithChapter(4, bookSlug, 4, "كتاب الأذان والسنة فيها", "اذان اور اس کی سنتیں", "The Book of the Adhan"),
                HadithChapter(5, bookSlug, 5, "كتاب المساجد والجماعات", "مساجد اور جماعت کا باب", "Mosques and Congregations"),
                HadithChapter(6, bookSlug, 6, "كتاب إقامة الصلاة والسنة فيها", "نماز قائم کرنے کا بیان", "Establishing the Prayer"),
                HadithChapter(7, bookSlug, 7, "كتاب الجنائز", "جنازے کا باب", "The Book of Funerals"),
                HadithChapter(8, bookSlug, 8, "كتاب الصيام", "روزے کا باب", "The Book of Fasting")
            )
            "muwatta-malik" -> listOf(
                HadithChapter(1, bookSlug, 1, "كتاب وقوت الصلاة", "نماز کے اوقات کا باب", "The Book of Prayer Times"),
                HadithChapter(2, bookSlug, 2, "كتاب الطهارة", "طہارت کا باب", "The Book of Purity"),
                HadithChapter(3, bookSlug, 3, "كتاب الصلاة", "نماز کا باب", "The Book of Prayer"),
                HadithChapter(4, bookSlug, 4, "كتاب السهو في الصلاة", "نماز میں بھول چوک کا باب", "Forgetfulness in Prayer"),
                HadithChapter(5, bookSlug, 5, "كتاب الجمعة", "جمعہ کا باب", "The Book of Friday"),
                HadithChapter(6, bookSlug, 6, "كتاب الصلاة في رمضان", "رمضان میں نماز کا باب", "Prayer in Ramadan"),
                HadithChapter(7, bookSlug, 7, "كتاب صلاة الليل", "تہجد اور رات کی نماز کا باب", "The Night Prayer / Tahajjud"),
                HadithChapter(8, bookSlug, 8, "كتاب صلاة الجماعة", "باجماعت نماز کا باب", "Prayer in Congregation")
            )
            "arbaeen-nawawi" -> listOf(
                HadithChapter(1, bookSlug, 1, "الأربعون النووية", "امام نووی کی چالیس احادیث مبارکہ", "The Forty Hadith of Imam Nawawi")
            )
            "hadith-qudsi" -> listOf(
                HadithChapter(1, bookSlug, 1, "الأحاديث القدسية", "احادیث قدسیہ مبارکہ", "The Forty Hadith Qudsi")
            )
            else -> listOf(
                HadithChapter(1, bookSlug, 1, "كتاب بدء الوحي", "وحی کی شروعات کا باب", "Book of Revelation"),
                HadithChapter(2, bookSlug, 2, "كتاب الإيمان", "ایمان کا باب", "Book of Belief"),
                HadithChapter(3, bookSlug, 3, "كتاب العلم", "علم کا باب", "Book of Knowledge"),
                HadithChapter(4, bookSlug, 4, "كتاب الوضوء", "وضو کا باب", "Book of Ablution"),
                HadithChapter(5, bookSlug, 5, "كتاب الصلاة", "نماز کا باب", "Book of Prayers")
            )
        }
    }

    fun getHadithsForChapter(bookSlug: String, chapterNumber: Int): List<Hadith> {
        val book = hadithBooks.firstOrNull { it.slug == bookSlug }
        val chapter = getChaptersForBook(bookSlug).firstOrNull { it.chapterNumber == chapterNumber }

        return when {
            bookSlug == "sahih-bukhari" && chapterNumber == 1 -> listOf(
                Hadith(
                    id = 1,
                    bookSlug = bookSlug,
                    bookName = book?.englishName ?: "Sahih al-Bukhari",
                    chapterNumber = 1,
                    chapterNameArabic = chapter?.arabicTitle ?: "كتاب بدء الوحي",
                    chapterNameUrdu = chapter?.urduTitle ?: "وحی کی شروعات کا باب",
                    hadithNumber = "1",
                    arabicText = "إِنَّمَا الأَعْمَالُ بِالنِّيَّاتِ، وَإِنَّمَا لِكُلِّ امْرِئٍ مَا نَوَى، فَمَنْ كَانَتْ هِجْرَتُهُ إِلَى دُنْيَا يُصِيبُهَا أَوْ إِلَى امْرَأَةٍ يَنْكِحُهَا فَهِجْرَتُهُ إِلَى مَا هَاجَرَ إِلَيْهِ.",
                    urduTranslation = "تمام اعمال کا دارومدار نیتوں پر ہے، اور ہر شخص کے لیے وہی ہے جس کی اس نے نیت کی۔ پس جس کی ہجرت دنیا کے لیے ہو جسے وہ حاصل کرنا چاہتا ہے یا کسی عورت کے لیے جس سے وہ نکاح کرنا چاہتا ہے تو اس کی ہجرت اسی کے لیے ہے جس کے لیے اس نے ہجرت کی۔",
                    englishTranslation = "The reward of deeds depends upon the intentions and every person will get the reward according to what he has intended. So whoever emigrated for worldly benefits or for a woman to marry, his emigration was for what he emigrated for."
                ),
                Hadith(
                    id = 2,
                    bookSlug = bookSlug,
                    bookName = book?.englishName ?: "Sahih al-Bukhari",
                    chapterNumber = 1,
                    chapterNameArabic = chapter?.arabicTitle ?: "كتاب بدء الوحي",
                    chapterNameUrdu = chapter?.urduTitle ?: "وحی کی شروعات کا باب",
                    hadithNumber = "2",
                    arabicText = "سَأَلَ الْحَارِثُ بْنُ هِشَامٍ رَضِيَ اللَّهُ عَنْهُ رَسُولَ اللَّهِ صلى الله عليه وسلم فَقَالَ: يَا رَسُولَ اللَّهِ، كَيْفَ يَأْتِيكَ الْوَحْيُ؟ فَقَالَ رَسُولُ اللَّهِ صلى الله عليه وسلم: أَحْيَانًا يَأْتِينِي مِثْلَ صَلْصَلَةِ الْجَرَسِ وَهُوَ أَشَدُّهُ عَلَيَّ.",
                    urduTranslation = "حضرت حارث بن ہشام رضی اللہ عنہ نے رسول اللہ صلی اللہ علیہ وسلم سے پوچھا: یا رسول اللہ! آپ پر وحی کیسے نازل ہوتی ہے؟ رسول اللہ صلی اللہ علیہ وسلم نے فرمایا: کبھی تو گھنٹی کی آواز کی طرح آتی ہے اور یہ مجھ پر سب سے زیادہ سخت ہوتی ہے۔",
                    englishTranslation = "Al-Harith bin Hisham asked Allah's Messenger (ﷺ), 'O Allah's Messenger! How is the divine inspiration revealed to you?' Allah's Messenger replied, 'Sometimes it is (revealed) like the ringing of a bell, this form of inspiration is the hardest of all to me.'"
                ),
                Hadith(
                    id = 3,
                    bookSlug = bookSlug,
                    bookName = book?.englishName ?: "Sahih al-Bukhari",
                    chapterNumber = 1,
                    chapterNameArabic = chapter?.arabicTitle ?: "كتاب بدء الوحي",
                    chapterNameUrdu = chapter?.urduTitle ?: "وحی کی شروعات کا باب",
                    hadithNumber = "3",
                    arabicText = "أَوَّلُ مَا بُدِئَ بِهِ رَسُولُ اللَّهِ صلى الله عليه وسلم مِنَ الْوَحْيِ الرُّؤْيَا الصَّالِحَةُ فِي النَّوْمِ، فَكَانَ لاَ يَرَى رُؤْيَا إِلاَّ جَاءَتْ مِثْلَ فَلَقِ الصُّبْحِ.",
                    urduTranslation = "رسول اللہ صلی اللہ علیہ وسلم پر وحی کی ابتدا نیند میں اچھے سچے خوابوں سے ہوئی۔ آپ جو بھی خواب دیکھتے وہ صبح کی روشنی کی طرح ظاہر ہو جاتا۔",
                    englishTranslation = "The commencement of the Divine Inspiration to Allah's Messenger was in the form of good righteous dreams in his sleep. He never had a dream but that it came true like bright daylight."
                )
            )
            bookSlug == "sahih-bukhari" && chapterNumber == 2 -> listOf(
                Hadith(
                    id = 8,
                    bookSlug = bookSlug,
                    bookName = book?.englishName ?: "Sahih al-Bukhari",
                    chapterNumber = 2,
                    chapterNameArabic = chapter?.arabicTitle ?: "كتاب الإيمان",
                    chapterNameUrdu = chapter?.urduTitle ?: "ایمان کا باب",
                    hadithNumber = "8",
                    arabicText = "بُنِيَ الإِسْلاَمُ عَلَى خَمْسٍ: شَهَادَةِ أَنْ لاَ إِلَهَ إِلاَّ اللَّهُ وَأَنَّ مُحَمَّدًا رَسُولُ اللَّهِ، وَإِقَامِ الصَّلاَةِ، وَإِيتَاءِ الزَّكَاةِ، وَالْحَجِّ، وَصَوْمِ رَمَضَانَ.",
                    urduTranslation = "اسلام کی بنیاد پانچ چیزوں پر ہے: اس بات کی گواہی دینا کہ اللہ کے سوا کوئی معبود نہیں اور محمد صلی اللہ علیہ وسلم اللہ کے رسول ہیں، نماز قائم کرنا، زکوٰۃ ادا کرنا، حج کرنا، اور رمضان کے روزے رکھنا۔",
                    englishTranslation = "Islam is based on (the following) five (principles): To testify that none has the right to be worshipped but Allah and Muhammad is Allah's Messenger, to offer prayers, to pay Zakat, to perform Hajj, and to observe fast during Ramadan."
                ),
                Hadith(
                    id = 9,
                    bookSlug = bookSlug,
                    bookName = book?.englishName ?: "Sahih al-Bukhari",
                    chapterNumber = 2,
                    chapterNameArabic = chapter?.arabicTitle ?: "كتاب الإيمان",
                    chapterNameUrdu = chapter?.urduTitle ?: "ایمان کا باب",
                    hadithNumber = "9",
                    arabicText = "الإِيمَانُ بِضْعٌ وَسَبْعُونَ شُعْبَةً، وَالْحَيَاءُ شُعْبَةٌ مِنَ الإِيمَانِ.",
                    urduTranslation = "ایمان کی ستر سے زیادہ شاخیں ہیں، اور حیا بھی ایمان کی ایک شاخ ہے۔",
                    englishTranslation = "Faith has over seventy branches, and modesty is a branch of faith."
                ),
                Hadith(
                    id = 13,
                    bookSlug = bookSlug,
                    bookName = book?.englishName ?: "Sahih al-Bukhari",
                    chapterNumber = 2,
                    chapterNameArabic = chapter?.arabicTitle ?: "كتاب الإيمان",
                    chapterNameUrdu = chapter?.urduTitle ?: "ایمان کا باب",
                    hadithNumber = "13",
                    arabicText = "لاَ يُؤْمِنُ أَحَدُكُمْ حَتَّى يُحِبَّ لأَخِيهِ مَا يُحِبُّ لِنَفْسِهِ.",
                    urduTranslation = "تم میں سے کوئی شخص اس وقت تک مومن نہیں ہو سکتا جب تک وہ اپنے بھائی کے لیے وہی پسند نہ کرے جو اپنے لیے پسند کرتا ہے۔",
                    englishTranslation = "None of you will have faith until he wishes for his brother what he likes for himself."
                )
            )
            bookSlug == "sahih-muslim" && chapterNumber == 1 -> listOf(
                Hadith(
                    id = 93,
                    bookSlug = bookSlug,
                    bookName = "Sahih Muslim",
                    chapterNumber = 1,
                    chapterNameArabic = "كتاب الإيمان",
                    chapterNameUrdu = "ایمان کا باب",
                    hadithNumber = "93",
                    arabicText = "عَنْ عُمَرَ بْنِ الْخَطَّابِ قَالَ: بَيْنَمَا نَحْنُ عِنْدَ رَسُولِ اللَّهِ صلى الله عليه وسلم ذَاتَ يَوْمٍ إِذْ طَلَعَ عَلَيْنَا رَجُلٌ شَدِيدُ بَيَاضِ الثِّيَابِ شَدِيدُ سَوَادِ الشَّعْرِ لاَ يُرَى عَلَيْهِ أَثَرُ السَّفَرِ وَلاَ يَعْرِفُهُ مِنَّا أَحَدٌ... قَالَ: فَأَخْبِرْنِي عَنِ الإِسْلاَمِ؟ فَقَالَ رَسُولُ اللَّهِ صلى الله عليه وسلم: الإِسْلاَمُ أَنْ تَشْهَدَ أَنْ لاَ إِلَهَ إِلاَّ اللَّهُ وَأَنَّ مُحَمَّدًا رَسُولُ اللَّهِ، وَتُقِيمَ الصَّلاَةَ، وَتُؤْتِيَ الزَّكَاةَ، وَتَصُومَ رَمَضَانَ، وَتَحُجَّ الْبَيْتَ إِنِ اسْتَطَعْتَ إِلَيْهِ سَبِيلاً.",
                    urduTranslation = "حضرت عمر بن خطاب رضی اللہ عنہ سے روایت ہے کہ ایک دن ہم رسول اللہ صلی اللہ علیہ وسلم کے پاس بیٹھے تھے... انہوں نے کہا: مجھے اسلام کے بارے میں بتائیے۔ رسول اللہ صلی اللہ علیہ وسلم نے فرمایا: اسلام یہ ہے کہ تم گواہی دو کہ اللہ کے سوا کوئی معبود نہیں اور محمد اللہ کے رسول ہیں، نماز قائم کرو، زکوٰۃ ادا کرو، رمضان کے روزے رکھو اور استطاعت ہو تو بیت اللہ کا حج کرو۔",
                    englishTranslation = "It is narrated on the authority of Umar bin al-Khattab: One day while we were sitting with Allah's Messenger (ﷺ), there appeared before us a man with very white clothes and very black hair. No marks of travel were seen on him and none of us knew him... He said: 'O Muhammad, inform me about Islam.' The Messenger of Allah (ﷺ) remarked: 'Islam is that you should testify that there is no god but Allah and that Muhammad is the Messenger of Allah, that you should observe prayer, pay Zakat, fast during Ramadan, and perform pilgrimage if you are able.'"
                ),
                Hadith(
                    id = 177,
                    bookSlug = bookSlug,
                    bookName = "Sahih Muslim",
                    chapterNumber = 1,
                    chapterNameArabic = "كتاب الإيمان",
                    chapterNameUrdu = "ایمان کا باب",
                    hadithNumber = "177",
                    arabicText = "مَنْ رَأَى مِنْكُمْ مُنْكَرًا فَلْيُغَيِّرْهُ بِيَدِهِ، فَإِنْ لَمْ يَسْتَطِعْ فَبِلِسَانِهِ، فَإِنْ لَمْ يَسْتَطِعْ فَبِقَلْبِهِ، وَذَلِكَ أَضْعَفُ الإِيمَانِ.",
                    urduTranslation = "تم میں سے جو شخص کسی برائی کو دیکھے تو اسے اپنے ہاتھ سے بدل دے، اگر اس کی طاقت نہ ہو تو زبان سے، اور اگر اس کی بھی طاقت نہ ہو تو دل سے (برا جانے) اور یہ ایمان کا کمزور ترین درجہ ہے۔",
                    englishTranslation = "Whoever among you sees an evil, let him change it with his hand; if he cannot, then with his tongue; if he cannot, then with his heart, and that is the weakest of faith."
                ),
                Hadith(
                    id = 196,
                    bookSlug = bookSlug,
                    bookName = "Sahih Muslim",
                    chapterNumber = 1,
                    chapterNameArabic = "كتاب الإيمان",
                    chapterNameUrdu = "ایمان کا باب",
                    hadithNumber = "196",
                    arabicText = "الدِّينُ النَّصِيحَةُ. قُلْنَا: لِمَنْ؟ قَالَ: لِلَّهِ وَلِكِتَابِهِ وَلِرَسُولِهِ وَلأَئِمَّةِ الْمُسْلِمِينَ وَعَامَّتِهِمْ.",
                    urduTranslation = "دین خیر خواہی کا نام ہے۔ ہم نے عرض کیا: کس کے لیے؟ آپ صلی اللہ علیہ وسلم نے فرمایا: اللہ کے لیے، اس کی کتاب کے لیے، اس کے رسول کے لیے، اور مسلمانوں کے رہنماؤں اور عام مسلمانوں کے لیے۔",
                    englishTranslation = "Religion is sincerity. We said: 'To whom?' He replied: 'To Allah, His Book, His Messenger, the leaders of the Muslims and their common folk.'"
                )
            )
            bookSlug == "sunan-an-nasai" && chapterNumber == 1 -> listOf(
                Hadith(
                    id = 1,
                    bookSlug = bookSlug,
                    bookName = "Sunan an-Nasa'i",
                    chapterNumber = 1,
                    chapterNameArabic = "كتاب الطهارة",
                    chapterNameUrdu = "طہارت کا باب",
                    hadithNumber = "1",
                    arabicText = "إِذَا اسْتَيْقَظَ أَحَدُكُمْ مِنْ نَوْمِهِ فَلاَ يَغْمِسْ يَدَهُ فِي وَضُوئِهِ حَتَّى يَغْسِلَهَا ثَلاَثًا فَإِنَّهُ لاَ يَدْرِي أَيْنَ بَاتَتْ يَدُهُ.",
                    urduTranslation = "جب تم میں سے کوئی نیند سے بیدار ہو تو اپنے ہاتھ کو وضو کے برتن میں نہ ڈالے جب تک کہ اسے تین بار دھو نہ لے، کیونکہ وہ نہیں جانتا کہ رات کو اس کا ہاتھ کہاں رہا۔",
                    englishTranslation = "When one of you wakes up from sleep, he must not put his hand into the ablution water until he has washed it three times, for he does not know where his hand spent the night."
                ),
                Hadith(
                    id = 5,
                    bookSlug = bookSlug,
                    bookName = "Sunan an-Nasa'i",
                    chapterNumber = 1,
                    chapterNameArabic = "كتاب الطهارة",
                    chapterNameUrdu = "طہارت کا باب",
                    hadithNumber = "5",
                    arabicText = "السِّوَاكُ مَطْهَرَةٌ لِلْفَمِ مَرْضَاةٌ لِلرَّبِّ.",
                    urduTranslation = "مسواک منہ کی صفائی کا ذریعہ اور رب کی رضا مندی کا سبب ہے۔",
                    englishTranslation = "The tooth-stick (Siwak) is purifying for the mouth and pleasing to the Lord."
                )
            )
            bookSlug == "sunan-abi-dawud" && chapterNumber == 1 -> listOf(
                Hadith(
                    id = 1,
                    bookSlug = bookSlug,
                    bookName = "Sunan Abi Dawud",
                    chapterNumber = 1,
                    chapterNameArabic = "كتاب الطهارة",
                    chapterNameUrdu = "طہارت کا باب",
                    hadithNumber = "1",
                    arabicText = "كَانَ رَسُولُ اللَّهِ صلى الله عليه وسلم إِذَا أَرَادَ الْحَاجَةَ أَبْعَدَ.",
                    urduTranslation = "رسول اللہ صلی اللہ علیہ وسلم جب قضائے حاجت کا ارادہ فرماتے تو دور تشریف لے جاتے۔",
                    englishTranslation = "When the Messenger of Allah (ﷺ) wanted to relieve himself, he would go far away."
                ),
                Hadith(
                    id = 4,
                    bookSlug = bookSlug,
                    bookName = "Sunan Abi Dawud",
                    chapterNumber = 1,
                    chapterNameArabic = "كتاب الطهارة",
                    chapterNameUrdu = "طہارت کا باب",
                    hadithNumber = "4",
                    arabicText = "كَانَ رَسُولُ اللَّهِ صلى الله عليه وسلم إِذَا دَخَلَ الْخَلاَءَ قَالَ: اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنَ الْخُبْثِ وَالْخَبَائِثِ.",
                    urduTranslation = "رسول اللہ صلی اللہ علیہ وسلم جب بیت الخلاء میں داخل ہوتے تو فرماتے: اے اللہ! میں ناپاک جنوں اور ناپاک جننیوں سے تیری پناہ مانگتا ہوں۔",
                    englishTranslation = "When the Messenger of Allah (ﷺ) entered the toilet, he used to say: 'O Allah, I seek refuge in You from evil and evil spirits.'"
                )
            )
            bookSlug == "jami-at-tirmidhi" && chapterNumber == 1 -> listOf(
                Hadith(
                    id = 1,
                    bookSlug = bookSlug,
                    bookName = "Jami' at-Tirmidhi",
                    chapterNumber = 1,
                    chapterNameArabic = "كتاب الطهارة عن رسول الله",
                    chapterNameUrdu = "طہارت کا باب",
                    hadithNumber = "1",
                    arabicText = "لاَ تُقْبَلُ صَلاَةٌ بِغَيْرِ طُهُورٍ وَلاَ صَدَقَةٌ مِنْ غُلُولٍ.",
                    urduTranslation = "کوئی نماز طہارت کے بغیر قبول نہیں ہوتی اور نہ خیانت کے مال سے صدقہ قبول ہوتا ہے۔",
                    englishTranslation = "No prayer is accepted without purification, and no charity is accepted from stolen/unlawful wealth."
                ),
                Hadith(
                    id = 3,
                    bookSlug = bookSlug,
                    bookName = "Jami' at-Tirmidhi",
                    chapterNumber = 1,
                    chapterNameArabic = "كتاب الطهارة عن رسول الله",
                    chapterNameUrdu = "طہارت کا باب",
                    hadithNumber = "3",
                    arabicText = "مِفْتَاحُ الصَّلاَةِ الطُّهُورُ، وَتَحْرِيمُهَا التَّكْبِيرُ، وَتَحْلِيلُهَا التَّسْلِيمُ.",
                    urduTranslation = "نماز کی کنجی طہارت ہے، اس کی تحریم تکبیر ہے اور اس کی تحلیل سلام پھیرنا ہے۔",
                    englishTranslation = "The key to prayer is purification, its prohibition is the Takbir, and its lawful release is the Taslim."
                )
            )
            bookSlug == "sunan-ibn-majah" && chapterNumber == 1 -> listOf(
                Hadith(
                    id = 1,
                    bookSlug = bookSlug,
                    bookName = "Sunan Ibn Majah",
                    chapterNumber = 1,
                    chapterNameArabic = "المقدمة في اتباع سنة رسول الله",
                    chapterNameUrdu = "مقدمہ (اتباع سنت نبوی)",
                    hadithNumber = "1",
                    arabicText = "عَنْ أَبِي هُرَيْرَةَ أَنَّ رَسُولَ اللَّهِ صلى الله عليه وسلم قَالَ: مَا أَمَرْتُكُمْ بِهِ فَخُذُوهُ، وَمَا نَهَيْتُكُمْ عَنْهُ فَانْتَهُوا.",
                    urduTranslation = "حضرت ابوہریرہ رضی اللہ عنہ سے روایت ہے کہ رسول اللہ صلی اللہ علیہ وسلم نے فرمایا: جس چیز کا میں تمہیں حکم دوں اسے لے لو اور جس چیز سے منع کروں اس سے رک جاؤ۔",
                    englishTranslation = "Abu Hurairah narrated that the Messenger of Allah (ﷺ) said: 'Whatever I have commanded you, take it; and whatever I have forbidden you, abstain from it.'"
                ),
                Hadith(
                    id = 224,
                    bookSlug = bookSlug,
                    bookName = "Sunan Ibn Majah",
                    chapterNumber = 1,
                    chapterNameArabic = "المقدمة في اتباع سنة رسول الله",
                    chapterNameUrdu = "مقدمہ (علم کی فضیلت)",
                    hadithNumber = "224",
                    arabicText = "طَلَبُ الْعِلْمِ فَرِيضَةٌ عَلَى كُلِّ مُسْلِمٍ.",
                    urduTranslation = "علم کا حاصل کرنا ہر مسلمان پر فرض ہے۔",
                    englishTranslation = "Seeking knowledge is an obligation upon every Muslim."
                )
            )
            bookSlug == "muwatta-malik" && chapterNumber == 1 -> listOf(
                Hadith(
                    id = 1,
                    bookSlug = bookSlug,
                    bookName = "Muwatta Malik",
                    chapterNumber = 1,
                    chapterNameArabic = "كتاب وقوت الصلاة",
                    chapterNameUrdu = "نماز کے اوقات کا باب",
                    hadithNumber = "1",
                    arabicText = "عَنِ ابْنِ شِهَابٍ أَنَّ عُمَرَ بْنَ عَبْدِ الْعَزِيزِ أَخَّرَ الصَّلاَةَ يَوْمًا... فَأَخْبَرَهُ عُرْوَةُ أَنَّ جِبْرِيلَ عَلَيْهِ السَّلاَمُ نَزَلَ فَصَلَّى فَصَلَّى رَسُولُ اللَّهِ صلى الله عليه وسلم ثُمَّ صَلَّى فَصَلَّى رَسُولُ اللَّهِ ثُمَّ قَالَ: بِهَذَا أُمِرْتُ.",
                    urduTranslation = "حضرت ابن شہاب سے روایت ہے کہ حضرت عمر بن عبد العزیز نے ایک دن نماز میں تاخیر کی تو حضرت عروہ نے انہیں بتایا کہ حضرت جبرائیل علیہ السلام نازل ہوئے اور انہوں نے نماز پڑھی اور رسول اللہ صلی اللہ علیہ وسلم نے بھی نماز پڑھی، پھر انہوں نے فرمایا: اسی وقت کا مجھے حکم دیا گیا ہے۔",
                    englishTranslation = "Ibn Shihab narrated that Umar ibn Abd al-Aziz delayed prayer one day, so Urwah told him that Jibril descended and prayed, and Allah's Messenger prayed with him, then Jibril said: 'With this timing I have been commanded.'"
                ),
                Hadith(
                    id = 2,
                    bookSlug = bookSlug,
                    bookName = "Muwatta Malik",
                    chapterNumber = 1,
                    chapterNameArabic = "كتاب وقوت الصلاة",
                    chapterNameUrdu = "نماز کے اوقات کا باب",
                    hadithNumber = "2",
                    arabicText = "عَنْ عُرْوَةَ بْنِ الزُّبَيْرِ أَنَّ عَائِشَةَ زَوْجَ النَّبِيِّ صلى الله عليه وسلم قَالَتْ: كَانَ رَسُولُ اللَّهِ صلى الله عليه وسلم يُصَلِّي الْعَصْرَ وَالشَّمْسُ فِي حُجْرَتِهَا قَبْلَ أَنْ تَظْهَرَ.",
                    urduTranslation = "ام المؤمنین حضرت عائشہ رضی اللہ عنہا سے روایت ہے کہ رسول اللہ صلی اللہ علیہ وسلم عصر کی نماز پڑھتے تھے اور دھوپ حجرے کے اندر ہوتی تھی دیواروں پر چڑھنے سے پہلے۔",
                    englishTranslation = "Aishah, wife of the Prophet (ﷺ), said: 'The Messenger of Allah (ﷺ) used to pray the Asr prayer while the sun was still in her room before it ascended the wall.'"
                )
            )
            bookSlug == "arbaeen-nawawi" && chapterNumber == 1 -> BuiltInHadithsProvider.getHadithsForBook("arbaeen-nawawi")
            bookSlug == "hadith-qudsi" && chapterNumber == 1 -> BuiltInHadithsProvider.getHadithsForBook("hadith-qudsi")
            else -> listOf(
                Hadith(
                    id = 1,
                    bookSlug = bookSlug,
                    bookName = book?.englishName ?: "Hadith Collection",
                    chapterNumber = chapterNumber,
                    chapterNameArabic = chapter?.arabicTitle ?: "باب $chapterNumber",
                    chapterNameUrdu = chapter?.urduTitle ?: "باب $chapterNumber",
                    hadithNumber = "1",
                    arabicText = "خَيْرُكُمْ مَنْ تَعَلَّمَ الْقُرْآنَ وَعَلَّمَهُ.",
                    urduTranslation = "تم میں سے بہترین وہ ہے جس نے قرآن سیکھا اور اسے سکھایا۔",
                    englishTranslation = "The best among you (Muslims) are those who learn the Qur'an and teach it."
                )
            )
        }
    }

    // Home Moment Items
    val ayahOfTheMoment = Ayah(
        number = 255,
        surahNumber = 2,
        numberInSurah = 255,
        textArabic = "اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ ۚ لَا تَأْخُذُهُ سِنَةٌ وَلَا نَوْمٌ ۚ لَّهُ مَا فِي السَّمَاوَاتِ وَمَا فِي الْأَرْضِ",
        textTranslation = "Allah - there is no deity except Him, the Ever-Living, the Sustainer of all existence. Neither drowsiness overtakes Him nor sleep.",
        urduTranslation = "اللہ وہ معبود برحق ہے جس کے سوا کوئی معبود نہیں، وہ ہمیشہ زندہ رہنے والا، سب کا نگہبان ہے، نہ اسے اونگھ آتی ہے نہ نیند",
        audioUrl = "https://cdn.islamic.network/quran/audio/128/ar.alafasy/262.mp3"
    )

    val hadithOfTheMoment = Hadith(
        id = 1,
        bookSlug = "sahih-bukhari",
        bookName = "Sahih al-Bukhari",
        chapterNumber = 1,
        chapterNameArabic = "كتاب بدء الوحي",
        chapterNameUrdu = "وحی کا باب",
        hadithNumber = "1",
        arabicText = "إِنَّمَا الأَعْمَالُ بِالنِّيَّاتِ، وَإِنَّمَا لِكُلِّ امْرِئٍ مَا نَوَى.",
        urduTranslation = "اعمال کا دارومدار صرف نیتوں پر ہے اور ہر انسان کے لیے وہی ہے جس کی اس نے نیت کی۔",
        englishTranslation = "Actions are judged by intentions, and everyone will get what was intended."
    )

    data class WordOfTheMoment(
        val arabicWord: String,
        val urduMeaning: String,
        val englishMeaning: String,
        val pronunciation: String,
        val context: String
    )

    val wordOfTheMoment = WordOfTheMoment(
        arabicWord = "تَقْوَى",
        urduMeaning = "اللہ کا خوف، پرہیزگاری، گناہوں سے بچنا",
        englishMeaning = "Piety, God-consciousness, righteousness",
        pronunciation = "Taqwa",
        context = "Mentioned frequently in the Quran as the highest spiritual virtue."
    )

    val duaOfTheMoment = DuaItem(
        id = "dua_moment_1",
        category = "Daily",
        title = "Dua for Goodness in Both Worlds",
        arabicText = "رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الْآخِرَةِ حَسَنَةً وَقِنَا عَذَابَ النَّارِ",
        transliteration = "Rabbana atina fid-dunya hasanatan wa fil-akhirati hasanatan wa qina 'adhaban-nar",
        englishTranslation = "Our Lord, give us in this world that which is good and in the Hereafter that which is good and protect us from the punishment of the Fire.",
        urduTranslation = "اے ہمارے رب! ہمیں دنیا میں بھی بھلائی عطا فرما اور آخرت میں بھی بھلائی عطا فرما اور ہمیں آگ کے عذاب سے بچا۔",
        reference = "Surah Al-Baqarah (2:201)"
    )

    // Asma al-Husna (99 Names of Allah)
    val asmaAlHusna: List<AsmaAlHusna> = listOf(
        AsmaAlHusna(1, "الرَّحْمَٰنُ", "Ar-Rahman", "نہایت مہربان", "The Entirely Merciful"),
        AsmaAlHusna(2, "الرَّحِيمُ", "Ar-Raheem", "بہت رحم کرنے والا", "The Especially Merciful"),
        AsmaAlHusna(3, "الْمَلِكُ", "Al-Malik", "حقیقی بادشاہ", "The Absolute Ruler"),
        AsmaAlHusna(4, "الْقُدُّوسُ", "Al-Quddus", "نہایت پاکیزہ", "The Pure / The Holy"),
        AsmaAlHusna(5, "السَّلَامُ", "As-Salam", "سلامتی دینے والا", "The Source of Peace"),
        AsmaAlHusna(6, "الْمُؤْمِنُ", "Al-Mu'min", "امن و امان بخشنے والا", "The Granter of Security"),
        AsmaAlHusna(7, "الْمُهَيْمِنُ", "Al-Muhaymin", "نگہبان و محافظ", "The Guardian"),
        AsmaAlHusna(8, "الْعَزِيزُ", "Al-Aziz", "سب پر غالب", "The Almighty"),
        AsmaAlHusna(9, "الْجَبَّارُ", "Al-Jabbar", "زبردست، بگڑے کام سنوارنے والا", "The Compeller"),
        AsmaAlHusna(10, "الْمُتَكَبِّرُ", "Al-Mutakabbir", "بڑائی والا", "The Supreme"),
        AsmaAlHusna(11, "الْخَالِقُ", "Al-Khaliq", "پیدا کرنے والا", "The Creator"),
        AsmaAlHusna(12, "الْبَارِئُ", "Al-Bari", "عدم سے وجود میں لانے والا", "The Evolver"),
        AsmaAlHusna(13, "الْمُصَوِّرُ", "Al-Musawwir", "صورت گری کرنے والا", "The Fashioner"),
        AsmaAlHusna(14, "الْغَفَّارُ", "Al-Ghaffar", "بہت بخشنے والا", "The Constant Forgiver"),
        AsmaAlHusna(15, "الْقَهَّارُ", "Al-Qahhar", "سب پر تسلط رکھنے والا", "The All-Subduer"),
        AsmaAlHusna(16, "الْوَهَّابُ", "Al-Wahhab", "بے حساب عطا کرنے والا", "The Supreme Bestower"),
        AsmaAlHusna(17, "الرَّزَّاقُ", "Ar-Razzaq", "رزق دینے والا", "The Total Provider"),
        AsmaAlHusna(18, "الْفَتَّاحُ", "Al-Fattah", "راہیں کھولنے والا", "The Supreme Opener"),
        AsmaAlHusna(19, "الْعَلِيمُ", "Al-Alim", "ہر بات جاننے والا", "The All-Knowing"),
        AsmaAlHusna(20, "الْقَابِضُ", "Al-Qabid", "روکنے اور تنگی کرنے والا", "The Restrainer"),
        AsmaAlHusna(21, "الْبَاسِطُ", "Al-Basit", "فراخی اور کشادگی بخشنے والا", "The Expander"),
        AsmaAlHusna(22, "الْخَافِضُ", "Al-Khafid", "پست کرنے والا", "The Humbler"),
        AsmaAlHusna(23, "الرَّافِعُ", "Ar-Rafi", "بلند درجے دینے والا", "The Exalter"),
        AsmaAlHusna(24, "الْمُعِزُّ", "Al-Mu'izz", "عزت بخشنے والا", "The Bestower of Honor"),
        AsmaAlHusna(25, "المُذِلُّ", "Al-Muzill", "رسوا کرنے والا", "The Dishonorer"),
        AsmaAlHusna(26, "السَّمِيعُ", "As-Sami", "سب کچھ سننے والا", "The All-Hearing"),
        AsmaAlHusna(27, "الْبَصِيرُ", "Al-Basir", "سب کچھ دیکھنے والا", "The All-Seeing"),
        AsmaAlHusna(28, "الْحَكَمُ", "Al-Hakam", "حقیقی فیصلہ کرنے والا", "The Impartial Judge"),
        AsmaAlHusna(29, "الْعَدْلُ", "Al-Adl", "مکمل انصاف کرنے والا", "The Utterly Just"),
        AsmaAlHusna(30, "اللَّطِيفُ", "Al-Latif", "نہایت باریک بین اور مہربان", "The Subtly Kind"),
        AsmaAlHusna(31, "الْخَبِيرُ", "Al-Khabir", "ہر پوشیدہ بات سے باخبر", "The All-Aware"),
        AsmaAlHusna(32, "الْحَلِيمُ", "Al-Halim", "نہایت بردبار", "The Most Forbearing"),
        AsmaAlHusna(33, "الْعَظِيمُ", "Al-Azim", "عظمت اور بزرگی والا", "The Magnificent"),
        AsmaAlHusna(34, "الْغَفُورُ", "Al-Ghafur", "بے حد معاف فرمانے والا", "The All-Forgiving"),
        AsmaAlHusna(35, "الشَّكُورُ", "Ash-Shakur", "قدردانی فرمانے والا", "The Most Appreciative"),
        AsmaAlHusna(36, "الْعَلِيُّ", "Al-Ali", "سب سے بلند مرتبہ", "The Most High"),
        AsmaAlHusna(37, "الْكَبِيرُ", "Al-Kabir", "سب سے بڑا", "The Most Great"),
        AsmaAlHusna(38, "الْحَفِيظُ", "Al-Hafiz", "سب کی حفاظت کرنے والا", "The Preserver"),
        AsmaAlHusna(39, "المُقِيتُ", "Al-Muqit", "سب کو غذا دینے والا", "The Sustainer"),
        AsmaAlHusna(40, "الْحَسِيبُ", "Al-Hasib", "حساب لینے میں کافی", "The Reckoner"),
        AsmaAlHusna(41, "الْجَلِيلُ", "Al-Jalil", "نہایت بزرگ و برتر", "The Majestic"),
        AsmaAlHusna(42, "الْكَرِيمُ", "Al-Karim", "بہت کرم فرمانے والا", "The Most Generous"),
        AsmaAlHusna(43, "الرَّقِيبُ", "Ar-Raqib", "سب پر نگران", "The Watchful"),
        AsmaAlHusna(44, "الْمُجِيبُ", "Al-Mujib", "دعائیں قبول فرمانے والا", "The Responsive"),
        AsmaAlHusna(45, "الْوَاسِعُ", "Al-Wasi", "وسیع رحمت اور علم والا", "The All-Encompassing"),
        AsmaAlHusna(46, "الْحَكِيمُ", "Al-Hakim", "کمال حکمت والا", "The All-Wise"),
        AsmaAlHusna(47, "الْوَدُودُ", "Al-Wadud", "سب سے زیادہ محبت فرمانے والا", "The Most Loving"),
        AsmaAlHusna(48, "الْمَجِيدُ", "Al-Majid", "بڑی بزرگی اور شان والا", "The Most Glorious"),
        AsmaAlHusna(49, "الْبَاعِثُ", "Al-Ba'ith", "مردوں کو زندہ فرمانے والا", "The Resurrector"),
        AsmaAlHusna(50, "الشَّهِيدُ", "Ash-Shahid", "ہر جگہ حاضر و ناظر", "The All-Witnessing"),
        AsmaAlHusna(51, "الْحَقُّ", "Al-Haqq", "سچا اور برحق", "The Absolute Truth"),
        AsmaAlHusna(52, "الْوَكِيلُ", "Al-Wakil", "بہترین کارساز", "The Ultimate Trustee"),
        AsmaAlHusna(53, "الْقَوِيُّ", "Al-Qawiyy", "انتہائی طاقتور", "The All-Strong"),
        AsmaAlHusna(54, "الْمَتِينُ", "Al-Matin", "نہایت مضبوط و بے لچک", "The Firm / Steadfast"),
        AsmaAlHusna(55, "الْوَلِيُّ", "Al-Waliyy", "مددگار اور دوست", "The Protecting Friend"),
        AsmaAlHusna(56, "الْحَمِيدُ", "Al-Hamid", "تعریف کا سزاوار", "The Praiseworthy"),
        AsmaAlHusna(57, "الْمُحْصِي", "Al-Muhsi", "ہر شے کو شمار کرنے والا", "The All-Accounter"),
        AsmaAlHusna(58, "الْمُبْدِئُ", "Al-Mubdi", "پہلی بار پیدا کرنے والا", "The Originator"),
        AsmaAlHusna(59, "الْمُعِيدُ", "Al-Mu'id", "دوبارہ پیدا کرنے والا", "The Restorer"),
        AsmaAlHusna(60, "الْمُحْيِي", "Al-Muhyi", "زندگی بخشنے والا", "The Giver of Life"),
        AsmaAlHusna(61, "الْمُمِيتُ", "Al-Mumit", "موت دینے والا", "The Bringer of Death"),
        AsmaAlHusna(62, "الْحَيُّ", "Al-Hayy", "ہمیشہ زندہ رہنے والا", "The Ever-Living"),
        AsmaAlHusna(63, "الْقَيُّومُ", "Al-Qayyum", "سب کو قائم رکھنے والا", "The Sustainer of All"),
        AsmaAlHusna(64, "الْوَاجِدُ", "Al-Wajid", "سب کچھ پانے والا", "The Perceiver"),
        AsmaAlHusna(65, "الْمَاجِدُ", "Al-Majid", "عزت و شرف والا", "The Illustrious"),
        AsmaAlHusna(66, "الْوَاحِدُ", "Al-Wahid", "اکیلا اور یکتا", "The One"),
        AsmaAlHusna(67, "الْأَحَدُ", "Al-Ahad", "بے مثل و لاشریک", "The Unique / Indivisible"),
        AsmaAlHusna(68, "الصَّمَدُ", "As-Samad", "بے نیاز، سب اس کے محتاج", "The Eternal / Self-Sufficient"),
        AsmaAlHusna(69, "الْقَادِرُ", "Al-Qadir", "سب پر قدرت رکھنے والا", "The All-Capable"),
        AsmaAlHusna(70, "الْمُقْتَدِرُ", "Al-Muqtadir", "مکمل اقتدار والا", "The Supreme Determiner"),
        AsmaAlHusna(71, "الْمُقَدِّمُ", "Al-Muqaddim", "آگے کرنے والا", "The Expediter"),
        AsmaAlHusna(72, "الْمُؤَخِّرُ", "Al-Mu'akhkhir", "پیچھے کرنے والا", "The Delayer"),
        AsmaAlHusna(73, "الْأَوَّلُ", "Al-Awwal", "سب سے پہلا، جس کی ابتدا نہیں", "The Very First"),
        AsmaAlHusna(74, "الْآخِرُ", "Al-Akhir", "سب کے بعد باقی رہنے والا", "The Ultimate Last"),
        AsmaAlHusna(75, "الظَّاهِرُ", "Az-Zahir", "نشانیاں آشکار کرنے والا", "The Manifest"),
        AsmaAlHusna(76, "الْبَاطِنُ", "Al-Batin", "نظروں سے پوشیدہ", "The Hidden / Immanent"),
        AsmaAlHusna(77, "الْوَالِي", "Al-Wali", "حاکم اور منتظم", "The Patron / Governor"),
        AsmaAlHusna(78, "الْمُتَعَالِي", "Al-Muta'ali", "سب سے اعلیٰ و ارفع", "The Self-Exalted"),
        AsmaAlHusna(79, "الْبَرُّ", "Al-Barr", "احسان فرمانے والا", "The Source of Goodness"),
        AsmaAlHusna(80, "التَّوَّابُ", "At-Tawwab", "توبہ قبول فرمانے والا", "The Ever-Pardoning"),
        AsmaAlHusna(81, "الْمُنْتَقِمُ", "Al-Muntaqim", "انتقام لینے والا", "The Just Avenger"),
        AsmaAlHusna(82, "الْعَفُوُّ", "Al-Afuww", "گناہوں کو مٹانے والا", "The Supreme Pardoner"),
        AsmaAlHusna(83, "الرَّءُوفُ", "Ar-Ra'uf", "انتہائی نرمی و شفقت والا", "The Most Compassionate"),
        AsmaAlHusna(84, "مَالِكُ الْمُلْكِ", "Malik-ul-Mulk", "کائنات کی بادشاہی کا مالک", "Owner of All Sovereignty"),
        AsmaAlHusna(85, "ذُو الْجَلَالِ وَالْإِكْرَامِ", "Dhul-Jalali wal-Ikram", "عظمت اور فضل و کرم والا", "Lord of Majesty and Bounty"),
        AsmaAlHusna(86, "الْمُقْسِطُ", "Al-Muqsit", "انصاف قائم کرنے والا", "The Equitable"),
        AsmaAlHusna(87, "الْجَامِعُ", "Al-Jami", "سب کو اکٹھا کرنے والا", "The Gatherer"),
        AsmaAlHusna(88, "الْغَنِيُّ", "Al-Ghaniyy", "بے پروا اور غنی", "The Self-Sufficient"),
        AsmaAlHusna(89, "الْمُغْنِي", "Al-Mughni", "بے نیاز کر دینے والا", "The Enricher"),
        AsmaAlHusna(90, "الْمَانِعُ", "Al-Mani", "روکنے اور حفاظت کرنے والا", "The Withholder / Preventer"),
        AsmaAlHusna(91, "الضَّارُّ", "Ad-Darr", "نقصان کا اختیار رکھنے والا", "The Distressor"),
        AsmaAlHusna(92, "النَّافِعُ", "An-Nafi", "نفع پہنچانے والا", "The Propitious / Benefactor"),
        AsmaAlHusna(93, "النُّورُ", "An-Nur", "روشن فرمانے والا نور", "The Radiant Light"),
        AsmaAlHusna(94, "الْهَادِي", "Al-Hadi", "سیدھی راہ دکھانے والا", "The Supreme Guide"),
        AsmaAlHusna(95, "الْبَدِيعُ", "Al-Badi", "بے مثال چیزوں کو وجود دینے والا", "The Incomparable Originator"),
        AsmaAlHusna(96, "الْبَاقِي", "Al-Baqi", "ہمیشہ باقی رہنے والا", "The Everlasting"),
        AsmaAlHusna(97, "الْوَارِثُ", "Al-Warith", "سب کے بعد باقی وارث", "The Supreme Inheritor"),
        AsmaAlHusna(98, "الرَّشِيدُ", "Ar-Rashid", "درست فیصلے فرمانے والا", "The Guide to Right Path"),
        AsmaAlHusna(99, "الصَّبُورُ", "As-Sabur", "بے پناہ صبر و حلم والا", "The Most Patient")
    )

    // Dua Categories for Blocks Display
    val duaCategories: List<DuaCategory> = listOf(
        DuaCategory("travel", "Traveling", "سفر اور سواری", "FlightTakeoff", "Supplications for journey, vehicle, returning home"),
        DuaCategory("before_salah", "Before Salah & Wudu", "نماز اور وضو سے قبل", "Mosque", "Supplications before prayer, during adhan, and wudu"),
        DuaCategory("after_salah", "After Salah", "نماز کے بعد", "VolunteerActivism", "Authentic sunnah adhkar after fard prayers"),
        DuaCategory("food", "Food & Drink", "کھانا اور پینا", "Restaurant", "Duas before and after meals, guests, drinking water"),
        DuaCategory("sleep", "Sleeping & Waking Up", "نیند اور بیداری", "NightlightRound", "Duas before sleep, waking up, and protection at night"),
        DuaCategory("morning_evening", "Morning & Evening", "صبح و شام کے اذکار", "WbSunny", "Essential daily morning and evening fortress azkar"),
        DuaCategory("protection", "Protection & Evil Eye", "حفاظت اور نظر بد", "Security", "Protection from harm, evil eye, whisperings, and shaytan"),
        DuaCategory("home", "Entering & Leaving Home", "گھر میں داخلہ و خروج", "Home", "Duas when entering and leaving home with barakah"),
        DuaCategory("mosque", "Mosque Entry & Exit", "مسجد میں داخلہ و خروج", "LocationCity", "Duas when going to, entering, and stepping out of mosque"),
        DuaCategory("distress", "Distress & Anxiety", "پریشانی، غم اور قرض", "SentimentDissatisfied", "Relief from worry, sorrow, grief, debt, and difficulty"),
        DuaCategory("rain_nature", "Rain & Nature", "بارش، بادل اور موسم", "CloudQueue", "Duas during rainfall, thunder, windstorms, and new moon"),
        DuaCategory("fasting", "Fasting & Ramadan", "روزہ اور افطار", "DarkMode", "Duas for keeping fast, breaking fast (iftar), and laylatul qadr"),
        DuaCategory("hajj_umrah", "Hajj & Umrah", "حج و عمرہ اور تلبیہ", "Navigation", "Talbiyah, tawaf, sa'i, Arafat, and pilgrimage duas"),
        DuaCategory("forgiveness", "Forgiveness & Repentance", "توبہ و استغفار", "FavoriteBorder", "Sayyidul Istighfar and comprehensive prayers for tawbah"),
        DuaCategory("sickness", "Sickness & Shifa", "عیادت، بیماری اور شفاء", "LocalHospital", "Duas for visiting the sick, pain relief, and healing"),
        DuaCategory("social", "Social Etiquette", "ملاقات، چھینک اور گفتگو", "People", "Greetings, sneezing, gratitude to people, and gatherings"),
        DuaCategory("funeral", "Funeral & Grave", "جنازہ اور زیارت قبور", "LocationOn", "Condolences, janazah prayer, and visiting graveyard"),
        DuaCategory("praise", "Praise & Gratitude", "اللہ کی حمد اور شکر", "Star", "Treasures of tasbeeh, tahmeed, takbeer, and tahlil")
    )

    // Comprehensive Curated Duas by Category
    val curatedDuas: List<DuaItem> = listOf(
        // 1. Traveling
        DuaItem(
            id = "dua_travel_1",
            category = "Traveling",
            title = "Dua for Boarding a Vehicle or Travelling",
            arabicText = "سُبْحَانَ الَّذِي سَخَّرَ لَنَا هَٰذَا وَمَا كُنَّا لَهُ مُقْرِنِينَ وَإِنَّا إِلَىٰ رَبِّنَا لَمُنْقَلِبُونَ",
            transliteration = "Subhanalladhi sakh-khara lana hadha wa ma kunna lahu muqrinin, wa inna ila Rabbina lamunqalibun",
            englishTranslation = "Glory to Him who has subjected this to us, and we could never have it by our efforts. And indeed, to our Lord we will return.",
            urduTranslation = "پاک ہے وہ ذات جس نے اس سواری کو ہمارے قابو میں کر دیا حالانکہ ہم اسے قابو میں لانے کی طاقت نہ رکھتے تھے، اور بے شک ہم اپنے رب ہی کی طرف لوٹ کر جانے والے ہیں۔",
            reference = "Surah Az-Zukhruf (43:13-14)"
        ),
        DuaItem(
            id = "dua_travel_2",
            category = "Traveling",
            title = "Supplication for the Traveler upon Journey",
            arabicText = "اللَّهُمَّ إِنَّا نَسْأَلُكَ فِي سَفَرِنَا هَذَا الْبِرَّ وَالتَّقْوَى، وَمِنَ الْعَمَلِ مَا تَرْضَى، اللَّهُمَّ هَوِّنْ عَلَيْنَا سَفَرَنَا هَذَا وَاطْوِ عَنَّا بُعْدَهُ",
            transliteration = "Allahumma inna nas'aluka fi safarina hadhal-birra wat-taqwa, wa minal-'amali ma tarda. Allahumma hawwin 'alayna safarana hadha watwi 'anna bu'dah",
            englishTranslation = "O Allah, we ask You on this journey of ours for righteousness and piety, and deeds that please You. O Allah, ease this journey for us and shorten its distance.",
            urduTranslation = "اے اللہ! ہم آپ سے اپنے اس سفر میں نیکی اور تقویٰ کا سوال کرتے ہیں اور ایسے عمل کا جس سے تو راضی ہو۔ اے اللہ! ہمارے اس سفر کو ہم پر آسان فرما اور اس کی مسافت کو لپیٹ دے۔",
            reference = "Sahih Muslim 1342"
        ),

        // 2. Before Salah & Wudu
        DuaItem(
            id = "dua_before_salah_1",
            category = "Before Salah & Wudu",
            title = "Before Starting Wudu (Ablution)",
            arabicText = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
            transliteration = "Bismillahir-Rahmanir-Raheem",
            englishTranslation = "In the name of Allah, the Entirely Merciful, the Especially Merciful.",
            urduTranslation = "اللہ کے نام سے شروع جو نہایت مہربان بہت رحم کرنے والا ہے۔",
            reference = "Sunan Abi Dawud 101"
        ),
        DuaItem(
            id = "dua_before_salah_2",
            category = "Before Salah & Wudu",
            title = "Dua upon Hearing the Adhan (Call to Prayer)",
            arabicText = "اللَّهُمَّ رَبَّ هَذِهِ الدَّعْوَةِ التَّامَّةِ، وَالصَّلَاةِ الْقَائِمَةِ، آتِ مُحَمَّدًا الْوَسِيلَةَ وَالْفَضِيلَةَ، وَابْعَثْهُ مَقَامًا مَحْمُودًا الَّذِي وَعَدْتَهُ",
            transliteration = "Allahumma Rabba hadhihid-da'watit-tammah, was-salatil-qa'imah, ati Muhammadanil-wasilata wal-fadilah, wab'athhu maqamam-mahmudanilladhi wa'adtah",
            englishTranslation = "O Allah, Owner of this perfect call and established prayer, grant Muhammad the station of intercession and favor, and resurrect him to the praised status which You have promised him.",
            urduTranslation = "اے اللہ! اس کامل پکار اور قائم ہونے والی نماز کے رب، حضرت محمد ﷺ کو وسیلہ اور بزرگی عطا فرما اور انہیں اس مقامِ محمود پر فائز فرما جس کا تو نے ان سے وعدہ فرمایا ہے۔",
            reference = "Sahih al-Bukhari 614"
        ),

        // 3. After Salah
        DuaItem(
            id = "dua_after_salah_1",
            category = "After Salah",
            title = "Istighfar and Peace After Prayer",
            arabicText = "أَسْتَغْفِرُ اللَّهَ، أَسْتَغْفِرُ اللَّهَ، أَسْتَغْفِرُ اللَّهَ، اللَّهُمَّ أَنْتَ السَّلَامُ وَمِنْكَ السَّلَامُ، تَبَارَكْتَ يَا ذَا الْجَلَالِ وَالإِكْرَامِ",
            transliteration = "Astaghfirullah (3x). Allahumma antas-salamu wa minkas-salam, tabarakta ya dhal-jalali wal-ikram",
            englishTranslation = "I seek Allah's forgiveness (3 times). O Allah, You are Peace and from You is peace. Blessed are You, Owner of majesty and honor.",
            urduTranslation = "میں اللہ سے بخشش مانگتا ہوں (تین بار)۔ اے اللہ! تو ہی سلامتی والا ہے اور تجھی سے سلامتی ہے، تو برکت والا ہے اے عظمت اور بزرگی والے۔",
            reference = "Sahih Muslim 591",
            repeatCount = 3
        ),
        DuaItem(
            id = "dua_after_salah_2",
            category = "After Salah",
            title = "Ayat al-Kursi After Every Obligatory Prayer",
            arabicText = "اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ ۚ لَا تَأْخُذُهُ سِنَةٌ وَلَا نَوْمٌ ۚ لَهُ مَا فِي السَّمَاوَاتِ وَمَا فِي الْأَرْضِ",
            transliteration = "Allahu la ilaha illa Huwal-Hayyul-Qayyum, la ta'khudhuhu sinatun wa la nawm, lahu ma fis-samawati wa ma fil-ard...",
            englishTranslation = "Allah - there is no deity except Him, the Ever-Living, the Sustainer of all existence. Neither drowsiness overtakes Him nor sleep. To Him belongs whatever is in the heavens and the earth.",
            urduTranslation = "اللہ، اس کے سوا کوئی معبود نہیں، وہ ہمیشہ زندہ رہنے والا اور سب کو تھامنے والا ہے۔ نہ اسے اونگھ آتی ہے نہ نیند، جو کچھ آسمانوں میں ہے اور جو کچھ زمین میں ہے سب اسی کا ہے۔",
            reference = "Surah Al-Baqarah (2:255) / Sunan An-Nasa'i"
        ),

        // 4. Food & Drink
        DuaItem(
            id = "dua_food_1",
            category = "Food & Drink",
            title = "Before Starting to Eat",
            arabicText = "بِسْمِ اللَّهِ وَعَلَى بَرَكَةِ اللَّهِ",
            transliteration = "Bismillahi wa 'ala barakatillah",
            englishTranslation = "In the name of Allah and with the blessings of Allah.",
            urduTranslation = "اللہ کے نام سے اور اللہ کی برکت پر شروع کرتا ہوں۔",
            reference = "Al-Mustadrak al-Hakim"
        ),
        DuaItem(
            id = "dua_food_2",
            category = "Food & Drink",
            title = "After Finishing the Meal",
            arabicText = "الْحَمْدُ لِلَّهِ الَّذِي أَطْعَمَنَا وَسَقَانَا وَجَعَلَنَا مُسْلِمِينَ",
            transliteration = "Alhamdu lillahilladhi at'amana wa saqana wa ja'alana muslimeen",
            englishTranslation = "Praise be to Allah who has fed us, given us drink, and made us Muslims.",
            urduTranslation = "تمام تعریفیں اللہ کے لیے ہیں جس نے ہمیں کھلایا اور پلایا اور ہمیں مسلمان بنایا۔",
            reference = "Sunan Abi Dawud 3850 & At-Tirmidhi"
        ),

        // 5. Sleeping & Waking Up
        DuaItem(
            id = "dua_sleep_1",
            category = "Sleeping & Waking Up",
            title = "Before Going to Sleep",
            arabicText = "بِاسْمِكَ اللَّهُمَّ أَمُوتُ وَأَحْيَا",
            transliteration = "Bismika Allahumma amutu wa ahya",
            englishTranslation = "In Your name, O Allah, I die and I live.",
            urduTranslation = "اے اللہ! تیرے ہی نام کے ساتھ میں مرتا ہوں (سوتا ہوں) اور جیتا ہوں (جاگتا ہوں)۔",
            reference = "Sahih al-Bukhari 6324"
        ),
        DuaItem(
            id = "dua_sleep_2",
            category = "Sleeping & Waking Up",
            title = "Upon Waking Up from Sleep",
            arabicText = "الْحَمْدُ لِلَّهِ الَّذِي أَحْيَانَا بَعْدَ مَا أَمَاتَنَا وَإِلَيْهِ النُّشُورُ",
            transliteration = "Alhamdu lillahilladhi ahyana ba'da ma amatana wa ilaihin-nushoor",
            englishTranslation = "Praise is to Allah who gave us life after having caused us to die, and to Him is the resurrection.",
            urduTranslation = "تمام تعریفیں اس اللہ کے لیے ہیں جس نے ہمیں مارنے کے بعد زندہ کیا اور اسی کی طرف لوٹ کر جانا ہے۔",
            reference = "Sahih al-Bukhari 6312"
        ),

        // 6. Morning & Evening
        DuaItem(
            id = "dua_morning_1",
            category = "Morning & Evening",
            title = "Morning Gratitude and Sovereignty",
            arabicText = "أَصْبَحْنَا وَأَصْبَحَ الْمُلْكُ لِلَّهِ، وَالْحَمْدُ لِلَّهِ، لاَ إِلَهَ إِلاَّ اللَّهُ وَحْدَهُ لاَ شَرِيكَ لَهُ، لَهُ الْمُلْكُ وَلَهُ الْحَمْدُ وَهُوَ عَلَى كُلِّ شَيْءٍ قَدِيرٌ",
            transliteration = "Asbahna wa asbahal-mulku lillah, wal-hamdu lillah, la ilaha illallahu wahdahu la sharika lah, lahul-mulku wa lahul-hamd, wa Huwa 'ala kulli shay'in Qadir",
            englishTranslation = "We have entered the morning and the kingdom belongs to Allah, and all praise is for Allah. There is no deity except Allah alone, without partner. His is the sovereignty and praise, and He is over all things capable.",
            urduTranslation = "ہم نے صبح کی اور اللہ کی بادشاہی نے صبح کی اور سب تعریف اللہ ہی کے لیے ہے۔ اللہ کے سوا کوئی معبود نہیں وہ اکیلا ہے اس کا کوئی شریک نہیں، اسی کی بادشاہی ہے اور اسی کی تعریف اور وہ ہر چیز پر قادر ہے۔",
            reference = "Sahih Muslim 2723"
        ),
        DuaItem(
            id = "dua_evening_1",
            category = "Morning & Evening",
            title = "Evening Remembrance and Surrender",
            arabicText = "أَمْسَيْنَا وَأَمْسَى الْمُلْكُ لِلَّهِ، وَالْحَمْدُ لِلَّهِ، لاَ إِلَهَ إِلاَّ اللَّهُ وَحْدَهُ لاَ شَرِيكَ لَهُ",
            transliteration = "Amsayna wa amsal-mulku lillah, wal-hamdu lillah, la ilaha illallahu wahdahu la sharika lah",
            englishTranslation = "We have reached the evening and the kingdom belongs to Allah, and all praise is for Allah. There is no deity except Allah alone.",
            urduTranslation = "ہم نے شام کی اور اللہ کے سارے ملک نے شام کی اور سب تعریف اللہ ہی کے لیے ہے، اللہ کے سوا کوئی معبود نہیں۔",
            reference = "Sahih Muslim 2723"
        ),

        // 7. Protection & Evil Eye
        DuaItem(
            id = "dua_protection_1",
            category = "Protection & Evil Eye",
            title = "Comprehensive Protection from All Harm",
            arabicText = "بِسْمِ اللَّهِ الَّذِي لَا يَضُرُّ مَعَ اسْمِهِ شَيْءٌ فِي الْأَرْضِ وَلَا فِي السَّمَاءِ وَهُوَ السَّمِيعُ الْعَلِيمُ",
            transliteration = "Bismillahilladhi la yadurru ma'asmihi shay'un fil-ardi wa la fis-sama'i wa Huwas-Sami'ul-'Alim",
            englishTranslation = "In the name of Allah, with whose Name nothing can cause harm on earth or in the heavens, and He is the All-Hearing, the All-Knowing.",
            urduTranslation = "اللہ کے نام سے جس کے نام کے ساتھ زمین اور آسمان کی کوئی چیز نقصان نہیں پہنچا سکتی اور وہ خوب سننے والا، سب کچھ جاننے والا ہے۔",
            reference = "Sunan Abi Dawud 5088 & At-Tirmidhi 3388",
            repeatCount = 3
        ),
        DuaItem(
            id = "dua_protection_2",
            category = "Protection & Evil Eye",
            title = "Seeking Refuge with Allah's Perfect Words",
            arabicText = "أَعُوذُ بِكَلِمَاتِ اللَّهِ التَّامَّاتِ مِنْ شَرِّ مَا خَلَقَ",
            transliteration = "A'udhu bi kalimatil-lahit-tammati min sharri ma khalaq",
            englishTranslation = "I seek refuge in the perfect words of Allah from the evil of what He has created.",
            urduTranslation = "میں اللہ کے مکمل کلمات کی پناہ مانگتا ہوں اس کی پیدا کردہ تمام مخلوقات کے شر سے۔",
            reference = "Sahih Muslim 2708",
            repeatCount = 3
        ),

        // 8. Home Entry & Exit
        DuaItem(
            id = "dua_home_1",
            category = "Entering & Leaving Home",
            title = "When Leaving the House",
            arabicText = "بِسْمِ اللَّهِ تَوَكَّلْتُ عَلَى اللَّهِ، وَلَا حَوْلَ وَلَا قُوَّةَ إِلَّا بِاللَّهِ",
            transliteration = "Bismillahi tawakkaltu 'alallah, wa la hawla wa la quwwata illa billah",
            englishTranslation = "In the name of Allah, I place my trust in Allah; there is no power and no strength except with Allah.",
            urduTranslation = "اللہ کے نام سے، میں نے اللہ پر بھروسہ کیا، اور گناہوں سے بچنے اور نیکی کرنے کی طاقت اللہ ہی کی توفیق سے ہے۔",
            reference = "Sunan Abi Dawud 5095 & At-Tirmidhi 3426"
        ),
        DuaItem(
            id = "dua_home_2",
            category = "Entering & Leaving Home",
            title = "When Entering the House",
            arabicText = "بِسْمِ اللَّهِ وَلَجْنَا، وَبِسْمِ اللَّهِ خَرَجْنَا، وَعَلَى اللَّهِ رَبِّنَا تَوَكَّلْنَا",
            transliteration = "Bismillahi walajna, wa bismillahi kharajna, wa 'alallahi Rabbina tawakkalna",
            englishTranslation = "In the name of Allah we enter, and in the name of Allah we leave, and upon our Lord we rely.",
            urduTranslation = "اللہ کے نام سے ہم داخل ہوئے اور اللہ ہی کے نام سے ہم نکلے اور ہم نے اپنے رب اللہ ہی پر بھروسہ کیا۔",
            reference = "Sunan Abi Dawud 5096"
        ),

        // 9. Mosque
        DuaItem(
            id = "dua_mosque_1",
            category = "Mosque Entry & Exit",
            title = "Upon Entering the Mosque",
            arabicText = "اللَّهُمَّ افْتَحْ لِي أَبْوَابَ رَحْمَتِكَ",
            transliteration = "Allahummaf-tah li abwaba rahmatik",
            englishTranslation = "O Allah, open for me the doors of Your mercy.",
            urduTranslation = "اے اللہ! میرے لیے اپنی رحمت کے دروازے کھول دے۔",
            reference = "Sahih Muslim 713"
        ),
        DuaItem(
            id = "dua_mosque_2",
            category = "Mosque Entry & Exit",
            title = "Upon Leaving the Mosque",
            arabicText = "اللَّهُمَّ إِنِّي أَسْأَلُكَ مِنْ فَضْلِكَ",
            transliteration = "Allahumma inni as'aluka min fadlik",
            englishTranslation = "O Allah, I ask You from Your bounty.",
            urduTranslation = "اے اللہ! میں تجھ سے تیرے فضل کا سوال کرتا ہوں۔",
            reference = "Sahih Muslim 713"
        ),

        // 10. Distress & Anxiety
        DuaItem(
            id = "dua_distress_1",
            category = "Distress & Anxiety",
            title = "Dua of Prophet Yunus (A.S) in Deep Distress",
            arabicText = "لَا إِلَٰهَ إِلَّا أَنتَ سُبْحَانَكَ إِنِّي كُنتُ مِنَ الظَّالِمِينَ",
            transliteration = "La ilaha illa Anta subhanaka inni kuntu minaz-zalimeen",
            englishTranslation = "There is no deity except You; exalted are You. Indeed, I have been of the wrongdoers.",
            urduTranslation = "تیرے سوا کوئی معبود نہیں، تو پاک ہے، بے شک میں ہی قصورواروں میں سے تھا۔",
            reference = "Surah Al-Anbiya (21:87) / At-Tirmidhi"
        ),
        DuaItem(
            id = "dua_distress_2",
            category = "Distress & Anxiety",
            title = "Supplication in Heavy Anxiety and Debt",
            arabicText = "اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنَ الْهَمِّ وَالْحَزَنِ، وَالْعَجْزِ وَالْكَسَلِ، وَالْبُخْلِ وَالْجُبْنِ، وَضَلَعِ الدَّيْنِ، وَغَلَبَةِ الرِّجَالِ",
            transliteration = "Allahumma inni a'udhu bika minal-hammi wal-hazan, wal-'ajzi wal-kasal, wal-bukhli wal-jubn, wa dala'id-dayni wa ghalabatir-rijal",
            englishTranslation = "O Allah, I seek refuge in You from grief and sorrow, helplessness and laziness, stinginess and cowardice, the burden of debt and the oppression of men.",
            urduTranslation = "اے اللہ! میں تیری پناہ مانگتا ہوں غم اور پریشانی سے، عاجزی اور سستی سے، بخل اور بزدلی سے، قرض کے بوجھ اور لوگوں کے دباؤ سے۔",
            reference = "Sahih al-Bukhari 2893"
        ),

        // 11. Rain & Nature
        DuaItem(
            id = "dua_rain_1",
            category = "Rain & Nature",
            title = "When Rain Begins to Fall",
            arabicText = "اللَّهُمَّ صَيِّبًا نَافِعًا",
            transliteration = "Allahumma sayyiban nafi'an",
            englishTranslation = "O Allah, make it a beneficial downpour.",
            urduTranslation = "اے اللہ! اس بارش کو نفع بخش اور فائدہ مند بنا۔",
            reference = "Sahih al-Bukhari 1032"
        ),
        DuaItem(
            id = "dua_rain_2",
            category = "Rain & Nature",
            title = "Upon Hearing Thunder",
            arabicText = "سُبْحَانَ الَّذِي يُسَبِّحُ الرَّعْدُ بِحَمْدِهِ وَالْمَلَائِكَةُ مِنْ خِيفَتِهِ",
            transliteration = "Subhanalladhi yusabbihur-ra'du bi hamdihi wal-mala'ikatu min kheefatih",
            englishTranslation = "Glory to Him whom thunder praises with praise, and so do the angels out of fear of Him.",
            urduTranslation = "پاک ہے وہ ذات جس کی تسبیح کڑک اس کی حمد کے ساتھ بیان کرتی ہے اور فرشتے اس کے خوف سے لرزاں ہو کر۔",
            reference = "Muwatta Malik 1808"
        ),

        // 12. Fasting & Ramadan
        DuaItem(
            id = "dua_fasting_1",
            category = "Fasting & Ramadan",
            title = "Dua for Breaking Fast (Iftar)",
            arabicText = "ذَهَبَ الظَّمَأُ، وَابْتَلَّتِ الْعُرُوقُ، وَثَبَتَ الْأَجْرُ إِنْ شَاءَ اللَّهُ",
            transliteration = "Dhahabadh-dhama'u, wabtallatil-'urooqu, wa thabatal-ajru in sha Allah",
            englishTranslation = "The thirst has gone, the veins are moistened, and the reward is confirmed, if Allah wills.",
            urduTranslation = "پیاس بجھ گئی، رگیں تر ہو گئیں اور اللہ نے چاہا تو ثواب پکا ہو گیا۔",
            reference = "Sunan Abi Dawud 2357"
        ),

        // 13. Forgiveness & Repentance
        DuaItem(
            id = "dua_forgiveness_1",
            category = "Forgiveness & Repentance",
            title = "Sayyidul Istighfar (Master Supplication for Forgiveness)",
            arabicText = "اللَّهُمَّ أَنْتَ رَبِّي لَا إِلَهَ إِلَّا أَنْتَ، خَلَقْتَنِي وَأَنَا عَبْدُكَ، وَأَنَا عَلَى عَهْدِكَ وَوَعْدِكَ مَا اسْتَطَعْتُ، أَعُوذُ بِكَ مِنْ شَرِّ مَا صَنَعْتُ، أَبُوءُ لَكَ بِنِعْمَتِكَ عَلَيَّ، وَأَبُوءُ بِذَنْبِي، فَاغْفِرْ لِي فَإِنَّهُ لَا يَغْفِرُ الذُّنُوبَ إِلَّا أَنْتَ",
            transliteration = "Allahumma Anta Rabbi la ilaha illa Anta, khalaqtani wa ana 'abduka, wa ana 'ala 'ahdika wa wa'dika mastata'tu, a'udhu bika min sharri ma sana'tu, abu'u laka bi ni'matika 'alayya, wa abu'u bi dhanbi faghfir li fa innahu la yaghfirudh-dhunuba illa Anta",
            englishTranslation = "O Allah, You are my Lord; there is no deity except You. You created me and I am Your servant, and I am committed to Your promise as best I can. I seek refuge in You from the evil of what I have done. I acknowledge Your blessing upon me and I confess my sin, so forgive me, for none forgives sins except You.",
            urduTranslation = "اے اللہ! تو میرا رب ہے تیرے سوا کوئی معبود نہیں، تو نے مجھے پیدا کیا اور میں تیرا بندہ ہوں اور میں اپنی طاقت کے مطابق تیرے عہد اور وعدے پر قائم ہوں۔ میں نے جو کچھ کیا اس کے شر سے تیری پناہ چاہتا ہوں، میں تیرے انعام کا اقرار کرتا ہوں اور اپنے گناہ کا اعتراف کرتا ہوں، لہٰذا مجھے بخش دے کیونکہ تیرے سوا کوئی گناہوں کو معاف نہیں کر سکتا۔",
            reference = "Sahih al-Bukhari 6306"
        ),

        // 14. Sickness & Shifa
        DuaItem(
            id = "dua_sickness_1",
            category = "Sickness & Shifa",
            title = "Supplication for Healing Pain and Illness",
            arabicText = "اللَّهُمَّ رَبَّ النَّاسِ، أَذْهِبِ الْبَاسَ، اشْفِ أَنْتَ الشَّافِي، لَا شِفَاءَ إِلَّا شِفَاؤُكَ، شِفَاءً لَا يُغَادِرُ سَقَمًا",
            transliteration = "Allahumma Rabban-nas, adh-hibil-ba's, ishfi Antash-Shafi, la shifa'a illa shifa'uk, shifa'an la yughadiru saqama",
            englishTranslation = "O Allah, Lord of mankind, remove the harm and heal, for You are the Healer. There is no healing except Your healing - a cure that leaves behind no ailment.",
            urduTranslation = "اے اللہ! انسانوں کے پالنے والے، تکلیف کو دور فرما دے اور شفا عطا فرما، تو ہی شفا دینے والا ہے، تیری شفا کے سوا کوئی شفا نہیں، ایسی شفا دے جو کسی بیماری کو باقی نہ چھوڑے۔",
            reference = "Sahih al-Bukhari 5743 & Muslim 2191"
        ),

        // 15. Praise & Gratitude
        DuaItem(
            id = "dua_praise_1",
            category = "Praise & Gratitude",
            title = "Treasures of SubhanAllah and Al-Hamdulillah",
            arabicText = "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ، سُبْحَانَ اللَّهِ الْعَظِيمِ",
            transliteration = "Subhanallahi wa bihamdihi, Subhanallahil-'Azim",
            englishTranslation = "Glory be to Allah and His is the praise; Glory be to Allah, the Magnificent.",
            urduTranslation = "اللہ پاک ہے اپنی تمام خوبیوں کے ساتھ، اللہ پاک ہے جو بہت بڑی عظمت والا ہے۔",
            reference = "Sahih al-Bukhari 6406 & Muslim 2694",
            repeatCount = 100
        ),

        // 16. Hajj & Umrah
        DuaItem(
            id = "dua_hajj_1",
            category = "Hajj & Umrah",
            title = "Talbiyah of Hajj & Umrah",
            arabicText = "لَبَّيْكَ اللَّهُمَّ لَبَّيْكَ، لَبَّيْكَ لَا شَرِيكَ لَكَ لَبَّيْكَ، إِنَّ الْحَمْدَ وَالنِّعْمَةَ لَكَ وَالْمُلْكَ، لَا شَرِيكَ لَكَ",
            transliteration = "Labbayka Allahumma Labbayk, Labbayka la sharika laka Labbayk, Innal-hamda wan-ni'mata laka wal-mulk, la sharika lak",
            englishTranslation = "Here I am at Your service, O Allah, here I am! Here I am, You have no partner, here I am! Verily, all praise, grace, and kingdom belong to You. You have no partner!",
            urduTranslation = "حاضر ہوں اے اللہ! میں حاضر ہوں، تیرا کوئی شریک نہیں میں حاضر ہوں، یقیناً تمام تعریفیں، نعمتیں اور بادشاہی تیری ہی ہے، تیرا کوئی شریک نہیں۔",
            reference = "Sahih al-Bukhari 1549 & Muslim 1184"
        ),
        DuaItem(
            id = "dua_hajj_2",
            category = "Hajj & Umrah",
            title = "Between Yemeni Corner and the Black Stone (Tawaf)",
            arabicText = "رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الْآخِرَةِ حَسَنَةً وَقِنَا عَذَابَ النَّارِ",
            transliteration = "Rabbana atina fid-dunya hasanatan wa fil-akhirati hasanatan wa qina 'adhaban-nar",
            englishTranslation = "Our Lord, give us in this world that which is good and in the Hereafter that which is good and protect us from the punishment of the Fire.",
            urduTranslation = "اے ہمارے رب! ہمیں دنیا میں بھی بھلائی عطا فرما اور آخرت میں بھی بھلائی عطا فرما اور ہمیں دوزخ کے عذاب سے بچا۔",
            reference = "Sunan Abi Dawud 1892"
        ),

        // 17. Social Etiquette
        DuaItem(
            id = "dua_social_1",
            category = "Social Etiquette",
            title = "Upon Sneezing and Responding",
            arabicText = "الْحَمْدُ لِلَّهِ | يَرْحَمُكَ اللَّهُ | يَهْدِيكُمُ اللَّهُ وَيُصْلِحُ بَالَكُمْ",
            transliteration = "Alhamdu lillah (sneezer) | Yarhamukallah (listener) | Yahdikumullahu wa yuslihu balakum",
            englishTranslation = "All praise is to Allah | May Allah have mercy on you | May Allah guide you and rectify your condition.",
            urduTranslation = "چھینکنے والا کہے: تمام تعریفیں اللہ کے لیے ہیں۔ سننے والا کہے: اللہ تجھ پر رحم فرمائے۔ پھر چھینکنے والا جواب دے: اللہ تمہیں ہدایت دے اور تمہاری حالت درست کرے۔",
            reference = "Sahih al-Bukhari 6224"
        ),
        DuaItem(
            id = "dua_social_2",
            category = "Social Etiquette",
            title = "When Expressing Gratitude to Someone",
            arabicText = "جَزَاكَ اللَّهُ خَيْرًا",
            transliteration = "Jazakallahu khayran",
            englishTranslation = "May Allah reward you with superior goodness.",
            urduTranslation = "اللہ تعالیٰ آپ کو بہترین جزائے خیر عطا فرمائے۔",
            reference = "Jami' at-Tirmidhi 2035"
        ),

        // 18. Funeral & Grave
        DuaItem(
            id = "dua_funeral_1",
            category = "Funeral & Grave",
            title = "Upon Visiting the Graveyard",
            arabicText = "السَّلَامُ عَلَيْكُمْ أَهْلَ الدِّيَارِ مِنَ الْمُؤْمِنِينَ وَالْمُسْلِمِينَ، وَإِنَّا إِنْ شَاءَ اللَّهُ بِكُمْ لَاحِقُونَ، نَسْأَلُ اللَّهَ لَنَا وَلَكُمُ الْعَافِيَةَ",
            transliteration = "Assalamu 'alaykum ahlad-diyari minal-mu'minina wal-muslimin, wa inna in sha Allahu bikum lahiqun, nas'alullaha lana wa lakumul-'afiyah",
            englishTranslation = "Peace be upon you, O inhabitants of the graves from the believers and Muslims. And indeed, if Allah wills, we will join you. We ask Allah for well-being for us and for you.",
            urduTranslation = "سلامتی ہو تم پر اے مومنوں اور مسلمانوں کے گھر والوں! اور ہم بھی اگر اللہ نے چاہا تو تم سے آ ملنے والے ہیں۔ ہم اپنے لیے اور تمہارے لیے عافیت کا سوال کرتے ہیں۔",
            reference = "Sahih Muslim 975"
        ),
        DuaItem(
            id = "dua_funeral_2",
            category = "Funeral & Grave",
            title = "Upon Hearing of a Calamity or Death",
            arabicText = "إِنَّا لِلَّهِ وَإِنَّا إِلَيْهِ رَاجِعُونَ، اللَّهُمَّ أْجُرْنِي فِي مُصِيبَتِي وَأَخْلِفْ لِي خَيْرًا مِنْهَا",
            transliteration = "Inna lillahi wa inna ilayhi raji'un, Allahumma'-jurni fi musibati wa akhlif li khayran minha",
            englishTranslation = "Indeed we belong to Allah, and indeed to Him we will return. O Allah, reward me in my affliction and replace it with something better for me.",
            urduTranslation = "بے شک ہم اللہ ہی کے ہیں اور اسی کی طرف لوٹ کر جانے والے ہیں۔ اے اللہ! مجھے میری اس مصیبت میں اجر عطا فرما اور مجھے اس سے بہتر نعم البدل عطا فرما۔",
            reference = "Sahih Muslim 918"
        )
    )

    val islamicEvents: List<IslamicEvent> = listOf(
        IslamicEvent("Islamic New Year", "رأس السنة الهجرية", "1 Muharram 1446", "Jul 7, 2024"),
        IslamicEvent("Day of Ashura", "یوم عاشوراء", "10 Muharram 1446", "Jul 16, 2024"),
        IslamicEvent("Mawlid an-Nabi", "عید میلاد النبی ﷺ", "12 Rabi al-Awwal 1446", "Sep 15, 2024"),
        IslamicEvent("Isra and Mi'raj", "شب معراج", "27 Rajab 1446", "Jan 27, 2025"),
        IslamicEvent("Shab-e-Barat", "شب برات", "15 Sha'ban 1446", "Feb 14, 2025"),
        IslamicEvent("First Day of Ramadan", "آغاز رمضان المبارک", "1 Ramadan 1446", "Mar 1, 2025"),
        IslamicEvent("Laylat al-Qadr", "شب قدر", "27 Ramadan 1446", "Mar 27, 2025"),
        IslamicEvent("Eid al-Fitr", "عید الفطر المبارک", "1 Shawwal 1446", "Mar 30, 2025"),
        IslamicEvent("Day of Arafah", "یوم عرفہ (حج)", "9 Dhu al-Hijjah 1446", "Jun 5, 2025"),
        IslamicEvent("Eid al-Adha", "عید الاضحیٰ", "10 Dhu al-Hijjah 1446", "Jun 6, 2025")
    )
}
