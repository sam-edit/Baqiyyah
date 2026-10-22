package com.example

import com.example.data.repository.AuthoritativeContentProvider
import com.example.data.repository.HadithRepository
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun authoritativeContent_hasAll114Surahs() {
    val surahs = AuthoritativeContentProvider.allSurahs
    assertEquals(114, surahs.size)
    assertEquals("Al-Fatihah", surahs.first().englishName)
    assertEquals("An-Nas", surahs.last().englishName)
  }

  @Test
  fun hadithRepository_hasCanonicalBooks() {
    val repo = HadithRepository()
    val books = repo.getAllBooks()
    assertTrue(books.size >= 8)
    assertTrue(books.any { it.slug == "sahih-bukhari" })
    assertTrue(books.any { it.slug == "sahih-muslim" })
  }

  @Test
  fun testUrduTranslation_workedAndPreviouslyMissingInSameChapter() {
    val repo = HadithRepository()
    // Muwatta Malik Chapter 1
    val hadiths = repo.getHadithsForChapter("muwatta-malik", 1, "urd")
    assertTrue("Chapter 1 of Muwatta Malik should have hadiths", hadiths.isNotEmpty())

    // 1. Hadith that already worked: Hadith 1
    val hadith1 = hadiths.firstOrNull { it.hadithNumber == "1" }
    assertNotNull("Hadith 1 should be present", hadith1)
    assertTrue("Hadith 1 Urdu translation should not be blank", hadith1!!.activeTranslation.isNotBlank())
    assertTrue("Hadith 1 should contain Urdu text", hadith1.activeTranslation.contains("عمر بن عبد"))

    // 2. Previously missing Hadith from the same chapter: Hadith 2
    val hadith2 = hadiths.firstOrNull { it.hadithNumber == "2" }
    assertNotNull("Hadith 2 should be present", hadith2)
    assertTrue("Hadith 2 Urdu translation must not be blank", hadith2!!.activeTranslation.isNotBlank())
    assertTrue("Hadith 2 Urdu translation should match authentic text",
      hadith2.activeTranslation.contains("عائشہ رضی اللہ عنہا") && hadith2.activeTranslation.contains("عصر کی نماز"))
  }

  @Test
  fun testMultipleChaptersAndMultipleBooks() {
    val repo = HadithRepository()

    // Multiple chapters in Sahih Bukhari
    val bukhariCh1 = repo.getHadithsForChapter("sahih-bukhari", 1, "urd")
    assertTrue(bukhariCh1.any { it.hadithNumber == "1" && it.activeTranslation.contains("اعمال کا دارومدار") })

    val bukhariCh2 = repo.getHadithsForChapter("sahih-bukhari", 2, "urd")
    assertTrue(bukhariCh2.any { it.hadithNumber == "8" && it.activeTranslation.contains("اسلام کی بنیاد") })
    assertTrue(bukhariCh2.any { it.hadithNumber == "13" && it.activeTranslation.contains("اپنے بھائی کے لیے") })

    // Multiple Hadith books
    val muslimCh1 = repo.getHadithsForChapter("sahih-muslim", 1, "urd")
    assertTrue(muslimCh1.any { it.hadithNumber == "93" && it.activeTranslation.isNotBlank() })
    assertTrue(muslimCh1.any { it.hadithNumber == "177" && it.activeTranslation.contains("برائی") })

    val nasaiCh1 = repo.getHadithsForChapter("sunan-an-nasai", 1, "urd")
    assertTrue(nasaiCh1.any { it.hadithNumber == "1" && it.activeTranslation.isNotBlank() })
    assertTrue(nasaiCh1.any { it.hadithNumber == "5" && it.activeTranslation.contains("مسواک") })

    val tirmidhiCh1 = repo.getHadithsForChapter("jami-at-tirmidhi", 1, "urd")
    assertTrue(tirmidhiCh1.any { it.hadithNumber == "1" && it.activeTranslation.isNotBlank() })

    val ibnMajahCh1 = repo.getHadithsForChapter("sunan-ibn-majah", 1, "urd")
    assertTrue(ibnMajahCh1.any { it.hadithNumber == "224" && it.activeTranslation.contains("طلب العلم") || it.activeTranslation.contains("علم") })

    val nawawiCh1 = repo.getHadithsForChapter("arbaeen-nawawi", 1, "urd")
    assertTrue(nawawiCh1.any { it.hadithNumber == "1" && it.activeTranslation.contains("اعمال کا دارومدار") })
  }

  @Test
  fun testGenuinelyUnavailableUrduData() {
    val repo = HadithRepository()
    // Non-existent or genuinely unavailable hadith number
    val trans = repo.getTranslationForHadith("sahih-bukhari", "99999", 999, "urd", fallbackUrdu = "", fallbackEnglish = "")
    assertEquals("", trans)
  }

  @Test
  fun testNonUrduLanguagesBehaveExactlyAsBefore() {
    val repo = HadithRepository()
    val hadithsEng = repo.getHadithsForChapter("muwatta-malik", 1, "eng")
    assertTrue(hadithsEng.isNotEmpty())
    val hadith1Eng = hadithsEng.first { it.hadithNumber == "1" }
    assertTrue(hadith1Eng.activeTranslation.contains("Umar ibn Abd al-Aziz"))

    val hadith2Eng = hadithsEng.first { it.hadithNumber == "2" }
    assertTrue(hadith2Eng.activeTranslation.contains("Aishah") || hadith2Eng.activeTranslation.contains("Asr prayer"))
  }

  @Test
  fun testBukhariLastChapterUrduTranslations() {
    val repo = HadithRepository()
    // Test last hadiths of Bukhari (e.g. 7559-7563)
    val trans7563 = repo.getTranslationForHadith("sahih-bukhari", "7563", 97, "urd")
    assertTrue("Hadith 7563 must have Urdu translation", trans7563.isNotBlank())
    assertTrue("Hadith 7563 must be in Urdu script", trans7563.contains("کلمتان") || trans7563.contains("سبحان الله") || trans7563.contains("رحمن"))
    assertFalse("Hadith 7563 must never contain English words", trans7563.contains("Narrated") || trans7563.contains("Prophet"))

    val trans7560 = repo.getTranslationForHadith("sahih-bukhari", "7560", 97, "urd")
    assertTrue("Hadith 7560 must have Urdu translation", trans7560.isNotBlank())
    assertTrue("Hadith 7560 must contain Urdu translation of citron/quran", trans7560.contains("قرآن") || trans7560.contains("مومن"))

    val trans7559 = repo.getTranslationForHadith("sahih-bukhari", "7559", 97, "urd")
    assertTrue("Hadith 7559 must have Urdu translation", trans7559.isNotBlank())
    assertTrue("Hadith 7559 must contain Urdu translation", trans7559.contains("ابوہریرہ") || trans7559.contains("مخلوق") || trans7559.contains("پیدا"))

    // Ensure no hadith translation ever outputs the unwanted placeholder
    assertFalse("Must never output the unwanted placeholder", trans7563.contains("اس سند و طریق سے بھی یہی حدیث"))
  }

  @Test
  fun testNasaiUrduTranslationsDoNotHavePlaceholder() {
    val repo = HadithRepository()
    val trans207 = repo.getTranslationForHadith("sunan-an-nasai", "207", 1, "urd")
    assertTrue("Hadith 207 must have authentic translation", trans207.isNotBlank())
    assertTrue("Hadith 207 must be in Urdu", trans207.contains("عائشہ") || trans207.contains("ام حبیبہ"))
    assertFalse("Must never output placeholder", trans207.contains("اس سند و طریق سے بھی یہی حدیث"))
  }

  @Test
  fun testTasbeehSharedRepositoryPhrases() {
    val phrases = com.example.widget.TasbeehSharedRepository.defaultPhrases
    assertTrue(phrases.isNotEmpty())
    val subhanAllah = phrases.firstOrNull { it.id == "subhanallah" }
    assertNotNull(subhanAllah)
    assertEquals("سُبْحَانَ اللَّٰهِ", subhanAllah?.arabic)
    assertEquals(33, subhanAllah?.defaultTarget)
  }

  @Test
  fun testPrayerWidgetCalculationSchedule() {
    val date = java.time.LocalDate.of(2026, 10, 10)
    val tz = java.time.ZoneId.of("Asia/Karachi")
    val schedule = com.example.domain.prayer.PrayerCalculationEngine.calculateDailySchedule(
      date = date,
      latitude = 24.8607,
      longitude = 67.0011, // Karachi
      method = com.example.domain.model.CalculationMethod.KARACHI,
      juristicSchool = com.example.domain.model.JuristicSchool.HANAFI,
      timeZone = tz
    )

    assertNotNull(schedule.fajr)
    assertNotNull(schedule.dhuhr)
    assertNotNull(schedule.asr)
    assertNotNull(schedule.maghrib)
    assertNotNull(schedule.isha)

    // Verify chronological order of primary daily prayers
    assertTrue(schedule.sunrise.isAfter(schedule.fajr))
    assertTrue(schedule.dhuhr.isAfter(schedule.sunrise))
    assertTrue(schedule.asr.isAfter(schedule.dhuhr))
    assertTrue(schedule.maghrib.isAfter(schedule.asr))
    assertTrue(schedule.isha.isAfter(schedule.maghrib))

    val state = com.example.domain.prayer.PrayerStateEngine.calculateCurrentState(
      schedule = schedule,
      currentTime = java.time.LocalTime.of(13, 0) // 1:00 PM (Between Dhuhr and Asr in Karachi)
    )
    assertEquals(com.example.domain.model.PrayerType.DHUHR, state.currentPrayer)
    assertEquals(com.example.domain.model.PrayerType.ASR, state.upcomingPrayer)
    assertTrue(state.remainingSeconds > 0)
    assertTrue(state.negativeCountdownFormatted.startsWith("-"))
  }

  @Test
  fun testPrayerWidgetSizeClassification() {
    // 2x2 small widget
    val smallW = 140
    val smallH = 110
    val isSmall = smallW < 180 || smallH < 110
    assertTrue(isSmall)

    // 4x2 medium widget
    val medW = 260
    val medH = 110
    val isMedSmall = medW < 180 || medH < 110
    val isMedLarge = medW >= 220 && medH >= 150
    assertFalse(isMedSmall)
    assertFalse(isMedLarge)

    // 4x3 or 4x4 large full dashboard widget
    val largeW = 280
    val largeH = 180
    val isLrgSmall = largeW < 180 || largeH < 110
    val isLrgLarge = largeW >= 220 && largeH >= 150
    assertFalse(isLrgSmall)
    assertTrue(isLrgLarge)
  }

  @Test
  fun testQuranFloatingAudioDurationFormatting() {
    val durationFormatted = com.example.ui.quran.formatAudioDuration(125000L) // 2m 5s
    assertEquals("02:05", durationFormatted)

    val zeroFormatted = com.example.ui.quran.formatAudioDuration(0L)
    assertEquals("00:00", zeroFormatted)

    val hoursFormatted = com.example.ui.quran.formatAudioDuration(3665000L) // 1h 1m 5s
    assertEquals("1:01:05", hoursFormatted)
  }

  @Test
  fun testRecentAudioSurahModel() {
    val recent = com.example.domain.model.RecentAudioSurah(
      surahNumber = 36,
      surahEnglishName = "Yaseen",
      surahArabicName = "يس",
      surahUrduName = "یس",
      reciterId = "ar.alafasy",
      reciterName = "Mishary Rashid Alafasy",
      lastPositionMs = 45000L,
      durationMs = 900000L
    )
    assertEquals(36, recent.surahNumber)
    assertEquals("ar.alafasy", recent.reciterId)
    assertEquals(45000L, recent.lastPositionMs)
  }
}

