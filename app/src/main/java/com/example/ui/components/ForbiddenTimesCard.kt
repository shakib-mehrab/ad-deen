package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DoNotDisturb
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DailyPrayerSchedule
import com.example.ui.Strings
import com.example.ui.theme.AppColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Forbidden (makruh) prayer times. The windows come straight from [DailyPrayerSchedule.forbiddenTimes],
 * i.e. the same PrayerCalculator run, location and time zone as every other time in the app.
 *
 * Layout: one row per window (name + optional description on the left, start/end stacked on the right),
 * so nothing is squeezed into thirds: fits 320dp at 1.3x font and long Bangla labels; text wraps or ellipsizes.
 */
@Composable
fun ForbiddenTimesCard(
    schedule: DailyPrayerSchedule,
    liveTime: Long,
    lang: String,
    showDescriptions: Boolean,
    modifier: Modifier = Modifier,
    testTag: String = "forbidden_times_card",
    onClick: (() -> Unit)? = null
) {
    val isBn = lang == "bn"
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.US).apply { timeZone = TimeZone.getDefault() } }
    val active = schedule.activeForbiddenTime(liveTime)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (active != null) AppColors.WarmTintAlternate else AppColors.Surface),
        border = BorderStroke(1.dp, if (active != null) AppColors.Secondary else AppColors.BorderSubtle),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    imageVector = Icons.Outlined.DoNotDisturb,
                    contentDescription = null,
                    tint = AppColors.Secondary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = Strings.t("forbidden_times", lang),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
                    color = AppColors.Secondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (active != null) {
                    Surface(color = AppColors.Secondary, shape = RoundedCornerShape(6.dp)) {
                        Text(
                            text = if (isBn) "চলমান" else "ACTIVE",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                            color = AppColors.OnSecondary,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            if (active != null) {
                val remainingSecs = ((active.endTimeMillis - liveTime) / 1000).coerceAtLeast(0)
                val mmss = String.format(Locale.US, "%02d:%02d", remainingSecs / 60, remainingSecs % 60)
                Text(
                    text = if (isBn) "সতর্কতা: ${active.titleBn} চলছে — শেষ হতে বাকি $mmss"
                    else "${active.titleEn} is active — ends in $mmss",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp, fontFeatureSettings = "tnum"),
                    color = AppColors.Secondary
                )
            }

            schedule.forbiddenTimes.forEach { fb ->
                val isCur = liveTime in fb.startTimeMillis until fb.endTimeMillis
                Surface(
                    color = if (isCur) AppColors.WarmTintAlternate else AppColors.WarmTintContainer,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (isCur) AppColors.Secondary else AppColors.BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isBn) fb.titleBn else fb.titleEn,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                                color = AppColors.Secondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (showDescriptions) {
                                Text(
                                    text = if (isBn) fb.descBn else fb.descEn,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                    color = AppColors.TextSecondary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = timeFormat.format(Date(fb.startTimeMillis)),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp, fontFeatureSettings = "tnum"),
                                color = if (isCur) AppColors.Secondary else AppColors.TextPrimary,
                                maxLines = 1,
                                softWrap = false,
                                textAlign = TextAlign.End
                            )
                            Text(
                                text = "– " + timeFormat.format(Date(fb.endTimeMillis)),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium, fontSize = 12.sp, fontFeatureSettings = "tnum"),
                                color = AppColors.TextSecondary,
                                maxLines = 1,
                                softWrap = false,
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }
            }
        }
    }
}
