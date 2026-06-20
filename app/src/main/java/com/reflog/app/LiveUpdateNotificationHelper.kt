package com.reflog.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Icon
import android.os.Build
import androidx.annotation.RequiresApi
import java.util.Locale

/**
 * 实况窗（Live Update）通知辅助类
 *
 * Android 16+：ProgressStyle 进度条（官方 API）+ chronometer 自动走字
 *   - Point 节点：中场哨位标记
 *   - chronometer：setWhen + setUsesChronometer 系统自动走秒
 *
 * 旧版本：标准 setProgress + chronometer
 */
object LiveUpdateNotificationHelper {

    private const val CHANNEL_ID = "match_timer_channel"
    const val NOTIFICATION_ID = 1001

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.notif_channel_desc)
            setShowBadge(true)
        }
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
    }

    fun buildNotification(
        context: Context,
        timerState: TimerState,
        currentHalf: HalfState,
        mainTimeSeconds: Long,
        stoppageSeconds: Long,
        homeTeamName: String,
        awayTeamName: String,
        matchName: String,
        baseTimeMillis: Long,
        halfTimeSeconds: Long,
        homeGoals: Int = 0,
        awayGoals: Int = 0,
    ): Notification {
        val pendingIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val halfLabel = resolveTitle(context, timerState, currentHalf)
        val scoreText = "$homeGoals : $awayGoals"

        val contentText = if (stoppageSeconds > 0) {
            context.getString(R.string.notif_stoppage_time, formatTime(stoppageSeconds))
        } else {
            matchName.ifEmpty { context.getString(R.string.notif_match_in_progress) }
        }

        val builder = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notif_icon)
            .setLargeIcon(Icon.createWithResource(context, R.drawable.ic_notif_icon))
            .setSubText(halfLabel)
            .setContentTitle(scoreText)
            .setContentText(contentText)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setShowWhen(true)
            .setOnlyAlertOnce(true)

        // 前台服务通知立刻弹出（API 31+）
        if (Build.VERSION.SDK_INT >= 31) {
            builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
        }

        // ★ chronometer：setWhen 固定为比赛开始时间，永不改变！系统自动走秒
        builder.setWhen(baseTimeMillis)
        builder.setUsesChronometer(true)

        if (Build.VERSION.SDK_INT >= 36) {
            // ★ 进度条模式：上半场绿色 + 下半场蓝色 + 足球滑块
            val halfSec = halfTimeSeconds.toInt().coerceAtLeast(1)
            val ps = Notification.ProgressStyle()
                .setStyledByProgress(false)
                .setProgress(mainTimeSeconds.toInt().coerceAtMost(halfSec * 2))
                .setProgressTrackerIcon(Icon.createWithResource(context, R.drawable.sports_soccer).setTint(Color.WHITE))
                .setProgressSegments(listOf(
                    Notification.ProgressStyle.Segment(halfSec).setColor(0xFF4CAF50.toInt()),
                    Notification.ProgressStyle.Segment(halfSec).setColor(0xFF2196F3.toInt()),
                ))
                .setProgressPoints(listOf(
                    Notification.ProgressStyle.Point(halfSec).setColor(Color.WHITE),
                ))

            builder.setStyle(ps)
            builder.extras.putBoolean("android.requestPromotedOngoing", true)
        } else {
            val halfSec = halfTimeSeconds.toInt().coerceAtLeast(1)
            builder.setProgress(halfSec, mainTimeSeconds.toInt().coerceAtMost(halfSec), false)
        }

        return builder.build()
    }

    fun notify(
        context: Context,
        timerState: TimerState,
        currentHalf: HalfState,
        mainTimeSeconds: Long,
        stoppageSeconds: Long,
        homeTeamName: String,
        awayTeamName: String,
        matchName: String,
        baseTimeMillis: Long,
        halfTimeSeconds: Long,
    ) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, buildNotification(
            context, timerState, currentHalf,
            mainTimeSeconds, stoppageSeconds,
            homeTeamName, awayTeamName, matchName,
            baseTimeMillis, halfTimeSeconds,
        ))
    }

    fun cancel(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.cancel(NOTIFICATION_ID)
    }

    private fun resolveTitle(context: Context, timerState: TimerState, currentHalf: HalfState): String {
        return when (timerState) {
            TimerState.PAUSED -> context.getString(R.string.notif_paused)
            TimerState.HALFTIME -> context.getString(R.string.notif_halftime_break)
            TimerState.FINISHED -> context.getString(R.string.status_finished)
            else -> when (currentHalf) {
                HalfState.FIRST -> context.getString(R.string.notif_first_half)
                HalfState.SECOND -> context.getString(R.string.notif_second_half)
                HalfState.BREAK -> context.getString(R.string.notif_halftime_break)
            }
        }
    }

    private fun formatTime(seconds: Long): String {
        return String.format(Locale.getDefault(), "%02d:%02d", seconds / 60, seconds % 60)
    }
}
