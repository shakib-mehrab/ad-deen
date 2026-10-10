package com.example

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.example.alarm.AlarmScheduler
import com.example.alarm.NotificationChannels
import com.example.alarm.SoundHelper
import com.example.data.Mappers.toEntity
import com.example.data.local.AppDatabase
import com.example.model.IstigfarDurudConfig
import com.example.model.PrayerName
import com.example.model.UserSettings
import com.example.receiver.AlarmReceiver
import com.example.receiver.BootReceiver
import com.example.service.PrayerAudioService
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/** Issues 1 (disabled alarms), 2 (independent Istigfar/Durud) and 8 (per-waqt channels/sounds). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AlarmSchedulerTest {

    private lateinit var context: Context
    private val tz: TimeZone = TimeZone.getTimeZone("Asia/Dhaka")
    private lateinit var originalTz: TimeZone

    // 2026-10-10 09:00 Dhaka
    private val now: Long = Calendar.getInstance(TimeZone.getTimeZone("Asia/Dhaka")).apply {
        set(2026, Calendar.OCTOBER, 10, 9, 0, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    @Before
    fun setUp() {
        originalTz = TimeZone.getDefault()
        TimeZone.setDefault(tz)
        context = ApplicationProvider.getApplicationContext()
        org.robolectric.shadows.ShadowAlarmManager.setCanScheduleExactAlarms(true)
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(originalTz)
    }

    private fun day(offset: Int): Long = AlarmScheduler.epochDay(Calendar.getInstance(tz).apply {
        timeInMillis = now; add(Calendar.DAY_OF_YEAR, offset)
    })

    private fun startScheduled(p: PrayerName, offset: Int) = AlarmScheduler.isScheduled(context, AlarmScheduler.startRequestCode(p, day(offset)))

    // ---------- Issue 1 ----------

    @Test
    fun requestCodesAreUniquePerWaqtTypeAndDate() {
        val codes = mutableSetOf<Int>()
        for (d in 0..400L) for (p in PrayerName.entries) {
            assertTrue(codes.add(AlarmScheduler.startRequestCode(p, 20_000 + d)))
            assertTrue(codes.add(AlarmScheduler.endRequestCode(p, 20_000 + d)))
        }
        assertFalse(codes.contains(AlarmScheduler.istigfarRequestCode()))
        assertFalse(codes.contains(AlarmScheduler.durudRequestCode()))
    }

    @Test
    fun disablingAsrStart_cancelsEveryFutureAsrStart_andKeepsOthers() {
        val s = UserSettings()
        AlarmScheduler.scheduleAll(context, s, now)
        for (d in 1..AlarmScheduler.DAYS_AHEAD) assertTrue("Asr day $d", startScheduled(PrayerName.ASR, d))

        val off = s.copy(alarmToggles = s.alarmToggles + ("start_asr" to false))
        AlarmScheduler.scheduleAll(context, off, now)
        for (d in 0..AlarmScheduler.DAYS_AHEAD) assertFalse("Asr start day $d must be cancelled", startScheduled(PrayerName.ASR, d))
        // End warning for Asr is a separate toggle and stays
        assertTrue(AlarmScheduler.isScheduled(context, AlarmScheduler.endRequestCode(PrayerName.ASR, day(0))))
        // Other waqts still ring
        for (p in listOf(PrayerName.DHUHR, PrayerName.MAGHRIB, PrayerName.ISHA)) assertTrue(p.name, startScheduled(p, 0))
        for (p in listOf(PrayerName.FAJR, PrayerName.DUHA, PrayerName.TAHAJJUD)) assertTrue(p.name, startScheduled(p, 1))

        // Rescheduling again (app open / daily refresh / boot) never re-creates it
        AlarmScheduler.scheduleAll(context, off, now + TimeUnit.HOURS.toMillis(1))
        for (d in 0..AlarmScheduler.DAYS_AHEAD) assertFalse(startScheduled(PrayerName.ASR, d))
    }

    @Test
    fun disabledAlarmDoesNotRingEvenIfStillDelivered_andSurvivesReboot() = runBlocking {
        val s = UserSettings()
        val off = s.copy(alarmToggles = s.alarmToggles + ("start_asr" to false))
        AppDatabase.getInstance(context).userSettingsDao().saveSettings(off.toEntity())

        // Simulate a reboot: BootReceiver rebuilds from stored settings
        BootReceiver().onReceive(context, Intent(Intent.ACTION_BOOT_COMPLETED))
        Thread.sleep(1500)
        assertFalse(startScheduled(PrayerName.ASR, 1))
        assertTrue(startScheduled(PrayerName.MAGHRIB, 1))

        // Simulate the Asr time arriving with a stale alarm: the receiver re-reads toggles and stays silent
        val app = shadowOf(context as android.app.Application)
        while (app.nextStartedService != null) { /* drain */ }
        AlarmReceiver().onReceive(context, Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_PRAYER_ALARM
            putExtra(AlarmScheduler.EXTRA_ALARM_TYPE, AlarmScheduler.TYPE_WINDOW_START)
            putExtra(AlarmScheduler.EXTRA_PRAYER_NAME, "ASR")
        })
        Thread.sleep(1500)
        assertNull("Disabled Asr must not start the alarm sound", app.nextStartedService)

        // Maghrib (enabled) does ring, with its own waqt
        AlarmReceiver().onReceive(context, Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_PRAYER_ALARM
            putExtra(AlarmScheduler.EXTRA_ALARM_TYPE, AlarmScheduler.TYPE_WINDOW_START)
            putExtra(AlarmScheduler.EXTRA_PRAYER_NAME, "MAGHRIB")
        })
        Thread.sleep(1500)
        val started = app.nextStartedService
        assertNotNull("Enabled Maghrib must ring", started)
        assertEquals(PrayerAudioService::class.java.name, started.component?.className)
        assertEquals("MAGHRIB", started.getStringExtra(PrayerAudioService.EXTRA_WAQT))
    }

    // ---------- Issue 2 ----------

    private fun cfg(mode: String, enabled: Boolean = true) = IstigfarDurudConfig(mode = mode, enabled = enabled)

    @Test
    fun sameInterval_istigfarFirst_durud30sLater_everyCycle() {
        var t = now + 7 * 60_000 // 09:07
        repeat(3) {
            val i = AlarmScheduler.nextReminderTime(t, 30, 0, tz)
            val d = AlarmScheduler.nextReminderTime(t, 30, AlarmScheduler.DURUD_OFFSET_MILLIS, tz)
            assertEquals(30_000L, d - i)
            assertEquals(0L, (i - now) % TimeUnit.MINUTES.toMillis(30)) // on the :00/:30 grid
            t = d + 1 // both fired; continue from just after Durud
        }
        // 3 consecutive cycles land 30 minutes apart
        val first = AlarmScheduler.nextReminderTime(now + 7 * 60_000, 30, 0, tz)
        val third = AlarmScheduler.nextReminderTime(first + 31_000 + 30 * 60_000L, 30, 0, tz)
        assertEquals(60 * 60_000L, third - first)
    }

    @Test
    fun differentIntervals_followTheirOwnTiming() {
        val t = now // 09:00
        val istigfar = generateSequence(AlarmScheduler.nextReminderTime(t, 30, 0, tz)) { AlarmScheduler.nextReminderTime(it, 30, 0, tz) }.take(4).toList()
        val durud = generateSequence(AlarmScheduler.nextReminderTime(t, 45, AlarmScheduler.DURUD_OFFSET_MILLIS, tz)) {
            AlarmScheduler.nextReminderTime(it, 45, AlarmScheduler.DURUD_OFFSET_MILLIS, tz)
        }.take(4).toList()
        istigfar.zipWithNext().forEach { (a, b) -> assertEquals(30 * 60_000L, b - a) }
        durud.zipWithNext().forEach { (a, b) -> assertEquals(45 * 60_000L, b - a) }
    }

    @Test
    fun reopeningTheAppDoesNotPushRemindersLater() {
        val a = AlarmScheduler.nextReminderTime(now + 60_000 * 5, 60, 0, tz)
        val b = AlarmScheduler.nextReminderTime(now + 60_000 * 40, 60, 0, tz)
        assertEquals(a, b)
    }

    @Test
    fun istigfarAndDurudAreScheduledAndCancelledIndependently() {
        val both = UserSettings(istigfar = cfg("interval_30"), durud = cfg("interval_30"))
        AlarmScheduler.scheduleAll(context, both, now)
        assertTrue(AlarmScheduler.isScheduled(context, AlarmScheduler.istigfarRequestCode()))
        assertTrue(AlarmScheduler.isScheduled(context, AlarmScheduler.durudRequestCode()))

        // Rescheduling Istigfar alone (what happens when it fires) leaves Durud untouched
        AlarmScheduler.scheduleReminder(context, both, isIstigfar = true, nowMillis = now + 30 * 60_000)
        assertTrue(AlarmScheduler.isScheduled(context, AlarmScheduler.durudRequestCode()))

        // Disabling Istigfar cancels only Istigfar
        AlarmScheduler.scheduleAll(context, both.copy(istigfar = cfg("interval_30", enabled = false)), now)
        assertFalse(AlarmScheduler.isScheduled(context, AlarmScheduler.istigfarRequestCode()))
        assertTrue(AlarmScheduler.isScheduled(context, AlarmScheduler.durudRequestCode()))

        assertEquals(15, AlarmScheduler.intervalMinutes(cfg("interval_15")))
        assertEquals(45, AlarmScheduler.intervalMinutes(cfg("interval_45")))
    }

    @Test
    fun quietHoursCrossMidnight() {
        fun at(h: Int, m: Int) = Calendar.getInstance().apply { timeInMillis = now; set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m) }.timeInMillis
        assertTrue(AlarmScheduler.isQuietHour(at(23, 30), "23:00", "04:00"))
        assertTrue(AlarmScheduler.isQuietHour(at(3, 59), "23:00", "04:00"))
        assertFalse(AlarmScheduler.isQuietHour(at(4, 0), "23:00", "04:00"))
        assertFalse(AlarmScheduler.isQuietHour(at(12, 0), "23:00", "04:00"))
        assertFalse(AlarmScheduler.isQuietHour(at(12, 0), "bad", "04:00"))
    }

    // ---------- Issue 8 ----------

    @Test
    fun everyWaqtHasItsOwnChannelIdAndSound_duhaNeverReusesDhuhr() {
        val w = NotificationChannels.Waqt.entries
        assertEquals(7, w.size)
        assertEquals(7, w.map { it.channelId }.toSet().size)
        assertEquals(7, w.map { it.soundKey }.toSet().size)
        assertEquals(7, w.map { it.rawSoundRes }.toSet().size)
        val ids = w.flatMap { listOf(it.startNotificationId, it.endWarningNotificationId) }
        assertEquals(ids.size, ids.toSet().size)
        assertTrue(w.all { it.channelId.endsWith("_v2") })

        val duha = NotificationChannels.Waqt.DUHA
        val dhuhr = NotificationChannels.Waqt.DHUHR
        assertNotEquals(duha.channelId, dhuhr.channelId)
        assertNotEquals(duha.startNotificationId, dhuhr.startNotificationId)
        assertNotEquals(SoundHelper.getSoundUri(context, duha.soundKey), SoundHelper.getSoundUri(context, dhuhr.soundKey))
        assertTrue(SoundHelper.getSoundUri(context, duha.soundKey).toString().endsWith("/${R.raw.duha}"))
        assertTrue(SoundHelper.getSoundUri(context, dhuhr.soundKey).toString().endsWith("/${R.raw.dhuhr}"))
    }

    @Test
    fun channelsCreated_legacyDeleted() {
        val nm = context.getSystemService(android.app.NotificationManager::class.java)
        nm.createNotificationChannel(android.app.NotificationChannel("prayer_alarm_channel", "old", android.app.NotificationManager.IMPORTANCE_HIGH))
        NotificationChannels.ensureCreated(context)
        assertNull(nm.getNotificationChannel("prayer_alarm_channel"))
        NotificationChannels.Waqt.entries.forEach { assertNotNull(it.channelId, nm.getNotificationChannel(it.channelId)) }
        assertNotNull(nm.getNotificationChannel(NotificationChannels.ISTIGFAR_CHANNEL_ID))
        assertNotNull(nm.getNotificationChannel(NotificationChannels.DURUD_CHANNEL_ID))
    }
}
