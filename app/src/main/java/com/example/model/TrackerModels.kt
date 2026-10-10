package com.example.model

data class DayPrayers(
    val fajr: PrayerStatus = PrayerStatus.NONE,
    val dhuhr: PrayerStatus = PrayerStatus.NONE,
    val asr: PrayerStatus = PrayerStatus.NONE,
    val maghrib: PrayerStatus = PrayerStatus.NONE,
    val isha: PrayerStatus = PrayerStatus.NONE
) {
    fun toMap(): Map<String, String> = mapOf(
        "fajr" to fajr.code,
        "dhuhr" to dhuhr.code,
        "asr" to asr.code,
        "maghrib" to maghrib.code,
        "isha" to isha.code
    )

    companion object {
        fun fromMap(map: Map<String, Any?>?): DayPrayers {
            if (map == null) return DayPrayers()
            return DayPrayers(
                fajr = PrayerStatus.fromCode(map["fajr"] as? String),
                dhuhr = PrayerStatus.fromCode(map["dhuhr"] as? String),
                asr = PrayerStatus.fromCode(map["asr"] as? String),
                maghrib = PrayerStatus.fromCode(map["maghrib"] as? String),
                isha = PrayerStatus.fromCode(map["isha"] as? String)
            )
        }
    }
}

data class DayNafl(
    val witr: Boolean = false,
    val tahajjud: Boolean = false,
    val duha: Boolean = false
) {
    fun toMap(): Map<String, Boolean> = mapOf(
        "witr" to witr,
        "tahajjud" to tahajjud,
        "duha" to duha
    )

    companion object {
        fun fromMap(map: Map<String, Any?>?): DayNafl {
            if (map == null) return DayNafl()
            return DayNafl(
                witr = (map["witr"] as? Boolean) == true,
                tahajjud = (map["tahajjud"] as? Boolean) == true,
                duha = (map["duha"] as? Boolean) == true
            )
        }
    }
}

data class DaySections(
    val wake: Boolean = false,
    val morning: Boolean = false,
    val evening: Boolean = false,
    val sleep: Boolean = false,
    val zikr: Boolean = false
) {
    fun toMap(): Map<String, Boolean> = mapOf(
        "wake" to wake,
        "morning" to morning,
        "evening" to evening,
        "sleep" to sleep,
        "zikr" to zikr
    )

    companion object {
        fun fromMap(map: Map<String, Any?>?): DaySections {
            if (map == null) return DaySections()
            return DaySections(
                wake = (map["wake"] as? Boolean) == true,
                morning = (map["morning"] as? Boolean) == true,
                evening = (map["evening"] as? Boolean) == true,
                sleep = (map["sleep"] as? Boolean) == true,
                zikr = (map["zikr"] as? Boolean) == true
            )
        }
    }
}

data class DayRecord(
    val date: String, // "yyyy-MM-dd"
    val prayers: DayPrayers = DayPrayers(),
    val nafl: DayNafl = DayNafl(),
    val sections: DaySections = DaySections(),
    val quran: Boolean = false,
    val scoreVersion: Int = 2,
    val counters: Map<String, Int> = emptyMap(), // "<section>:<itemId>" -> count
    val score: Int = 0,
    val perfect: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        fun calculateScore(
            prayers: DayPrayers,
            nafl: DayNafl,
            sections: DaySections,
            quran: Boolean = false,
            scoreVersion: Int = 2
        ): Pair<Int, Boolean> {
            var pts = 0
            pts += prayers.fajr.points
            pts += prayers.dhuhr.points
            pts += prayers.asr.points
            pts += prayers.maghrib.points
            pts += prayers.isha.points

            if (nafl.witr) pts += 10
            if (nafl.tahajjud) pts += 10
            if (nafl.duha) pts += 5

            if (scoreVersion < 2) {
                // Version 1 (legacy): Azkhar sections 3 points each = 15 points
                if (sections.wake) pts += 3
                if (sections.morning) pts += 3
                if (sections.evening) pts += 3
                if (sections.sleep) pts += 3
                if (sections.zikr) pts += 3

                val isPerfect = (pts == 100) &&
                    prayers.fajr == PrayerStatus.JAMAT &&
                    prayers.dhuhr == PrayerStatus.JAMAT &&
                    prayers.asr == PrayerStatus.JAMAT &&
                    prayers.maghrib == PrayerStatus.JAMAT &&
                    prayers.isha == PrayerStatus.JAMAT &&
                    nafl.witr && nafl.tahajjud && nafl.duha &&
                    sections.wake && sections.morning && sections.evening && sections.sleep && sections.zikr

                return Pair(pts, isPerfect)
            } else {
                // Version 2 (new): Total 100
                // Prayers 60, Witr 10, Tahajjud 10, Duha 5, Quran 5, 5 Azkhar sections 2 each = 10
                if (quran) pts += 5

                if (sections.wake) pts += 2
                if (sections.morning) pts += 2
                if (sections.evening) pts += 2
                if (sections.sleep) pts += 2
                if (sections.zikr) pts += 2

                val isPerfect = (pts == 100) &&
                    prayers.fajr == PrayerStatus.JAMAT &&
                    prayers.dhuhr == PrayerStatus.JAMAT &&
                    prayers.asr == PrayerStatus.JAMAT &&
                    prayers.maghrib == PrayerStatus.JAMAT &&
                    prayers.isha == PrayerStatus.JAMAT &&
                    nafl.witr && nafl.tahajjud && nafl.duha &&
                    quran &&
                    sections.wake && sections.morning && sections.evening && sections.sleep && sections.zikr

                return Pair(pts, isPerfect)
            }
        }
    }
}

data class ZikrItem(
    val id: String,
    val text: String,
    val target: Int,
    val order: Int = 0,
    val isDefault: Boolean = false,
    val archived: Boolean = false
)

data class AzkharItem(
    val id: String,
    val order: Int,
    val titleEn: String,
    val titleBn: String,
    val arabic: String,
    val transliteration: String,
    val translationEn: String,
    val translationBn: String,
    val target: Int
)

data class AllahName(
    val number: Int,
    val arabic: String,
    val transliteration: String,
    val meaningEn: String,
    val meaningBn: String
)

data class ReminderPhrase(
    val arabic: String,
    val transliteration: String,
    val en: String,
    val bn: String
)

data class IstigfarDurudConfig(
    val mode: String = "interval_60", // "interval_30", "interval_45", "interval_60", "interval_120", "interval_180", "custom_interval", "times_day", "specific_times"
    val customIntervalMinutes: Int = 60,
    val timesPerDay: Int = 5,
    val specificTimes: List<String> = listOf("09:00", "12:00", "15:00", "18:00", "21:00"),
    val enabled: Boolean = true
)

data class UserSettings(
    val language: String = "en", // "en" or "bn"
    val calcMethod: String = "IFB",
    val locationMode: String = "district", // "gps" or "district"
    val lat: Double = 23.8103,
    val lng: Double = 90.4125,
    val district: String = "Dhaka",
    val alarmToggles: Map<String, Boolean> = defaultAlarmToggles(),
    val morningReminder: Boolean = true,
    val eveningReminder: Boolean = true,
    val istigfar: IstigfarDurudConfig = IstigfarDurudConfig(enabled = true),
    val durud: IstigfarDurudConfig = IstigfarDurudConfig(enabled = true),
    val quietHoursStart: String = "23:00",
    val quietHoursEnd: String = "04:00",
    val hijriOffset: Int = 0,
    val arabicFontSize: Int = 26,
    val customSoundNames: Map<String, String> = emptyMap()
) {
    companion object {
        fun defaultAlarmToggles(): Map<String, Boolean> = mapOf(
            "start_tahajjud" to true,
            "end_tahajjud" to true,
            "start_fajr" to true,
            "end_fajr" to true,
            "start_duha" to true,
            "end_duha" to true,
            "start_dhuhr" to true,
            "end_dhuhr" to true,
            "start_asr" to true,
            "end_asr" to true,
            "start_maghrib" to true,
            "end_maghrib" to true,
            "start_isha" to true,
            "end_isha" to true
        )
    }
}
