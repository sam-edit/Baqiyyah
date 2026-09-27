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
import com.example.domain.model.PrayerType
import com.example.domain.model.UserLocation
import com.example.ui.theme.AppThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class SettingsRepository(private val context: Context) {
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
                    isAutoDetected = prefs.getBoolean("primary_is_auto", false)
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
                val savedCity = prefs.getString("city_name", null)
                if (savedCity != null) {
                    UserLocation(
                        cityName = savedCity,
                        countryName = prefs.getString("country_name", "") ?: "",
                        latitude = prefs.getString("latitude", "0.0")?.toDoubleOrNull() ?: 0.0,
                        longitude = prefs.getString("longitude", "0.0")?.toDoubleOrNull() ?: 0.0,
                        isAutoDetected = prefs.getBoolean("is_auto_detected", false)
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

    init {
        // Attempt initial detection immediately if permission is available, but ONLY if user hasn't pinned a primary location
        if (_primaryLocation.value == null) {
            detectAndUseCurrentLocation(context)
        }
    }

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

    // Per-prayer notification map
    private val _prayerNotifications = MutableStateFlow(
        PrayerType.values().associateWith { prayer ->
            // Fard prayers ON by default, optional/derived OFF by default
            val defaultOn = prayer.isFard && prayer != PrayerType.JUMMAH
            prefs.getBoolean("notif_${prayer.name}", defaultOn)
        }
    )
    val prayerNotifications: StateFlow<Map<PrayerType, Boolean>> = _prayerNotifications.asStateFlow()

    fun updateLocation(location: UserLocation, autoAlignMethod: Boolean = true) {
        _userLocation.value = location
        // Only persist as general default city if user has NOT set a primary location
        if (_primaryLocation.value == null) {
            prefs.edit()
                .putString("city_name", location.cityName)
                .putString("country_name", location.countryName)
                .putString("latitude", location.latitude.toString())
                .putString("longitude", location.longitude.toString())
                .putBoolean("is_auto_detected", location.isAutoDetected)
                .apply()
        }

        if (autoAlignMethod && location.countryName.isNotBlank()) {
            val recommendedMethod = com.example.domain.model.getRecommendedMethodForLocation(location.countryName, location.cityName)
            _calculationMethod.value = recommendedMethod
            prefs.edit().putInt("calc_method", recommendedMethod.id).apply()
        }
    }

    fun setPrimaryLocation(location: UserLocation) {
        _primaryLocation.value = location
        prefs.edit()
            .putBoolean("has_primary_location", true)
            .putString("primary_city_name", location.cityName)
            .putString("primary_country_name", location.countryName)
            .putString("primary_latitude", location.latitude.toString())
            .putString("primary_longitude", location.longitude.toString())
            .putBoolean("primary_is_auto", location.isAutoDetected)
            .apply()

        try {
            com.example.notification.PrayerNotificationManager.rescheduleAlarmsFromSavedState(context)
        } catch (_: Exception) {}
    }

    fun clearPrimaryLocation() {
        _primaryLocation.value = null
        prefs.edit()
            .putBoolean("has_primary_location", false)
            .remove("primary_city_name")
            .remove("primary_country_name")
            .remove("primary_latitude")
            .remove("primary_longitude")
            .remove("primary_is_auto")
            .apply()

        try {
            com.example.notification.PrayerNotificationManager.rescheduleAlarmsFromSavedState(context)
        } catch (_: Exception) {}
    }

    fun getNotificationLocation(): UserLocation {
        return _primaryLocation.value ?: _userLocation.value
    }

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
    }

    fun setHijriAdjustment(adjustment: Int) {
        _hijriAdjustment.value = adjustment
        prefs.edit().putInt("hijri_adjustment", adjustment).apply()
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
    }

    fun setJuristicSchool(school: JuristicSchool) {
        _juristicSchool.value = school
        prefs.edit().putInt("juristic_school", school.id).apply()
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
    }

    fun getLocationZoneId(): java.time.ZoneId {
        return getZoneIdForLocation(_userLocation.value)
    }

    fun getZoneIdForLocation(loc: UserLocation): java.time.ZoneId {
        val country = loc.countryName.lowercase().trim()
        val city = loc.cityName.lowercase().trim()

        val zoneName = when {
            country.contains("pakistan") || city.contains("karachi") || city.contains("lahore") || city.contains("islamabad") || city.contains("rawalpindi") || city.contains("peshawar") || city.contains("quetta") -> "Asia/Karachi"
            country.contains("saudi") || city.contains("makkah") || city.contains("mecca") || city.contains("madinah") || city.contains("medina") || city.contains("riyadh") || city.contains("jeddah") -> "Asia/Riyadh"
            country.contains("emirates") || country.contains("uae") || city.contains("dubai") || city.contains("abu dhabi") || city.contains("sharjah") -> "Asia/Dubai"
            country.contains("qatar") || city.contains("doha") -> "Asia/Qatar"
            country.contains("kuwait") -> "Asia/Kuwait"
            country.contains("oman") || city.contains("muscat") -> "Asia/Muscat"
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
            country.contains("united kingdom") || country.contains("uk") || city.contains("london") || city.contains("birmingham") || city.contains("manchester") -> "Europe/London"
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

            if (loc != null) {
                applyLocationFromCoordinates(appContext, loc.latitude, loc.longitude)
                return true
            } else {
                // If last known is null, request a single fresh fix
                val provider = if (isGpsEnabled) LocationManager.GPS_PROVIDER else LocationManager.NETWORK_PROVIDER
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
                    locationManager.requestLocationUpdates(provider, 0L, 0f, listener, android.os.Looper.getMainLooper())
                }
                return true
            }
        } catch (_: Exception) {}
        return false
    }

    private fun applyLocationFromCoordinates(appContext: Context, lat: Double, lng: Double) {
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

        val detected = UserLocation(
            cityName = cityName,
            countryName = countryName,
            latitude = lat,
            longitude = lng,
            isAutoDetected = true
        )
        updateLocation(detected)
    }

    companion object {
        @Volatile
        private var INSTANCE: SettingsRepository? = null

        fun getInstance(context: Context): SettingsRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = SettingsRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
