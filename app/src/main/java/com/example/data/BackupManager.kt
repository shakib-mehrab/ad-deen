package com.example.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.local.AppDatabase
import com.example.data.local.DayRecordEntity
import com.example.data.local.UserSettingsEntity
import com.example.data.local.ZikrItemEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupManager {

    private const val BACKUP_VERSION = 2

    /**
     * Generates a complete JSON backup of the user's data (days, zikr, settings).
     */
    suspend fun createBackupJson(context: Context): String = withContext(Dispatchers.IO) {
        val db = AppDatabase.getInstance(context)
        val days = db.dayRecordDao().getAllDays().first()
        val zikrItems = db.zikrItemDao().getAllItems().first()
        val settings = db.userSettingsDao().getSettingsOnce()

        val root = JSONObject()
        root.put("app", "Ad-Deen")
        root.put("version", BACKUP_VERSION)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("exportedDate", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()))

        // Days array
        val daysArray = JSONArray()
        for (day in days) {
            val dObj = JSONObject().apply {
                put("date", day.date)
                put("fajr", day.fajr)
                put("dhuhr", day.dhuhr)
                put("asr", day.asr)
                put("maghrib", day.maghrib)
                put("isha", day.isha)
                put("witr", day.witr)
                put("tahajjud", day.tahajjud)
                put("duha", day.duha)
                put("quran", day.quran)
                put("scoreVersion", day.scoreVersion)
                put("wake", day.wake)
                put("morning", day.morning)
                put("evening", day.evening)
                put("sleep", day.sleep)
                put("zikr", day.zikr)
                put("countersJson", day.countersJson)
                put("score", day.score)
                put("perfect", day.perfect)
                put("updatedAt", day.updatedAt)
            }
            daysArray.put(dObj)
        }
        root.put("days", daysArray)

        // Zikr items array
        val zikrArray = JSONArray()
        for (item in zikrItems) {
            val zObj = JSONObject().apply {
                put("id", item.id)
                put("text", item.text)
                put("target", item.target)
                put("orderIndex", item.orderIndex)
                put("isDefault", item.isDefault)
                put("archived", item.archived)
            }
            zikrArray.put(zObj)
        }
        root.put("zikrItems", zikrArray)

        // Settings object
        if (settings != null) {
            val sObj = JSONObject().apply {
                put("language", settings.language)
                put("calcMethod", settings.calcMethod)
                put("locationMode", settings.locationMode)
                put("lat", settings.lat)
                put("lng", settings.lng)
                put("district", settings.district)
                put("alarmTogglesJson", settings.alarmTogglesJson)
                put("morningReminder", settings.morningReminder)
                put("eveningReminder", settings.eveningReminder)
                put("istigfarMode", settings.istigfarMode)
                put("istigfarCustomMinutes", settings.istigfarCustomMinutes)
                put("istigfarTimesPerDay", settings.istigfarTimesPerDay)
                put("istigfarSpecificTimesJson", settings.istigfarSpecificTimesJson)
                put("istigfarEnabled", settings.istigfarEnabled)
                put("durudMode", settings.durudMode)
                put("durudCustomMinutes", settings.durudCustomMinutes)
                put("durudTimesPerDay", settings.durudTimesPerDay)
                put("durudSpecificTimesJson", settings.durudSpecificTimesJson)
                put("durudEnabled", settings.durudEnabled)
                put("quietHoursStart", settings.quietHoursStart)
                put("quietHoursEnd", settings.quietHoursEnd)
                put("hijriOffset", settings.hijriOffset)
                put("arabicFontSize", settings.arabicFontSize)
            }
            root.put("settings", sObj)
        }

        root.toString(2)
    }

    /**
     * Writes backup JSON to a temporary file in cache/backups/
     */
    suspend fun saveBackupToCache(context: Context, jsonString: String): File = withContext(Dispatchers.IO) {
        val backupDir = File(context.cacheDir, "backups").apply { if (!exists()) mkdirs() }
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        val file = File(backupDir, "AdDin_Backup_$timeStamp.json")
        file.writeText(jsonString)
        file
    }

    /**
     * Creates an Android Share Intent.
     * When Google Drive is installed on device, Google Drive will appear in the share sheet as "Save to Drive"!
     */
    fun createShareIntent(context: Context, file: File): Intent {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Ad Din Worship Tracker Backup")
            putExtra(Intent.EXTRA_TEXT, "Ad Din worship backup file generated on ${SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date())}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    /**
     * Directly writes backup JSON string to an SAF Uri (e.g. Google Drive, Downloads, or SD Card).
     */
    suspend fun writeBackupToUri(context: Context, uri: Uri, jsonString: String) = withContext(Dispatchers.IO) {
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            outputStream.write(jsonString.toByteArray(Charsets.UTF_8))
            outputStream.flush()
        }
    }

    /**
     * Reads and restores backup from an SAF Uri (from Google Drive, Downloads, or local files).
     * Returns count of days restored.
     */
    suspend fun restoreBackupFromUri(context: Context, uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val jsonText = context.contentResolver.openInputStream(uri)?.use { stream ->
                InputStreamReader(stream, Charsets.UTF_8).readText()
            } ?: throw IllegalArgumentException("Could not read backup file")

            val root = JSONObject(jsonText)
            val daysArray = root.optJSONArray("days") ?: JSONArray()
            val zikrArray = root.optJSONArray("zikrItems") ?: JSONArray()
            val settingsObj = root.optJSONObject("settings")

            val db = AppDatabase.getInstance(context)

            // 1. Restore Days
            var restoredDaysCount = 0
            for (i in 0 until daysArray.length()) {
                val d = daysArray.getJSONObject(i)
                val date = d.getString("date")
                // Skip malformed dates instead of importing garbage rows.
                if (!Regex("""\d{4}-\d{2}-\d{2}""").matches(date)) continue
                // v1 backups had no Quran/scoreVersion: those days pre-date Quran tracking.
                val scoreVersion = d.optInt("scoreVersion", com.example.model.DayRecord.LEGACY_NO_QURAN_VERSION)
                val raw = DayRecordEntity(
                    date = date,
                    fajr = d.optString("fajr", "none"),
                    dhuhr = d.optString("dhuhr", "none"),
                    asr = d.optString("asr", "none"),
                    maghrib = d.optString("maghrib", "none"),
                    isha = d.optString("isha", "none"),
                    witr = d.optBoolean("witr", false),
                    tahajjud = d.optBoolean("tahajjud", false),
                    duha = d.optBoolean("duha", false),
                    quran = d.optBoolean("quran", false),
                    scoreVersion = scoreVersion,
                    wake = d.optBoolean("wake", false),
                    morning = d.optBoolean("morning", false),
                    evening = d.optBoolean("evening", false),
                    sleep = d.optBoolean("sleep", false),
                    zikr = d.optBoolean("zikr", false),
                    countersJson = d.optString("countersJson", "{}"),
                    score = d.optInt("score", 0),
                    perfect = d.optBoolean("perfect", false),
                    updatedAt = d.optLong("updatedAt", System.currentTimeMillis())
                )
                // Recompute score/perfect from the restored fields so they always match the data.
                val entity = with(Mappers) { raw.toModel().toEntity() }
                db.dayRecordDao().insertOrUpdate(entity)
                restoredDaysCount++
            }

            // 2. Restore Zikr Items
            if (zikrArray.length() > 0) {
                val zikrEntities = mutableListOf<ZikrItemEntity>()
                for (i in 0 until zikrArray.length()) {
                    val z = zikrArray.getJSONObject(i)
                    zikrEntities.add(
                        ZikrItemEntity(
                            id = z.getString("id"),
                            text = z.getString("text"),
                            target = z.getInt("target"),
                            orderIndex = z.optInt("orderIndex", i),
                            isDefault = z.optBoolean("isDefault", false),
                            archived = z.optBoolean("archived", false)
                        )
                    )
                }
                db.zikrItemDao().insertAll(zikrEntities)
            }

            // 3. Restore Settings
            if (settingsObj != null) {
                val sEntity = UserSettingsEntity(
                    id = 1,
                    language = settingsObj.optString("language", "en"),
                    calcMethod = settingsObj.optString("calcMethod", "IFB"),
                    locationMode = settingsObj.optString("locationMode", "district"),
                    lat = settingsObj.optDouble("lat", 23.8103),
                    lng = settingsObj.optDouble("lng", 90.4125),
                    district = settingsObj.optString("district", "Dhaka"),
                    alarmTogglesJson = settingsObj.optString("alarmTogglesJson", "{}"),
                    morningReminder = settingsObj.optBoolean("morningReminder", true),
                    eveningReminder = settingsObj.optBoolean("eveningReminder", true),
                    istigfarMode = settingsObj.optString("istigfarMode", "interval_60"),
                    istigfarCustomMinutes = settingsObj.optInt("istigfarCustomMinutes", 60),
                    istigfarTimesPerDay = settingsObj.optInt("istigfarTimesPerDay", 5),
                    istigfarSpecificTimesJson = settingsObj.optString("istigfarSpecificTimesJson", "[]"),
                    istigfarEnabled = settingsObj.optBoolean("istigfarEnabled", true),
                    durudMode = settingsObj.optString("durudMode", "interval_60"),
                    durudCustomMinutes = settingsObj.optInt("durudCustomMinutes", 60),
                    durudTimesPerDay = settingsObj.optInt("durudTimesPerDay", 5),
                    durudSpecificTimesJson = settingsObj.optString("durudSpecificTimesJson", "[]"),
                    durudEnabled = settingsObj.optBoolean("durudEnabled", true),
                    quietHoursStart = settingsObj.optString("quietHoursStart", "23:00"),
                    quietHoursEnd = settingsObj.optString("quietHoursEnd", "04:00"),
                    hijriOffset = settingsObj.optInt("hijriOffset", 0),
                    arabicFontSize = settingsObj.optInt("arabicFontSize", 26),
                    customSoundNamesJson = settingsObj.optString("customSoundNamesJson", "{}")
                )
                db.userSettingsDao().saveSettings(sEntity)
            }

            restoredDaysCount
        }
    }
}
