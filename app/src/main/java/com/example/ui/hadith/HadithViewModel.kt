package com.example.ui.hadith

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.hadith.HadithDownloadManager
import com.example.data.hadith.HadithDownloadState
import com.example.data.hadith.HadithLanguage
import com.example.data.local.AlDeenDatabase
import com.example.data.repository.BookmarksRepository
import com.example.data.repository.HadithRepository
import com.example.domain.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HadithViewModel(application: Application) : AndroidViewModel(application) {
    val downloadManager = HadithDownloadManager.getInstance(application)
    val hadithRepository = HadithRepository(application, downloadManager)
    private val database = AlDeenDatabase.getInstance(application)
    val bookmarksRepository = BookmarksRepository(database.bookmarkDao())

    val downloadStatusMap: StateFlow<Map<String, HadithDownloadState>> = downloadManager.downloadStatusMap
    val languageDownloadStatusMap: StateFlow<Map<String, HadithDownloadState>> = downloadManager.languageDownloadStatusMap
    val isTranslationEnabled: StateFlow<Boolean> = downloadManager.isTranslationEnabled
    val selectedLanguageCode: StateFlow<String> = downloadManager.selectedLanguageCode
    val supportedLanguages: List<HadithLanguage> = downloadManager.supportedLanguages

    fun getLanguagesForBook(bookSlug: String): List<HadithLanguage> =
        downloadManager.getLanguagesForBook(bookSlug)

    val books: List<HadithBook> = hadithRepository.getAllBooks()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _hadithMode = MutableStateFlow(HadithSearchMode.BY_WORD)
    val hadithMode: StateFlow<HadithSearchMode> = _hadithMode.asStateFlow()

    private val _searchLanguage = MutableStateFlow(SearchLanguage.URDU)
    val searchLanguage: StateFlow<SearchLanguage> = _searchLanguage.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _searchProgress = MutableStateFlow(0)
    val searchProgress: StateFlow<Int> = _searchProgress.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Hadith>>(emptyList())
    val searchResults: StateFlow<List<Hadith>> = _searchResults.asStateFlow()

    // Book Selection Filter for Home & Search (null = All Books)
    private val _selectedBookFilter = MutableStateFlow<HadithBook?>(null)
    val selectedBookFilter: StateFlow<HadithBook?> = _selectedBookFilter.asStateFlow()

    fun setSelectedBookFilter(book: HadithBook?) {
        _selectedBookFilter.value = book
        if (book != null) {
            _selectedBook.value = book
        }
    }

    private var searchJob: kotlinx.coroutines.Job? = null

    data class HadithSearchRequest(
        val query: String,
        val mode: HadithSearchMode,
        val lang: SearchLanguage,
        val bookFilter: HadithBook?
    )

    init {
        viewModelScope.launch {
            @OptIn(kotlinx.coroutines.FlowPreview::class)
            combine(
                _searchQuery.debounce(150L),
                _hadithMode,
                _searchLanguage,
                _selectedBookFilter
            ) { query, mode, lang, bookFilter ->
                HadithSearchRequest(query, mode, lang, bookFilter)
            }.collectLatest { req ->
                val query = req.query.trim()
                if (query.isBlank()) {
                    _searchResults.value = emptyList()
                    _searchProgress.value = 0
                    _isSearching.value = false
                } else {
                    _isSearching.value = true
                    _searchResults.value = emptyList()
                    _searchProgress.value = 10
                    try {
                        val isNum = (req.mode == HadithSearchMode.BY_NUMBER) || (query.toIntOrNull() != null)
                        hadithRepository.searchDownloadedHadithsFlow(
                            query = query,
                            searchByNumber = isNum,
                            langCode = req.lang.code,
                            bookSlug = req.bookFilter?.slug
                        ).collect { batch ->
                            _searchResults.value = batch.results
                            _searchProgress.value = batch.progress
                            if (batch.isComplete) {
                                _isSearching.value = false
                            }
                        }
                        _searchProgress.value = 100
                        _isSearching.value = false
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        // Job was cancelled by a newer query input - ignore
                        throw e
                    } catch (_: Exception) {
                        _searchResults.value = emptyList()
                        _searchProgress.value = 100
                        _isSearching.value = false
                    }
                }
            }
        }
    }

    // Active Book Selection
    private val _selectedBook = MutableStateFlow(books.first())
    val selectedBook: StateFlow<HadithBook> = _selectedBook.asStateFlow()

    // Chapters for Active Book, observing downloadStatusMap to auto-refresh when book is downloaded/deleted
    val chapters: StateFlow<List<HadithChapter>> = combine(_selectedBook, downloadStatusMap) { book, _ ->
        hadithRepository.getChaptersForBook(book.slug)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Chapter Selection
    private val _selectedChapter = MutableStateFlow<HadithChapter?>(null)
    val selectedChapter: StateFlow<HadithChapter?> = _selectedChapter.asStateFlow()

    // Ahadith for Active Chapter (recomputes when book, chapter, download status, active language, or translation revision changes)
    val hadithsForChapter: StateFlow<List<Hadith>> = combine(
        _selectedBook,
        _selectedChapter,
        selectedLanguageCode,
        languageDownloadStatusMap,
        downloadManager.translationRevision
    ) { book, chapter, langCode, _, _ ->
        if (chapter != null) {
            hadithRepository.clearCacheForBook(book.slug)
            hadithRepository.invalidateTranslationCache(book.slug, langCode)
            hadithRepository.getHadithsForChapter(book.slug, chapter.chapterNumber, langCode)
        } else {
            emptyList()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookmarkedRefs: StateFlow<Set<String>> = bookmarksRepository.allBookmarks
        .map { list -> list.map { it.referenceId }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    fun isBookDownloaded(bookSlug: String): Boolean = downloadManager.isBookDownloaded(bookSlug)

    fun isLanguageDownloaded(bookSlug: String, langCode: String): Boolean =
        downloadManager.isLanguageDownloaded(bookSlug, langCode)

    fun downloadBook(bookSlug: String) {
        downloadManager.downloadBook(bookSlug)
    }

    fun deleteBook(bookSlug: String) {
        hadithRepository.clearCacheForBook(bookSlug)
        downloadManager.deleteBook(bookSlug)
    }

    fun downloadLanguage(bookSlug: String, langCode: String) {
        downloadManager.downloadLanguage(bookSlug, langCode)
    }

    fun deleteLanguage(bookSlug: String, langCode: String) {
        hadithRepository.clearCacheForBook(bookSlug)
        downloadManager.deleteLanguage(bookSlug, langCode)
    }

    fun setTranslationEnabled(enabled: Boolean) {
        downloadManager.setTranslationEnabled(enabled)
    }

    fun toggleTranslation() {
        downloadManager.toggleTranslation()
    }

    fun selectLanguage(langCode: String) {
        downloadManager.selectLanguage(langCode)
    }

    fun downloadLanguageAndSync(bookSlug: String, langCode: String) {
        val norm = downloadManager.normalizeLang(langCode)
        downloadManager.downloadLanguage(bookSlug, norm)
        downloadManager.selectLanguage(norm)
        downloadManager.setTranslationEnabled(true)
        val matchedSearchLang = SearchLanguage.hadithLanguages.firstOrNull {
            downloadManager.normalizeLang(it.code) == norm
        }
        if (matchedSearchLang != null) {
            _searchLanguage.value = matchedSearchLang
        }
    }

    fun selectLanguageAndSync(lang: SearchLanguage) {
        setSearchLanguage(lang)
        val norm = downloadManager.normalizeLang(lang.code)
        downloadManager.selectLanguage(norm)
        downloadManager.setTranslationEnabled(true)
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setHadithMode(mode: HadithSearchMode) {
        _hadithMode.value = mode
    }

    fun setSearchLanguage(lang: SearchLanguage) {
        _searchLanguage.value = lang
    }

    fun selectBook(book: HadithBook) {
        _selectedBook.value = book
    }

    fun selectChapter(chapter: HadithChapter) {
        _selectedChapter.value = chapter
        val currentLang = selectedLanguageCode.value
        if (!downloadManager.isLanguageDownloaded(chapter.bookSlug, currentLang) &&
            !downloadManager.isLanguageDownloading(chapter.bookSlug, currentLang)
        ) {
            downloadManager.downloadLanguage(chapter.bookSlug, currentLang)
        }
    }

    fun openHadith(bookSlug: String, chapterNumber: Int, hadithNumber: String): HadithChapter {
        val book = books.firstOrNull { it.slug == bookSlug } ?: selectedBook.value
        selectBook(book)
        val chapter = hadithRepository.getChapterForHadith(bookSlug, chapterNumber, hadithNumber)
        selectChapter(chapter)
        return chapter
    }

    fun toggleHadithBookmark(hadith: Hadith) {
        val ref = "hadith_${hadith.bookSlug}_${hadith.hadithNumber}"
        viewModelScope.launch {
            if (bookmarkedRefs.value.contains(ref)) {
                bookmarksRepository.removeBookmark(ref)
            } else {
                bookmarksRepository.toggleBookmark(
                    BookmarkItem(
                        type = BookmarkType.HADITH,
                        referenceId = ref,
                        title = "${hadith.bookName} #${hadith.hadithNumber}",
                        subtitle = hadith.chapterNameUrdu,
                        arabicText = hadith.arabicText,
                        translation = if (hadith.activeTranslation.isNotBlank()) hadith.activeTranslation else hadith.urduTranslation
                    )
                )
            }
        }
    }

    fun isHadithBookmarked(bookSlug: String, hadithNumber: String): Boolean {
        val ref = "hadith_${bookSlug}_${hadithNumber}"
        return bookmarkedRefs.value.contains(ref)
    }
}
