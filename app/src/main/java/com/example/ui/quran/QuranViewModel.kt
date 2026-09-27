package com.example.ui.quran

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioPlaybackState
import com.example.audio.QuranAudioDownloadManager
import com.example.audio.QuranAudioPlayer
import com.example.data.local.AlDeenDatabase
import com.example.data.repository.BookmarksRepository
import com.example.data.repository.QuranRepository
import com.example.domain.model.Ayah
import com.example.domain.model.BookmarkItem
import com.example.domain.model.BookmarkType
import com.example.domain.model.JuzInfo
import com.example.domain.model.QuranReciter
import com.example.domain.model.QuranSearchMode
import com.example.domain.model.SUPPORTED_RECITERS
import com.example.domain.model.SearchLanguage
import com.example.domain.model.Surah
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class QuranSearchResultData(
    val surahs: List<Surah> = emptyList(),
    val ayahs: List<Pair<Surah, Ayah>> = emptyList()
)

class QuranViewModel(application: Application) : AndroidViewModel(application) {
    val quranRepository = QuranRepository(application)
    private val database = AlDeenDatabase.getInstance(application)
    val bookmarksRepository = BookmarksRepository(database.bookmarkDao())
    val audioPlayer = QuranAudioPlayer(application)
    val audioDownloadManager = QuranAudioDownloadManager(application)
    val translationManager = com.example.data.quran.QuranTranslationManager.getInstance(application)

    // Navigation state: Tracks whether "Al Quran" full section (Surah/Juz list) is open
    val isAlQuranOpened = MutableStateFlow(false)

    fun setAlQuranOpened(opened: Boolean) {
        isAlQuranOpened.value = opened
    }

    // Reciters and Audio downloads
    val supportedReciters: List<QuranReciter> = SUPPORTED_RECITERS

    private val _selectedReciter = MutableStateFlow<QuranReciter>(SUPPORTED_RECITERS.first())
    val selectedReciter: StateFlow<QuranReciter> = _selectedReciter.asStateFlow()

    val downloadedRecitersMap: StateFlow<Map<Int, Set<String>>> = audioDownloadManager.downloadedRecitersMap
    val downloadedSurahs: StateFlow<Set<Int>> = audioDownloadManager.downloadedSurahs
    val downloadingKeys: StateFlow<Set<String>> = audioDownloadManager.downloadingKeys
    val downloadProgressMap: StateFlow<Map<String, Float>> = audioDownloadManager.downloadProgressMap
    val downloadStatusMap: StateFlow<Map<String, String>> = audioDownloadManager.downloadStatusMap

    // Multi-Language Translation Management
    val isTranslationEnabled: StateFlow<Boolean> = translationManager.isTranslationEnabled
    val selectedTranslationId: StateFlow<String> = translationManager.selectedLanguageId
    val translationDownloadStatusMap: StateFlow<Map<String, com.example.data.quran.TranslationDownloadState>> = translationManager.downloadStatusMap
    val supportedTranslationLanguages = translationManager.supportedLanguages
    val translationRevision: StateFlow<Int> = translationManager.translationRevision

    fun toggleTranslation() {
        translationManager.toggleTranslationEnabled()
    }

    fun setTranslationEnabled(enabled: Boolean) {
        translationManager.setTranslationEnabled(enabled)
    }

    fun selectQuranTranslation(id: String) {
        translationManager.selectLanguage(id)
        viewModelScope.launch {
            translationManager.ensureSurahTranslationLoaded(_currentSurahNumber.value, id)
        }
    }

    fun ensureTranslationLoadedForSurah(surahNumber: Int, id: String) {
        viewModelScope.launch {
            translationManager.ensureSurahTranslationLoaded(surahNumber, id)
        }
    }

    fun downloadQuranTranslation(id: String) {
        translationManager.downloadTranslation(id)
    }

    fun deleteQuranTranslation(id: String) {
        translationManager.deleteTranslation(id)
    }

    fun isTranslationDownloaded(id: String): Boolean {
        return translationManager.isDownloaded(id)
    }

    fun getAyahTranslation(surahNumber: Int, ayahNumberInSurah: Int, fallbackUrdu: String, fallbackEnglish: String): String {
        return when (selectedTranslationId.value) {
            "ur.jalandhry" -> fallbackUrdu
            "en.sahih" -> {
                val fromMgr = translationManager.getTranslationForAyah(surahNumber, ayahNumberInSurah)
                if (!fromMgr.isNullOrBlank()) fromMgr else fallbackEnglish
            }
            else -> {
                translationManager.getTranslationForAyah(surahNumber, ayahNumberInSurah) ?: ""
            }
        }
    }

    fun setSelectedReciter(reciter: QuranReciter) {
        val wasPlaying = audioPlaybackState.value is AudioPlaybackState.Playing
        _selectedReciter.value = reciter
        // If playing current surah, switch stream/file to new reciter seamlessly
        if (wasPlaying) {
            val surah = currentSurah.value
            if (surah != null) {
                playSurahAudio(surah, reciter)
            }
        }
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _quranSearchMode = MutableStateFlow(QuranSearchMode.BY_WORD)
    val quranSearchMode: StateFlow<QuranSearchMode> = _quranSearchMode.asStateFlow()

    private val _quranSearchLanguage = MutableStateFlow(SearchLanguage.URDU)
    val quranSearchLanguage: StateFlow<SearchLanguage> = _quranSearchLanguage.asStateFlow()

    fun setQuranSearchMode(mode: QuranSearchMode) {
        _quranSearchMode.value = mode
    }

    fun setQuranSearchLanguage(lang: SearchLanguage) {
        _quranSearchLanguage.value = lang
    }

    private val _selectedTab = MutableStateFlow(0) // 0: Surah, 1: Juz
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _searchProgress = MutableStateFlow(0)
    val searchProgress: StateFlow<Int> = _searchProgress.asStateFlow()

    private val _searchResults = MutableStateFlow(QuranSearchResultData(surahs = quranRepository.searchQuran("")))
    val surahs: StateFlow<List<Surah>> = _searchResults.map { it.surahs }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), quranRepository.searchQuran(""))
    val ayahSearchResults: StateFlow<List<Pair<Surah, Ayah>>> = _searchResults.map { it.ayahs }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Preload complete Quran on background IO thread on ViewModel startup so search is instantaneous
        viewModelScope.launch(Dispatchers.IO) {
            com.example.data.repository.OfflineQuranProvider.preload(getApplication())
        }

        viewModelScope.launch {
            combine(
                _searchQuery.debounce(150L),
                _quranSearchMode,
                _quranSearchLanguage
            ) { query, mode, lang ->
                Triple(query, mode, lang)
            }.collectLatest { (query, mode, lang) ->
                if (query.isBlank()) {
                    _isSearching.value = false
                    _searchProgress.value = 0
                    _searchResults.value = QuranSearchResultData(
                        surahs = quranRepository.searchQuran(""),
                        ayahs = emptyList()
                    )
                } else {
                    _isSearching.value = true
                    _searchProgress.value = 15
                    val progressJob = launch(Dispatchers.Main) {
                        while (_isSearching.value && _searchProgress.value < 90) {
                            kotlinx.coroutines.delay(50)
                            if (_searchProgress.value < 90) {
                                _searchProgress.value = (_searchProgress.value + 20).coerceAtMost(90)
                            }
                        }
                    }
                    val trimmed = query.trim()
                    val resultData = withContext(Dispatchers.IO) {
                        val isAyahSearch = (mode == QuranSearchMode.BY_AYAH)
                        val ayahs = quranRepository.searchAyahsOffline(
                            context = getApplication(),
                            query = trimmed,
                            searchByAyah = isAyahSearch,
                            langCode = if (isAyahSearch) "all" else lang.code,
                            translationManager = translationManager
                        )

                        val matchingSurahs = if (isAyahSearch) {
                            val colonRegex = Regex("^(\\d{1,3})\\s*[:\\s-]\\s*(\\d{1,3})$")
                            val colonMatch = colonRegex.find(trimmed)
                            if (colonMatch != null) {
                                val sNum = colonMatch.groupValues[1].toIntOrNull() ?: 1
                                quranRepository.searchQuran("").filter { it.number == sNum }
                            } else {
                                val num = trimmed.toIntOrNull()
                                if (num != null) {
                                    val matchingSurahNumber = quranRepository.searchQuran("").filter { it.number == num }
                                    if (matchingSurahNumber.isNotEmpty()) {
                                        matchingSurahNumber
                                    } else {
                                        quranRepository.searchQuran("").filter { it.numberOfAyahs >= num }
                                    }
                                } else {
                                    quranRepository.searchQuran(trimmed)
                                }
                            }
                        } else {
                            val directMatches = quranRepository.searchQuran(trimmed)
                            val directNumbers = directMatches.map { it.number }.toSet()
                            val surahsFromAyahs = ayahs.map { it.first.number }.toSet()
                            val allSurahs = quranRepository.searchQuran("")
                            val additionalSurahs = allSurahs.filter { it.number in surahsFromAyahs && it.number !in directNumbers }
                            directMatches + additionalSurahs
                        }

                        QuranSearchResultData(surahs = matchingSurahs, ayahs = ayahs)
                    }
                    progressJob.cancel()
                    _searchProgress.value = 100
                    _searchResults.value = resultData
                    _isSearching.value = false
                }
            }
        }
    }

    val juzList: List<JuzInfo> = quranRepository.getAllJuz()

    // Selected Juz for BottomSheet
    private val _activeJuzForSheet = MutableStateFlow<JuzInfo?>(null)
    val activeJuzForSheet: StateFlow<JuzInfo?> = _activeJuzForSheet.asStateFlow()

    // Active Reader State
    private val _currentSurahNumber = MutableStateFlow(1)
    val currentSurahNumber: StateFlow<Int> = _currentSurahNumber.asStateFlow()

    private val _isOpenedFromPara = MutableStateFlow(false)
    val isOpenedFromPara: StateFlow<Boolean> = _isOpenedFromPara.asStateFlow()

    val currentSurah: StateFlow<Surah?> = _currentSurahNumber.map { num ->
        quranRepository.searchQuran("").firstOrNull { it.number == num }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentAyahs: StateFlow<List<Ayah>> = combine(_currentSurahNumber, selectedTranslationId, translationRevision) { num, transId, _ ->
        val langCode = supportedTranslationLanguages.firstOrNull { it.id == transId }?.languageCode ?: "ur"
        Pair(num, langCode)
    }.flatMapLatest { (num, langCode) ->
        quranRepository.getAyahsForSurah(num, langCode)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookmarkedRefs: StateFlow<Set<String>> = bookmarksRepository.allBookmarks
        .map { bookmarks -> bookmarks.map { it.referenceId }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val audioPlaybackState: StateFlow<AudioPlaybackState> = audioPlayer.playbackState

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun onTabSelected(index: Int) {
        _selectedTab.value = index
    }

    fun setSelectedTab(index: Int) {
        _selectedTab.value = index
    }

    fun openJuzSheet(juz: JuzInfo) {
        _activeJuzForSheet.value = juz
    }

    fun closeJuzSheet() {
        _activeJuzForSheet.value = null
    }

    fun openSurah(surahNumber: Int, fromPara: Boolean = false) {
        _currentSurahNumber.value = surahNumber
        _isOpenedFromPara.value = fromPara
        viewModelScope.launch {
            translationManager.ensureSurahTranslationLoaded(surahNumber, selectedTranslationId.value)
        }
    }

    fun goToNextSurah() {
        if (_currentSurahNumber.value < 114) {
            _currentSurahNumber.value += 1
        }
    }

    fun goToPreviousSurah() {
        if (_currentSurahNumber.value > 1) {
            _currentSurahNumber.value -= 1
        }
    }

    fun nextSurah() {
        goToNextSurah()
    }

    fun playSurahAudio(surah: Surah, reciter: QuranReciter = _selectedReciter.value) {
        // Strict user mandate: Full Surah audio must NOT play without downloading first!
        if (!isReciterDownloaded(surah.number, reciter.id)) {
            return
        }

        val trackId = "surah_${surah.number}_${reciter.id}"
        val legacyTrackId = "surah_${surah.number}"

        // Check if already playing this surah and reciter -> toggle pause
        when (val state = audioPlaybackState.value) {
            is AudioPlaybackState.Playing -> {
                if (state.trackId == trackId || (reciter.id == "ar.alafasy" && state.trackId == legacyTrackId)) {
                    audioPlayer.pause()
                    return
                }
            }
            is AudioPlaybackState.Paused -> {
                if (state.trackId == trackId || (reciter.id == "ar.alafasy" && state.trackId == legacyTrackId)) {
                    val localFile = audioDownloadManager.getLocalAudioFile(surah.number, reciter.id)
                    if (localFile != null && localFile.exists()) {
                        audioPlayer.play(localFile.absolutePath, trackId)
                    }
                    return
                }
            }
            else -> {}
        }

        // Play downloaded audio file exclusively
        val localFile = audioDownloadManager.getLocalAudioFile(surah.number, reciter.id)
        if (localFile != null && localFile.exists()) {
            audioPlayer.play(localFile.absolutePath, trackId)
        }
    }

    fun downloadReciterAudio(surahNumber: Int, reciter: QuranReciter) {
        audioDownloadManager.startBackgroundDownload(surahNumber, reciter)
    }

    fun downloadSelectedReciters(surahNumber: Int, reciters: List<QuranReciter>) {
        audioDownloadManager.startBackgroundDownloadSelected(surahNumber, reciters)
    }

    fun downloadAllReciters(surahNumber: Int) {
        audioDownloadManager.startBackgroundDownloadSelected(surahNumber, SUPPORTED_RECITERS)
    }

    fun deleteReciterAudio(surahNumber: Int, reciterId: String) {
        val currentPlayback = audioPlaybackState.value
        val trackId = "surah_${surahNumber}_${reciterId}"
        if ((currentPlayback is AudioPlaybackState.Playing && currentPlayback.trackId == trackId) ||
            (currentPlayback is AudioPlaybackState.Paused && currentPlayback.trackId == trackId)) {
            audioPlayer.stop()
        }
        audioDownloadManager.deleteReciterAudio(surahNumber, reciterId)
    }

    fun deleteAllRecitersAudio(surahNumber: Int) {
        val currentPlayback = audioPlaybackState.value
        if ((currentPlayback is AudioPlaybackState.Playing && currentPlayback.trackId.startsWith("surah_${surahNumber}")) ||
            (currentPlayback is AudioPlaybackState.Paused && currentPlayback.trackId.startsWith("surah_${surahNumber}"))) {
            audioPlayer.stop()
        }
        audioDownloadManager.deleteAllRecitersAudio(surahNumber)
    }

    fun isReciterDownloaded(surahNumber: Int, reciterId: String): Boolean {
        return audioDownloadManager.isReciterDownloaded(surahNumber, reciterId)
    }

    fun areAllRecitersDownloaded(surahNumber: Int): Boolean {
        return audioDownloadManager.areAllRecitersDownloaded(surahNumber)
    }

    fun isReciterDownloading(surahNumber: Int, reciterId: String): Boolean {
        return audioDownloadManager.isReciterDownloading(surahNumber, reciterId)
    }

    fun isSurahDownloaded(surahNumber: Int): Boolean {
        return audioDownloadManager.isDownloaded(surahNumber)
    }

    fun playAyahAudio(ayah: Ayah) {
        ayah.audioUrl?.let { url ->
            audioPlayer.play(url, "ayah_${ayah.number}")
        }
    }

    fun toggleAyahBookmark(ayah: Ayah, surah: Surah) {
        val ref = "surah_${surah.number}_ayah_${ayah.numberInSurah}"
        viewModelScope.launch {
            if (bookmarkedRefs.value.contains(ref)) {
                bookmarksRepository.removeBookmark(ref)
            } else {
                bookmarksRepository.toggleBookmark(
                    BookmarkItem(
                        type = BookmarkType.QURAN_AYAH,
                        referenceId = ref,
                        title = "${surah.englishName} (${surah.urduName})",
                        subtitle = "Ayah ${ayah.numberInSurah}",
                        arabicText = ayah.textArabic,
                        translation = ayah.urduTranslation.ifBlank { ayah.textTranslation }
                    )
                )
            }
        }
    }

    fun isAyahBookmarked(surahNumber: Int, ayahNumberInSurah: Int): Boolean {
        val ref = "surah_${surahNumber}_ayah_${ayahNumberInSurah}"
        return bookmarkedRefs.value.contains(ref)
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
    }
}
