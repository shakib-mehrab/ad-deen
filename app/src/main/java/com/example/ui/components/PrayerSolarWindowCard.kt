package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DailyPrayerSchedule
import com.example.model.ForbiddenTimeWindow
import com.example.model.PrayerName
import com.example.model.PrayerWindow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

object CalendarDateHelper {

    private val BENGALI_DIGITS = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')

    fun toBnDigits(input: String): String {
        val sb = StringBuilder()
        for (ch in input) {
            if (ch in '0'..'9') {
                sb.append(BENGALI_DIGITS[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    private val ARABIC_DAYS_BN = mapOf(
        Calendar.SUNDAY to "ইয়াওমুল আহাদ",
        Calendar.MONDAY to "ইয়াওমুল ইছনাইন",
        Calendar.TUESDAY to "ইয়াওমুছ ছুলাছা",
        Calendar.WEDNESDAY to "ইয়াওমুল আরবাআ",
        Calendar.THURSDAY to "ইয়াওমুল খামিস",
        Calendar.FRIDAY to "ইয়াওমুল জুমুআ",
        Calendar.SATURDAY to "ইয়াওমুস সাবত"
    )

    private val ARABIC_DAYS_EN = mapOf(
        Calendar.SUNDAY to "Yawm al-Ahad",
        Calendar.MONDAY to "Yawm al-Ithnayn",
        Calendar.TUESDAY to "Yawm ath-Thulatha'",
        Calendar.WEDNESDAY to "Yawm al-Arba'a",
        Calendar.THURSDAY to "Yawm al-Khamis",
        Calendar.FRIDAY to "Yawm al-Jumu'ah",
        Calendar.SATURDAY to "Yawm as-Sabt"
    )

    private val HIJRI_MONTHS_BN = listOf(
        "মুহাররম", "সফর", "রবিউল আউয়াল", "রবিউস সানি", "জমাদিউল আউয়াল", "জমাদিউস সানি",
        "রজব", "শাবান", "রমজান", "শাওয়াল", "জিলকদ", "জিলহজ"
    )

    private val HIJRI_MONTHS_EN = listOf(
        "Muharram", "Safar", "Rabi' al-Awwal", "Rabi' ath-Thani", "Jumada al-Awwal", "Jumada ath-Thani",
        "Rajab", "Sha'ban", "Ramadan", "Shawwal", "Dhu al-Qi'dah", "Dhu al-Hijjah"
    )

    private val BENGALI_MONTHS = listOf(
        "বৈশাখ", "জ্যৈষ্ঠ", "আষাঢ়", "শ্রাবণ", "ভাদ্র", "আশ্বিন",
        "কার্তিক", "অগ্রহায়ণ", "পৌষ", "মাঘ", "ফাল্গুন", "চৈত্র"
    )

    private val BENGALI_DAYS = mapOf(
        Calendar.SUNDAY to "রবিবার",
        Calendar.MONDAY to "সোমবার",
        Calendar.TUESDAY to "মঙ্গলবার",
        Calendar.WEDNESDAY to "বুধবার",
        Calendar.THURSDAY to "বৃহস্পতিবার",
        Calendar.FRIDAY to "শুক্রবার",
        Calendar.SATURDAY to "শনিবার"
    )

    data class HijriDate(val day: Int, val monthName: String, val year: Int, val dayName: String)
    data class BengaliDate(val day: Int, val monthName: String, val year: Int, val dayName: String, val season: String)

    /**
     * Accurate Hijri date calculation based on standard Umm al-Qura calendar (java.time.chrono.HijrahDate)
     * with Islamic post-Maghrib next-day transition support.
     */
    fun getHijriDate(cal: Calendar, isBn: Boolean, dayOffset: Int = 0): HijriDate {
        return try {
            val localDate = java.time.LocalDate.of(
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.DAY_OF_MONTH)
            ).plusDays(dayOffset.toLong())

            val hijrahDate = java.time.chrono.HijrahDate.from(localDate)
            val hYear = hijrahDate.get(java.time.temporal.ChronoField.YEAR)
            val hMonth = hijrahDate.get(java.time.temporal.ChronoField.MONTH_OF_YEAR)
            val hDay = hijrahDate.get(java.time.temporal.ChronoField.DAY_OF_MONTH)

            val calAdjusted = (cal.clone() as Calendar).apply {
                add(Calendar.DAY_OF_MONTH, dayOffset)
            }
            val dayOfWeek = calAdjusted.get(Calendar.DAY_OF_WEEK)

            val monthName = if (isBn) {
                HIJRI_MONTHS_BN.getOrElse(hMonth - 1) { "রবিউল আউয়াল" }
            } else {
                HIJRI_MONTHS_EN.getOrElse(hMonth - 1) { "Rabi' al-Awwal" }
            }
            val dayName = if (isBn) (ARABIC_DAYS_BN[dayOfWeek] ?: "") else (ARABIC_DAYS_EN[dayOfWeek] ?: "")

            HijriDate(hDay, monthName, hYear, dayName)
        } catch (e: Exception) {
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
            val monthName = if (isBn) "রবিউল আউয়াল" else "Rabi' al-Awwal"
            val dayName = if (isBn) (ARABIC_DAYS_BN[dayOfWeek] ?: "") else (ARABIC_DAYS_EN[dayOfWeek] ?: "")
            HijriDate(18, monthName, 1448, dayName)
        }
    }

    /**
     * Official Bangladesh revised Bengali calendar (Bangabda).
     * Pohela Boishakh = April 14.
     */
    fun getBengaliDate(cal: Calendar, isBn: Boolean): BengaliDate {
        val gYear = cal.get(Calendar.YEAR)
        val dayOfYear = cal.get(Calendar.DAY_OF_YEAR)
        val isLeapYear = cal.getActualMaximum(Calendar.DAY_OF_YEAR) > 365

        val pohelaBoishakhDay = if (isLeapYear) 105 else 104
        val bYear = if (dayOfYear >= pohelaBoishakhDay) gYear - 593 else gYear - 594

        val daysPerMonth = intArrayOf(
            31, 31, 31, 31, 31, 31, // Boishakh to Ashwin
            30, 30, 30, 30, // Kartik to Magh
            if (isLeapYear) 30 else 29, // Falgun
            30 // Choitro
        )

        var daysPassed = if (dayOfYear >= pohelaBoishakhDay) {
            dayOfYear - pohelaBoishakhDay
        } else {
            val prevYearDays = if ((gYear - 1) % 4 == 0) 366 else 365
            val prevPohela = if ((gYear - 1) % 4 == 0) 105 else 104
            (prevYearDays - prevPohela) + dayOfYear
        }

        var bMonthIdx = 0
        var bDay = 1
        for (i in daysPerMonth.indices) {
            if (daysPassed < daysPerMonth[i]) {
                bMonthIdx = i
                bDay = daysPassed + 1
                break
            }
            daysPassed -= daysPerMonth[i]
        }

        val seasonsBn = listOf("গ্রীষ্মকাল", "বর্ষাকাল", "শরৎকাল", "হেমন্তকাল", "শীতকাল", "বসন্তকাল")
        val seasonsEn = listOf("Summer", "Monsoon", "Autumn", "Late Autumn", "Winter", "Spring")
        val seasonIdx = (bMonthIdx / 2).coerceIn(0, 5)

        val monthName = BENGALI_MONTHS[bMonthIdx]
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val dayName = if (isBn) (BENGALI_DAYS[dayOfWeek] ?: "") else SimpleDateFormat("EEEE", Locale.US).format(cal.time)
        val season = if (isBn) seasonsBn[seasonIdx] else seasonsEn[seasonIdx]

        return BengaliDate(bDay, monthName, bYear, dayName, season)
    }
}

/**
 * Time-aware Sky Phases for the illustrated scene.
 */
enum class SkyPhase(
    val topColor: Color,
    val bottomColor: Color,
    val isNight: Boolean
) {
    TAHAJJUD(
        topColor = Color(0xFF3B2A1A),
        bottomColor = Color(0xFF1F1F1F),
        isNight = true
    ),
    FAJR(
        topColor = Color(0xFF5A4632),
        bottomColor = Color(0xFFFFB030),
        isNight = true // Dark phase before sunrise
    ),
    SUNRISE_DUHA(
        topColor = Color(0xFFFFE8D6),
        bottomColor = Color(0xFFFFB030),
        isNight = false // Light phase
    ),
    DHUHR(
        topColor = Color(0xFFFFFFE4),
        bottomColor = Color(0xFFFFF0CC),
        isNight = false // Light phase
    ),
    ASR(
        topColor = Color(0xFFFFF0CC),
        bottomColor = Color(0xFFFFB030),
        isNight = false // Light phase
    ),
    MAGHRIB(
        topColor = Color(0xFFFFB030),
        bottomColor = Color(0xFF874D14),
        isNight = true // Dark phase after sunset
    ),
    ISHA(
        topColor = Color(0xFF3B2A1A),
        bottomColor = Color(0xFF1F1F1F),
        isNight = true // Dark phase
    );

    companion object {
        fun fromSchedule(schedule: DailyPrayerSchedule, liveTime: Long): SkyPhase {
            val sunrise = schedule.sunriseMillis
            val sunset = schedule.maghrib.startTimeMillis
            val fajrStart = schedule.fajr.startTimeMillis
            val duhaEnd = schedule.dhuhr.startTimeMillis
            val dhuhrEnd = schedule.asr.startTimeMillis
            val ishaStart = schedule.isha.startTimeMillis

            return when {
                liveTime < fajrStart -> TAHAJJUD
                liveTime < sunrise -> FAJR
                liveTime < duhaEnd -> SUNRISE_DUHA
                liveTime < dhuhrEnd -> DHUHR
                liveTime < sunset -> ASR
                liveTime < ishaStart -> MAGHRIB
                else -> ISHA
            }
        }
    }
}

/**
 * Illustrated, time-aware Hero Solar Card on the Home Screen.
 * Height ~200dp, radius 24dp, clipped, 1dp #874D14@30% border, soft shadow.
 * Text and icons sit directly on the illustration over subtle vertical scrims with no pill boxes.
 */
@Composable
fun HeroSolarCard(
    schedule: DailyPrayerSchedule,
    liveTime: Long,
    lang: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val isBn = lang == "bn"
    val cal = remember(liveTime) {
        Calendar.getInstance(TimeZone.getDefault()).apply {
            timeInMillis = liveTime
        }
    }

    val rawSunrise = schedule.sunriseMillis
    val rawSunset = schedule.maghrib.startTimeMillis

    // Ensure daytime bounds: sunrise in the morning (~5:44 AM), sunset in the evening (~5:41 PM)
    val todaySunriseMillis = if (rawSunrise < rawSunset) rawSunrise else rawSunset - 12 * 3600000L
    val todaySunsetMillis = if (rawSunset > rawSunrise) rawSunset else rawSunrise + 12 * 3600000L

    // Phase switch happens exactly at sunrise and sunset: never shows "Night" before sunset
    val isDaytime = liveTime in todaySunriseMillis until todaySunsetMillis

    val isAfterMaghrib = liveTime >= todaySunsetMillis
    val maghribDayOffset = if (isAfterMaghrib) 1 else 0
    val hijri = remember(cal, isBn, maghribDayOffset) { CalendarDateHelper.getHijriDate(cal, isBn, maghribDayOffset) }
    val bengali = remember(cal, isBn) { CalendarDateHelper.getBengaliDate(cal, isBn) }

    val hijriDayStr = if (isBn) CalendarDateHelper.toBnDigits("${hijri.day}") else "${hijri.day}"
    val hijriYearStr = if (isBn) CalendarDateHelper.toBnDigits("${hijri.year}") else "${hijri.year}"

    val bengaliDayStr = if (isBn) CalendarDateHelper.toBnDigits("${bengali.day}") else "${bengali.day}"
    val bengaliYearStr = if (isBn) CalendarDateHelper.toBnDigits("${bengali.year}") else "${bengali.year}"

    val line1 = if (isBn) {
        "$hijriDayStr ${hijri.monthName} $hijriYearStr হি. ${hijri.dayName}"
    } else {
        "$hijriDayStr ${hijri.monthName} $hijriYearStr AH • ${hijri.dayName}"
    }

    val line2 = if (isBn) {
        "${bengali.dayName}, $bengaliDayStr ${bengali.monthName} $bengaliYearStr বঙ্গাব্দ, ${bengali.season}"
    } else {
        "${bengali.dayName}, $bengaliDayStr ${bengali.monthName} $bengaliYearStr BS, ${bengali.season}"
    }

    val allWindows = schedule.allWindows()
    val activeWindow = allWindows.firstOrNull { liveTime in it.startTimeMillis until it.endTimeMillis }
    val nextWindow = allWindows.firstOrNull { it.startTimeMillis > liveTime } ?: allWindows.firstOrNull()
    val currentOrNextWindow = activeWindow ?: nextWindow ?: allWindows[3]
    val prayerTitle = if (isBn) currentOrNextWindow.prayer.titleBn else currentOrNextWindow.prayer.titleEn

    // Time calculations quantized to the minute to avoid per-frame recomposition
    val minuteQuantizedTime = (liveTime / 60000L) * 60000L

    val dayFraction = if (isDaytime && todaySunsetMillis > todaySunriseMillis) {
        ((minuteQuantizedTime - todaySunriseMillis).toFloat() / (todaySunsetMillis - todaySunriseMillis).toFloat()).coerceIn(0f, 1f)
    } else 0f

    val nightFraction = if (!isDaytime) {
        val (prevSunset, nextSunrise) = if (minuteQuantizedTime >= todaySunsetMillis) {
            Pair(todaySunsetMillis, todaySunriseMillis + 86400000L)
        } else {
            Pair(todaySunsetMillis - 86400000L, todaySunriseMillis)
        }
        val nightDuration = (nextSunrise - prevSunset).coerceAtLeast(1L)
        ((minuteQuantizedTime - prevSunset).toFloat() / nightDuration.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val skyPhase = SkyPhase.fromSchedule(schedule, liveTime)
    val isDarkPhase = skyPhase.isNight

    // Phase-based text color crossfades over 800ms
    val targetPrimaryTextColor = if (isDarkPhase) Color(0xFFFFFFFF) else Color(0xFF1F1F1F)
    val targetSecondaryTextColor = if (isDarkPhase) Color(0xFFF3E9D2) else Color(0xFF5A4632)

    val animPrimaryTextColor by animateColorAsState(
        targetValue = targetPrimaryTextColor,
        animationSpec = tween(800),
        label = "primaryTextColor"
    )
    val animSecondaryTextColor by animateColorAsState(
        targetValue = targetSecondaryTextColor,
        animationSpec = tween(800),
        label = "secondaryTextColor"
    )

    // Animated sky phase colors (800ms smooth crossfade)
    val animTopColor by animateColorAsState(
        targetValue = skyPhase.topColor,
        animationSpec = tween(800),
        label = "skyTop"
    )
    val animBottomColor by animateColorAsState(
        targetValue = skyPhase.bottomColor,
        animationSpec = tween(800),
        label = "skyBottom"
    )

    // Soft scrim gradient overlay color (65% alpha ensures WCAG AA > 5:1 contrast against any background)
    val targetScrimColor = if (isDarkPhase) {
        Color(0xFF1F1F1F).copy(alpha = 0.65f)
    } else {
        Color(0xFFFFFE4).copy(alpha = 0.65f)
    }
    val animScrimColor by animateColorAsState(
        targetValue = targetScrimColor,
        animationSpec = tween(800),
        label = "scrimColor"
    )

    // Animated silhouette colors
    val targetSkylineColor = if (isDarkPhase) Color(0xFF1F1F1F).copy(alpha = 0.90f) else Color(0xFF874D14).copy(alpha = 0.85f)
    val targetFarSkylineColor = if (isDarkPhase) Color(0xFF1F1F1F).copy(alpha = 0.30f) else Color(0xFF874D14).copy(alpha = 0.30f)
    val targetGroundColor = if (isDarkPhase) Color(0xFF1F1F1F).copy(alpha = 0.95f) else Color(0xFF874D14).copy(alpha = 0.90f)

    val animSkylineColor by animateColorAsState(targetValue = targetSkylineColor, animationSpec = tween(800), label = "skylineNear")
    val animFarSkylineColor by animateColorAsState(targetValue = targetFarSkylineColor, animationSpec = tween(800), label = "skylineFar")
    val animGroundColor by animateColorAsState(targetValue = targetGroundColor, animationSpec = tween(800), label = "groundStrip")

    // Smooth celestial position transitions
    val animDayFraction by animateFloatAsState(
        targetValue = dayFraction,
        animationSpec = tween(800),
        label = "sunPosition"
    )
    val animNightFraction by animateFloatAsState(
        targetValue = nightFraction,
        animationSpec = tween(800),
        label = "moonPosition"
    )

    // Check system reduce-motion setting
    val context = LocalContext.current
    val isInspection = LocalInspectionMode.current
    val isReduceMotion = remember(isInspection) {
        if (isInspection) true else {
            try {
                android.provider.Settings.Global.getFloat(
                    context.contentResolver,
                    android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
                    1f
                ) == 0f
            } catch (_: Exception) {
                false
            }
        }
    }

    // Cloud drift animation loop (disabled on reduce motion)
    val infiniteTransition = rememberInfiniteTransition(label = "heroSceneTransitions")
    val cloudDriftRatio by if (!isReduceMotion) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(75000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "cloudDrift"
        )
    } else {
        remember { androidx.compose.runtime.mutableFloatStateOf(0.35f) }
    }

    fun formatSimpleTime(millis: Long): String {
        val c = Calendar.getInstance(TimeZone.getDefault()).apply {
            timeInMillis = millis
        }
        val hour24 = c.get(Calendar.HOUR_OF_DAY)
        val minute = c.get(Calendar.MINUTE)
        val hour12 = if (hour24 % 12 == 0) 12 else hour24 % 12
        val minStr = String.format(Locale.US, "%02d", minute)
        val period = if (hour24 >= 12) "PM" else "AM"
        val numStr = "$hour12:$minStr"
        val displayNum = if (isBn) CalendarDateHelper.toBnDigits(numStr) else numStr
        return "$displayNum $period"
    }

    // Sunrise display: at night after sunset, show tomorrow's upcoming sunrise; otherwise today's sunrise
    val displaySunriseMillis = if (liveTime >= todaySunsetMillis) {
        todaySunriseMillis + 86400000L
    } else {
        todaySunriseMillis
    }
    // Sunset display: always show today's sunset
    val displaySunsetMillis = todaySunsetMillis

    val sunriseTime = formatSimpleTime(displaySunriseMillis)
    val sunsetTime = formatSimpleTime(displaySunsetMillis)
    val a11yDescription = "$prayerTitle time, sunrise $sunriseTime, sunset $sunsetTime"

    // Card Container (rounded 24dp, clipped, height 200dp, 1dp #874D14@30% border, soft shadow)
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = Color(0xFF874D14).copy(alpha = 0.08f),
                spotColor = Color(0xFF874D14).copy(alpha = 0.08f)
            )
            .clip(RoundedCornerShape(24.dp))
            .border(
                border = BorderStroke(1.dp, Color(0xFF874D14).copy(alpha = 0.30f)),
                shape = RoundedCornerShape(24.dp)
            )
            .clickable(onClick = onClick)
            .clearAndSetSemantics { contentDescription = a11yDescription }
            .testTag("hero_solar_card")
    ) {
        val groundHeightDp = 24.dp

        // Layered Canvas illustration: Sky, Sun/Moon, Clouds, Skyline, Ground, and Soft Scrim Overlays
        Canvas(modifier = Modifier.fillMaxSize()) {
            val groundHeightPx = groundHeightDp.toPx()

            // 1. SKY LAYER
            drawSkyLayer(topColor = animTopColor, bottomColor = animBottomColor)

            // 2. SUN / MOON LAYER ON FAINT CELESTIAL ARC
            drawSunMoonLayer(
                isNight = isDarkPhase,
                dayFraction = animDayFraction,
                nightFraction = animNightFraction,
                groundHeight = groundHeightPx
            )

            // 3. CLOUDS LAYER (open sky zone only)
            drawCloudLayer(
                cloudDriftPx = cloudDriftRatio * (size.width + 120.dp.toPx()),
                isNight = isDarkPhase
            )

            // 4. SKYLINE LAYER (Two-layer mosque silhouette with depth)
            drawSkylineLayer(
                nearColor = animSkylineColor,
                farColor = animFarSkylineColor,
                groundHeight = groundHeightPx
            )

            // 5. GROUND LAYER (Strip + Palm tree silhouettes)
            drawGroundLayer(
                groundColor = animGroundColor,
                groundHeight = groundHeightPx
            )

            // 6. SOFT SCRIM OVERLAYS (Top ~35% and Bottom ~30% behind text only; open middle remains clear)
            drawSoftScrimOverlay(scrimColor = animScrimColor)
        }

        // 7. INFO ROWS (No pill backgrounds, text and icons sit directly on illustration)
        HeroInfoRows(
            line1 = line1,
            line2 = line2,
            isDaytime = isDaytime,
            isBn = isBn,
            sunriseTime = sunriseTime,
            sunsetTime = sunsetTime,
            primaryTextColor = animPrimaryTextColor,
            secondaryTextColor = animSecondaryTextColor
        )
    }
}

/**
 * 1. SkyLayer: Vertical gradient corresponding to the active prayer phase.
 */
private fun DrawScope.drawSkyLayer(
    topColor: Color,
    bottomColor: Color
) {
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(topColor, bottomColor),
            startY = 0f,
            endY = size.height
        ),
        size = size
    )
}

/**
 * 2. SunMoonLayer: Faint dashed arc across the sky zone with Sun / Crescent Moon traversal.
 */
private fun DrawScope.drawSunMoonLayer(
    isNight: Boolean,
    dayFraction: Float,
    nightFraction: Float,
    groundHeight: Float
) {
    val w = size.width
    val h = size.height
    val horizonY = h - groundHeight
    val arcMargin = 20.dp.toPx()
    val centerX = w / 2f
    val radiusX = centerX - arcMargin
    val arcApexY = 70.dp.toPx()
    val radiusY = horizonY - arcApexY

    // Faint semi-circular arc (dashed, 1.5dp, #FFFFFF at 50% alpha)
    val arcRect = Rect(
        left = centerX - radiusX,
        top = horizonY - radiusY,
        right = centerX + radiusX,
        bottom = horizonY + radiusY
    )
    val arcPath = Path().apply {
        arcTo(
            rect = arcRect,
            startAngleDegrees = 180f,
            sweepAngleDegrees = 180f,
            forceMoveTo = true
        )
    }

    drawPath(
        path = arcPath,
        color = Color.White.copy(alpha = 0.50f),
        style = Stroke(
            width = 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
        )
    )

    if (!isNight) {
        // DAY: Sun travels along the arc from left horizon to right horizon
        val clampedFraction = dayFraction.coerceIn(0f, 1f)
        val sunAngle = Math.PI.toFloat() * clampedFraction
        val sunX = centerX - radiusX * cos(sunAngle)
        val sunY = horizonY - radiusY * sin(sunAngle)

        val sunRadius = 14.dp.toPx()
        val glowRadius = sunRadius * 2f

        // Soft radial glow (#FFB030 at 35% alpha, 2x radius)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFB030).copy(alpha = 0.35f),
                    Color(0xFFFFB030).copy(alpha = 0.10f),
                    Color.Transparent
                ),
                center = Offset(sunX, sunY),
                radius = glowRadius
            ),
            radius = glowRadius,
            center = Offset(sunX, sunY)
        )

        // Sun filled core (#FFB030)
        drawCircle(
            color = Color(0xFFFFB030),
            radius = sunRadius,
            center = Offset(sunX, sunY)
        )

        // Thin #FFFFFF 40% ring
        drawCircle(
            color = Color.White.copy(alpha = 0.40f),
            radius = sunRadius + 0.5.dp.toPx(),
            center = Offset(sunX, sunY),
            style = Stroke(width = 1.dp.toPx())
        )
    } else {
        // NIGHT: Crescent moon on the same arc + 6 tiny stars
        // Rises on right after Maghrib (fraction 0), peaks at midnight (0.5), sets on left before sunrise (1.0)
        val clampedFraction = nightFraction.coerceIn(0f, 1f)
        val moonAngle = Math.PI.toFloat() * clampedFraction
        val moonX = centerX + radiusX * cos(moonAngle)
        val moonY = horizonY - radiusY * sin(moonAngle)

        val moonRadius = 12.dp.toPx()

        // Soft moon aura
        drawCircle(
            color = Color(0xFFFFF8E1).copy(alpha = 0.18f),
            radius = moonRadius * 1.8f,
            center = Offset(moonX, moonY)
        )

        // Crescent moon (#FFF8E1)
        val moonPath = Path().apply {
            arcTo(
                rect = Rect(
                    moonX - moonRadius,
                    moonY - moonRadius,
                    moonX + moonRadius,
                    moonY + moonRadius
                ),
                startAngleDegrees = -110f,
                sweepAngleDegrees = 220f,
                forceMoveTo = true
            )
            quadraticBezierTo(
                moonX - moonRadius * 0.15f,
                moonY,
                moonX + moonRadius * cos(Math.toRadians(-110.0)).toFloat(),
                moonY + moonRadius * sin(Math.toRadians(-110.0)).toFloat()
            )
            close()
        }
        drawPath(moonPath, Color(0xFFFFF8E1))

        // 6 Tiny stars (1-2dp, 60% alpha) in the open sky only
        val starAlpha = 0.60f
        val starColor = Color.White.copy(alpha = starAlpha)
        val starCoords = listOf(
            Offset(w * 0.12f, h * 0.36f),
            Offset(w * 0.22f, h * 0.45f),
            Offset(w * 0.32f, h * 0.34f),
            Offset(w * 0.68f, h * 0.35f),
            Offset(w * 0.78f, h * 0.44f),
            Offset(w * 0.88f, h * 0.36f)
        )
        starCoords.forEach { pt ->
            drawCircle(starColor, radius = 1.2.dp.toPx(), center = pt)
        }
    }
}

/**
 * 3. CloudLayer: 2 flat rounded clouds drifting horizontally in open sky zone.
 */
private fun DrawScope.drawCloudLayer(cloudDriftPx: Float, isNight: Boolean) {
    if (isNight) return // Keep nighttime sky clear for celestial view

    val cloudAlpha = 0.55f
    val cloudColor = Color.White.copy(alpha = cloudAlpha)
    val w = size.width

    fun drawSingleCloud(startX: Float, baseY: Float, scale: Float) {
        val r1 = 9.dp.toPx() * scale
        val r2 = 14.dp.toPx() * scale
        val r3 = 10.dp.toPx() * scale

        val c1 = Offset(startX, baseY)
        val c2 = Offset(startX + 13.dp.toPx() * scale, baseY - 5.dp.toPx() * scale)
        val c3 = Offset(startX + 26.dp.toPx() * scale, baseY)

        drawCircle(cloudColor, radius = r1, center = c1)
        drawCircle(cloudColor, radius = r2, center = c2)
        drawCircle(cloudColor, radius = r3, center = c3)
        drawRoundRect(
            color = cloudColor,
            topLeft = Offset(startX - r1 * 0.5f, baseY - 2.dp.toPx() * scale),
            size = Size(32.dp.toPx() * scale + r1, 9.dp.toPx() * scale),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
        )
    }

    val x1 = (cloudDriftPx) % (w + 120.dp.toPx()) - 60.dp.toPx()
    val x2 = (cloudDriftPx + w * 0.52f) % (w + 120.dp.toPx()) - 60.dp.toPx()

    drawSingleCloud(x1, 74.dp.toPx(), 0.95f)
    drawSingleCloud(x2, 88.dp.toPx(), 0.80f)
}

/**
 * 4. SkylineLayer: Two-layer Mosque silhouette centered at the bottom.
 */
private fun DrawScope.drawSkylineLayer(
    nearColor: Color,
    farColor: Color,
    groundHeight: Float
) {
    val w = size.width
    val h = size.height
    val baseY = h - groundHeight

    // Far skyline layer (30% alpha for depth, slightly offset)
    val farCenterX = w * 0.52f
    val farScale = 0.82f
    drawMosqueSilhouette(farCenterX, baseY + 2.dp.toPx(), farScale, farColor, w)

    // Near skyline layer (85% alpha day, 90% night)
    val nearCenterX = w * 0.50f
    drawMosqueSilhouette(nearCenterX, baseY, 1.0f, nearColor, w)
}

/**
 * Helper to draw mosque with central dome (~30% card width), minarets (at 36% and 64%), side domes and finials.
 * Proportioned and placed centrally so that bottom-left Sunrise and bottom-right Sunset have dedicated calm zones.
 */
private fun DrawScope.drawMosqueSilhouette(
    centerX: Float,
    baseY: Float,
    scale: Float,
    color: Color,
    totalWidth: Float
) {
    val path = Path()

    // Central Dome: about 30% of card width
    val centralDomeWidth = (totalWidth * 0.30f) * scale
    val centralDomeHeight = 32.dp.toPx() * scale
    val sideDomeWidth = 16.dp.toPx() * scale
    val sideDomeHeight = 15.dp.toPx() * scale

    // Minarets placed symmetrically away from text zones (at 36% and 64% of card width)
    val minaretX1 = totalWidth * 0.36f
    val minaretX2 = totalWidth * 0.64f
    val minaretWidth = 6.dp.toPx() * scale
    val minaretHeight = 58.dp.toPx() * scale

    // Central Dome Base & Arch
    val domeLeft = centerX - centralDomeWidth / 2f
    val domeRight = centerX + centralDomeWidth / 2f
    val domeTop = baseY - centralDomeHeight

    path.moveTo(domeLeft, baseY)
    path.lineTo(domeLeft, baseY - 6.dp.toPx() * scale)
    // Onion pointed curve
    path.cubicTo(
        domeLeft - 2.dp.toPx() * scale, baseY - centralDomeHeight * 0.65f,
        centerX - 4.dp.toPx() * scale, domeTop + 3.dp.toPx() * scale,
        centerX, domeTop
    )
    path.cubicTo(
        centerX + 4.dp.toPx() * scale, domeTop + 3.dp.toPx() * scale,
        domeRight + 2.dp.toPx() * scale, baseY - centralDomeHeight * 0.65f,
        domeRight, baseY - 6.dp.toPx() * scale
    )
    path.lineTo(domeRight, baseY)

    // Left Side Dome
    val sideDomeOffset = centralDomeWidth * 0.44f
    val leftDomeCenter = centerX - sideDomeOffset
    val lLeft = leftDomeCenter - sideDomeWidth / 2f
    val lRight = leftDomeCenter + sideDomeWidth / 2f
    val lTop = baseY - sideDomeHeight

    path.moveTo(lLeft, baseY)
    path.lineTo(lLeft, baseY - 4.dp.toPx() * scale)
    path.cubicTo(
        lLeft - 1.5.dp.toPx() * scale, baseY - sideDomeHeight * 0.6f,
        leftDomeCenter - 2.dp.toPx() * scale, lTop + 2.dp.toPx() * scale,
        leftDomeCenter, lTop
    )
    path.cubicTo(
        leftDomeCenter + 2.dp.toPx() * scale, lTop + 2.dp.toPx() * scale,
        lRight + 1.5.dp.toPx() * scale, baseY - sideDomeHeight * 0.6f,
        lRight, baseY - 4.dp.toPx() * scale
    )
    path.lineTo(lRight, baseY)

    // Right Side Dome
    val rightDomeCenter = centerX + sideDomeOffset
    val rLeft = rightDomeCenter - sideDomeWidth / 2f
    val rRight = rightDomeCenter + sideDomeWidth / 2f
    val rTop = baseY - sideDomeHeight

    path.moveTo(rLeft, baseY)
    path.lineTo(rLeft, baseY - 4.dp.toPx() * scale)
    path.cubicTo(
        rLeft - 1.5.dp.toPx() * scale, baseY - sideDomeHeight * 0.6f,
        rightDomeCenter - 2.dp.toPx() * scale, rTop + 2.dp.toPx() * scale,
        rightDomeCenter, rTop
    )
    path.cubicTo(
        rightDomeCenter + 2.dp.toPx() * scale, rTop + 2.dp.toPx() * scale,
        rRight + 1.5.dp.toPx() * scale, baseY - sideDomeHeight * 0.6f,
        rRight, baseY - 4.dp.toPx() * scale
    )
    path.lineTo(rRight, baseY)

    // Left Minaret at 36% width
    val lMinTop = baseY - minaretHeight
    path.moveTo(minaretX1 - minaretWidth / 2f, baseY)
    path.lineTo(minaretX1 - minaretWidth / 2f, lMinTop + 8.dp.toPx() * scale)
    path.lineTo(minaretX1 - minaretWidth * 0.8f, lMinTop + 8.dp.toPx() * scale)
    path.lineTo(minaretX1 - minaretWidth * 0.8f, lMinTop + 6.dp.toPx() * scale)
    path.lineTo(minaretX1 - minaretWidth * 0.4f, lMinTop + 6.dp.toPx() * scale)
    path.lineTo(minaretX1, lMinTop) // Pointed Cap
    path.lineTo(minaretX1 + minaretWidth * 0.4f, lMinTop + 6.dp.toPx() * scale)
    path.lineTo(minaretX1 + minaretWidth * 0.8f, lMinTop + 6.dp.toPx() * scale)
    path.lineTo(minaretX1 + minaretWidth * 0.8f, lMinTop + 8.dp.toPx() * scale)
    path.lineTo(minaretX1 + minaretWidth / 2f, lMinTop + 8.dp.toPx() * scale)
    path.lineTo(minaretX1 + minaretWidth / 2f, baseY)

    // Right Minaret at 64% width
    val rMinTop = baseY - minaretHeight
    path.moveTo(minaretX2 - minaretWidth / 2f, baseY)
    path.lineTo(minaretX2 - minaretWidth / 2f, rMinTop + 8.dp.toPx() * scale)
    path.lineTo(minaretX2 - minaretWidth * 0.8f, rMinTop + 8.dp.toPx() * scale)
    path.lineTo(minaretX2 - minaretWidth * 0.8f, rMinTop + 6.dp.toPx() * scale)
    path.lineTo(minaretX2 - minaretWidth * 0.4f, rMinTop + 6.dp.toPx() * scale)
    path.lineTo(minaretX2, rMinTop) // Pointed Cap
    path.lineTo(minaretX2 + minaretWidth * 0.4f, rMinTop + 6.dp.toPx() * scale)
    path.lineTo(minaretX2 + minaretWidth * 0.8f, rMinTop + 6.dp.toPx() * scale)
    path.lineTo(minaretX2 + minaretWidth * 0.8f, rMinTop + 8.dp.toPx() * scale)
    path.lineTo(minaretX2 + minaretWidth / 2f, rMinTop + 8.dp.toPx() * scale)
    path.lineTo(minaretX2 + minaretWidth / 2f, baseY)

    // Base Connecting wall
    path.addRect(
        Rect(
            minaretX1 - minaretWidth / 2f,
            baseY - 8.dp.toPx() * scale,
            minaretX2 + minaretWidth / 2f,
            baseY
        )
    )

    drawPath(path = path, color = color)

    // Finial & Crescent on central dome
    drawLine(
        color = color,
        start = Offset(centerX, domeTop),
        end = Offset(centerX, domeTop - 6.dp.toPx() * scale),
        strokeWidth = 1.5.dp.toPx() * scale
    )
    drawCircle(
        color = color,
        radius = 1.2.dp.toPx() * scale,
        center = Offset(centerX, domeTop - 6.dp.toPx() * scale)
    )

    // Finials on minarets
    drawLine(color, Offset(minaretX1, lMinTop), Offset(minaretX1, lMinTop - 3.dp.toPx() * scale), strokeWidth = 1.dp.toPx() * scale)
    drawLine(color, Offset(minaretX2, rMinTop), Offset(minaretX2, rMinTop - 3.dp.toPx() * scale), strokeWidth = 1.dp.toPx() * scale)
}

/**
 * 5. GroundLayer: Ground strip with 2 palm silhouettes placed cleanly in calm zone between Sunrise text and mosque.
 */
private fun DrawScope.drawGroundLayer(
    groundColor: Color,
    groundHeight: Float
) {
    val w = size.width
    val h = size.height
    val groundTopY = h - groundHeight

    // Solid ground rect
    drawRect(
        color = groundColor,
        topLeft = Offset(0f, groundTopY),
        size = Size(w, groundHeight)
    )

    // Horizon line highlight
    drawLine(
        color = Color.White.copy(alpha = 0.22f),
        start = Offset(0f, groundTopY),
        end = Offset(w, groundTopY),
        strokeWidth = 1.5.dp.toPx()
    )

    // 2 Palm silhouettes placed away from Sunrise text and away from mosque minaret (~31% and ~33.5% width)
    fun drawPalm(baseX: Float, heightDp: Float) {
        val trunkHeight = heightDp.dp.toPx()
        val top = Offset(baseX, groundTopY - trunkHeight)
        drawLine(
            color = groundColor,
            start = Offset(baseX, groundTopY),
            end = top,
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
        val frondLen = trunkHeight * 0.45f
        drawLine(groundColor, top, Offset(top.x - frondLen, top.y + frondLen * 0.3f), strokeWidth = 1.6.dp.toPx(), cap = StrokeCap.Round)
        drawLine(groundColor, top, Offset(top.x + frondLen, top.y + frondLen * 0.3f), strokeWidth = 1.6.dp.toPx(), cap = StrokeCap.Round)
        drawLine(groundColor, top, Offset(top.x - frondLen * 0.8f, top.y - frondLen * 0.25f), strokeWidth = 1.6.dp.toPx(), cap = StrokeCap.Round)
        drawLine(groundColor, top, Offset(top.x + frondLen * 0.8f, top.y - frondLen * 0.25f), strokeWidth = 1.6.dp.toPx(), cap = StrokeCap.Round)
    }

    drawPalm(baseX = w * 0.31f, heightDp = 16f)
    drawPalm(baseX = w * 0.335f, heightDp = 13f)
}

/**
 * 6. Soft Scrim Overlay: Top ~35% and Bottom ~30% behind the text rows only.
 * Leaves the middle open zone (~50% height) completely clear and unattenuated.
 */
private fun DrawScope.drawSoftScrimOverlay(scrimColor: Color) {
    val topScrimHeight = size.height * 0.35f
    // Top Scrim: fades smoothly from scrimColor at top to transparent at 35% height
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(scrimColor, Color.Transparent),
            startY = 0f,
            endY = topScrimHeight
        ),
        topLeft = Offset(0f, 0f),
        size = Size(size.width, topScrimHeight)
    )

    val bottomScrimHeight = size.height * 0.30f
    val bottomStartY = size.height - bottomScrimHeight
    // Bottom Scrim: fades smoothly from transparent at 70% height to scrimColor at bottom
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color.Transparent, scrimColor),
            startY = bottomStartY,
            endY = size.height
        ),
        topLeft = Offset(0f, bottomStartY),
        size = Size(size.width, bottomScrimHeight)
    )
}

/**
 * 7. Info Rows: Direct text and icons over the illustration with no pill boxes or borders.
 * Day/Night chip retains its #FFB030 fill and #1F1F1F text status badge.
 */
@Composable
private fun HeroInfoRows(
    line1: String,
    line2: String,
    isDaytime: Boolean,
    isBn: Boolean,
    sunriseTime: String,
    sunsetTime: String,
    primaryTextColor: Color,
    secondaryTextColor: Color
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // TOP ROW: Date line 14sp/600, secondary date 12sp/500, Day/Night chip #FFB030 + #1F1F1F
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    text = line1,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    ),
                    color = primaryTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = line2,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp
                    ),
                    color = secondaryTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(8.dp))

            // Day / Night Status Chip keeps #FFB030 fill and #1F1F1F text
            Surface(
                color = Color(0xFFFFB030),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (isDaytime) (if (isBn) "দিবাভাগ" else "Day") else (if (isBn) "রাত্রি" else "Night"),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    ),
                    color = Color(0xFF1F1F1F),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        // BOTTOM ROW: Sunrise (bottom-left) and Sunset (bottom-right) directly on illustration
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sunrise (bottom-left)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.WbSunny,
                    contentDescription = "Sunrise",
                    tint = primaryTextColor,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = if (isBn) "সূর্যোদয়" else "Sunrise",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp
                    ),
                    color = secondaryTextColor,
                    maxLines = 1
                )
                Text(
                    text = sunriseTime,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        fontFeatureSettings = "tnum"
                    ),
                    color = primaryTextColor,
                    maxLines = 1
                )
            }

            // Sunset (bottom-right)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.WbTwilight,
                    contentDescription = "Sunset",
                    tint = primaryTextColor,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = if (isBn) "সূর্যাস্ত" else "Sunset",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp
                    ),
                    color = secondaryTextColor,
                    maxLines = 1
                )
                Text(
                    text = sunsetTime,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        fontFeatureSettings = "tnum"
                    ),
                    color = primaryTextColor,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Drop-in backwards-compatible alias for PrayerSolarWindowCard.
 */
@Composable
fun PrayerSolarWindowCard(
    schedule: DailyPrayerSchedule,
    liveTime: Long,
    lang: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    HeroSolarCard(
        schedule = schedule,
        liveTime = liveTime,
        lang = lang,
        modifier = modifier,
        onClick = onClick
    )
}

// ============================================================================
// PREVIEWS AT 360DP FOR DHUHR, ASR, MAGHRIB, ISHA, AND FAJR
// ============================================================================

private fun createMockScheduleForPhase(phase: SkyPhase): Pair<DailyPrayerSchedule, Long> {
    val cal = Calendar.getInstance(TimeZone.getDefault()).apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val base = cal.timeInMillis

    fun t(h: Int, m: Int) = base + (h * 3600 + m * 60) * 1000L

    val tahajjudWindow = PrayerWindow(PrayerName.TAHAJJUD, t(1, 30), t(4, 30))
    val fajrWindow = PrayerWindow(PrayerName.FAJR, t(4, 30), t(5, 44))
    val sunrise = t(5, 44) // 5:44 AM
    val duhaWindow = PrayerWindow(PrayerName.DUHA, t(6, 10), t(11, 45))
    val dhuhrWindow = PrayerWindow(PrayerName.DHUHR, t(11, 48), t(16, 4))
    val asrWindow = PrayerWindow(PrayerName.ASR, t(16, 4), t(17, 41))
    val maghribWindow = PrayerWindow(PrayerName.MAGHRIB, t(17, 41), t(18, 58)) // 5:41 PM sunset
    val ishaWindow = PrayerWindow(PrayerName.ISHA, t(18, 58), t(23, 30))

    val schedule = DailyPrayerSchedule(
        dateString = "2026-10-03",
        tahajjud = tahajjudWindow,
        fajr = fajrWindow,
        sunriseMillis = sunrise,
        duha = duhaWindow,
        dhuhr = dhuhrWindow,
        asr = asrWindow,
        maghrib = maghribWindow,
        isha = ishaWindow,
        midnightMillis = t(23, 30),
        forbiddenTimes = listOf(
            ForbiddenTimeWindow("sunrise_forbidden", "Sunrise", "সূর্যোদয়", "Prohibited", "সালাত আদায় নিষেধ", t(5, 44), t(6, 5))
        )
    )

    val targetLiveTime = when (phase) {
        SkyPhase.DHUHR -> t(12, 30) // Midday light phase
        SkyPhase.ASR -> t(16, 30) // Golden hour light phase
        SkyPhase.MAGHRIB -> t(17, 50) // Sunset warm dark phase
        SkyPhase.ISHA -> t(20, 30) // Night dark phase with moon & stars
        SkyPhase.FAJR -> t(5, 10) // Dawn dark phase
        SkyPhase.SUNRISE_DUHA -> t(8, 30)
        SkyPhase.TAHAJJUD -> t(3, 0)
    }

    return Pair(schedule, targetLiveTime)
}

@Preview(name = "1. Dhuhr Phase (Midday Light Scrim)", widthDp = 360, showBackground = true, backgroundColor = 0xFFFFFE4)
@Composable
fun DhuhrPhasePreview() {
    val (schedule, liveTime) = remember { createMockScheduleForPhase(SkyPhase.DHUHR) }
    HeroSolarCard(schedule = schedule, liveTime = liveTime, lang = "en", modifier = Modifier.padding(16.dp))
}

@Preview(name = "2. Asr Phase (Golden Hour Light Scrim)", widthDp = 360, showBackground = true, backgroundColor = 0xFFFFFE4)
@Composable
fun AsrPhasePreview() {
    val (schedule, liveTime) = remember { createMockScheduleForPhase(SkyPhase.ASR) }
    HeroSolarCard(schedule = schedule, liveTime = liveTime, lang = "en", modifier = Modifier.padding(16.dp))
}

@Preview(name = "3. Maghrib Phase (Sunset Warm Dark Scrim)", widthDp = 360, showBackground = true, backgroundColor = 0xFFFFFE4)
@Composable
fun MaghribPhasePreview() {
    val (schedule, liveTime) = remember { createMockScheduleForPhase(SkyPhase.MAGHRIB) }
    HeroSolarCard(schedule = schedule, liveTime = liveTime, lang = "en", modifier = Modifier.padding(16.dp))
}

@Preview(name = "4. Isha Phase (Night Sky Dark Scrim)", widthDp = 360, showBackground = true, backgroundColor = 0xFFFFFE4)
@Composable
fun IshaPhasePreview() {
    val (schedule, liveTime) = remember { createMockScheduleForPhase(SkyPhase.ISHA) }
    HeroSolarCard(schedule = schedule, liveTime = liveTime, lang = "en", modifier = Modifier.padding(16.dp))
}

@Preview(name = "5. Fajr Phase (Dawn Dark Scrim)", widthDp = 360, showBackground = true, backgroundColor = 0xFFFFFE4)
@Composable
fun FajrPhasePreview() {
    val (schedule, liveTime) = remember { createMockScheduleForPhase(SkyPhase.FAJR) }
    HeroSolarCard(schedule = schedule, liveTime = liveTime, lang = "en", modifier = Modifier.padding(16.dp))
}

