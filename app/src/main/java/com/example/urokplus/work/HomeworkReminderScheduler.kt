package com.example.urokplus.work

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import com.example.urokplus.Assignment
import java.time.LocalTime
import java.time.ZoneId

private const val PREFS = "urok_homework_alarms"
private const val KEY_IDS = "alarm_ids"
private const val TAG = "HomeworkReminder"

object HomeworkReminderScheduler {

    /** Напоминание в 18:00 в день сдачи. */
    fun schedule(context: Context, assignments: List<Assignment>) {
        try {
            scheduleInternal(context.applicationContext, assignments)
        } catch (e: Exception) {
            Log.e(TAG, "schedule failed", e)
        }
    }

    private fun scheduleInternal(appCtx: Context, assignments: List<Assignment>) {
        val prefs = appCtx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val oldIds = prefs.getStringSet(KEY_IDS, null) ?: emptySet()
        val am = appCtx.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        for (idStr in oldIds) {
            val id = idStr.toLongOrNull() ?: continue
            runCatching { am.cancel(cancelPendingIntent(appCtx, id)) }
        }

        val zone = ZoneId.systemDefault()
        val now = System.currentTimeMillis()
        val newIds = mutableSetOf<String>()

        for (a in assignments) {
            val due = a.dueDate ?: continue
            val dueDayStart = due.atStartOfDay(zone).toInstant().toEpochMilli()
            val remindAt = dueDayStart + LocalTime.of(18, 0).toSecondOfDay() * 1000L
            if (remindAt <= now + 60_000L) continue

            val pi = pendingIntent(appCtx, a.id, a.title, a.subject)
            scheduleOneAlarm(am, remindAt, pi)
            newIds.add(a.id.toString())
        }
        prefs.edit().putStringSet(KEY_IDS, HashSet(newIds)).apply()
    }

    /**
     * На части устройств (Android 12+, отказ в «точных будильниках») setExactAndAllowWhileIdle
     * бросает SecurityException и роняет корутину LaunchedEffect — из‑за этого приложение закрывается после входа.
     */
    private fun scheduleOneAlarm(am: AlarmManager, remindAt: Long, pi: PendingIntent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, remindAt, pi)
            } else {
                @Suppress("DEPRECATION")
                am.setExact(AlarmManager.RTC_WAKEUP, remindAt, pi)
            }
            return
        } catch (e: SecurityException) {
            Log.w(TAG, "Exact alarm not permitted, trying fallback", e)
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, remindAt, pi)
                return
            }
        } catch (e: Exception) {
            Log.w(TAG, "setAndAllowWhileIdle failed", e)
        }
        try {
            @Suppress("DEPRECATION")
            am.set(AlarmManager.RTC_WAKEUP, remindAt, pi)
        } catch (e: Exception) {
            Log.e(TAG, "Could not schedule homework reminder", e)
        }
    }

    private fun cancelPendingIntent(context: Context, assignmentId: Long): PendingIntent {
        val intent = Intent(context, HomeworkAlarmReceiver::class.java).apply {
            data = Uri.parse("urokplus://hw/$assignmentId")
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, assignmentId.toInt() and 0x7FFF_FFFF, intent, flags)
    }

    private fun pendingIntent(context: Context, assignmentId: Long, title: String, subject: String): PendingIntent {
        val intent = Intent(context, HomeworkAlarmReceiver::class.java).apply {
            data = Uri.parse("urokplus://hw/$assignmentId")
            putExtra(HomeworkAlarmReceiver.EXTRA_TITLE, title)
            putExtra(HomeworkAlarmReceiver.EXTRA_SUBJECT, subject)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, assignmentId.toInt() and 0x7FFF_FFFF, intent, flags)
    }
}
