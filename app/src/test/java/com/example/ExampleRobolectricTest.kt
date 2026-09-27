package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.repository.OfflineQuranProvider
import com.example.data.repository.QuranRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Baqiyyah", appName)
  }

  @Test
  fun `test built-in Urdu Word-by-Word Quran loading`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val fatihahAyahs = OfflineQuranProvider.getAyahsForSurah(context, 1)
    assertEquals(7, fatihahAyahs.size)
    
    // Check first Ayah has words populated
    val ayah1 = fatihahAyahs[0]
    assertTrue("Ayah 1 should have words", ayah1.words.isNotEmpty())
    assertEquals("بِسْمِ", ayah1.words[0].textUthmani)
    assertTrue("Ayah 1 word 1 Urdu translation should not be blank", ayah1.words[0].urduTranslation.isNotBlank())
    
    // Test QuranRepository returns Urdu Ayahs with words
    val repo = QuranRepository(context)
    val repoAyahs = repo.getAyahsForSurah(1, "ur").first()
    assertEquals(7, repoAyahs.size)
    assertTrue(repoAyahs[0].words.isNotEmpty())
    assertFalse(repoAyahs[0].urduTranslation.isBlank())
  }

  @Test
  fun `test Sahih Muslim chapters and hadiths structure`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val hadithRepo = com.example.data.repository.HadithRepository(context)
    val chapters = hadithRepo.getChaptersForBook("sahih-muslim")
    assertTrue("Sahih Muslim should have chapters", chapters.isNotEmpty())
    val faithChapter = chapters.firstOrNull { it.chapterNumber == 1 }
    assertNotNull("Chapter 1 (Book of Faith) should exist", faithChapter)
    
    val hadiths = hadithRepo.getHadithsForChapter("sahih-muslim", 1, "urd")
    assertTrue("Hadiths in Chapter 1 should not be empty", hadiths.isNotEmpty())
    val hadith1 = hadiths[0]
    assertTrue("Hadith arabic text must not be blank", hadith1.arabicText.isNotBlank())
    assertTrue("Hadith urdu translation must not be blank", hadith1.activeTranslation.isNotBlank())
  }

  @Test
  fun `test Urdu translations across multiple books and chapters`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val hadithRepo = com.example.data.repository.HadithRepository(context)

    // 1. Bukhari Chapter 1 & Chapter 2
    val bukhariCh1 = hadithRepo.getHadithsForChapter("sahih-bukhari", 1, "urd")
    assertTrue("Bukhari Ch 1 should have hadiths", bukhariCh1.isNotEmpty())
    val bukhariH1 = bukhariCh1.first()
    assertTrue("Bukhari #1 Urdu translation should start correctly", bukhariH1.activeTranslation.isNotBlank())
    assertTrue("Bukhari #1 should contain niyat translation", bukhariH1.activeTranslation.contains("نیت"))

    // 2. Malik Chapter 1 with Hadith 2 fallback
    val malikCh1 = hadithRepo.getHadithsForChapter("muwatta-malik", 1, "urd")
    assertTrue("Malik Ch 1 should have hadiths", malikCh1.isNotEmpty())
    val malikH2 = malikCh1.firstOrNull { it.hadithNumber == "2" }
    assertNotNull("Malik Hadith 2 should exist", malikH2)
    assertTrue("Malik Hadith 2 Urdu translation must be populated", malikH2!!.activeTranslation.isNotBlank())

    // 3. Abu Dawud, Tirmidhi, Nasai, Ibn Majah
    val abudawudCh1 = hadithRepo.getHadithsForChapter("sunan-abi-dawud", 1, "urd")
    assertTrue("Abu Dawud Ch 1 hadiths should exist", abudawudCh1.isNotEmpty())
    assertTrue("Abu Dawud #1 Urdu translation should not be blank", abudawudCh1.first().activeTranslation.isNotBlank())

    val tirmidhiCh1 = hadithRepo.getHadithsForChapter("jami-at-tirmidhi", 1, "urd")
    assertTrue("Tirmidhi Ch 1 hadiths should exist", tirmidhiCh1.isNotEmpty())
    assertTrue("Tirmidhi #1 Urdu translation should not be blank", tirmidhiCh1.first().activeTranslation.isNotBlank())

    // 4. Test English translation works cleanly
    val bukhariEng = hadithRepo.getHadithsForChapter("sahih-bukhari", 1, "eng")
    assertTrue("Bukhari English should exist", bukhariEng.isNotEmpty())
    assertTrue("Bukhari #1 English should contain deeds/intentions", bukhariEng.first().activeTranslation.contains("intentions") || bukhariEng.first().englishTranslation.contains("intentions"))

    // 5. Genuinely unavailable Urdu data should return empty translation without crashing
    val qudsiHadiths = hadithRepo.getHadithsForChapter("hadith-qudsi", 1, "urd")
    assertTrue("Qudsi hadiths exist", qudsiHadiths.isNotEmpty())
  }

  @Test
  fun `test search by number across books returns exact matches fast and without crashing`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val hadithRepo = com.example.data.repository.HadithRepository(context)

    // Search by number "1"
    var progressReported = 0
    val (results, _) = hadithRepo.searchDownloadedHadiths(
        query = "1",
        searchByNumber = true,
        langCode = "ur",
        onProgress = { p, list ->
            progressReported = p
        }
    )

    assertTrue("Search by number 1 should return matching hadiths", results.isNotEmpty())
    for (hadith in results) {
        val norm = com.example.data.repository.HadithRepository.normalizeHadithNumber(hadith.hadithNumber)
        val normAra = com.example.data.repository.HadithRepository.normalizeHadithNumber(hadith.arabicNumber)
        assertTrue(
            "Each result must match hadith number 1 (was ${hadith.hadithNumber})",
            norm == "1" || hadith.hadithNumber.equals("1", ignoreCase = true) || normAra == "1"
        )
        assertTrue("Arabic text must not be blank", hadith.arabicText.isNotBlank())
        assertTrue("Hadith must have bookSlug", hadith.bookSlug.isNotBlank())
    }
  }

  @Test
  fun `test search by word across books returns matches with translations`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val hadithRepo = com.example.data.repository.HadithRepository(context)

    val (results, _) = hadithRepo.searchDownloadedHadiths(
        query = "نیت",
        searchByNumber = false,
        langCode = "ur"
    )

    assertTrue("Search by word 'نیت' should return matching results", results.isNotEmpty())
    val first = results.first()
    assertTrue("Matching hadith must have Arabic text", first.arabicText.isNotBlank())
    assertTrue("Matching hadith must have Urdu translation", first.urduTranslation.isNotBlank())
  }
}
