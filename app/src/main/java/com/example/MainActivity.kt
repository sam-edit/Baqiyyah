package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.repository.AuthoritativeContentProvider
import com.example.data.repository.SettingsRepository
import com.example.domain.model.HadithBook
import com.example.domain.model.HadithChapter
import com.example.ui.components.AlDeenBottomNav
import com.example.ui.components.AlDeenDrawerContent
import com.example.ui.components.CircularRevealDrawer
import com.example.ui.components.NavTab
import com.example.ui.theme.AlDeenMotion
import com.example.ui.goals.GoalsScreen
import com.example.ui.goals.GoalsViewModel
import com.example.ui.hadith.BaabListScreen
import com.example.ui.hadith.HadithReaderScreen
import com.example.ui.hadith.HadithScreen
import com.example.ui.hadith.HadithViewModel
import com.example.ui.home.HomeScreen
import com.example.ui.home.HomeViewModel
import com.example.ui.more.*
import com.example.ui.quran.QuranScreen
import com.example.ui.quran.QuranViewModel
import com.example.ui.quran.SurahReaderScreen
import com.example.ui.settings.AboutUsScreen
import com.example.ui.settings.ContactUsScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.AlDeenFontRegistry
import com.example.ui.theme.AlDeenTheme
import com.example.ui.donate.DonateUsScreen
import kotlinx.coroutines.launch

sealed class AppScreen : java.io.Serializable {
    object MainTab : AppScreen()
    object UniversalSearch : AppScreen()
    object Quran16 : AppScreen()
    data class SurahReader(val surahNumber: Int, val scrollToAyah: Int? = null) : AppScreen()
    data class HadithBaabList(val book: HadithBook) : AppScreen()
    data class HadithReader(val chapter: HadithChapter, val scrollToHadith: String? = null) : AppScreen()
    object ShareHappiness : AppScreen()
    object Qibla : AppScreen()
    data class Tasbeeh(val initialPhraseId: String? = null) : AppScreen()
    object Calendar : AppScreen()
    object AllahNames : AppScreen()
    object Bookmarks : AppScreen()
    object Duas : AppScreen()
    object Settings : AppScreen()
    object AboutUs : AppScreen()
    object ContactUs : AppScreen()
    object DonateUs : AppScreen()
}

class MainActivity : ComponentActivity() {
    private val widgetNavigationTrigger = mutableStateOf<Pair<String, String?>?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            com.example.ui.quran.FloatingAudioButtonHolder.resetToDefault()
        }
        handleWidgetIntent(intent)
        AlDeenFontRegistry.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            val settingsRepo = remember { SettingsRepository.getInstance(applicationContext) }
            val themeMode by settingsRepo.themeMode.collectAsStateWithLifecycle()

            AlDeenTheme(themeMode = themeMode) {
                AlDeenApp(settingsRepo, widgetNavigationTrigger)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        com.example.notification.PrayerNotificationManager.rescheduleAlarmsFromSavedState(this)
        try {
            com.example.widget.PrayerWidgetProvider.updateAllWidgets(this)
            com.example.widget.PrayerWidgetProvider.scheduleNextBoundaryUpdate(this)
        } catch (_: Exception) {}
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleWidgetIntent(intent)
    }

    private fun handleWidgetIntent(intent: android.content.Intent?) {
        val nav = intent?.getStringExtra("navigate_to")
        if (nav != null) {
            val phraseId = intent.getStringExtra("selected_phrase_id")
            widgetNavigationTrigger.value = Pair(nav, phraseId)
        }
    }

    // NOTE: the streaming cache is intentionally NOT cleared here. The activity is destroyed on
    // back-press / rotation while audio keeps playing from QuranAudioService; wiping the cache now
    // would pull it out from under the player. The cache is cleared by QuranAudioService
    // (onTaskRemoved / onDestroy = app closed from Recents) and on the next app start.
}

@Composable
fun AlDeenApp(
    settingsRepository: SettingsRepository,
    widgetNavigationTrigger: MutableState<Pair<String, String?>?>? = null
) {
    val context = LocalContext.current
    var isDrawerOpen by rememberSaveable { mutableStateOf(false) }
    // True only when Settings is opened from the Home "Tap to change" note
    var highlightCalcMethod by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    var currentTab by rememberSaveable { mutableStateOf(NavTab.HOME) }
    var currentScreen by rememberSaveable { mutableStateOf<AppScreen>(AppScreen.MainTab) }
    var showCityPicker by rememberSaveable { mutableStateOf(false) }
    var isTopSupportStripeVisible by rememberSaveable { mutableStateOf(true) }

    // Observe widget navigation requests (e.g. user tapped the home screen widget)
    LaunchedEffect(widgetNavigationTrigger?.value) {
        val trigger = widgetNavigationTrigger?.value
        if (trigger != null) {
            val (destination, param) = trigger
            when (destination) {
                "tasbeeh" -> {
                    currentScreen = AppScreen.Tasbeeh(param)
                }
                "prayer_dashboard" -> {
                    currentScreen = AppScreen.MainTab
                    currentTab = NavTab.HOME
                }
                "city_picker" -> {
                    currentScreen = AppScreen.MainTab
                    currentTab = NavTab.HOME
                    showCityPicker = true
                }
                "quran" -> {
                    currentScreen = AppScreen.MainTab
                    currentTab = NavTab.QURAN
                }
            }
            widgetNavigationTrigger.value = null
        }
    }

    val homeViewModel: HomeViewModel = viewModel()
    val quranViewModel: QuranViewModel = viewModel()
    val hadithViewModel: HadithViewModel = viewModel()
    val goalsViewModel: GoalsViewModel = viewModel()
    val donateViewModel: com.example.ui.donate.DonateViewModel = viewModel()
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val isScreenActive = currentScreen is AppScreen.DonateUs
    val bannerOnline = com.example.ui.components.rememberBannerOnline()
    val startupBanners = com.example.ui.components.rememberStartupBanners(bannerOnline)
    val homepageBanner = startupBanners.home
    val moreBanner = startupBanners.more
    val sideLoaderBanner = startupBanners.side
    val popupBanner = startupBanners.popup

    // AnimatedContent retains an outgoing screen until its animation finishes.
    // Gate by the actual destination as well as the activity's resumed state.
    DisposableEffect(donateViewModel, lifecycleOwner, isScreenActive, bannerOnline) {
        val lifecycle = lifecycleOwner.lifecycle
        fun updateConnection() {
            if (isScreenActive && lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) {
                if (bannerOnline) donateViewModel.startListening()
                else donateViewModel.showOfflineCache()
            } else {
                donateViewModel.stopListeningAndGoOffline()
            }
        }
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> updateConnection()
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE, androidx.lifecycle.Lifecycle.Event.ON_STOP, androidx.lifecycle.Lifecycle.Event.ON_DESTROY ->
                    donateViewModel.stopListeningAndGoOffline()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        updateConnection()
        onDispose {
            lifecycle.removeObserver(observer)
            donateViewModel.stopListeningAndGoOffline()
        }
    }


    val location by homeViewModel.userLocation.collectAsStateWithLifecycle()
    val primaryLocation by homeViewModel.primaryLocation.collectAsStateWithLifecycle()
    val prayerState by homeViewModel.prayerState.collectAsStateWithLifecycle()
    val formattedCurrentTime = homeViewModel.formatTime(prayerState.currentPrayerTime)
    val formattedUpcomingTime = homeViewModel.formatTime(prayerState.upcomingPrayerTime)

    // Request Location and Notification permissions simultaneously on app launch
    val permissionsToRequest = remember {
        buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (locationGranted && homeViewModel.primaryLocation.value == null) {
            homeViewModel.detectAndUseCurrentLocation()
        }
    }

    // Re-detect the city whenever the app returns to the foreground, not only on a cold start,
    // so someone who travelled sees their new city. A primary city, when set, always wins.
    // (Startup still opens the location settings if GPS is off; this resume refresh stays silent.)
    DisposableEffect(lifecycleOwner) {
        var wasStopped = false
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_STOP -> wasStopped = true
                androidx.lifecycle.Lifecycle.Event.ON_START -> if (wasStopped) {
                    wasStopped = false
                    val hasPermission =
                        androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                            android.content.pm.PackageManager.PERMISSION_GRANTED ||
                        androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                            android.content.pm.PackageManager.PERMISSION_GRANTED
                    if (hasPermission && homeViewModel.primaryLocation.value == null &&
                        homeViewModel.settingsRepository.isDeviceLocationEnabled(context)) {
                        homeViewModel.detectAndUseCurrentLocation()
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(permissionsToRequest)
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            com.example.data.repository.OfflineQuranProvider.preload(context)
        }
    }

    // Handle system back navigation:
    // Sub-section -> Main section -> Home -> Exit
    val shouldInterceptBack = isDrawerOpen ||
        currentScreen !is AppScreen.MainTab ||
        (currentScreen is AppScreen.MainTab && currentTab != NavTab.HOME)

    BackHandler(enabled = shouldInterceptBack) {
        if (isDrawerOpen) {
            isDrawerOpen = false
        } else if (currentScreen !is AppScreen.MainTab) {
            when (currentScreen) {
                is AppScreen.UniversalSearch -> {
                    currentScreen = AppScreen.MainTab
                    currentTab = NavTab.HOME
                    homeViewModel.refreshMoments()
                }
                is AppScreen.HadithReader -> {
                    currentScreen = AppScreen.HadithBaabList(hadithViewModel.selectedBook.value)
                }
                is AppScreen.HadithBaabList -> {
                    currentScreen = AppScreen.MainTab
                    currentTab = NavTab.HADEETH
                }
                is AppScreen.Quran16 -> {
                    currentScreen = AppScreen.MainTab
                    currentTab = NavTab.QURAN
                }
                is AppScreen.SurahReader -> {
                    currentScreen = AppScreen.MainTab
                    currentTab = NavTab.QURAN
                }
                is AppScreen.ShareHappiness, is AppScreen.Qibla, is AppScreen.Tasbeeh, is AppScreen.Calendar,
                is AppScreen.AllahNames, is AppScreen.Bookmarks, is AppScreen.Duas -> {
                    currentScreen = AppScreen.MainTab
                    currentTab = NavTab.MORE
                }
                is AppScreen.Settings, is AppScreen.AboutUs -> {
                    highlightCalcMethod = false
                    currentScreen = AppScreen.MainTab
                    if (currentTab == NavTab.HOME) {
                        homeViewModel.refreshMoments()
                    }
                }
                else -> {
                    currentScreen = AppScreen.MainTab
                    currentTab = NavTab.HOME
                    homeViewModel.refreshMoments()
                }
            }
        } else if (currentTab != NavTab.HOME) {
            currentTab = NavTab.HOME
            homeViewModel.refreshMoments()
        }
    }

    if (showCityPicker) {
        com.example.ui.components.CityPickerDialog(
            currentLocation = location,
            primaryLocation = primaryLocation,
            onLocationSelected = { newLocation ->
                homeViewModel.updateLocation(newLocation)
                showCityPicker = false
            },
            onSetAsPrimaryLocation = { homeViewModel.setPrimaryLocation(it) },
            onClearPrimaryLocation = { homeViewModel.clearPrimaryLocation() },
            onUseCurrentLocation = {
                homeViewModel.detectAndUseCurrentLocation()
                showCityPicker = false
            },
            onDismissRequest = { showCityPicker = false }
        )
    }

    // Popup ad on app open (cached first, then verified by the startup check).
    com.example.ui.components.PopupBannerHost(state = popupBanner)

    CircularRevealDrawer(
        isOpen = isDrawerOpen,
        onClose = { isDrawerOpen = false },
        onAboutUsClick = {
            isDrawerOpen = false
            currentScreen = AppScreen.AboutUs
        },
        onDonateUsClick = {
            isDrawerOpen = false
            currentScreen = AppScreen.DonateUs
        },
        onContactUsClick = {
            isDrawerOpen = false
            currentScreen = AppScreen.ContactUs
        },
        onSettingsClick = {
            isDrawerOpen = false
            highlightCalcMethod = false
            currentScreen = AppScreen.Settings
        },
        sideBanner = sideLoaderBanner
    ) {
        Scaffold(
            bottomBar = {
                // Bottom bar is animated smoothly when navigating to/from sub-screens
                AnimatedVisibility(
                    visible = currentScreen is AppScreen.MainTab,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(AlDeenMotion.DurationQuick, easing = AlDeenMotion.EmphasizedDecelerateEasing)
                    ) + fadeIn(animationSpec = tween(AlDeenMotion.DurationFast)),
                    exit = slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(AlDeenMotion.DurationFast, easing = AlDeenMotion.EmphasizedAccelerateEasing)
                    ) + fadeOut(animationSpec = tween(AlDeenMotion.DurationFast))
                ) {
                    AlDeenBottomNav(
                        currentTab = currentTab,
                        onTabSelected = { tab ->
                            if (tab == NavTab.HOME && currentTab != NavTab.HOME) {
                                homeViewModel.refreshMoments()
                            }
                            currentTab = tab
                            currentScreen = AppScreen.MainTab
                        }
                    )
                }
            },
            modifier = Modifier.fillMaxSize().testTag("main_scaffold")
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Top horizontal support stripe (above app name / header)
                    AnimatedVisibility(
                        visible = isTopSupportStripeVisible && currentScreen is AppScreen.MainTab,
                        enter = expandVertically(animationSpec = tween(AlDeenMotion.DurationNormal, easing = AlDeenMotion.EmphasizedDecelerateEasing)) + fadeIn(),
                        exit = shrinkVertically(animationSpec = tween(AlDeenMotion.DurationFast, easing = AlDeenMotion.EmphasizedAccelerateEasing)) + fadeOut()
                    ) {
                        com.example.ui.components.TopSupportStripe(
                            onSupportClick = {
                                currentScreen = AppScreen.DonateUs
                            },
                            onDismiss = {
                                isTopSupportStripeVisible = false
                            }
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        AnimatedContent(
                            targetState = currentScreen,
                    transitionSpec = {
                        if (targetState is AppScreen.MainTab) {
                            (fadeIn(animationSpec = tween(AlDeenMotion.DurationQuick, easing = AlDeenMotion.EmphasizedDecelerateEasing)) +
                             slideIntoContainer(
                                 towards = AnimatedContentTransitionScope.SlideDirection.End,
                                 animationSpec = tween(AlDeenMotion.DurationNormal, easing = AlDeenMotion.EmphasizedDecelerateEasing),
                                 initialOffset = { -it / 6 }
                             ))
                                .togetherWith(
                                    fadeOut(animationSpec = tween(AlDeenMotion.DurationFast, easing = AlDeenMotion.EmphasizedAccelerateEasing)) +
                                    slideOutOfContainer(
                                        towards = AnimatedContentTransitionScope.SlideDirection.End,
                                        animationSpec = tween(AlDeenMotion.DurationQuick, easing = AlDeenMotion.EmphasizedAccelerateEasing),
                                        targetOffset = { it / 6 }
                                    )
                                )
                        } else {
                            (fadeIn(animationSpec = tween(AlDeenMotion.DurationQuick, easing = AlDeenMotion.EmphasizedDecelerateEasing)) +
                             slideIntoContainer(
                                 towards = AnimatedContentTransitionScope.SlideDirection.Start,
                                 animationSpec = tween(AlDeenMotion.DurationNormal, easing = AlDeenMotion.EmphasizedDecelerateEasing),
                                 initialOffset = { it / 6 }
                             ))
                                .togetherWith(
                                    fadeOut(animationSpec = tween(AlDeenMotion.DurationFast, easing = AlDeenMotion.EmphasizedAccelerateEasing)) +
                                    slideOutOfContainer(
                                        towards = AnimatedContentTransitionScope.SlideDirection.Start,
                                        animationSpec = tween(AlDeenMotion.DurationQuick, easing = AlDeenMotion.EmphasizedAccelerateEasing),
                                        targetOffset = { -it / 6 }
                                    )
                                )
                        }
                    },
                    label = "screen_transition"
                ) { screen ->
                    when (screen) {
                        is AppScreen.Quran16 -> com.example.ui.quran16.Quran16Screen(
                            onBack = { currentScreen = AppScreen.MainTab; currentTab = NavTab.QURAN }
                        )
                        is AppScreen.MainTab -> {
                            AnimatedContent(
                                targetState = currentTab,
                                transitionSpec = {
                                    val direction = if (targetState.ordinal >= initialState.ordinal) {
                                        AnimatedContentTransitionScope.SlideDirection.Start
                                    } else {
                                        AnimatedContentTransitionScope.SlideDirection.End
                                    }
                                    (slideIntoContainer(
                                        towards = direction,
                                        animationSpec = tween(AlDeenMotion.DurationQuick, easing = AlDeenMotion.EmphasizedDecelerateEasing),
                                        initialOffset = { it / 10 }
                                    ) + fadeIn(animationSpec = tween(AlDeenMotion.DurationFast, easing = AlDeenMotion.EmphasizedDecelerateEasing)))
                                        .togetherWith(
                                            slideOutOfContainer(
                                                towards = direction,
                                                animationSpec = tween(AlDeenMotion.DurationFast, easing = AlDeenMotion.EmphasizedAccelerateEasing),
                                                targetOffset = { -it / 10 }
                                            ) + fadeOut(animationSpec = tween(AlDeenMotion.DurationFast, easing = AlDeenMotion.EmphasizedAccelerateEasing))
                                        )
                                },
                                label = "tab_transition"
                            ) { tab ->
                                when (tab) {
                                    NavTab.HOME -> HomeScreen(
                                        bannerState = homepageBanner,
                                        viewModel = homeViewModel,
                                        onOpenDrawer = {
                                            isDrawerOpen = !isDrawerOpen
                                        },
                                        onOpenSearch = {
                                            currentScreen = AppScreen.UniversalSearch
                                        },
                                        onOpenAyah = { surahNum, ayahNum ->
                                            quranViewModel.openSurah(surahNum, fromPara = false)
                                            currentScreen = AppScreen.SurahReader(surahNum, scrollToAyah = ayahNum)
                                        },
                                        onOpenHadith = { bookSlug, chapterNum, hadithNum ->
                                            val chapter = hadithViewModel.openHadith(bookSlug, chapterNum, hadithNum)
                                            currentScreen = AppScreen.HadithReader(chapter, scrollToHadith = hadithNum)
                                        },
                                        onOpenCalculationSettings = {
                                            highlightCalcMethod = true
                                            currentScreen = AppScreen.Settings
                                        },
                                        isDrawerOpen = isDrawerOpen
                                    )
                                    NavTab.QURAN -> QuranScreen(
                                        onOpen16LineQuran = { currentScreen = AppScreen.Quran16 },
                                        viewModel = quranViewModel,
                                        prayerState = prayerState,
                                        location = location,
                                        formattedCurrentTime = formattedCurrentTime,
                                        formattedUpcomingTime = formattedUpcomingTime,
                                        onOpenDrawer = {
                                            isDrawerOpen = !isDrawerOpen
                                        },
                                        onLocationClick = {
                                            showCityPicker = true
                                        },
                                        onSurahSelected = { surahNum ->
                                            quranViewModel.openSurah(surahNum, fromPara = false)
                                            currentScreen = AppScreen.SurahReader(surahNum)
                                        },
                                        onAyahSelected = { surahNum, ayahNum ->
                                            quranViewModel.openSurah(surahNum, fromPara = false)
                                            currentScreen = AppScreen.SurahReader(surahNum, scrollToAyah = ayahNum)
                                        },
                                        isDrawerOpen = isDrawerOpen
                                    )
                                    NavTab.HADEETH -> HadithScreen(
                                        viewModel = hadithViewModel,
                                        prayerState = prayerState,
                                        location = location,
                                        formattedCurrentTime = formattedCurrentTime,
                                        formattedUpcomingTime = formattedUpcomingTime,
                                        onOpenDrawer = {
                                            isDrawerOpen = !isDrawerOpen
                                        },
                                        onLocationClick = {
                                            showCityPicker = true
                                        },
                                        onBookSelected = { book ->
                                            hadithViewModel.selectBook(book)
                                            currentScreen = AppScreen.HadithBaabList(book)
                                        },
                                        onOpenHadith = { bookSlug, chapterNum, hadithNum ->
                                            val chapter = hadithViewModel.openHadith(bookSlug, chapterNum, hadithNum)
                                            currentScreen = AppScreen.HadithReader(chapter, scrollToHadith = hadithNum)
                                        },
                                        isDrawerOpen = isDrawerOpen
                                    )
                                    NavTab.GOALS -> GoalsScreen(
                                        viewModel = goalsViewModel,
                                        prayerState = prayerState,
                                        location = location,
                                        formattedCurrentTime = formattedCurrentTime,
                                        formattedUpcomingTime = formattedUpcomingTime,
                                        onOpenDrawer = {
                                            isDrawerOpen = !isDrawerOpen
                                        },
                                        onLocationClick = {
                                            showCityPicker = true
                                        },
                                        isDrawerOpen = isDrawerOpen
                                    )
                                    NavTab.MORE -> MoreScreen(
                                        bannerState = moreBanner,
                                        prayerState = prayerState,
                                        location = location,
                                        formattedCurrentTime = formattedCurrentTime,
                                        formattedUpcomingTime = formattedUpcomingTime,
                                        onOpenDrawer = {
                                            isDrawerOpen = !isDrawerOpen
                                        },
                                        onLocationClick = {
                                            showCityPicker = true
                                        },
                                        onNavigateToItem = { route ->
                                            when (route) {
                                                "more_find_masjid" -> {
                                                    try {
                                                        val mapIntent = android.content.Intent(
                                                            android.content.Intent.ACTION_VIEW,
                                                            android.net.Uri.parse("https://www.google.com/maps/search/Masjid+Near+Me")
                                                        )
                                                        context.startActivity(mapIntent)
                                                    } catch (e: Exception) {
                                                        // ignore
                                                    }
                                                }
                                                "more_share_happiness" -> currentScreen = AppScreen.ShareHappiness
                                                "more_qibla" -> currentScreen = AppScreen.Qibla
                                                "more_tasbeeh" -> currentScreen = AppScreen.Tasbeeh()
                                                "more_calendar" -> currentScreen = AppScreen.Calendar
                                                "more_allah_names" -> currentScreen = AppScreen.AllahNames
                                                "more_bookmarks" -> currentScreen = AppScreen.Bookmarks
                                                "more_duas" -> currentScreen = AppScreen.Duas
                                            }
                                        },
                                        isDrawerOpen = isDrawerOpen
                                    )
                                }
                            }
                        }

                        is AppScreen.UniversalSearch -> com.example.ui.search.UniversalSearchScreen(
                            hadithViewModel = hadithViewModel,
                            onBack = {
                                currentScreen = AppScreen.MainTab
                                currentTab = NavTab.HOME
                            },
                            onOpenSurah = { surahNum ->
                                currentScreen = AppScreen.SurahReader(surahNum)
                            },
                            onOpenHadithBook = { bookSlug ->
                                val book = AuthoritativeContentProvider.hadithBooks.firstOrNull { it.slug == bookSlug }
                                    ?: hadithViewModel.selectedBook.value
                                hadithViewModel.selectBook(book)
                                currentScreen = AppScreen.HadithBaabList(book)
                            },
                            onNavigateToHadithSection = {
                                currentScreen = AppScreen.MainTab
                                currentTab = NavTab.HADEETH
                            },
                            onNavigateToQuranSection = {
                                currentScreen = AppScreen.MainTab
                                currentTab = NavTab.QURAN
                            },
                            onOpenAyah = { surahNum, ayahNum ->
                                quranViewModel.openSurah(surahNum, fromPara = false)
                                currentScreen = AppScreen.SurahReader(surahNum, scrollToAyah = ayahNum)
                            },
                            onOpenHadith = { bookSlug, chapterNum, hadithNum ->
                                val chapter = hadithViewModel.openHadith(bookSlug, chapterNum, hadithNum)
                                currentScreen = AppScreen.HadithReader(chapter, scrollToHadith = hadithNum)
                            }
                        )

                        is AppScreen.SurahReader -> SurahReaderScreen(
                            viewModel = quranViewModel,
                            onBack = {
                                currentScreen = AppScreen.MainTab
                                currentTab = NavTab.QURAN
                            },
                            surahNumber = screen.surahNumber,
                            scrollToAyah = screen.scrollToAyah
                        )

                        is AppScreen.HadithBaabList -> BaabListScreen(
                            viewModel = hadithViewModel,
                            onChapterSelected = { chapter ->
                                hadithViewModel.selectChapter(chapter)
                                currentScreen = AppScreen.HadithReader(chapter)
                            },
                            onBack = {
                                currentScreen = AppScreen.MainTab
                                currentTab = NavTab.HADEETH
                            }
                        )

                        is AppScreen.HadithReader -> HadithReaderScreen(
                            viewModel = hadithViewModel,
                            onBack = {
                                currentScreen = AppScreen.HadithBaabList(hadithViewModel.selectedBook.value)
                            },
                            initialChapter = screen.chapter,
                            scrollToHadith = screen.scrollToHadith
                        )

                        is AppScreen.ShareHappiness -> com.example.ui.happiness.ShareHappinessScreen(
                            onBack = {
                                currentScreen = AppScreen.MainTab
                                currentTab = NavTab.MORE
                            }
                        )

                        is AppScreen.Qibla -> QiblaScreen(
                            userLocation = homeViewModel.userLocation.value,
                            onBack = {
                                currentScreen = AppScreen.MainTab
                                currentTab = NavTab.MORE
                            }
                        )

                        is AppScreen.Tasbeeh -> TasbeehScreen(
                            initialPhraseId = screen.initialPhraseId,
                            onBack = {
                                currentScreen = AppScreen.MainTab
                                currentTab = NavTab.MORE
                            }
                        )

                        is AppScreen.Calendar -> CalendarScreen(
                            hijriAdjustment = settingsRepository.hijriAdjustment.collectAsStateWithLifecycle().value,
                            onAdjustmentChange = { adjustment ->
                                settingsRepository.setHijriAdjustment(adjustment)
                            },
                            onBack = {
                                currentScreen = AppScreen.MainTab
                                currentTab = NavTab.MORE
                            }
                        )

                        is AppScreen.AllahNames -> AllahNamesScreen(
                            onBack = {
                                currentScreen = AppScreen.MainTab
                                currentTab = NavTab.MORE
                            }
                        )

                        is AppScreen.Bookmarks -> BookmarksScreen(
                            bookmarksRepository = homeViewModel.bookmarksRepository,
                            onBack = {
                                currentScreen = AppScreen.MainTab
                                currentTab = NavTab.MORE
                            }
                        )

                        is AppScreen.Duas -> DuaZikrScreen(
                            bookmarksRepository = homeViewModel.bookmarksRepository,
                            onBack = {
                                currentScreen = AppScreen.MainTab
                                currentTab = NavTab.MORE
                            }
                        )

                        is AppScreen.Settings -> SettingsScreen(
                            settingsRepository = settingsRepository,
                            highlightCalcMethod = highlightCalcMethod,
                            onBack = {
                                highlightCalcMethod = false
                                currentScreen = AppScreen.MainTab
                            }
                        )

                        is AppScreen.AboutUs -> AboutUsScreen(
                            onBack = {
                                currentScreen = AppScreen.MainTab
                            }
                        )

                        is AppScreen.ContactUs -> ContactUsScreen(
                            onBack = {
                                currentScreen = AppScreen.MainTab
                            }
                        )

                        is AppScreen.DonateUs -> DonateUsScreen(
                            isScreenActive = isScreenActive,
                            viewModel = donateViewModel,
                            onBack = {
                                currentScreen = AppScreen.MainTab
                            }
                        )
                    }
                }
                    }
                }

                // Global floating audio player and headphone button host across the 5 main sections and 16 Line Quran
                if (currentScreen is AppScreen.MainTab || currentScreen is AppScreen.Quran16) {
                    com.example.ui.quran.QuranGlobalFloatingPlayerHost(viewModel = quranViewModel)
                }
            }
        }
    }
}
