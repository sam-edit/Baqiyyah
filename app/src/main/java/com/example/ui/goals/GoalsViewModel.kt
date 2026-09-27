package com.example.ui.goals

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AlDeenDatabase
import com.example.data.repository.GoalsRepository
import com.example.domain.model.DailyGoalItem
import com.example.domain.model.GoalCategory
import com.example.util.GoalsPdfGenerator
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

class GoalsViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AlDeenDatabase.getInstance(application)
    val goalsRepository = GoalsRepository(database.goalDao(), application)
    val settingsRepository = com.example.data.repository.SettingsRepository.getInstance(application)

    fun getToday(): LocalDate = settingsRepository.getTodayDateForLocation()

    fun getInstallDate(): LocalDate = goalsRepository.getStartDate()

    fun getEarliestSelectableDate(): LocalDate {
        // 60-day tracking window: dates older than 60 days are disabled, under 60 days are enabled
        return getToday().minusDays(59)
    }

    fun isSelectedDateToday(): Boolean {
        return _selectedDate.value == getToday()
    }

    private val _selectedDate = MutableStateFlow(getToday())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _isGeneratingReport = MutableStateFlow(false)
    val isGeneratingReport: StateFlow<Boolean> = _isGeneratingReport.asStateFlow()

    init {
        // Automatically delete goals records older than 60 days to keep DB within 60-day retention
        viewModelScope.launch {
            goalsRepository.cleanOldGoals()
        }

        // Automatically check at 12:00 AM of the selected location's timezone to roll over to the fresh day (unchecking all goals)
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(15_000L)
                val currentLocToday = getToday()
                if (_selectedDate.value.isBefore(currentLocToday) && _selectedDate.value == currentLocToday.minusDays(1)) {
                    _selectedDate.value = currentLocToday
                }
            }
        }
    }

    val goalsForDate: StateFlow<List<DailyGoalItem>> = _selectedDate.flatMapLatest { date ->
        goalsRepository.getGoalsForDate(date)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val fardPrayers: StateFlow<List<DailyGoalItem>> = goalsForDate.map { list ->
        list.filter { it.category == GoalCategory.FARD_SALAT }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dailyDeeds: StateFlow<List<DailyGoalItem>> = goalsForDate.map { list ->
        list.filter { it.category == GoalCategory.DAILY_IBADAH }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customGoals: StateFlow<List<DailyGoalItem>> = goalsForDate.map { list ->
        list.filter { it.category == GoalCategory.CUSTOM }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completedCount: StateFlow<Int> = goalsForDate.map { list ->
        list.count { it.isCompleted }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val progressFraction: StateFlow<Float> = goalsForDate.map { list ->
        if (list.isEmpty()) 0f else list.count { it.isCompleted }.toFloat() / list.size.toFloat()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    fun isDateSelectable(date: LocalDate): Boolean {
        val earliest = getEarliestSelectableDate()
        val today = getToday()
        return !date.isBefore(earliest) && !date.isAfter(today)
    }

    fun selectDate(date: LocalDate) {
        if (isDateSelectable(date)) {
            _selectedDate.value = date
        }
    }

    fun previousDay() {
        val prev = _selectedDate.value.minusDays(1)
        if (!prev.isBefore(getEarliestSelectableDate())) {
            _selectedDate.value = prev
        }
    }

    fun nextDay() {
        val next = _selectedDate.value.plusDays(1)
        if (!next.isAfter(getToday())) {
            _selectedDate.value = next
        }
    }

    fun canGoPreviousDay(): Boolean {
        return _selectedDate.value.isAfter(getEarliestSelectableDate())
    }

    fun canGoNextDay(): Boolean {
        return _selectedDate.value.isBefore(getToday())
    }

    fun goToToday() {
        _selectedDate.value = getToday()
    }

    fun generatePdfReport(context: Context, days: Int, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            _isGeneratingReport.value = true
            try {
                val reportData = goalsRepository.getReportData(days)
                GoalsPdfGenerator.generateAndOpenPdf(context, reportData)
            } finally {
                _isGeneratingReport.value = false
                onComplete()
            }
        }
    }

    fun toggleGoal(item: DailyGoalItem): Boolean {
        // Only present day can be edited; past dates are read-only
        if (_selectedDate.value != getToday()) {
            return false
        }
        viewModelScope.launch {
            goalsRepository.toggleGoal(item.goalId, _selectedDate.value, item.isCompleted)
        }
        return true
    }

    fun addCustomGoal(title: String, titleUrdu: String = "") {
        goalsRepository.addCustomGoal(title, titleUrdu)
    }

    fun deleteCustomGoal(goalId: String) {
        if (_selectedDate.value != getToday()) return
        goalsRepository.deleteCustomGoal(goalId)
    }
}
