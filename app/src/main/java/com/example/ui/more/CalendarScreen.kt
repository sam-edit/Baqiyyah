package com.example.ui.more

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.AuthoritativeContentProvider
import com.example.domain.model.IslamicEvent
import com.example.ui.components.AlDeenText
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class HijriDateInfo(
    val day: Int,
    val month: Int,
    val year: Int,
    val monthNameEnglish: String,
    val monthNameArabic: String
)

private val hijriMonthNamesEnglish = listOf(
    "Muharram", "Safar", "Rabi' al-Awwal", "Rabi' al-Thani",
    "Jumada al-Ula", "Jumada al-Akhirah", "Rajab", "Sha'ban",
    "Ramadan", "Shawwal", "Dhu al-Qi'dah", "Dhu al-Hijjah"
)

private val hijriMonthNamesArabic = listOf(
    "مُحَرَّم", "صَفَر", "رَبِيع الأَوَّل", "رَبِيع الآخِر",
    "جُمَادَى الأُولَى", "جُمَادَى الآخِرَة", "رَجَب", "شَعْبَان",
    "رَمَضَان", "شَوَّال", "ذُو القَعْدَة", "ذُو الحِجَّة"
)

// Convert Gregorian to Hijri algorithm
fun convertGregorianToHijri(year: Int, month: Int, day: Int, adjustmentDays: Int = 0): HijriDateInfo {
    var y = year
    var m = month // 1-based
    if (m <= 2) {
        y -= 1
        m += 12
    }
    val a = y / 100
    val b = 2 - a + a / 4
    val jd = (365.25 * (y + 4716)).toLong() + (30.6001 * (m + 1)).toLong() + day + b - 1524

    val adjustedJd = jd + adjustmentDays
    val l = adjustedJd - 1948440 + 10632
    val n = (l - 1) / 10631
    val lPrime = l - 10631 * n + 354
    val j = ((10985 - lPrime) / 5316) * ((50 * lPrime) / 17719) + (lPrime / 5670) * ((43 * lPrime) / 15238)
    val lDoublePrime = lPrime - ((30 - j) / 15) * ((17719 * j) / 50) - (j / 16) * ((15238 * j) / 43) + 29
    val hMonth = ((24 * lDoublePrime) / 709).toInt()
    val hDay = (lDoublePrime - (709 * hMonth) / 24).toInt()
    val hYear = (30 * n + j - 30).toInt()

    val safeMonth = hMonth.coerceIn(1, 12)
    val safeDay = hDay.coerceIn(1, 30)

    return HijriDateInfo(
        day = safeDay,
        month = safeMonth,
        year = hYear,
        monthNameEnglish = hijriMonthNamesEnglish[safeMonth - 1],
        monthNameArabic = hijriMonthNamesArabic[safeMonth - 1]
    )
}

// Convert English numerals to Arabic numerals e.g. 15 -> ١٥
fun toArabicNumerals(number: Int): String {
    val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
    val str = number.toString()
    val builder = StringBuilder()
    for (ch in str) {
        if (ch in '0'..'9') {
            builder.append(arabicDigits[ch - '0'])
        } else {
            builder.append(ch)
        }
    }
    return builder.toString()
}

data class CalendarDayItem(
    val year: Int,
    val month: Int, // 1-based
    val day: Int,
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val hijri: HijriDateInfo,
    val event: IslamicEvent? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    hijriAdjustment: Int,
    onAdjustmentChange: (Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allEvents = AuthoritativeContentProvider.islamicEvents

    // Calendar navigation state
    val todayCal = remember { Calendar.getInstance() }
    val todayYear = remember { todayCal.get(Calendar.YEAR) }
    val todayMonth = remember { todayCal.get(Calendar.MONTH) + 1 }
    val todayDay = remember { todayCal.get(Calendar.DAY_OF_MONTH) }

    var currentDisplayYear by remember { mutableIntStateOf(todayYear) }
    var currentDisplayMonth by remember { mutableIntStateOf(todayMonth) } // 1-based

    var selectedYear by remember { mutableIntStateOf(todayYear) }
    var selectedMonth by remember { mutableIntStateOf(todayMonth) }
    var selectedDay by remember { mutableIntStateOf(todayDay) }

    val selectedHijri = remember(selectedYear, selectedMonth, selectedDay, hijriAdjustment) {
        convertGregorianToHijri(selectedYear, selectedMonth, selectedDay, hijriAdjustment)
    }

    // Days of week header: Starts on Monday as specifically requested
    val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val urduDayNames = listOf("پیر", "منگل", "بدھ", "جمعرات", "جمعہ", "ہفتہ", "اتوار")

    // Generate days for currentDisplayMonth
    val calendarDays = remember(currentDisplayYear, currentDisplayMonth, hijriAdjustment) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, currentDisplayYear)
        cal.set(Calendar.MONTH, currentDisplayMonth - 1)
        cal.set(Calendar.DAY_OF_MONTH, 1)

        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        // Day of week for 1st of month: Calendar.MONDAY = 2, Calendar.SUNDAY = 1
        // We want Monday = 0, Tuesday = 1, ..., Sunday = 6
        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val leadingSpaces = when (firstDayOfWeek) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
        }

        // Previous month days for padding
        cal.add(Calendar.MONTH, -1)
        val daysInPrevMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val prevYear = cal.get(Calendar.YEAR)
        val prevMonth = cal.get(Calendar.MONTH) + 1

        val list = mutableListOf<CalendarDayItem>()

        // Leading padding from previous month
        for (i in (daysInPrevMonth - leadingSpaces + 1)..daysInPrevMonth) {
            val hijri = convertGregorianToHijri(prevYear, prevMonth, i, hijriAdjustment)
            list.add(
                CalendarDayItem(
                    year = prevYear,
                    month = prevMonth,
                    day = i,
                    isCurrentMonth = false,
                    isToday = (prevYear == todayYear && prevMonth == todayMonth && i == todayDay),
                    hijri = hijri
                )
            )
        }

        // Current month days
        for (d in 1..daysInMonth) {
            val hijri = convertGregorianToHijri(currentDisplayYear, currentDisplayMonth, d, hijriAdjustment)
            // Match potential Islamic event
            val matchedEvent = allEvents.firstOrNull { ev ->
                ev.hijriDate.contains("${hijri.day} ${hijri.monthNameEnglish}", ignoreCase = true) ||
                ev.hijriDate.contains("${hijri.day} ${hijri.monthNameArabic}")
            }

            list.add(
                CalendarDayItem(
                    year = currentDisplayYear,
                    month = currentDisplayMonth,
                    day = d,
                    isCurrentMonth = true,
                    isToday = (currentDisplayYear == todayYear && currentDisplayMonth == todayMonth && d == todayDay),
                    hijri = hijri,
                    event = matchedEvent
                )
            )
        }

        // Trailing padding to complete 7-day grid rows (multiples of 7)
        var nextDay = 1
        val nextCal = Calendar.getInstance()
        nextCal.set(Calendar.YEAR, currentDisplayYear)
        nextCal.set(Calendar.MONTH, currentDisplayMonth - 1)
        nextCal.add(Calendar.MONTH, 1)
        val nextYear = nextCal.get(Calendar.YEAR)
        val nextMonth = nextCal.get(Calendar.MONTH) + 1

        while (list.size % 7 != 0) {
            val hijri = convertGregorianToHijri(nextYear, nextMonth, nextDay, hijriAdjustment)
            list.add(
                CalendarDayItem(
                    year = nextYear,
                    month = nextMonth,
                    day = nextDay,
                    isCurrentMonth = false,
                    isToday = (nextYear == todayYear && nextMonth == todayMonth && nextDay == todayDay),
                    hijri = hijri
                )
            )
            nextDay++
        }

        list
    }

    // Month name in English for header
    val displayMonthName = remember(currentDisplayMonth, currentDisplayYear) {
        val cal = Calendar.getInstance()
        cal.set(Calendar.MONTH, currentDisplayMonth - 1)
        cal.set(Calendar.YEAR, currentDisplayYear)
        SimpleDateFormat("MMMM yyyy", Locale.ENGLISH).format(cal.time)
    }

    // Representative Hijri months span for this Gregorian month
    val hijriSpanHeader = remember(calendarDays) {
        val firstDayHijri = calendarDays.firstOrNull { it.isCurrentMonth }?.hijri
        val lastDayHijri = calendarDays.lastOrNull { it.isCurrentMonth }?.hijri
        if (firstDayHijri != null && lastDayHijri != null) {
            if (firstDayHijri.monthNameEnglish == lastDayHijri.monthNameEnglish) {
                "${firstDayHijri.monthNameEnglish} ${firstDayHijri.year} AH"
            } else {
                "${firstDayHijri.monthNameEnglish} / ${lastDayHijri.monthNameEnglish} ${lastDayHijri.year} AH"
            }
        } else {
            "Hijri Calendar"
        }
    }

    // Selected day info
    val selectedEvent = remember(selectedYear, selectedMonth, selectedDay, selectedHijri) {
        allEvents.firstOrNull { ev ->
            ev.hijriDate.contains("${selectedHijri.day} ${selectedHijri.monthNameEnglish}", ignoreCase = true) ||
            ev.hijriDate.contains("${selectedHijri.day} ${selectedHijri.monthNameArabic}")
        }
    }

    // Determine day of week for selected date
    val selectedDayOfWeek = remember(selectedYear, selectedMonth, selectedDay) {
        val cal = Calendar.getInstance()
        cal.set(selectedYear, selectedMonth - 1, selectedDay)
        SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.ENGLISH).format(cal.time)
    }

    // Is Sunnah fasting day? (Monday or Thursday, or White Days 13, 14, 15)
    val isSunnahFast = remember(selectedYear, selectedMonth, selectedDay, selectedHijri) {
        val cal = Calendar.getInstance()
        cal.set(selectedYear, selectedMonth - 1, selectedDay)
        val dow = cal.get(Calendar.DAY_OF_WEEK)
        dow == Calendar.MONDAY || dow == Calendar.THURSDAY || selectedHijri.day in 13..15
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        AlDeenText(
                            text = "Islamic Calendar (التقويم الإسلامي)",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                        )
                        Text(
                            text = hijriSpanHeader,
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("calendar_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = EmeraldPrimary)
                    }
                },
                actions = {
                    // Quick return to "Today" button
                    TextButton(
                        onClick = {
                            currentDisplayYear = todayYear
                            currentDisplayMonth = todayMonth
                            selectedYear = todayYear
                            selectedMonth = todayMonth
                            selectedDay = todayDay
                        },
                        modifier = Modifier.testTag("calendar_today_btn")
                    ) {
                        AlDeenText("Today (آج)", fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        modifier = modifier.testTag("calendar_screen")
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = AlDeenTokens.SpacingLarge),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Calendar Header Navigation: Month Selector (< September 2026 >)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = AlDeenTokens.ShapeCard,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MintBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    if (currentDisplayMonth == 1) {
                                        currentDisplayMonth = 12
                                        currentDisplayYear -= 1
                                    } else {
                                        currentDisplayMonth -= 1
                                    }
                                },
                                modifier = Modifier.testTag("calendar_prev_month")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Month", tint = EmeraldPrimary)
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = displayMonthName,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldPrimary,
                                        fontSize = 19.sp
                                    )
                                )
                                Text(
                                    text = hijriSpanHeader,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = GoldAccent
                                    )
                                )
                            }

                            IconButton(
                                onClick = {
                                    if (currentDisplayMonth == 12) {
                                        currentDisplayMonth = 1
                                        currentDisplayYear += 1
                                    } else {
                                        currentDisplayMonth += 1
                                    }
                                },
                                modifier = Modifier.testTag("calendar_next_month")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Month", tint = EmeraldPrimary)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 2. Day-of-week row: Starts on Monday (Mon, Tue, Wed, Thu, Fri, Sat, Sun)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            dayNames.forEachIndexed { idx, name ->
                                val isFriday = idx == 4 // Friday (Jumu'ah)
                                Column(
                                    modifier = Modifier.weight(1f),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (isFriday) EmeraldPrimary else TextSecondary,
                                            fontSize = 12.sp
                                        )
                                    )
                                    AlDeenText(
                                        text = urduDayNames[idx],
                                        style = AlDeenTypography.UrduBody.copy(
                                            fontSize = 10.sp,
                                            color = if (isFriday) EmeraldPrimary else TextSecondary.copy(alpha = 0.8f)
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = MintBorder.copy(alpha = 0.6f))
                        Spacer(modifier = Modifier.height(8.dp))

                        // 3. Complete 7-Column Calendar Grid with English + Arabic Dates in every cell
                        val chunkedRows = calendarDays.chunked(7)
                        chunkedRows.forEach { weekRow ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                weekRow.forEach { dayItem ->
                                    val isSelected = dayItem.year == selectedYear &&
                                            dayItem.month == selectedMonth &&
                                            dayItem.day == selectedDay

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(52.dp)
                                            .padding(2.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                when {
                                                    isSelected -> EmeraldPrimary
                                                    dayItem.isToday -> EmeraldContainer
                                                    else -> Color.Transparent
                                                }
                                            )
                                            .clickable {
                                                selectedYear = dayItem.year
                                                selectedMonth = dayItem.month
                                                selectedDay = dayItem.day
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            // English Day Number
                                            Text(
                                                text = "${dayItem.day}",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = if (dayItem.isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = when {
                                                        isSelected -> Color.White
                                                        !dayItem.isCurrentMonth -> TextSecondary.copy(alpha = 0.35f)
                                                        dayItem.isToday -> EmeraldPrimary
                                                        else -> TextPrimary
                                                    },
                                                    fontSize = 14.sp
                                                )
                                            )

                                            // Arabic Hijri Day Number (e.g. ١٥)
                                            AlDeenText(
                                                text = toArabicNumerals(dayItem.hijri.day),
                                                style = AlDeenTypography.ArabicBody.copy(
                                                    color = when {
                                                        isSelected -> GoldAccent
                                                        !dayItem.isCurrentMonth -> TextSecondary.copy(alpha = 0.35f)
                                                        dayItem.isToday -> EmeraldPrimary
                                                        else -> TextSecondary
                                                    },
                                                    fontSize = 11.sp,
                                                    lineHeight = 12.sp
                                                )
                                            )

                                            // Dot indicator if an event is on this day
                                            if (dayItem.event != null) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(4.dp)
                                                        .clip(CircleShape)
                                                        .background(if (isSelected) GoldAccent else EmeraldPrimary)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Hijri Date Correction Card (placed right below the calendar box)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = AlDeenTokens.ShapeCard,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MintBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Hijri Date Correction",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                )
                                AlDeenText(
                                    text = "چاند کی رویت کے مطابق تاریخ ایڈجسٹ کریں",
                                    style = AlDeenTypography.UrduBody.copy(
                                        color = EmeraldPrimary,
                                        fontSize = 12.sp
                                    )
                                )
                            }

                            Surface(
                                shape = AlDeenTokens.ShapePill,
                                color = EmeraldContainer
                            ) {
                                Text(
                                    text = if (hijriAdjustment > 0) "+$hijriAdjustment Days" else if (hijriAdjustment < 0) "$hijriAdjustment Days" else "Exact",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldPrimary
                                    ),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            listOf(-2, -1, 0, 1, 2).forEach { offset ->
                                val isSelected = hijriAdjustment == offset
                                val label = if (offset > 0) "+$offset" else "$offset"
                                Surface(
                                    onClick = { onAdjustmentChange(offset) },
                                    shape = AlDeenTokens.ShapePill,
                                    color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.testTag("hijri_adjust_$offset")
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = if (isSelected) Color.White else TextPrimary,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Details Card for Selected Date
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = AlDeenTokens.ShapeCard,
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DashboardGradient)
                            .padding(AlDeenTokens.SpacingLarge)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Selected Date Details",
                                        style = MaterialTheme.typography.labelSmall.copy(color = GoldAccent, fontWeight = FontWeight.Bold)
                                    )
                                }

                                if (isSunnahFast) {
                                    Surface(
                                        shape = AlDeenTokens.ShapePill,
                                        color = GoldAccent.copy(alpha = 0.25f),
                                        border = BorderStroke(0.8.dp, GoldAccent)
                                    ) {
                                        Text(
                                            text = "Sunnah Fasting Day",
                                            style = MaterialTheme.typography.labelSmall.copy(color = Color.White, fontWeight = FontWeight.Bold),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // English Date
                            Text(
                                text = selectedDayOfWeek,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp
                                )
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // Full Hijri Date in English & Arabic
                            Text(
                                text = "${selectedHijri.day} ${selectedHijri.monthNameEnglish} ${selectedHijri.year} AH",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 24.sp
                                )
                            )

                            AlDeenText(
                                text = "${toArabicNumerals(selectedHijri.day)} ${selectedHijri.monthNameArabic} ${toArabicNumerals(selectedHijri.year)} هـ",
                                style = AlDeenTypography.ArabicBody.copy(
                                    color = GoldAccent,
                                    fontSize = 18.sp
                                )
                            )

                            // Islamic Event badge if applicable
                            if (selectedEvent != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White.copy(alpha = 0.15f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Star, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = selectedEvent.title,
                                                style = MaterialTheme.typography.bodyMedium.copy(color = Color.White, fontWeight = FontWeight.Bold)
                                            )
                                            Text(
                                                text = selectedEvent.gregorianEquivalent,
                                                style = MaterialTheme.typography.labelSmall.copy(color = Color.White.copy(alpha = 0.8f))
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. Holy Islamic Events of the Year
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AlDeenText(
                        text = "Holy Islamic Events (اہم اسلامی ایام)",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                }
            }

            items(allEvents) { event ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = AlDeenTokens.ShapeCard,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(AlDeenTokens.CardBorderWidth, MintBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = EmeraldContainer,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Event, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(22.dp))
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = event.title,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            AlDeenText(
                                text = event.urduTitle,
                                style = AlDeenTypography.UrduBody.copy(
                                    color = EmeraldPrimary,
                                    fontSize = 13.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = event.gregorianEquivalent,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextSecondary,
                                    lineHeight = 15.sp
                                )
                            )
                        }

                        Surface(
                            shape = AlDeenTokens.ShapePill,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = event.hijriDate,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary,
                                    fontSize = 11.sp
                                ),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
