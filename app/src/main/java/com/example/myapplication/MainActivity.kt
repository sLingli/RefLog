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

        // 1. 第一步：必须先找到所有按钮和文字控件
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
        statusLabel = findViewById(R.id.statusLabel)
        mainTimeLabel = findViewById(R.id.mainTimeLabel)
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

                    updateStatusLabel()
                }
                HALF_SECOND -> {
                    endSecondHalf()
                    updateStatusLabel()
                }
            }
        }

        dialog.show()
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
    }



    private fun endFirstHalf() {
        state = STATE_HALFTIME
        currentHalf = HALF_BREAK
        firstHalfStoppage = stoppageTime

        statusLabel.text = "☕ 中场休息"


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
                    statusLabel.text = "上半场补时"
                }
            }
            HALF_SECOND -> {


                val targetTime = halfTimeSeconds * 2  // 比如45*2=90分钟
                if (mainTime >= targetTime && !fullTimeAlertShown) {
                    fullTimeAlertShown = true
                    triggerAlert("${halfTimeMin * 2}分钟", "准备结束比赛")
                    mainTimeLabel.setTextColor(getColor(R.color.timer_danger))
                    statusLabel.text = "下半场补时"

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
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
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
        tvTitle.text = if (isHistory) "历史详情" else "比赛总结"


        // 2.1 算比分：主队进球 vs 客队进球
        val homeGoals = eventsToShow.count { it.event == "进球" && it.detail.contains("主队") }
        val awayGoals = eventsToShow.count { it.event == "进球" && it.detail.contains("客队") }

        tvStatMatchTime.text = "时长: 每半场 ${hTime}分"
        // 新格式： 比分: 3 : 2
        tvStatGoals.text = "比分: $homeGoals : $awayGoals"

        tvStatYellow.text = "黄牌: ${eventsToShow.count { it.event == "黄牌" }}"
        tvStatRed.text = "红牌: ${eventsToShow.count { it.event == "红牌" }}"


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
            window.setGravity(android.view.Gravity.BOTTOM)

            // 设置 Y 轴偏移量 (距离底部的距离)
            params.y = (300 * resources.displayMetrics.density).toInt()

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

            "finished" -> {
                // 🟥 状态：重置比赛
                mainButton.text = "重置比赛"

                // 换成你准备好的矢量图 ic_substitute (或者 ic_refresh 也可以)
                mainButton.setIconResource(R.drawable.ic_substitute)

                // 颜色可以是红色，或者换个颜色提示用户这是“重置”
                // 这里暂时保持深红色，或者换成深灰色避免误触
                mainButton.backgroundTintList = android.content.res.ColorStateList.valueOf(0xFFC62828.toInt())

                mainButton.visibility = View.VISIBLE
                endHalfButton.visibility = View.GONE // 既然已经结束了，就不需要再显示“结束”按钮了
            }
        }
    }



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
                itemView.findViewById<android.widget.TextView>(R.id.tvRecordDuration).text = "${record.halfTimeMinutes}分钟/半场"
                itemView.findViewById<android.widget.TextView>(R.id.tvRecordStoppage).text = "补时: 上 ${record.firstHalfStoppage} | 下 ${record.secondHalfStoppage}"
                itemView.findViewById<android.view.View>(R.id.tvRecordEvents).visibility = android.view.View.GONE

                itemView.findViewById<android.widget.TextView>(R.id.tvRecordDate).text = record.date
                itemView.findViewById<android.widget.TextView>(R.id.tvRecordDuration).text = "${record.halfTimeMinutes}分钟/半场"
                itemView.findViewById<android.widget.TextView>(R.id.tvRecordStoppage).text =
                    "补时: 上 ${record.firstHalfStoppage} | 下 ${record.secondHalfStoppage}"

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

                // 3. 核心交互逻辑
                var startX = 0f
                var isSwiped = false

                itemView.setOnTouchListener { v, event ->
                    when (event.action) {
                        android.view.MotionEvent.ACTION_DOWN -> {
                            startX = event.x
                            true
                        }
                        android.view.MotionEvent.ACTION_UP -> {
                            val deltaX = startX - event.x

                            if (deltaX > 100) {
                                // 向左滑：展开
                                v.animate().translationX(-200f).setDuration(200).start()
                                btnDelete.animate().alpha(1f).setDuration(200).start() // 按钮浮现
                                btnDelete.isEnabled = true // 按钮变为可点
                                isSwiped = true
                            }
                            else if (deltaX < -100 || (isSwiped && Math.abs(deltaX) < 10)) {
                                // 向右滑 或 在展开状态下轻点：收回
                                v.animate().translationX(0f).setDuration(200).start()
                                btnDelete.animate().alpha(0f).setDuration(200).start() // 按钮消失
                                btnDelete.isEnabled = false // 按钮不可点
                                isSwiped = false
                            }
                            else if (Math.abs(deltaX) < 10 && !isSwiped) {
                                // 正常轻点（未展开）：查看详情
                                showMatchSummary(isHistory = true, historyRecord = record)
                            }
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

            if (records.isEmpty()) {

                android.widget.Toast.makeText(this, "暂无历史记录可清空", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }


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
            "黄牌" -> R.drawable.ic_card to android.graphics.Color.YELLOW
            "红牌" -> R.drawable.ic_card to android.graphics.Color.RED
            "进球" -> R.drawable.sports_soccer to android.graphics.Color.WHITE
            else -> 0 to 0
        }

        tvTitle.text = "$eventType - 选择队伍"
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
            selectedTeam = "主队"
            dialog.dismiss()
            showNumberSelectionDialog(eventType, selectedTeam)
        }

        btnAwayTeam.setOnClickListener {
            selectedTeam = "客队"
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
            "黄牌" -> R.drawable.ic_card to android.graphics.Color.YELLOW
            "红牌" -> R.drawable.ic_card to android.graphics.Color.RED
            "进球" -> R.drawable.sports_soccer to android.graphics.Color.WHITE
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
        tvTeamInfo.setTextColor(if (team == "主队") 0xFF1565C0.toInt() else 0xFFC62828.toInt())

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



        updateStoppageTimeDisplay()

        addLog("$emoji [$timeStr] $eventType - $teamEmoji $detailText")
    }





    // ==================== 数据类 ====================




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
}
