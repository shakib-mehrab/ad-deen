package com.example.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.model.CalculationMethod
import com.example.model.IstigfarDurudConfig
import com.example.model.PrayerName
import com.example.model.UserSettings
import com.example.prayer.PrayerCalculator
import com.example.receiver.AlarmReceiver
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * Schedules every alarm from the CURRENT settings each time it runs (app open, settings change,
 * boot, time/timezone change, daily refresh, and after each alarm fires). Nothing is cached:
 * callers always pass freshly loaded [UserSettings].
 *
 * Prayer alarms get a deterministic request code per (waqt, type, date), so a disabled toggle
 * cancels exactly those PendingIntents and they are never re-created while disabled.
 */
object AlarmScheduler {

    private const val TAG = "AlarmScheduler"

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
    const val TYPE_DAILY_REFRESH = "daily_refresh"

    /** Days scheduled ahead (today + 7). The daily refresh keeps this window rolling. */
    const val DAYS_AHEAD = 7

    /** Durud fires this long after Istigfar when both land on the same slot, so sounds never overlap. */
    const val DURUD_OFFSET_MILLIS = 30_000L

    // Request-code layout: 1_KTT_DDDDD style, all well below Int.MAX_VALUE.
    private const val KIND_START = 1
    private const val KIND_END = 2
    private const val KIND_AZKHAR_MORNING = 3
    private const val KIND_AZKHAR_EVENING = 4
    private const val RC_ISTIGFAR = 7_000_001
    private const val RC_DURUD = 8_000_001
    private const val RC_DAILY_REFRESH = 9_000_001

    /** Deterministic request code for one alarm of one waqt on one calendar date. */
    fun requestCode(kind: Int, slot: Int, epochDay: Long): Int =
        100_000_000 + kind * 10_000_000 + slot * 1_000_000 + (epochDay % 1_000_000).toInt()

    fun startRequestCode(prayer: PrayerName, epochDay: Long) = requestCode(KIND_START, prayer.ordinal, epochDay)
    fun endRequestCode(prayer: PrayerName, epochDay: Long) = requestCode(KIND_END, prayer.ordinal, epochDay)

    /** Local calendar day number (days since 1970-01-01 in the given zone). */
    fun epochDay(cal: Calendar): Long {
        val offset = cal.timeZone.getOffset(cal.timeInMillis)
        return Math.floorDiv(cal.timeInMillis + offset, TimeUnit.DAYS.toMillis(1))
    }

    fun scheduleAll(context: Context, settings: UserSettings, nowMillis: Long = System.currentTimeMillis()) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        NotificationChannels.ensureCreated(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            Log.w(TAG, "Exact alarm permission not granted; alarms may be delayed")
        }

        val method = CalculationMethod.fromId(settings.calcMethod)
        val tz = TimeZone.getDefault()
        val isBn = settings.language == "bn"

        // Yesterday is included only so its leftovers get cancelled when a toggle is turned off.
        for (dayOffset in -1..DAYS_AHEAD) {
            val cal = Calendar.getInstance(tz).apply {
                timeInMillis = nowMillis
                add(Calendar.DAY_OF_YEAR, dayOffset)
            }
            val day = epochDay(cal)
            val schedule = PrayerCalculator.calculateSchedule(cal, settings.lat, settings.lng, method, tz)

            for (window in schedule.allWindows()) {
                val waqt = NotificationChannels.Waqt.of(window.prayer)
                val key = window.prayer.id

                val startCode = startRequestCode(window.prayer, day)
                val startOn = settings.alarmToggles["start_$key"] != false
                if (startOn && dayOffset >= 0 && window.startTimeMillis > nowMillis) {
                    val title = if (isBn) "${window.prayer.titleBn} এর ওয়াক্ত শুরু" else "${window.prayer.titleEn} window started"
                    val msg = if (isBn) "নামাজের জন্য প্রস্তুতি গ্রহণ করুন" else "Time to prepare for prayer"
                    set(am, window.startTimeMillis, broadcast(context, startCode, TYPE_WINDOW_START, waqt.prayer.name, title, msg), alarmClock = true, context = context)
                } else {
                    cancel(context, am, startCode)
                }

                val endCode = endRequestCode(window.prayer, day)
                val endOn = settings.alarmToggles["end_$key"] != false
                val warningAt = window.endTimeMillis - TimeUnit.MINUTES.toMillis(15)
                if (endOn && dayOffset >= 0 && warningAt > nowMillis && warningAt > window.startTimeMillis) {
                    val title = if (isBn) "${window.prayer.titleBn} এর ওয়াক্ত শেষ হতে ১৫ মিনিট বাকি" else "15 mins left: ${window.prayer.titleEn}"
                    val msg = if (isBn) "ওয়াক্ত শেষ হওয়ার পূর্বে নামাজ আদায় করে নিন" else "Complete the prayer before the window ends"
                    set(am, warningAt, broadcast(context, endCode, TYPE_WINDOW_END, waqt.prayer.name, title, msg))
                } else {
                    cancel(context, am, endCode)
                }
            }

            val morningCode = requestCode(KIND_AZKHAR_MORNING, 0, day)
            val morningAt = schedule.fajr.startTimeMillis + TimeUnit.MINUTES.toMillis(20)
            if (settings.morningReminder && dayOffset >= 0 && morningAt > nowMillis) {
                val title = if (isBn) "সকালের আযকার ও দুআ" else "Morning Azkhar Reminder"
                val msg = if (isBn) "রাসূলুল্লাহ (সা.)-এর সুন্নাহ অনুযায়ী সকালের যিকির পাঠ করুন" else "Recite your morning azkhar from the Sunnah"
                set(am, morningAt, broadcast(context, morningCode, TYPE_AZKHAR_MORNING, null, title, msg))
            } else {
                cancel(context, am, morningCode)
            }

            val eveningCode = requestCode(KIND_AZKHAR_EVENING, 0, day)
            val eveningAt = schedule.asr.startTimeMillis
            if (settings.eveningReminder && dayOffset >= 0 && eveningAt > nowMillis) {
                val title = if (isBn) "সন্ধ্যার আযকার ও দুআ" else "Evening Azkhar Reminder"
                val msg = if (isBn) "সন্ধ্যার সুরক্ষার মাসনূন যিকির পাঠ করুন" else "Recite your evening azkhar for protection"
                set(am, eveningAt, broadcast(context, eveningCode, TYPE_AZKHAR_EVENING, null, title, msg))
            } else {
                cancel(context, am, eveningCode)
            }
        }

        scheduleReminder(context, settings, isIstigfar = true, nowMillis = nowMillis)
        scheduleReminder(context, settings, isIstigfar = false, nowMillis = nowMillis)
        scheduleDailyRefresh(context, am, nowMillis)
    }

    /**
     * Schedules (or cancels) ONE periodic reminder. Istigfar and Durud each have their own
     * request code, interval and channel, so scheduling one never touches the other.
     */
    fun scheduleReminder(context: Context, settings: UserSettings, isIstigfar: Boolean, nowMillis: Long = System.currentTimeMillis()) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val code = if (isIstigfar) RC_ISTIGFAR else RC_DURUD
        val config = if (isIstigfar) settings.istigfar else settings.durud
        if (!config.enabled) {
            cancel(context, am, code)
            return
        }
        val at = nextReminderTime(nowMillis, intervalMinutes(config), if (isIstigfar) 0L else DURUD_OFFSET_MILLIS)
        val type = if (isIstigfar) TYPE_ISTIGFAR else TYPE_DURUD
        set(am, at, broadcast(context, code, type, null, null, null))
    }

    fun intervalMinutes(config: IstigfarDurudConfig): Int = when (config.mode) {
        "interval_15" -> 15
        "interval_30" -> 30
        "interval_45" -> 45
        "interval_60" -> 60
        "interval_120" -> 120
        "interval_180" -> 180
        "custom_interval" -> config.customIntervalMinutes.coerceAtLeast(15)
        else -> 60
    }

    /**
     * Next reminder slot, aligned to local midnight (e.g. every 30 min -> :00 and :30) plus [offsetMillis].
     * Aligning to the clock makes the schedule stateless: reopening the app or rebooting never
     * pushes a reminder later, and two reminders with the same interval always share a slot,
     * where Durud's +30 s offset keeps it after Istigfar.
     * The 60 s look-ahead guarantees the slot that just fired is never picked again.
     */
    fun nextReminderTime(nowMillis: Long, intervalMinutes: Int, offsetMillis: Long, tz: TimeZone = TimeZone.getDefault()): Long {
        val intervalMs = TimeUnit.MINUTES.toMillis(intervalMinutes.toLong())
        val midnight = Calendar.getInstance(tz).apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val reference = nowMillis + 60_000L - offsetMillis
        val slots = Math.floorDiv(reference - midnight, intervalMs) + 1
        return midnight + slots * intervalMs + offsetMillis
    }

    /** Re-runs [scheduleAll] once a day so the 7-day window never runs out, even if the app is not opened. */
    private fun scheduleDailyRefresh(context: Context, am: AlarmManager, nowMillis: Long) {
        val next = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 5); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val pi = broadcast(context, RC_DAILY_REFRESH, TYPE_DAILY_REFRESH, null, null, null)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pi)
        } else {
            am.set(AlarmManager.RTC_WAKEUP, next, pi)
        }
    }

    private fun intent(context: Context) = Intent(context, AlarmReceiver::class.java).apply {
        action = AlarmReceiver.ACTION_PRAYER_ALARM
    }

    private fun broadcast(context: Context, code: Int, type: String, prayerName: String?, title: String?, msg: String?): PendingIntent {
        val i = intent(context).apply {
            putExtra(EXTRA_ALARM_TYPE, type)
            prayerName?.let { putExtra(EXTRA_PRAYER_NAME, it) }
            title?.let { putExtra(EXTRA_TITLE, it) }
            msg?.let { putExtra(EXTRA_MESSAGE, it) }
        }
        return PendingIntent.getBroadcast(context, code, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    /** Cancels the alarm for [code] if one exists. Extras are not part of PendingIntent identity, so this matches exactly. */
    private fun cancel(context: Context, am: AlarmManager, code: Int) {
        val pi = PendingIntent.getBroadcast(context, code, intent(context), PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
        if (pi != null) {
            am.cancel(pi)
            pi.cancel()
        }
    }

    /** True if an alarm with this request code is currently scheduled (used by tests and diagnostics). */
    fun isScheduled(context: Context, code: Int): Boolean =
        PendingIntent.getBroadcast(context, code, intent(context), PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE) != null

    fun istigfarRequestCode() = RC_ISTIGFAR
    fun durudRequestCode() = RC_DURUD

    private fun set(am: AlarmManager, at: Long, pi: PendingIntent, alarmClock: Boolean = false, context: Context? = null) {
        try {
            val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
            when {
                // Prayer-start alarms use setAlarmClock: exempt from Doze limits, so they ring on time.
                alarmClock && canExact && context != null -> {
                    val show = PendingIntent.getActivity(
                        context, 0,
                        Intent(context, com.example.MainActivity::class.java),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    am.setAlarmClock(AlarmManager.AlarmClockInfo(at, show), pi)
                }
                canExact && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ->
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ->
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
                else -> am.setExact(AlarmManager.RTC_WAKEUP, at, pi)
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Exact alarm not permitted, falling back to inexact", e)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    /** Quiet hours apply only to Istigfar/Durud. Times are "HH:mm"; end is exclusive; ranges may cross midnight. */
    fun isQuietHour(nowMillis: Long, startStr: String, endStr: String): Boolean {
        return runCatching {
            val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
            val currentMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
            val (sh, sm) = startStr.trim().split(":").map { it.toInt() }
            val (eh, em) = endStr.trim().split(":").map { it.toInt() }
            val startMin = sh * 60 + sm
            val endMin = eh * 60 + em
            when {
                startMin == endMin -> false
                startMin < endMin -> currentMinutes in startMin until endMin
                else -> currentMinutes >= startMin || currentMinutes < endMin
            }
        }.getOrDefault(false)
    }
}
