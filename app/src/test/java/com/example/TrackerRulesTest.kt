package com.example

import com.example.data.AppRepository
import com.example.data.Mappers
import com.example.data.Mappers.toEntity
import com.example.data.Mappers.toModel
import com.example.model.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Issues 4 (undo / deselect) and 9 (Quran scoring), plus sync merge rules from issue 6. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TrackerRulesTest {

    private val allJamat = DayPrayers(PrayerStatus.JAMAT, PrayerStatus.JAMAT, PrayerStatus.JAMAT, PrayerStatus.JAMAT, PrayerStatus.JAMAT)
    private val allNafl = DayNafl(witr = true, tahajjud = true, duha = true)
    private val allSections = DaySections(wake = true, morning = true, evening = true, sleep = true, zikr = true)

    // ---------- Issue 9: Quran scoring ----------

    @Test
    fun everythingIncludingQuran_is100AndPerfect() {
        val (score, perfect) = DayRecord.calculateScore(allJamat, allNafl, allSections, quran = true)
        assertEquals(100, score)
        assertTrue(perfect)
    }

    @Test
    fun sameDayWithoutQuran_is95AndNotPerfect() {
        val (score, perfect) = DayRecord.calculateScore(allJamat, allNafl, allSections, quran = false)
        assertEquals(95, score)
        assertFalse(perfect)
    }

    @Test
    fun breakdownMatchesSpec() {
        // 5 x Alone = 35, Witr 10, Tahajjud 10, Duha 5, Quran 5, 5 sections x 2 = 10
        val alone = DayPrayers(PrayerStatus.ALONE, PrayerStatus.ALONE, PrayerStatus.ALONE, PrayerStatus.ALONE, PrayerStatus.ALONE)
        assertEquals(75, DayRecord.calculateScore(alone, allNafl, allSections, quran = true).first)
        assertEquals(5, DayRecord.calculateScore(DayPrayers(), DayNafl(), DaySections(), quran = true).first)
        assertEquals(2, DayRecord.calculateScore(DayPrayers(), DayNafl(), DaySections(zikr = true)).first)
        assertEquals(0, DayRecord.calculateScore(
            DayPrayers(PrayerStatus.QAZA, PrayerStatus.MISSED, PrayerStatus.NONE, PrayerStatus.QAZA, PrayerStatus.MISSED),
            DayNafl(), DaySections()
        ).first)
    }

    @Test
    fun oldDaysKeepTheirResults_quranNotApplicable() {
        // A day recorded before the update (v2, no Quran field) stays perfect without Quran,
        // and Quran never adds points to it -> streaks built before the update don't break.
        val (score, perfect) = DayRecord.calculateScore(allJamat, allNafl, allSections, quran = false,
            scoreVersion = DayRecord.LEGACY_NO_QURAN_VERSION)
        assertEquals(95, score)
        assertTrue(perfect)
        assertEquals(95, DayRecord.calculateScore(allJamat, allNafl, allSections, quran = true,
            scoreVersion = DayRecord.LEGACY_NO_QURAN_VERSION).first)
        // v1 legacy formula unchanged: azkhar 3 pts each.
        assertEquals(100, DayRecord.calculateScore(allJamat, allNafl, allSections, scoreVersion = DayRecord.LEGACY_V1).first)
    }

    @Test
    fun newDaysUseCurrentRules() {
        val d = DayRecord(date = "2026-10-10")
        assertEquals(DayRecord.CURRENT_SCORE_VERSION, d.scoreVersion)
        assertTrue(d.isQuranApplicable)
    }

    @Test
    fun firestoreDocWithoutQuranField_isLegacy() {
        val legacy = Mappers.dayFromFirestore("2026-09-01", mapOf("prayers" to mapOf("fajr" to "jamat"), "score" to 12))
        assertEquals(DayRecord.LEGACY_NO_QURAN_VERSION, legacy.scoreVersion)
        assertFalse(legacy.isQuranApplicable)

        val current = Mappers.dayFromFirestore("2026-10-10", mapOf("quran" to true, "scoreVersion" to 3, "clientUpdatedAt" to 5L))
        assertTrue(current.quran)
        assertEquals(3, current.scoreVersion)
        assertEquals(5L, current.updatedAt)
    }

    @Test
    fun storedScoreAlwaysMatchesFields() {
        // Even if a stale score/perfect comes in, the stored entity is recomputed from the fields.
        val day = DayRecord(date = "2026-10-10", prayers = allJamat, nafl = allNafl, sections = allSections,
            quran = true, score = 3, perfect = false)
        val e = day.toEntity()
        assertEquals(100, e.score)
        assertTrue(e.perfect)
        val noQuran = day.copy(quran = false).toEntity().toModel()
        assertEquals(95, noQuran.score)
        assertFalse(noQuran.perfect)
    }

    // ---------- Issue 4: deselect + undo ----------

    @Test
    fun tappingSelectedStatusAgain_clearsToNotMarked() {
        val p = DayPrayers().withToggled("asr", PrayerStatus.JAMAT)
        assertEquals(PrayerStatus.JAMAT, p.asr)
        assertEquals(PrayerStatus.NONE, p.withToggled("asr", PrayerStatus.JAMAT).asr)
        for (s in listOf(PrayerStatus.JAMAT, PrayerStatus.ALONE, PrayerStatus.QAZA, PrayerStatus.MISSED)) {
            assertEquals(PrayerStatus.NONE, DayPrayers().withToggled("fajr", s).withToggled("fajr", s).fajr)
        }
        // Switching to a different status replaces it (no deselect).
        assertEquals(PrayerStatus.ALONE, p.withToggled("asr", PrayerStatus.ALONE).asr)
        // Other prayers untouched
        assertEquals(PrayerStatus.NONE, p.fajr)
    }

    @Test
    fun undoRestoresPreviousScoreAndPerfect() {
        val before = DayRecord(date = "2026-10-10", prayers = allJamat, nafl = allNafl, sections = allSections, quran = true).toEntity().toModel()
        assertTrue(before.perfect)
        // Wrong tap: Isha Jamat -> tap Jamat again = cleared
        val after = before.copy(prayers = before.prayers.withToggled("isha", PrayerStatus.JAMAT)).toEntity().toModel()
        assertEquals(88, after.score)
        assertFalse(after.perfect)
        // Undo = save the "before" record again
        val undone = before.copy(updatedAt = after.updatedAt + 1).toEntity().toModel()
        assertEquals(100, undone.score)
        assertTrue(undone.perfect)
    }

    // ---------- Issue 6: sync merge ----------

    @Test
    fun lastWriterWins_soClearingSyncsAcrossDevices() {
        val older = DayRecord(date = "2026-10-10", nafl = DayNafl(witr = true), updatedAt = 1_000)
        val newerCleared = DayRecord(date = "2026-10-10", nafl = DayNafl(witr = false), updatedAt = 2_000)
        assertFalse(AppRepository.mergeDay(older, newerCleared).nafl.witr) // cloud newer -> cloud wins
        assertSame(newerCleared, AppRepository.mergeDay(newerCleared, older)) // local newer -> local kept
        assertSame(older, AppRepository.mergeDay(null, older)) // nothing local -> take cloud
    }
}
