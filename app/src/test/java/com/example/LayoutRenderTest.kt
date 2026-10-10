package com.example

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.example.model.CalculationMethod
import com.example.model.PrayerName
import com.example.model.PrayerWindow
import com.example.prayer.PrayerCalculator
import com.example.ui.components.ForbiddenTimesCard
import com.example.ui.screens.PrayerCountdownHeroCard
import com.example.ui.theme.AdDinTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.util.Calendar
import java.util.TimeZone

/** Issues 7 (forbidden card) and 10 (waqt card) at 320dp width, 1.3x font, English + Bangla. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w320dp-h700dp-xxhdpi")
class LayoutRenderTest {

    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    private val outDir = File(System.getProperty("user.dir"), "build/reports/layout-screens").apply { mkdirs() }

    private val schedule = PrayerCalculator.calculateSchedule(
        Calendar.getInstance(TimeZone.getTimeZone("Asia/Dhaka")).apply { set(2026, Calendar.OCTOBER, 10, 12, 0, 0) },
        23.8103, 90.4125, CalculationMethod.IFB, TimeZone.getTimeZone("Asia/Dhaka")
    )

    private var content by mutableStateOf<(@Composable () -> Unit)?>(null)
    private var fontScale by mutableStateOf(1f)
    private var started = false

    /** One setContent per test; variants are swapped through state. */
    private fun host(scale: Float, body: @Composable () -> Unit) {
        fontScale = scale
        content = body
        if (!started) {
            started = true
            rule.setContent {
                val d = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(d.density, fontScale)) {
                    AdDinTheme {
                        Column(Modifier.width(320.dp).padding(horizontal = 16.dp)) { content?.invoke() }
                    }
                }
            }
        }
        rule.waitForIdle()
    }

    /** Saves the node's area of the real rendered view (Robolectric native graphics) as a PNG for visual review. */
    private fun save(tag: String, name: String) {
        runCatching {
            val b = rule.onNodeWithTag(tag).getBoundsInRoot()
            val view = rule.activity.window.decorView.rootView
            val full = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(full))
            val px = rule.activity.resources.displayMetrics.density
            val x = (b.left.value * px).toInt().coerceAtLeast(0)
            val y = (b.top.value * px).toInt().coerceAtLeast(0)
            val w = ((b.right - b.left).value * px).toInt().coerceAtMost(full.width - x)
            val h = ((b.bottom - b.top).value * px).toInt().coerceAtMost(full.height - y)
            File(outDir, "$name.png").outputStream().use { Bitmap.createBitmap(full, x, y, w, h).compress(Bitmap.CompressFormat.PNG, 100, it) }
        }.onFailure { println("SCREENSHOT FAILED $name: $it") }
    }

    /** True when the whole string fits in the width it was given (nothing clipped or ellipsized). */
    private fun TextLayoutResult.fitsFully(): Boolean =
        multiParagraph.intrinsics.maxIntrinsicWidth <= size.width + 1f && !hasVisualOverflow.let { it && lineCount > 1 } && !isLineEllipsized(lineCount - 1)

    private fun SemanticsNodeInteraction.layout(): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
        return results.first()
    }

    @Test
    fun waqtCard_constantHeight_fixedTimer_nameNeverPushesTimer() {
        val heights = mutableSetOf<Float>()
        for (lang in listOf("en", "bn")) for (scale in listOf(1.0f, 1.3f)) for (p in PrayerName.entries) {
            val w = PrayerWindow(p, schedule.dhuhr.startTimeMillis, schedule.dhuhr.endTimeMillis)
            host(scale) { PrayerCountdownHeroCard(w, isActive = true, remainingMillis = 23 * 3_600_000L + 59 * 60_000L + 59_000L, lang = lang, onClick = {}) }
            val card = rule.onNodeWithTag("prayer_hero_card").getBoundsInRoot()
            val box = rule.onNodeWithTag("hero_countdown_box", useUnmergedTree = true).getBoundsInRoot()
            val name = rule.onNodeWithTag("hero_waqt_name", useUnmergedTree = true).getBoundsInRoot()
            assertEquals("timer width fixed", 84f, (box.right - box.left).value, 0.5f)
            assertEquals("timer height fixed", 26f, (box.bottom - box.top).value, 0.5f)
            val overlaps = name.left < box.right && box.left < name.right && name.top < box.bottom && box.top < name.bottom
            assertFalse("$p/$lang/$scale name overlaps timer", overlaps)
            assertTrue("card fits 320dp", card.right <= 320.dp)
            if (scale == 1.0f) heights += (card.bottom - card.top).value
            val timer = rule.onNodeWithText("23:59:59", useUnmergedTree = true).layout()
            assertTrue("$p/$lang/$scale timer clipped: ${timer.size} vs ${timer.multiParagraph.intrinsics.maxIntrinsicWidth}", timer.fitsFully())
            if (p == PrayerName.DUHA || p == PrayerName.TAHAJJUD) save("prayer_hero_card", "waqt_${p.id}_${lang}_x$scale")
        }
        assertEquals("card height must be identical for every waqt and language: $heights", 1, heights.size)
    }

    @Test
    fun forbiddenCard_noClippedTimes_at320dp_1_3x_bothLanguages() {
        for (lang in listOf("en", "bn")) for (scale in listOf(1.0f, 1.3f)) for (desc in listOf(true, false)) {
            host(scale) { ForbiddenTimesCard(schedule, liveTime = schedule.sunriseMillis + 60_000, lang = lang, showDescriptions = desc) }
            val card = rule.onNodeWithTag("forbidden_times_card").getBoundsInRoot()
            assertTrue(card.right <= 320.dp)
            // 12-hour times present and never clipped
            val timeNodes = rule.onAllNodes(hasText("AM", substring = true) or hasText("PM", substring = true), useUnmergedTree = true)
            val count = timeNodes.fetchSemanticsNodes().size
            assertEquals("3 windows x (start, end)", 6, count)
            for (i in 0 until count) {
                val n = timeNodes[i]
                val l = n.layout()
                assertTrue("$lang/$scale time clipped: '${l.layoutInput.text}' size=${l.size} intrinsic=${l.multiParagraph.intrinsics.maxIntrinsicWidth}", l.fitsFully())
                assertTrue(n.getBoundsInRoot().right <= card.right)
            }
            save("forbidden_times_card", "forbidden_${lang}_x${scale}_${if (desc) "full" else "home"}")
        }
    }
}
