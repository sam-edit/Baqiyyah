package com.example.ui.donate

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.BankConfig
import com.example.data.repository.DonateRepository
import coil.ImageLoader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class DonateUiState {
    object Loading : DonateUiState()
    data class Success(val banks: List<BankConfig>) : DonateUiState()
    data class Empty(val message: String = "Currently no bank accounts configured.") : DonateUiState()
}

class DonateViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val donateRepository = DonateRepository(application.applicationContext)

    private val _uiState = MutableStateFlow<DonateUiState>(DonateUiState.Loading)
    val uiState: StateFlow<DonateUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private var fetchJob: Job? = null
    private var session: DonateRepository.Session? = null
    private val _imageLoader = MutableStateFlow<ImageLoader?>(null)
    val imageLoader: StateFlow<ImageLoader?> = _imageLoader.asStateFlow()
    private var currentBanks: List<BankConfig> = emptyList()

    /** Offline: use the most recently saved banks and disk-cached QR images only. */
    fun showOfflineCache() {
        stopListeningAndGoOffline()
        currentBanks = donateRepository.getCachedBanks()
        _uiState.value = if (currentBanks.isNotEmpty()) {
            val active = currentBanks.filter { it.isEnabled }
            DonateUiState.Success(active.ifEmpty { currentBanks })
        } else DonateUiState.Empty("No saved bank accounts available offline.")
        _imageLoader.value = ImageLoader.Builder(getApplication<Application>())
            .networkCachePolicy(coil.request.CachePolicy.DISABLED)
            .build()
        _isRefreshing.value = false
    }

    /**
     * Called only while the Donate screen is on screen (see MainActivity). One REST request per visit,
     * no live listener: a 304 answer (nothing changed) downloads no data at all.
     */
    fun startListening() {
        if (session != null) return
        val visit = donateRepository.openSession()
        session = visit
        // QR previews and thumbnails share this visit's cancellable transport.
        _imageLoader.value = ImageLoader.Builder(getApplication<Application>())
            .okHttpClient(visit.httpClient)
            .build()
        currentBanks = donateRepository.getCachedBanks()
        if (currentBanks.isNotEmpty()) {
            val active = currentBanks.filter { it.isEnabled }
            _uiState.value = if (active.isNotEmpty()) DonateUiState.Success(active)
                else DonateUiState.Success(currentBanks)
        } else {
            _uiState.value = DonateUiState.Loading
        }
        fetch(visit)
    }

    private fun fetch(visit: DonateRepository.Session) {
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val banks = donateRepository.fetchBanksViaRest(visit)
                if (session === visit) {
                    when {
                        // 304: the cache shown above is already current.
                        banks == null -> Unit
                        banks.isNotEmpty() -> {
                            handleFreshBanks(banks)
                            donateRepository.commitEtag()
                        }
                        currentBanks.isEmpty() -> _uiState.value = DonateUiState.Empty()
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                if (session === visit && currentBanks.isEmpty()) {
                    _uiState.value = DonateUiState.Empty("Unable to load bank accounts. Tap refresh to retry.")
                }
            } finally {
                if (session === visit) _isRefreshing.value = false
            }
        }
    }

    /**
     * Compares values and image URLs against current banks.
     * If all values match: retains current without flicker.
     * If any value or image URL changed: updates state and saves to cache.
     */
    private fun handleFreshBanks(freshBanks: List<BankConfig>) {
        val (mergedList, hasChanges) = donateRepository.mergeAndCheckChanges(currentBanks, freshBanks)

        if (hasChanges || currentBanks.isEmpty()) {
            currentBanks = mergedList
            donateRepository.saveCachedBanks(mergedList)

            val active = mergedList.filter { it.isEnabled }
            _uiState.value = when {
                active.isNotEmpty() -> DonateUiState.Success(active)
                mergedList.isNotEmpty() -> DonateUiState.Success(mergedList)
                else -> DonateUiState.Empty()
            }
        }
    }

    fun stopListeningAndGoOffline() {
        val visit = session
        session = null // Invalidate callbacks before cancelling any work.
        if (visit != null) donateRepository.closeSession(visit)
        fetchJob?.cancel()
        fetchJob = null
        _imageLoader.value?.shutdown()
        _imageLoader.value = null
        _isRefreshing.value = false
    }

    fun refresh() {
        val visit = session ?: return
        fetch(visit)
    }

    override fun onCleared() {
        stopListeningAndGoOffline()
        super.onCleared()
    }
}
