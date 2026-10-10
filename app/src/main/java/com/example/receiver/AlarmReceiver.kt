package com.example.receiver

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.alarm.AlarmScheduler
import com.example.alarm.NotificationChannels
import com.example.alarm.SoundHelper
import com.example.data.AssetLoader
import com.example.data.Mappers
import com.example.service.PrayerAudioService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.random.Random

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_PRAYER_ALARM = "com.example.addin.ACTION_PRAYER_ALARM"
        private const val TAG = "AlarmReceiver"

        /** Posts an Istigfar/Durud reminder right now (used by the alarm and by the "Send test" button). */
        suspend fun deliverReminder(context: Context, isIstigfar: Boolean, respectQuietHours: Boolean = true) {
            val settings = Mappers.loadSettings(context)
            if (respectQuietHours && AlarmScheduler.isQuietHour(System.currentTimeMillis(), settings.quietHoursStart, settings.quietHoursEnd)) {
                Log.d(TAG, "Quiet hours: skipping ${if (isIstigfar) "istigfar" else "durud"}")
                return
            }
            NotificationChannels.ensureCreated(context)
            val (istigfarList, durudList) = AssetLoader.loadReminderPhrases(context)
            val list = if (isIstigfar) istigfarList else durudList
            val isBn = settings.language == "bn"
            val title = if (isIstigfar) {
                if (isBn) "ইস্তিগফার স্মরণিকা" else "Istigfar Reminder"
            } else {
                if (isBn) "দরূদ শরীফ স্মরণিকা" else "Durud Reminder"
            }
            val msg = list.takeIf { it.isNotEmpty() }?.get(Random.nextInt(list.size))
                ?.let { "${it.arabic}\n${if (isBn) it.bn else it.en}" }
                ?: title
            val channel = if (isIstigfar) NotificationChannels.ISTIGFAR_CHANNEL_ID else NotificationChannels.DURUD_CHANNEL_ID
            val id = if (isIstigfar) NotificationChannels.ISTIGFAR_NOTIFICATION_ID else NotificationChannels.DURUD_NOTIFICATION_ID
            postNotification(context, channel, id, title, msg)

            if (NotificationChannels.isChannelAudible(context, channel)) {
                val key = if (isIstigfar) SoundHelper.SOUND_ISTIGFAR else SoundHelper.SOUND_DARUD
                // Wait for playback (bounded) so the process is not killed mid-sound.
                withTimeoutOrNull(8_000L) {
                    suspendCancellableCoroutine { cont ->
                        val player = SoundHelper.playPreviewSound(context, key) { if (cont.isActive) cont.resume(Unit) }
                        if (player == null && cont.isActive) cont.resume(Unit)
                        cont.invokeOnCancellation { runCatching { player?.release() } }
                    }
                }
            }
        }

        fun postNotification(context: Context, channelId: String, id: Int, title: String, message: String) {
            val appIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pi = PendingIntent.getActivity(context, id, appIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val notification = NotificationCompat.Builder(context, channelId)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setSmallIcon(R.drawable.ic_stat_notification)
                .setColor(0xFF874D14.toInt())
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()
            runCatching {
                (context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)?.notify(id, notification)
            }.onFailure { Log.w(TAG, "notify failed: ${it.message}") }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val type = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_TYPE) ?: return
        val title = intent.getStringExtra(AlarmScheduler.EXTRA_TITLE)
        val message = intent.getStringExtra(AlarmScheduler.EXTRA_MESSAGE)
        val waqt = NotificationChannels.Waqt.ofName(intent.getStringExtra(AlarmScheduler.EXTRA_PRAYER_NAME))

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Always re-read the toggles: an alarm that was disabled after scheduling must not ring.
                val settings = Mappers.loadSettings(context)
                NotificationChannels.ensureCreated(context)
                when (type) {
                    AlarmScheduler.TYPE_WINDOW_START -> if (waqt != null && settings.alarmToggles["start_${waqt.prayer.id}"] != false) {
                        if (NotificationChannels.isChannelAudible(context, waqt.channelId)) {
                            PrayerAudioService.startAlarm(
                                context,
                                title ?: waqt.prayer.titleEn,
                                message ?: "Time for prayer",
                                waqt
                            )
                        } else {
                            postNotification(context, waqt.channelId, waqt.startNotificationId, title ?: waqt.prayer.titleEn, message ?: "")
                        }
                    }
                    AlarmScheduler.TYPE_WINDOW_END -> if (waqt != null && settings.alarmToggles["end_${waqt.prayer.id}"] != false) {
                        postNotification(
                            context, NotificationChannels.WARNINGS_CHANNEL_ID, waqt.endWarningNotificationId,
                            title ?: "Prayer time ending soon", message ?: "15 minutes remaining"
                        )
                    }
                    AlarmScheduler.TYPE_AZKHAR_MORNING -> if (settings.morningReminder) {
                        postNotification(context, NotificationChannels.AZKHAR_CHANNEL_ID, NotificationChannels.AZKHAR_MORNING_NOTIFICATION_ID,
                            title ?: "Morning Azkhar", message ?: "")
                    }
                    AlarmScheduler.TYPE_AZKHAR_EVENING -> if (settings.eveningReminder) {
                        postNotification(context, NotificationChannels.AZKHAR_CHANNEL_ID, NotificationChannels.AZKHAR_EVENING_NOTIFICATION_ID,
                            title ?: "Evening Azkhar", message ?: "")
                    }
                    AlarmScheduler.TYPE_ISTIGFAR, AlarmScheduler.TYPE_DURUD -> {
                        val isIstigfar = type == AlarmScheduler.TYPE_ISTIGFAR
                        // Re-arm THIS reminder first so a slow/failed delivery never stops the cycle.
                        AlarmScheduler.scheduleReminder(context, settings, isIstigfar)
                        val enabled = if (isIstigfar) settings.istigfar.enabled else settings.durud.enabled
                        if (enabled) deliverReminder(context, isIstigfar)
                    }
                    AlarmScheduler.TYPE_DAILY_REFRESH -> AlarmScheduler.scheduleAll(context, settings)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed handling $type", e)
            } finally {
                pending.finish()
            }
        }
    }
}
