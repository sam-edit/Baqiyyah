package com.example.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.audio.AudioPlaybackState
import com.example.ui.components.*
import com.example.ui.theme.AlDeenTokens
import com.example.ui.theme.staggeredSlideFade

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenDrawer: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSurah: (Int) -> Unit = {},
    onOpenAyah: (Int, Int) -> Unit = { _, _ -> },
    onOpenHadithBook: (String) -> Unit = {},
    onOpenHadith: (String, Int, String) -> Unit = { _, _, _ -> },
    onOpenCalculationSettings: () -> Unit = {},
    isDrawerOpen: Boolean = false,
    bannerState: com.example.data.repository.BannerState = com.example.data.repository.BannerState(),
    modifier: Modifier = Modifier
) {
    val location by viewModel.userLocation.collectAsStateWithLifecycle()
    val primaryLocation by viewModel.primaryLocation.collectAsStateWithLifecycle()
    val schedule by viewModel.dailySchedule.collectAsStateWithLifecycle()
    val prayerState by viewModel.prayerState.collectAsStateWithLifecycle()
    val audioState by viewModel.audioPlayer.playbackState.collectAsStateWithLifecycle()
    val bookmarkedRefs by viewModel.bookmarkedReferences.collectAsStateWithLifecycle()

    val currentAyah by viewModel.currentAyahMoment.collectAsStateWithLifecycle()
    val currentHadith by viewModel.currentHadithMoment.collectAsStateWithLifecycle()
    val currentWord by viewModel.currentWordMoment.collectAsStateWithLifecycle()
    val currentDua by viewModel.currentDuaMoment.collectAsStateWithLifecycle()
    val activeAyahTranslation by viewModel.activeAyahTranslation.collectAsStateWithLifecycle()
    val activeHadithTranslation by viewModel.activeHadithTranslation.collectAsStateWithLifecycle()
    val juristicSchool by viewModel.juristicSchool.collectAsStateWithLifecycle()
    val calculationMethod by viewModel.calculationMethod.collectAsStateWithLifecycle()

    // Automatically change/refresh moments whenever HomeScreen is entered from any section or tab
    LaunchedEffect(Unit) {
        viewModel.refreshMoments()
    }

    // Refresh moments whenever screen resumes (user navigated back from another screen, or reopened app)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshMoments()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var showCityPicker by remember { mutableStateOf(false) }
    val isAudioPlaying = audioState is AudioPlaybackState.Playing

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("home_screen_root")
    ) {
        // 1. Header: Top-left: [Drawer Icon] Al Deen. Top-right: Location control
        AlDeenHeader(
            location = location,
            onMenuClick = onOpenDrawer,
            onLocationClick = { showCityPicker = true },
            isMenuOpen = isDrawerOpen
        )

        // Scrollable content in exact requested order
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AlDeenTokens.SpacingLarge)
                .padding(bottom = AlDeenTokens.SpacingXXLarge)
        ) {
            // 2. Prayer Dashboard
            PrayerDashboardCard(
                prayerState = prayerState,
                formattedCurrentTime = viewModel.formatTime(prayerState.currentPrayerTime),
                formattedCurrentStart = viewModel.formatTime(prayerState.currentPrayerStart),
                formattedUpcomingTime = viewModel.formatTime(prayerState.upcomingPrayerTime),
                onToggleQuietMode = { viewModel.toggleQuietMode() },
                modifier = Modifier.staggeredSlideFade(0)
            )

            // Calculation method disclaimer directly below Dashboard
            CalculationMethodNote(
                method = calculationMethod,
                onClick = onOpenCalculationSettings,
                modifier = Modifier
                    .padding(top = 6.dp)
                    .staggeredSlideFade(0)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Search Bar directly below Dashboard
            HomeSearchBarTrigger(
                onClick = onOpenSearch,
                modifier = Modifier.staggeredSlideFade(1)
            )

            Spacer(modifier = Modifier.height(AlDeenTokens.SpacingLarge))

            // 3. Today Prayer Times
            TodayPrayerTimesCard(
                schedule = schedule,
                activePrayer = prayerState.currentPrayer,
                formatTime = { viewModel.formatTime(it) },
                juristicSchool = juristicSchool,
                onToggleJuristicSchool = { viewModel.toggleJuristicSchool() },
                modifier = Modifier.staggeredSlideFade(2)
            )

            Spacer(modifier = Modifier.height(AlDeenTokens.SpacingLarge))

            // 4. Special Three-Card Row (Suhur End, Iftar, Tahajjud)
            SpecialThreeCardsRow(
                schedule = schedule,
                formatTime = { viewModel.formatTime(it) },
                modifier = Modifier.staggeredSlideFade(3)
            )

            Spacer(modifier = Modifier.height(AlDeenTokens.SpacingLarge))

            // 5. Forbidden Salat Times Card (Sunrise, Zawal, Sunset)
            ForbiddenSalatTimesCard(
                schedule = schedule,
                formatTime = { viewModel.formatTime(it) },
                modifier = Modifier.staggeredSlideFade(4)
            )

            Spacer(modifier = Modifier.height(AlDeenTokens.SpacingLarge))

            // 6 & 7. Home Moment Menu & Home Moment Content
            HomeMomentSection(
                onPlayAudio = { url, id -> viewModel.playAudio(url, id) },
                isPlayingAudio = isAudioPlaying,
                onToggleBookmark = { viewModel.toggleBookmark(it) },
                isBookmarked = { viewModel.isBookmarked(it) },
                ayah = currentAyah,
                hadith = currentHadith,
                word = currentWord,
                dua = currentDua,
                onRefreshMoment = { viewModel.refreshMoments(it) },
                ayahTranslation = activeAyahTranslation,
                hadithTranslation = activeHadithTranslation,
                modifier = Modifier.staggeredSlideFade(5)
            )
            AppConfigBanner(
                state = bannerState,
                modifier = Modifier
                    .padding(top = AlDeenTokens.SpacingLarge)
                    .staggeredSlideFade(6)
            )
            Spacer(modifier = Modifier.height(AlDeenTokens.SpacingXXLarge))
        }
    }

    if (showCityPicker) {
        CityPickerDialog(
            currentLocation = location,
            primaryLocation = primaryLocation,
            onLocationSelected = { viewModel.updateLocation(it) },
            onSetAsPrimaryLocation = { viewModel.setPrimaryLocation(it) },
            onClearPrimaryLocation = { viewModel.clearPrimaryLocation() },
            onUseCurrentLocation = { viewModel.detectAndUseCurrentLocation() },
            onDismissRequest = { showCityPicker = false }
        )
    }
}
