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

        const val HALF_FIRST = "上半场"
        const val HALF_BREAK = "中场休息"
        const val HALF_SECOND = "下半场"

        const val DEFAULT_HALF_TIME = 45  // 默认半场时间(分钟)
    }

    // ==================== UI 组件 ====================
    private lateinit var statusLabel: TextView

    private lateinit var mainTimeLabel: TextView

    private lateinit var stoppageTimeLabel: TextView
    // ✅ 修改成这样
    private lateinit var mainButton: com.google.android.material.button.MaterialButton
    private lateinit var endHalfButton: com.google.android.material.button.MaterialButton


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

    // 默认主队蓝色，客队红色
    private var homeTeamColor: Int = 0xFF1565C0.toInt()
    private var awayTeamColor: Int = 0xFFC62828.toInt()


    // ==================== 生命周期方法 ====================

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. 🔥 第一步：必须先找到所有按钮和文字控件
        initializeUI()

        // 2. 第二步：然后再去设置它们的状态（这时候控件肯定都在了）
        resetMatch()

        // 3. 第三步：最后启动计时器逻辑
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
        // 绑定 XML 里的控件 ID
        statusLabel = findViewById(R.id.statusLabel)
        mainTimeLabel = findViewById(R.id.mainTimeLabel)

        // 🔥 重点检查这里：确保这一行存在且正确！
        stoppageTimeLabel = findViewById(R.id.stoppageTimeLabel)

        // 绑定按钮
        mainButton = findViewById(R.id.mainButton)
        endHalfButton = findViewById(R.id.endHalfButton)
        btnHistory = findViewById(R.id.btnHistory)

        // 设置点击事件
        mainButton.setOnClickListener { toggleTimer() }
        endHalfButton.setOnClickListener { endHalf() }
        btnHistory.setOnClickListener { showHistoryDialog() }

        // 初始化其他逻辑
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
                // 这里会调用 startTimer() -> updateButtonStyle("pause")
                // 界面从【单按钮】分裂为【双按钮】
                startTimer()
            }
            STATE_RUNNING -> {
                Log.d("状态机", "从RUNNING暂停 -> 保持双按钮")
                // 暂停计时
                pauseTimer()
                // 界面从【红暂停】切为【绿继续】，右边结束按钮保持不动
                updateButtonStyle("resume")
            }
            STATE_PAUSED -> {
                Log.d("状态机", "从PAUSED继续 -> 保持双按钮")
                // 恢复计时
                resumeTimer()
                // 界面从【绿继续】切为【红暂停】，右边结束按钮保持不动
                updateButtonStyle("pause")
            }
            STATE_HALFTIME -> {
                Log.d("状态机", "从中场休息开始下半场 -> 触发分裂动画")
                // 这里也需要类似的逻辑：开始下半场 -> 分裂为双按钮
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

        // 🔥 状态栏更新：这里会自动变成 "上半场 + 足球图标"
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
        lastUpdateTime = System.currentTimeMillis()  // ✅ 重置时间基准

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

        // 🔥 状态栏更新：这里会自动变成 "下半场 + 足球图标"
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



    private fun endHalf() {
        if (state == STATE_READY) return

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_confirm, null)
        val btnNo = dialogView.findViewById<Button>(R.id.btnNo)
        val btnYes = dialogView.findViewById<Button>(R.id.btnYes)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        btnNo.setOnClickListener { dialog.dismiss() }

        btnYes.setOnClickListener {
            dialog.dismiss()
            when (currentHalf) {
                HALF_FIRST -> {
                    endFirstHalf()
                    // 🔥 结束上半场后，状态变为中场休息，这里刷新一下图标 (变成咖啡)
                    updateStatusLabel()
                }
                HALF_SECOND -> {
                    endSecondHalf()
                    // 🔥 结束下半场后，状态变为比赛结束，这里刷新一下图标 (变成奖杯)
                    updateStatusLabel()
                }
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

        // 🔥 状态栏更新：这里会自动变成 "准备开始 + 足球图标"
        updateStatusLabel()

        // 注意：原本的 statusLabel.setTextColor(getColor(R.color.timer_normal)) 可以删了
        // 因为 updateStatusLabel 里已经会自动把图标染成和文字一样的颜色 (通常是绿色)

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
            detail = "", // 简单事件没有详情
            half = halfName,
            minute = minute
        ))

        // 🔥 删掉了 if (state == STATE_PAUSED) { stoppageTime += stoppageSeconds }
        // 🔥 删掉了 updateStoppageTimeDisplay()，因为时间没变不需要更新

        // 修改日志，删掉 (+秒)
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
        tvTitle.text = if (isHistory) "历史详情" else "比赛总结"

        // 2. 🔥 填充统计数据 (这部分是新修改的)

        // 2.1 算比分：主队进球 vs 客队进球
        val homeGoals = eventsToShow.count { it.event == "进球" && it.detail.contains("主队") }
        val awayGoals = eventsToShow.count { it.event == "进球" && it.detail.contains("客队") }

        tvStatMatchTime.text = "时长: 每半场 ${hTime}分"
        // 新格式： 比分: 3 : 2
        tvStatGoals.text = "比分: $homeGoals : $awayGoals"

        tvStatYellow.text = "黄牌: ${eventsToShow.count { it.event == "黄牌" }}"
        tvStatRed.text = "红牌: ${eventsToShow.count { it.event == "红牌" }}"

        // 2.2 算补时：一行显示两个
        // 新格式： 补时: 上 02:00 | 下 03:00
        tvStatStoppage.text = "补时: 上 ${formatTime(st1)} | 下 ${formatTime(st2)}"

        // 3. 填充事件明细 (使用 LinearLayout 容器法，确保图标贴着文字居中)
        listEvents.removeAllViews()
        if (eventsToShow.isEmpty()) {
            val tv = TextView(this)
            tv.text = "暂无事件记录"
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
                    "进球" -> R.drawable.sports_soccer
                    "黄牌", "红牌" -> R.drawable.ic_card
                    "换人" -> R.drawable.ic_substitute
                    "伤停" -> R.drawable.ic_medical
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
                        "进球" -> android.graphics.Color.WHITE
                        "黄牌" -> android.graphics.Color.YELLOW
                        "红牌" -> android.graphics.Color.RED
                        "伤停" -> android.graphics.Color.parseColor("#2196F3")
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

        val dialog = AlertDialog.Builder(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
            .setView(dialogView)
            .create()

        btnClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    // 辅助函数：dp转px
    fun Int.dpToPx(): Int = (this * Resources.getSystem().displayMetrics.density).toInt()



    // ==================== UI 更新方法 ====================

    private fun updateButtonStyle(mode: String) {

        val btnMain = findViewById<com.google.android.material.button.MaterialButton>(R.id.mainButton)
        val btnEnd = findViewById<com.google.android.material.button.MaterialButton>(R.id.endHalfButton)

        // 🔥 动画魔法：让按钮分裂/合并时丝滑过渡
        TransitionManager.beginDelayedTransition(findViewById(android.R.id.content), AutoTransition())

        when (mode) {
            "start" -> {
                // 🟩 初始状态：单按钮 (开始半场)
                mainButton.text = "开始"
                mainButton.setIconResource(R.drawable.baseline_play_arrow_24)
                mainButton.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF2E7D32.toInt()) // 绿

                mainButton.visibility = View.VISIBLE
                endHalfButton.visibility = View.GONE
            }

            "pause" -> {
                // 🟥 比赛进行中状态：双按钮 (显示暂停 + 结束)
                mainButton.text = "暂停"
                mainButton.setIconResource(R.drawable.pause_circle)
                mainButton.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFFC62828.toInt()) // 红

                // 确保结束按钮正确显示
                endHalfButton.text = "结束"
                endHalfButton.setIconResource(R.drawable.stop_circle)

                mainButton.visibility = View.VISIBLE
                endHalfButton.visibility = View.VISIBLE
            }

            "resume" -> {
                // 🟩 比赛暂停中状态：双按钮 (显示继续 + 结束)
                mainButton.text = "继续"
                mainButton.setIconResource(R.drawable.baseline_play_arrow_24)
                mainButton.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF2E7D32.toInt()) // 绿

                // 确保结束按钮保持显示
                endHalfButton.text = "结束"
                endHalfButton.setIconResource(R.drawable.stop_circle)

                mainButton.visibility = View.VISIBLE
                endHalfButton.visibility = View.VISIBLE
            }

            "halftime" -> {
                // 🟩 中场休息状态：单按钮 (下半场)
                mainButton.text = "下半场"
                mainButton.setIconResource(R.drawable.baseline_play_arrow_24)
                mainButton.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFF2E7D32.toInt()) // 绿

                mainButton.visibility = View.VISIBLE
                endHalfButton.visibility = View.GONE
            }
        }
    }


    // 放在 updateButtonStyle 附近就行
    private fun updateStatusLabel() {
        var textStr = ""
        var iconRes = 0

        if (state == STATE_READY) {
            textStr = "准备开始"
            iconRes = R.drawable.sports_soccer
        } else if (state == STATE_RUNNING || state == STATE_PAUSED) {
            // 🔥 修复点：直接使用 currentHalf 的值（它本身就是 "上半场" 或 "下半场"）
            // 不需要再判断 if (currentHalf == 1) 了
            textStr = currentHalf
            iconRes = R.drawable.sports_soccer
        } else if (state == STATE_HALFTIME) {
            textStr = "中场休息"
            iconRes = R.drawable.ic_coffee
        } else if (state == STATE_FINISHED) {
            textStr = "比赛结束"
            iconRes = R.drawable.ic_trophy
        }

        // 更新 UI
        statusLabel.text = textStr

        // 只有当有图标资源时才设置
        if (iconRes != 0) {
            statusLabel.setCompoundDrawablesWithIntrinsicBounds(iconRes, 0, 0, 0)
        }

        // 染色
        statusLabel.compoundDrawableTintList = statusLabel.textColors
    }





    private fun updateStoppageDisplay(active: Boolean) {
        // 1. 确定颜色：激活是亮橙色(0xFFFF6600)，停止是暗灰色(0xFF666666)
        val color = if (active) 0xFFFF6600.toInt() else 0xFF666666.toInt()

        // 2. 同时改变文字颜色和图标颜色
        // 因为现在图标是 stoppageTimeLabel 的 drawableStart，所以直接操作这就行
        stoppageTimeLabel.setTextColor(color)
        stoppageTimeLabel.compoundDrawableTintList = android.content.res.ColorStateList.valueOf(color)

        // 3. 刷新一下时间数字
        updateStoppageTimeDisplay()
    }



    private fun updateStoppageTimeDisplay() {
        // 只做一件事：把最新的毫秒数格式化成 00:00 显示出来
        stoppageTimeLabel.text = formatTime(stoppageTime)
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

        // ✂️ 删掉了之前那个 eventsList 的 map 转换逻辑，因为我们不需要 String 了

        // 统计主客队进球
        val homeGoals = matchEvents.count { it.event == "进球" && it.detail.contains("主队") }
        val awayGoals = matchEvents.count { it.event == "进球" && it.detail.contains("客队") }

        val record = MatchRecord(
            date = currentDate,
            halfTimeMinutes = (halfTimeSeconds / 60).toInt(),
            firstHalfStoppage = formatTime(firstHalfStoppage.toLong()),
            secondHalfStoppage = formatTime(stoppageTime.toLong()),
            totalStoppage = formatTime((firstHalfStoppage + stoppageTime).toLong()),
            goalCount = matchEvents.count { it.event == "进球" },
            yellowCount = matchEvents.count { it.event == "黄牌" },
            redCount = matchEvents.count { it.event == "红牌" },
            substitutionCount = matchEvents.count { it.event == "换人" },
            injuryCount = matchEvents.count { it.event == "伤停" },

            // 🔥【核心修改】：直接把原始的对象列表存进去！
            events = matchEvents.toList(), // 使用 .toList() 复制一份，防止后续改动影响历史记录

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

            records.forEach { record ->
                val itemView = android.view.LayoutInflater.from(this).inflate(R.layout.item_match_record, recordsContainer, false) as android.view.ViewGroup

                itemView.findViewById<android.widget.TextView>(R.id.tvRecordDate).text = record.date
                itemView.findViewById<android.widget.TextView>(R.id.tvRecordDuration).text = "${record.halfTimeMinutes}分钟/半场"
                itemView.findViewById<android.widget.TextView>(R.id.tvRecordStoppage).text =
                    "补时: 上 ${record.firstHalfStoppage} | 下 ${record.secondHalfStoppage}"

                // 1. 隐藏旧的 Emoji 文本
                val oldTv = itemView.findViewById<android.widget.TextView>(R.id.tvRecordEvents)
                oldTv.visibility = android.view.View.GONE

                // 2. 创建矢量图标行
                val statsLayout = android.widget.LinearLayout(this)
                statsLayout.orientation = android.widget.LinearLayout.HORIZONTAL
                statsLayout.gravity = android.view.Gravity.CENTER_VERTICAL
                statsLayout.setPadding(0, (8 * resources.displayMetrics.density).toInt(), 0, 0)

                // 3. 定义内部函数：添加图标项
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

                // 添加各项数据 (确保你的 R.drawable 里有这些图标)
                addStat(R.drawable.sports_soccer, record.goalCount, android.graphics.Color.WHITE)
                addStat(R.drawable.ic_card, record.yellowCount, android.graphics.Color.YELLOW)
                addStat(R.drawable.ic_card, record.redCount, android.graphics.Color.RED)
                addStat(R.drawable.ic_substitute, record.substitutionCount, android.graphics.Color.GREEN)
                addStat(R.drawable.ic_medical, record.injuryCount, android.graphics.Color.parseColor("#2196F3"))

                itemView.addView(statsLayout)

                itemView.setOnClickListener {
                    showMatchSummary(isHistory = true, historyRecord = record)
                }

                recordsContainer.addView(itemView)
            }
        }

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        // 清空按钮确认
        btnClearHistory.setOnClickListener {
            val confirmView = android.view.LayoutInflater.from(this).inflate(R.layout.dialog_confirm, null)
            val confirmDialog = androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(confirmView)
                .create()

            confirmView.findViewById<android.view.View>(R.id.btnNo).setOnClickListener { confirmDialog.dismiss() }
            confirmView.findViewById<android.view.View>(R.id.btnYes).setOnClickListener {
                recordManager.clearAllRecords()
                confirmDialog.dismiss()
                dialog.dismiss()
                android.widget.Toast.makeText(this, "历史记录已清空", android.widget.Toast.LENGTH_SHORT).show()
            }
            confirmDialog.show()
        }

        btnCloseHistory.setOnClickListener { dialog.dismiss() }
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

        // 1. 设置标题
        // 这里的 emoji 其实在标题栏不太重要了，因为下面的按钮才是主角
        val eventEmoji = when (eventType) {
            "黄牌" -> "🟨"
            "红牌" -> "🟥"
            "进球" -> "⚽"
            else -> ""
        }
        tvTitle.text = "$eventEmoji $eventType - 选择队伍"

        // 2. 🔥🔥🔥 核心魔法：应用主客队颜色 🔥🔥🔥
        btnHomeTeam.backgroundTintList = android.content.res.ColorStateList.valueOf(homeTeamColor)
        btnAwayTeam.backgroundTintList = android.content.res.ColorStateList.valueOf(awayTeamColor)

        // 3. 智能反色逻辑：如果球衣是白色，把字和图标改成黑色
        // (0xFFFFFFFF.toInt() 就是纯白色)
        if (homeTeamColor == 0xFFFFFFFF.toInt()) {
            btnHomeTeam.setTextColor(android.graphics.Color.BLACK)
            btnHomeTeam.iconTint = android.content.res.ColorStateList.valueOf(android.graphics.Color.BLACK)
        } else {
            // 其他深色球衣，字和图标保持白色
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

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        // 4. 点击事件
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





    // ==================== 数据类 ====================




    data class EventItem(
        val displayText: String,
        val type: String,
        val color: String
    )
    // 🎬 这是一个专门控制历史记录按钮“变魔术”的函数
    private fun animateHistoryButton(show: Boolean) {
        // 1. 通过 ID 找到你的按钮
        val historyBtn = findViewById<View>(R.id.btnHistory) ?: return

        if (show) {
            // 让按钮【现身】✨
            if (historyBtn.visibility == View.VISIBLE && historyBtn.alpha == 1f) return

            historyBtn.visibility = View.VISIBLE
            historyBtn.alpha = 0f          // 先透明
            historyBtn.scaleX = 0.8f       // 先缩小
            historyBtn.scaleY = 0.8f

            historyBtn.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(400)
                .setInterpolator(android.view.animation.OvershootInterpolator()) // 弹一下，显高级
                .start()
        } else {
            // 让按钮【隐身】👻
            if (historyBtn.visibility == View.GONE) return

            historyBtn.animate()
                .alpha(0f)
                .scaleX(0.8f)
                .scaleY(0.8f)
                .setDuration(300)
                .withEndAction { historyBtn.visibility = View.GONE } // 动画播完彻底消失
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

        var tempHomeColor = colors[1] // 默认蓝
        var tempAwayColor = colors[0] // 默认红

        fun setupWheel(rv: androidx.recyclerview.widget.RecyclerView, initialIndex: Int, onSelect: (Int) -> Unit) {
            rv.layoutManager = CenterScaleLayoutManager(this)
            val adapter = ColorWheelAdapter(colors) { }
            rv.adapter = adapter

            // 📐 核心修正 1：Padding 精确计算
            // 容器高度 150dp，Item高度 60dp -> 空余 90dp -> 上下各 45dp
            val density = resources.displayMetrics.density
            val padding = (45 * density).toInt()
            rv.setPadding(0, padding, 0, padding)
            rv.clipToPadding = false

            // 🔥 终极版：带惯性 + 带回弹阻尼的吸附器
            val snapHelper = object : androidx.recyclerview.widget.LinearSnapHelper() {

                // 1. 保持之前的惯性增强（让它滚得远）
                override fun calculateScrollDistance(velocityX: Int, velocityY: Int): IntArray {
                    return super.calculateScrollDistance(velocityX, (velocityY * 1.5).toInt())
                }

                // 2. 核心魔法：重写滚动控制器，制造“回弹”效果
                override fun createScroller(layoutManager: androidx.recyclerview.widget.RecyclerView.LayoutManager?): androidx.recyclerview.widget.RecyclerView.SmoothScroller? {
                    if (layoutManager !is androidx.recyclerview.widget.RecyclerView.SmoothScroller.ScrollVectorProvider) return null

                    return object : androidx.recyclerview.widget.LinearSmoothScroller(rv.context) {

                        // A. 让“停车”的过程变慢，显得更有质感
                        override fun calculateTimeForDeceleration(dx: Int): Int {
                            // 原来的速度太快，我们让它慢一倍，营造“沉重感”
                            return super.calculateTimeForDeceleration(dx) * 2
                        }

                        // B. 加入“回弹插值器” (OvershootInterpolator)
                        override fun onTargetFound(targetView: android.view.View, state: androidx.recyclerview.widget.RecyclerView.State, action: Action) {
                            val snapDistances = calculateDistanceToFinalSnap(layoutManager, targetView)
                            val dx = snapDistances!![0]
                            val dy = snapDistances[1]

                            // 计算需要的时间
                            val time = calculateTimeForDeceleration(Math.max(Math.abs(dx), Math.abs(dy)))

                            if (time > 0) {
                                // 🔥 重点在这里：OvershootInterpolator(1.2f)
                                // 1.2f 是回弹力度，数字越大回弹越猛。建议 1.0f - 1.5f 之间
                                action.update(dx, dy, time, android.view.animation.OvershootInterpolator(3.0f))
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

            // 🎯 核心修正 2：初始定位逻辑
            // 算出中间位置，并加上 initialIndex 偏移
            val centerStart = Int.MAX_VALUE / 2
            val startPos = centerStart - (centerStart % colors.size) + initialIndex

            // 使用 scrollToPositionWithOffset(pos, 0) 让它停在 Padding 的边缘（也就是正中间）
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
    }
}
