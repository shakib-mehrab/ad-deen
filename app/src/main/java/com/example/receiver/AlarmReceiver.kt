package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.alarm.AlarmScheduler
import com.example.data.AppRepository
import com.example.data.AssetLoader
import com.example.service.PrayerAudioService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.random.Random

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_PRAYER_ALARM = "com.example.addin.ACTION_PRAYER_ALARM"
        const val REMINDER_CHANNEL_ID = "addin_reminders_channel"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val type = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_TYPE) ?: return
        val title = intent.getStringExtra(AlarmScheduler.EXTRA_TITLE)
        val message = intent.getStringExtra(AlarmScheduler.EXTRA_MESSAGE)
        val prayerName = intent.getStringExtra(AlarmScheduler.EXTRA_PRAYER_NAME)

        createReminderChannel(context)

        when (type) {
            AlarmScheduler.TYPE_WINDOW_START -> {
                val soundKey = when (prayerName?.uppercase()) {
                    "FAJR" -> com.example.alarm.SoundHelper.SOUND_FAJR
                    "DHUHR" -> com.example.alarm.SoundHelper.SOUND_DUHR
                    "ASR" -> com.example.alarm.SoundHelper.SOUND_ASR
                    "MAGHRIB" -> com.example.alarm.SoundHelper.SOUND_MAGHRIB
                    "ISHA" -> com.example.alarm.SoundHelper.SOUND_ISHA
                    else -> com.example.alarm.SoundHelper.SOUND_DUHR
                }
                PrayerAudioService.startAlarm(
                    context,
                    title ?: "Prayer Window Started",
                    message ?: "Time for prayer",
                    isPrayerStart = true,
                    soundKey = soundKey
                )
            }
            AlarmScheduler.TYPE_WINDOW_END -> {
                showSimpleNotification(
                    context,
                    notificationId = 5001,
                    title = title ?: "Prayer Time Ending Soon",
                    message = message ?: "15 minutes remaining for prayer"
                )
            }
            AlarmScheduler.TYPE_AZKHAR_MORNING -> {
                showSimpleNotification(
                    context,
                    notificationId = 5002,
                    title = title ?: "Morning Azkhar",
                    message = message ?: "Start your morning with the remembrance of Allah"
                )
            }
            AlarmScheduler.TYPE_AZKHAR_EVENING -> {
                showSimpleNotification(
                    context,
                    notificationId = 5003,
                    title = title ?: "Evening Azkhar",
                    message = message ?: "Recite your evening azkhar for peace and protection"
                )
            }
            AlarmScheduler.TYPE_ISTIGFAR -> {
                handlePeriodicReminder(context, isIstigfar = true)
            }
            AlarmScheduler.TYPE_DURUD -> {
                handlePeriodicReminder(context, isIstigfar = false)
            }
        }
    }

    private fun handlePeriodicReminder(context: Context, isIstigfar: Boolean) {
        val repo = AppRepository(context)
        CoroutineScope(Dispatchers.IO).launch {
            val settings = repo.getSettingsOnce()
            val now = System.currentTimeMillis()

            // Quiet hours check - quiet hours only apply to Istigfar/Durud
            val inQuiet = AlarmScheduler.isQuietHour(now, settings.quietHoursStart, settings.quietHoursEnd)
            if (!inQuiet) {
                val (istigfarList, durudList) = AssetLoader.loadReminderPhrases(context)
                val list = if (isIstigfar) istigfarList else durudList

                if (list.isNotEmpty()) {
                    val phrase = list[Random.nextInt(list.size)]
                    val isBn = settings.language == "bn"
                    val title = if (isIstigfar) {
                        if (isBn) "ইস্তিগফার স্মরণিকা" else "Istigfar Reminder"
                    } else {
                        if (isBn) "দরূদ শরীফ স্মরণিকা" else "Darud Sharif Reminder"
                    }
                    val msg = "${phrase.arabic}\n${if (isBn) phrase.bn else phrase.en}"
                    val soundKey = if (isIstigfar) {
                        com.example.alarm.SoundHelper.SOUND_ISTIGFAR
                    } else {
                        com.example.alarm.SoundHelper.SOUND_DARUD
                    }

                    showSimpleNotification(
                        context,
                        notificationId = if (isIstigfar) 6001 else 6002,
                        title = title,
                        message = msg,
                        soundKey = soundKey
                    )
                }
            }

            // Reschedule next
            AlarmScheduler.scheduleNext7Days(context, settings)
        }
    }

    private fun showSimpleNotification(
        context: Context,
        notificationId: Int,
        title: String,
        message: String,
        soundKey: String? = null
    ) {
        val appIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            context,
            notificationId,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val largeIcon = try {
            android.graphics.BitmapFactory.decodeResource(context.resources, com.example.R.drawable.ic_app_logo)
        } catch (_: Exception) { null }

        val builder = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setSmallIcon(com.example.R.drawable.ic_notification_logo)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        if (largeIcon != null) {
            builder.setLargeIcon(largeIcon)
        }

        val notification = builder.build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(notificationId, notification)

        // Play custom notification sound if specified, or fallback to notification ringtone
        if (!soundKey.isNullOrEmpty()) {
            com.example.alarm.SoundHelper.playPreviewSound(context, soundKey)
        }
    }

    private fun createReminderChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                REMINDER_CHANNEL_ID,
                "Ad Din Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Daily reminders for prayers, azkhar, istigfar, and durud"
                enableVibration(true)
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}
