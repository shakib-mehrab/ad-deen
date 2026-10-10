package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DailyPrayerSchedule
import com.example.model.PrayerName
import com.example.model.PrayerWindow
import com.example.ui.MainViewModel
import com.example.ui.Strings
import com.example.ui.components.PrayerSolarWindowCard
import com.example.ui.theme.JamatGreen
import com.example.ui.theme.PerfectGold
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToPrayerTimes: () -> Unit,
    onNavigateToTracker: () -> Unit,
    onNavigateToAzkhar: (tabIndex: Int) -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    val schedule by viewModel.prayerSchedule.collectAsState()
    val tomorrowSchedule by viewModel.tomorrowSchedule.collectAsState()
    val todayRecord by viewModel.todayRecord.collectAsState()
    val currentStreak by viewModel.currentStreak.collectAsState()
    val longestStreak by viewModel.longestStreak.collectAsState()
    val liveTime by viewModel.liveTimeMillis.collectAsState()
    val currentUser by viewModel.currentUserAccount.collectAsState()

    val lang = settings.language

    val scrollState = rememberScrollState()

    // Find active prayer window or next upcoming window
    // Today's windows followed by tomorrow's, so after Isha the next waqt is tomorrow's (never a past one).
    val allWindows = schedule.allWindows() + tomorrowSchedule.allWindows()
    val activeWindow = allWindows.firstOrNull { liveTime in it.startTimeMillis until it.endTimeMillis }
    val nextWindow = allWindows.firstOrNull { it.startTimeMillis > liveTime }

    val targetWindow = activeWindow ?: nextWindow
    val isWindowActive = activeWindow != null

    val timeRemainingMillis = if (targetWindow != null) {
        if (isWindowActive) targetWindow.endTimeMillis - liveTime else targetWindow.startTimeMillis - liveTime
    } else 0L

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Header Banner: Logo and Name together
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            com.example.ui.components.AdDeenBrandHeader(
                logoSize = 44.dp,
                showSubtitle = true,
                subtitleText = "${settings.district} - ${settings.calcMethod}",
                textColor = com.example.ui.theme.AppColors.Secondary
            )

            // Streak pill badge: #FFB030 fill + #1F1F1F text + flame icon in #1F1F1F
            Surface(
                color = com.example.ui.theme.AppColors.Primary,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.testTag("streak_badge")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = "Streak",
                        tint = com.example.ui.theme.AppColors.TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "$currentStreak ${Strings.t("days", lang)}",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            fontFeatureSettings = "tnum"
                        ),
                        color = com.example.ui.theme.AppColors.TextPrimary
                    )
                }
            }
        }

        // Authenticated user greeting banner clearly displaying user name
        if (currentUser != null) {
            val effectiveUserName = currentUser?.effectiveName ?: ""
            Surface(
                color = com.example.ui.theme.AppColors.Surface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, com.example.ui.theme.AppColors.BorderSubtle),
                shadowElevation = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("authenticated_user_banner")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    com.example.ui.components.UserAvatar(name = effectiveUserName, size = 44.dp)
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = effectiveUserName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = com.example.ui.theme.AppColors.TextPrimary,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("authenticated_user_name_text")
                        )
                        Text(
                            text = if (currentUser?.isCloudConnected == true)
                                (if (lang == "bn") "ক্লাউড সিঙ্ক চালু • ${currentUser?.email.orEmpty()}" else "Cloud sync on • ${currentUser?.email.orEmpty()}")
                            else
                                currentUser?.email.orEmpty(),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = com.example.ui.theme.AppColors.TextSecondary,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Exact Solar Timing Window Card as requested in user image
        PrayerSolarWindowCard(
            schedule = schedule,
            liveTime = liveTime,
            lang = lang,
            onClick = onNavigateToPrayerTimes
        )

        // Live Prayer Countdown Hero Card
        if (targetWindow != null) {
            PrayerCountdownHeroCard(
                window = targetWindow,
                isActive = isWindowActive,
                remainingMillis = timeRemainingMillis,
                lang = lang,
                onClick = onNavigateToPrayerTimes
            )
        }

        // Measured Forbidden Prayer Times Card
        com.example.ui.components.ForbiddenTimesCard(
            schedule = schedule,
            liveTime = liveTime,
            lang = lang,
            showDescriptions = false,
            testTag = "home_forbidden_times_card",
            onClick = onNavigateToPrayerTimes
        )

        // Today's Score Circle & Perfect Day Card
        TodayScoreCard(
            score = todayRecord.score,
            isPerfect = todayRecord.perfect,
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            lang = lang,
            onClick = onNavigateToTracker
        )

        // Daily Quran checklist item (today): tap toggles Done / Not done, with Undo snackbar.
        QuranChecklistCard(
            done = todayRecord.quran,
            lang = lang,
            onToggle = { viewModel.toggleQuran(onToday = true) }
        )

        // Quick Azkhar Navigation
        Text(
            text = Strings.t("quick_azkhar", lang),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            ),
            color = com.example.ui.theme.AppColors.Secondary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AzkharQuickCard(
                title = Strings.t("morning_azkhar", lang),
                isDone = todayRecord.sections.morning,
                icon = Icons.Outlined.WbSunny,
                modifier = Modifier.weight(1f),
                onClick = { onNavigateToAzkhar(0) }
            )
            AzkharQuickCard(
                title = Strings.t("evening_azkhar", lang),
                isDone = todayRecord.sections.evening,
                icon = Icons.Outlined.NightsStay,
                modifier = Modifier.weight(1f),
                onClick = { onNavigateToAzkhar(1) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AzkharQuickCard(
                title = Strings.t("sleep_azkhar", lang),
                isDone = todayRecord.sections.sleep,
                icon = Icons.Outlined.Bedtime,
                modifier = Modifier.weight(1f),
                onClick = { onNavigateToAzkhar(2) }
            )
            AzkharQuickCard(
                title = Strings.t("wake_azkhar", lang),
                isDone = todayRecord.sections.wake,
                icon = Icons.Outlined.Alarm,
                modifier = Modifier.weight(1f),
                onClick = { onNavigateToAzkhar(3) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AzkharQuickCard(
                title = Strings.t("daily_zikr", lang),
                isDone = todayRecord.sections.zikr,
                icon = Icons.Outlined.TouchApp,
                modifier = Modifier.weight(1f),
                onClick = { onNavigateToAzkhar(5) }
            )
            AzkharQuickCard(
                title = Strings.t("names_99", lang),
                isDone = false,
                icon = Icons.Outlined.AutoStories,
                modifier = Modifier.weight(1f),
                onClick = { onNavigateToAzkhar(4) }
            )
        }

        // Hadith / quote card: #FFF0CC background, 4dp left border #874D14, heading in #874D14 14sp/600, quote in #1F1F1F 14sp italic
        Card(
            colors = CardDefaults.cardColors(
                containerColor = com.example.ui.theme.AppColors.WarmTintContainer
            ),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, com.example.ui.theme.AppColors.BorderSubtle),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawLine(
                        color = com.example.ui.theme.AppColors.Secondary,
                        start = Offset(0f, 0f),
                        end = Offset(0f, size.height),
                        strokeWidth = 4.dp.toPx()
                    )
                }
        ) {
            Column(modifier = Modifier.padding(start = 20.dp, end = 16.dp, top = 16.dp, bottom = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = Icons.Default.FormatQuote,
                        contentDescription = null,
                        tint = com.example.ui.theme.AppColors.Secondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (lang == "bn") "রাসূলুল্লাহ (সা.) ইরশাদ করেছেন:" else "The Prophet said:",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        ),
                        color = com.example.ui.theme.AppColors.Secondary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (lang == "bn")
                        "\"যে ব্যক্তি আল্লাহর সন্তুষ্টির উদ্দেশ্যে নিয়মিত ইবাদত ও সালাত কায়েম করে, আল্লাহ তার সকল নেক আমল কবুল করেন এবং তার রিজিক ও জীবনে বরকত দান করেন।\""
                    else
                        "\"Whoever maintains their prayers consistently for the sake of Allah, Allah accepts their righteous deeds and grants barakah in their life.\"",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    ),
                    color = com.example.ui.theme.AppColors.TextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
internal fun PrayerCountdownHeroCard(
    window: PrayerWindow,
    isActive: Boolean,
    remainingMillis: Long,
    lang: String,
    onClick: () -> Unit
) {
    val prayerTitle = if (lang == "bn") window.prayer.titleBn else window.prayer.titleEn
    // User request: "instead of Current Window add 'Wakt'"
    val waktLabel = if (lang == "bn") "ওয়াক্ত" else "Wakt"

    val hours = (remainingMillis / (1000 * 60 * 60)).coerceAtLeast(0)
    val minutes = ((remainingMillis / (1000 * 60)) % 60).coerceAtLeast(0)
    val seconds = ((remainingMillis / 1000) % 60).coerceAtLeast(0)

    val timeFormatted = String.format("%02d:%02d:%02d", hours, minutes, seconds)

    val timeFormat = SimpleDateFormat("hh:mm a", Locale.US).apply {
        timeZone = TimeZone.getDefault()
    }
    val windowStartStr = timeFormat.format(Date(window.startTimeMillis))
    val windowEndStr = timeFormat.format(Date(window.endTimeMillis))

    val cardShape = RoundedCornerShape(16.dp)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("prayer_hero_card"),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = com.example.ui.theme.AppColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(2.dp, com.example.ui.theme.AppColors.Secondary)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(cardShape)
                .background(com.example.ui.theme.AppColors.Surface)
        ) {
            // Background Islamic Architectural Illustration Fill (delicate subtle accents)
            Canvas(modifier = Modifier.matchParentSize()) {
                drawIslamicGlassFill(isActive)
            }

            // Foreground Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                // Top row: Wakt badge + time range
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Wakt badge = #FFB030 fill + #1F1F1F text
                    Surface(
                        color = com.example.ui.theme.AppColors.Primary,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = waktLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                letterSpacing = 0.5.sp
                            ),
                            color = com.example.ui.theme.AppColors.OnPrimary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }

                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = com.example.ui.theme.AppColors.WarmTintContainer,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, com.example.ui.theme.AppColors.BorderSubtle),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Text(
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            text = "$windowStartStr - $windowEndStr",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                fontFeatureSettings = "tnum"
                            ),
                            color = com.example.ui.theme.AppColors.Secondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom row: waqt name (one line, ellipsis) + fixed-size countdown box.
                // Fixed heights/widths keep the card the same size for every waqt name and language.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = prayerTitle,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp,
                                lineHeight = 30.sp
                            ),
                            color = com.example.ui.theme.AppColors.TextPrimary,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("hero_waqt_name")
                        )
                        Text(
                            text = if (isActive) (if (lang == "bn") "ওয়াক্ত শেষ হতে বাকি" else "Time Remaining") else (if (lang == "bn") "ওয়াক্ত শুরু হতে বাকি" else "Starts in"),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            ),
                            color = com.example.ui.theme.AppColors.Secondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(Modifier.width(10.dp))

                    // Countdown box: fixed size, monospaced tabular digits so its width never changes.
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = com.example.ui.theme.AppColors.Surface,
                        border = BorderStroke(2.dp, com.example.ui.theme.AppColors.Secondary),
                        modifier = Modifier
                            .width(124.dp)
                            .height(48.dp)
                            .testTag("hero_countdown_box")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = timeFormatted,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    // Sized in dp so the fixed box fits at any system font scale (e.g. 1.3x).
                                    fontSize = with(LocalDensity.current) { 20.dp.toSp() },
                                    fontFamily = FontFamily.Monospace,
                                    fontFeatureSettings = "tnum"
                                ),
                                color = com.example.ui.theme.AppColors.Secondary,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Draws an artistic Islamic architectural illustration fill (Mihrab arch, 8-point Islamic star,
 * and delicate arabesque lattice) behind the frosted glassmorphism layer.
 */
private fun DrawScope.drawIslamicGlassFill(isActive: Boolean) {
    val w = size.width
    val h = size.height

    val goldAccent = Color(0xFFFFB030).copy(alpha = 0.22f)
    val chestnutAccent = Color(0xFF874D14).copy(alpha = 0.16f)
    val subtleLine = Color(0xFF874D14).copy(alpha = 0.06f)

    // 1. Arabesque Diagonal Lattice (Mashrabiya tracery) across the background
    val step = 32.dp.toPx()
    var x = -h
    while (x < w + h) {
        drawLine(
            color = subtleLine,
            start = Offset(x, 0f),
            end = Offset(x + h, h),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = subtleLine,
            start = Offset(x, h),
            end = Offset(x + h, 0f),
            strokeWidth = 1.dp.toPx()
        )
        x += step
    }

    // 2. Islamic Pointed Arch (Mihrab) framing the right side
    val archWidth = w * 0.42f
    val archRight = w * 0.98f
    val archLeft = archRight - archWidth
    val archBaseY = h * 1.05f
    val archSpringY = h * 0.48f
    val archApexX = (archLeft + archRight) / 2f
    val archApexY = h * 0.08f

    // Outer pointed arch path
    val outerArch = Path().apply {
        moveTo(archLeft, archBaseY)
        lineTo(archLeft, archSpringY)
        cubicTo(
            archLeft, archSpringY - (archSpringY - archApexY) * 0.6f,
            archApexX - archWidth * 0.15f, archApexY + 10f,
            archApexX, archApexY
        )
        cubicTo(
            archApexX + archWidth * 0.15f, archApexY + 10f,
            archRight, archSpringY - (archSpringY - archApexY) * 0.6f,
            archRight, archSpringY
        )
        lineTo(archRight, archBaseY)
    }

    drawPath(
        path = outerArch,
        color = goldAccent,
        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
    )

    // Inner cusped/scalloped arch
    val innerArch = Path().apply {
        val iLeft = archLeft + 12.dp.toPx()
        val iRight = archRight - 12.dp.toPx()
        val iApexY = archApexY + 12.dp.toPx()
        val iSpringY = archSpringY + 4.dp.toPx()

        moveTo(iLeft, archBaseY)
        lineTo(iLeft, iSpringY)
        cubicTo(
            iLeft, iSpringY - (iSpringY - iApexY) * 0.6f,
            archApexX - archWidth * 0.12f, iApexY + 8f,
            archApexX, iApexY
        )
        cubicTo(
            archApexX + archWidth * 0.12f, iApexY + 8f,
            iRight, iSpringY - (iSpringY - iApexY) * 0.6f,
            iRight, iSpringY
        )
        lineTo(iRight, archBaseY)
    }

    drawPath(
        path = innerArch,
        color = chestnutAccent,
        style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round)
    )

    // 3. 8-Pointed Islamic Star (Khatam / Rub el Hizb) in the upper right
    val starCenterX = archApexX
    val starCenterY = archApexY + 36.dp.toPx()
    val starRadius = 15.dp.toPx()

    drawIslamic8PointStar(
        center = Offset(starCenterX, starCenterY),
        outerRadius = starRadius,
        innerRadius = starRadius * 0.54f,
        color = goldAccent
    )

    // 4. Crescent finial at arch apex
    drawCircle(
        color = goldAccent,
        radius = 3.dp.toPx(),
        center = Offset(archApexX, archApexY - 4.dp.toPx())
    )
}

/**
 * Helper to draw an authentic 8-point geometric star (Rub el Hizb)
 */
private fun DrawScope.drawIslamic8PointStar(
    center: Offset,
    outerRadius: Float,
    innerRadius: Float,
    color: Color
) {
    val path = Path()
    val points = 16
    for (i in 0 until points) {
        val r = if (i % 2 == 0) outerRadius else innerRadius
        val angle = (i * Math.PI / 8.0) - (Math.PI / 2.0)
        val px = (center.x + r * Math.cos(angle)).toFloat()
        val py = (center.y + r * Math.sin(angle)).toFloat()
        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
    }
    path.close()

    drawPath(
        path = path,
        color = color,
        style = Stroke(width = 1.4.dp.toPx())
    )
}

@Composable
private fun TodayScoreCard(
    score: Int,
    isPerfect: Boolean,
    currentStreak: Int,
    longestStreak: Int,
    lang: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("today_score_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = com.example.ui.theme.AppColors.Surface
        ),
        border = BorderStroke(1.dp, com.example.ui.theme.AppColors.BorderSubtle),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = Strings.t("today_score", lang),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    ),
                    color = com.example.ui.theme.AppColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))

                if (isPerfect) {
                    Surface(
                        color = com.example.ui.theme.AppColors.WarmTintContainer,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, com.example.ui.theme.AppColors.Secondary)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stars,
                                contentDescription = null,
                                tint = com.example.ui.theme.AppColors.Secondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = Strings.t("perfect_day", lang),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                                color = com.example.ui.theme.AppColors.Secondary
                            )
                        }
                    }
                } else {
                    Text(
                        text = if (lang == "bn") "১০০ পয়েন্ট অর্জন করে 'পারফেক্ট ডে' করুন" else "Reach 100 pts for a Perfect Day!",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = com.example.ui.theme.AppColors.TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                // Current/Best streak text in #5A4632 12sp, not gold
                Text(
                    text = "${Strings.t("current_streak", lang)}: $currentStreak • ${Strings.t("longest_streak", lang)}: $longestStreak",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        fontFeatureSettings = "tnum"
                    ),
                    color = com.example.ui.theme.AppColors.TextSecondary
                )
            }

            // Score Circle Progress
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(80.dp)
            ) {
                val animatedProgress by animateFloatAsState(
                    targetValue = (score / 100f).coerceIn(0f, 1f),
                    animationSpec = tween(durationMillis = 600),
                    label = "score_arc"
                )

                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.fillMaxSize(),
                    color = com.example.ui.theme.AppColors.ProgressTrack,
                    strokeWidth = 7.dp,
                    strokeCap = StrokeCap.Round
                )

                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxSize(),
                    color = com.example.ui.theme.AppColors.Primary,
                    strokeWidth = 7.dp,
                    strokeCap = StrokeCap.Round
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$score",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp,
                            fontFeatureSettings = "tnum"
                        ),
                        color = com.example.ui.theme.AppColors.Secondary
                    )
                    Text(
                        text = "/100",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            fontFeatureSettings = "tnum"
                        ),
                        color = com.example.ui.theme.AppColors.TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun AzkharQuickCard(
    title: String,
    isDone: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(84.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = com.example.ui.theme.AppColors.Surface
        ),
        border = BorderStroke(
            1.dp,
            if (isDone) com.example.ui.theme.AppColors.Primary else com.example.ui.theme.AppColors.BorderSubtle
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isDone) com.example.ui.theme.AppColors.Primary else com.example.ui.theme.AppColors.WarmTintContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isDone) Icons.Default.Check else icon,
                    contentDescription = null,
                    tint = if (isDone) com.example.ui.theme.AppColors.OnPrimary else com.example.ui.theme.AppColors.Secondary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                ),
                color = com.example.ui.theme.AppColors.TextPrimary,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun QuranChecklistCard(done: Boolean, lang: String, onToggle: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .testTag("home_quran_item"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = com.example.ui.theme.AppColors.Surface),
        border = BorderStroke(1.dp, if (done) com.example.ui.theme.AppColors.Primary else com.example.ui.theme.AppColors.BorderSubtle),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (done) com.example.ui.theme.AppColors.Primary else com.example.ui.theme.AppColors.WarmTintContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (done) Icons.Default.Check else Icons.Outlined.AutoStories,
                    contentDescription = null,
                    tint = if (done) com.example.ui.theme.AppColors.OnPrimary else com.example.ui.theme.AppColors.Secondary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = Strings.t("quran_title", lang),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                    color = com.example.ui.theme.AppColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (lang == "bn") "আজকের আমল • ট্যাপ করে পরিবর্তন করুন" else "Today's amal • tap to change",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = com.example.ui.theme.AppColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Surface(
                color = if (done) com.example.ui.theme.AppColors.Primary else com.example.ui.theme.AppColors.Surface,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, com.example.ui.theme.AppColors.Secondary.copy(alpha = if (done) 1f else 0.4f))
            ) {
                Text(
                    text = Strings.t(if (done) "done" else "not_done", lang),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                    color = if (done) com.example.ui.theme.AppColors.OnPrimary else com.example.ui.theme.AppColors.TextSecondary,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}
