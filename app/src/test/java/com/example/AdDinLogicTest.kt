package com.example

import com.example.model.*
import com.example.prayer.PrayerCalculator
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class AdDinLogicTest {

    @Test
    fun testPrayerCalculationEngine() {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Dhaka")).apply {
            set(2026, Calendar.OCTOBER, 1, 12, 0, 0)
        }
        val dhakaLat = 23.8103
        val dhakaLng = 90.4125

        val schedule = PrayerCalculator.calculateSchedule(
            calendar = cal,
            latitude = dhakaLat,
            longitude = dhakaLng,
            method = CalculationMethod.KARACHI
        )

        assertNotNull(schedule)
        assertEquals("2026-10-01", schedule.dateString)

        // Verify window sequence order
        assertTrue(schedule.tahajjud.startTimeMillis < schedule.tahajjud.endTimeMillis)
        assertEquals(schedule.tahajjud.endTimeMillis, schedule.fajr.startTimeMillis)
        assertEquals(schedule.fajr.endTimeMillis, schedule.sunriseMillis)
        assertTrue(schedule.sunriseMillis < schedule.duha.startTimeMillis)
        assertEquals(schedule.duha.endTimeMillis, schedule.dhuhr.startTimeMillis)
        assertTrue(schedule.dhuhr.startTimeMillis < schedule.asr.startTimeMillis)
        assertTrue(schedule.asr.startTimeMillis < schedule.maghrib.startTimeMillis)
        assertTrue(schedule.maghrib.startTimeMillis < schedule.isha.startTimeMillis)
        assertTrue(schedule.isha.startTimeMillis < schedule.midnightMillis)
    }

    @Test
    fun testScoringEngine_PerfectDay() {
        val prayers = DayPrayers(
            fajr = PrayerStatus.JAMAT,
            dhuhr = PrayerStatus.JAMAT,
            asr = PrayerStatus.JAMAT,
            maghrib = PrayerStatus.JAMAT,
            isha = PrayerStatus.JAMAT
        )
        val nafl = DayNafl(witr = true, tahajjud = true, duha = true)
        val sections = DaySections(wake = true, morning = true, evening = true, sleep = true, zikr = true)

        val (score, isPerfect) = DayRecord.calculateScore(prayers, nafl, sections)

        assertEquals(100, score)
        assertTrue(isPerfect)
    }

    @Test
    fun testScoringEngine_PartialPrayersAlone() {
        val prayers = DayPrayers(
            fajr = PrayerStatus.ALONE, // 7 pts
            dhuhr = PrayerStatus.JAMAT, // 12 pts
            asr = PrayerStatus.JAMAT, // 12 pts
            maghrib = PrayerStatus.QAZA, // 0 pts
            isha = PrayerStatus.MISSED // 0 pts
        )
        val nafl = DayNafl(witr = true, tahajjud = false, duha = true) // 10 + 0 + 5 = 15
        val sections = DaySections(wake = true, morning = true, evening = false, sleep = false, zikr = false) // 3 + 3 = 6

        val (score, isPerfect) = DayRecord.calculateScore(prayers, nafl, sections)

        // 7 + 12 + 12 + 0 + 0 + 15 + 6 = 52
        assertEquals(52, score)
        assertFalse(isPerfect)
    }

    @Test
    fun testSunPositionCalculation() {
        val tz = TimeZone.getTimeZone("Asia/Dhaka")
        val cal = Calendar.getInstance(tz)
        val schedule = PrayerCalculator.calculateSchedule(
            calendar = cal,
            latitude = 23.8103,
            longitude = 90.4125,
            method = CalculationMethod.KARACHI
        )
        val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.US).apply { timeZone = tz }
        val sunriseStr = sdf.format(java.util.Date(schedule.sunriseMillis))
        val sunsetStr = sdf.format(java.util.Date(schedule.maghrib.startTimeMillis))
        val dhuhrStr = sdf.format(java.util.Date(schedule.dhuhr.startTimeMillis))
        val asrStr = sdf.format(java.util.Date(schedule.asr.startTimeMillis))
        println("REAL SOLAR TIMES IN DHAKA: Sunrise=$sunriseStr, Dhuhr=$dhuhrStr, Asr=$asrStr, Sunset=$sunsetStr")
        
        // Assert reasonable times for Dhaka
        assertTrue(schedule.sunriseMillis < schedule.dhuhr.startTimeMillis)
        assertTrue(schedule.dhuhr.startTimeMillis < schedule.maghrib.startTimeMillis)
    }

    @Test
    fun testAccurateHijriCalculation() {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Dhaka")).apply {
            set(2026, Calendar.OCTOBER, 3, 12, 0, 0)
        }
        val hijriDate = com.example.ui.components.CalendarDateHelper.getHijriDate(cal, isBn = false, dayOffset = 0)
        assertEquals(22, hijriDate.day)
        assertEquals("Rabi' ath-Thani", hijriDate.monthName)
        assertEquals(1448, hijriDate.year)

        // Post-Maghrib next day
        val hijriNextDay = com.example.ui.components.CalendarDateHelper.getHijriDate(cal, isBn = false, dayOffset = 1)
        assertEquals(23, hijriNextDay.day)
    }
}
