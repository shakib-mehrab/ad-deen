package com.example.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.R
import com.example.model.PrayerName

/**
 * Single source of truth for notification channels, notification ID ranges and sound mapping.
 * The table is mirrored in docs/SOUNDS.md - update both together.
 *
 * Android locks a channel's sound/importance once it is created, so changing the mapping
 * means bumping the channel ID suffix (currently _v2) and adding the old ID to [LEGACY_CHANNEL_IDS].
 */
object NotificationChannels {

    /** Per-waqt configuration. Duha never shares anything with Dhuhr. */
    enum class Waqt(
        val prayer: PrayerName,
        val channelId: String,
        val channelNameEn: String,
        val notificationIdBase: Int,
        val soundKey: String,
        val rawSoundRes: Int?
    ) {
        TAHAJJUD(PrayerName.TAHAJJUD, "addin_tahajjud_v2", "Tahajjud alarm", 1100, SoundHelper.SOUND_TAHAJJUD, R.raw.tahajjud),
        FAJR(PrayerName.FAJR, "addin_fajr_v2", "Fajr alarm", 1200, SoundHelper.SOUND_FAJR, R.raw.fajr),
        DUHA(PrayerName.DUHA, "addin_duha_v2", "Duha alarm", 1300, SoundHelper.SOUND_DUHA, R.raw.duha),
        DHUHR(PrayerName.DHUHR, "addin_dhuhr_v2", "Dhuhr alarm", 1400, SoundHelper.SOUND_DHUHR, R.raw.dhuhr),
        ASR(PrayerName.ASR, "addin_asr_v2", "Asr alarm", 1500, SoundHelper.SOUND_ASR, R.raw.asr),
        MAGHRIB(PrayerName.MAGHRIB, "addin_maghrib_v2", "Maghrib alarm", 1600, SoundHelper.SOUND_MAGHRIB, R.raw.maghrib),
        ISHA(PrayerName.ISHA, "addin_isha_v2", "Isha alarm", 1700, SoundHelper.SOUND_ISHA, R.raw.isha);

        /** Window-start alarm notification (foreground service). */
        val startNotificationId: Int get() = notificationIdBase + 1
        /** 15-minute "window ending" warning notification. */
        val endWarningNotificationId: Int get() = notificationIdBase + 2

        companion object {
            fun of(prayer: PrayerName): Waqt = entries.first { it.prayer == prayer }
            fun ofName(name: String?): Waqt? = entries.firstOrNull { it.prayer.name.equals(name, ignoreCase = true) }
        }
    }

    const val WARNINGS_CHANNEL_ID = "addin_warnings_v2"
    const val AZKHAR_CHANNEL_ID = "addin_azkhar_v2"
    const val ISTIGFAR_CHANNEL_ID = "addin_istigfar_v2"
    const val DURUD_CHANNEL_ID = "addin_durud_v2"

    const val AZKHAR_MORNING_NOTIFICATION_ID = 5002
    const val AZKHAR_EVENING_NOTIFICATION_ID = 5003
    const val ISTIGFAR_NOTIFICATION_ID = 7001
    const val DURUD_NOTIFICATION_ID = 8001

    /** Channels created by earlier builds; removed on startup so users don't see duplicates. */
    val LEGACY_CHANNEL_IDS = listOf("prayer_alarm_channel", "addin_reminders_channel")

    /**
     * Creates every channel (idempotent) and deletes legacy ones.
     * Sound is NOT attached to the channels: the app plays the mapped sound itself so the user's
     * custom sound (More > Custom Sounds) can be swapped without recreating channels. Each channel
     * stays independently controllable from Android settings, and the app skips playback when the
     * user has silenced or blocked that channel (see [isChannelAudible]).
     */
    fun ensureCreated(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return

        LEGACY_CHANNEL_IDS.forEach { id ->
            if (nm.getNotificationChannel(id) != null) nm.deleteNotificationChannel(id)
        }

        val channels = mutableListOf<NotificationChannel>()
        Waqt.entries.forEach { w ->
            channels += silentChannel(w.channelId, w.channelNameEn, NotificationManager.IMPORTANCE_HIGH,
                "Alarm when the ${w.prayer.titleEn} window starts")
        }
        channels += NotificationChannel(WARNINGS_CHANNEL_ID, "Prayer ending warnings", NotificationManager.IMPORTANCE_HIGH)
            .apply { description = "15 minutes before a prayer window ends" }
        channels += NotificationChannel(AZKHAR_CHANNEL_ID, "Azkhar reminders", NotificationManager.IMPORTANCE_DEFAULT)
            .apply { description = "Morning and evening azkhar reminders" }
        channels += silentChannel(ISTIGFAR_CHANNEL_ID, "Istigfar reminders", NotificationManager.IMPORTANCE_HIGH,
            "Periodic Istigfar reminders")
        channels += silentChannel(DURUD_CHANNEL_ID, "Durud reminders", NotificationManager.IMPORTANCE_HIGH,
            "Periodic Durud / Darud Sharif reminders")
        nm.createNotificationChannels(channels)
    }

    private fun silentChannel(id: String, name: String, importance: Int, desc: String) =
        NotificationChannel(id, name, importance).apply {
            description = desc
            setSound(null, null)
            enableVibration(true)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }

    /** False when the user blocked the channel or lowered it below "default" (silent) in system settings. */
    fun isChannelAudible(context: Context, channelId: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return true
        val nm = context.getSystemService(NotificationManager::class.java) ?: return true
        if (!nm.areNotificationsEnabled()) return false
        val ch = nm.getNotificationChannel(channelId) ?: return true
        return ch.importance >= NotificationManager.IMPORTANCE_DEFAULT
    }
}
