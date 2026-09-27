package com.example.domain.model

import java.time.LocalDate

data class DailyGoalItem(
    val goalId: String,
    val titleEnglish: String,
    val titleUrdu: String,
    val category: GoalCategory,
    val isCompleted: Boolean = false,
    val date: LocalDate = LocalDate.now()
)

enum class GoalCategory {
    FARD_SALAT,
    DAILY_IBADAH,
    CUSTOM
}

data class DayGoalItemRecord(
    val goalId: String,
    val titleEnglish: String,
    val titleUrdu: String,
    val category: GoalCategory,
    val isCompleted: Boolean
)

data class DayGoalSummary(
    val date: LocalDate,
    val fardCompleted: Int,
    val fardTotal: Int = 5,
    val deedsCompleted: Int,
    val deedsTotal: Int = 7,
    val customCompleted: Int,
    val customTotal: Int,
    val totalCompleted: Int,
    val totalGoals: Int,
    val completionPercentage: Int,
    val items: List<DayGoalItemRecord> = emptyList()
)

data class GoalsReportData(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val requestedDays: Int,
    val actualDaysAvailable: Int,
    val dailySummaries: List<DayGoalSummary>,
    val overallPercentage: Int,
    val totalFardCompleted: Int,
    val totalFardPossible: Int,
    val totalDeedsCompleted: Int,
    val totalDeedsPossible: Int,
    val totalCustomCompleted: Int,
    val totalCustomPossible: Int
)

data class DuaCategory(
    val id: String,
    val englishName: String,
    val urduName: String,
    val iconSymbol: String,
    val description: String
)

data class DuaItem(
    val id: String,
    val category: String,
    val title: String,
    val arabicText: String,
    val transliteration: String,
    val englishTranslation: String,
    val urduTranslation: String,
    val reference: String,
    val repeatCount: Int = 1,
    val audioUrl: String? = null
)

data class AsmaAlHusna(
    val number: Int,
    val arabicName: String,
    val transliteration: String,
    val urduMeaning: String,
    val englishMeaning: String
)

enum class BookmarkType(val displayName: String) {
    QURAN_AYAH("Quran"),
    HADITH("Hadith"),
    DUA("Dua")
}

data class IslamicEvent(
    val title: String,
    val urduTitle: String,
    val hijriDate: String,
    val gregorianEquivalent: String
)

data class BookmarkItem(
    val id: Long = 0,
    val type: BookmarkType,
    val referenceId: String, // e.g. "surah_2_ayah_255", "hadith_bukhari_1"
    val title: String,
    val subtitle: String,
    val arabicText: String,
    val translation: String,
    val timestamp: Long = System.currentTimeMillis()
)
