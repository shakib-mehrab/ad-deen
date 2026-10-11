package com.example

import com.example.model.CalculationMethod
import com.example.prayer.PrayerCalculator
import com.example.ui.isNightAt
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

/** Day/night theme follows the prayer schedule only: night from Maghrib, day from Fajr. */
class NightThemeTest {

    private val dhaka = TimeZone.getTimeZone("Asia/Dhaka")
    private val schedule = PrayerCalculator.calculateSchedule(
        Calendar.getInstance(dhaka).apply { clear(); set(2026, Calendar.OCTOBER, 11, 12, 0) },
        23.8103, 90.4125, CalculationMethod.values().first(), dhaka
    )
    private val minute = 60_000L

    @Test fun nightStartsAtMaghrib() {
        assertFalse(isNightAt(schedule.maghrib.startTimeMillis - minute, schedule))
        assertTrue(isNightAt(schedule.maghrib.startTimeMillis, schedule))
    }

    @Test fun dayStartsAtFajr() {
        assertTrue(isNightAt(schedule.fajr.startTimeMillis - minute, schedule))
        assertFalse(isNightAt(schedule.fajr.startTimeMillis, schedule))
    }

    @Test fun middayIsDayAndMidnightIsNight() {
        assertFalse(isNightAt(schedule.dhuhr.startTimeMillis, schedule))
        val earlyMorning = Calendar.getInstance(dhaka).apply { clear(); set(2026, Calendar.OCTOBER, 11, 1, 0) }.timeInMillis
        assertTrue(isNightAt(earlyMorning, schedule))
    }
}
