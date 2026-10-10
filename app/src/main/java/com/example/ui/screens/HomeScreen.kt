package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DayRecord
import com.example.model.PrayerName
import com.example.model.PrayerStatus
import com.example.model.PrayerWindow
import com.example.ui.MainViewModel
import com.example.ui.Strings
import com.example.ui.components.*
import com.example.ui.theme.AppColors
import com.example.ui.theme.CondensedNumeralsFamily
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToPrayerTimes: () -> Unit,
    onNavigateToTracker: () -> Unit,
    onNavigateToAzkhar: (tabIndex: Int) -> Unit,
    onNavigateToMore: () -> Unit = {}
) {
    val settings by viewModel.settings.collectAsState()
    val schedule by viewModel.prayerSchedule.collectAsState()
    val tomorrowSchedule by viewModel.tomorrowSchedule.collectAsState()
    val todayRecord by viewModel.todayRecord.collectAsState()
    val allDays by viewModel.allDays.collectAsState()
    val currentStreak by viewModel.currentStreak.collectAsState()
    val longestStreak by viewModel.longestStreak.collectAsState()
    val liveTime by viewModel.liveTimeMillis.collectAsState()
    val currentUser by viewModel.currentUserAccount.collectAsState()
    val todayDate by viewModel.todayDate.collectAsState()
    val lang = settings.language
    val bn = lang == "bn"

    // Today's windows followed by tomorrow's, so after Isha the next waqt is tomorrow's (never a past one).
    val allWindows = schedule.allWindows() + tomorrowSchedule.allWindows()
    val activeWindow = allWindows.firstOrNull { liveTime in it.startTimeMillis until it.endTimeMillis }
    val nextWindow = allWindows.firstOrNull { it.startTimeMillis > liveTime }
    val targetWindow = activeWindow ?: nextWindow
    val isWindowActive = activeWindow != null
    val remaining = targetWindow?.let { if (isWindowActive) it.endTimeMillis - liveTime else it.startTimeMillis - liveTime } ?: 0L

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // Hero sits directly on the page: glow + skyline + big time, then the details line.
        // Real sky position: sun from sunrise to sunset, moon from sunset to next sunrise.
        val sunrise = schedule.sunriseMillis
        val sunset = schedule.maghrib.startTimeMillis
        val celestial = when {
            liveTime in sunrise until sunset -> (liveTime - sunrise).toFloat() / (sunset - sunrise)
            liveTime >= sunset -> (liveTime - sunset).toFloat() / (tomorrowSchedule.sunriseMillis - sunset).coerceAtLeast(1)
            else -> (liveTime - (sunset - 86_400_000L)).toFloat() / (sunrise - (sunset - 86_400_000L)).coerceAtLeast(1)
        }
        val sunUp = liveTime in sunrise until sunset
        Box(Modifier.fillMaxWidth()) {
            SkyBackdrop(Modifier.matchParentSize(), isNight = !sunUp, celestialProgress = celestial)
            Column(Modifier.fillMaxWidth()) {
                HomeTopBar(currentUser?.effectiveName, currentStreak, lang)
                if (targetWindow != null) {
                    PrayerCountdownHeroCard(
                        window = targetWindow,
                        isActive = isWindowActive,
                        remainingMillis = remaining,
                        lang = lang,
                        onClick = onNavigateToPrayerTimes,
                        district = settings.district,
                        liveTime = liveTime,
                        hijriOffset = settings.hijriOffset
                    )
                }
            }
        }

        Column(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 6.dp, bottom = 24.dp + com.example.ui.components.LocalBottomBarSpace.current),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            QuickActionsGrid(lang, todayRecord, onNavigateToTracker, onNavigateToAzkhar, onNavigateToPrayerTimes)

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeader(if (bn) "নামাজের সময়" else "Prayer Times", action = if (bn) "সব" else "See all", onAction = onNavigateToPrayerTimes)
                PrayerTimesStrip(schedule.allWindows(), liveTime, lang, onNavigateToPrayerTimes)
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeader(if (bn) "আজকের আমল" else "Today's amal", action = if (bn) "ট্র্যাকার" else "Tracker", onAction = onNavigateToTracker)
                TodayProgressCard(todayRecord, lang, onNavigateToTracker) { viewModel.toggleQuran(onToday = true) }
            }

            WeeklyStreakCard(allDays, todayDate, currentStreak, longestStreak, lang)
            MilestonesRow(allDays, longestStreak, lang)

            ForbiddenTimesCard(
                schedule = schedule,
                liveTime = liveTime,
                lang = lang,
                showDescriptions = false,
                testTag = "home_forbidden_times_card",
                onClick = onNavigateToPrayerTimes
            )

            HadithCard(bn)
        }
    }
}

// ============================================================================ top bar

@Composable
private fun HomeTopBar(name: String?, streak: Int, lang: String) {
    val bn = lang == "bn"
    Column(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp).testTag("authenticated_user_banner")) {
        ScreenTitle(
            title = if (bn) "আদ-দ্বীন" else "Ad-Deen",
            leading = { if (name != null) UserAvatar(name, size = 36.dp) else AdDeenLogo(size = 36.dp) },
            trailing = {
                Row(
                    Modifier.clip(RoundedCornerShape(18.dp)).background(AppColors.Surface.copy(alpha = 0.7f)).padding(horizontal = 10.dp, vertical = 6.dp).testTag("streak_badge"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.LocalFireDepartment, Strings.t("current_streak", lang), tint = Color(0xFFE8792B), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(3.dp))
                    Text("$streak", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"), color = AppColors.TextPrimary)
                }
            }
        )
        Text(
            text = (if (bn) "আসসালামু আলাইকুম" else "Assalamu Alaikum") + (name?.takeIf { it.isNotBlank() }?.let { ", $it" } ?: ""),
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.TextSecondary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().testTag("authenticated_user_name_text")
        )
    }
}

// ============================================================================ hero

/**
 * Reference-style hero: the waqt time in large condensed numerals (crescent between the digits) over a
 * pale mosque skyline, then "REMAINING TIME | date, place". Fixed sizes keep the hero the same height
 * for every waqt name and language, and the countdown in a fixed-width box (issue 10).
 */
@Composable
internal fun PrayerCountdownHeroCard(
    window: PrayerWindow,
    isActive: Boolean,
    remainingMillis: Long,
    lang: String,
    onClick: () -> Unit,
    district: String? = null,
    liveTime: Long = System.currentTimeMillis(),
    hijriOffset: Int = 0
) {
    val bn = lang == "bn"
    val title = if (bn) window.prayer.titleBn else window.prayer.titleEn
    val target = if (isActive) window.endTimeMillis else window.startTimeMillis
    val clock = remember(target) { SimpleDateFormat("h:mm", Locale.US).apply { timeZone = TimeZone.getDefault() }.format(Date(target)) }
    val ampm = remember(target) { SimpleDateFormat("a", Locale.US).apply { timeZone = TimeZone.getDefault() }.format(Date(target)) }
    val h = (remainingMillis / 3_600_000).coerceAtLeast(0)
    val m = ((remainingMillis / 60_000) % 60).coerceAtLeast(0)
    val s = ((remainingMillis / 1000) % 60).coerceAtLeast(0)
    val countdown = String.format(Locale.US, "%d:%02d:%02d", h, m, s)
    val density = LocalDensity.current
    val gregDate = remember(liveTime / 60_000, bn) {
        SimpleDateFormat("EEE, d MMMM yyyy", if (bn) Locale("bn") else Locale.US).format(Date(liveTime))
    }
    val hijriDate = remember(liveTime / 60_000, bn, hijriOffset) {
        runCatching {
            val hd = CalendarDateHelper.getHijriDate(Calendar.getInstance().apply { timeInMillis = liveTime }, bn, hijriOffset)
            val day = if (bn) CalendarDateHelper.toBnDigits(hd.day.toString()) else hd.day.toString()
            val year = if (bn) CalendarDateHelper.toBnDigits(hd.year.toString()) else hd.year.toString()
            "$day ${hd.monthName} $year ${if (bn) "হিজরি" else "AH"}"
        }.getOrNull()
    }
    val interaction = remember { MutableInteractionSource() }

    Column(
        Modifier
            .fillMaxWidth()
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .testTag("prayer_hero_card")
    ) {
        Box(Modifier.fillMaxWidth().height(170.dp), contentAlignment = Alignment.Center) {
            MosqueSkyline(Modifier.fillMaxWidth().height(150.dp).align(Alignment.BottomCenter).padding(horizontal = 10.dp))
            // Caption explains what the big time is (end of current waqt / start of next).
            Text(
                text = (if (isActive) (if (bn) "$title শেষ হবে" else "$title ends at") else (if (bn) "$title শুরু হবে" else "$title starts at")).uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                color = AppColors.Secondary.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 4.dp, start = 24.dp, end = 24.dp)
            )
            // Big numerals: sized in dp so large system font can't break the layout.
            val big = with(density) { 92.dp.toSp() }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(bottom = 18.dp)
                    // Numerals melt into the skyline: full ink at the top, fading out toward the bottom.
                    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            Brush.verticalGradient(0f to Color.Black, 0.45f to Color.Black, 1f to Color.Black.copy(alpha = 0.18f)),
                            blendMode = BlendMode.DstIn
                        )
                    }
            ) {
                val parts = clock.split(":")
                Text(parts[0], style = MaterialTheme.typography.displayLarge.copy(fontFamily = CondensedNumeralsFamily, fontSize = big, lineHeight = big), color = AppColors.Secondary)
                Crescent(Modifier.padding(horizontal = 4.dp).size(26.dp, 56.dp))
                Text(parts.getOrElse(1) { "" }, style = MaterialTheme.typography.displayLarge.copy(fontFamily = CondensedNumeralsFamily, fontSize = big, lineHeight = big), color = AppColors.Secondary)
                Spacer(Modifier.width(6.dp))
                Text(ampm, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = AppColors.Secondary, modifier = Modifier.align(Alignment.Bottom).padding(bottom = 18.dp))
            }
        }
        // Two-row grid around one centre divider:
        //   REMAINING TIME      |  Hijri date
        //   Maghrib   0:11:00   |  (pin) Location
        // Left half is right-aligned to the bar, right half left-aligned to it.
        Row(
            Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(horizontal = 20.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Box(Modifier.height(18.dp), contentAlignment = Alignment.CenterEnd) {
                    Text(
                        if (isActive) (if (bn) "ওয়াক্ত শেষ হতে বাকি" else "REMAINING TIME") else (if (bn) "ওয়াক্ত শুরু হতে বাকি" else "STARTS IN"),
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp, fontSize = 10.sp),
                        color = AppColors.TextSecondary,
                        maxLines = 1
                    )
                }
                Row(Modifier.height(28.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        title,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium, fontSize = 13.sp),
                        color = AppColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false).testTag("hero_waqt_name")
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.width(76.dp).height(26.dp).testTag("hero_countdown_box"), contentAlignment = Alignment.CenterEnd) {
                        Text(
                            countdown,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = CondensedNumeralsFamily,
                                fontSize = with(density) { 20.dp.toSp() },
                                fontFeatureSettings = "tnum"
                            ),
                            color = AppColors.TextPrimary,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
            Box(Modifier.padding(horizontal = 14.dp).width(1.dp).fillMaxHeight().background(AppColors.BorderMedium))
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                Box(Modifier.height(18.dp), contentAlignment = Alignment.CenterStart) {
                    Text(
                        hijriDate ?: gregDate,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                        color = AppColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(Modifier.height(28.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Place, null, tint = AppColors.Secondary, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(
                        district ?: "",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium, fontSize = 13.sp),
                        color = AppColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/** Crescent "colon" between the hour and minute digits. */
@Composable
private fun Crescent(modifier: Modifier) {
    val c = AppColors.Primary
    Canvas(modifier) {
        val r = size.width * 0.5f
        val center = Offset(size.width / 2f, r + 2f)
        val moon = Path().apply { addOval(Rect(center, r)) }
        val bite = Path().apply { addOval(Rect(Offset(center.x + r * 0.45f, center.y - r * 0.25f), r * 0.85f)) }
        drawPath(Path.combine(PathOperation.Difference, moon, bite), c)
        drawCircle(c, size.width * 0.16f, Offset(size.width / 2f, size.height * 0.78f))
    }
}

// ============================================================================ prayer strip

private fun prayerIcon(p: PrayerName): Pair<ImageVector, Color> = when (p) {
    PrayerName.TAHAJJUD -> Icons.Rounded.Bedtime to Color(0xFF7C6FD6)
    PrayerName.FAJR -> Icons.Rounded.WbTwilight to Color(0xFFF08A3C)
    PrayerName.DUHA -> Icons.Rounded.LightMode to Color(0xFFF2B33D)
    PrayerName.DHUHR -> Icons.Rounded.WbSunny to Color(0xFFF59E2E)
    PrayerName.ASR -> Icons.Rounded.WbCloudy to Color(0xFFEE8B4A)
    PrayerName.MAGHRIB -> Icons.Rounded.Brightness3 to Color(0xFFE9733A)
    PrayerName.ISHA -> Icons.Rounded.NightsStay to Color(0xFF6C7BD9)
}

@Composable
private fun PrayerTimesStrip(windows: List<PrayerWindow>, liveTime: Long, lang: String, onClick: () -> Unit) {
    val fmt = remember { SimpleDateFormat("h:mm", Locale.US).apply { timeZone = TimeZone.getDefault() } }
    val currentIndex = windows.indexOfFirst { liveTime in it.startTimeMillis until it.endTimeMillis }
    val nextIndex = windows.indexOfFirst { it.startTimeMillis > liveTime }
    val listState = rememberLazyListState()
    LaunchedEffect(currentIndex, nextIndex) {
        val focus = if (currentIndex >= 0) currentIndex else nextIndex
        if (focus > 1) listState.animateScrollToItem(focus - 1)
    }
    LazyRow(state = listState, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.testTag("prayer_strip")) {
        items(windows.size) { i ->
            val w = windows[i]
            val current = i == currentIndex
            val (icon, tint) = prayerIcon(w.prayer)
            val interaction = remember { MutableInteractionSource() }
            Column(
                modifier = Modifier
                    .width(82.dp)
                    .pressScale(interaction)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (current) AppColors.Primary.copy(alpha = 0.22f) else AppColors.WarmTintContainer)
                    .clickable(interactionSource = interaction, indication = ripple(), onClick = onClick)
                    .padding(vertical = 14.dp, horizontal = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(icon, null, tint = tint, modifier = Modifier.size(26.dp))
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (lang == "bn") w.prayer.titleBn.substringBefore(" /") else w.prayer.titleEn.substringBefore(" /"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = fmt.format(Date(w.startTimeMillis)),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
                    color = if (current) AppColors.Secondary else AppColors.TextPrimary, maxLines = 1
                )
            }
        }
    }
}

// ============================================================================ quick actions

private data class QuickAction(val icon: ImageVector, val label: String, val tint: Color, val done: Boolean, val tag: String, val onClick: () -> Unit)

@Composable
private fun QuickActionsGrid(lang: String, record: DayRecord, onTracker: () -> Unit, onAzkhar: (Int) -> Unit, onWakt: () -> Unit) {
    val bn = lang == "bn"
    val actions = listOf(
        QuickAction(Icons.Rounded.WbSunny, if (bn) "সকাল" else "Morning", Color(0xFFF2A93B), record.sections.morning, "qa_morning") { onAzkhar(0) },
        QuickAction(Icons.Rounded.NightsStay, if (bn) "সন্ধ্যা" else "Evening", Color(0xFF8C7AE6), record.sections.evening, "qa_evening") { onAzkhar(1) },
        QuickAction(Icons.Rounded.Bedtime, if (bn) "ঘুম" else "Sleep", Color(0xFF6C8FE0), record.sections.sleep, "qa_sleep") { onAzkhar(2) },
        QuickAction(Icons.Rounded.Alarm, if (bn) "জাগরণ" else "Wake", Color(0xFFE87A8C), record.sections.wake, "qa_wake") { onAzkhar(3) },
        QuickAction(Icons.Rounded.Fingerprint, if (bn) "তাসবীহ" else "Tasbih", Color(0xFF4DB59A), record.sections.zikr, "qa_tasbih") { onAzkhar(5) },
        QuickAction(Icons.Rounded.AutoStories, if (bn) "৯৯ নাম" else "99 Names", Color(0xFF5BA4D9), false, "qa_names") { onAzkhar(4) },
        QuickAction(Icons.Rounded.TaskAlt, if (bn) "ট্র্যাকার" else "Tracker", Color(0xFFE5793B), false, "qa_tracker", onTracker),
        QuickAction(Icons.Rounded.NotificationsActive, if (bn) "অ্যালার্ম" else "Reminder", Color(0xFFC77DDB), false, "qa_alarms", onWakt)
    )
    GlowCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 16.dp, horizontal = 6.dp)) {
        actions.chunked(4).forEachIndexed { row, chunk ->
            if (row > 0) Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth()) { chunk.forEach { QuickActionItem(it, Modifier.weight(1f)) } }
        }
    }
}

@Composable
private fun QuickActionItem(a: QuickAction, modifier: Modifier) {
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier.pressScale(interaction, 0.93f).clip(RoundedCornerShape(14.dp))
            .clickable(interactionSource = interaction, indication = ripple(), onClick = a.onClick)
            .padding(vertical = 6.dp).testTag(a.tag),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box {
            Icon(a.icon, null, tint = a.tint, modifier = Modifier.size(32.dp))
            if (a.done) {
                Box(
                    Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = (-4).dp).size(15.dp).clip(CircleShape).background(Color(0xFF4DB59A)),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(10.dp)) }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(a.label, style = MaterialTheme.typography.bodyMedium, color = AppColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// ============================================================================ today progress

@Composable
private fun TodayProgressCard(record: DayRecord, lang: String, onOpenTracker: () -> Unit, onToggleQuran: () -> Unit) {
    val bn = lang == "bn"
    val animated by animateFloatAsState((record.score / 100f).coerceIn(0f, 1f), tween(700), label = "score")
    GlowCard(modifier = Modifier.fillMaxWidth().testTag("today_score_card"), onClick = onOpenTracker, highlighted = record.perfect) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(78.dp), contentAlignment = Alignment.Center) {
                val track = AppColors.ProgressTrack
                val primary = AppColors.Primary
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = 7.dp.toPx()
                    drawArc(track, -90f, 360f, false, style = Stroke(stroke, cap = StrokeCap.Round))
                    drawArc(primary, -90f, 360f * animated, false, style = Stroke(stroke, cap = StrokeCap.Round))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${record.score}", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"), color = AppColors.TextPrimary)
                    Text("/100", style = MaterialTheme.typography.labelSmall, color = AppColors.TextSecondary)
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(Strings.t("today_score", lang), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = AppColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(
                    if (record.perfect) (if (bn) "মাশাআল্লাহ! আজ পারফেক্ট ডে" else "MashaAllah! A Perfect Day")
                    else (if (bn) "পারফেক্ট ডে-র জন্য আরও ${100 - record.score} পয়েন্ট" else "${100 - record.score} pts to a Perfect Day"),
                    style = MaterialTheme.typography.bodySmall, color = if (record.perfect) AppColors.Secondary else AppColors.TextSecondary, maxLines = 2
                )
                Spacer(Modifier.height(6.dp))
                val prayed = listOf(record.prayers.fajr, record.prayers.dhuhr, record.prayers.asr, record.prayers.maghrib, record.prayers.isha)
                    .count { it == PrayerStatus.JAMAT || it == PrayerStatus.ALONE }
                Text(
                    if (bn) "ফরজ সালাত: $prayed/5" else "Fard prayers: $prayed/5",
                    style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                    color = AppColors.Secondary
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        // Quran checklist item (same switch as everywhere; Undo snackbar appears on change).
        ToggleRow(
            icon = Icons.Outlined.AutoStories,
            title = Strings.t("quran_title", lang),
            subtitle = Strings.t(if (record.quran) "done" else "not_done", lang),
            checked = record.quran,
            onCheckedChange = { onToggleQuran() },
            highlighted = true,
            modifier = Modifier.testTag("home_quran_item"),
            switchTag = "home_quran_switch"
        )
    }
}

// ============================================================================ streak + milestones

@Composable
private fun WeeklyStreakCard(allDays: List<DayRecord>, today: String, current: Int, best: Int, lang: String) {
    val bn = lang == "bn"
    val map = remember(allDays) { allDays.associateBy { it.date } }
    val days = remember(today, map, bn) {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getDefault() }
        val dayFmt = SimpleDateFormat("EEE", if (bn) Locale("bn") else Locale.US)
        (6 downTo 0).map { back ->
            val cal = Calendar.getInstance().apply {
                runCatching { fmt.parse(today) }.getOrNull()?.let { time = it }
                add(Calendar.DAY_OF_MONTH, -back)
            }
            val key = fmt.format(cal.time)
            Triple(dayFmt.format(cal.time).take(3), map[key], key == today)
        }
    }
    GlowCard(modifier = Modifier.fillMaxWidth().testTag("weekly_streak_card")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (bn) "গত ৭ দিন" else "Last 7 days", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = AppColors.TextPrimary, modifier = Modifier.weight(1f))
            Text((if (bn) "সর্বোচ্চ " else "Best ") + best, style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"), color = AppColors.TextSecondary)
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            days.forEach { (label, rec, isToday) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (isToday) FontWeight.SemiBold else FontWeight.Normal), color = if (isToday) AppColors.TextPrimary else AppColors.TextSecondary, maxLines = 1)
                    Spacer(Modifier.height(6.dp))
                    val perfect = rec?.perfect == true
                    val score = rec?.score ?: 0
                    Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) {
                        if (perfect) {
                            Box(Modifier.fillMaxSize().clip(CircleShape).background(Color(0xFFFCE3CF)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.LocalFireDepartment, null, tint = Color(0xFFE8792B), modifier = Modifier.size(20.dp))
                            }
                        } else {
                            val track = AppColors.ProgressTrack
                            val primary = AppColors.Primary
                            Canvas(Modifier.fillMaxSize()) {
                                drawArc(track, -90f, 360f, false, style = Stroke(3.dp.toPx()))
                                drawArc(primary, -90f, 360f * score / 100f, false, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
                            }
                            if (isToday) Box(Modifier.size(5.dp).clip(CircleShape).background(AppColors.Secondary))
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            if (bn) "বর্তমান স্ট্রিক: $current দিন" else "Current streak: $current day${if (current == 1) "" else "s"}",
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.TextSecondary
        )
    }
}

/** A milestone badge computed from stored days (no extra storage needed). */
data class Milestone(val id: String, val titleEn: String, val titleBn: String, val icon: ImageVector, val progress: Int, val target: Int) {
    val achieved get() = progress >= target
}

fun computeMilestones(days: List<DayRecord>, longestStreak: Int): List<Milestone> {
    val perfect = days.count { it.perfect }
    val quran = days.count { it.quran && it.isQuranApplicable }
    val allJamat = days.count { d ->
        listOf(d.prayers.fajr, d.prayers.dhuhr, d.prayers.asr, d.prayers.maghrib, d.prayers.isha).all { it == PrayerStatus.JAMAT }
    }
    return listOf(
        Milestone("first_perfect", "First Perfect Day", "প্রথম পারফেক্ট ডে", Icons.Rounded.Stars, perfect.coerceAtMost(1), 1),
        Milestone("streak_3", "3-day streak", "৩ দিনের স্ট্রিক", Icons.Rounded.LocalFireDepartment, longestStreak.coerceAtMost(3), 3),
        Milestone("streak_7", "7-day streak achiever", "৭ দিনের স্ট্রিক", Icons.Rounded.LocalFireDepartment, longestStreak.coerceAtMost(7), 7),
        Milestone("jamat_7", "7 days all in Jamat", "৭ দিন সব জামাতে", Icons.Rounded.Groups, allJamat.coerceAtMost(7), 7),
        Milestone("quran_7", "Quran on 7 days", "৭ দিন কুরআন তিলাওয়াত", Icons.Rounded.AutoStories, quran.coerceAtMost(7), 7),
        Milestone("streak_30", "30-day streak", "৩০ দিনের স্ট্রিক", Icons.Rounded.EmojiEvents, longestStreak.coerceAtMost(30), 30)
    ).sortedWith(compareByDescending<Milestone> { it.achieved }.thenByDescending { it.progress.toFloat() / it.target })
}

@Composable
private fun MilestonesRow(days: List<DayRecord>, longestStreak: Int, lang: String) {
    val bn = lang == "bn"
    val milestones = remember(days, longestStreak) { computeMilestones(days, longestStreak) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(if (bn) "মাইলস্টোন" else "Milestones")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.testTag("milestones_row")) {
            items(milestones, key = { it.id }) { m ->
                Column(
                    Modifier.width(126.dp).clip(RoundedCornerShape(18.dp))
                        .background(if (m.achieved) AppColors.Primary.copy(alpha = 0.16f) else AppColors.Surface)
                        .padding(12.dp)
                ) {
                    Icon(m.icon, null, tint = if (m.achieved) Color(0xFFE8792B) else AppColors.TextSecondary.copy(alpha = 0.6f), modifier = Modifier.size(26.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(if (bn) m.titleBn else m.titleEn, style = MaterialTheme.typography.labelLarge, color = AppColors.TextPrimary, maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(8.dp))
                    if (m.achieved) {
                        Text(if (bn) "✓ অর্জিত" else "✓ Achieved", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = AppColors.Secondary)
                    } else {
                        LinearProgressIndicator(
                            progress = { m.progress.toFloat() / m.target },
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                            color = AppColors.Primary,
                            trackColor = AppColors.ProgressTrack,
                            drawStopIndicator = {}
                        )
                        Spacer(Modifier.height(4.dp))
                        Text("${m.progress}/${m.target}", style = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum"), color = AppColors.TextSecondary)
                    }
                }
            }
        }
    }
}

// ============================================================================ hadith

@Composable
private fun HadithCard(bn: Boolean) {
    GlowCard(modifier = Modifier.fillMaxWidth(), color = AppColors.WarmTintContainer, elevation = 0.dp, contentPadding = PaddingValues(18.dp)) {
        Text(
            if (bn) "রাসূলুল্লাহ (সা.) বলেছেন" else "The Prophet ﷺ said",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = AppColors.Secondary
        )
        Spacer(Modifier.height(6.dp))
        Text(
            if (bn) "\"আল্লাহর নিকট সর্বাধিক প্রিয় আমল হলো যা নিয়মিত করা হয়, যদিও তা অল্প হয়।\""
            else "\"The most beloved deeds to Allah are those done regularly, even if they are small.\"",
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
            color = AppColors.TextPrimary
        )
        Spacer(Modifier.height(6.dp))
        Text(if (bn) "সহীহ বুখারী ৬৪৬৪" else "Sahih al-Bukhari 6464", style = MaterialTheme.typography.labelMedium, color = AppColors.TextSecondary)
    }
}
