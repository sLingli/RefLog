package com.example.myapplication

import android.content.res.Resources
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.widget.NumberPicker
import android.content.res.ColorStateList
import android.util.DisplayMetrics
import android.transition.TransitionManager
import android.transition.AutoTransition





class MainActivity : AppCompatActivity() {

    // ==================== 状态常量 ====================
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

    // ==================== UI 组件 ====================
    private lateinit var statusLabel: TextView

    private lateinit var mainTimeLabel: TextView

    private lateinit var stoppageTimeLabel: TextView
    private lateinit var mainButton: com.google.android.material.button.MaterialButton
    private lateinit var endHalfButton: com.google.android.material.button.MaterialButton


    // ==================== 状态变量 ====================
    private var state: String = STATE_READY
    private var currentHalf: String = HALF_FIRST
    private lateinit var btnHistory: Button
    private lateinit var recordManager: MatchRecordManager


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


    // ==================== 生命周期方法 ====================

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initializeUI()
        resetMatch()
        initializeTimer()
    }
    private fun initializeTimer() {
        // 初始化updateRunnable
        updateRunnable = object : Runnable {
            override fun run() {
                updateTimer()
                handler.postDelayed(this, 100)
            }
        }

        // 立即启动计时器循环（这样计时器就会每秒更新）
        handler.post(updateRunnable)

        Log.i("FootballTimer", "⏱️ 计时器已初始化")
    }


    @SuppressLint("ClickableViewAccessibility")
    private fun initializeUI() {
        statusLabel = findViewById(R.id.statusLabel)
        mainTimeLabel = findViewById(R.id.mainTimeLabel)
        stoppageTimeLabel = findViewById(R.id.stoppageTimeLabel)

        mainButton = findViewById(R.id.mainButton)
        endHalfButton = findViewById(R.id.endHalfButton)
        btnHistory = findViewById(R.id.btnHistory)

        // 绑定点击事件
        mainButton.setOnClickListener { toggleTimer() }
        btnHistory.setOnClickListener { showHistoryDialog() }

        // 定义倒计时任务变量
        var triggerAction: Runnable? = null

        val holdAnimator = android.animation.ValueAnimator.ofInt(0, 10000).apply {
            duration = 1500
            addUpdateListener { animation ->
                endHalfButton.background.level = animation.animatedValue as Int
            }
        }

        endHalfButton.setOnTouchListener { v, event ->
            if (state == STATE_READY) return@setOnTouchListener false

            val vibrator = getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator

            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    // 清理旧任务
                    triggerAction?.let { v.removeCallbacks(it) }

                    holdAnimator.start()

                    // 初始微震
                    if (android.os.Build.VERSION.SDK_INT >= 29) {
                        vibrator.vibrate(android.os.VibrationEffect.createPredefined(android.os.VibrationEffect.EFFECT_TICK))
                    }

                    // 创建新任务
                    triggerAction = Runnable {
                        if (holdAnimator.isRunning) {
                            holdAnimator.end()
                            v.background.level = 0

                            // 成功大震动
                            if (android.os.Build.VERSION.SDK_INT >= 29) {
                                vibrator.vibrate(android.os.VibrationEffect.createPredefined(android.os.VibrationEffect.EFFECT_HEAVY_CLICK))
                            } else {
                                vibrator.vibrate(100)
                            }

                            // 触发结束逻辑
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
                        val currentLevel = endHalfButton.background.level
                        android.animation.ValueAnimator.ofInt(currentLevel, 0).apply {
                            duration = 200
                            addUpdateListener { anim ->
                                endHalfButton.background.level = anim.animatedValue as Int
                            }
                        }.start()
                        holdAnimator.cancel()
                    }
                    true
                }
                else -> false
            }
        }

        recordManager = MatchRecordManager(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        // 停止计时器
        handler.removeCallbacks(updateRunnable)
    }

    // ==================== 状态机控制 ====================

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
        if (!matchTimeSet) {
            showColorSelectionDialog()
            return
        }

        TransitionManager.beginDelayedTransition(findViewById(android.R.id.content), AutoTransition())

        state = STATE_RUNNING
        lastUpdateTime = System.currentTimeMillis()

        updateStatusLabel()

        updateButtonStyle("pause")
        updateStoppageDisplay(active = false)

        val btnHistory = findViewById<View>(R.id.btnHistory)
        btnHistory.visibility = View.GONE

        addLog("🏁 比赛开始")
        val halfTimeMin = halfTimeSeconds / 60
        Log.i("FootballTimer", "📢 比赛开始！每半场 $halfTimeMin 分钟")
    }



    private fun resumeTimer() {
        state = STATE_RUNNING

        updateButtonStyle("pause")
        updateStoppageDisplay(active = false)
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

        updateButtonStyle("start")
        updateStoppageDisplay(active = false)

        // 显式确保结束按钮隐藏
        val endBtn = findViewById<View>(R.id.endHalfButton)
        endBtn.visibility = View.GONE

        Log.i("FootballTimer", "📢 比赛已重置")
        animateHistoryButton(true)
    }


    // ==================== 计时器核心逻辑 ====================

    private fun startUpdateLoop() {
        // 先移除之前的计时器（避免重复）
        handler.removeCallbacks(updateRunnable)

        updateRunnable = object : Runnable {
            override fun run() {
                updateTimer()
                handler.postDelayed(this, 100)  // 每100ms更新一次
            }
        }
        handler.post(updateRunnable)
    }


    private fun updateTimer() {
        val currentTime = System.currentTimeMillis()

        // 检查是否有足够的时间差（至少1秒）
        if (lastUpdateTime > 0 && (currentTime - lastUpdateTime) >= 1000) {


            if (state == STATE_RUNNING || state == STATE_PAUSED) {

                // 1. 主计时器：只要没吹终场哨，它就一直加
                mainTime++

                // 2. 补时计时器：只有在“暂停”状态下，才记录浪费的时间
                if (state == STATE_PAUSED) {
                    stoppageTime++
                }

                // 3. 实时更新 UI 显示
                runOnUiThread {

                    mainTimeLabel.text = formatTime(mainTime)

                    // 补时显示 (胶囊区域)
                    updateStoppageTimeDisplay()

                    // 确保结束按钮状态正确
                    updateEndHalfButton()
                }

                // 4. 检查关键时间点（比如 45 分钟到了震动提醒）
                checkTimeAlerts()

                // 调试日志
                Log.d("计时器", "状态: $state, 主时间: ${formatTime(mainTime)}, 补时: ${formatTime(stoppageTime)}")
            }

            // 更新时间基准
            lastUpdateTime = currentTime

        } else if (lastUpdateTime == 0L) {
            // 如果是第一次，初始化时间基准
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


                val targetTime = halfTimeSeconds * 2  // 比如45*2=90分钟
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

    // ==================== 事件弹窗 ====================

    private fun pauseTimer() {
        state = STATE_PAUSED

        updateButtonStyle("start")
        updateStoppageDisplay(active = true)

        // 显示事件选择弹窗
        showEventDialog()
    }

    private fun showEventDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_event_selection, null)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        // 黄牌 - 需要选择队伍和号码
        dialogView.findViewById<View>(R.id.btnYellow).setOnClickListener {
            dialog.dismiss()
            showTeamSelectionDialog(getString(R.string.event_yellow))
        }

        // 红牌 - 需要选择队伍和号码
        dialogView.findViewById<View>(R.id.btnRed).setOnClickListener {
            dialog.dismiss()
            showTeamSelectionDialog(getString(R.string.event_red))
        }

        // 进球 - 需要选择队伍和号码
        dialogView.findViewById<View>(R.id.btnGoal).setOnClickListener {
            dialog.dismiss()
            showTeamSelectionDialog(getString(R.string.event_goal))
        }

        // 伤停 - 直接记录（不需要选择队伍和号码）
        dialogView.findViewById<View>(R.id.btnInjury).setOnClickListener {
            dialog.dismiss()
            recordSimpleEvent(getString(R.string.event_injury), " ", 30)
        }

        // 换人 - 直接记录（不需要选择队伍和号码）
        dialogView.findViewById<View>(R.id.btnSubstitution).setOnClickListener {
            dialog.dismiss()
            recordSimpleEvent(getString(R.string.event_substitute), " ", 30)
        }

        // 取消
        dialogView.findViewById<View>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
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

        // 取消按钮
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        // 确认按钮
        btnConfirm.setOnClickListener {
            // 设置比赛时间
            halfTimeSeconds = selectedTime * 60L
            matchTimeSet = true


            dialog.dismiss()

            // 开始比赛
            startTimer()
        }

        dialog.show()
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
    }


    private fun showMatchSummary(isHistory: Boolean = false, historyRecord: MatchRecord? = null) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_match_summary, null)

        val tvTitle = dialogView.findViewById<TextView>(R.id.tvSummaryTitle)
        val tvStatMatchTime = dialogView.findViewById<TextView>(R.id.tvStatMatchTime)
        val tvStatGoals = dialogView.findViewById<TextView>(R.id.tvStatGoals)
        val tvStatYellow = dialogView.findViewById<TextView>(R.id.tvStatYellow)
        val tvStatRed = dialogView.findViewById<TextView>(R.id.tvStatRed)
        val tvStatStoppage = dialogView.findViewById<TextView>(R.id.tvStatStoppage)
        val listEvents = dialogView.findViewById<LinearLayout>(R.id.listSummaryEvents)
        val btnClose = dialogView.findViewById<Button>(R.id.btnSummaryClose)

        // 数据准备
        val hTime: Int = if (isHistory) {
            (historyRecord?.halfTimeMinutes ?: 0).toInt()
        } else {
            (halfTimeSeconds.toLong() / 60L).toInt()
        }

        val st1: Long = if (isHistory) {
            historyRecord?.firstHalfStoppage?.toLongOrNull() ?: 0L
        } else {
            try { firstHalfStoppage.toLong() } catch(e: Exception) { 0L }
        }

        val st2: Long = if (isHistory) {
            historyRecord?.secondHalfStoppage?.toLongOrNull() ?: 0L
        } else {
            try { stoppageTime.toLong() } catch(e: Exception) { 0L }
        }

        val eventsToShow: List<MatchEvent> = if (isHistory) {
            historyRecord?.events ?: listOf()
        } else {
            matchEvents
        }

        // 1. 设置标题
        tvTitle.text = if (isHistory) "历史详情" else getString(R.string.title_summary)

        val homeGoals = eventsToShow.count { it.event == getString(R.string.event_goal) && it.detail.contains(getString(R.string.team_home)) }
        val awayGoals = eventsToShow.count { it.event == getString(R.string.event_goal) && it.detail.contains(getString(R.string.team_away)) }

        // 1. 时长：使用占位符填入分钟数
        tvStatMatchTime.text = getString(R.string.summary_duration, hTime)

// 2. 比分：填入主客队进球数
        tvStatGoals.text = getString(R.string.summary_score, homeGoals, awayGoals)

// 3. 黄牌：先计算数量，再填入占位符
        val yellowCount = eventsToShow.count { it.event == getString(R.string.event_yellow) }
        tvStatYellow.text = getString(R.string.summary_yellow, yellowCount)

// 4. 红牌：先计算数量，再填入占位符
        val redCount = eventsToShow.count { it.event == getString(R.string.event_red) }
        tvStatRed.text = getString(R.string.summary_red, redCount)

// 5. 补时：填入格式化后的时间字符串
        tvStatStoppage.text = getString(R.string.summary_stoppage, formatTime(st1), formatTime(st2))

        // 3. 填充事件明细 (使用 LinearLayout 容器法，确保图标贴着文字居中)
        listEvents.removeAllViews()
        if (eventsToShow.isEmpty()) {
            val tv = TextView(this)
            tv.text = getString(R.string.msg_no_events)
            tv.setTextColor(android.graphics.Color.GRAY)
            tv.gravity = android.view.Gravity.CENTER
            listEvents.addView(tv)
        } else {
            eventsToShow.forEach { event ->
                // 1. 创建一个水平容器
                val rowContainer = LinearLayout(this)
                rowContainer.orientation = LinearLayout.HORIZONTAL
                rowContainer.gravity = android.view.Gravity.CENTER // 让里面的东西居中
                rowContainer.setPadding(0, 8, 0, 8) // 上下间距

                // 2. 创建图标 ImageView
                val iconView = android.widget.ImageView(this)
                val iconRes = when(event.event) {
                    getString(R.string.event_goal) -> R.drawable.sports_soccer
                    getString(R.string.event_yellow), getString(R.string.event_red) -> R.drawable.ic_card
                    getString(R.string.event_substitute) -> R.drawable.ic_substitute
                    getString(R.string.event_injury) -> R.drawable.ic_medical
                    else -> R.drawable.ic_history
                }
                iconView.setImageResource(iconRes)

                // 设置图标大小 (20dp)
                val density = resources.displayMetrics.density
                val iconSize = (20 * density).toInt()
                val params = LinearLayout.LayoutParams(iconSize, iconSize)
                params.marginEnd = (8 * density).toInt() // 图标和字的间距
                iconView.layoutParams = params

                // 设置图标颜色
                try {
                    val iconColor = when(event.event){
                        getString(R.string.event_goal) -> android.graphics.Color.WHITE
                        getString(R.string.event_yellow) -> android.graphics.Color.YELLOW
                        getString(R.string.event_red) -> android.graphics.Color.RED
                        getString(R.string.event_injury) -> android.graphics.Color.parseColor("#2196F3")
                        else -> android.graphics.Color.GREEN
                    }
                    iconView.setColorFilter(iconColor)
                } catch (e: Exception) {}

                // 3. 创建文字 TextView
                val textView = TextView(this)
                val contentText = if (event.detail.isNotEmpty()) event.detail else event.event
                textView.text = "[${event.timeStr}] $contentText"
                textView.setTextColor(android.graphics.Color.parseColor("#CCCCCC"))
                textView.textSize = 13f

                // 4. 装填进容器
                rowContainer.addView(iconView)
                rowContainer.addView(textView)

                // 5. 添加到列表
                listEvents.addView(rowContainer)
            }
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        btnClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        val window = dialog.window
        if (window != null) {
            window.setLayout(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            )
            val params = window.attributes

            // 设置对齐方式为：底部对齐
            window.setGravity(android.view.Gravity.TOP)

            // 设置 Y 轴偏移量 (距离底部的距离)
            params.y = (200 * resources.displayMetrics.density).toInt()

            window.attributes = params
        }
    }

    // 辅助函数：dp转px
    fun Int.dpToPx(): Int = (this * Resources.getSystem().displayMetrics.density).toInt()



    // ==================== UI 更新方法 ====================

    private fun updateButtonStyle(mode: String) {

        val btnMain = findViewById<com.google.android.material.button.MaterialButton>(R.id.mainButton)
        val btnEnd = findViewById<com.google.android.material.button.MaterialButton>(R.id.endHalfButton)


        TransitionManager.beginDelayedTransition(findViewById(android.R.id.content), AutoTransition())

        when (mode) {
            "start" -> {
                mainButton.text = getString(R.string.btn_start)
                mainButton.setIconResource(R.drawable.baseline_play_arrow_24)
                mainButton.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF2E7D32.toInt()) // 绿

                mainButton.visibility = View.VISIBLE
                endHalfButton.visibility = View.GONE
            }

            "pause" -> {
                mainButton.text = getString(R.string.btn_pause)
                mainButton.setIconResource(R.drawable.pause_circle)
                mainButton.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFFC62828.toInt()) // 红

                endHalfButton.text = getString(R.string.btn_stop)
                endHalfButton.setIconResource(R.drawable.stop_circle)

                mainButton.visibility = View.VISIBLE
                endHalfButton.visibility = View.VISIBLE
            }

            "resume" -> {
                mainButton.text = getString(R.string.btn_resume)
                mainButton.setIconResource(R.drawable.baseline_play_arrow_24)
                mainButton.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF2E7D32.toInt()) // 绿

                endHalfButton.text = getString(R.string.btn_stop)
                endHalfButton.setIconResource(R.drawable.stop_circle)

                mainButton.visibility = View.VISIBLE
                endHalfButton.visibility = View.VISIBLE
            }

            "halftime" -> {
                mainButton.text = getString(R.string.status_second_half)
                mainButton.setIconResource(R.drawable.baseline_play_arrow_24)
                mainButton.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF2E7D32.toInt()) // 绿

                mainButton.visibility = View.VISIBLE
                endHalfButton.visibility = View.GONE
            }

            "finished" -> {
                mainButton.text = getString(R.string.btn_reset)
                mainButton.setIconResource(R.drawable.ic_substitute)
                mainButton.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFFC62828.toInt())
                mainButton.visibility = View.VISIBLE
                endHalfButton.visibility = View.GONE
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



    // ==================== 工具方法 ====================

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
        val dialogView = android.view.LayoutInflater.from(this).inflate(R.layout.dialog_history, null)

        val recordsContainer = dialogView.findViewById<android.widget.LinearLayout>(R.id.recordsContainer)
        val tvNoRecords = dialogView.findViewById<android.widget.TextView>(R.id.tvNoRecords)
        val btnClearHistory = dialogView.findViewById<android.widget.Button>(R.id.btnClearHistory)
        val btnCloseHistory = dialogView.findViewById<android.widget.Button>(R.id.btnCloseHistory)

        val records = recordManager.getAllRecords()

        if (records.isEmpty()) {
            tvNoRecords.visibility = android.view.View.VISIBLE
            recordsContainer.visibility = android.view.View.GONE
        } else {
            tvNoRecords.visibility = android.view.View.GONE
            recordsContainer.visibility = android.view.View.VISIBLE

            @SuppressLint("ClickableViewAccessibility")
            records.forEach { record ->
                val itemWrapper = android.widget.FrameLayout(this)
                val wrapperParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                )
                wrapperParams.setMargins(0, 0, 0, (8 * resources.displayMetrics.density).toInt())
                itemWrapper.layoutParams = wrapperParams


                val btnDelete = android.widget.ImageView(this).apply {
                    setImageResource(R.drawable.outline_delete_24)
                    setColorFilter(android.graphics.Color.WHITE)
                    background = android.graphics.drawable.GradientDrawable().apply {
                        shape = android.graphics.drawable.GradientDrawable.OVAL
                        setColor(android.graphics.Color.parseColor("#D32F2F"))
                    }
                    val btnSize = (42 * resources.displayMetrics.density).toInt()
                    layoutParams = android.widget.FrameLayout.LayoutParams(btnSize, btnSize).apply {
                        gravity = android.view.Gravity.CENTER_VERTICAL or android.view.Gravity.END
                        marginEnd = (16 * resources.displayMetrics.density).toInt()
                    }
                    setPadding(24, 24, 24, 24)
                    elevation = 2f
                    alpha = 0f
                    isEnabled = false
                }

                // 2. 顶层内容布局
                val itemView = android.view.LayoutInflater.from(this).inflate(R.layout.item_match_record, itemWrapper, false) as android.view.ViewGroup

                itemView.setBackgroundResource(R.drawable.bg_dialog_rounded)


                itemView.findViewById<android.widget.TextView>(R.id.tvRecordDate).text = record.date
                itemView.findViewById<android.widget.TextView>(R.id.tvRecordDuration).text = getString(R.string.fmt_duration_simple)
                itemView.findViewById<android.widget.TextView>(R.id.tvRecordStoppage).text = getString(R.string.summary_stoppage)
                itemView.findViewById<android.view.View>(R.id.tvRecordEvents).visibility = android.view.View.GONE

                itemView.findViewById<android.widget.TextView>(R.id.tvRecordDate).text = record.date
                itemView.findViewById<android.widget.TextView>(R.id.tvRecordDuration).text = getString(R.string.fmt_duration_simple,record.halfTimeMinutes)
                itemView.findViewById<android.widget.TextView>(R.id.tvRecordStoppage).text =
                    getString(R.string.summary_stoppage,record.firstHalfStoppage, record.secondHalfStoppage)

                val oldTv = itemView.findViewById<android.widget.TextView>(R.id.tvRecordEvents)
                oldTv.visibility = android.view.View.GONE

                val statsLayout = android.widget.LinearLayout(this)
                statsLayout.orientation = android.widget.LinearLayout.HORIZONTAL
                statsLayout.gravity = android.view.Gravity.CENTER_VERTICAL
                statsLayout.setPadding(0, (8 * resources.displayMetrics.density).toInt(), 0, 0)

                fun addStat(iconRes: Int, count: Int, color: Int) {
                    val itemContainer = android.widget.LinearLayout(this)
                    itemContainer.orientation = android.widget.LinearLayout.HORIZONTAL
                    itemContainer.gravity = android.view.Gravity.CENTER_VERTICAL
                    val lp = android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    lp.setMargins(0, 0, (12 * resources.displayMetrics.density).toInt(), 0)
                    itemContainer.layoutParams = lp
                    val iv = android.widget.ImageView(this)
                    iv.setImageResource(iconRes)
                    iv.setColorFilter(color)
                    val size = (16 * resources.displayMetrics.density).toInt()
                    iv.layoutParams = android.widget.LinearLayout.LayoutParams(size, size)
                    val tv = android.widget.TextView(this)
                    tv.text = count.toString()
                    tv.setTextColor(android.graphics.Color.WHITE)
                    tv.textSize = 13f
                    tv.setPadding((4 * resources.displayMetrics.density).toInt(), 0, 0, 0)
                    itemContainer.addView(iv)
                    itemContainer.addView(tv)
                    statsLayout.addView(itemContainer)
                }

                addStat(R.drawable.sports_soccer, record.goalCount, android.graphics.Color.WHITE)
                addStat(R.drawable.ic_card, record.yellowCount, android.graphics.Color.YELLOW)
                addStat(R.drawable.ic_card, record.redCount, android.graphics.Color.RED)
                addStat(R.drawable.ic_substitute, record.substitutionCount, android.graphics.Color.GREEN)
                addStat(R.drawable.ic_medical, record.injuryCount, android.graphics.Color.parseColor("#2196F3"))

                itemView.addView(statsLayout)


                var startX = 0f
                var isSwiped = false


                var startRawX = 0f
                var startTranslationX = 0f
                val maxSwipeDistance = -200f

                itemView.setOnTouchListener { v, event ->
                    when (event.action) {
                        android.view.MotionEvent.ACTION_DOWN -> {
                            startRawX = event.rawX
                            startTranslationX = v.translationX

                            v.parent.requestDisallowInterceptTouchEvent(true)
                            true
                        }

                        android.view.MotionEvent.ACTION_MOVE -> {
                            val deltaX = event.rawX - startRawX

                            val targetX = (startTranslationX + deltaX).coerceIn(maxSwipeDistance, 0f)

                            v.translationX = targetX


                            val progress = Math.abs(targetX / maxSwipeDistance)
                            btnDelete.alpha = progress

                            true
                        }

                        android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                            val currentX = v.translationX
                            val totalDelta = Math.abs(event.rawX - startRawX)


                            if (totalDelta < 10) {
                                if (currentX == 0f) {

                                    showMatchSummary(isHistory = true, historyRecord = record)
                                } else {

                                    v.animate().translationX(0f).setDuration(200).start()
                                    btnDelete.animate().alpha(0f).setDuration(200).start()
                                    btnDelete.isEnabled = false
                                }
                            }
                            // 2. 判断滑动意图
                            else {

                                if (currentX < maxSwipeDistance / 2) {
                                    v.animate().translationX(maxSwipeDistance).setDuration(200).start()
                                    btnDelete.animate().alpha(1f).setDuration(200).start()
                                    btnDelete.isEnabled = true
                                } else {
                                    v.animate().translationX(0f).setDuration(200).start()
                                    btnDelete.animate().alpha(0f).setDuration(200).start()
                                    btnDelete.isEnabled = false
                                }
                            }

                            // 恢复父容器滑动
                            v.parent.requestDisallowInterceptTouchEvent(false)
                            true
                        }
                        else -> false
                    }
                }

                // 4. 删除按钮点击
                btnDelete.setOnClickListener {

                    recordManager.deleteRecord(record.id)
                    itemWrapper.animate().alpha(0f).translationX(-500f).setDuration(300).withEndAction {
                        recordsContainer.removeView(itemWrapper)
                        if (recordsContainer.childCount == 0) {
                            tvNoRecords.visibility = android.view.View.VISIBLE
                        }
                    }.start()
                }

                itemWrapper.addView(btnDelete)
                itemWrapper.addView(itemView)
                recordsContainer.addView(itemWrapper)
            }
        }

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        // 清空按钮点击事件
        btnClearHistory.setOnClickListener {
            // 1. 如果没记录，直接提示并返回
            if (records.isEmpty()) {
                android.widget.Toast.makeText(this, getString(R.string.msg_no_history_to_clear), android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 2. 加载确认弹窗布局
            val confirmView = android.view.LayoutInflater.from(this).inflate(R.layout.dialog_confirm, null)

            val tvMessage = confirmView.findViewById<android.widget.TextView>(R.id.tvConfirmMessage)
            tvMessage.text = getString(R.string.msg_confirm_clear_all)
            tvMessage.visibility = android.view.View.VISIBLE

            val confirmDialog = androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(confirmView)
                .create()

            // 3. 绑定按钮事件
            confirmView.findViewById<android.view.View>(R.id.btnNo).setOnClickListener {
                confirmDialog.dismiss()
            }

            confirmView.findViewById<android.view.View>(R.id.btnYes).setOnClickListener {
                recordManager.clearAllRecords()
                confirmDialog.dismiss()
                dialog.dismiss()
                android.widget.Toast.makeText(this, getString(R.string.msg_history_cleared), android.widget.Toast.LENGTH_SHORT).show()
            }

            // 4. 显示弹窗并去白角
            confirmDialog.show()
            confirmDialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        }

        btnCloseHistory.setOnClickListener { dialog.dismiss() }
        dialog.show()
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
    }
    // 显示队伍选择弹窗
    private fun showTeamSelectionDialog(eventType: String) {
        pendingEventType = eventType
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_team_selection, null)

        val tvTitle = dialogView.findViewById<TextView>(R.id.tvTeamSelectionTitle)
        val btnHomeTeam = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnHomeTeam)
        val btnAwayTeam = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnAwayTeam)
        val btnCancel = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancelTeam)


        val (iconRes, iconColor) = when (eventType) {
            getString(R.string.event_yellow) -> R.drawable.ic_card to android.graphics.Color.YELLOW
            getString(R.string.event_red) -> R.drawable.ic_card to android.graphics.Color.RED
            getString(R.string.event_goal) -> R.drawable.sports_soccer to android.graphics.Color.WHITE
            else -> 0 to 0
        }
        val actionText = getString(R.string.title_select_team_generic)
        tvTitle.text = "$eventType - $actionText"
        if (iconRes != 0) {
            val drawable = androidx.core.content.ContextCompat.getDrawable(this, iconRes)?.mutate()
            drawable?.setTint(iconColor)
            // 设置图标大小为 20dp
            val size = (20 * resources.displayMetrics.density).toInt()
            drawable?.setBounds(0, 0, size, size)
            tvTitle.setCompoundDrawables(drawable, null, null, null)
            tvTitle.compoundDrawablePadding = (8 * resources.displayMetrics.density).toInt()
        }

        // 2. 应用主客队颜色
        btnHomeTeam.backgroundTintList = android.content.res.ColorStateList.valueOf(homeTeamColor)
        btnAwayTeam.backgroundTintList = android.content.res.ColorStateList.valueOf(awayTeamColor)

        // 3. 智能反色逻辑
        if (homeTeamColor == 0xFFFFFFFF.toInt()) {
            btnHomeTeam.setTextColor(android.graphics.Color.BLACK)
            btnHomeTeam.iconTint = android.content.res.ColorStateList.valueOf(android.graphics.Color.BLACK)
        } else {
            btnHomeTeam.setTextColor(android.graphics.Color.WHITE)
            btnHomeTeam.iconTint = android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE)
        }

        if (awayTeamColor == 0xFFFFFFFF.toInt()) {
            btnAwayTeam.setTextColor(android.graphics.Color.BLACK)
            btnAwayTeam.iconTint = android.content.res.ColorStateList.valueOf(android.graphics.Color.BLACK)
        } else {
            btnAwayTeam.setTextColor(android.graphics.Color.WHITE)
            btnAwayTeam.iconTint = android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE)
        }

        val dialog = AlertDialog.Builder(this).setView(dialogView).setCancelable(true).create()

        btnHomeTeam.setOnClickListener {
            selectedTeam = getString(R.string.team_home)
            dialog.dismiss()
            showNumberSelectionDialog(eventType, selectedTeam)
        }

        btnAwayTeam.setOnClickListener {
            selectedTeam = getString(R.string.team_away)
            dialog.dismiss()
            showNumberSelectionDialog(eventType, selectedTeam)
        }

        btnCancel.setOnClickListener { dialog.dismiss() }
        dialog.show()
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
    }

    // 显示号码选择弹窗
    private fun showNumberSelectionDialog(eventType: String, team: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_number_selection, null)

        val tvTitle = dialogView.findViewById<TextView>(R.id.tvNumberTitle)
        val tvTeamInfo = dialogView.findViewById<TextView>(R.id.tvTeamInfo)
        val tvSelectedNumber = dialogView.findViewById<TextView>(R.id.tvSelectedNumber)
        val pickerTens = dialogView.findViewById<NumberPicker>(R.id.pickerTens)
        val pickerOnes = dialogView.findViewById<NumberPicker>(R.id.pickerOnes)
        val btnCancel = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancelNumber)
        val btnConfirm = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnConfirmNumber)


        val (iconRes, iconColor) = when (eventType) {
            getString(R.string.event_yellow) -> R.drawable.ic_card to android.graphics.Color.YELLOW
            getString(R.string.event_red) -> R.drawable.ic_card to android.graphics.Color.RED
            getString(R.string.event_goal) -> R.drawable.sports_soccer to android.graphics.Color.WHITE
            else -> 0 to 0
        }

        tvTitle.text = eventType
        if (iconRes != 0) {
            val drawable = androidx.core.content.ContextCompat.getDrawable(this, iconRes)?.mutate()
            drawable?.setTint(iconColor)
            val size = (20 * resources.displayMetrics.density).toInt()
            drawable?.setBounds(0, 0, size, size)
            tvTitle.setCompoundDrawables(drawable, null, null, null)
            tvTitle.compoundDrawablePadding = (8 * resources.displayMetrics.density).toInt()
        }

        // 设置队伍信息颜色
        tvTeamInfo.text = team
        tvTeamInfo.setTextColor(if (team == getString(R.string.team_home)) 0xFF1565C0.toInt() else 0xFFC62828.toInt())

        // 设置滚轮逻辑 (保持不变)
        pickerTens.minValue = 0
        pickerTens.maxValue = 9
        pickerTens.value = 0
        pickerTens.wrapSelectorWheel = true
        pickerOnes.minValue = 0
        pickerOnes.maxValue = 9
        pickerOnes.value = 1
        pickerOnes.wrapSelectorWheel = true

        fun updateSelectedNumber() {
            val number = pickerTens.value * 10 + pickerOnes.value
            tvSelectedNumber.text = "# ${String.format("%02d", number)}"
        }

        updateSelectedNumber()
        pickerTens.setOnValueChangedListener { _, _, _ -> updateSelectedNumber() }
        pickerOnes.setOnValueChangedListener { _, _, _ -> updateSelectedNumber() }

        val dialog = AlertDialog.Builder(this).setView(dialogView).setCancelable(true).create()

        btnCancel.setOnClickListener { dialog.dismiss() }
        btnConfirm.setOnClickListener {
            val number = pickerTens.value * 10 + pickerOnes.value
            val numberStr = String.format("%02d", number)
            dialog.dismiss()
            recordEventWithDetails(eventType, team, numberStr)
        }
        dialog.show()
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
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


    private fun showColorSelectionDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_color_selection, null)

        val rvHome = dialogView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvHomeColors)
        val rvAway = dialogView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvAwayColors)
        val btnConfirm = dialogView.findViewById<Button>(R.id.btnConfirmColor)

        val colors = listOf(
            0xFFF44336.toInt(), // 红 (Index 0)
            0xFF2196F3.toInt(), // 蓝 (Index 1)
            0xFF4CAF50.toInt(), // 绿
            0xFFFFEB3B.toInt(), // 黄
            0xFFFFFFFF.toInt(), // 白
            0xFF000000.toInt(), // 黑
            0xFF9C27B0.toInt(), // 紫
            0xFFFF9800.toInt()  // 橙
        )

        var tempHomeColor = colors[1]
        var tempAwayColor = colors[0]

        fun setupWheel(rv: androidx.recyclerview.widget.RecyclerView, initialIndex: Int, onSelect: (Int) -> Unit) {
            rv.layoutManager = CenterScaleLayoutManager(this)
            val adapter = ColorWheelAdapter(colors) { }
            rv.adapter = adapter


            val density = resources.displayMetrics.density
            val padding = (45 * density).toInt()
            rv.setPadding(0, padding, 0, padding)
            rv.clipToPadding = false


            val snapHelper = object : androidx.recyclerview.widget.LinearSnapHelper() {


                override fun calculateScrollDistance(velocityX: Int, velocityY: Int): IntArray {
                    return super.calculateScrollDistance(velocityX, (velocityY * 0.5).toInt())
                }


                override fun createScroller(layoutManager: androidx.recyclerview.widget.RecyclerView.LayoutManager?): androidx.recyclerview.widget.RecyclerView.SmoothScroller? {
                    if (layoutManager !is androidx.recyclerview.widget.RecyclerView.SmoothScroller.ScrollVectorProvider) return null

                    return object : androidx.recyclerview.widget.LinearSmoothScroller(rv.context) {


                        override fun calculateTimeForDeceleration(dx: Int): Int {

                            return super.calculateTimeForDeceleration(dx) * 5
                        }


                        override fun onTargetFound(targetView: android.view.View, state: androidx.recyclerview.widget.RecyclerView.State, action: Action) {
                            val snapDistances = calculateDistanceToFinalSnap(layoutManager, targetView)
                            val dx = snapDistances!![0]
                            val dy = snapDistances[1]

                            // 计算需要的时间
                            val time = calculateTimeForDeceleration(Math.max(Math.abs(dx), Math.abs(dy)))

                            if (time > 0) {

                                action.update(dx, dy, time, android.view.animation.OvershootInterpolator(2.0f))
                            }
                        }
                    }
                }
            }
            snapHelper.attachToRecyclerView(rv)

            rv.addOnScrollListener(object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: androidx.recyclerview.widget.RecyclerView, newState: Int) {
                    if (newState == androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE) {
                        val centerView = snapHelper.findSnapView(rv.layoutManager)
                        centerView?.let {
                            val pos = rv.layoutManager?.getPosition(it) ?: 0
                            val color = colors[pos % colors.size]
                            onSelect(color)
                        }
                    }
                }
            })


            val centerStart = Int.MAX_VALUE / 2
            val startPos = centerStart - (centerStart % colors.size) + initialIndex


            (rv.layoutManager as androidx.recyclerview.widget.LinearLayoutManager).scrollToPositionWithOffset(startPos, 0)

            onSelect(colors[initialIndex])
        }

        // 主队：默认蓝 (Index 1)
        setupWheel(rvHome, 1) { tempHomeColor = it }
        // 客队：默认红 (Index 0)
        setupWheel(rvAway, 0) { tempAwayColor = it }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        btnConfirm.setOnClickListener {
            homeTeamColor = tempHomeColor
            awayTeamColor = tempAwayColor
            dialog.dismiss()
            showTimeSettingDialog()
        }

        dialog.show()
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
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
