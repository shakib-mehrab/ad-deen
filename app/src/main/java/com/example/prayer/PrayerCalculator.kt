package com.example.prayer

import com.example.model.CalculationMethod
import com.example.model.DailyPrayerSchedule
import com.example.model.ForbiddenTimeWindow
import com.example.model.PrayerName
import com.example.model.PrayerWindow
import java.util.Calendar
import java.util.Date
import java.util.TimeZone
import kotlin.math.*

object PrayerCalculator {

    private const val SUN_ALTITUDE_SUNRISE = -0.8333 // accounting for refraction and sun disc
    private const val SAFETY_BUFFER_MINUTES = 2L

    /**
     * Calculates complete daily prayer schedule for a specific date, coordinates, and calculation method.
     */
    fun calculateSchedule(
        calendar: Calendar,
        latitude: Double,
        longitude: Double,
        method: CalculationMethod,
        timeZone: TimeZone = calendar.timeZone
    ): DailyPrayerSchedule {
        val y = calendar.get(Calendar.YEAR)
        val m = calendar.get(Calendar.MONTH) + 1
        val d = calendar.get(Calendar.DAY_OF_MONTH)

        // Date string in yyyy-MM-dd
        val dateString = String.format("%04d-%02d-%02d", y, m, d)

        // Today times
        val todaySun = calculateSolarTimes(y, m, d, latitude, longitude, method, timeZone)

        // Yesterday times for Tahajjud (span between yesterday's Maghrib and today's Fajr)
        val prevCal = (calendar.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, -1) }
        val prevSun = calculateSolarTimes(
            prevCal.get(Calendar.YEAR),
            prevCal.get(Calendar.MONTH) + 1,
            prevCal.get(Calendar.DAY_OF_MONTH),
            latitude,
            longitude,
            method,
            timeZone
        )

        // Tomorrow times for Islamic Midnight (halfway between today's Maghrib and tomorrow's Fajr)
        val nextCal = (calendar.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 1) }
        val nextSun = calculateSolarTimes(
            nextCal.get(Calendar.YEAR),
            nextCal.get(Calendar.MONTH) + 1,
            nextCal.get(Calendar.DAY_OF_MONTH),
            latitude,
            longitude,
            method,
            timeZone
        )

        // 1. Tahajjud: Last third of the night (previous Maghrib to today Fajr)
        val nightDurationYesterdayToToday = todaySun.fajrMillis - prevSun.maghribMillis
        val tahajjudStartMillis = todaySun.fajrMillis - (nightDurationYesterdayToToday / 3)
        val tahajjudEndMillis = todaySun.fajrMillis

        // 2. Fajr: Fajr start to Sunrise
        val fajrStartMillis = todaySun.fajrMillis
        val sunriseMillis = todaySun.sunriseMillis
        val fajrEndMillis = sunriseMillis

        // 3. Duha: Sunrise + 20 minutes to Dhuhr start
        val duhaStartMillis = sunriseMillis + 20 * 60 * 1000L
        val dhuhrStartMillis = todaySun.dhuhrMillis
        val duhaEndMillis = dhuhrStartMillis

        // 4. Dhuhr: Zawal / Dhuhr start to Asr start
        val asrStartMillis = todaySun.asrMillis
        val dhuhrEndMillis = asrStartMillis

        // 5. Asr: Asr start to Sunset (Maghrib)
        val maghribStartMillis = todaySun.maghribMillis
        val asrEndMillis = maghribStartMillis

        // 6. Maghrib: Maghrib start to Isha start
        val ishaStartMillis = todaySun.ishaMillis
        val maghribEndMillis = ishaStartMillis

        // 7. Isha (and Witr): Isha start to Islamic midnight (halfway between Maghrib and Fajr)
        val nightDurationTodayToTomorrow = nextSun.fajrMillis - todaySun.maghribMillis
        val midnightMillis = todaySun.maghribMillis + (nightDurationTodayToTomorrow / 2)
        val ishaEndMillis = midnightMillis

        val now = System.currentTimeMillis()

        fun makeWindow(name: PrayerName, start: Long, end: Long): PrayerWindow {
            val isCurrent = now in start until end
            return PrayerWindow(
                prayer = name,
                startTimeMillis = start,
                endTimeMillis = end,
                isCurrent = isCurrent,
                isNext = false
            )
        }

        val tahajjudWindow = makeWindow(PrayerName.TAHAJJUD, tahajjudStartMillis, tahajjudEndMillis)
        val fajrWindow = makeWindow(PrayerName.FAJR, fajrStartMillis, fajrEndMillis)
        val duhaWindow = makeWindow(PrayerName.DUHA, duhaStartMillis, duhaEndMillis)
        val dhuhrWindow = makeWindow(PrayerName.DHUHR, dhuhrStartMillis, dhuhrEndMillis)
        val asrWindow = makeWindow(PrayerName.ASR, asrStartMillis, asrEndMillis)
        val maghribWindow = makeWindow(PrayerName.MAGHRIB, maghribStartMillis, maghribEndMillis)
        val ishaWindow = makeWindow(PrayerName.ISHA, ishaStartMillis, ishaEndMillis)

        val windows = listOf(tahajjudWindow, fajrWindow, duhaWindow, dhuhrWindow, asrWindow, maghribWindow, ishaWindow)
        val nextWindow = windows.firstOrNull { it.startTimeMillis > now }

        val finalWindows = windows.map { w ->
            w.copy(isNext = (w.prayer == nextWindow?.prayer))
        }

        val forbiddenTimes = listOf(
            ForbiddenTimeWindow(
                id = "sunrise_forbidden",
                titleEn = "Sunrise (Shuruq)",
                titleBn = "সূর্যোদয়কাল (মাকরুহ)",
                descEn = "From sunrise until sun is a spear high (~15 mins)",
                descBn = "সূর্যোদয়ের সময় থেকে ১৫ মিনিট পর্যন্ত",
                startTimeMillis = sunriseMillis,
                endTimeMillis = sunriseMillis + 15 * 60 * 1000L
            ),
            ForbiddenTimeWindow(
                id = "zawal_forbidden",
                titleEn = "Midday Zenith (Zawal)",
                titleBn = "দ্বিপ্রহর / নিস্ফে নাহার (মাকরুহ)",
                descEn = "Exact solar noon until Dhuhr starts (~8 mins)",
                descBn = "ঠিক দ্বিপ্রহরে সূর্য মাথার উপরে অবস্থানকালে",
                startTimeMillis = dhuhrStartMillis - 8 * 60 * 1000L,
                endTimeMillis = dhuhrStartMillis
            ),
            ForbiddenTimeWindow(
                id = "sunset_forbidden",
                titleEn = "Sunset (Ghurub)",
                titleBn = "সূর্যাস্তকাল (মাকরুহ)",
                descEn = "15 mins before Maghrib until sunset completes",
                descBn = "সূর্যাস্তের পূর্বের ১৫ মিনিট",
                startTimeMillis = maghribStartMillis - 15 * 60 * 1000L,
                endTimeMillis = maghribStartMillis
            )
        )

        return DailyPrayerSchedule(
            dateString = dateString,
            tahajjud = finalWindows[0],
            fajr = finalWindows[1],
            sunriseMillis = sunriseMillis,
            duha = finalWindows[2],
            dhuhr = finalWindows[3],
            asr = finalWindows[4],
            maghrib = finalWindows[5],
            isha = finalWindows[6],
            midnightMillis = midnightMillis,
            forbiddenTimes = forbiddenTimes
        )
    }

    private data class SolarTimesRaw(
        val fajrMillis: Long,
        val sunriseMillis: Long,
        val dhuhrMillis: Long,
        val asrMillis: Long,
        val maghribMillis: Long,
        val ishaMillis: Long
    )

    private fun calculateSolarTimes(
        year: Int,
        month: Int,
        day: Int,
        lat: Double,
        lng: Double,
        method: CalculationMethod,
        tz: TimeZone
    ): SolarTimesRaw {
        // Julian Day
        val jd = julianDate(year, month, day)
        val d = jd - 2451545.0

        // Mean anomaly & Longitude
        val meanAnomaly = fixAngle(357.529 + 0.98560028 * d)
        val meanLongitude = fixAngle(280.459 + 0.98564736 * d)

        // Ecliptic longitude
        val eclipticLongitude = fixAngle(
            meanLongitude + 1.915 * sin(Math.toRadians(meanAnomaly)) +
                0.020 * sin(Math.toRadians(2 * meanAnomaly))
        )

        // Obliquity of ecliptic
        val obliquity = 23.439 - 0.00000036 * d

        // Declination
        val declination = Math.toDegrees(
            asin(sin(Math.toRadians(obliquity)) * sin(Math.toRadians(eclipticLongitude)))
        )

        // Right Ascension
        var ra = Math.toDegrees(
            atan2(
                cos(Math.toRadians(obliquity)) * sin(Math.toRadians(eclipticLongitude)),
                cos(Math.toRadians(eclipticLongitude))
            )
        )
        ra = fixAngle(ra) / 15.0

        // Equation of Time in hours with angular difference wrapping normalization
        var dLon = meanLongitude - (ra * 15.0)
        while (dLon < -180.0) dLon += 360.0
        while (dLon > 180.0) dLon -= 360.0
        val eqTime = dLon / 15.0

        // TimeZone offset in hours
        val cal = Calendar.getInstance(tz).apply {
            set(year, month - 1, day, 12, 0, 0)
        }
        val tzOffsetHours = tz.getOffset(cal.timeInMillis) / 3600000.0

        // Solar Transit (Zawal / Midday)
        val solarNoonHours = 12.0 + tzOffsetHours - (lng / 15.0) - eqTime

        // Sunrise & Sunset angle (-0.8333 deg)
        val sunriseHourAngle = hourAngle(SUN_ALTITUDE_SUNRISE, lat, declination)
        val sunriseHours = solarNoonHours - (sunriseHourAngle / 15.0)
        val sunsetHours = solarNoonHours + (sunriseHourAngle / 15.0)

        // Fajr angle (18 deg for IFB / Karachi)
        val fajrHourAngle = hourAngle(-method.fajrAngle, lat, declination)
        val fajrHours = solarNoonHours - (fajrHourAngle / 15.0)

        // Asr - Strict Hanafi (shadow factor = 2, standard across Bangladesh)
        // altitude = arccot(2 + tan(|lat - dec|))
        val latDecDiff = Math.abs(lat - declination)
        val asrAltitude = Math.toDegrees(atan(1.0 / (2.0 + tan(Math.toRadians(latDecDiff)))))
        val asrHourAngle = hourAngle(asrAltitude, lat, declination)
        val asrHours = solarNoonHours + (asrHourAngle / 15.0)

        // Isha
        val ishaHours = if (method == CalculationMethod.UMM_AL_QURA) {
            sunsetHours + 1.5 // 90 minutes after Maghrib
        } else {
            val ishaHourAngle = hourAngle(-method.ishaAngle, lat, declination)
            solarNoonHours + (ishaHourAngle / 15.0)
        }

        // Base midnight millis for this day
        val baseCal = Calendar.getInstance(tz).apply {
            set(year, month - 1, day, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val baseMidnight = baseCal.timeInMillis

        fun hoursToMillis(hours: Double): Long {
            return baseMidnight + (hours * 3600000.0).toLong()
        }

        // Precautionary buffers practiced by Islamic Foundation Bangladesh (IFB / Deen App)
        val dhuhrMillis = hoursToMillis(solarNoonHours) + (1L * 60 * 1000L)
        val maghribMillis = hoursToMillis(sunsetHours) + (2L * 60 * 1000L)
        val ishaMillis = hoursToMillis(ishaHours) + (2L * 60 * 1000L)

        return SolarTimesRaw(
            fajrMillis = hoursToMillis(fajrHours),
            sunriseMillis = hoursToMillis(sunriseHours),
            dhuhrMillis = dhuhrMillis,
            asrMillis = hoursToMillis(asrHours),
            maghribMillis = maghribMillis,
            ishaMillis = ishaMillis
        )
    }

    private fun hourAngle(altitude: Double, latitude: Double, declination: Double): Double {
        val latRad = Math.toRadians(latitude)
        val decRad = Math.toRadians(declination)
        val altRad = Math.toRadians(altitude)

        val cosHA = (sin(altRad) - sin(latRad) * sin(decRad)) / (cos(latRad) * cos(decRad))
        val clamped = cosHA.coerceIn(-1.0, 1.0)
        return Math.toDegrees(acos(clamped))
    }

    private fun julianDate(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    private fun fixAngle(angle: Double): Double {
        var a = angle % 360.0
        if (a < 0) a += 360.0
        return a
    }
}
