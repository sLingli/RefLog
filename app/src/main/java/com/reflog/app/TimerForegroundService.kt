package com.reflog.app

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 比赛计时前台 Service — 官方标准架构
 *
 * - Binder + StateFlow：ViewModel 通过 collect 观察状态
 * - SystemClock.elapsedRealtime()：时间戳差值法，防漂移
 * - delay(1000) + 差值计算：即使 delay 被调度延迟，时间依然精确
 * - 统一 5 秒刷新通知：不区分前后台，系统自动管理渲染优先级
 */

/** Service 暴露给 ViewModel 的只读状态 */
data class TimerServiceState(
    val timerState: TimerState = TimerState.READY,
    val currentHalf: HalfState = HalfState.FIRST,
    val mainTimeSeconds: Long = 0,
    val stoppageSeconds: Long = 0,
    val firstHalfStoppage: Long = 0,
    val halfTimeSeconds: Long = 2700,
    val isServiceRunning: Boolean = false,
)

class TimerForegroundService : Service() {

    companion object {
        private const val EXTRA_HALF_TIME_SECONDS = "half_time_seconds"
        private const val EXTRA_HOME_TEAM = "home_team"
        private const val EXTRA_AWAY_TEAM = "away_team"
        private const val EXTRA_MATCH_NAME = "match_name"

        private const val NOTIFY_INTERVAL = 5

        fun start(
            context: Context,
            halfTimeSeconds: Long,
            homeTeamName: String,
            awayTeamName: String,
            matchName: String,
        ) {
            LiveUpdateNotificationHelper.createChannel(context)
            val intent = Intent(context, TimerForegroundService::class.java).apply {
                putExtra(EXTRA_HALF_TIME_SECONDS, halfTimeSeconds)
                putExtra(EXTRA_HOME_TEAM, homeTeamName)
                putExtra(EXTRA_AWAY_TEAM, awayTeamName)
                putExtra(EXTRA_MATCH_NAME, matchName)
            }
            context.startForegroundService(intent)
        }
    }

    // ── Binder（官方标准） ────────────────────────

    inner class LocalBinder : Binder() {
        fun getService(): TimerForegroundService = this@TimerForegroundService
    }

    private val binder = LocalBinder()

    override fun onBind(intent: Intent?): IBinder = binder

    // ── StateFlow（官方标准） ─────────────────────

    private val _state = MutableStateFlow(TimerServiceState())
    val state: StateFlow<TimerServiceState> = _state.asStateFlow()

    // ── 内部计时状态 ──────────────────────────────

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var timerJob: Job? = null

    // 时间锚点：SystemClock.elapsedRealtime()（官方推荐，防漂移）
    private var matchStartTime = 0L       // 比赛开始的绝对时间戳
    private var pauseAccumulatedMs = 0L   // 暂停期间累积的毫秒数
    private var pauseStartTime = 0L       // 本次暂停开始的时间戳

    // 通知 / 匹配信息
    private var halfTimeSeconds: Long = 2700
    private var homeTeamName: String = ""
    private var awayTeamName: String = ""
    private var matchName: String = ""
    private var currentHalf = HalfState.FIRST
    private var firstHalfStoppage: Long = 0
    private var halfTimeAlertShown = false
    private var fullTimeAlertShown = false
    private var homeGoals = 0
    private var awayGoals = 0
    // ★ chronometer 基准时间：只在开球/下半场开始时设置一次，永不改变！
    private var chronometerBaseMillis: Long = 0L

    // ── 生命周期 ──────────────────────────────────

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 解析 Intent 参数
        halfTimeSeconds = intent?.getLongExtra(EXTRA_HALF_TIME_SECONDS, 2700) ?: 2700
        homeTeamName = intent?.getStringExtra(EXTRA_HOME_TEAM).orEmpty()
        awayTeamName = intent?.getStringExtra(EXTRA_AWAY_TEAM).orEmpty()
        matchName = intent?.getStringExtra(EXTRA_MATCH_NAME).orEmpty()

        // ★ 官方铁律：立刻 startForeground
        val notification = buildNotification()
        startForeground(LiveUpdateNotificationHelper.NOTIFICATION_ID, notification)
        notifyOnce()

        // 开始计时
        startMatch()

        return START_NOT_STICKY
    }

    // ── 公开命令（ViewModel 通过 Binder 直接调用） ─

    fun startMatch() {
        matchStartTime = SystemClock.elapsedRealtime()
        chronometerBaseMillis = System.currentTimeMillis()  // ★ 固定！永不改变
        pauseAccumulatedMs = 0
        currentHalf = HalfState.FIRST
        firstHalfStoppage = 0
        halfTimeAlertShown = false
        fullTimeAlertShown = false
        homeGoals = 0
        awayGoals = 0

        updateState(TimerState.RUNNING)
        startTimerLoop()
    }

    fun pause() {
        pauseStartTime = SystemClock.elapsedRealtime()
        updateState(TimerState.PAUSED)
        notifyOnce()
    }

    fun resume() {
        // 暂停结束，累积本次暂停时长
        pauseAccumulatedMs += SystemClock.elapsedRealtime() - pauseStartTime
        updateState(TimerState.RUNNING)
        notifyOnce()
    }

    fun endFirstHalf() {
        // 记录上半场补时
        firstHalfStoppage = getStoppageSeconds()
        currentHalf = HalfState.BREAK
        timerJob?.cancel()
        updateState(TimerState.HALFTIME)
        notifyOnce()
    }

    fun startSecondHalf() {
        currentHalf = HalfState.SECOND
        halfTimeAlertShown = false
        fullTimeAlertShown = false
        // 重新锚定
        matchStartTime = SystemClock.elapsedRealtime() - halfTimeSeconds * 1000
        chronometerBaseMillis = System.currentTimeMillis() - halfTimeSeconds * 1000  // ★ 固定！
        pauseAccumulatedMs = 0
        updateState(TimerState.RUNNING)
        notifyOnce()
        startTimerLoop()
    }

    fun endSecondHalf() {
        timerJob?.cancel()
        updateState(TimerState.FINISHED)
        notifyOnce()
    }

    fun getFirstHalfStoppage(): Long = firstHalfStoppage

    fun recordGoal(isHome: Boolean) {
        if (isHome) homeGoals++ else awayGoals++
        notifyOnce()
    }

    // ── 计时循环（更新 StateFlow + 每 5 秒 notify 进度条） ──

    private fun startTimerLoop() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            var lastNotifyTime = 0L
            while (isActive) {
                delay(1000)

                val s = _state.value
                if (s.timerState != TimerState.RUNNING && s.timerState != TimerState.PAUSED) continue

                // 主计时器永不停
                val mainTime = getMainTimeSeconds()
                // 补时只在暂停时计数
                val stoppage = if (s.timerState == TimerState.PAUSED) getStoppageSeconds() else s.stoppageSeconds

                checkTimeAlerts(mainTime)

                _state.value = s.copy(
                    mainTimeSeconds = mainTime,
                    stoppageSeconds = stoppage,
                )

                // ★ 每 5 秒 notify 一次更新进度条位置
                // chronometer 不受影响（setWhen 固定不变）
                if (mainTime - lastNotifyTime >= NOTIFY_INTERVAL) {
                    lastNotifyTime = mainTime
                    notifyOnce()
                }
            }
        }
    }

    /**
     * 主计时器：从比赛开始到现在的总秒数，永不停止。
     * 即使暂停期间，主计时器依然在走。
     */
    private fun getMainTimeSeconds(): Long {
        return ((SystemClock.elapsedRealtime() - matchStartTime) / 1000).coerceAtLeast(0)
    }

    /**
     * 补时计时器：只在暂停期间计数。
     * = 已累积的暂停时间 + 当前这次暂停已过时间
     */
    private fun getStoppageSeconds(): Long {
        val currentPauseMs = if (_state.value.timerState == TimerState.PAUSED) {
            SystemClock.elapsedRealtime() - pauseStartTime
        } else {
            0
        }
        return ((pauseAccumulatedMs + currentPauseMs) / 1000).coerceAtLeast(0)
    }

    private fun checkTimeAlerts(elapsed: Long) {
        when (currentHalf) {
            HalfState.FIRST -> {
                if (elapsed >= halfTimeSeconds && !halfTimeAlertShown) halfTimeAlertShown = true
            }
            HalfState.SECOND -> {
                if (elapsed >= halfTimeSeconds * 2 && !fullTimeAlertShown) fullTimeAlertShown = true
            }
            HalfState.BREAK -> {}
        }
    }

    private fun updateState(timerState: TimerState) {
        _state.value = _state.value.copy(
            timerState = timerState,
            currentHalf = currentHalf,
            mainTimeSeconds = getMainTimeSeconds(),
            stoppageSeconds = getStoppageSeconds(),
            halfTimeSeconds = halfTimeSeconds,
            firstHalfStoppage = firstHalfStoppage,
            isServiceRunning = timerState != TimerState.READY && timerState != TimerState.FINISHED,
        )
    }

    // ── 通知 ──────────────────────────────────────

    private fun notifyOnce() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(LiveUpdateNotificationHelper.NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): android.app.Notification {
        val s = _state.value

        return LiveUpdateNotificationHelper.buildNotification(
            context = this,
            timerState = s.timerState,
            currentHalf = s.currentHalf,
            mainTimeSeconds = s.mainTimeSeconds,
            stoppageSeconds = s.stoppageSeconds,
            homeTeamName = homeTeamName,
            awayTeamName = awayTeamName,
            matchName = matchName,
            baseTimeMillis = chronometerBaseMillis,  // ★ 固定值，永不改变
            halfTimeSeconds = halfTimeSeconds,
            homeGoals = homeGoals,
            awayGoals = awayGoals,
        )
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
