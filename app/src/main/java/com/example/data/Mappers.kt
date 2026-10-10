package com.example.data

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.DayRecordEntity
import com.example.data.local.UserSettingsEntity
import com.example.model.*
import org.json.JSONArray
import org.json.JSONObject

/** Entity <-> model mapping shared by the repository, receivers and backup code. */
internal object Mappers {

    private val DEFAULT_TIMES = listOf("09:00", "12:00", "15:00", "18:00", "21:00")

    /** Reads settings straight from Room. Cheap: no Firebase, safe to call from receivers. */
    suspend fun loadSettings(context: Context): UserSettings =
        AppDatabase.getInstance(context).userSettingsDao().getSettingsOnce()?.toModel() ?: UserSettings()

    fun UserSettingsEntity.toModel(): UserSettings = UserSettings(
        language = language,
        calcMethod = calcMethod,
        locationMode = locationMode,
        lat = lat,
        lng = lng,
        district = district,
        alarmToggles = jsonToMapBool(alarmTogglesJson).let { UserSettings.defaultAlarmToggles() + it },
        morningReminder = morningReminder,
        eveningReminder = eveningReminder,
        istigfar = IstigfarDurudConfig(
            mode = istigfarMode,
            customIntervalMinutes = istigfarCustomMinutes,
            timesPerDay = istigfarTimesPerDay,
            specificTimes = jsonToList(istigfarSpecificTimesJson).ifEmpty { DEFAULT_TIMES },
            enabled = istigfarEnabled
        ),
        durud = IstigfarDurudConfig(
            mode = durudMode,
            customIntervalMinutes = durudCustomMinutes,
            timesPerDay = durudTimesPerDay,
            specificTimes = jsonToList(durudSpecificTimesJson).ifEmpty { DEFAULT_TIMES },
            enabled = durudEnabled
        ),
        quietHoursStart = quietHoursStart,
        quietHoursEnd = quietHoursEnd,
        hijriOffset = hijriOffset,
        arabicFontSize = if (arabicFontSize in 18..44) arabicFontSize else 26,
        customSoundNames = jsonToMapString(customSoundNamesJson)
    )

    fun UserSettings.toEntity(): UserSettingsEntity = UserSettingsEntity(
        id = 1,
        language = language,
        calcMethod = calcMethod,
        locationMode = locationMode,
        lat = lat,
        lng = lng,
        district = district,
        alarmTogglesJson = mapToJson(alarmToggles),
        morningReminder = morningReminder,
        eveningReminder = eveningReminder,
        istigfarMode = istigfar.mode,
        istigfarCustomMinutes = istigfar.customIntervalMinutes,
        istigfarTimesPerDay = istigfar.timesPerDay,
        istigfarSpecificTimesJson = listToJson(istigfar.specificTimes),
        istigfarEnabled = istigfar.enabled,
        durudMode = durud.mode,
        durudCustomMinutes = durud.customIntervalMinutes,
        durudTimesPerDay = durud.timesPerDay,
        durudSpecificTimesJson = listToJson(durud.specificTimes),
        durudEnabled = durud.enabled,
        quietHoursStart = quietHoursStart,
        quietHoursEnd = quietHoursEnd,
        hijriOffset = hijriOffset,
        arabicFontSize = arabicFontSize,
        customSoundNamesJson = mapToJson(customSoundNames)
    )

    fun DayRecordEntity.toModel(): DayRecord = DayRecord(
        date = date,
        prayers = DayPrayers(
            fajr = PrayerStatus.fromCode(fajr),
            dhuhr = PrayerStatus.fromCode(dhuhr),
            asr = PrayerStatus.fromCode(asr),
            maghrib = PrayerStatus.fromCode(maghrib),
            isha = PrayerStatus.fromCode(isha)
        ),
        nafl = DayNafl(witr = witr, tahajjud = tahajjud, duha = duha),
        sections = DaySections(wake = wake, morning = morning, evening = evening, sleep = sleep, zikr = zikr),
        quran = quran,
        scoreVersion = scoreVersion,
        counters = jsonToMapInt(countersJson),
        score = score,
        perfect = perfect,
        updatedAt = updatedAt
    )

    /** Recomputes score/perfect from the fields using the day's own scoreVersion, so stored values always match. */
    fun DayRecord.toEntity(): DayRecordEntity {
        val (s, p) = DayRecord.calculateScore(prayers, nafl, sections, quran, scoreVersion)
        return DayRecordEntity(
            date = date,
            fajr = prayers.fajr.code,
            dhuhr = prayers.dhuhr.code,
            asr = prayers.asr.code,
            maghrib = prayers.maghrib.code,
            isha = prayers.isha.code,
            witr = nafl.witr,
            tahajjud = nafl.tahajjud,
            duha = nafl.duha,
            quran = quran,
            scoreVersion = scoreVersion,
            wake = sections.wake,
            morning = sections.morning,
            evening = sections.evening,
            sleep = sections.sleep,
            zikr = sections.zikr,
            countersJson = mapToJson(counters),
            score = s,
            perfect = p,
            updatedAt = updatedAt
        )
    }

    /** Firestore representation of a day (users/{uid}/days/{yyyy-MM-dd}). */
    fun DayRecord.toFirestoreMap(): Map<String, Any> {
        val (s, p) = DayRecord.calculateScore(prayers, nafl, sections, quran, scoreVersion)
        return mapOf(
            "prayers" to prayers.toMap(),
            "nafl" to nafl.toMap(),
            "sections" to sections.toMap(),
            "quran" to quran,
            "scoreVersion" to scoreVersion,
            "counters" to counters,
            "score" to s,
            "perfect" to p,
            "clientUpdatedAt" to updatedAt,
            "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )
    }

    /**
     * Parses a Firestore day document. Docs written by older builds may lack quran/scoreVersion:
     * a missing scoreVersion means the legacy rules that pre-date Quran tracking (Quran not applicable).
     */
    @Suppress("UNCHECKED_CAST")
    fun dayFromFirestore(date: String, data: Map<String, Any?>): DayRecord {
        val counters = (data["counters"] as? Map<*, *>)?.mapNotNull { (k, v) ->
            val key = k as? String
            val value = (v as? Number)?.toInt()
            if (key != null && value != null) key to value else null
        }?.toMap() ?: emptyMap()
        return DayRecord(
            date = date,
            prayers = DayPrayers.fromMap(data["prayers"] as? Map<String, Any?>),
            nafl = DayNafl.fromMap(data["nafl"] as? Map<String, Any?>),
            sections = DaySections.fromMap(data["sections"] as? Map<String, Any?>),
            quran = (data["quran"] as? Boolean) == true,
            scoreVersion = (data["scoreVersion"] as? Number)?.toInt() ?: DayRecord.LEGACY_NO_QURAN_VERSION,
            counters = counters,
            score = (data["score"] as? Number)?.toInt() ?: 0,
            perfect = (data["perfect"] as? Boolean) == true,
            updatedAt = (data["clientUpdatedAt"] as? Number)?.toLong() ?: 0L
        )
    }

    // ---------------- JSON helpers ----------------

    fun mapToJson(map: Map<String, Any?>): String {
        val obj = JSONObject()
        map.forEach { (k, v) -> obj.put(k, v) }
        return obj.toString()
    }

    fun listToJson(list: List<String>): String = JSONArray().apply { list.forEach { put(it) } }.toString()

    fun jsonToList(json: String?): List<String> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            List(arr.length()) { arr.getString(it) }
        }.getOrDefault(emptyList())
    }

    fun jsonToMapInt(json: String?): Map<String, Int> = jsonToMap(json) { o, k -> o.getInt(k) }
    fun jsonToMapBool(json: String?): Map<String, Boolean> = jsonToMap(json) { o, k -> o.getBoolean(k) }
    fun jsonToMapString(json: String?): Map<String, String> = jsonToMap(json) { o, k -> o.getString(k) }

    private fun <T> jsonToMap(json: String?, read: (JSONObject, String) -> T): Map<String, T> {
        if (json.isNullOrBlank()) return emptyMap()
        return runCatching {
            val obj = JSONObject(json)
            val map = mutableMapOf<String, T>()
            obj.keys().forEach { k -> map[k] = read(obj, k) }
            map.toMap()
        }.getOrDefault(emptyMap())
    }
}
