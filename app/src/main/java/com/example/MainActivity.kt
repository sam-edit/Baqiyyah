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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.repository.AuthoritativeContentProvider
import com.example.data.repository.SettingsRepository
import com.example.domain.model.HadithBook
import com.example.domain.model.HadithChapter
import com.example.ui.components.AlDeenBottomNav
import com.example.ui.components.AlDeenDrawerContent
import com.example.ui.components.NavTab
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
import kotlinx.coroutines.launch

sealed class AppScreen {
    object MainTab : AppScreen()
    object UniversalSearch : AppScreen()
    data class SurahReader(val surahNumber: Int, val scrollToAyah: Int? = null) : AppScreen()
    data class HadithBaabList(val book: HadithBook) : AppScreen()
    data class HadithReader(val chapter: HadithChapter, val scrollToHadith: String? = null) : AppScreen()
    object Qibla : AppScreen()
    object Tasbeeh : AppScreen()
    object Calendar : AppScreen()
    object AllahNames : AppScreen()
    object Bookmarks : AppScreen()
    object Duas : AppScreen()
    object Settings : AppScreen()
    object AboutUs : AppScreen()
    object ContactUs : AppScreen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AlDeenFontRegistry.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            val settingsRepo = remember { SettingsRepository.getInstance(applicationContext) }
            val themeMode by settingsRepo.themeMode.collectAsStateWithLifecycle()

            AlDeenTheme(themeMode = themeMode) {
                AlDeenApp(settingsRepo)
            }
        }
    }
}

@Composable
fun AlDeenApp(settingsRepository: SettingsRepository) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    var currentTab by remember { mutableStateOf(NavTab.HOME) }
    var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.MainTab) }

    val homeViewModel: HomeViewModel = viewModel()
    val quranViewModel: QuranViewModel = viewModel()
    val hadithViewModel: HadithViewModel = viewModel()
    val goalsViewModel: GoalsViewModel = viewModel()

    val location by homeViewModel.userLocation.collectAsStateWithLifecycle()
    val primaryLocation by homeViewModel.primaryLocation.collectAsStateWithLifecycle()
    val prayerState by homeViewModel.prayerState.collectAsStateWithLifecycle()
    val formattedCurrentTime = homeViewModel.formatTime(prayerState.currentPrayerTime)
    val formattedUpcomingTime = homeViewModel.formatTime(prayerState.upcomingPrayerTime)
    var showCityPicker by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current

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

    LaunchedEffect(Unit) {
        permissionLauncher.launch(permissionsToRequest)
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            com.example.data.repository.OfflineQuranProvider.preload(context)
        }
    }

    // Handle system back navigation:
    // Sub-section -> Main section -> Home -> Exit
    val shouldInterceptBack = drawerState.isOpen ||
        currentScreen !is AppScreen.MainTab ||
        (currentScreen is AppScreen.MainTab && currentTab != NavTab.HOME)

    BackHandler(enabled = shouldInterceptBack) {
        if (drawerState.isOpen) {
            coroutineScope.launch { drawerState.close() }
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
                is AppScreen.SurahReader -> {
                    currentScreen = AppScreen.MainTab
                    currentTab = NavTab.QURAN
                }
                is AppScreen.Qibla, is AppScreen.Tasbeeh, is AppScreen.Calendar,
                is AppScreen.AllahNames, is AppScreen.Bookmarks, is AppScreen.Duas -> {
                    currentScreen = AppScreen.MainTab
                    currentTab = NavTab.MORE
                }
                is AppScreen.Settings, is AppScreen.AboutUs -> {
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

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AlDeenDrawerContent(
                onAboutUsClick = {
                    currentScreen = AppScreen.AboutUs
                },
                onContactUsClick = {
                    currentScreen = AppScreen.ContactUs
                },
                onSettingsClick = {
                    currentScreen = AppScreen.Settings
                },
                onCloseDrawer = {
                    coroutineScope.launch { drawerState.close() }
                }
            )
        }
    ) {
        Scaffold(
            bottomBar = {
                // Bottom bar is shown on Main Tab
                if (currentScreen is AppScreen.MainTab) {
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
                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "screen_transition"
                ) { screen ->
                    when (screen) {
                        is AppScreen.MainTab -> {
                            when (currentTab) {
                                NavTab.HOME -> HomeScreen(
                                    viewModel = homeViewModel,
                                    onOpenDrawer = {
                                        coroutineScope.launch { drawerState.open() }
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
                                    }
                                )
                                NavTab.QURAN -> QuranScreen(
                                    viewModel = quranViewModel,
                                    prayerState = prayerState,
                                    location = location,
                                    formattedCurrentTime = formattedCurrentTime,
                                    formattedUpcomingTime = formattedUpcomingTime,
                                    onOpenDrawer = {
                                        coroutineScope.launch { drawerState.open() }
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
                                    }
                                )
                                NavTab.HADEETH -> HadithScreen(
                                    viewModel = hadithViewModel,
                                    prayerState = prayerState,
                                    location = location,
                                    formattedCurrentTime = formattedCurrentTime,
                                    formattedUpcomingTime = formattedUpcomingTime,
                                    onOpenDrawer = {
                                        coroutineScope.launch { drawerState.open() }
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
                                    }
                                )
                                NavTab.GOALS -> GoalsScreen(
                                    viewModel = goalsViewModel,
                                    prayerState = prayerState,
                                    location = location,
                                    formattedCurrentTime = formattedCurrentTime,
                                    formattedUpcomingTime = formattedUpcomingTime,
                                    onOpenDrawer = {
                                        coroutineScope.launch { drawerState.open() }
                                    },
                                    onLocationClick = {
                                        showCityPicker = true
                                    }
                                )
                                NavTab.MORE -> MoreScreen(
                                    prayerState = prayerState,
                                    location = location,
                                    formattedCurrentTime = formattedCurrentTime,
                                    formattedUpcomingTime = formattedUpcomingTime,
                                    onOpenDrawer = {
                                        coroutineScope.launch { drawerState.open() }
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
                                            "more_qibla" -> currentScreen = AppScreen.Qibla
                                            "more_tasbeeh" -> currentScreen = AppScreen.Tasbeeh
                                            "more_calendar" -> currentScreen = AppScreen.Calendar
                                            "more_allah_names" -> currentScreen = AppScreen.AllahNames
                                            "more_bookmarks" -> currentScreen = AppScreen.Bookmarks
                                            "more_duas" -> currentScreen = AppScreen.Duas
                                        }
                                    }
                                )
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

                        is AppScreen.Qibla -> QiblaScreen(
                            userLocation = homeViewModel.userLocation.value,
                            onBack = {
                                currentScreen = AppScreen.MainTab
                                currentTab = NavTab.MORE
                            }
                        )

                        is AppScreen.Tasbeeh -> TasbeehScreen(
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
                            onBack = {
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
                    }
                }
            }
        }
    }
}
