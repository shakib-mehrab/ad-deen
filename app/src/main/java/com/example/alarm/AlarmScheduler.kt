package com.example.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.model.CalculationMethod
import com.example.model.PrayerName
import com.example.model.UserSettings
import com.example.prayer.PrayerCalculator
import com.example.receiver.AlarmReceiver
import java.util.Calendar

object AlarmScheduler {

    const val EXTRA_ALARM_TYPE = "extra_alarm_type"
    const val EXTRA_PRAYER_NAME = "extra_prayer_name"
    const val EXTRA_TITLE = "extra_title"
    const val EXTRA_MESSAGE = "extra_message"

    const val TYPE_WINDOW_START = "window_start"
    const val TYPE_WINDOW_END = "window_end_warning"
    const val TYPE_AZKHAR_MORNING = "azkhar_morning"
    const val TYPE_AZKHAR_EVENING = "azkhar_evening"
    const val TYPE_ISTIGFAR = "istigfar"
    const val TYPE_DURUD = "durud"

    fun scheduleNext7Days(context: Context, settings: UserSettings) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        // Check if exact alarms can be scheduled on Android 12+ (API 31+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.w("AlarmScheduler", "Exact alarm permission not granted by user yet")
            }
        }

        val method = CalculationMethod.fromId(settings.calcMethod)
        val lat = settings.lat
        val lng = settings.lng
        val toggles = settings.alarmToggles

        val now = System.currentTimeMillis()

        for (dayOffset in 0..6) {
            val cal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, dayOffset)
            }
            val schedule = PrayerCalculator.calculateSchedule(cal, lat, lng, method)

            val windows = schedule.allWindows()

            windows.forEachIndexed { pIndex, window ->
                val prayerKey = window.prayer.id // "tahajjud", "fajr", etc.

                // 1. Window Start Alarm
                val startToggle = toggles["start_$prayerKey"] != false
                if (startToggle && window.startTimeMillis > now) {
                    val reqCode = 10000 + (dayOffset * 100) + (pIndex * 2)
                    val title = if (settings.language == "bn") "${window.prayer.titleBn} এর ওয়াক্ত শুরু" else "${window.prayer.titleEn} Window Started"
                    val msg = if (settings.language == "bn") "নামাজের জন্য প্রস্তুতি গ্রহণ করুন" else "Time to prepare for prayer"
                    val intent = Intent(context, AlarmReceiver::class.java).apply {
                        action = AlarmReceiver.ACTION_PRAYER_ALARM
                        putExtra(EXTRA_ALARM_TYPE, TYPE_WINDOW_START)
                        putExtra(EXTRA_PRAYER_NAME, window.prayer.name)
                        putExtra(EXTRA_TITLE, title)
                        putExtra(EXTRA_MESSAGE, msg)
                    }
                    val pi = PendingIntent.getBroadcast(
                        context,
                        reqCode,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    setExactAlarm(alarmManager, window.startTimeMillis, pi)
                }

                // 2. Window End Warning Alarm (15 mins before end)
                val endToggle = toggles["end_$prayerKey"] != false
                val warningTime = window.endTimeMillis - (15 * 60 * 1000L)
                if (endToggle && warningTime > now) {
                    val reqCode = 10000 + (dayOffset * 100) + (pIndex * 2) + 1
                    val title = if (settings.language == "bn") "${window.prayer.titleBn} এর ওয়াক্ত শেষ হতে ১৫ মিনিট বাকি" else "15 Mins Remaining: ${window.prayer.titleEn}"
                    val msg = if (settings.language == "bn") "ওয়াক্ত শেষ হওয়ার পূর্বে নামাজ আদায় করে নিন" else "Complete prayer before the window ends"
                    val intent = Intent(context, AlarmReceiver::class.java).apply {
                        action = AlarmReceiver.ACTION_PRAYER_ALARM
                        putExtra(EXTRA_ALARM_TYPE, TYPE_WINDOW_END)
                        putExtra(EXTRA_PRAYER_NAME, window.prayer.name)
                        putExtra(EXTRA_TITLE, title)
                        putExtra(EXTRA_MESSAGE, msg)
                    }
                    val pi = PendingIntent.getBroadcast(
                        context,
                        reqCode,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    setExactAlarm(alarmManager, warningTime, pi)
                }
            }

            // Azkhar Morning Reminder (e.g. 20 min after Fajr start)
            if (settings.morningReminder) {
                val morningAzkharTime = schedule.fajr.startTimeMillis + 20 * 60 * 1000L
                if (morningAzkharTime > now) {
                    val reqCode = 30000 + dayOffset
                    val title = if (settings.language == "bn") "সকালের আযকার ও দুআ" else "Morning Azkhar Reminder"
                    val msg = if (settings.language == "bn") "রাসূলুল্লাহ (সা.)-এর সুন্নাহ অনুযায়ী সকালের যিকির পাঠ করুন" else "Recite your morning azkhar from the Sunnah"
                    val intent = Intent(context, AlarmReceiver::class.java).apply {
                        action = AlarmReceiver.ACTION_PRAYER_ALARM
                        putExtra(EXTRA_ALARM_TYPE, TYPE_AZKHAR_MORNING)
                        putExtra(EXTRA_TITLE, title)
                        putExtra(EXTRA_MESSAGE, msg)
                    }
                    val pi = PendingIntent.getBroadcast(
                        context,
                        reqCode,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    setExactAlarm(alarmManager, morningAzkharTime, pi)
                }
            }

            // Azkhar Evening Reminder (e.g. at Asr start)
            if (settings.eveningReminder) {
                val eveningAzkharTime = schedule.asr.startTimeMillis
                if (eveningAzkharTime > now) {
                    val reqCode = 40000 + dayOffset
                    val title = if (settings.language == "bn") "সন্ধ্যার আযকার ও দুআ" else "Evening Azkhar Reminder"
                    val msg = if (settings.language == "bn") "সন্ধ্যার সুরক্ষার মাসনূন যিকির পাঠ করুন" else "Recite your evening azkhar for protection"
                    val intent = Intent(context, AlarmReceiver::class.java).apply {
                        action = AlarmReceiver.ACTION_PRAYER_ALARM
                        putExtra(EXTRA_ALARM_TYPE, TYPE_AZKHAR_EVENING)
                        putExtra(EXTRA_TITLE, title)
                        putExtra(EXTRA_MESSAGE, msg)
                    }
                    val pi = PendingIntent.getBroadcast(
                        context,
                        reqCode,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    setExactAlarm(alarmManager, eveningAzkharTime, pi)
                }
            }
        }

        // Schedule Istigfar & Durud Periodic Reminders
        schedulePeriodicReminders(context, settings)
    }

    private fun schedulePeriodicReminders(context: Context, settings: UserSettings) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val now = System.currentTimeMillis()

        if (settings.istigfar.enabled) {
            val intervalMinutes = when (settings.istigfar.mode) {
                "interval_30" -> 30
                "interval_45" -> 45
                "interval_60" -> 60
                "interval_120" -> 120
                "interval_180" -> 180
                "custom_interval" -> settings.istigfar.customIntervalMinutes.coerceAtLeast(15)
                else -> 60
            }
            val nextIstigfar = now + (intervalMinutes * 60 * 1000L)
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = AlarmReceiver.ACTION_PRAYER_ALARM
                putExtra(EXTRA_ALARM_TYPE, TYPE_ISTIGFAR)
            }
            val pi = PendingIntent.getBroadcast(
                context,
                50001,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            setExactAlarm(alarmManager, nextIstigfar, pi)
        }

        if (settings.durud.enabled) {
            val intervalMinutes = when (settings.durud.mode) {
                "interval_30" -> 30
                "interval_45" -> 45
                "interval_60" -> 60
                "interval_120" -> 120
                "interval_180" -> 180
                "custom_interval" -> settings.durud.customIntervalMinutes.coerceAtLeast(15)
                else -> 60
            }
            val nextDurud = now + (intervalMinutes * 60 * 1000L) + (10 * 60 * 1000L) // staggered from istigfar
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = AlarmReceiver.ACTION_PRAYER_ALARM
                putExtra(EXTRA_ALARM_TYPE, TYPE_DURUD)
            }
            val pi = PendingIntent.getBroadcast(
                context,
                60001,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            setExactAlarm(alarmManager, nextDurud, pi)
        }
    }

    private fun setExactAlarm(alarmManager: AlarmManager, triggerAtMillis: Long, operation: PendingIntent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, operation)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, operation)
            }
        } catch (e: Exception) {
            Log.e("AlarmScheduler", "Failed to schedule exact alarm: ${e.message}")
        }
    }

    /**
     * Checks if current time is within user quiet hours. Quiet hours only apply to Istigfar/Durud.
     */
    fun isQuietHour(nowMillis: Long, startStr: String, endStr: String): Boolean {
        return runCatching {
            val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
            val currentMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

            val sParts = startStr.split(":")
            val startMin = sParts[0].toInt() * 60 + sParts[1].toInt()

            val eParts = endStr.split(":")
            val endMin = eParts[0].toInt() * 60 + eParts[1].toInt()

            if (startMin <= endMin) {
                currentMinutes in startMin until endMin
            } else {
                // cross midnight e.g. 23:00 to 04:00
                currentMinutes >= startMin || currentMinutes < endMin
            }
        }.getOrDefault(false)
    }
}
