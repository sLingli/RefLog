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
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    private lateinit var stateIndicator: TextView
    private lateinit var mainTimeLabel: TextView
    private lateinit var stoppageTitleLabel: TextView
    private lateinit var stoppageTimeLabel: TextView
    private lateinit var mainButton: Button
    private lateinit var endHalfButton: Button
    private lateinit var logText: TextView

    // ==================== 状态变量 ====================
    private var state: String = STATE_READY
    private var currentHalf: String = HALF_FIRST

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
        stateIndicator = findViewById(R.id.stateIndicator)
        mainTimeLabel = findViewById(R.id.mainTimeLabel)
        stoppageTitleLabel = findViewById(R.id.stoppageTitleLabel)
        stoppageTimeLabel = findViewById(R.id.stoppageTimeLabel)
        mainButton = findViewById(R.id.mainButton)
        endHalfButton = findViewById(R.id.endHalfButton)
        logText = findViewById(R.id.logText)

        // 设置按钮点击事件
        mainButton.setOnClickListener { toggleTimer() }
        endHalfButton.setOnClickListener { endHalf() }
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
        updateStateIndicator("比赛进行中")

        addLog("🏁 比赛开始")
        val halfTimeMin = halfTimeSeconds / 60
        Log.i("FootballTimer", "📢 比赛开始！每半场 $halfTimeMin 分钟")
    }



    private fun resumeTimer() {
        state = STATE_RUNNING
        lastUpdateTime = System.currentTimeMillis()  // ✅ 重置时间基准

        updateButtonStyle("pause")
        updateStoppageDisplay(active = false)
        updateStateIndicator("比赛进行中")
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
        updateStateIndicator("比赛进行中")
        updateStoppageTimeDisplay()

        // ⭐⭐⭐ 确保计时器运行 ⭐⭐⭐
        startUpdateLoop()

        addLog("🏁 下半场开始 - 从 ${formatTime(mainTime)} 继续计时")
        Log.i("FootballTimer", "📢 下半场开始！从 ${formatTime(mainTime)} 计时")
    }



    private fun endHalf() {
        if (state == STATE_READY) return

        when (currentHalf) {
            HALF_FIRST -> endFirstHalf()
            HALF_SECOND -> endSecondHalf()
        }
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
        updateStateIndicator("等待下半场开始")

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

        // ❌ 不要停止计时器！否则下半场无法启动
        // handler.removeCallbacks(updateRunnable)
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
        updateStateIndicator("点击重新开始")

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
        statusLabel.setTextColor(0xFF00FF00.toInt())
        mainTimeLabel.text = "00:00"
        mainTimeLabel.setTextColor(0xFF00FF00.toInt())
        stoppageTimeLabel.text = "00:00"
        updateButtonStyle("start")
        updateStoppageDisplay(active = false)
        updateStateIndicator("准备开始 - 点击开始设置时间")

        // 清空日志
        logText.text = ""

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
                    mainTimeLabel.setTextColor(0xFFFF6600.toInt())
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
                    mainTimeLabel.setTextColor(0xFFFF0000.toInt())
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
        updateStateIndicator("⏸ 暂停中（补时计时）")

        // 显示事件选择弹窗
        showEventDialog()
    }

    private fun showEventDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_event_selection, null)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        // 设置按钮点击事件
        dialogView.findViewById<Button>(R.id.btnGoal).setOnClickListener {
            logEvent("进球")
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.btnYellow).setOnClickListener {
            logEvent("黄牌")
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.btnRed).setOnClickListener {
            logEvent("红牌")
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.btnInjury).setOnClickListener {
            logEvent("伤停")
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.btnSubstitution).setOnClickListener {
            logEvent("换人")
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
            // 取消后不记录事件
        }

        dialog.show()
    }


    private fun logEvent(eventType: String) {
        // 根据事件类型选择emoji
        val emojiMap = mapOf(
            "进球" to "⚽",
            "黄牌" to "🟨",
            "红牌" to "🟥",
            "伤停" to "🏥",
            "换人" to "🔄"
        )
        val emoji = emojiMap[eventType] ?: "📝"

        // 计算当前是第几分钟
        val minute = (mainTime / 60).toInt()
        val currentTimeStr = formatTime(mainTime)

        // 确定当前是上半场还是下半场
        val halfName = if (currentHalf == HALF_FIRST) "上半场" else "下半场"

        // 保存事件记录
        matchEvents.add(MatchEvent(
            minute = minute,
            timeStr = currentTimeStr,
            half = halfName,
            event = eventType,
            emoji = emoji
        ))

        // 添加到日志显示
        addLog("$emoji $eventType")
    }

    private fun showTimeSettingDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_time_setting, null)
        val timeEditText = dialogView.findViewById<EditText>(R.id.timeEditText)

        AlertDialog.Builder(this)
            .setTitle("⚙️ 设置比赛时间")
            .setView(dialogView)
            .setMessage("每半场时间（分钟）\n常用：45（正式）/ 20（友谊）/ 5（测试）")
            .setPositiveButton("✓ 开始比赛") { _, _ ->
                try {
                    val halfTimeInput = timeEditText.text.toString().toInt()
                    var halfTime = halfTimeInput

                    // 验证输入范围
                    if (halfTime < 1) halfTime = 1
                    if (halfTime > 60) halfTime = 60

                    // 设置比赛时间
                    halfTimeSeconds = halfTime * 60L
                    matchTimeSet = true

                    updateStateIndicator("每半场 $halfTime 分钟")

                    // 继续开始比赛
                    startTimer()

                } catch (e: NumberFormatException) {
                    Toast.makeText(this, "请输入有效的数字", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("✕ 取消", null)
            .create()
            .show()
    }

    private fun showMatchSummary() {
        val halfTimeMin = halfTimeSeconds / 60

        // 统计信息
        val goalCount = matchEvents.count { it.event == "进球" }  // ⭐ 添加这一行
        val yellowCount = matchEvents.count { it.event == "黄牌" }
        val redCount = matchEvents.count { it.event == "红牌" }
        val subCount = matchEvents.count { it.event == "换人" }
        val injuryCount = matchEvents.count { it.event == "伤停" }
        val statsText = """
        比赛设置：每半场 $halfTimeMin 分钟
    
        ⚽ 进球: $goalCount
        🟨 黄牌: $yellowCount
        🟥 红牌: $redCount
        🔄 换人: $subCount
        🏥 伤停: $injuryCount
        """.trimIndent()


        // 事件列表
        val eventsText = if (matchEvents.isNotEmpty()) {
            buildString {
                append("事件记录（按时间顺序）：\n\n")
                matchEvents.forEach { event ->
                    append("  ${event.emoji} 第 ${event.minute}' [${event.half}] ${event.event}\n")
                }
            }
        } else {
            "本场比赛没有记录任何事件"
        }

        AlertDialog.Builder(this)
            .setTitle("📊 比赛事件总结")
            .setMessage("$statsText\n\n$eventsText")
            .setPositiveButton("确  定") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    // ==================== UI 更新方法 ====================

    private fun updateButtonStyle(mode: String) {
        val styles = mapOf(
            "start" to Triple("▶  继  续", 0xFF006400.toInt(), "green_button"),
            "pause" to Triple("⏸  暂  停", 0xFF8B0000.toInt(), "red_button"),
            "halftime" to Triple("▶ 开始下半场", 0xFF006400.toInt(), "green_button"),
            "restart" to Triple("🔄 重新开始", 0xFF444444.toInt(), "gray_button")
        )

        val (text, color, _) = styles[mode] ?: styles["start"]!!
        mainButton.text = text
        mainButton.setBackgroundColor(color)
    }

    private fun updateStoppageDisplay(active: Boolean) {
        // 这个方法主要控制文字颜色，实际计时状态由updateStoppageTimeDisplay()控制
        val color = if (active) 0xFFFF6600.toInt() else 0xFF666666.toInt()
        stoppageTitleLabel.setTextColor(color)

        // ⭐⭐⭐ 重要：也要更新补时显示，确保颜色同步 ⭐⭐⭐
        updateStoppageTimeDisplay()
    }


    private fun updateStateIndicator(text: String) {
        stateIndicator.text = text
    }

    private fun updateStoppageTimeDisplay() {
        // 显示当前的补时时间
        stoppageTimeLabel.text = formatTime(stoppageTime)

        // 根据状态改变颜色
        val color = if (state == STATE_PAUSED) {
            0xFFFF6600.toInt()  // 橙色（正在计时补时）
        } else {
            0xFF666666.toInt()   // 灰色（停止计补时）
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
        // 获取当前时间和阶段
        val currentTime = formatTime(mainTime)

        // 阶段标识
        val halfIndicator = when (currentHalf) {
            HALF_FIRST -> "H1"
            HALF_SECOND -> "H2"
            else -> "--"
        }

        // 格式化日志条目
        val logEntry = "[$halfIndicator $currentTime] $message\n"

        // 添加到日志显示
        logText.append(logEntry)

        // 自动滚动到底部
        val scrollView = findViewById<android.widget.ScrollView>(R.id.logScrollView)
        scrollView.post {
            scrollView.fullScroll(android.view.View.FOCUS_DOWN)
        }
    }

    // ==================== 工具方法 ====================

    private fun formatTime(seconds: Long): String {
        val minutes = seconds / 60
        val secs = seconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, secs)
    }

    // ==================== 数据类 ====================

    data class MatchEvent(
        val minute: Int,
        val timeStr: String,
        val half: String,
        val event: String,
        val emoji: String
    )

    data class EventItem(
        val displayText: String,
        val type: String,
        val color: String
    )
}
