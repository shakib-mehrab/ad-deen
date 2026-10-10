package com.example.alarm

import android.content.ContentResolver
import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.util.Log
import com.example.R
import java.io.File

object SoundHelper {
    private const val TAG = "SoundHelper"

    const val SOUND_TAHAJJUD = "tahajjud_sound"
    const val SOUND_FAJR = "fajr_sound"
    const val SOUND_DUHA = "duha_sound"
    const val SOUND_DHUHR = "dhuhr_sound"
    const val SOUND_ASR = "asr_sound"
    const val SOUND_MAGHRIB = "maghrib_sound"
    const val SOUND_ISHA = "isha_sound"
    const val SOUND_ISTIGFAR = "istigfar_sound"
    const val SOUND_DARUD = "darud_sound"

    /**
     * Bundled sound for each key: app/src/main/res/raw/<file>. Mirrors docs/SOUNDS.md.
     * A key mapped to null (or a missing file) falls back to the system default sound.
     */
    private val BUNDLED: Map<String, Int?> = mapOf(
        SOUND_TAHAJJUD to R.raw.tahajjud,
        SOUND_FAJR to R.raw.fajr,
        SOUND_DUHA to R.raw.duha,
        SOUND_DHUHR to R.raw.dhuhr,
        SOUND_ASR to R.raw.asr,
        SOUND_MAGHRIB to R.raw.maghrib,
        SOUND_ISHA to R.raw.isha,
        SOUND_ISTIGFAR to R.raw.istigfar,
        SOUND_DARUD to R.raw.darud
    )

    private val EXTENSIONS = listOf(".mp3", ".wav", ".ogg", ".m4a", ".aac")

    /** Ensures the folders used for user-picked custom sounds exist. */
    fun ensureCustomSoundFolders(context: Context): File {
        val intDir = File(context.filesDir, "custom_sounds").apply { if (!exists()) mkdirs() }
        val extDir = context.getExternalFilesDir("custom_sounds")?.apply { if (!exists()) mkdirs() }
        return extDir ?: intDir
    }

    private fun customSoundFile(context: Context, soundKey: String): File? {
        val dirs = listOfNotNull(File(context.filesDir, "custom_sounds"), context.getExternalFilesDir("custom_sounds"))
        for (dir in dirs) {
            for (ext in EXTENSIONS) {
                val f = File(dir, "${soundKey.lowercase()}$ext")
                if (f.exists() && f.length() > 0) return f
            }
        }
        return null
    }

    /**
     * Resolves the sound for [soundKey]:
     * 1. a custom sound the user picked (More > Custom Sounds), so their choice always wins;
     * 2. the bundled file in res/raw;
     * 3. null -> caller uses the system default.
     */
    fun getSoundUri(context: Context, soundKey: String): Uri? {
        try {
            customSoundFile(context, soundKey)?.let { return Uri.fromFile(it) }
            BUNDLED[soundKey]?.let { resId ->
                return Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/$resId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving sound $soundKey", e)
        }
        return null
    }

    /** True when the user picked a custom file for this key. */
    fun hasCustomSound(context: Context, soundKey: String): Boolean = customSoundFile(context, soundKey) != null

    /** Copies the picked audio into app storage as custom_sounds/<key>.mp3. Returns the original file name. */
    fun saveCustomAudio(context: Context, soundKey: String, sourceUri: Uri): String {
        var originalName = "$soundKey.mp3"
        try {
            context.contentResolver.query(sourceUri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) originalName = cursor.getString(nameIndex)
            }
            val dir = File(context.filesDir, "custom_sounds").apply { if (!exists()) mkdirs() }
            val destFile = File(dir, "${soundKey.lowercase()}.mp3")
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                destFile.outputStream().use { output -> input.copyTo(output) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save custom audio for $soundKey", e)
        }
        return originalName
    }

    /** Deletes every custom file for the key so the bundled sound is used again. */
    fun removeCustomAudio(context: Context, soundKey: String): Boolean {
        var ok = true
        while (true) {
            val f = customSoundFile(context, soundKey) ?: break
            if (!f.delete()) { ok = false; break }
        }
        return ok
    }

    /** Plays the sound once on the notification stream (used for previews and Istigfar/Durud reminders). */
    fun playPreviewSound(context: Context, soundName: String, onCompletion: (() -> Unit)? = null): MediaPlayer? {
        val soundUri = getSoundUri(context, soundName)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: return null
        return try {
            MediaPlayer().apply {
                setDataSource(context, soundUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setOnCompletionListener {
                    it.release()
                    onCompletion?.invoke()
                }
                setOnErrorListener { mp, _, _ ->
                    mp.release()
                    onCompletion?.invoke()
                    true
                }
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing sound $soundName", e)
            onCompletion?.invoke()
            null
        }
    }
}
