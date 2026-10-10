package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.alarm.NotificationChannels
import com.example.alarm.SoundHelper
import com.example.model.CalculationMethod
import com.example.model.PrayerName
import com.example.model.PrayerWindow
import com.example.ui.MainViewModel
import com.example.ui.Strings
import com.example.ui.components.*
import com.example.ui.theme.AppColors
import java.text.SimpleDateFormat
import java.util.*

private fun waqtIcon(p: PrayerName): ImageVector = when (p) {
    PrayerName.TAHAJJUD -> Icons.Rounded.Bedtime
    PrayerName.FAJR -> Icons.Rounded.WbTwilight
    PrayerName.DUHA -> Icons.Rounded.LightMode
    PrayerName.DHUHR -> Icons.Rounded.WbSunny
    PrayerName.ASR -> Icons.Rounded.WbCloudy
    PrayerName.MAGHRIB -> Icons.Rounded.Brightness3
    PrayerName.ISHA -> Icons.Rounded.NightsStay
}

/**
 * Wakt screen: location & method, sun times, forbidden times, and one toggle row per waqt
 * (switch = start alarm). Tapping a row opens that waqt's notification sheet.
 */
@Composable
fun PrayerTimesScreen(viewModel: MainViewModel) {
    val settings by viewModel.settings.collectAsState()
    val schedule by viewModel.prayerSchedule.collectAsState()
    val liveTime by viewModel.liveTimeMillis.collectAsState()
    val lang = settings.language
    val bn = lang == "bn"

    var showDistrictDialog by remember { mutableStateOf(false) }
    var showMethodDialog by remember { mutableStateOf(false) }
    var sheetFor by remember { mutableStateOf<PrayerWindow?>(null) }

    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.US).apply { timeZone = TimeZone.getDefault() } }
    val method = CalculationMethod.fromId(settings.calcMethod)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp + com.example.ui.components.LocalBottomBarSpace.current),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { ScreenTitle(if (bn) "ওয়াক্ত" else "Wakt") }

        // Sun times
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SunTile(Icons.Rounded.WbSunny, Strings.t("sunrise", lang), timeFormat.format(Date(schedule.sunriseMillis)), Modifier.weight(1f))
                SunTile(Icons.Rounded.WbTwilight, Strings.t("sunset", lang), timeFormat.format(Date(schedule.maghrib.startTimeMillis)), Modifier.weight(1f))
                SunTile(Icons.Rounded.DarkMode, if (bn) "মধ্যরাত" else "Midnight", timeFormat.format(Date(schedule.midnightMillis)), Modifier.weight(1f))
            }
        }

        // Location & method (settings-row style)
        item {
            Spacer(Modifier.height(6.dp))
            SettingsGroup {
                SettingValueRow(
                    title = Strings.t("location", lang),
                    value = settings.district,
                    icon = Icons.Outlined.Place,
                    onClick = { showDistrictDialog = true },
                    modifier = Modifier.testTag("change_location_button")
                )
                SettingValueRow(
                    title = Strings.t("calculation_method", lang),
                    value = method.id,
                    subtitle = if (bn) method.titleBn else method.titleEn,
                    icon = Icons.Outlined.Calculate,
                    onClick = { showMethodDialog = true }
                )
            }
        }

        item {
            Spacer(Modifier.height(6.dp))
            SectionHeader(if (bn) "নামাজের অ্যালার্ম" else "Prayer alarms")
            Text(
                if (bn) "সুইচ = ওয়াক্ত শুরুর অ্যালার্ম। বিস্তারিত সেটিংসের জন্য ট্যাপ করুন।" else "Switch = alarm when the waqt starts. Tap a row for more options.",
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextSecondary
            )
        }

        items(schedule.allWindows(), key = { it.prayer.id }) { window ->
            val key = window.prayer.id
            val isCurrent = liveTime in window.startTimeMillis until window.endTimeMillis
            val startOn = settings.alarmToggles["start_$key"] != false
            ToggleRow(
                icon = waqtIcon(window.prayer),
                title = (if (bn) window.prayer.titleBn else window.prayer.titleEn) + if (isCurrent) (if (bn) "  • চলমান" else "  • now") else "",
                subtitle = "${timeFormat.format(Date(window.startTimeMillis))} – ${timeFormat.format(Date(window.endTimeMillis))}",
                checked = startOn,
                onCheckedChange = { viewModel.toggleAlarm("start_$key", it) },
                highlighted = isCurrent,
                onClick = { sheetFor = window },
                modifier = Modifier.testTag("prayer_window_$key"),
                switchTag = "start_alarm_switch_$key"
            )
        }

        item {
            Spacer(Modifier.height(6.dp))
            ForbiddenTimesCard(schedule = schedule, liveTime = liveTime, lang = lang, showDescriptions = true)
        }
    }

    sheetFor?.let { w -> WaqtNotificationSheet(viewModel, w, lang) { sheetFor = null } }

    if (showDistrictDialog) {
        DistrictSelectionDialog(
            districts = viewModel.districts,
            currentDistrict = settings.district,
            lang = lang,
            onDismiss = { showDistrictDialog = false },
            onSelect = {
                viewModel.selectDistrict(it)
                showDistrictDialog = false
            },
            onGpsSelect = { viewModel.requestGpsLocation { success -> if (success) showDistrictDialog = false } }
        )
    }

    if (showMethodDialog) {
        SoftSheet(title = Strings.t("calc_method_label", lang), onDismiss = { showMethodDialog = false }) {
            CalculationMethod.entries.forEach { m ->
                val selected = m.id.equals(settings.calcMethod, ignoreCase = true)
                SettingRow(
                    title = if (bn) m.titleBn else m.titleEn,
                    subtitle = "Fajr ${m.fajrAngle}° • Isha ${if (m == CalculationMethod.UMM_AL_QURA) "90 min" else "${m.ishaAngle}°"} • Hanafi Asr",
                    onClick = {
                        viewModel.updateCalculationMethod(m.id)
                        showMethodDialog = false
                    }
                ) {
                    RadioButton(selected = selected, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = AppColors.Action))
                }
            }
        }
    }
}

@Composable
private fun SunTile(icon: ImageVector, label: String, time: String, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(AppColors.WarmTintContainer).padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = AppColors.Primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = AppColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(time, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"), color = AppColors.TextPrimary, maxLines = 1)
    }
}

/** Per-waqt notification sheet (reference): start alarm, 15-min warning, sound preview; Cancel / Save. */
@Composable
private fun WaqtNotificationSheet(viewModel: MainViewModel, window: PrayerWindow, lang: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val bn = lang == "bn"
    val key = window.prayer.id
    var start by remember { mutableStateOf(settings.alarmToggles["start_$key"] != false) }
    var end by remember { mutableStateOf(settings.alarmToggles["end_$key"] != false) }
    val waqt = NotificationChannels.Waqt.of(window.prayer)
    val custom = settings.customSoundNames[waqt.soundKey]
    var player by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
    DisposableEffect(Unit) { onDispose { runCatching { player?.release() } } }

    SoftSheet(
        title = (if (bn) window.prayer.titleBn else window.prayer.titleEn) + if (bn) " নোটিফিকেশন" else " notification",
        onDismiss = onDismiss,
        cancelText = Strings.t("cancel", lang),
        confirmText = Strings.t("save", lang),
        onConfirm = {
            viewModel.setWaqtAlarms(key, start, end)
            onDismiss()
        }
    ) {
        SettingSwitchRow(
            title = if (bn) "ওয়াক্ত শুরুর অ্যালার্ম" else "Alarm when waqt starts",
            subtitle = if (bn) "নিজস্ব সাউন্ডসহ ৪৫ সেকেন্ড পর্যন্ত বাজবে" else "Rings with its own sound for up to 45 s",
            checked = start,
            onCheckedChange = { start = it }
        )
        SettingSwitchRow(
            title = Strings.t("end_warning_15m", lang),
            subtitle = if (bn) "ওয়াক্ত শেষ হওয়ার ১৫ মিনিট আগে" else "Notification 15 minutes before it ends",
            checked = end,
            onCheckedChange = { end = it }
        )
        SettingRow(
            title = if (bn) "অ্যালার্ম সাউন্ড" else "Alarm sound",
            subtitle = custom ?: (if (bn) "ডিফল্ট টোন" else "Default ${window.prayer.titleEn.substringBefore(" /")} tone") +
                (if (bn) " • পরিবর্তন: আরও › কাস্টম সাউন্ড" else " • change in More › Custom sounds"),
            onClick = {
                runCatching { player?.release() }
                player = SoundHelper.playPreviewSound(context, waqt.soundKey)
            }
        ) {
            Box(Modifier.size(34.dp).clip(CircleShape).background(AppColors.Surface), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.PlayArrow, contentDescription = if (bn) "শুনুন" else "Preview", tint = AppColors.Secondary)
            }
        }
    }
}
