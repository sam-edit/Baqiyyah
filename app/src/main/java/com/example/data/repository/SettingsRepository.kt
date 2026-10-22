package com.example.data.repository

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.example.domain.model.CalculationMethod
import com.example.domain.model.JuristicSchool
import com.example.domain.model.PrayerAlertType
import com.example.domain.model.PrayerType
import com.example.domain.model.UserLocation
import com.example.ui.theme.AppThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class SettingsRepository(val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("al_deen_prefs", Context.MODE_PRIVATE)

    // Primary Location: Persistent location for notifications and cold-start app launch
    private val _primaryLocation = MutableStateFlow<UserLocation?>(
        run {
            val hasPrimary = prefs.getBoolean("has_primary_location", false)
            val primaryCity = prefs.getString("primary_city_name", null)
            if (hasPrimary && !primaryCity.isNullOrBlank()) {
                UserLocation(
                    cityName = primaryCity,
                    countryName = prefs.getString("primary_country_name", "") ?: "",
                    latitude = prefs.getString("primary_latitude", "0.0")?.toDoubleOrNull() ?: 0.0,
                    longitude = prefs.getString("primary_longitude", "0.0")?.toDoubleOrNull() ?: 0.0,
                    isAutoDetected = prefs.getBoolean("primary_is_auto", false),
                    timeZoneId = prefs.getString("primary_tz", null)
                )
            } else {
                null
            }
        }
    )
    val primaryLocation: StateFlow<UserLocation?> = _primaryLocation.asStateFlow()

    // User Location: On cold start, if primary location is set, default to primary location!
    // If not, use previously saved city or detect dynamically.
    private val _userLocation = MutableStateFlow(
        run {
            val primary = _primaryLocation.value
            if (primary != null) {
                primary
            } else {
                val detected = prefs.contains("detected_city")
                val savedCity = prefs.getString(if (detected) "detected_city" else "city_name", null)
                if (savedCity != null) {
                    UserLocation(
                        cityName = savedCity,
                        countryName = prefs.getString(if (detected) "detected_country" else "country_name", "") ?: "",
                        latitude = prefs.getString(if (detected) "detected_lat" else "latitude", "0.0")?.toDoubleOrNull() ?: 0.0,
                        longitude = prefs.getString(if (detected) "detected_lon" else "longitude", "0.0")?.toDoubleOrNull() ?: 0.0,
                        isAutoDetected = detected || prefs.getBoolean("is_auto_detected", false),
                        timeZoneId = prefs.getString(if (detected) "detected_tz" else "tz", null)
                    )
                } else {
                    UserLocation(
                        cityName = "Current Location",
                        countryName = "",
                        latitude = 0.0,
                        longitude = 0.0,
                        isAutoDetected = true
                    )
                }
            }
        }
    )
    val userLocation: StateFlow<UserLocation> = _userLocation.asStateFlow()

    // Background startup must never request GPS or launch a settings screen.
    // Remember GPS separately from temporary city previews.
    private val _lastDetectedLocation = MutableStateFlow<UserLocation?>(run {
        val city = prefs.getString("detected_city", null)
        if (city != null) UserLocation(city, prefs.getString("detected_country", "") ?: "",
            prefs.getString("detected_lat", "0")?.toDoubleOrNull() ?: 0.0,
            prefs.getString("detected_lon", "0")?.toDoubleOrNull() ?: 0.0, true,
            prefs.getString("detected_tz", null))
        else if (prefs.getBoolean("is_auto_detected", false) && prefs.contains("city_name")) {
            UserLocation(prefs.getString("city_name", "Current Location") ?: "Current Location",
                prefs.getString("country_name", "") ?: "",
                prefs.getString("latitude", "0")?.toDoubleOrNull() ?: 0.0,
                prefs.getString("longitude", "0")?.toDoubleOrNull() ?: 0.0, true,
                prefs.getString("tz", null))
        } else _userLocation.value.takeIf { it.isAutoDetected && (it.latitude != 0.0 || it.longitude != 0.0) }
    })
    val lastDetectedLocation: StateFlow<UserLocation?> = _lastDetectedLocation.asStateFlow()

    // Theme Mode (Default SYSTEM)
    private val _themeMode = MutableStateFlow(
        AppThemeMode.valueOf(prefs.getString("theme_mode", AppThemeMode.SYSTEM.name) ?: AppThemeMode.SYSTEM.name)
    )
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    // 24 Hour Time format
    private val _is24HourFormat = MutableStateFlow(prefs.getBoolean("is_24_hour", false))
    val is24HourFormat: StateFlow<Boolean> = _is24HourFormat.asStateFlow()

    // Hijri Adjustment (-2 to +2)
    private val _hijriAdjustment = MutableStateFlow(prefs.getInt("hijri_adjustment", 0))
    val hijriAdjustment: StateFlow<Int> = _hijriAdjustment.asStateFlow()

    // Reader Font Sizes
    private val _quranArabicFontSize = MutableStateFlow(prefs.getFloat("quran_arabic_font_size", 24f))
    val quranArabicFontSize: StateFlow<Float> = _quranArabicFontSize.asStateFlow()

    private val _quranTransFontSize = MutableStateFlow(prefs.getFloat("quran_trans_font_size", 13f))
    val quranTransFontSize: StateFlow<Float> = _quranTransFontSize.asStateFlow()

    private val _hadithArabicFontSize = MutableStateFlow(prefs.getFloat("hadith_arabic_font_size", 20f))
    val hadithArabicFontSize: StateFlow<Float> = _hadithArabicFontSize.asStateFlow()

    private val _hadithTransFontSize = MutableStateFlow(prefs.getFloat("hadith_trans_font_size", 14f))
    val hadithTransFontSize: StateFlow<Float> = _hadithTransFontSize.asStateFlow()

    // Haptics
    private val _hapticsEnabled = MutableStateFlow(prefs.getBoolean("haptics_enabled", true))
    val hapticsEnabled: StateFlow<Boolean> = _hapticsEnabled.asStateFlow()

    // Calculation Method
    private val _calculationMethod = MutableStateFlow(
        CalculationMethod.values().firstOrNull { it.id == prefs.getInt("calc_method", CalculationMethod.KARACHI.id) }
            ?: CalculationMethod.KARACHI
    )
    val calculationMethod: StateFlow<CalculationMethod> = _calculationMethod.asStateFlow()

    // Juristic School for Asr (Hanafi vs Shafi'i/Standard)
    private val _juristicSchool = MutableStateFlow(
        try {
            val savedId = prefs.getInt("juristic_school", JuristicSchool.HANAFI.id)
            JuristicSchool.values().firstOrNull { it.id == savedId } ?: JuristicSchool.HANAFI
        } catch (_: Exception) {
            JuristicSchool.HANAFI
        }
    )
    val juristicSchool: StateFlow<JuristicSchool> = _juristicSchool.asStateFlow()

    // Pre-Prayer Reminder (Minutes)
    private val _prePrayerReminderMin = MutableStateFlow(prefs.getInt("pre_prayer_min", 10))
    val prePrayerReminderMin: StateFlow<Int> = _prePrayerReminderMin.asStateFlow()

    // Quiet Mode Toggle
    private val _isQuietMode = MutableStateFlow(prefs.getBoolean("is_quiet_mode", false))
    val isQuietMode: StateFlow<Boolean> = _isQuietMode.asStateFlow()

    // Prayer Alert Type (Default is LIGHT)
    private val _prayerAlertType = MutableStateFlow(
        PrayerAlertType.fromId(prefs.getString("prayer_alert_type", PrayerAlertType.LIGHT.id))
    )
    val prayerAlertType: StateFlow<PrayerAlertType> = _prayerAlertType.asStateFlow()

    // Per-prayer notification map
    private val _prayerNotifications = MutableStateFlow(
        PrayerType.values().associateWith { prayer ->
            // Fard prayers ON by default, optional/derived OFF by default
            val defaultOn = prayer.isFard && prayer != PrayerType.JUMMAH
            prefs.getBoolean("notif_${prayer.name}", defaultOn)
        }
    )
    val prayerNotifications: StateFlow<Map<PrayerType, Boolean>> = _prayerNotifications.asStateFlow()

    /**
     * Shows [location]. A GPS-detected location becomes the "last detected" city; a city picked
     * manually from the list is only a preview: it never touches the cache, alarms or widgets
     * (those follow the primary city, or else the last detected city).
     */
    fun updateLocation(location: UserLocation, autoAlignMethod: Boolean = true) {
        val backgroundBefore = getBackgroundLocation()
        val methodBefore = _calculationMethod.value
        _userLocation.value = location
        if (location.isAutoDetected) {
            _lastDetectedLocation.value = location
            prefs.edit().putString("detected_city", location.cityName)
                .putString("detected_country", location.countryName)
                .putString("detected_lat", location.latitude.toString())
                .putString("detected_lon", location.longitude.toString())
                .putString("detected_tz", location.timeZoneId).commit()
        }
        // Only persist as general default city if user has NOT set a primary location
        if (_primaryLocation.value == null && location.isAutoDetected) {
            prefs.edit()
                .putString("city_name", location.cityName)
                .putString("country_name", location.countryName)
                .putString("latitude", location.latitude.toString())
                .putString("longitude", location.longitude.toString())
                .putBoolean("is_auto_detected", location.isAutoDetected)
                .putString("tz", location.timeZoneId)
                .apply()
        }

        if (autoAlignMethod && (location.isAutoDetected || _primaryLocation.value == location) && location.countryName.isNotBlank()) {
            val recommendedMethod = com.example.domain.model.getRecommendedMethodForLocation(location.countryName, location.cityName)
            _calculationMethod.value = recommendedMethod
            prefs.edit().putInt("calc_method", recommendedMethod.id).apply()
        }
        // Alarms/widgets/cache only care about the background city. Skip all of that work for a
        // plain city preview; still refresh when nothing is saved yet (widget falls back to it).
        val backgroundAfter = getBackgroundLocation()
        if (backgroundAfter == null || !sameSpot(backgroundBefore, backgroundAfter) ||
            methodBefore != _calculationMethod.value) {
            notifyPrayerWidgets()
        }
    }

    private fun sameCoordinates(a: UserLocation?, b: UserLocation?): Boolean {
        if (a == null || b == null) return false
        return kotlin.math.abs(a.latitude - b.latitude) < 1e-6 &&
            kotlin.math.abs(a.longitude - b.longitude) < 1e-6
    }

    /** Same place AND same zone: what the schedule cache / alarm plan are keyed on. */
    private fun sameSpot(a: UserLocation?, b: UserLocation?): Boolean {
        if (a == null || b == null) return a == null && b == null
        return sameCoordinates(a, b) && getZoneIdForLocation(a) == getZoneIdForLocation(b)
    }

    /** True when [location] is the city the cache, alarms and widgets are built for. */
    fun isBackgroundLocation(location: UserLocation): Boolean =
        sameCoordinates(location, getBackgroundLocation())

    private fun notifyPrayerWidgets() {
        try {
            com.example.notification.PrayerNotificationManager.rescheduleAlarmsFromSavedState(context)

            com.example.widget.PrayerWidgetProvider.updateAllWidgets(context)
            com.example.widget.PrayerWidgetProvider.scheduleNextBoundaryUpdate(context)
        } catch (_: Exception) {}
    }

    fun setPrimaryLocation(location: UserLocation) {
        // New primary city: drop the previous city's cached schedule before anything reads it.
        if (!sameSpot(getBackgroundLocation(), location)) PrayerScheduleCache.clear(context)
        _primaryLocation.value = location
        _userLocation.value = location
        prefs.edit()
            .putBoolean("has_primary_location", true)
            .putString("primary_city_name", location.cityName)
            .putString("primary_country_name", location.countryName)
            .putString("primary_latitude", location.latitude.toString())
            .putString("primary_longitude", location.longitude.toString())
            .putBoolean("primary_is_auto", location.isAutoDetected)
            .putString("primary_tz", location.timeZoneId)
            .apply()

        notifyPrayerWidgets()
    }

    fun clearPrimaryLocation() {
        // Background city falls back to the last detected one (or nothing): old cache must go.
        if (!sameSpot(getBackgroundLocation(), _lastDetectedLocation.value)) PrayerScheduleCache.clear(context)
        _primaryLocation.value = null
        _lastDetectedLocation.value?.let { _userLocation.value = it }
        prefs.edit()
            .putBoolean("has_primary_location", false)
            .remove("primary_city_name")
            .remove("primary_country_name")
            .remove("primary_latitude")
            .remove("primary_longitude")
            .remove("primary_is_auto")
            .remove("primary_tz")
            .apply()

        notifyPrayerWidgets()
    }

    fun getBackgroundLocation(): UserLocation? = _primaryLocation.value ?: _lastDetectedLocation.value

    fun getNotificationLocation(): UserLocation = getBackgroundLocation() ?: _userLocation.value

    fun getNotificationZoneId(): java.time.ZoneId {
        return getZoneIdForLocation(getNotificationLocation())
    }

    fun setThemeMode(mode: AppThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString("theme_mode", mode.name).apply()
    }

    fun set24HourFormat(enabled: Boolean) {
        _is24HourFormat.value = enabled
        prefs.edit().putBoolean("is_24_hour", enabled).apply()
        notifyPrayerWidgets()
    }

    fun setHijriAdjustment(adjustment: Int) {
        _hijriAdjustment.value = adjustment
        prefs.edit().putInt("hijri_adjustment", adjustment).apply()
        notifyPrayerWidgets()
    }

    fun setQuranArabicFontSize(size: Float) {
        _quranArabicFontSize.value = size
        prefs.edit().putFloat("quran_arabic_font_size", size).apply()
    }

    fun setQuranTransFontSize(size: Float) {
        _quranTransFontSize.value = size
        prefs.edit().putFloat("quran_trans_font_size", size).apply()
    }

    fun setHadithArabicFontSize(size: Float) {
        _hadithArabicFontSize.value = size
        prefs.edit().putFloat("hadith_arabic_font_size", size).apply()
    }

    fun setHadithTransFontSize(size: Float) {
        _hadithTransFontSize.value = size
        prefs.edit().putFloat("hadith_trans_font_size", size).apply()
    }

    fun setHapticsEnabled(enabled: Boolean) {
        _hapticsEnabled.value = enabled
        prefs.edit().putBoolean("haptics_enabled", enabled).apply()
    }

    fun setCalculationMethod(method: CalculationMethod) {
        _calculationMethod.value = method
        prefs.edit().putInt("calc_method", method.id).apply()
        notifyPrayerWidgets()
    }

    fun setJuristicSchool(school: JuristicSchool) {
        _juristicSchool.value = school
        prefs.edit().putInt("juristic_school", school.id).apply()
        notifyPrayerWidgets()
    }

    fun setPrePrayerReminder(minutes: Int) {
        _prePrayerReminderMin.value = minutes
        prefs.edit().putInt("pre_prayer_min", minutes).apply()
    }

    fun toggleQuietMode() {
        val next = !_isQuietMode.value
        _isQuietMode.value = next
        prefs.edit().putBoolean("is_quiet_mode", next).apply()
    }

    fun setPrayerNotification(prayer: PrayerType, enabled: Boolean) {
        val current = _prayerNotifications.value.toMutableMap()
        current[prayer] = enabled
        _prayerNotifications.value = current
        prefs.edit().putBoolean("notif_${prayer.name}", enabled).apply()
        com.example.notification.PrayerNotificationManager.rescheduleAlarmsFromSavedState(context)
    }

    fun setPrayerAlertType(type: PrayerAlertType) {
        _prayerAlertType.value = type
        prefs.edit().putString("prayer_alert_type", type.id).apply()
    }

    fun getPrayerAlertType(): PrayerAlertType {
        return _prayerAlertType.value
    }

    fun getLocationZoneId(): java.time.ZoneId {
        return getZoneIdForLocation(_userLocation.value)
    }

    fun getZoneIdForLocation(loc: UserLocation): java.time.ZoneId {
        // Exact IANA zone saved with the location (GPS fix / city search result) always wins.
        val explicitZone = loc.timeZoneId?.takeIf { it.isNotBlank() }
            ?.let { runCatching { java.time.ZoneId.of(it) }.getOrNull() }
        if (explicitZone != null) return explicitZone

        val country = loc.countryName.lowercase().trim()
        val city = loc.cityName.lowercase().trim()
        // Whole-word country tokens, so "Romania" is not matched by "oman" nor "Ukraine" by "uk".
        val countryWords = country.split(Regex("[^a-z]+")).filter { it.isNotEmpty() }

        val zoneName = when {
            city.contains("los angeles") || city.contains("san francisco") -> "America/Los_Angeles"
            city.contains("chicago") || city.contains("houston") || city.contains("dallas") -> "America/Chicago"
            city.contains("vancouver") -> "America/Vancouver"
            city.contains("perth") -> "Australia/Perth"
            country.contains("pakistan") || city.contains("karachi") || city.contains("lahore") || city.contains("islamabad") || city.contains("rawalpindi") || city.contains("peshawar") || city.contains("quetta") -> "Asia/Karachi"
            country.contains("saudi") || city.contains("makkah") || city.contains("mecca") || city.contains("madinah") || city.contains("medina") || city.contains("riyadh") || city.contains("jeddah") -> "Asia/Riyadh"
            country.contains("emirates") || country.contains("uae") || city.contains("dubai") || city.contains("abu dhabi") || city.contains("sharjah") -> "Asia/Dubai"
            country.contains("qatar") || city.contains("doha") -> "Asia/Qatar"
            country.contains("kuwait") -> "Asia/Kuwait"
            "oman" in countryWords || city.contains("muscat") -> "Asia/Muscat"
            country.contains("bahrain") || city.contains("manama") -> "Asia/Bahrain"
            country.contains("jordan") || city.contains("amman") -> "Asia/Amman"
            country.contains("lebanon") || city.contains("beirut") -> "Asia/Beirut"
            country.contains("iraq") || city.contains("baghdad") -> "Asia/Baghdad"
            country.contains("iran") || city.contains("tehran") -> "Asia/Tehran"
            country.contains("afghanistan") || city.contains("kabul") -> "Asia/Kabul"
            country.contains("india") || city.contains("delhi") || city.contains("mumbai") || city.contains("hyderabad") -> "Asia/Kolkata"
            country.contains("bangladesh") || city.contains("dhaka") -> "Asia/Dhaka"
            country.contains("turkey") || city.contains("istanbul") || city.contains("ankara") -> "Europe/Istanbul"
            country.contains("egypt") || city.contains("cairo") || city.contains("alexandria") -> "Africa/Cairo"
            country.contains("morocco") || city.contains("casablanca") || city.contains("rabat") -> "Africa/Casablanca"
            country.contains("algeria") || city.contains("algiers") -> "Africa/Algiers"
            country.contains("tunisia") || city.contains("tunis") -> "Africa/Tunis"
            country.contains("indonesia") || city.contains("jakarta") -> "Asia/Jakarta"
            country.contains("malaysia") || city.contains("kuala lumpur") -> "Asia/Kuala_Lumpur"
            country.contains("singapore") -> "Asia/Singapore"
            country.contains("united kingdom") || "uk" in countryWords || city.contains("london") || city.contains("birmingham") || city.contains("manchester") -> "Europe/London"
            country.contains("germany") || city.contains("berlin") || city.contains("frankfurt") || city.contains("munich") -> "Europe/Berlin"
            country.contains("france") || city.contains("paris") -> "Europe/Paris"
            country.contains("spain") || city.contains("madrid") || city.contains("barcelona") -> "Europe/Madrid"
            country.contains("italy") || city.contains("rome") -> "Europe/Rome"
            country.contains("united states") || country.contains("usa") || city.contains("new york") || city.contains("washington") -> "America/New_York"
            city.contains("chicago") || city.contains("houston") || city.contains("dallas") -> "America/Chicago"
            city.contains("los angeles") || city.contains("san francisco") -> "America/Los_Angeles"
            country.contains("canada") || city.contains("toronto") || city.contains("montreal") -> "America/Toronto"
            city.contains("vancouver") -> "America/Vancouver"
            country.contains("australia") || city.contains("sydney") || city.contains("melbourne") -> "Australia/Sydney"
            city.contains("perth") -> "Australia/Perth"
            country.contains("south africa") || city.contains("johannesburg") || city.contains("cape town") -> "Africa/Johannesburg"
            country.contains("japan") || city.contains("tokyo") -> "Asia/Tokyo"
            country.contains("china") || city.contains("beijing") || city.contains("shanghai") -> "Asia/Shanghai"
            country.contains("russia") || city.contains("moscow") -> "Europe/Moscow"
            else -> null
        }

        if (zoneName != null) {
            try {
                return java.time.ZoneId.of(zoneName)
            } catch (_: Exception) {}
        }

        if (loc.longitude != 0.0 || loc.latitude != 0.0) {
            val hours = kotlin.math.round(loc.longitude / 15.0).toInt().coerceIn(-12, 14)
            return try {
                java.time.ZoneOffset.ofHours(hours)
            } catch (_: Exception) {
                java.time.ZoneId.systemDefault()
            }
        }

        return java.time.ZoneId.systemDefault()
    }

    fun getTodayDateForLocation(): java.time.LocalDate {
        return java.time.LocalDate.now(getLocationZoneId())
    }

    fun isDeviceLocationEnabled(appContext: Context = context): Boolean {
        val locationManager = appContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    fun openLocationSettings(appContext: Context = context) {
        try {
            val intent = android.content.Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS).apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            appContext.startActivity(intent)
        } catch (_: Exception) {}
    }

    fun detectAndUseCurrentLocation(
        appContext: Context = context,
        onGpsDisabled: (() -> Unit)? = null
    ): Boolean {
        try {
            val locationManager = appContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                ?: return false
            val fineGranted = ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val coarseGranted = ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            if (!fineGranted && !coarseGranted) {
                return false
            }

            val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            if (!isGpsEnabled && !isNetworkEnabled) {
                if (onGpsDisabled != null) {
                    onGpsDisabled()
                } else {
                    openLocationSettings(appContext)
                }
                return false
            }

            val loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)

            val provider = if (isGpsEnabled) LocationManager.GPS_PROVIDER else LocationManager.NETWORK_PROVIDER
            if (loc != null) {
                applyLocationFromCoordinates(appContext, loc.latitude, loc.longitude)
                // A cached fix can be hours/days old (e.g. after travelling): ask for a fresh one too.
                if (System.currentTimeMillis() - loc.time > STALE_FIX_MS) {
                    requestFreshFix(appContext, locationManager, provider)
                }
                return true
            } else {
                requestFreshFix(appContext, locationManager, provider)
                return true
            }
        } catch (_: Exception) {}
        return false
    }

    private fun requestFreshFix(appContext: Context, locationManager: LocationManager, provider: String) {
        val listener = object : android.location.LocationListener {
            override fun onLocationChanged(freshLoc: android.location.Location) {
                applyLocationFromCoordinates(appContext, freshLoc.latitude, freshLoc.longitude)
                try { locationManager.removeUpdates(this) } catch (_: Exception) {}
            }
            override fun onProviderDisabled(provider: String) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
        }
        try {
            locationManager.requestSingleUpdate(provider, listener, android.os.Looper.getMainLooper())
        } catch (_: Exception) {
            try {
                locationManager.requestLocationUpdates(provider, 0L, 0f, listener, android.os.Looper.getMainLooper())
            } catch (_: Exception) {}
        }
    }

    private fun applyLocationFromCoordinates(appContext: Context, lat: Double, lng: Double) {
        // The device zone is the right zone for a GPS fix taken where the user physically is.
        val zoneId = java.time.ZoneId.systemDefault().id
        // Geocoder does blocking I/O: keep it off the main thread.
        kotlin.concurrent.thread(name = "geocode-location") {
            var cityName = "Current Location"
            var countryName = ""
            try {
                val geocoder = Geocoder(appContext, Locale.getDefault())
                val addresses = geocoder.getFromLocation(lat, lng, 1)
                if (!addresses.isNullOrEmpty()) {
                    cityName = addresses[0].locality ?: addresses[0].subAdminArea ?: addresses[0].adminArea ?: "My Location"
                    countryName = addresses[0].countryName ?: ""
                }
            } catch (_: Exception) {}
            // Offline / geocoder failure: keep the known name when we have not moved far.
            val previous = _lastDetectedLocation.value
            if (countryName.isBlank() && previous != null && previous.countryName.isNotBlank() &&
                kotlin.math.abs(previous.latitude - lat) < 0.05 && kotlin.math.abs(previous.longitude - lng) < 0.05) {
                cityName = previous.cityName
                countryName = previous.countryName
            }
            updateLocation(
                UserLocation(
                    cityName = cityName,
                    countryName = countryName,
                    latitude = lat,
                    longitude = lng,
                    isAutoDetected = true,
                    timeZoneId = zoneId
                )
            )
        }
    }

    companion object {
        private const val STALE_FIX_MS = 15 * 60 * 1000L

        @Volatile
        private var INSTANCE: SettingsRepository? = null

        fun getInstance(context: Context): SettingsRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
