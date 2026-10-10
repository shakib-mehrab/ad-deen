package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DayRecord
import com.example.model.PrayerStatus
import com.example.ui.MainViewModel
import com.example.ui.Strings
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TrackerScreen(
    viewModel: MainViewModel
) {
    val settings by viewModel.settings.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val record by viewModel.selectedDayRecord.collectAsState()
    val allDays by viewModel.allDays.collectAsState()
    val currentStreak by viewModel.currentStreak.collectAsState()
    val longestStreak by viewModel.longestStreak.collectAsState()
    val lang = settings.language

    val isToday = viewModel.isDateToday()
    val isEditable = selectedDate <= viewModel.todayDateString

    var showCalendarView by remember { mutableStateOf(false) }

    val displayDateFormat = remember {
        SimpleDateFormat("EEE, d MMM yyyy", Locale.US).apply {
            timeZone = TimeZone.getDefault()
        }
    }
    val parseFormat = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getDefault()
        }
    }

    val parsedDate = runCatching { parseFormat.parse(selectedDate) }.getOrNull() ?: Date()
    val formattedDateText = displayDateFormat.format(parsedDate)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.Transparent)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 24.dp + com.example.ui.components.LocalBottomBarSpace.current),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { com.example.ui.components.ScreenTitle(if (lang == "bn") "আমল ট্র্যাকার" else "Amal Tracker") }

        // Date Selector Bar: White surface + 1dp #874D14@30% border, 48dp targets, no clipping on Today
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = com.example.ui.theme.AppColors.Surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.stepDate(-1) },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("prev_date_button")
                    ) {
                        Icon(
                            Icons.Default.ChevronLeft,
                            contentDescription = "Previous Day",
                            tint = com.example.ui.theme.AppColors.Secondary
                        )
                    }

                    // Center Date Title: allow ellipsis only on the date, never the word "Today"
                    Row(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showCalendarView = !showCalendarView }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (isToday) {
                            Text(
                                text = if (lang == "bn") "আজ (" else "Today (",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                ),
                                color = com.example.ui.theme.AppColors.TextPrimary,
                                maxLines = 1
                            )
                            Text(
                                text = formattedDateText,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                ),
                                color = com.example.ui.theme.AppColors.TextPrimary,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Text(
                                text = ")",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                ),
                                color = com.example.ui.theme.AppColors.TextPrimary,
                                maxLines = 1
                            )
                        } else {
                            Text(
                                text = formattedDateText,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                ),
                                color = com.example.ui.theme.AppColors.TextPrimary,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }

                        Spacer(Modifier.width(6.dp))
                        Icon(
                            imageVector = if (showCalendarView) Icons.Default.CalendarToday else Icons.Outlined.CalendarMonth,
                            contentDescription = "Calendar",
                            modifier = Modifier.size(20.dp),
                            tint = com.example.ui.theme.AppColors.Secondary
                        )
                    }

                    val canGoForward = selectedDate < viewModel.todayDateString
                    IconButton(
                        onClick = { if (canGoForward) viewModel.stepDate(1) },
                        enabled = canGoForward,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("next_date_button")
                    ) {
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = "Next Day",
                            tint = if (canGoForward) com.example.ui.theme.AppColors.Secondary else com.example.ui.theme.AppColors.BorderSubtle
                        )
                    }
                }
            }
        }

        // Monthly Calendar Toggle Area
        if (showCalendarView) {
            item {
                MonthlyCalendarCard(
                    allDays = allDays,
                    selectedDate = selectedDate,
                    todayDate = viewModel.todayDateString,
                    lang = lang,
                    onDateSelect = {
                        viewModel.selectDate(it)
                        showCalendarView = false
                    }
                )
            }
        }

        // Score Card: "12/100" at 32sp in #874D14, with a progress ring (#FFB030 progress on #F0E6C8 track). Current/Best streak in #5A4632 12sp
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = com.example.ui.theme.AppColors.Surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth().testTag("day_score_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        // Circular Progress Ring: #FFB030 progress on #F0E6C8 track
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(56.dp)) {
                            CircularProgressIndicator(
                                progress = { (record.score / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxSize(),
                                color = com.example.ui.theme.AppColors.Primary,
                                trackColor = com.example.ui.theme.AppColors.ProgressTrack,
                                strokeWidth = 5.dp
                            )
                            Text(
                                text = "${record.score}%",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    fontFeatureSettings = "tnum"
                                ),
                                color = com.example.ui.theme.AppColors.Secondary
                            )
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "${record.score}",
                                    style = MaterialTheme.typography.headlineLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 32.sp,
                                        fontFeatureSettings = "tnum"
                                    ),
                                    color = com.example.ui.theme.AppColors.Secondary
                                )
                                Text(
                                    text = "/ 100",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        fontFeatureSettings = "tnum"
                                    ),
                                    color = com.example.ui.theme.AppColors.Secondary
                                )

                                if (record.perfect) {
                                    Surface(
                                        color = com.example.ui.theme.AppColors.WarmTintContainer,
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, com.example.ui.theme.AppColors.Secondary)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = com.example.ui.theme.AppColors.Secondary,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = Strings.t("perfect_day", lang),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp
                                                ),
                                                color = com.example.ui.theme.AppColors.Secondary
                                            )
                                        }
                                    }
                                }
                            }
                            Text(
                                text = if (record.perfect) (if (lang == "bn") "আলহামদুলিল্লাহ! সম্পূর্ণ আমল সম্পন্ন।" else "Alhamdulillah! Complete Perfect Day.")
                                else (if (lang == "bn") "১০০ পয়েন্ট অর্জনে সব জামাত, নফল, কুরআন ও আযকার সম্পন্ন করুন" else "Earn 100 points: 5 Jamat + Nafl + Quran + Azkhar"),
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = com.example.ui.theme.AppColors.TextSecondary
                            )
                        }
                    }

                    // Streak stats: text in #5A4632 12sp (never gold)
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Surface(
                            color = com.example.ui.theme.AppColors.WarmTintContainer,
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = com.example.ui.theme.AppColors.Secondary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "$currentStreak ${Strings.t("days", lang)}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        fontFeatureSettings = "tnum"
                                    ),
                                    color = com.example.ui.theme.AppColors.Secondary
                                )
                            }
                        }

                        Text(
                            text = "${Strings.t("current_streak", lang)}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                            color = com.example.ui.theme.AppColors.TextSecondary
                        )
                        Text(
                            text = "${Strings.t("longest_streak", lang)}: $longestStreak",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 12.sp,
                                fontFeatureSettings = "tnum"
                            ),
                            color = com.example.ui.theme.AppColors.TextSecondary
                        )
                    }
                }
            }
        }

        // 1. 5 Daily Fard Prayers (Max 60 points)
        item {
            Text(
                text = Strings.t("fard_prayers", lang),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                ),
                color = com.example.ui.theme.AppColors.Secondary
            )
        }

        item {
            PrayerTrackItem("fajr", if (lang == "bn") "ফজর" else "Fajr", record.prayers.fajr, isEditable, lang) {
                viewModel.updatePrayerStatus("fajr", it)
            }
        }

        item {
            PrayerTrackItem("dhuhr", if (lang == "bn") "যোহর" else "Dhuhr", record.prayers.dhuhr, isEditable, lang) {
                viewModel.updatePrayerStatus("dhuhr", it)
            }
        }

        item {
            PrayerTrackItem("asr", if (lang == "bn") "আসর" else "Asr", record.prayers.asr, isEditable, lang) {
                viewModel.updatePrayerStatus("asr", it)
            }
        }

        item {
            PrayerTrackItem("maghrib", if (lang == "bn") "মাগরিব" else "Maghrib", record.prayers.maghrib, isEditable, lang) {
                viewModel.updatePrayerStatus("maghrib", it)
            }
        }

        item {
            PrayerTrackItem("isha", if (lang == "bn") "ইশা" else "Isha", record.prayers.isha, isEditable, lang) {
                viewModel.updatePrayerStatus("isha", it)
            }
        }

        // 2. Nafl, Witr & Tahajjud (+25 points)
        item {
            Spacer(Modifier.height(4.dp))
            Text(
                text = Strings.t("nafl_prayers", lang),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                ),
                color = com.example.ui.theme.AppColors.Secondary
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = com.example.ui.theme.AppColors.Surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    NaflCheckboxRow(
                        title = Strings.t("witr_title", lang),
                        desc = if (lang == "bn") "ইশার পর বিতর সালাত আদায়" else "3 Rakah Witr with Isha",
                        checked = record.nafl.witr,
                        enabled = isEditable,
                        testTag = "checkbox_witr",
                        onCheckedChange = { viewModel.toggleNafl("witr") }
                    )
                    HorizontalDivider(color = com.example.ui.theme.AppColors.Divider)
                    NaflCheckboxRow(
                        title = Strings.t("tahajjud_title", lang),
                        desc = if (lang == "bn") "রাতের শেষ তৃতীয়াংশে তাহাজ্জুদ সালাত" else "Night Tahajjud prayer performed",
                        checked = record.nafl.tahajjud,
                        enabled = isEditable,
                        testTag = "checkbox_tahajjud",
                        onCheckedChange = { viewModel.toggleNafl("tahajjud") }
                    )
                    HorizontalDivider(color = com.example.ui.theme.AppColors.Divider)
                    NaflCheckboxRow(
                        title = Strings.t("duha_title", lang),
                        desc = if (lang == "bn") "সূর্যোদয়ের ২০ মিনিট পর দুহা / চাশত" else "Duha (Ishraq / Chasht) prayer",
                        checked = record.nafl.duha,
                        enabled = isEditable,
                        testTag = "checkbox_duha",
                        onCheckedChange = { viewModel.toggleNafl("duha") }
                    )
                }
            }
        }

        // 3. Quran reading (+5 points)
        item {
            Spacer(Modifier.height(4.dp))
            Text(
                text = Strings.t("quran_section", lang),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                ),
                color = com.example.ui.theme.AppColors.Secondary
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = com.example.ui.theme.AppColors.Surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    NaflCheckboxRow(
                        title = Strings.t("quran_title", lang),
                        desc = if (record.isQuranApplicable) Strings.t("quran_desc", lang) else Strings.t("quran_not_applicable", lang),
                        checked = record.quran && record.isQuranApplicable,
                        enabled = isEditable && record.isQuranApplicable,
                        testTag = "checkbox_quran",
                        onCheckedChange = { viewModel.toggleQuran() }
                    )
                }
            }
        }

        // 4. Azkhar Sections (+10 points: 2 pts each)
        item {
            Spacer(Modifier.height(4.dp))
            Text(
                text = Strings.t("azkhar_sections", lang),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                ),
                color = com.example.ui.theme.AppColors.Secondary
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = com.example.ui.theme.AppColors.Surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionCheckboxRow(
                        title = "${Strings.t("wake_azkhar", lang)} (+2)",
                        checked = record.sections.wake,
                        enabled = isEditable,
                        testTag = "checkbox_wake",
                        onCheckedChange = { viewModel.toggleSection("wake") }
                    )
                    HorizontalDivider(color = com.example.ui.theme.AppColors.Divider)
                    SectionCheckboxRow(
                        title = "${Strings.t("morning_azkhar", lang)} (+2)",
                        checked = record.sections.morning,
                        enabled = isEditable,
                        testTag = "checkbox_morning",
                        onCheckedChange = { viewModel.toggleSection("morning") }
                    )
                    HorizontalDivider(color = com.example.ui.theme.AppColors.Divider)
                    SectionCheckboxRow(
                        title = "${Strings.t("evening_azkhar", lang)} (+2)",
                        checked = record.sections.evening,
                        enabled = isEditable,
                        testTag = "checkbox_evening",
                        onCheckedChange = { viewModel.toggleSection("evening") }
                    )
                    HorizontalDivider(color = com.example.ui.theme.AppColors.Divider)
                    SectionCheckboxRow(
                        title = "${Strings.t("sleep_azkhar", lang)} (+2)",
                        checked = record.sections.sleep,
                        enabled = isEditable,
                        testTag = "checkbox_sleep",
                        onCheckedChange = { viewModel.toggleSection("sleep") }
                    )
                    HorizontalDivider(color = com.example.ui.theme.AppColors.Divider)
                    SectionCheckboxRow(
                        title = "${Strings.t("daily_zikr", lang)} (+2)",
                        checked = record.sections.zikr,
                        enabled = isEditable,
                        testTag = "checkbox_zikr",
                        onCheckedChange = { viewModel.toggleSection("zikr") }
                    )
                }
            }
        }

        // Worship Score Trends (Past 7 Days Bar Visualizer)
        item {
            Spacer(Modifier.height(4.dp))
            ScoreTrendsCard(allDays = allDays, lang = lang)
        }

        item {
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PrayerTrackItem(
    prayerKey: String,
    title: String,
    currentStatus: PrayerStatus,
    enabled: Boolean,
    lang: String,
    onStatusChange: (PrayerStatus) -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = com.example.ui.theme.AppColors.Surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth().testTag("prayer_track_$prayerKey")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    ),
                    color = com.example.ui.theme.AppColors.TextPrimary
                )
                Text(
                    text = "${currentStatus.points} pts",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        fontFeatureSettings = "tnum"
                    ),
                    color = if (currentStatus.points > 0) com.example.ui.theme.AppColors.Secondary else com.example.ui.theme.AppColors.TextSecondary
                )
            }

            Spacer(Modifier.height(8.dp))

            // 4 Status Segmented Options
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StatusButton(
                    label = if (lang == "bn") "জামাত" else "Jamat",
                    pts = "+12",
                    isSelected = currentStatus == PrayerStatus.JAMAT,
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                    onClick = { onStatusChange(PrayerStatus.JAMAT) }
                )
                StatusButton(
                    label = if (lang == "bn") "একাকী" else "Alone",
                    pts = "+7",
                    isSelected = currentStatus == PrayerStatus.ALONE,
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                    onClick = { onStatusChange(PrayerStatus.ALONE) }
                )
                StatusButton(
                    label = if (lang == "bn") "ক্বাযা" else "Qaza",
                    pts = "0",
                    isSelected = currentStatus == PrayerStatus.QAZA,
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                    onClick = { onStatusChange(PrayerStatus.QAZA) }
                )
                StatusButton(
                    label = if (lang == "bn") "মিস" else "Missed",
                    pts = "0",
                    isSelected = currentStatus == PrayerStatus.MISSED,
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                    onClick = { onStatusChange(PrayerStatus.MISSED) }
                )
            }
        }
    }
}

// 4 equal chips with a 1dp #874D14@40% border. Selected = #FFB030 fill + #1F1F1F bold text + a check icon. Unselected = white with #5A4632 text. Points ("+12") shown under each label at 12sp.
@Composable
private fun StatusButton(
    label: String,
    pts: String,
    isSelected: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick),
        color = if (isSelected) com.example.ui.theme.AppColors.Primary.copy(alpha = 0.28f) else com.example.ui.theme.AppColors.WarmTintContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 5.dp, horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = com.example.ui.theme.AppColors.Secondary
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 12.sp
                    ),
                    color = if (isSelected) com.example.ui.theme.AppColors.Secondary else com.example.ui.theme.AppColors.TextSecondary,
                    maxLines = 1
                )
            }
            Text(
                text = pts,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFeatureSettings = "tnum"
                ),
                color = if (isSelected) com.example.ui.theme.AppColors.Secondary else com.example.ui.theme.AppColors.TextSecondary
            )
        }
    }
}

@Composable
private fun NaflCheckboxRow(
    title: String,
    desc: String,
    checked: Boolean,
    enabled: Boolean,
    testTag: String,
    onCheckedChange: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onCheckedChange)
            .testTag(testTag)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        com.example.ui.components.SoftSwitch(
            checked = checked,
            onCheckedChange = if (enabled) ({ _: Boolean -> onCheckedChange() }) else null,
            enabled = enabled
        )
    }
}

@Composable
private fun SectionCheckboxRow(
    title: String,
    checked: Boolean,
    enabled: Boolean,
    testTag: String,
    onCheckedChange: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onCheckedChange)
            .testTag(testTag)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
        com.example.ui.components.SoftSwitch(
            checked = checked,
            onCheckedChange = if (enabled) ({ _: Boolean -> onCheckedChange() }) else null,
            enabled = enabled
        )
    }
}

@Composable
private fun MonthlyCalendarCard(
    allDays: List<DayRecord>,
    selectedDate: String,
    todayDate: String,
    lang: String,
    onDateSelect: (String) -> Unit
) {
    val dayMap = remember(allDays) { allDays.associateBy { it.date } }

    val cal = Calendar.getInstance().apply {
        timeZone = TimeZone.getDefault()
    }

    // Days in current month
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val curYear = cal.get(Calendar.YEAR)
    val curMonth = cal.get(Calendar.MONTH) + 1

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = com.example.ui.theme.AppColors.Surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = Strings.t("calendar_view", lang),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                ),
                color = com.example.ui.theme.AppColors.Secondary
            )
            Spacer(Modifier.height(10.dp))

            // Grid of days in month
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val dates = (1..daysInMonth).map { dayNum ->
                    String.format("%04d-%02d-%02d", curYear, curMonth, dayNum)
                }

                val rows = dates.chunked(7)
                for (row in rows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (dStr in row) {
                            val isFuture = dStr > todayDate
                            val isSelected = dStr == selectedDate
                            val record = dayMap[dStr]
                            val score = record?.score ?: 0
                            val isPerfect = record?.perfect == true
                            val dayNumber = dStr.takeLast(2).toIntOrNull() ?: 1

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isFuture -> com.example.ui.theme.AppColors.DisabledTrack.copy(alpha = 0.3f)
                                            isPerfect -> com.example.ui.theme.AppColors.Primary
                                            score > 50 -> com.example.ui.theme.AppColors.WarmTintAlternate
                                            score > 0 -> com.example.ui.theme.AppColors.WarmTintContainer
                                            else -> com.example.ui.theme.AppColors.DisabledTrack.copy(alpha = 0.2f)
                                        }
                                    )
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) com.example.ui.theme.AppColors.Secondary else com.example.ui.theme.AppColors.BorderSubtle,
                                        shape = CircleShape
                                    )
                                    .clickable(enabled = !isFuture) {
                                        onDateSelect(dStr)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "$dayNumber",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            fontFeatureSettings = "tnum"
                                        ),
                                        color = if (isFuture) com.example.ui.theme.AppColors.DisabledTrack
                                        else if (isPerfect) com.example.ui.theme.AppColors.OnPrimary
                                        else com.example.ui.theme.AppColors.TextPrimary
                                    )
                                    if (score > 0 && !isFuture) {
                                        Text(
                                            text = "$score",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                fontFeatureSettings = "tnum"
                                            ),
                                            color = if (isPerfect) com.example.ui.theme.AppColors.OnPrimary else com.example.ui.theme.AppColors.Secondary
                                        )
                                    }
                                }
                            }
                        }
                        // Fill empty if last row has fewer than 7
                        if (row.size < 7) {
                            for (i in 0 until (7 - row.size)) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoreTrendsCard(allDays: List<DayRecord>, lang: String) {
    val dayMap = remember(allDays) { allDays.associateBy { it.date } }
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getDefault()
    }

    // Past 7 days
    val past7Dates = (6 downTo 0).map { offset ->
        val c = Calendar.getInstance().apply {
            timeZone = TimeZone.getDefault()
            add(Calendar.DAY_OF_YEAR, -offset)
        }
        val dateStr = dateFormat.format(c.time)
        val dayLabel = SimpleDateFormat("EEE", Locale.US).format(c.time)
        val rec = dayMap[dateStr]
        TrendDay(dayLabel, rec?.score ?: 0, rec?.perfect == true, rec?.quran == true && rec.isQuranApplicable)
    }
    val quranDays = past7Dates.count { it.quran }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = com.example.ui.theme.AppColors.Surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = Strings.t("score_history", lang),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                ),
                color = com.example.ui.theme.AppColors.Secondary
            )
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(138.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                past7Dates.forEach { (label, score, isPerfect, quranDone) ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (score > 0) "$score" else "-",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFeatureSettings = "tnum"
                            ),
                            color = if (score > 0) com.example.ui.theme.AppColors.Secondary else com.example.ui.theme.AppColors.TextSecondary
                        )
                        Spacer(Modifier.height(4.dp))

                        val heightFactor = (score / 100f).coerceIn(0.08f, 1f)
                        Box(
                            modifier = Modifier
                                .width(22.dp)
                                .height((70 * heightFactor).dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    when {
                                        isPerfect -> com.example.ui.theme.AppColors.Primary
                                        score > 60 -> com.example.ui.theme.AppColors.Secondary
                                        score > 0 -> com.example.ui.theme.AppColors.WarmTintAlternate
                                        else -> com.example.ui.theme.AppColors.DisabledTrack.copy(alpha = 0.4f)
                                    }
                                )
                        )
                        Spacer(Modifier.height(4.dp))
                        // Quran marker: book icon when Quran was marked Done that day
                        Box(modifier = Modifier.size(14.dp), contentAlignment = Alignment.Center) {
                            if (quranDone) {
                                Icon(
                                    imageVector = Icons.Outlined.AutoStories,
                                    contentDescription = Strings.t("quran_title", lang),
                                    tint = com.example.ui.theme.AppColors.Secondary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                            color = com.example.ui.theme.AppColors.TextSecondary
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(
                    imageVector = Icons.Outlined.AutoStories,
                    contentDescription = null,
                    tint = com.example.ui.theme.AppColors.Secondary,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = if (lang == "bn") "কুরআন তিলাওয়াত: গত ৭ দিনে $quranDays দিন" else "Quran read on $quranDays of the last 7 days",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp, fontFeatureSettings = "tnum"),
                    color = com.example.ui.theme.AppColors.TextSecondary
                )
            }
        }
    }
}

private data class TrendDay(val label: String, val score: Int, val perfect: Boolean, val quran: Boolean)
