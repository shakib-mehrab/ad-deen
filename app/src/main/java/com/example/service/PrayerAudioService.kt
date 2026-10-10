package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity

class PrayerAudioService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val handler = Handler(Looper.getMainLooper())
    private val autoStopRunnable = Runnable { stopSelf() }

    companion object {
        const val CHANNEL_ID = "prayer_alarm_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START_ALARM = "com.example.addin.ACTION_START_ALARM"
        const val ACTION_STOP_ALARM = "com.example.addin.ACTION_STOP_ALARM"

        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_MESSAGE = "extra_message"
        const val EXTRA_IS_PRAYER_START = "extra_is_prayer_start"
        const val EXTRA_SOUND_NAME = "extra_sound_name"

        fun startAlarm(
            context: Context,
            title: String,
            message: String,
            isPrayerStart: Boolean,
            soundKey: String? = null
        ) {
            val intent = Intent(context, PrayerAudioService::class.java).apply {
                action = ACTION_START_ALARM
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_MESSAGE, message)
                putExtra(EXTRA_IS_PRAYER_START, isPrayerStart)
                if (soundKey != null) {
                    putExtra(EXTRA_SOUND_NAME, soundKey)
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopAlarm(context: Context) {
            val intent = Intent(context, PrayerAudioService::class.java).apply {
                action = ACTION_STOP_ALARM
            }
            context.startService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AdDin:PrayerAudioWakeLock")
        wakeLock?.acquire(60 * 1000L) // 1 minute max
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_ALARM) {
            stopAudio()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val title = intent?.getStringExtra(EXTRA_TITLE) ?: "Ad-Deen - Prayer Alert"
        val message = intent?.getStringExtra(EXTRA_MESSAGE) ?: "Time for Prayer"
        val soundKey = intent?.getStringExtra(EXTRA_SOUND_NAME)

        val notification = buildForegroundNotification(title, message)
        startForeground(NOTIFICATION_ID, notification)

        playAudio(soundKey)

        // Auto-stop after 45 seconds
        handler.removeCallbacks(autoStopRunnable)
        handler.postDelayed(autoStopRunnable, 45000L)

        return START_NOT_STICKY
    }

    private fun playAudio(soundKey: String?) {
        stopAudio()
        try {
            // Check custom sound folder / res/raw first (e.g. fajr_sound, duhr_sound)
            val customUri = if (!soundKey.isNullOrEmpty()) {
                com.example.alarm.SoundHelper.getSoundUri(applicationContext, soundKey)
            } else null

            // Fallback to default notification or alarm tone
            val soundUri: Uri = customUri
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, soundUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopAudio() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
            mediaPlayer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun buildForegroundNotification(title: String, message: String): Notification {
        val stopIntent = Intent(this, PrayerAudioService::class.java).apply {
            action = ACTION_STOP_ALARM
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            2002,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val appIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val appPendingIntent = PendingIntent.getActivity(
            this,
            2001,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val largeIcon = try {
            android.graphics.BitmapFactory.decodeResource(resources, com.example.R.drawable.ic_app_logo)
        } catch (_: Exception) { null }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(com.example.R.drawable.ic_notification_logo)
            .setContentIntent(appPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)

        if (largeIcon != null) {
            builder.setLargeIcon(largeIcon)
        }

        return builder.build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Prayer Alarms & Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High priority alarms for prayer windows and reminders"
                enableVibration(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        stopAudio()
        handler.removeCallbacks(autoStopRunnable)
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        super.onDestroy()
    }
}
