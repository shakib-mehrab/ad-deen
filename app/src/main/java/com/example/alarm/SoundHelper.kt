package com.example.alarm

import android.content.ContentResolver
import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.util.Log
import java.io.File

object SoundHelper {
    private const val TAG = "SoundHelper"

    const val SOUND_TAHAJJUD = "tahajjud_sound"
    const val SOUND_FAJR = "fajr_sound"
    const val SOUND_DUHA = "duha_sound"
    const val SOUND_DHUHR = "dhuhr_sound"
    const val SOUND_DUHR = "duhr_sound" // backward compatibility alias
    const val SOUND_ASR = "asr_sound"
    const val SOUND_MAGHRIB = "maghrib_sound"
    const val SOUND_ISHA = "isha_sound"
    const val SOUND_ISTIGFAR = "istigfar_sound"
    const val SOUND_DARUD = "darud_sound"

    /**
     * Initializes the custom sound storage directory on the device.
     */
    fun ensureCustomSoundFolders(context: Context): File {
        val extDir = context.getExternalFilesDir("custom_sounds")
        if (extDir != null && !extDir.exists()) {
            extDir.mkdirs()
        }
        val intDir = File(context.filesDir, "custom_sounds")
        if (!intDir.exists()) {
            intDir.mkdirs()
        }
        return extDir ?: intDir
    }

    /**
     * Resolves the sound Uri in the following order:
     * 1. res/raw/ (e.g. R.raw.duha_sound, R.raw.duha, R.raw.dhuhr_sound, etc.)
     * 2. App storage directory: /Android/data/<package>/files/custom_sounds/<name>.mp3 (.wav, .ogg)
     * 3. Fallback: null (caller should use system default notification / alarm sound)
     */
    fun getSoundUri(context: Context, soundName: String): Uri? {
        try {
            val namesToCheck = listOf(
                soundName.lowercase(),
                soundName.lowercase().removeSuffix("_sound"),
                "${soundName.lowercase()}_sound"
            ).distinct()

            // 1. Check res/raw
            for (name in namesToCheck) {
                val rawResId = context.resources.getIdentifier(name, "raw", context.packageName)
                if (rawResId != 0) {
                    Log.d(TAG, "Found custom sound in res/raw: $name (resId: $rawResId)")
                    return Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/$rawResId")
                }
            }

            // 2. Check external / internal storage folder
            val searchDirs = listOfNotNull(
                context.getExternalFilesDir("custom_sounds"),
                File(context.filesDir, "custom_sounds")
            )
            val extensions = listOf(".mp3", ".wav", ".ogg", ".m4a", ".aac")

            for (dir in searchDirs) {
                if (dir.exists()) {
                    for (ext in extensions) {
                        val file = File(dir, "${soundName.lowercase()}$ext")
                        if (file.exists() && file.length() > 0) {
                            Log.d(TAG, "Found custom sound file in storage: ${file.absolutePath}")
                            return Uri.fromFile(file)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving custom sound $soundName", e)
        }

        // 3. Fallback
        return null
    }

    /**
     * Returns true if a custom audio file is found for the given sound name.
     */
    fun hasCustomSound(context: Context, soundName: String): Boolean {
        return getSoundUri(context, soundName) != null
    }

    /**
     * Copies selected audio from external/system Uri to local app storage.
     */
    fun saveCustomAudio(context: Context, soundKey: String, sourceUri: Uri): String {
        var originalName = "$soundKey.mp3"
        try {
            context.contentResolver.query(sourceUri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    originalName = cursor.getString(nameIndex)
                }
            }

            val dir = File(context.filesDir, "custom_sounds").apply { if (!exists()) mkdirs() }
            val destFile = File(dir, "${soundKey.lowercase()}.mp3")

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            Log.d(TAG, "Saved custom audio for $soundKey to ${destFile.absolutePath} ($originalName)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save custom audio for $soundKey", e)
        }
        return originalName
    }

    /**
     * Deletes local custom audio file for a given sound key.
     */
    fun removeCustomAudio(context: Context, soundKey: String): Boolean {
        return try {
            val dir = File(context.filesDir, "custom_sounds")
            val file = File(dir, "${soundKey.lowercase()}.mp3")
            if (file.exists()) file.delete() else true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Helper to play a sound (either custom or fallback to notification ringtone).
     */
    fun playPreviewSound(context: Context, soundName: String, onCompletion: (() -> Unit)? = null): MediaPlayer? {
        val soundUri = getSoundUri(context, soundName)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

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
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing preview sound $soundName", e)
            null
        }
    }
}
