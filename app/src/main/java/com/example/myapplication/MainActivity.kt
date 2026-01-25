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




class MainActivity : AppCompatActivity() {

    // ==================== 状态常量 ====================
    private companion object {
        const val STATE_READY = "ready"
        const val STATE_RUNNING = "running"
        const val STATE_PAUSED = "paused"
        const val STATE_HALFTIME = "halftime"
        const val STATE_FINISHED = "finished"

        const val HALF_FIRST = "上半场"
        const val HALF_BREAK = "中场休息"
        const val HALF_SECOND = "下半场"

        const val DEFAULT_HALF_TIME = 45  // 默认半场时间(分钟)
    }

    // ==================== UI 组件 ====================
    private lateinit var statusLabel: TextView

    private lateinit var mainTimeLabel: TextView
    private lateinit var stoppageTitleLabel: TextView
    private lateinit var stoppageTimeLabel: TextView
    private lateinit var mainButton: Button
    private lateinit var endHalfButton: Button


    // ==================== 状态变量 ====================
    private var state: String = STATE_READY
    private var currentHalf: String = HALF_FIRST
    private lateinit var btnHistory: Button
    private lateinit var recordManager: MatchRecordManager


    // 计时器变量
    private var mainTime: Long = 0  // 主计时器（秒）
    private var stoppageTime: Long = 0  // 当前半场补时（秒）
    private var firstHalfStoppage: Long = 0  // 上半场补时总计
    private var lastUpdateTime: Long = 0  // 上次更新的时间戳

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
    private var pendingEventType: String = ""  // 待处理的事件类型
    private var selectedTeam: String = ""       // 选择的队伍


    // ==================== 生命周期方法 ====================

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 初始化UI组件
        initializeUI()

        // 初始化状态变量
        resetMatch()

        // 启动计时器循环
        initializeTimer()
    }
    private fun initializeTimer() {
        // 初始化updateRunnable
        updateRunnable = object : Runnable {
            override fun run() {
                updateTimer()
                handler.postDelayed(this, 100)  // 每100ms更新一次
            }
        }

        // 立即启动计时器循环（这样计时器就会每秒更新）
        handler.post(updateRunnable)

        Log.i("FootballTimer", "⏱️ 计时器已初始化")
    }

    private fun initializeUI() {
        statusLabel = findViewById(R.id.statusLabel)
        mainTimeLabel = findViewById(R.id.mainTimeLabel)
        stoppageTitleLabel = findViewById(R.id.stoppageTitleLabel)
        stoppageTimeLabel = findViewById(R.id.stoppageTimeLabel)
        mainButton = findViewById(R.id.mainButton)
        endHalfButton = findViewById(R.id.endHalfButton)

        // 设置按钮点击事件
        mainButton.setOnClickListener { toggleTimer() }
        endHalfButton.setOnClickListener { endHalf() }
        // 初始化历史记录
        btnHistory = findViewById(R.id.btnHistory)
        recordManager = MatchRecordManager(this)

// 历史记录按钮点击事件
        btnHistory.setOnClickListener { showHistoryDialog() }

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
                Log.d("状态机", "从READY开始")
                startTimer()
            }
            STATE_RUNNING -> {
                Log.d("状态机", "从RUNNING暂停")
                pauseTimer()
            }
            STATE_PAUSED -> {
                Log.d("状态机", "从PAUSED继续")
                resumeTimer()
            }
            STATE_HALFTIME -> {
                Log.d("状态机", "从中场休息开始下半场")
                startSecondHalf()
            }
            STATE_FINISHED -> {
                Log.d("状态机", "比赛结束，重新开始")
                resetMatch()
            }
        }
    }


    private fun startTimer() {
        // 如果还没设置比赛时间，先弹出设置窗口
        if (!matchTimeSet) {
            showTimeSettingDialog()
            return
        }

        state = STATE_RUNNING
        lastUpdateTime = System.currentTimeMillis()  // ✅ 重置时间基准，让计时器立即开始工作

        updateButtonStyle("pause")
        updateStoppageDisplay(active = false)


        addLog("🏁 比赛开始")
        val halfTimeMin = halfTimeSeconds / 60
        Log.i("FootballTimer", "📢 比赛开始！每半场 $halfTimeMin 分钟")
    }



    private fun resumeTimer() {
        state = STATE_RUNNING
        lastUpdateTime = System.currentTimeMillis()  // ✅ 重置时间基准

        updateButtonStyle("pause")
        updateStoppageDisplay(active = false)
    }


    private fun startSecondHalf() {
        currentHalf = HALF_SECOND

        // ⭐⭐⭐ 核心：下半场主时间从半场时间的末尾开始 ⭐⭐⭐
        // 比如半场2分钟，上半场在 02:30 结束，下半场应该从 02:00 开始
        // 或者按足球习惯，从 45:00 开始计时
        mainTime = halfTimeSeconds  // ✅ 正确：从半场时间的开始处

        state = STATE_RUNNING
        stoppageTime = 0
        lastUpdateTime = System.currentTimeMillis()
        fullTimeAlertShown = false

        statusLabel.text = "⚽ 下半场"

        // ⭐⭐⭐ 更新显示：显示起始时间 ⭐⭐⭐
        mainTimeLabel.text = formatTime(mainTime)
        mainTimeLabel.setTextColor(0xFF00FF00.toInt())

        updateButtonStyle("pause")
        updateStoppageDisplay(active = false)
        updateStoppageTimeDisplay()

        // ⭐⭐⭐ 确保计时器运行 ⭐⭐⭐
        startUpdateLoop()

        addLog("🏁 下半场开始 - 从 ${formatTime(mainTime)} 继续计时")
        Log.i("FootballTimer", "📢 下半场开始！从 ${formatTime(mainTime)} 计时")
    }



    private fun endHalf() {
        if (state == STATE_READY) return

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_confirm, null)

        val btnNo = dialogView.findViewById<Button>(R.id.btnNo)
        val btnYes = dialogView.findViewById<Button>(R.id.btnYes)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        // 取消按钮
        btnNo.setOnClickListener {
            dialog.dismiss()
        }

        // 确认按钮
        btnYes.setOnClickListener {
            dialog.dismiss()
            when (currentHalf) {
                HALF_FIRST -> endFirstHalf()
                HALF_SECOND -> endSecondHalf()
            }
        }

        dialog.show()
    }



    private fun endFirstHalf() {
        state = STATE_HALFTIME
        currentHalf = HALF_BREAK
        firstHalfStoppage = stoppageTime

        statusLabel.text = "☕ 中场休息"

        // ⭐⭐⭐ 显示上半场结束时的比赛时间（不加补时）
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
            HALF_FIRST -> mainTime  // 上半场：从0开始
            HALF_SECOND -> mainTime  // 下半场：从45开始（实际显示45:00, 45:01...）
            HALF_BREAK -> mainTime + firstHalfStoppage  // 中场：显示上半场总时间
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

        statusLabel.text = "🏆 比赛结束"
        mainTimeLabel.setTextColor(0xFF888888.toInt())
        updateButtonStyle("restart")

        // ⭐⭐⭐ 显示下半场结束时的比赛时间（不加补时）
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

        // 显示比赛总结
        showMatchSummary()

        Log.i("FootballTimer", "📢 比赛结束！总补时: $totalStr")

    }


    private fun resetMatch() {
        // 重置状态变量
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

        // 更新UI
        statusLabel.text = "⚽ 上半场"
        statusLabel.setTextColor(getColor(R.color.timer_normal))
        mainTimeLabel.text = "00:00"
        mainTimeLabel.setTextColor(getColor(R.color.timer_normal))
        stoppageTimeLabel.text = "00:00"
        updateButtonStyle("start")
        updateStoppageDisplay(active = false)


        // 隐藏结束按钮
        endHalfButton.visibility = View.GONE

        // ⭐⭐⭐ 重要：这里不要停止计时器！让它保持在ready状态 ⭐⭐⭐
        // (计时器会每秒检查一次，但不会增加时间，因为state不是RUNNING)

        Log.i("FootballTimer", "📢 比赛已重置")
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

            // ⭐⭐⭐ 调试日志，查看状态 ⭐⭐⭐
            Log.d("计时器",
                "状态: $state, " +
                        "半场: $currentHalf, " +
                        "mainTime: ${formatTime(mainTime)}, " +
                        "lastUpdateTime存在: ${lastUpdateTime > 0}"
            )

            when (state) {
                STATE_RUNNING -> {
                    // 主计时器运行中 - 增加比赛时间（上半场和下半场都一样）
                    mainTime++  // 每秒加1秒

                    // ⭐⭐⭐ 这里有一个重要区别：要实时更新显示 ⭐⭐⭐
                    runOnUiThread {
                        mainTimeLabel.text = formatTime(mainTime)
                    }

                    // 检查关键时间点
                    checkTimeAlerts()
                }
                STATE_PAUSED -> {
                    // 暂停中 - 增加补时时间
                    stoppageTime++  // 每秒加1秒

                    // 更新补时显示
                    runOnUiThread {
                        updateStoppageTimeDisplay()
                    }
                }
            }

            // 更新时间基准
            lastUpdateTime = currentTime

            // ⭐⭐⭐ 确保更新按钮状态 ⭐⭐⭐
            runOnUiThread {
                updateEndHalfButton()
            }

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
                    statusLabel.text = "⚠️ 上半场补时"
                }
            }
            HALF_SECOND -> {
                // ⭐⭐⭐ 修改：下半场判断应该是从半场时间到两倍半场时间 ⭐⭐⭐
                // 比如半场45分钟：上半场0-45，下半场45-90
                // 判断：mainTime >= (halfTimeSeconds * 1.5)？不，应该是 mainTime >= (halfTimeSeconds * 2)
                // 因为下半场从 halfTimeSeconds 开始计时，到 halfTimeSeconds*2 结束

                val targetTime = halfTimeSeconds * 2  // 比如45*2=90分钟
                if (mainTime >= targetTime && !fullTimeAlertShown) {
                    fullTimeAlertShown = true
                    triggerAlert("${halfTimeMin * 2}分钟", "准备结束比赛")
                    mainTimeLabel.setTextColor(getColor(R.color.timer_danger))
                    statusLabel.text = "⚠️ 下半场补时"

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

        // 可以在这里添加手表震动
        // vibrateWatch()
    }

    // ==================== 事件弹窗 ====================

    private fun pauseTimer() {
        state = STATE_PAUSED
        lastUpdateTime = System.currentTimeMillis()

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
            showTeamSelectionDialog("黄牌")
        }

        // 红牌 - 需要选择队伍和号码
        dialogView.findViewById<View>(R.id.btnRed).setOnClickListener {
            dialog.dismiss()
            showTeamSelectionDialog("红牌")
        }

        // 进球 - 需要选择队伍和号码
        dialogView.findViewById<View>(R.id.btnGoal).setOnClickListener {
            dialog.dismiss()
            showTeamSelectionDialog("进球")
        }

        // 伤停 - 直接记录（不需要选择队伍和号码）
        dialogView.findViewById<View>(R.id.btnInjury).setOnClickListener {
            dialog.dismiss()
            recordSimpleEvent("伤停", "🏥", 30)
        }

        // 换人 - 直接记录（不需要选择队伍和号码）
        dialogView.findViewById<View>(R.id.btnSubstitution).setOnClickListener {
            dialog.dismiss()
            recordSimpleEvent("换人", "🔄", 30)
        }

        // 取消
        dialogView.findViewById<View>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun recordSimpleEvent(eventType: String, emoji: String, stoppageSeconds: Int) {
        val timeStr = formatTime(mainTime)
        val halfName = if (currentHalf == HALF_FIRST) "上半场" else "下半场"
        val minute = (mainTime / 60).toInt()

        matchEvents.add(MatchEvent(
            timeStr = timeStr,
            event = eventType,
            emoji = emoji,
            detail = "",
            half = halfName,
            minute = minute
        ))

        if (state == STATE_PAUSED) {
            stoppageTime += stoppageSeconds
        }

        updateStoppageTimeDisplay()
        addLog("$emoji [$timeStr] $eventType (+${stoppageSeconds}秒)")
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
    }


    private fun showMatchSummary() {
        val halfTimeMin = halfTimeSeconds / 60

        // 统计信息
        val goalCount = matchEvents.count { it.event == "进球" }
        val yellowCount = matchEvents.count { it.event == "黄牌" }
        val redCount = matchEvents.count { it.event == "红牌" }
        val subCount = matchEvents.count { it.event == "换人" }
        val injuryCount = matchEvents.count { it.event == "伤停" }

        // 补时统计
        val firstHalfStr = formatTime(firstHalfStoppage)
        val secondHalfStr = formatTime(stoppageTime)
        val totalStoppage = firstHalfStoppage + stoppageTime
        val totalStr = formatTime(totalStoppage)

        // 构建事件记录文本
        // 构建事件记录文本
        val eventsText = if (matchEvents.isNotEmpty()) {
            matchEvents.joinToString("\n") { event ->
                if (event.detail.isNotEmpty()) {
                    "  ${event.emoji} ${event.timeStr} [${event.half}] ${event.event} - ${event.detail}"
                } else {
                    "  ${event.emoji} ${event.timeStr} [${event.half}] ${event.event}"
                }
            }
        } else {
            "  本场比赛没有记录任何事件"
        }


        val summaryText = """
═══════════════════════
📊 比赛统计
═══════════════════════
比赛设置：每半场 $halfTimeMin 分钟

⚽ 进球: $goalCount
🟨 黄牌: $yellowCount
🟥 红牌: $redCount
🔄 换人: $subCount
🏥 伤停: $injuryCount

═══════════════════════
⏱ 补时统计
═══════════════════════
上半场补时: $firstHalfStr
下半场补时: $secondHalfStr
总补时: $totalStr

═══════════════════════
📋 事件记录
═══════════════════════
$eventsText
    """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("🏆 比赛结束")
            .setMessage(summaryText)
            .setPositiveButton("确  定") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .create()
            .show()
    }



    // ==================== UI 更新方法 ====================

    private fun updateButtonStyle(mode: String) {
        when (mode) {
            "start" -> {
                mainButton.text = "▶ 继续"
                mainButton.setBackgroundColor(0xFF2E7D32.toInt())
            }
            "pause" -> {
                mainButton.text = "⏸ 暂停"
                mainButton.setBackgroundColor(0xFFC62828.toInt())
            }
            "halftime" -> {
                mainButton.text = "▶ 下半场"
                mainButton.setBackgroundColor(0xFF2E7D32.toInt())
            }
            "restart" -> {
                mainButton.text = "🔄 重新开始"
                mainButton.setBackgroundColor(0xFF424242.toInt())
            }
        }
    }





    private fun updateStoppageDisplay(active: Boolean) {
        // 这个方法主要控制文字颜色，实际计时状态由updateStoppageTimeDisplay()控制
        val color = if (active) 0xFFFF6600.toInt() else 0xFF666666.toInt()
        stoppageTitleLabel.setTextColor(color)

        // ⭐⭐⭐ 重要：也要更新补时显示，确保颜色同步 ⭐⭐⭐
        updateStoppageTimeDisplay()
    }



    private fun updateStoppageTimeDisplay() {
        stoppageTimeLabel.text = formatTime(stoppageTime)

        val color = if (state == STATE_PAUSED) {
            getColor(R.color.timer_warning)
        } else {
            getColor(R.color.timer_inactive)
        }

        stoppageTimeLabel.setTextColor(color)
        stoppageTitleLabel.setTextColor(color)
    }



    private fun updateEndHalfButton() {
        // 只有在比赛进行中或暂停时才显示结束按钮
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

        val eventsList = matchEvents.map { event ->
            if (event.detail.isNotEmpty()) {
                "${event.emoji} ${event.timeStr} ${event.event} - ${event.detail}"
            } else {
                "${event.emoji} ${event.timeStr} ${event.event}"
            }
        }

        // 统计主客队进球
        val homeGoals = matchEvents.count { it.event == "进球" && it.detail.contains("主队") }
        val awayGoals = matchEvents.count { it.event == "进球" && it.detail.contains("客队") }

        val record = MatchRecord(
            date = currentDate,
            halfTimeMinutes = (halfTimeSeconds / 60).toInt(),
            firstHalfStoppage = formatTime(firstHalfStoppage),
            secondHalfStoppage = formatTime(stoppageTime),
            totalStoppage = formatTime(firstHalfStoppage + stoppageTime),
            goalCount = matchEvents.count { it.event == "进球" },
            yellowCount = matchEvents.count { it.event == "黄牌" },
            redCount = matchEvents.count { it.event == "红牌" },
            substitutionCount = matchEvents.count { it.event == "换人" },
            injuryCount = matchEvents.count { it.event == "伤停" },
            events = eventsList,
            homeGoals = homeGoals,
            awayGoals = awayGoals
        )

        recordManager.saveRecord(record)
        Log.i("FootballTimer", "📁 比赛记录已保存: 主队 $homeGoals - $awayGoals 客队")
    }


    private fun showHistoryDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_history, null)

        val recordsContainer = dialogView.findViewById<LinearLayout>(R.id.recordsContainer)
        val tvNoRecords = dialogView.findViewById<TextView>(R.id.tvNoRecords)
        val btnClearHistory = dialogView.findViewById<Button>(R.id.btnClearHistory)
        val btnCloseHistory = dialogView.findViewById<Button>(R.id.btnCloseHistory)

        val records = recordManager.getAllRecords()

        if (records.isEmpty()) {
            tvNoRecords.visibility = View.VISIBLE
            recordsContainer.visibility = View.GONE
        } else {
            tvNoRecords.visibility = View.GONE
            recordsContainer.visibility = View.VISIBLE

            // 动态添加记录项
            records.forEach { record ->
                val itemView = LayoutInflater.from(this).inflate(R.layout.item_match_record, recordsContainer, false)

                itemView.findViewById<TextView>(R.id.tvRecordDate).text = record.date
                itemView.findViewById<TextView>(R.id.tvRecordDuration).text = "${record.halfTimeMinutes}分钟/半场"
                itemView.findViewById<TextView>(R.id.tvRecordStoppage).text =
                    "补时: 上 ${record.firstHalfStoppage} | 下 ${record.secondHalfStoppage}"
                itemView.findViewById<TextView>(R.id.tvRecordEvents).text =
                    "⚽${record.goalCount} 🟨${record.yellowCount} 🟥${record.redCount} 🔄${record.substitutionCount} 🏥${record.injuryCount}"

                // 点击查看详情
                itemView.setOnClickListener {
                    showRecordDetail(record)
                }

                recordsContainer.addView(itemView)
            }
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        // 清空按钮
        btnClearHistory.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("确认清空")
                .setMessage("确定要清空所有历史记录吗？")
                .setPositiveButton("✓") { _, _ ->
                    recordManager.clearAllRecords()
                    dialog.dismiss()
                    Toast.makeText(this, "历史记录已清空", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("✗", null)
                .show()
        }

        // 关闭按钮
        btnCloseHistory.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    // 显示队伍选择弹窗
    private fun showTeamSelectionDialog(eventType: String) {
        pendingEventType = eventType

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_team_selection, null)

        val tvTitle = dialogView.findViewById<TextView>(R.id.tvTeamSelectionTitle)
        val btnHomeTeam = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnHomeTeam)
        val btnAwayTeam = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnAwayTeam)
        val btnCancel = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancelTeam)

        // 根据事件类型设置标题
        val eventEmoji = when (eventType) {
            "黄牌" -> "🟨"
            "红牌" -> "🟥"
            "进球" -> "⚽"
            else -> ""
        }
        tvTitle.text = "$eventEmoji $eventType - 选择队伍"

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        btnHomeTeam.setOnClickListener {
            selectedTeam = "主队"
            dialog.dismiss()
            showNumberSelectionDialog(eventType, selectedTeam)
        }

        btnAwayTeam.setOnClickListener {
            selectedTeam = "客队"
            dialog.dismiss()
            showNumberSelectionDialog(eventType, selectedTeam)
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
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

        // 设置标题
        val eventEmoji = when (eventType) {
            "黄牌" -> "🟨"
            "红牌" -> "🟥"
            "进球" -> "⚽"
            else -> ""
        }
        tvTitle.text = "$eventEmoji $eventType"

        // 设置队伍信息颜色
        tvTeamInfo.text = team
        tvTeamInfo.setTextColor(if (team == "主队") 0xFF1565C0.toInt() else 0xFFC62828.toInt())

        // 设置十位数滚轮 (0-9)
        pickerTens.minValue = 0
        pickerTens.maxValue = 9
        pickerTens.value = 0
        pickerTens.wrapSelectorWheel = true

        // 设置个位数滚轮 (0-9)
        pickerOnes.minValue = 0
        pickerOnes.maxValue = 9
        pickerOnes.value = 1
        pickerOnes.wrapSelectorWheel = true

        // 更新显示的号码
        fun updateSelectedNumber() {
            val number = pickerTens.value * 10 + pickerOnes.value
            tvSelectedNumber.text = "# ${String.format("%02d", number)}"
        }

        updateSelectedNumber()

        pickerTens.setOnValueChangedListener { _, _, _ -> updateSelectedNumber() }
        pickerOnes.setOnValueChangedListener { _, _, _ -> updateSelectedNumber() }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnConfirm.setOnClickListener {
            val number = pickerTens.value * 10 + pickerOnes.value
            val numberStr = String.format("%02d", number)
            dialog.dismiss()

            // 记录事件
            recordEventWithDetails(eventType, team, numberStr)
        }

        dialog.show()
    }

    // 记录带详细信息的事件
    private fun recordEventWithDetails(eventType: String, team: String, number: String) {
        val emoji = when (eventType) {
            "黄牌" -> "🟨"
            "红牌" -> "🟥"
            "进球" -> "⚽"
            else -> "📝"
        }

        val teamEmoji = if (team == "主队") "🏠" else "✈️"
        val detailText = "$team #$number"
        val timeStr = formatTime(mainTime)
        val halfName = if (currentHalf == HALF_FIRST) "上半场" else "下半场"
        val minute = (mainTime / 60).toInt()

        matchEvents.add(MatchEvent(
            timeStr = timeStr,
            event = eventType,
            emoji = emoji,
            detail = detailText,
            half = halfName,
            minute = minute
        ))

        // ✂️ --- 我把那段自动加秒的代码删掉了 ---

        updateStoppageTimeDisplay()
        // 删掉日志里的 (+60秒) 字样
        addLog("$emoji [$timeStr] $eventType - $teamEmoji $detailText")
    }




    private fun showRecordDetail(record: MatchRecord) {
        val eventsText = if (record.events.isNotEmpty()) {
            record.events.joinToString("\n")
        } else {
            "无事件记录"
        }

        val detailText = """
日期: ${record.date}
时长: ${record.halfTimeMinutes}分钟/半场

═══ 补时统计 ═══
上半场: ${record.firstHalfStoppage}
下半场: ${record.secondHalfStoppage}
总计: ${record.totalStoppage}

═══ 事件统计 ═══
⚽ 进球: ${record.goalCount}
🟨 黄牌: ${record.yellowCount}
🟥 红牌: ${record.redCount}
🔄 换人: ${record.substitutionCount}
🏥 伤停: ${record.injuryCount}

═══ 事件记录 ═══
$eventsText
    """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("📋 比赛详情")
            .setMessage(detailText)
            .setPositiveButton("关闭", null)
            .show()
    }


    // ==================== 数据类 ====================

    data class MatchEvent(
        val timeStr: String,
        val event: String,
        val emoji: String = "",
        val detail: String = "",
        val half: String = "",
        val minute: Int = 0
    )


    data class EventItem(
        val displayText: String,
        val type: String,
        val color: String
    )

}
