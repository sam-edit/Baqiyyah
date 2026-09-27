package com.example.data.repository

import android.content.Context
import com.example.data.local.dao.GoalDao
import com.example.data.local.entities.GoalEntity
import com.example.domain.model.DailyGoalItem
import com.example.domain.model.GoalCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

class GoalsRepository(private val goalDao: GoalDao, context: Context) {

    private val prefs = context.getSharedPreferences("custom_goals_prefs", Context.MODE_PRIVATE)

    private val defaultGoalDefinitions = listOf(
        // 5 Fard Prayers
        DailyGoalItem("fajr", "Fajr Prayer", "نماز فجر", GoalCategory.FARD_SALAT),
        DailyGoalItem("dhuhr", "Dhuhr Prayer", "نماز ظہر", GoalCategory.FARD_SALAT),
        DailyGoalItem("asr", "Asr Prayer", "نماز عصر", GoalCategory.FARD_SALAT),
        DailyGoalItem("maghrib", "Maghrib Prayer", "نماز مغرب", GoalCategory.FARD_SALAT),
        DailyGoalItem("isha", "Isha Prayer", "نماز عشاء", GoalCategory.FARD_SALAT),

        // Daily Spiritual Deeds
        DailyGoalItem("quran", "Read Quran Today", "آج قرآن پڑھا", GoalCategory.DAILY_IBADAH),
        DailyGoalItem("good_deed", "Performed a Good Deed", "آج کوئی نیکی کی", GoalCategory.DAILY_IBADAH),
        DailyGoalItem("azkar", "Morning / Evening Azkar", "اذکار کیے", GoalCategory.DAILY_IBADAH),
        DailyGoalItem("dua", "Supplicated / Made Dua", "دعا کی", GoalCategory.DAILY_IBADAH),
        DailyGoalItem("durood", "Recited Durood Shareef", "درود پاک پڑھا", GoalCategory.DAILY_IBADAH),
        DailyGoalItem("istighfar", "Sought Forgiveness (Istighfar)", "استغفار کیا", GoalCategory.DAILY_IBADAH),
        DailyGoalItem("charity", "Given Charity / Helped Someone", "صدقہ / کسی کی مدد کی", GoalCategory.DAILY_IBADAH)
    )

    private val _customGoals = MutableStateFlow(loadCustomGoals())
    val customGoalsFlow = _customGoals.asStateFlow()

    private fun loadCustomGoals(): List<DailyGoalItem> {
        val raw = prefs.getString("custom_goals_list", null) ?: return emptyList()
        val list = mutableListOf<DailyGoalItem>()
        try {
            val array = JSONArray(raw)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    DailyGoalItem(
                        goalId = obj.getString("id"),
                        titleEnglish = obj.getString("title"),
                        titleUrdu = obj.optString("urdu", obj.getString("title")),
                        category = GoalCategory.CUSTOM
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    private fun saveCustomGoals(list: List<DailyGoalItem>) {
        val array = JSONArray()
        for (item in list) {
            val obj = JSONObject().apply {
                put("id", item.goalId)
                put("title", item.titleEnglish)
                put("urdu", item.titleUrdu)
            }
            array.put(obj)
        }
        prefs.edit().putString("custom_goals_list", array.toString()).apply()
    }

    fun addCustomGoal(title: String, titleUrdu: String = "") {
        val trimmed = title.trim()
        if (trimmed.isBlank()) return
        val id = "custom_" + System.currentTimeMillis()
        val item = DailyGoalItem(
            goalId = id,
            titleEnglish = trimmed,
            titleUrdu = if (titleUrdu.isNotBlank()) titleUrdu.trim() else trimmed,
            category = GoalCategory.CUSTOM
        )
        val updated = _customGoals.value + item
        _customGoals.value = updated
        saveCustomGoals(updated)
    }

    fun deleteCustomGoal(goalId: String) {
        val updated = _customGoals.value.filter { it.goalId != goalId }
        _customGoals.value = updated
        saveCustomGoals(updated)
    }

    fun getGoalsForDate(date: LocalDate): Flow<List<DailyGoalItem>> {
        val dateString = date.toString()
        return combine(goalDao.getGoalsForDate(dateString), _customGoals) { entities, customList ->
            val allDefs = defaultGoalDefinitions + customList
            val entityMap = entities.associateBy { it.goalId }
            allDefs.map { def ->
                val saved = entityMap[def.goalId]
                def.copy(
                    isCompleted = saved?.isCompleted ?: false,
                    date = date
                )
            }
        }
    }

    suspend fun toggleGoal(goalId: String, date: LocalDate, currentStatus: Boolean) {
        val dateString = date.toString()
        val newStatus = !currentStatus
        goalDao.insertOrUpdateGoal(
            GoalEntity(
                goalId = goalId,
                date = dateString,
                isCompleted = newStatus,
                completedTimestamp = if (newStatus) System.currentTimeMillis() else null
            )
        )
    }

    fun getStartDate(): LocalDate {
        val stored = prefs.getString("goals_install_date", null)
        if (stored != null) {
            try {
                return LocalDate.parse(stored)
            } catch (_: Exception) {}
        }
        val today = LocalDate.now()
        prefs.edit().putString("goals_install_date", today.toString()).apply()
        return today
    }

    suspend fun cleanOldGoals() {
        try {
            // Keep last 60 days of data only. Delete anything older than today - 60 days.
            val cutoff = LocalDate.now().minusDays(60).toString()
            goalDao.deleteGoalsOlderThan(cutoff)
        } catch (_: Exception) {}
    }

    suspend fun getReportData(days: Int): com.example.domain.model.GoalsReportData {
        val today = LocalDate.now()
        val maxDaysBack = (days - 1).coerceAtLeast(0).toLong()
        val requestedStartDate = today.minusDays(maxDaysBack)
        val effectiveStartDate = requestedStartDate
        val effectiveEndDate = today

        val entities = goalDao.getGoalsBetweenDates(effectiveStartDate.toString(), effectiveEndDate.toString())
        val entitiesByDate = entities.groupBy { it.date }

        val customGoalsList = _customGoals.value
        val dailySummaries = mutableListOf<com.example.domain.model.DayGoalSummary>()

        var totalFardCompleted = 0
        var totalFardPossible = 0
        var totalDeedsCompleted = 0
        var totalDeedsPossible = 0
        var totalCustomCompleted = 0
        var totalCustomPossible = 0

        var cur = effectiveStartDate
        while (!cur.isAfter(effectiveEndDate)) {
            val dateStr = cur.toString()
            val dayEntities = entitiesByDate[dateStr] ?: emptyList()
            val entityMap = dayEntities.associateBy { it.goalId }

            val itemsList = mutableListOf<com.example.domain.model.DayGoalItemRecord>()

            // 5 Fard Prayers
            defaultGoalDefinitions.filter { it.category == GoalCategory.FARD_SALAT }.forEach { def ->
                val completed = entityMap[def.goalId]?.isCompleted == true
                itemsList.add(
                    com.example.domain.model.DayGoalItemRecord(
                        goalId = def.goalId,
                        titleEnglish = def.titleEnglish,
                        titleUrdu = def.titleUrdu,
                        category = GoalCategory.FARD_SALAT,
                        isCompleted = completed
                    )
                )
            }

            // 7 Daily Deeds
            defaultGoalDefinitions.filter { it.category == GoalCategory.DAILY_IBADAH }.forEach { def ->
                val completed = entityMap[def.goalId]?.isCompleted == true
                itemsList.add(
                    com.example.domain.model.DayGoalItemRecord(
                        goalId = def.goalId,
                        titleEnglish = def.titleEnglish,
                        titleUrdu = def.titleUrdu,
                        category = GoalCategory.DAILY_IBADAH,
                        isCompleted = completed
                    )
                )
            }

            // Custom Goals
            customGoalsList.forEach { custom ->
                val completed = entityMap[custom.goalId]?.isCompleted == true
                itemsList.add(
                    com.example.domain.model.DayGoalItemRecord(
                        goalId = custom.goalId,
                        titleEnglish = custom.titleEnglish,
                        titleUrdu = custom.titleUrdu,
                        category = GoalCategory.CUSTOM,
                        isCompleted = completed
                    )
                )
            }

            val fardCompleted = itemsList.filter { it.category == GoalCategory.FARD_SALAT }.count { it.isCompleted }
            val deedsCompleted = itemsList.filter { it.category == GoalCategory.DAILY_IBADAH }.count { it.isCompleted }
            val customCompleted = itemsList.filter { it.category == GoalCategory.CUSTOM }.count { it.isCompleted }

            val fardTotal = 5
            val deedsTotal = 7
            val customTotal = customGoalsList.size
            val totalCompleted = fardCompleted + deedsCompleted + customCompleted
            val totalGoals = fardTotal + deedsTotal + customTotal
            val pct = if (totalGoals > 0) (totalCompleted * 100) / totalGoals else 0

            dailySummaries.add(
                com.example.domain.model.DayGoalSummary(
                    date = cur,
                    fardCompleted = fardCompleted,
                    fardTotal = fardTotal,
                    deedsCompleted = deedsCompleted,
                    deedsTotal = deedsTotal,
                    customCompleted = customCompleted,
                    customTotal = customTotal,
                    totalCompleted = totalCompleted,
                    totalGoals = totalGoals,
                    completionPercentage = pct,
                    items = itemsList
                )
            )

            totalFardCompleted += fardCompleted
            totalFardPossible += fardTotal
            totalDeedsCompleted += deedsCompleted
            totalDeedsPossible += deedsTotal
            totalCustomCompleted += customCompleted
            totalCustomPossible += customTotal

            cur = cur.plusDays(1)
        }

        val allTotalCompleted = totalFardCompleted + totalDeedsCompleted + totalCustomCompleted
        val allTotalPossible = totalFardPossible + totalDeedsPossible + totalCustomPossible
        val overallPct = if (allTotalPossible > 0) (allTotalCompleted * 100) / allTotalPossible else 0

        dailySummaries.sortByDescending { it.date }

        return com.example.domain.model.GoalsReportData(
            startDate = effectiveStartDate,
            endDate = effectiveEndDate,
            requestedDays = days,
            actualDaysAvailable = dailySummaries.size,
            dailySummaries = dailySummaries,
            overallPercentage = overallPct,
            totalFardCompleted = totalFardCompleted,
            totalFardPossible = totalFardPossible,
            totalDeedsCompleted = totalDeedsCompleted,
            totalDeedsPossible = totalDeedsPossible,
            totalCustomCompleted = totalCustomCompleted,
            totalCustomPossible = totalCustomPossible
        )
    }
}
