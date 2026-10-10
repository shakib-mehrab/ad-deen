package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CalculationMethod
import com.example.model.District
import com.example.model.PrayerName
import com.example.model.PrayerWindow
import com.example.ui.MainViewModel
import com.example.ui.Strings
import com.example.ui.components.DistrictSelectionDialog
import com.example.ui.components.PrayerSolarWindowCard
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerTimesScreen(
    viewModel: MainViewModel
) {
    val settings by viewModel.settings.collectAsState()
    val schedule by viewModel.prayerSchedule.collectAsState()
    val liveTime by viewModel.liveTimeMillis.collectAsState()
    val lang = settings.language

    var showDistrictDialog by remember { mutableStateOf(false) }
    var showMethodDialog by remember { mutableStateOf(false) }

    val timeFormat = remember {
        SimpleDateFormat("hh:mm a", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("Asia/Dhaka")
        }
    }

    val windows = schedule.allWindows()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Solar Timing Window Header (as requested)
        item {
            PrayerSolarWindowCard(
                schedule = schedule,
                liveTime = liveTime,
                lang = lang,
                onClick = { }
            )
        }

        // Location & Method Info Bar
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = com.example.ui.theme.AppColors.Surface
                ),
                border = BorderStroke(1.dp, com.example.ui.theme.AppColors.BorderSubtle),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = com.example.ui.theme.AppColors.Secondary
                            )
                            Column {
                                Text(
                                    text = settings.district,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = com.example.ui.theme.AppColors.TextPrimary
                                )
                                Text(
                                    text = "${String.format(Locale.US, "%.3f", settings.lat)}° N, ${String.format(Locale.US, "%.3f", settings.lng)}° E",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                    color = com.example.ui.theme.AppColors.TextSecondary
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = { showDistrictDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, com.example.ui.theme.AppColors.Secondary),
                            modifier = Modifier.testTag("change_location_button")
                        ) {
                            Text(
                                Strings.t("change_location", lang),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                ),
                                color = com.example.ui.theme.AppColors.Secondary
                            )
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        thickness = 1.dp,
                        color = com.example.ui.theme.AppColors.Divider
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showMethodDialog = true },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = Strings.t("calculation_method", lang),
                                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                                color = com.example.ui.theme.AppColors.TextSecondary
                            )
                            Text(
                                text = CalculationMethod.fromId(settings.calcMethod).titleEn,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                ),
                                color = com.example.ui.theme.AppColors.Secondary
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = com.example.ui.theme.AppColors.TextSecondary
                        )
                    }
                }
            }
        }

        // Celestial Horizon Bar: Sunrise, Sunset & Islamic Midnight (distinct icons & colors)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Sunrise
                Surface(
                    modifier = Modifier.weight(1f),
                    color = com.example.ui.theme.AppColors.WarmTintContainer,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, com.example.ui.theme.AppColors.BorderSubtle)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(
                                imageVector = Icons.Default.WbSunny,
                                contentDescription = null,
                                tint = com.example.ui.theme.AppColors.Secondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = Strings.t("sunrise", lang),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                ),
                                color = com.example.ui.theme.AppColors.Secondary
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = timeFormat.format(Date(schedule.sunriseMillis)),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                fontFeatureSettings = "tnum"
                            ),
                            color = com.example.ui.theme.AppColors.TextPrimary
                        )
                    }
                }

                // Sunset
                Surface(
                    modifier = Modifier.weight(1f),
                    color = com.example.ui.theme.AppColors.WarmTintContainer,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, com.example.ui.theme.AppColors.BorderSubtle)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(
                                imageVector = Icons.Default.WbTwilight,
                                contentDescription = null,
                                tint = com.example.ui.theme.AppColors.Secondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = Strings.t("sunset", lang),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                ),
                                color = com.example.ui.theme.AppColors.Secondary
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = timeFormat.format(Date(schedule.maghrib.startTimeMillis)),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                fontFeatureSettings = "tnum"
                            ),
                            color = com.example.ui.theme.AppColors.TextPrimary
                        )
                    }
                }

                // Midnight
                Surface(
                    modifier = Modifier.weight(1f),
                    color = com.example.ui.theme.AppColors.WarmTintContainer,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, com.example.ui.theme.AppColors.BorderSubtle)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(
                                imageVector = Icons.Default.Bedtime,
                                contentDescription = null,
                                tint = com.example.ui.theme.AppColors.Secondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = Strings.t("midnight", lang),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                ),
                                color = com.example.ui.theme.AppColors.Secondary
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = timeFormat.format(Date(schedule.midnightMillis)),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                fontFeatureSettings = "tnum"
                            ),
                            color = com.example.ui.theme.AppColors.TextPrimary
                        )
                    }
                }
            }
        }

        // Forbidden Prayer Times (মাকরুহ ওয়াক্ত / নিষিদ্ধ সময়) Card
        item {
            val activeForbidden = schedule.activeForbiddenTime(liveTime)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("forbidden_times_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (activeForbidden != null) com.example.ui.theme.AppColors.WarmTintAlternate else com.example.ui.theme.AppColors.Surface
                ),
                border = BorderStroke(
                    1.dp,
                    if (activeForbidden != null) com.example.ui.theme.AppColors.Secondary else com.example.ui.theme.AppColors.BorderSubtle
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(
                                imageVector = Icons.Outlined.DoNotDisturb,
                                contentDescription = "Forbidden",
                                tint = com.example.ui.theme.AppColors.Secondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = Strings.t("forbidden_times", lang),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp
                                ),
                                color = com.example.ui.theme.AppColors.Secondary
                            )
                            Surface(
                                color = com.example.ui.theme.AppColors.WarmTintContainer,
                                shape = RoundedCornerShape(6.dp),
                                border = BorderStroke(1.dp, com.example.ui.theme.AppColors.BorderSubtle)
                            ) {
                                Text(
                                    text = if (lang == "bn") "নিষিদ্ধ" else "Forbidden",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    ),
                                    color = com.example.ui.theme.AppColors.Secondary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        if (activeForbidden != null) {
                            Surface(
                                color = com.example.ui.theme.AppColors.Secondary,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = if (lang == "bn") "চলমান" else "ACTIVE NOW",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    ),
                                    color = com.example.ui.theme.AppColors.OnSecondary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    if (activeForbidden != null) {
                        val remainingSecs = ((activeForbidden.endTimeMillis - liveTime) / 1000).coerceAtLeast(0)
                        val m = remainingSecs / 60
                        val s = remainingSecs % 60
                        Text(
                            text = if (lang == "bn") "সতর্কতা: ${activeForbidden.titleBn} চলছে (শেষ হতে বাকি ${String.format(Locale.US, "%02d:%02d", m, s)})"
                            else "Warning: ${activeForbidden.titleEn} is active (Ends in ${String.format(Locale.US, "%02d:%02d", m, s)})",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                fontFeatureSettings = "tnum"
                            ),
                            color = com.example.ui.theme.AppColors.Secondary
                        )
                    }

                    // 3 Forbidden Windows
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        schedule.forbiddenTimes.forEach { fb ->
                            val isFbCurrent = liveTime in fb.startTimeMillis until fb.endTimeMillis
                            val fbStartStr = timeFormat.format(Date(fb.startTimeMillis))
                            val fbEndStr = timeFormat.format(Date(fb.endTimeMillis))

                            Surface(
                                color = if (isFbCurrent) com.example.ui.theme.AppColors.WarmTintAlternate else com.example.ui.theme.AppColors.WarmTintContainer,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (isFbCurrent) com.example.ui.theme.AppColors.Secondary else com.example.ui.theme.AppColors.BorderSubtle
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = if (lang == "bn") fb.titleBn else fb.titleEn,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 14.sp
                                            ),
                                            color = com.example.ui.theme.AppColors.Secondary
                                        )
                                        Text(
                                            text = if (lang == "bn") fb.descBn else fb.descEn,
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                            color = com.example.ui.theme.AppColors.TextSecondary
                                        )
                                    }

                                    Text(
                                        text = "$fbStartStr - $fbEndStr",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            fontFeatureSettings = "tnum"
                                        ),
                                        color = if (isFbCurrent) com.example.ui.theme.AppColors.Secondary else com.example.ui.theme.AppColors.TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 7 Prayer Windows List
        items(windows) { window ->
            val isCurrent = liveTime in window.startTimeMillis until window.endTimeMillis
            val prayerKey = window.prayer.id
            val title = if (lang == "bn") window.prayer.titleBn else window.prayer.titleEn

            val startToggleKey = "start_$prayerKey"
            val endToggleKey = "end_$prayerKey"

            val startAlarmEnabled = settings.alarmToggles[startToggleKey] ?: true
            val endAlarmEnabled = settings.alarmToggles[endToggleKey] ?: true

            val startStr = timeFormat.format(Date(window.startTimeMillis))
            val endStr = timeFormat.format(Date(window.endTimeMillis))

            // Current prayer card (Wakt, Dhuhr): white surface, 2dp #874D14 border
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("prayer_window_${prayerKey}"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = com.example.ui.theme.AppColors.Surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = BorderStroke(
                    if (isCurrent) 2.dp else 1.dp,
                    if (isCurrent) com.example.ui.theme.AppColors.Secondary else com.example.ui.theme.AppColors.BorderSubtle
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    ),
                                    color = com.example.ui.theme.AppColors.TextPrimary
                                )
                                if (isCurrent) {
                                    // "Wakt" badge = #FFB030 fill + #1F1F1F text
                                    Surface(
                                        color = com.example.ui.theme.AppColors.Primary,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = if (lang == "bn") "ওয়াক্ত" else "Wakt",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            ),
                                            color = com.example.ui.theme.AppColors.OnPrimary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "$startStr – $endStr",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    fontFeatureSettings = "tnum"
                                ),
                                color = if (isCurrent) com.example.ui.theme.AppColors.Secondary else com.example.ui.theme.AppColors.TextSecondary
                            )
                        }

                        // Countdown box = #874D14 border/text on white (tabular numerals)
                        if (isCurrent) {
                            val remainingSeconds = ((window.endTimeMillis - liveTime) / 1000).coerceAtLeast(0)
                            val remMins = remainingSeconds / 60
                            val remSecs = remainingSeconds % 60
                            Surface(
                                color = com.example.ui.theme.AppColors.Surface,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(2.dp, com.example.ui.theme.AppColors.Secondary)
                            ) {
                                Text(
                                    text = String.format("%02d:%02d", remMins, remSecs),
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp,
                                        fontFeatureSettings = "tnum"
                                    ),
                                    color = com.example.ui.theme.AppColors.Secondary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        thickness = 1.dp,
                        color = com.example.ui.theme.AppColors.Divider
                    )

                    // Alarm Toggles: ON = #874D14 track + white thumb; OFF = #E3D9C0 track + white thumb.
                    // Keep alarm icons in #874D14. Touch target at least 48x48dp.
                    val toggleColors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = com.example.ui.theme.AppColors.Secondary,
                        checkedBorderColor = com.example.ui.theme.AppColors.Secondary,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = com.example.ui.theme.AppColors.DisabledTrack,
                        uncheckedBorderColor = com.example.ui.theme.AppColors.DisabledTrack
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Start Alarm Toggle
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = if (startAlarmEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                                contentDescription = null,
                                tint = com.example.ui.theme.AppColors.Secondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = Strings.t("start_alarm", lang),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                                color = com.example.ui.theme.AppColors.TextPrimary
                            )
                            Switch(
                                checked = startAlarmEnabled,
                                onCheckedChange = { viewModel.toggleAlarm(startToggleKey, it) },
                                colors = toggleColors,
                                modifier = Modifier.testTag("start_alarm_switch_$prayerKey")
                            )
                        }

                        // 15m End Warning Alarm Toggle
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = if (endAlarmEnabled) Icons.Default.AlarmOn else Icons.Default.AlarmOff,
                                contentDescription = null,
                                tint = com.example.ui.theme.AppColors.Secondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = Strings.t("end_warning_15m", lang),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                                color = com.example.ui.theme.AppColors.TextPrimary
                            )
                            Switch(
                                checked = endAlarmEnabled,
                                onCheckedChange = { viewModel.toggleAlarm(endToggleKey, it) },
                                colors = toggleColors,
                                modifier = Modifier.testTag("end_alarm_switch_$prayerKey")
                            )
                        }
                    }
                }
            }
        }
    }

    // District Selector Dialog
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
            onGpsSelect = {
                viewModel.requestGpsLocation { success ->
                    if (success) showDistrictDialog = false
                }
            }
        )
    }

    // Calculation Method Dialog
    if (showMethodDialog) {
        CalculationMethodDialog(
            currentMethod = settings.calcMethod,
            lang = lang,
            onDismiss = { showMethodDialog = false },
            onSelect = {
                viewModel.updateCalculationMethod(it.id)
                showMethodDialog = false
            }
        )
    }
}

private fun Modifier.scale(scale: Float): Modifier = this.then(
    Modifier.size(width = (48 * scale).dp, height = (32 * scale).dp)
)



@Composable
private fun CalculationMethodDialog(
    currentMethod: String,
    lang: String,
    onDismiss: () -> Unit,
    onSelect: (CalculationMethod) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Strings.t("calc_method_label", lang)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CalculationMethod.entries.forEach { method ->
                    val isSelected = method.id.equals(currentMethod, ignoreCase = true)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(method) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(10.dp),
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            RadioButton(selected = isSelected, onClick = { onSelect(method) })
                            Column {
                                Text(
                                    text = if (lang == "bn") method.titleBn else method.titleEn,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "Fajr: ${method.fajrAngle}° • Isha: ${if (method == CalculationMethod.UMM_AL_QURA) "90m" else "${method.ishaAngle}°"} (Hanafi Asr)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(Strings.t("cancel", lang))
            }
        }
    )
}
