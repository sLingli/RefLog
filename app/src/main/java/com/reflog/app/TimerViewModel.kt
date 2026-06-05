package com.reflog.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.reflog.app.db.HalfType
import com.reflog.app.repository.MatchRecordRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * 计时器 ViewModel
 *
 * 状态机 + 定时循环（协程） + 事件记录 + 比赛保存
 */
class TimerViewModel(
    private val recordRepository: MatchRecordRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TimerUiState())
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()

    // 内部计时器变量
    private var timerState: TimerState = TimerState.READY
    private var currentHalf: HalfState = HalfState.FIRST
    private var mainTime: Long = 0
    private var stoppageTime: Long = 0
    private var firstHalfStoppage: Long = 0
    private var lastUpdateTime: Long = 0
    private var halfTimeSeconds: Long = DEFAULT_HALF_TIME * 60L
    private var halfTimeAlertShown: Boolean = false
    private var fullTimeAlertShown: Boolean = false
    private val matchEvents = mutableListOf<MatchEvent>()
    private var tickJob: Job? = null

    // ── 比赛初始化 ──────────────────────────────

    fun initMatch(template: MatchTemplate) {
        resetMatch()
        halfTimeSeconds = template.halfTimeMinutes * 60L
        updateUiState {
            copy(
                matchName = template.name,
                homeTeamName = template.homeTeamName,
                awayTeamName = template.awayTeamName,
                homeTeamColor = template.homeTeamColor,
                awayTeamColor = template.awayTeamColor,
            )
        }
    }

    // ── 状态机 ──────────────────────────────────

    fun toggleTimer() {
        when (timerState) {
            TimerState.READY -> startTimer()
            TimerState.RUNNING -> pauseTimer()
            TimerState.PAUSED -> resumeTimer()
            TimerState.HALFTIME -> startSecondHalf()
            TimerState.FINISHED -> {}
        }
    }

    fun onEndHalfButtonLongPress() {
        when (currentHalf) {
            HalfState.FIRST -> endFirstHalf()
            HalfState.SECOND -> endSecondHalf()
            HalfState.BREAK -> {}
        }
    }

    private fun startTimer() {
        timerState = TimerState.RUNNING
        lastUpdateTime = System.currentTimeMillis()
        syncUiState()
        startUpdateLoop()
    }

    private fun resumeTimer() {
        timerState = TimerState.RUNNING
        syncUiState()
        startUpdateLoop()
    }

    private fun pauseTimer() {
        timerState = TimerState.PAUSED
        syncUiState()
        updateUiState { copy(showEventSheet = true) }
    }

    private fun startSecondHalf() {
        currentHalf = HalfState.SECOND
        mainTime = halfTimeSeconds
        timerState = TimerState.RUNNING
        stoppageTime = 0
        lastUpdateTime = System.currentTimeMillis()
        fullTimeAlertShown = false
        syncUiState()
        startUpdateLoop()
    }

    private fun endFirstHalf() {
        timerState = TimerState.HALFTIME
        currentHalf = HalfState.BREAK
        firstHalfStoppage = stoppageTime
        stoppageTime = 0
        halfTimeAlertShown = false
        tickJob?.cancel()
        syncUiState()
    }

    private fun endSecondHalf() {
        timerState = TimerState.FINISHED
        tickJob?.cancel()
        syncUiState()
        viewModelScope.launch { saveMatchRecord() }
        showMatchSummary()
    }

    fun resetMatch() {
        timerState = TimerState.READY
        currentHalf = HalfState.FIRST
        mainTime = 0
        stoppageTime = 0
        firstHalfStoppage = 0
        halfTimeSeconds = DEFAULT_HALF_TIME * 60L
        halfTimeAlertShown = false
        fullTimeAlertShown = false
        matchEvents.clear()
        tickJob?.cancel()
        syncUiState()
        updateUiState {
            copy(
                matchName = "", homeTeamName = "", awayTeamName = "",
                homeTeamColor = 0xFF1565C0.toInt(), awayTeamColor = 0xFFC62828.toInt(),
                showEventSheet = false, matchSummary = null,
            )
        }
    }

    // ── 定时循环 ────────────────────────────────

    private fun startUpdateLoop() {
        tickJob?.cancel()
        tickJob = viewModelScope.launch {
            while (isActive) { tick(); delay(100) }
        }
    }

    private fun tick() {
        val now = System.currentTimeMillis()
        if (lastUpdateTime > 0 && (now - lastUpdateTime) >= 1000) {
            if (timerState == TimerState.RUNNING || timerState == TimerState.PAUSED) {
                mainTime++
                if (timerState == TimerState.PAUSED) stoppageTime++
                checkTimeAlerts()
                syncUiState()
            }
            lastUpdateTime = now
        } else if (lastUpdateTime == 0L) {
            lastUpdateTime = now
        }
    }

    private fun checkTimeAlerts() {
        when (currentHalf) {
            HalfState.FIRST -> {
                if (mainTime >= halfTimeSeconds && !halfTimeAlertShown) halfTimeAlertShown = true
            }
            HalfState.SECOND -> {
                if (mainTime >= halfTimeSeconds * 2 && !fullTimeAlertShown) fullTimeAlertShown = true
            }
            HalfState.BREAK -> {}
        }
    }

    // ── 事件记录 ────────────────────────────────

    fun handleEventConfirmed(eventType: EventType, team: TeamSelection, number: String) {
        when (eventType) {
            EventType.INJURY, EventType.SUBSTITUTION -> recordSimpleEvent(eventType)
            EventType.CANCEL -> {}
            else -> {
                if (team == TeamSelection.CANCEL) return
                recordEventWithDetails(eventType, team, number)
            }
        }
        updateUiState { copy(showEventSheet = false) }
    }

    fun onEventSheetDismissed() {
        updateUiState { copy(showEventSheet = false) }
    }

    private fun recordSimpleEvent(eventType: EventType) {
        val halfType = if (currentHalf == HalfState.FIRST) HalfType.FIRST_HALF else HalfType.SECOND_HALF
        matchEvents.add(MatchEvent(
            timeStr = formatTime(mainTime), event = eventType.name, emoji = eventType.toEmoji(),
            detail = "", half = halfType.name, minute = (mainTime / 60).toInt(),
        ))
    }

    private fun recordEventWithDetails(eventType: EventType, team: TeamSelection, number: String) {
        val teamStr = if (team == TeamSelection.HOME) "Home" else "Away"
        val halfType = if (currentHalf == HalfState.FIRST) HalfType.FIRST_HALF else HalfType.SECOND_HALF
        matchEvents.add(MatchEvent(
            timeStr = formatTime(mainTime), event = eventType.name, emoji = eventType.toEmoji(),
            detail = "$teamStr #$number", half = halfType.name, minute = (mainTime / 60).toInt(),
        ))
    }

    // ── 比赛记录保存 ────────────────────────────

    private suspend fun saveMatchRecord() {
        val s = _uiState.value
        val record = MatchRecordRepository.buildMatchRecord(
            matchEvents = matchEvents.toList(), halfTimeSeconds = halfTimeSeconds,
            firstHalfStoppage = firstHalfStoppage, stoppageTime = stoppageTime,
            currentMatchName = s.matchName, currentHomeTeamName = s.homeTeamName,
            currentAwayTeamName = s.awayTeamName, homeTeamColor = s.homeTeamColor,
            awayTeamColor = s.awayTeamColor,
        )
        recordRepository.saveRecord(record)
    }

    // ── 比赛总结弹窗 ────────────────────────────

    private fun showMatchSummary() {
        val events = matchEvents.toList()
        updateUiState {
            copy(matchSummary = MatchSummaryData(
                isHistory = false, halfTimeMinutes = (halfTimeSeconds / 60).toInt(),
                homeGoals = events.count { EventType.valueOf(it.event) == EventType.GOAL && it.detail.contains("Home", ignoreCase = true) },
                awayGoals = events.count { EventType.valueOf(it.event) == EventType.GOAL && it.detail.contains("Away", ignoreCase = true) },
                yellowCount = events.count { EventType.valueOf(it.event) == EventType.YELLOW_CARD },
                redCount = events.count { EventType.valueOf(it.event) == EventType.RED_CARD },
                firstHalfStoppage = formatTime(firstHalfStoppage),
                secondHalfStoppage = formatTime(stoppageTime), events = events,
            ))
        }
    }

    fun showHistoryMatchSummary(record: MatchRecord) {
        updateUiState {
            copy(matchSummary = MatchSummaryData(
                isHistory = true, halfTimeMinutes = record.halfTimeMinutes,
                homeGoals = record.homeGoals, awayGoals = record.awayGoals,
                yellowCount = record.yellowCount, redCount = record.redCount,
                firstHalfStoppage = record.firstHalfStoppage,
                secondHalfStoppage = record.secondHalfStoppage, events = record.events,
            ))
        }
    }

    fun dismissMatchSummary() {
        updateUiState { copy(matchSummary = null) }
    }

    // ── 状态同步 & 工具 ────────────────────────

    private fun syncUiState() {
        updateUiState {
            copy(
                timerState = this@TimerViewModel.timerState,
                currentHalf = this@TimerViewModel.currentHalf,
                mainTimeSeconds = mainTime, stoppageSeconds = stoppageTime,
                showEndHalfButton = timerState == TimerState.RUNNING || timerState == TimerState.PAUSED,
            )
        }
    }

    private fun updateUiState(transform: TimerUiState.() -> TimerUiState) {
        _uiState.value = _uiState.value.transform()
    }

    fun formatTime(seconds: Long): String {
        return String.format(Locale.getDefault(), "%02d:%02d", seconds / 60, seconds % 60)
    }

    fun isInStoppage(): Boolean = when (currentHalf) {
        HalfState.FIRST -> halfTimeAlertShown
        HalfState.SECOND -> fullTimeAlertShown
        HalfState.BREAK -> false
    }

    companion object {
        const val DEFAULT_HALF_TIME = 45
    }

    class Factory(private val recordRepository: MatchRecordRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TimerViewModel::class.java)) {
                return TimerViewModel(recordRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
