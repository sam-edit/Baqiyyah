package com.example.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.QuranAudioPlayer
import com.example.data.local.AlDeenDatabase
import com.example.data.repository.*
import com.example.domain.model.*
import com.example.notification.PrayerNotificationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalTime

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    val settingsRepository = SettingsRepository.getInstance(application)
    private val database = AlDeenDatabase.getInstance(application)
    val bookmarksRepository = BookmarksRepository(database.bookmarkDao())
    val prayerRepository = PrayerRepository(settingsRepository, viewModelScope)
    val audioPlayer = QuranAudioPlayer(application)

    val userLocation: StateFlow<UserLocation> = settingsRepository.userLocation
    val dailySchedule: StateFlow<DailyPrayerSchedule> = prayerRepository.dailySchedule
    val prayerState: StateFlow<PrayerState> = prayerRepository.prayerState
    val is24HourFormat: StateFlow<Boolean> = settingsRepository.is24HourFormat
    val juristicSchool: StateFlow<JuristicSchool> = settingsRepository.juristicSchool
    val calculationMethod: StateFlow<CalculationMethod> = settingsRepository.calculationMethod

    fun toggleJuristicSchool() {
        val current = settingsRepository.juristicSchool.value
        val next = if (current == JuristicSchool.HANAFI) JuristicSchool.STANDARD else JuristicSchool.HANAFI
        settingsRepository.setJuristicSchool(next)
    }

    fun setJuristicSchool(school: JuristicSchool) {
        settingsRepository.setJuristicSchool(school)
    }

    private val _currentAyahMoment = MutableStateFlow(com.example.data.repository.MomentsProvider.getRandomAyah(null))
    val currentAyahMoment: StateFlow<com.example.domain.model.Ayah> = _currentAyahMoment.asStateFlow()

    private val _currentHadithMoment = MutableStateFlow(com.example.data.repository.MomentsProvider.getRandomHadith(null))
    val currentHadithMoment: StateFlow<com.example.domain.model.Hadith> = _currentHadithMoment.asStateFlow()

    private val _currentWordMoment = MutableStateFlow(com.example.data.repository.MomentsProvider.getRandomWord())
    val currentWordMoment: StateFlow<com.example.data.repository.AuthoritativeContentProvider.WordOfTheMoment> = _currentWordMoment.asStateFlow()

    private val _currentDuaMoment = MutableStateFlow(com.example.data.repository.MomentsProvider.getRandomDua())
    val currentDuaMoment: StateFlow<com.example.domain.model.DuaItem> = _currentDuaMoment.asStateFlow()

    val quranTranslationManager = com.example.data.quran.QuranTranslationManager.getInstance(application)
    val hadithDownloadManager = com.example.data.hadith.HadithDownloadManager.getInstance(application)
    val hadithRepository = com.example.data.repository.HadithRepository(application, hadithDownloadManager)

    val selectedQuranTranslationId: StateFlow<String> = quranTranslationManager.selectedLanguageId
    val quranTranslationRevision: StateFlow<Int> = quranTranslationManager.translationRevision
    val selectedHadithLanguageCode: StateFlow<String> = hadithDownloadManager.selectedLanguageCode
    val hadithTranslationRevision: StateFlow<Int> = hadithDownloadManager.translationRevision

    val activeAyahTranslation: StateFlow<String> = combine(
        _currentAyahMoment,
        selectedQuranTranslationId,
        quranTranslationRevision
    ) { ayah, langId, _ ->
        when (langId) {
            "ur.jalandhry" -> ayah.urduTranslation
            "en.sahih" -> {
                val fromMgr = quranTranslationManager.getTranslationForAyah(ayah.surahNumber, ayah.numberInSurah)
                if (!fromMgr.isNullOrBlank()) fromMgr else ayah.textTranslation.ifBlank { ayah.urduTranslation }
            }
            else -> {
                val fromMgr = quranTranslationManager.getTranslationForAyah(ayah.surahNumber, ayah.numberInSurah)
                if (!fromMgr.isNullOrBlank()) {
                    fromMgr
                } else {
                    viewModelScope.launch {
                        quranTranslationManager.ensureSurahTranslationLoaded(ayah.surahNumber, langId)
                    }
                    ayah.urduTranslation.ifBlank { ayah.textTranslation }
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _currentAyahMoment.value.urduTranslation)

    val activeHadithTranslation: StateFlow<String> = combine(
        _currentHadithMoment,
        selectedHadithLanguageCode,
        hadithTranslationRevision
    ) { hadith, langCode, _ ->
        val normLang = hadithDownloadManager.normalizeLang(langCode)
        if (hadith.bookSlug == "not_downloaded" || hadith.id < 0) {
            when (normLang) {
                "eng" -> hadith.englishTranslation.ifBlank { hadith.urduTranslation }
                "urd" -> hadith.urduTranslation.ifBlank { hadith.englishTranslation }
                else -> hadith.englishTranslation.ifBlank { hadith.urduTranslation }
            }
        } else {
            val isDownloaded = hadithDownloadManager.isLanguageDownloaded(hadith.bookSlug, normLang)
            if (!isDownloaded && normLang != "urd") {
                viewModelScope.launch {
                    hadithDownloadManager.downloadLanguage(hadith.bookSlug, normLang)
                }
            }
            val res = hadithRepository.getTranslationForHadith(
                bookSlug = hadith.bookSlug,
                hadithNumber = hadith.hadithNumber,
                chapterNumber = hadith.chapterNumber,
                langCode = normLang,
                fallbackUrdu = hadith.urduTranslation,
                fallbackEnglish = hadith.englishTranslation
            )
            if (res.isNotBlank()) {
                res
            } else {
                when (normLang) {
                    "eng" -> hadith.englishTranslation.ifBlank { hadith.urduTranslation }
                    "urd" -> hadith.urduTranslation.ifBlank { hadith.englishTranslation }
                    else -> hadith.englishTranslation.ifBlank { hadith.urduTranslation }
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _currentHadithMoment.value.urduTranslation.ifBlank { _currentHadithMoment.value.englishTranslation })

    val primaryLocation: StateFlow<UserLocation?> = settingsRepository.primaryLocation

    val bookmarkedReferences: StateFlow<Set<String>> = bookmarksRepository.allBookmarks
        .map { list -> list.map { it.referenceId }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    init {
        // Automatically schedule alarms for every prayer using Primary Location (if set) or active location
        viewModelScope.launch {
            combine(
                settingsRepository.primaryLocation,
                settingsRepository.userLocation,
                settingsRepository.prayerNotifications,
                settingsRepository.calculationMethod,
                settingsRepository.juristicSchool
            ) { primaryLoc, activeLoc, notifs, method, school ->
                val notifLoc = primaryLoc ?: activeLoc
                val zoneId = settingsRepository.getZoneIdForLocation(notifLoc)
                val todayDate = java.time.LocalDate.now(zoneId)
                val schedule = com.example.domain.prayer.PrayerCalculationEngine.calculateDailySchedule(
                    date = todayDate,
                    latitude = notifLoc.latitude,
                    longitude = notifLoc.longitude,
                    method = method,
                    juristicSchool = school,
                    timeZone = zoneId
                )
                Triple(schedule, notifLoc, notifs)
            }.collectLatest { (schedule, notifLoc, notifs) ->
                PrayerNotificationManager.scheduleDailyPrayerAlarms(
                    context = getApplication(),
                    schedule = schedule,
                    cityName = notifLoc.cityName,
                    notificationSettings = notifs
                )
            }
        }
    }

    fun updateLocation(location: UserLocation) {
        settingsRepository.updateLocation(location)
    }

    fun setPrimaryLocation(location: UserLocation) {
        settingsRepository.setPrimaryLocation(location)
    }

    fun clearPrimaryLocation() {
        settingsRepository.clearPrimaryLocation()
    }

    fun detectAndUseCurrentLocation(): Boolean {
        return settingsRepository.detectAndUseCurrentLocation(getApplication())
    }

    fun toggleQuietMode() {
        settingsRepository.toggleQuietMode()
    }

    fun playAudio(url: String, trackId: String) {
        audioPlayer.play(url, trackId)
    }

    fun toggleBookmark(item: BookmarkItem) {
        viewModelScope.launch {
            if (bookmarkedReferences.value.contains(item.referenceId)) {
                bookmarksRepository.removeBookmark(item.referenceId)
            } else {
                bookmarksRepository.toggleBookmark(item)
            }
        }
    }

    fun isBookmarked(referenceId: String): Boolean {
        return bookmarkedReferences.value.contains(referenceId)
    }

    fun formatTime(time: LocalTime): String {
        return prayerRepository.formatTime(time, is24HourFormat.value)
    }

    fun refreshMoments(type: com.example.ui.components.MomentType? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            when (type) {
                com.example.ui.components.MomentType.AYAH -> {
                    val ayah = com.example.data.repository.MomentsProvider.getRandomAyah(
                        context = getApplication(),
                        excludeGlobalNumber = _currentAyahMoment.value.number
                    )
                    _currentAyahMoment.value = ayah
                }
                com.example.ui.components.MomentType.HADEETH -> {
                    val hadith = com.example.data.repository.MomentsProvider.getRandomHadith(
                        context = getApplication(),
                        excludeId = _currentHadithMoment.value.id
                    )
                    _currentHadithMoment.value = hadith
                }
                com.example.ui.components.MomentType.WORD -> {
                    val word = com.example.data.repository.MomentsProvider.getRandomWord(
                        excludeWord = _currentWordMoment.value.arabicWord
                    )
                    _currentWordMoment.value = word
                }
                com.example.ui.components.MomentType.DUA -> {
                    val dua = com.example.data.repository.MomentsProvider.getRandomDua(
                        excludeId = _currentDuaMoment.value.id
                    )
                    _currentDuaMoment.value = dua
                }
                null -> {
                    val ayah = com.example.data.repository.MomentsProvider.getRandomAyah(
                        context = getApplication(),
                        excludeGlobalNumber = _currentAyahMoment.value.number
                    )
                    val hadith = com.example.data.repository.MomentsProvider.getRandomHadith(
                        context = getApplication(),
                        excludeId = _currentHadithMoment.value.id
                    )
                    val word = com.example.data.repository.MomentsProvider.getRandomWord(
                        excludeWord = _currentWordMoment.value.arabicWord
                    )
                    val dua = com.example.data.repository.MomentsProvider.getRandomDua(
                        excludeId = _currentDuaMoment.value.id
                    )
                    _currentAyahMoment.value = ayah
                    _currentHadithMoment.value = hadith
                    _currentWordMoment.value = word
                    _currentDuaMoment.value = dua
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
    }
}
