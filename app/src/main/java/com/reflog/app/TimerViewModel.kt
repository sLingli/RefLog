package com.reflog.app

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.reflog.app.db.HalfType
import com.reflog.app.repository.MatchRecordRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * 计时器 ViewModel — 官方标准架构
 *
 * - 通过 Binder 绑定 Service
 * - collect Service 的 StateFlow 驱动 UI
 * - 直接调用 Service 方法发送命令
 * - 事件记录、比赛保存仍在 ViewModel
 */
class TimerViewModel(
    private val recordRepository: MatchRecordRepository,
    private val appContext: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TimerUiState())
    val uiState: StateFlow<TimerUiState> = _uiState.asStateFlow()

    // ViewModel 本地保留的信息
    private var halfTimeSeconds: Long = DEFAULT_HALF_TIME * 60L
    private val matchEvents = mutableListOf<MatchEvent>()
    private var homeTeamName: String = ""
    private var awayTeamName: String = ""
    private var matchName: String = ""

    // ── Binder 连接（官方标准） ──────────────────

    private var service: TimerForegroundService? = null
    private var isBound = false
    private var collectJob: Job? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as TimerForegroundService.LocalBinder
            service = localBinder.getService()
            isBound = true

            // ★ 官方标准：绑定成功后 collect Service 的 StateFlow
            collectJob = viewModelScope.launch {
                service?.state?.collect { serviceState ->
                    _uiState.value = _uiState.value.copy(
                        timerState = serviceState.timerState,
                        currentHalf = serviceState.currentHalf,
                        mainTimeSeconds = serviceState.mainTimeSeconds,
                        stoppageSeconds = serviceState.stoppageSeconds,
                        showEndHalfButton = serviceState.isServiceRunning &&
                            (serviceState.timerState == TimerState.RUNNING ||
                             serviceState.timerState == TimerState.PAUSED),
                    )
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            collectJob?.cancel()
            service = null
            isBound = false
        }
    }

    // ── 比赛初始化 ──────────────────────────────

    fun initMatch(template: MatchTemplate) {
        resetMatch()
        halfTimeSeconds = template.halfTimeMinutes * 60L
        homeTeamName = template.homeTeamName
        awayTeamName = template.awayTeamName
        matchName = template.name
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

    // ── 状态机（通过 Binder 调用 Service 方法） ──

    fun toggleTimer() {
        val svc = service
        if (svc == null) {
            // Service 还没绑定，首次启动
            startTimer()
            return
        }
        val s = svc.state.value
        when (s.timerState) {
            TimerState.READY -> startTimer()
            TimerState.RUNNING -> svc.pause().also { updateUiState { copy(showEventSheet = true) } }
            TimerState.PAUSED -> svc.resume()
            TimerState.HALFTIME -> svc.startSecondHalf()
            TimerState.FINISHED -> {}
            else -> startTimer()
        }
    }

    fun onEndHalfButtonLongPress() {
        val svc = service ?: return
        when (svc.state.value.currentHalf) {
            HalfState.FIRST -> svc.endFirstHalf()
            HalfState.SECOND -> {
                svc.endSecondHalf()
                firstHalfStoppage = svc.getFirstHalfStoppage()
                viewModelScope.launch {
                    kotlinx.coroutines.delay(200)
                    saveMatchRecord()
                    showMatchSummary()
                }
            }
            HalfState.BREAK -> {}
        }
    }

    private var firstHalfStoppage: Long = 0

    private fun startTimer() {
        // 启动前台 Service
        TimerForegroundService.start(
            context = appContext,
            halfTimeSeconds = halfTimeSeconds,
            homeTeamName = homeTeamName,
            awayTeamName = awayTeamName,
            matchName = matchName,
        )
        // 绑定 Service
        val intent = Intent(appContext, TimerForegroundService::class.java)
        appContext.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    fun resetMatch() {
        matchEvents.clear()
        halfTimeSeconds = DEFAULT_HALF_TIME * 60L
        homeTeamName = ""
        awayTeamName = ""
        matchName = ""
        firstHalfStoppage = 0

        // 解绑 Service
        if (isBound) {
            collectJob?.cancel()
            appContext.unbindService(serviceConnection)
            isBound = false
            service = null
        }

        // 停止 Service
        appContext.stopService(Intent(appContext, TimerForegroundService::class.java))

        updateUiState {
            copy(
                timerState = TimerState.READY,
                currentHalf = HalfState.FIRST,
                mainTimeSeconds = 0,
                stoppageSeconds = 0,
                matchName = "", homeTeamName = "", awayTeamName = "",
                homeTeamColor = 0xFF1565C0.toInt(), awayTeamColor = 0xFFC62828.toInt(),
                showEventSheet = false, showEndHalfButton = false, matchSummary = null,
            )
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
        // 恢复计时
        val svc = service
        if (svc != null && svc.state.value.timerState == TimerState.PAUSED) {
            svc.resume()
        }
    }

    private fun recordSimpleEvent(eventType: EventType) {
        val svc = service ?: return
        val s = svc.state.value
        val halfType = if (s.currentHalf == HalfState.FIRST) HalfType.FIRST_HALF else HalfType.SECOND_HALF
        matchEvents.add(MatchEvent(
            timeStr = formatTime(s.mainTimeSeconds), event = eventType.name, emoji = eventType.toEmoji(),
            detail = "", half = halfType.name, minute = (s.mainTimeSeconds / 60).toInt(),
        ))
    }

    private fun recordEventWithDetails(eventType: EventType, team: TeamSelection, number: String) {
        val svc = service ?: return
        val s = svc.state.value
        val ui = _uiState.value
        val teamName = if (team == TeamSelection.HOME) ui.homeTeamName.ifEmpty { "Home" } else ui.awayTeamName.ifEmpty { "Away" }
        val halfType = if (s.currentHalf == HalfState.FIRST) HalfType.FIRST_HALF else HalfType.SECOND_HALF
        matchEvents.add(MatchEvent(
            timeStr = formatTime(s.mainTimeSeconds), event = eventType.name, emoji = eventType.toEmoji(),
            detail = "$teamName #$number", half = halfType.name, minute = (s.mainTimeSeconds / 60).toInt(),
            team = team,
        ))
        // 进球时通知 Service 更新比分
        if (eventType == EventType.GOAL) {
            svc.recordGoal(team == TeamSelection.HOME)
        }
    }

    // ── 比赛记录保存 ────────────────────────────

    private suspend fun saveMatchRecord() {
        val svc = service ?: return
        val s = svc.state.value
        val ui = _uiState.value
        val record = MatchRecordRepository.buildMatchRecord(
            matchEvents = matchEvents.toList(), halfTimeSeconds = s.halfTimeSeconds,
            firstHalfStoppage = svc.getFirstHalfStoppage(), stoppageTime = s.stoppageSeconds,
            currentMatchName = ui.matchName, currentHomeTeamName = ui.homeTeamName,
            currentAwayTeamName = ui.awayTeamName, homeTeamColor = ui.homeTeamColor,
            awayTeamColor = ui.awayTeamColor,
        )
        recordRepository.saveRecord(record)
    }

    // ── 比赛总结弹窗 ────────────────────────────

    private fun showMatchSummary() {
        val svc = service ?: return
        val s = svc.state.value
        val events = matchEvents.toList()
        updateUiState {
            copy(matchSummary = MatchSummaryData(
                isHistory = false, halfTimeMinutes = (s.halfTimeSeconds / 60).toInt(),
                homeGoals = events.count { EventType.valueOf(it.event) == EventType.GOAL && it.team == TeamSelection.HOME },
                awayGoals = events.count { EventType.valueOf(it.event) == EventType.GOAL && it.team == TeamSelection.AWAY },
                yellowCount = events.count { EventType.valueOf(it.event) == EventType.YELLOW_CARD },
                redCount = events.count { EventType.valueOf(it.event) == EventType.RED_CARD },
                firstHalfStoppage = formatTime(svc.getFirstHalfStoppage()),
                secondHalfStoppage = formatTime(s.stoppageSeconds), events = events,
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

    // ── 工具 ────────────────────────────────────

    private fun updateUiState(transform: TimerUiState.() -> TimerUiState) {
        _uiState.value = _uiState.value.transform()
    }

    fun formatTime(seconds: Long): String {
        return String.format(Locale.getDefault(), "%02d:%02d", seconds / 60, seconds % 60)
    }

    fun isInStoppage(): Boolean {
        val svc = service ?: return false
        val s = svc.state.value
        return when (s.currentHalf) {
            HalfState.FIRST -> s.mainTimeSeconds >= s.halfTimeSeconds
            HalfState.SECOND -> s.mainTimeSeconds >= s.halfTimeSeconds * 2
            HalfState.BREAK -> false
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (isBound) {
            collectJob?.cancel()
            appContext.unbindService(serviceConnection)
            isBound = false
        }
    }

    companion object {
        const val DEFAULT_HALF_TIME = 45
    }

    class Factory(
        private val recordRepository: MatchRecordRepository,
        private val appContext: Context,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TimerViewModel::class.java)) {
                return TimerViewModel(recordRepository, appContext) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
