package com.example.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.alarm.NotificationChannels
import com.example.alarm.SoundHelper

/** Plays the waqt's own sound for up to 45 s with a "Stop" action, on that waqt's own channel. */
class PrayerAudioService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var currentNotificationId: Int = 0
    private val handler = Handler(Looper.getMainLooper())
    private val autoStopRunnable = Runnable { stopEverything() }

    companion object {
        private const val TAG = "PrayerAudioService"
        const val ACTION_START_ALARM = "com.example.addin.ACTION_START_ALARM"
        const val ACTION_STOP_ALARM = "com.example.addin.ACTION_STOP_ALARM"

        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_MESSAGE = "extra_message"
        const val EXTRA_WAQT = "extra_waqt"

        private const val AUTO_STOP_MILLIS = 45_000L

        fun startAlarm(context: Context, title: String, message: String, waqt: NotificationChannels.Waqt) {
            val intent = Intent(context, PrayerAudioService::class.java).apply {
                action = ACTION_START_ALARM
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_MESSAGE, message)
                putExtra(EXTRA_WAQT, waqt.name)
            }
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (e: Exception) {
                // e.g. ForegroundServiceStartNotAllowedException: still show the notification.
                Log.e(TAG, "Could not start alarm service", e)
                com.example.receiver.AlarmReceiver.postNotification(context, waqt.channelId, waqt.startNotificationId, title, message)
            }
        }

        fun stopAlarm(context: Context) {
            context.startService(Intent(context, PrayerAudioService::class.java).apply { action = ACTION_STOP_ALARM })
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        NotificationChannels.ensureCreated(this)
        val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
        wakeLock = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AdDeen:PrayerAudio")?.apply { acquire(60_000L) }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_ALARM) {
            stopEverything()
            return START_NOT_STICKY
        }

        val waqt = runCatching { NotificationChannels.Waqt.valueOf(intent?.getStringExtra(EXTRA_WAQT) ?: "") }
            .getOrDefault(NotificationChannels.Waqt.FAJR)
        val title = intent?.getStringExtra(EXTRA_TITLE) ?: waqt.prayer.titleEn
        val message = intent?.getStringExtra(EXTRA_MESSAGE) ?: "Time for prayer"

        // A new alarm replaces a still-ringing one; remove the old notification first.
        if (currentNotificationId != 0 && currentNotificationId != waqt.startNotificationId) {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        }
        currentNotificationId = waqt.startNotificationId

        val notification = buildNotification(waqt, title, message)
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK else 0
        ServiceCompat.startForeground(this, waqt.startNotificationId, notification, type)

        playAudio(waqt)
        handler.removeCallbacks(autoStopRunnable)
        handler.postDelayed(autoStopRunnable, AUTO_STOP_MILLIS)
        return START_NOT_STICKY
    }

    private fun playAudio(waqt: NotificationChannels.Waqt) {
        stopAudio()
        // Waqt's own sound (custom file > res/raw/<waqt>.wav); system default alarm if none exists.
        val uri: Uri = SoundHelper.getSoundUri(applicationContext, waqt.soundKey)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: return
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, uri)
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
            Log.d(TAG, "Playing ${waqt.name} sound: $uri")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play ${waqt.name} sound", e)
        }
    }

    private fun stopAudio() {
        runCatching {
            mediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
        }
        mediaPlayer = null
    }

    private fun stopEverything() {
        stopAudio()
        handler.removeCallbacks(autoStopRunnable)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH)
        stopSelf()
    }

    private fun buildNotification(waqt: NotificationChannels.Waqt, title: String, message: String): Notification {
        val stopPendingIntent = PendingIntent.getService(
            this, waqt.startNotificationId,
            Intent(this, PrayerAudioService::class.java).apply { action = ACTION_STOP_ALARM },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val appPendingIntent = PendingIntent.getActivity(
            this, waqt.startNotificationId,
            Intent(this, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, waqt.channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setColor(0xFF874D14.toInt())
            .setContentIntent(appPendingIntent)
            .setDeleteIntent(stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .build()
    }

    override fun onDestroy() {
        stopAudio()
        handler.removeCallbacks(autoStopRunnable)
        if (wakeLock?.isHeld == true) wakeLock?.release()
        super.onDestroy()
    }
}
