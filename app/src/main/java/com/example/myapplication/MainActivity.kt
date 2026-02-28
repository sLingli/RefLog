package com.example.myapplication

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.transition.TransitionManager
import android.transition.AutoTransition
import com.google.android.material.bottomsheet.BottomSheetDialog
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.compose.ui.platform.ComposeView
import androidx.wear.compose.material.MaterialTheme
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.compose.ui.platform.ViewCompositionStrategy
import android.widget.FrameLayout

class MainActivity : AppCompatActivity() {

    private val timeSettingLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val selectedMinutes = result.data?.getIntExtra("SELECTED_TIME", 45) ?: 45

            halfTimeSeconds = selectedMinutes * 60L
            matchTimeSet = true

            val btnSetMatchTime = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSetMatchTime)
            btnSetMatchTime?.text = getString(R.string.fmt_duration_simple, selectedMinutes)

            addLog("⚙️ 比赛时间调整为: $selectedMinutes 分钟")
        }
    }
    //  状态常量
    private companion object {
        const val STATE_READY = "ready"
        const val STATE_RUNNING = "running"
        const val STATE_PAUSED = "paused"
        const val STATE_HALFTIME = "halftime"
        const val STATE_FINISHED = "finished"

        const val HALF_FIRST = "code_first_half"
        const val HALF_BREAK = "code_halftime"
        const val HALF_SECOND = "code_second_half"
        const val DEFAULT_HALF_TIME = 45
    }

    // UI 组件
    private lateinit var statusLabel: TextView

    private lateinit var mainTimeLabel: TextView

    private lateinit var stoppageTimeLabel: TextView
    private lateinit var mainButton: com.google.android.material.button.MaterialButton
    private lateinit var endHalfButton: com.google.android.material.button.MaterialButton

    private var state: String = STATE_READY
    private var currentHalf: String = HALF_FIRST
    private lateinit var btnHistory: Button
    private lateinit var recordManager: MatchRecordManager
    private var hideRunnable: Runnable? = null
    private val hideHandler = android.os.Handler(android.os.Looper.getMainLooper())


    // 计时器变量
    private var mainTime: Long = 0
    private var stoppageTime: Long = 0
    private var firstHalfStoppage: Long = 0
    private var lastUpdateTime: Long = 0

    // 自定义比赛时间（秒）
    private var halfTimeSeconds: Long = DEFAULT_HALF_TIME * 60L
    private var matchTimeSet: Boolean = false

    // 提醒标志
    private var halfTimeAlertShown: Boolean = false
    private var fullTimeAlertShown: Boolean = false

    // 事件记录
    private val matchEvents = mutableListOf<MatchEvent>()

    // 定时器Handler
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var updateRunnable: Runnable

    // 事件选择相关
    private var pendingEventType: String = ""
    private var selectedTeam: String = ""

    // 默认主队蓝色，客队红色
    private var homeTeamColor: Int = 0xFF1565C0.toInt()
    private var awayTeamColor: Int = 0xFFC62828.toInt()

    private val rippleHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var rippleRunnable: Runnable? = null

    // 底部历史条拖拽用的临时变量
    private var historyStartY: Float = 0f
    private var historyTriggered: Boolean = false
    private var historySheet: BottomSheetDialog? = null


    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initializeUI()
        resetMatch()
        initializeTimer()
    }
    private fun initializeTimer() {
        updateRunnable = object : Runnable {
            override fun run() {
                updateTimer()
                handler.postDelayed(this, 100)
            }
        }

        handler.post(updateRunnable)

        Log.i("FootballTimer", "⏱️ 计时器已初始化")
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initializeUI() {
        // 1. 绑定基础视图
        statusLabel = findViewById(R.id.statusLabel)
        mainTimeLabel = findViewById(R.id.mainTimeLabel)
        stoppageTimeLabel = findViewById(R.id.stoppageTimeLabel)
        mainButton = findViewById(R.id.mainButton)
        endHalfButton = findViewById(R.id.endHalfButton)
        btnHistory = findViewById(R.id.btnHistory)
        val btnBigStart = findViewById<android.view.View>(R.id.btnBigStart)
        val btnHistorySmall = findViewById<android.view.View>(R.id.btnHistorySmall)
        val btnSetMatchTime = findViewById<android.view.View>(R.id.btnSetMatchTime)

        // 2. 初始化时间设置按钮文字
        val initialMinutes = (halfTimeSeconds / 60).toInt()
        val btn = btnSetMatchTime as? com.google.android.material.button.MaterialButton
        btn?.text = getString(R.string.fmt_duration_simple, initialMinutes)

        // 3. 颜色选择逻辑
        val clickHomeColor = findViewById<View>(R.id.clickHomeColor)
        val clickAwayColor = findViewById<View>(R.id.clickAwayColor)

        clickHomeColor?.setOnClickListener {
            showColorSelectionDialog(isHome = true)
        }
        clickAwayColor?.setOnClickListener {
            showColorSelectionDialog(isHome = false)
        }

        // 4. 比赛控制面板逻辑
        val touchOverlay = findViewById<android.view.View>(R.id.touchOverlay)
        val controlPanel = findViewById<android.view.View>(R.id.controlPanel)
        val timerContainer = findViewById<android.view.View>(R.id.timerContainer)
        val btnPauseRound = findViewById<android.view.View>(R.id.btnPauseRound)
        val btnEndRound = findViewById<android.view.View>(R.id.btnEndRound)

        mainButton.setOnClickListener { toggleTimer() }
        btnBigStart?.setOnClickListener { toggleTimer() }
        btnPauseRound?.setOnClickListener { toggleTimer() }

        val openHistoryAction: () -> Unit = {
            // 统一走底部抽屉弹窗，打开设置界面
            if (supportFragmentManager.findFragmentByTag(SettingsBottomSheetFragment.TAG) == null) {
                showSettingsDialog()
            }
        }

        val historyDragTarget: View? = btnHistorySmall ?: btnHistory
        var startYOnBar = 0f
        var triggeredOnBar = false

        historyDragTarget?.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    startYOnBar = event.y
                    triggeredOnBar = false
                    true
                }
                android.view.MotionEvent.ACTION_MOVE -> {
                    val dy = startYOnBar - event.y   // 向上为正
                    if (!triggeredOnBar && dy > 0f) {
                        triggeredOnBar = true
                        openHistoryAction()
                    }
                    true
                }
                android.view.MotionEvent.ACTION_UP,
                android.view.MotionEvent.ACTION_CANCEL -> {
                    triggeredOnBar = false
                    true
                }
                else -> false
            }
        }

        // 5. 时间选择跳转 (Compose)
        btnSetMatchTime.setOnClickListener {
            val intent = android.content.Intent(this, TimeSelectionActivity::class.java)
            timeSettingLauncher.launch(intent)
        }

        hideRunnable = Runnable {
            controlPanel?.animate()?.translationY(250f)?.setDuration(300)?.start()
            timerContainer?.animate()?.scaleX(1.0f)?.scaleY(1.0f)?.translationY(0f)?.setDuration(300)?.start()
        }

        touchOverlay?.setOnClickListener {
            controlPanel?.animate()?.translationY(0f)?.setDuration(300)?.start()
            timerContainer?.animate()?.scaleX(0.85f)?.scaleY(0.85f)?.translationY(-60f)?.setDuration(300)?.start()

            hideHandler.removeCallbacks(hideRunnable!!)
            hideHandler.postDelayed(hideRunnable!!, 3000)
        }

        btnEndRound?.let { setupLongPressEnd(it) }
        endHalfButton?.let { setupLongPressEnd(it) }

        recordManager = MatchRecordManager(this)

        val rippleRing = findViewById<View>(R.id.rippleRing)
        if (rippleRing != null) {
            rippleRunnable = object : Runnable {
                override fun run() {
                    rippleRing.scaleX = 1f
                    rippleRing.scaleY = 1f
                    rippleRing.alpha = 1f
                    rippleRing.visibility = View.VISIBLE

                    rippleRing.animate()
                        .scaleX(1.5f)
                        .scaleY(1.5f)
                        .alpha(0f)
                        .setDuration(1000)
                        .withEndAction {
                            rippleRing.visibility = View.GONE
                        }
                        .start()

                    rippleHandler.postDelayed(this, 3000)
                }
            }
            rippleHandler.post(rippleRunnable!!)
        }
    }

    private fun setupLongPressEnd(button: android.view.View) {
        var triggerAction: Runnable? = null
        val holdAnimator = android.animation.ValueAnimator.ofInt(0, 10000).apply {
            duration = 1500
            addUpdateListener { animation ->
                button.background?.level = animation.animatedValue as Int
            }
        }

        button.setOnTouchListener { v, event ->
            if (state == STATE_READY) return@setOnTouchListener false
            val vibrator = getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    // 暂停自动隐藏倒计时
                    hideRunnable?.let { hideHandler.removeCallbacks(it) }

                    triggerAction?.let { v.removeCallbacks(it) }
                    holdAnimator.start()
                    if (android.os.Build.VERSION.SDK_INT >= 29) {
                        vibrator.vibrate(android.os.VibrationEffect.createPredefined(android.os.VibrationEffect.EFFECT_TICK))
                    }
                    triggerAction = Runnable {
                        if (holdAnimator.isRunning) {
                            holdAnimator.end()
                            v.background?.level = 0
                            if (android.os.Build.VERSION.SDK_INT >= 29) {
                                vibrator.vibrate(android.os.VibrationEffect.createPredefined(android.os.VibrationEffect.EFFECT_HEAVY_CLICK))
                            } else { vibrator.vibrate(100) }
                            when (currentHalf) {
                                HALF_FIRST -> { endFirstHalf(); updateStatusLabel() }
                                HALF_SECOND -> { endSecondHalf(); updateStatusLabel() }
                            }
                        }
                    }
                    v.postDelayed(triggerAction, 1500)
                    true
                }
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                    triggerAction?.let { v.removeCallbacks(it) }
                    if (holdAnimator.isRunning) {
                        // 长按未完成，恢复自动隐藏倒计时
                        hideRunnable?.let { hideHandler.postDelayed(it, 3000) }

                        val currentLevel = button.background?.level ?: 0
                        android.animation.ValueAnimator.ofInt(currentLevel, 0).apply {
                            duration = 200
                            addUpdateListener { anim -> button.background?.level = anim.animatedValue as Int }
                        }.start()
                        holdAnimator.cancel()
                    }
                    true
                }
                else -> false
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 停止计时器
        handler.removeCallbacks(updateRunnable)
    }

    private fun toggleTimer() {
        Log.d("状态机", "toggleTimer - 当前状态: $state, 当前半场: $currentHalf")
        when (state) {
            STATE_READY -> {
                Log.d("状态机", "从READY开始 -> 触发分裂动画")
                startTimer()
            }
            STATE_RUNNING -> {
                Log.d("状态机", "从RUNNING暂停 -> 保持双按钮")
                pauseTimer()
                updateButtonStyle("resume")
            }
            STATE_PAUSED -> {
                Log.d("状态机", "从PAUSED继续 -> 保持双按钮")
                resumeTimer()
                updateButtonStyle("pause")
            }
            STATE_HALFTIME -> {
                Log.d("状态机", "从中场休息开始下半场 -> 触发分裂动画")
                startSecondHalf()
            }
            STATE_FINISHED -> {
                Log.d("状态机", "比赛结束，重新开始")
                resetMatch()
            }
        }
    }

    private fun startTimer() {
        rippleRunnable?.let { rippleHandler.removeCallbacks(it) }
        findViewById<View>(R.id.rippleRing)?.visibility = View.GONE

        // 1. 获取布局引用
        val layoutReady = findViewById<android.view.View>(R.id.layoutReady)
        val layoutRunning = findViewById<android.view.View>(R.id.layoutRunning)

        // 2. 动画转场 (手表专用)
        if (layoutReady != null && layoutRunning != null) {
            layoutReady.animate().alpha(0f).setDuration(400).withEndAction {
                layoutReady.visibility = android.view.View.GONE
                layoutRunning.visibility = android.view.View.VISIBLE
                layoutRunning.alpha = 0f
                layoutRunning.animate().alpha(1f).setDuration(400).start()
            }.start()
        }

        // 3. 启动计时核心逻辑
        state = STATE_RUNNING
        lastUpdateTime = System.currentTimeMillis()
        updateStatusLabel()
        updateButtonStyle("pause")
        updateStoppageDisplay(active = false)

        // 4. 隐藏主页的交互组件
        btnHistory.visibility = android.view.View.GONE
        findViewById<android.view.View>(R.id.btnHistorySmall)?.visibility = android.view.View.GONE

        addLog("🏁 比赛开始")
    }

    private fun resumeTimer() {
        state = STATE_RUNNING

        updateButtonStyle("pause")
        updateStoppageDisplay(active = false)

        // 继续后立刻隐藏控制面板
        val controlPanel = findViewById<android.view.View>(R.id.controlPanel)
        val timerContainer = findViewById<android.view.View>(R.id.timerContainer)
        hideHandler.removeCallbacks(hideRunnable!!)
        controlPanel?.animate()?.translationY(250f)?.setDuration(300)?.start()
        timerContainer?.animate()?.scaleX(1.0f)?.scaleY(1.0f)?.translationY(0f)?.setDuration(300)?.start()
    }

    private fun startSecondHalf() {
        currentHalf = HALF_SECOND
        mainTime = halfTimeSeconds

        state = STATE_RUNNING
        stoppageTime = 0
        lastUpdateTime = System.currentTimeMillis()
        fullTimeAlertShown = false

        updateStatusLabel()

        mainTimeLabel.text = formatTime(mainTime)
        mainTimeLabel.setTextColor(0xFF00FF00.toInt())

        updateButtonStyle("pause")
        updateStoppageDisplay(active = false)
        updateStoppageTimeDisplay()

        startUpdateLoop()

        // 开始下半场后立刻隐藏控制面板
        val controlPanel = findViewById<android.view.View>(R.id.controlPanel)
        val timerContainer = findViewById<android.view.View>(R.id.timerContainer)
        hideHandler.removeCallbacks(hideRunnable!!)
        controlPanel?.animate()?.translationY(250f)?.setDuration(300)?.start()
        timerContainer?.animate()?.scaleX(1.0f)?.scaleY(1.0f)?.translationY(0f)?.setDuration(300)?.start()

        addLog("🏁 下半场开始 - 从 ${formatTime(mainTime)} 继续计时")
        Log.i("FootballTimer", "📢 下半场开始！从 ${formatTime(mainTime)} 计时")
    }

    private fun endFirstHalf() {
        state = STATE_HALFTIME
        currentHalf = HALF_BREAK
        firstHalfStoppage = stoppageTime

        statusLabel.text = getString(R.string.status_halftime)


        mainTimeLabel.text = formatTime(mainTime)
        mainTimeLabel.setTextColor(0xFF888888.toInt())


        updateButtonStyle("halftime")

        val stoppageStr = formatTime(stoppageTime)
        addLog("📊 上半场结束 | 比赛: ${formatTime(mainTime)} | 补时: $stoppageStr")

        Log.i("FootballTimer",
            "📊 上半场总结：" +
                    "比赛时间: ${formatTime(mainTime)}, " +
                    "补时: $stoppageStr"
        )


        // 重置补时计时器准备下半场
        stoppageTime = 0
        halfTimeAlertShown = false
        updateStoppageTimeDisplay()


    }

    private fun updateMainTimeDisplay() {
        // 根据当前半场，显示正确的时间
        val displayTime = when (currentHalf) {
            HALF_FIRST -> mainTime
            HALF_SECOND -> mainTime
            HALF_BREAK -> mainTime + firstHalfStoppage
            else -> mainTime
        }

        // 更新显示
        mainTimeLabel.text = formatTime(displayTime)

        // 调试日志
        if (currentHalf == HALF_SECOND) {
            Log.d("时间显示",
                "下半场显示：" +
                        "mainTime: ${formatTime(mainTime)}, " +
                        "原半场时间: ${formatTime(halfTimeSeconds)}"
            )
        }
    }

    private fun endSecondHalf() {
        state = STATE_FINISHED


        updateStatusLabel()

        mainTimeLabel.setTextColor(0xFF888888.toInt())


        updateButtonStyle("finished")

        // 显示比赛时间
        mainTimeLabel.text = formatTime(mainTime)

        val stoppageStr = formatTime(stoppageTime)
        val firstHalfStr = formatTime(firstHalfStoppage)
        val totalStoppage = stoppageTime + firstHalfStoppage
        val totalStr = formatTime(totalStoppage)

        addLog("🏆 比赛结束")
        addLog("📊 上半场补时: $firstHalfStr")
        addLog("📊 下半场补时: $stoppageStr")
        addLog("📊 总补时: $totalStr")

        saveMatchRecord()

        // 自动弹出总结页
        showMatchSummary()

        Log.i("FootballTimer", "📢 比赛结束！总补时: $totalStr")
        animateHistoryButton(true)
    }

    private fun resetMatch() {
        state = STATE_READY
        currentHalf = HALF_FIRST
        mainTime = 0
        stoppageTime = 0
        firstHalfStoppage = 0
        halfTimeSeconds = DEFAULT_HALF_TIME * 60L
        matchTimeSet = false
        halfTimeAlertShown = false
        fullTimeAlertShown = false
        matchEvents.clear()

        updateStatusLabel()

        mainTimeLabel.text = "00:00"
        mainTimeLabel.setTextColor(getColor(R.color.timer_normal))
        stoppageTimeLabel.text = "00:00"

        val layoutReady = findViewById<View>(R.id.layoutReady)
        val layoutRunning = findViewById<View>(R.id.layoutRunning)
        val btnHistorySmall = findViewById<View>(R.id.btnHistorySmall)

        if (layoutReady != null && layoutRunning != null) {
            layoutRunning.visibility = View.GONE
            layoutReady.visibility = View.VISIBLE
            layoutReady.alpha = 1f
            btnHistorySmall?.visibility = View.VISIBLE
        }

        updateButtonStyle("start")
        updateStoppageDisplay(active = false)

        // 手机版结束按钮隐藏
        findViewById<View>(R.id.endHalfButton).visibility = View.GONE

        Log.i("FootballTimer", "")
        animateHistoryButton(true)
        val rippleRing = findViewById<View>(R.id.rippleRing)
        if (rippleRing != null && rippleRunnable != null) {
            rippleHandler.removeCallbacks(rippleRunnable!!)
            rippleHandler.post(rippleRunnable!!)
        }
    }

    // 计时器核心逻辑
    private fun startUpdateLoop() {
        handler.removeCallbacks(updateRunnable)

        updateRunnable = object : Runnable {
            override fun run() {
                updateTimer()
                handler.postDelayed(this, 100)
            }
        }
        handler.post(updateRunnable)
    }

    private fun updateTimer() {
        val currentTime = System.currentTimeMillis()

        // 检查是否有足够的时间差（至少1秒）
        if (lastUpdateTime > 0 && (currentTime - lastUpdateTime) >= 1000) {


            if (state == STATE_RUNNING || state == STATE_PAUSED) {

                mainTime++

                if (state == STATE_PAUSED) {
                    stoppageTime++
                }

                runOnUiThread {

                    mainTimeLabel.text = formatTime(mainTime)

                    updateStoppageTimeDisplay()

                    updateEndHalfButton()
                }

                checkTimeAlerts()

                // 调试日志
                Log.d("计时器", "状态: $state, 主时间: ${formatTime(mainTime)}, 补时: ${formatTime(stoppageTime)}")
            }

            // 更新时间基准
            lastUpdateTime = currentTime

        } else if (lastUpdateTime == 0L) {
            lastUpdateTime = currentTime
        }
    }

    private fun checkTimeAlerts() {
        val halfTimeMin = halfTimeSeconds / 60

        when (currentHalf) {
            HALF_FIRST -> {
                // 上半场结束提醒：检查是否达到设定的半场时间
                if (mainTime >= halfTimeSeconds && !halfTimeAlertShown) {
                    halfTimeAlertShown = true
                    triggerAlert("${halfTimeMin}分钟", "准备中场休息")
                    mainTimeLabel.setTextColor(getColor(R.color.timer_warning))
                    statusLabel.text = getString(R.string.status_first_half_stoppage)
                }
            }
            HALF_SECOND -> {


                val targetTime = halfTimeSeconds * 2
                if (mainTime >= targetTime && !fullTimeAlertShown) {
                    fullTimeAlertShown = true
                    triggerAlert("${halfTimeMin * 2}分钟", "准备结束比赛")
                    mainTimeLabel.setTextColor(getColor(R.color.timer_danger))
                    statusLabel.text = getString(R.string.status_second_half_stoppage)

                    Log.d("时间提醒",
                        "下半场提醒触发：" +
                                "当前mainTime: ${formatTime(mainTime)}, " +
                                "目标: ${formatTime(targetTime)}"
                    )
                }
            }
        }
    }

    private fun triggerAlert(timeStr: String, message: String) {
        Log.i("FootballTimer", "\n🔔🔔🔔🔔🔔🔔🔔🔔🔔🔔")
        Log.i("FootballTimer", "⏰ ${timeStr}到！$message")
        Log.i("FootballTimer", "🔔🔔🔔🔔🔔🔔🔔🔔🔔🔔\n")

        addLog("⏰ ${timeStr}到 - $message")


    }

    // 事件弹窗

    private fun pauseTimer() {
        state = STATE_PAUSED

        updateButtonStyle("start")
        updateStoppageDisplay(active = true)

        // 显示事件选择弹窗
        showEventDialog()
    }

    private fun showEventDialog() {
        // Compose-based Dialog
        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)

        val composeView = ComposeView(this).apply {
            // 设置 LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner 解决 Crash 问题
            setViewTreeLifecycleOwner(this@MainActivity)
            setViewTreeViewModelStoreOwner(this@MainActivity)
            setViewTreeSavedStateRegistryOwner(this@MainActivity)

            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                MaterialTheme {
                    EventSelectionDialog(
                        onEventSelected = { eventType ->
                            dialog.dismiss()
                            when(eventType) {
                                EventType.YELLOW_CARD -> showTeamSelectionDialog(getString(R.string.event_yellow))
                                EventType.RED_CARD -> showTeamSelectionDialog(getString(R.string.event_red))
                                EventType.GOAL -> showTeamSelectionDialog(getString(R.string.event_goal))
                                EventType.INJURY -> recordSimpleEvent(getString(R.string.event_injury), " ", 30)
                                EventType.SUBSTITUTION -> recordSimpleEvent(getString(R.string.event_substitute), " ", 30)
                            }
                        },
                        onDismiss = {
                            dialog.dismiss()
                        }
                    )
                }
            }
        }

        dialog.setContentView(composeView)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()

    }

    private fun recordSimpleEvent(eventType: String, emoji: String, stoppageSeconds: Int) {
        val timeStr = formatTime(mainTime)
        val halfName = if (currentHalf == HALF_FIRST) getString(R.string.status_first_half) else getString(R.string.status_second_half)
        val minute = (mainTime / 60).toInt()

        matchEvents.add(MatchEvent(
            timeStr = timeStr,
            event = eventType,
            emoji = emoji,
            detail = "",
            half = halfName,
            minute = minute
        ))


        addLog("$emoji [$timeStr] $eventType")
    }

    private fun showTimeSettingDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_time_setting, null)

        val tvTimeValue = dialogView.findViewById<TextView>(R.id.tvTimeValue)
        val btnDecrease = dialogView.findViewById<Button>(R.id.btnDecrease)
        val btnIncrease = dialogView.findViewById<Button>(R.id.btnIncrease)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val btnConfirm = dialogView.findViewById<Button>(R.id.btnConfirm)

        // 当前选择的时间（默认45分钟）
        var selectedTime = 45

        // 更新显示
        fun updateDisplay() {
            tvTimeValue.text = selectedTime.toString()
        }

        // 减少按钮
        btnDecrease.setOnClickListener {
            if (selectedTime > 5) {
                selectedTime -= 5
                updateDisplay()
            }
        }

        // 增加按钮
        btnIncrease.setOnClickListener {
            if (selectedTime < 45) {
                selectedTime += 5
                updateDisplay()
            }
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.show()

        dialog.window?.let { window ->
            window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
            window.setLayout(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
            window.decorView.setPadding(0, 0, 0, 0)
        }

        // 取消按钮
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }
        btnConfirm.setOnClickListener {
            halfTimeSeconds = selectedTime * 60L
            matchTimeSet = true

            val btnSetMatchTime = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSetMatchTime)
            btnSetMatchTime?.text = getString(R.string.fmt_duration_simple, selectedTime)

            dialog.dismiss()
        }
    }

    private fun showMatchSummary(isHistory: Boolean = false, historyRecord: MatchRecord? = null) {
        // Prepare data
        val hTime: Int = if (isHistory) {
            historyRecord?.halfTimeMinutes ?: 0
        } else {
            (halfTimeSeconds / 60L).toInt()
        }

        val st1Str = if (isHistory) {
            historyRecord?.firstHalfStoppage ?: "00:00"
        } else {
            formatTime(firstHalfStoppage)
        }

        val st2Str = if (isHistory) {
            historyRecord?.secondHalfStoppage ?: "00:00"
        } else {
            formatTime(stoppageTime)
        }

        val eventsToShow: List<MatchEvent> = if (isHistory) {
            historyRecord?.events ?: listOf()
        } else {
            matchEvents.toList()
        }

        val homeGoals = eventsToShow.count { it.event == getString(R.string.event_goal) && it.detail.contains(getString(R.string.team_home)) }
        val awayGoals = eventsToShow.count { it.event == getString(R.string.event_goal) && it.detail.contains(getString(R.string.team_away)) }
        val yellowCount = eventsToShow.count { it.event == getString(R.string.event_yellow) }
        val redCount = eventsToShow.count { it.event == getString(R.string.event_red) }

        // Launch MatchSummaryActivity instead of Dialog to avoid ScalingLazyColumn height issues
        val intent = android.content.Intent(this, MatchSummaryActivity::class.java).apply {
            putExtra(MatchSummaryActivity.EXTRA_IS_HISTORY, isHistory)
            putExtra(MatchSummaryActivity.EXTRA_DURATION_MINUTES, hTime)
            putExtra(MatchSummaryActivity.EXTRA_HOME_GOALS, homeGoals)
            putExtra(MatchSummaryActivity.EXTRA_AWAY_GOALS, awayGoals)
            putExtra(MatchSummaryActivity.EXTRA_YELLOW_COUNT, yellowCount)
            putExtra(MatchSummaryActivity.EXTRA_RED_COUNT, redCount)
            putExtra(MatchSummaryActivity.EXTRA_STOPPAGE_TIME_1, st1Str)
            putExtra(MatchSummaryActivity.EXTRA_STOPPAGE_TIME_2, st2Str)
            // Serialize events to JSON
            val gson = com.google.gson.Gson()
            putExtra(MatchSummaryActivity.EXTRA_EVENTS_JSON, gson.toJson(eventsToShow))
        }
        startActivity(intent)
    }



    // UI 更新方法

    private fun updateButtonStyle(mode: String) {
        // 1. 获取所有按钮引用（包括手表版的）
        val btnMain = findViewById<com.google.android.material.button.MaterialButton>(R.id.mainButton)
        val btnEnd = findViewById<com.google.android.material.button.MaterialButton>(R.id.endHalfButton)

        // 手表版特有组件
        val btnPauseRound = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnPauseRound)
        val btnEndRound = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnEndRound)
        val controlPanel = findViewById<View>(R.id.controlPanel)
        val btnHistorySmall = findViewById<View>(R.id.btnHistorySmall)

        // 辅助函数：转换 dp 到 px
        fun Int.dp(): Int = (this * resources.displayMetrics.density).toInt()

        // 开启过渡动画
        TransitionManager.beginDelayedTransition(findViewById(android.R.id.content), AutoTransition())

        when (mode) {
            "start" -> {
                // 手机版
                btnMain.text = getString(R.string.btn_start)
                btnMain.setIconResource(R.drawable.baseline_play_arrow_24)
                btnMain.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF2E7D32.toInt())
                btnMain.visibility = View.VISIBLE
                btnEnd.visibility = View.GONE

                // 手表版：恢复初始状态 (圆形，60dp)
                btnPauseRound?.layoutParams = (btnPauseRound?.layoutParams as? LinearLayout.LayoutParams)?.apply {
                    width = 60.dp()
                    marginEnd = 16.dp()
                }
                btnPauseRound?.background = androidx.core.content.ContextCompat.getDrawable(this, R.drawable.bg_btn_pause_round)
                btnPauseRound?.backgroundTintList = null // 清除 tint 以显示 drawable 原色
                btnPauseRound?.setIconResource(R.drawable.pause_circle)
                btnPauseRound?.visibility = View.VISIBLE

                btnEndRound?.visibility = View.VISIBLE
            }

            "pause" -> {
                // 手机版
                btnMain.text = getString(R.string.btn_pause)
                btnMain.setIconResource(R.drawable.pause_circle)
                btnMain.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFFC62828.toInt())
                btnEnd.visibility = View.VISIBLE

                // 手表版：暂停图标 (圆形，60dp)
                // 动画：绿 -> 红
                btnPauseRound?.let { btn ->
                    val startColor = 0xFF2E7D32.toInt() // Green
                    val endColor = 0xFFD32F2F.toInt()   // Red

                    btn.layoutParams = (btn.layoutParams as? LinearLayout.LayoutParams)?.apply {
                        width = 60.dp()
                        marginEnd = 16.dp()
                    }
                    // 确保背景是圆形
                    btn.background = androidx.core.content.ContextCompat.getDrawable(this, R.drawable.bg_btn_pause_round)

                    val animator = android.animation.ValueAnimator.ofArgb(startColor, endColor)
                    animator.duration = 300
                    animator.addUpdateListener { anim ->
                        btn.backgroundTintList = android.content.res.ColorStateList.valueOf(anim.animatedValue as Int)
                    }
                    animator.start()

                    btn.setIconResource(R.drawable.pause_circle)
                    btn.visibility = View.VISIBLE
                }

                btnEndRound?.visibility = View.VISIBLE
            }

            "resume" -> {
                // 手机版
                btnMain.text = getString(R.string.btn_resume)
                btnMain.setIconResource(R.drawable.baseline_play_arrow_24)
                btnMain.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF2E7D32.toInt())
                btnEnd.visibility = View.VISIBLE

                // 手表版：继续图标 (圆形，60dp)
                // 动画：红 -> 绿
                btnPauseRound?.let { btn ->
                    val startColor = 0xFFD32F2F.toInt() // Red
                    val endColor = 0xFF2E7D32.toInt()   // Green

                    btn.layoutParams = (btn.layoutParams as? LinearLayout.LayoutParams)?.apply {
                        width = 60.dp()
                        marginEnd = 16.dp()
                    }
                    // 确保背景是圆形
                    btn.background = androidx.core.content.ContextCompat.getDrawable(this, R.drawable.bg_btn_pause_round)

                    val animator = android.animation.ValueAnimator.ofArgb(startColor, endColor)
                    animator.duration = 300
                    animator.addUpdateListener { anim ->
                        btn.backgroundTintList = android.content.res.ColorStateList.valueOf(anim.animatedValue as Int)
                    }
                    animator.start()

                    btn.setIconResource(R.drawable.baseline_play_arrow_24)
                    btn.visibility = View.VISIBLE
                }

                btnEndRound?.visibility = View.VISIBLE
            }

            "halftime" -> {
                // 手机版
                btnMain.text = getString(R.string.status_second_half)
                btnMain.setIconResource(R.drawable.baseline_play_arrow_24)
                btnMain.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF2E7D32.toInt()) // 绿
                btnMain.visibility = View.VISIBLE
                btnEnd.visibility = View.GONE

                // 手表版逻辑：
                controlPanel?.translationY = 0f // 强制弹出
                btnEndRound?.visibility = View.GONE

                // 变长、变绿、居中
                btnPauseRound?.layoutParams = (btnPauseRound?.layoutParams as? LinearLayout.LayoutParams)?.apply {
                    width = 120.dp() // 变长
                    marginEnd = 0    // 移除右边距以居中
                }
                btnPauseRound?.background = androidx.core.content.ContextCompat.getDrawable(this, R.drawable.bg_btn_pause_pill_green) // 绿色胶囊
                btnPauseRound?.backgroundTintList = null
                btnPauseRound?.setIconResource(R.drawable.baseline_play_arrow_24)
                btnPauseRound?.visibility = View.VISIBLE

                // 取消自动隐藏
                hideHandler.removeCallbacks(hideRunnable!!)
            }

            "finished" -> {
                // 手机版
                btnMain.text = getString(R.string.btn_reset)
                btnMain.setIconResource(R.drawable.ic_substitute)
                btnMain.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFFC62828.toInt())
                btnMain.visibility = View.VISIBLE
                btnEnd.visibility = View.GONE

                // 手表版
                controlPanel?.translationY = 0f
                btnEndRound?.visibility = View.GONE

                // 变长、变红、居中
                btnPauseRound?.layoutParams = (btnPauseRound?.layoutParams as? LinearLayout.LayoutParams)?.apply {
                    width = 120.dp() // 变长
                    marginEnd = 0    // 移除右边距以居中
                }
                btnPauseRound?.background = androidx.core.content.ContextCompat.getDrawable(this, R.drawable.bg_btn_pause_pill_red) // 红色胶囊
                btnPauseRound?.backgroundTintList = null
                btnPauseRound?.setIconResource(R.drawable.ic_substitute)
                btnPauseRound?.visibility = View.VISIBLE

                // 显示历史记录按钮
                btnHistorySmall?.visibility = View.VISIBLE
                hideHandler.removeCallbacks(hideRunnable!!)
            }
        }
    }

    private fun updateStatusLabel() {
        var textStr = ""
        var iconRes = 0

        if (state == STATE_READY) {
            textStr = getString(R.string.status_ready)
            iconRes = R.drawable.sports_soccer
        } else if (state == STATE_RUNNING || state == STATE_PAUSED) {
            textStr = getHalfText(currentHalf)
            iconRes = R.drawable.sports_soccer
        } else if (state == STATE_HALFTIME) {
            textStr = getString(R.string.status_halftime)
            iconRes = R.drawable.ic_coffee
        } else if (state == STATE_FINISHED) {
            textStr = getString(R.string.status_finished)
            iconRes = R.drawable.ic_trophy
        }

        // 更新 UI
        statusLabel.text = textStr

        // 只有当有图标资源时才设置
        if (iconRes != 0) {
            val drawable = androidx.core.content.ContextCompat.getDrawable(this, iconRes)
            if (drawable != null) {
                val size = (20 * resources.displayMetrics.density).toInt()
                drawable.setBounds(0, 0, size, size)
                statusLabel.setCompoundDrawables(drawable, null, null, null)
                statusLabel.compoundDrawablePadding = (8 * resources.displayMetrics.density).toInt()
            }
        }

        statusLabel.compoundDrawableTintList = statusLabel.textColors
    }

    private fun updateStoppageDisplay(active: Boolean) {
        val color = if (active) 0xFFFF6600.toInt() else 0xFF666666.toInt()
        stoppageTimeLabel.setTextColor(color)
        stoppageTimeLabel.compoundDrawableTintList = android.content.res.ColorStateList.valueOf(color)
        updateStoppageTimeDisplay()
    }

    private fun updateStoppageTimeDisplay() {

        stoppageTimeLabel.text = formatTime(stoppageTime)
    }

    private fun updateEndHalfButton() {

        val shouldShow = state == STATE_RUNNING || state == STATE_PAUSED
        endHalfButton.visibility = if (shouldShow) View.VISIBLE else View.GONE
    }

    private fun addLog(message: String) {
        val currentTime = formatTime(mainTime)
        val halfIndicator = when (currentHalf) {
            HALF_FIRST -> "H1"
            HALF_SECOND -> "H2"
            else -> "--"
        }
        Log.d("FootballTimer", "[$halfIndicator $currentTime] $message")
    }

    // 工具方法
    private fun formatTime(seconds: Long): String {
        val minutes = seconds / 60
        val secs = seconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, secs)
    }
    private fun saveMatchRecord() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val currentDate = dateFormat.format(Date())


        // 统计主客队进球
        val homeGoals = matchEvents.count { it.event == getString(R.string.event_goal) && it.detail.contains(getString(R.string.team_home)) }
        val awayGoals = matchEvents.count { it.event == getString(R.string.event_goal) && it.detail.contains(getString(R.string.team_away)) }

        val record = MatchRecord(
            date = currentDate,
            halfTimeMinutes = (halfTimeSeconds / 60).toInt(),
            firstHalfStoppage = formatTime(firstHalfStoppage.toLong()),
            secondHalfStoppage = formatTime(stoppageTime.toLong()),
            totalStoppage = formatTime((firstHalfStoppage + stoppageTime).toLong()),
            goalCount = matchEvents.count { it.event == getString(R.string.event_goal) },
            yellowCount = matchEvents.count { it.event == getString(R.string.event_yellow) },
            redCount = matchEvents.count { it.event == getString(R.string.event_red) },
            substitutionCount = matchEvents.count { it.event == getString(R.string.event_substitute) },
            injuryCount = matchEvents.count { it.event == getString(R.string.event_injury) },

            events = matchEvents.toList(),

            homeGoals = homeGoals,
            awayGoals = awayGoals
        )

        recordManager.saveRecord(record)
        Log.i("FootballTimer", "📁 比赛记录已保存: 主队 $homeGoals - $awayGoals 客队")
    }

    private fun showHistoryDialog() {
        // 使用 Fragment 承载 Compose BottomSheet，避免在 Activity 里直接管理 ComposeView 生命周期
        if (supportFragmentManager.findFragmentByTag(HistoryBottomSheetFragment.TAG) != null) return
        HistoryBottomSheetFragment().show(supportFragmentManager, HistoryBottomSheetFragment.TAG)
    }

    private fun showSettingsDialog() {
        // 使用 Fragment 承载 Compose BottomSheet，显示设置界面
        if (supportFragmentManager.findFragmentByTag(SettingsBottomSheetFragment.TAG) != null) return
        SettingsBottomSheetFragment().show(supportFragmentManager, SettingsBottomSheetFragment.TAG)
    }
    // 显示队伍选择弹窗
    private fun showTeamSelectionDialog(eventType: String) {
        pendingEventType = eventType
        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@MainActivity)
            setViewTreeViewModelStoreOwner(this@MainActivity)
            setViewTreeSavedStateRegistryOwner(this@MainActivity)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)

            setContent {
                MaterialTheme {
                    TeamSelectionDialogCompose(
                        onHomeTeamSelected = {
                            selectedTeam = getString(R.string.team_home)
                            dialog.dismiss()
                            showNumberSelectionDialog(eventType, selectedTeam)
                        },
                        onAwayTeamSelected = {
                            selectedTeam = getString(R.string.team_away)
                            dialog.dismiss()
                            showNumberSelectionDialog(eventType, selectedTeam)
                        },
                        onDismiss = {
                            dialog.dismiss()
                        },
                        homeTeamColor = androidx.compose.ui.graphics.Color(homeTeamColor),
                        awayTeamColor = androidx.compose.ui.graphics.Color(awayTeamColor)
                    )
                }
            }
        }

        dialog.setContentView(composeView)
        dialog.show()
    }

    // 显示号码选择弹窗 (使用 Compose NumberSelectionDialog)
    private fun showNumberSelectionDialog(eventType: String, team: String) {
        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@MainActivity)
            setViewTreeViewModelStoreOwner(this@MainActivity)
            setViewTreeSavedStateRegistryOwner(this@MainActivity)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)

            setContent {
                MaterialTheme {
                    NumberSelectionDialog(
                        initialNumber = 10,
                        title = eventType,
                        onNumberConfirmed = { number ->
                            val numberStr = String.format(Locale.US, "%02d", number)
                            dialog.dismiss()
                            recordEventWithDetails(eventType, team, numberStr)
                        }
                    )
                }
            }
        }

        dialog.setContentView(composeView)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }

    // 记录带详细信息的事件
    private fun recordEventWithDetails(eventType: String, team: String, number: String) {
        val emoji = when (eventType) {
            getString(R.string.event_yellow) -> "🟨"
            getString(R.string.event_red) -> "🟥"
            getString(R.string.event_goal) -> "⚽"
            else -> "📝"
        }

        val teamEmoji = if (team == getString(R.string.team_home)) "🏠" else "✈️"
        val detailText = "$team #$number"
        val timeStr = formatTime(mainTime)
        val halfName = if (currentHalf == HALF_FIRST) getString(R.string.status_first_half) else getString(R.string.status_second_half)
        val minute = (mainTime / 60).toInt()

        matchEvents.add(MatchEvent(
            timeStr = timeStr,
            event = eventType,
            emoji = emoji,
            detail = detailText,
            half = halfName,
            minute = minute
        ))



        updateStoppageTimeDisplay()

        addLog("$emoji [$timeStr] $eventType - $teamEmoji $detailText")
    }

    // 数据类
    data class EventItem(
        val displayText: String,
        val type: String,
        val color: String
    )
    private fun animateHistoryButton(show: Boolean) {

        val historyBtn = findViewById<View>(R.id.btnHistory) ?: return

        if (show) {

            if (historyBtn.visibility == View.VISIBLE && historyBtn.alpha == 1f) return

            historyBtn.visibility = View.VISIBLE
            historyBtn.alpha = 0f
            historyBtn.scaleX = 0.8f
            historyBtn.scaleY = 0.8f

            historyBtn.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(400)
                .setInterpolator(android.view.animation.OvershootInterpolator())
                .start()
        } else {

            if (historyBtn.visibility == View.GONE) return

            historyBtn.animate()
                .alpha(0f)
                .scaleX(0.8f)
                .scaleY(0.8f)
                .setDuration(300)
                .withEndAction { historyBtn.visibility = View.GONE }
                .start()
        }
    }

    private fun showColorSelectionDialog(isHome: Boolean) {
        // 获取当前颜色作为初始值
        val initialColor = if (isHome) homeTeamColor else awayTeamColor

        // 创建一个全屏的FrameLayout作为容器
        val rootView = window.decorView.findViewById<FrameLayout>(android.R.id.content)

        // 创建一个遮罩背景View
        val overlayView = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(0x80000000.toInt()) // 半透明黑色背景
            isClickable = true
            isFocusable = true
        }

        // 创建ComposeView来承载Compose UI
        val composeView = ComposeView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setViewTreeLifecycleOwner(this@MainActivity)
            setViewTreeViewModelStoreOwner(this@MainActivity)
            setViewTreeSavedStateRegistryOwner(this@MainActivity)
        }

        // 定义移除弹窗的方法
        val dismissDialog: () -> Unit = {
            rootView.removeView(composeView)
            rootView.removeView(overlayView)
        }

        // 点击遮罩关闭弹窗
        overlayView.setOnClickListener { dismissDialog() }

        // 设置Compose内容
        composeView.setContent {
            ColorSelectionDialog(
                initialColor = initialColor,
                onColorSelected = { selectedColor ->
                    val r = 15f * resources.displayMetrics.density
                    val finalColor = (0x66 shl 24) or (selectedColor and 0x00FFFFFF)

                    if (isHome) {
                        homeTeamColor = selectedColor
                        val overlay = findViewById<View>(R.id.overlayHome)

                        val shape = android.graphics.drawable.GradientDrawable()
                        shape.shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                        shape.setColor(finalColor)
                        shape.cornerRadii = floatArrayOf(r, r, 0f, 0f, 0f, 0f, r, r)

                        overlay?.background = shape
                    } else {
                        awayTeamColor = selectedColor
                        val overlay = findViewById<View>(R.id.overlayAway)

                        val shape = android.graphics.drawable.GradientDrawable()
                        shape.shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                        shape.setColor(finalColor)
                        shape.cornerRadii = floatArrayOf(0f, 0f, r, r, r, r, 0f, 0f)

                        overlay?.background = shape
                    }
                    dismissDialog()
                }
            )
        }

        // 添加到视图层级
        rootView.addView(overlayView)
        rootView.addView(composeView)
    }
    private fun getHalfText(code: String): String {
        return when (code) {
            HALF_FIRST -> getString(R.string.status_first_half)
            HALF_BREAK -> getString(R.string.status_halftime)
            HALF_SECOND -> getString(R.string.status_second_half)
            else -> ""
        }
    }
}
