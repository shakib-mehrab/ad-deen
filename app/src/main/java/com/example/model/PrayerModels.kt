package com.example.model

enum class PrayerName(val id: String, val titleEn: String, val titleBn: String, val isFard: Boolean) {
    TAHAJJUD("tahajjud", "Tahajjud", "তাহাজ্জুদ", false),
    FAJR("fajr", "Fajr", "ফজর", true),
    DUHA("duha", "Duha / Ishraq", "দুহা / চাশত", false),
    DHUHR("dhuhr", "Dhuhr", "যোহর", true),
    ASR("asr", "Asr", "আসর", true),
    MAGHRIB("maghrib", "Maghrib", "মাগরিব", true),
    ISHA("isha", "Isha", "ইশা", true)
}

enum class PrayerStatus(val code: String, val titleEn: String, val titleBn: String, val points: Int) {
    JAMAT("jamat", "With Jamat (+12)", "জামাতে (+১২)", 12),
    ALONE("alone", "Prayed Alone (+7)", "একাকী (+৭)", 7),
    QAZA("qaza", "Qaza (0 pts)", "ক্বাযা (০)", 0),
    MISSED("missed", "Missed (0 pts)", "ছুটে গেছে (০)", 0),
    NONE("none", "Not Marked", "চিহ্নিত নয়", 0);

    companion object {
        fun fromCode(code: String?): PrayerStatus {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: NONE
        }
    }
}

enum class CalculationMethod(val id: String, val titleEn: String, val titleBn: String, val fajrAngle: Double, val ishaAngle: Double) {
    IFB("IFB", "Islamic Foundation Bangladesh (Deen App)", "ইসলামিক ফাউন্ডেশন বাংলাদেশ", 18.0, 18.0),
    KARACHI("Karachi", "University of Islamic Sciences, Karachi (Hanafi)", "করাচি ইসলামিক বিশ্ববিদ্যালয় (হানাফী)", 18.0, 18.0),
    MWL("MWL", "Muslim World League", "মুসলিম ওয়ার্ল্ড লীগ", 18.0, 17.0),
    EGYPTIAN("Egyptian", "Egyptian General Authority of Survey", "মিশরীয় জরিপ কর্তৃপক্ষ", 19.5, 17.5),
    UMM_AL_QURA("UmmAlQura", "Umm al-Qura University, Makkah", "উম্মুল কুরা বিশ্ববিদ্যালয়, মক্কা", 18.5, 0.0); // 90 min after Maghrib

    companion object {
        fun fromId(id: String?): CalculationMethod {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: IFB
        }
    }
}

data class District(
    val id: String,
    val nameEn: String,
    val nameBn: String,
    val lat: Double,
    val lng: Double
)

data class PrayerWindow(
    val prayer: PrayerName,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val isCurrent: Boolean = false,
    val isNext: Boolean = false
)

data class ForbiddenTimeWindow(
    val id: String,
    val titleEn: String,
    val titleBn: String,
    val descEn: String,
    val descBn: String,
    val startTimeMillis: Long,
    val endTimeMillis: Long
)

data class DailyPrayerSchedule(
    val dateString: String,
    val tahajjud: PrayerWindow,
    val fajr: PrayerWindow,
    val sunriseMillis: Long,
    val duha: PrayerWindow,
    val dhuhr: PrayerWindow,
    val asr: PrayerWindow,
    val maghrib: PrayerWindow,
    val isha: PrayerWindow,
    val midnightMillis: Long,
    val forbiddenTimes: List<ForbiddenTimeWindow> = emptyList()
) {
    fun allWindows(): List<PrayerWindow> = listOf(
        tahajjud, fajr, duha, dhuhr, asr, maghrib, isha
    )

    fun activeForbiddenTime(nowMillis: Long): ForbiddenTimeWindow? {
        return forbiddenTimes.firstOrNull { nowMillis in it.startTimeMillis until it.endTimeMillis }
    }
}
