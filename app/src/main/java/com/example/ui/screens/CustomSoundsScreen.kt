package com.example.ui.screens

import android.media.MediaPlayer
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alarm.SoundHelper
import com.example.ui.MainViewModel
import com.example.ui.components.AdDeenBrandHeader
import com.example.ui.components.AdDeenLogo
import com.example.ui.components.CalendarDateHelper
import java.util.Calendar
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomSoundsAndLogoSubScreen(
    viewModel: MainViewModel,
    lang: String,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val isBn = lang == "bn"
    val settings by viewModel.settings.collectAsState()

    var activePickingSoundKey by remember { mutableStateOf<String?>(null) }
    var playingSound by remember { mutableStateOf<String?>(null) }
    var currentPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    // Native Audio Picker launcher
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        val key = activePickingSoundKey
        if (uri != null && key != null) {
            val savedFileName = SoundHelper.saveCustomAudio(context, key, uri)
            viewModel.registerCustomSound(key, savedFileName)
            val msg = if (isBn) "কাস্টম সাউন্ড সেট করা হয়েছে: $savedFileName" else "Custom audio set: $savedFileName"
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
        activePickingSoundKey = null
    }

    DisposableEffect(Unit) {
        onDispose {
            currentPlayer?.release()
            currentPlayer = null
        }
    }

    val soundsList = listOf(
        SoundConfigItem(
            soundKey = SoundHelper.SOUND_ISTIGFAR,
            titleBn = "ইস্তিগফার স্মরণিকা সাউন্ড",
            titleEn = "Istigfar Reminder Sound",
            descBn = "পর্যায়ক্রমিক ইস্তিগফার সতর্কবার্তা চলাকালে এই সাউন্ড বেজে উঠবে",
            descEn = "Plays when periodic Istigfar reminders trigger"
        ),
        SoundConfigItem(
            soundKey = SoundHelper.SOUND_DARUD,
            titleBn = "দরূদ শরীফ স্মরণিকা সাউন্ড",
            titleEn = "Darud Sharif Reminder Sound",
            descBn = "পর্যায়ক্রমিক দরূদ শরীফ সতর্কবার্তা চলাকালে এই সাউন্ড বেজে উঠবে",
            descEn = "Plays when periodic Darud reminders trigger"
        ),
        SoundConfigItem(
            soundKey = SoundHelper.SOUND_TAHAJJUD,
            titleBn = "তাহাজ্জুদ অ্যালার্ম টোন",
            titleEn = "Tahajjud Alarm Tone",
            descBn = "তাহাজ্জুদের ওয়াক্ত শুরু হলে এই সাউন্ড বাজবে",
            descEn = "Plays when the Tahajjud window begins"
        ),
        SoundConfigItem(
            soundKey = SoundHelper.SOUND_FAJR,
            titleBn = "ফজর সালাত অ্যালার্ম টোন",
            titleEn = "Fajr Prayer Alarm Tone",
            descBn = "ফজরের ওয়াক্ত শুরু হলে স্বয়ংক্রিয়ভাবে এই আযান বা সাউন্ড বাজবে",
            descEn = "Plays when Fajr prayer window begins"
        ),
        SoundConfigItem(
            soundKey = SoundHelper.SOUND_DUHA,
            titleBn = "দুহা / চাশত অ্যালার্ম টোন",
            titleEn = "Duha Alarm Tone",
            descBn = "দুহার ওয়াক্ত শুরু হলে এই সাউন্ড বাজবে (যোহরের সাউন্ড থেকে আলাদা)",
            descEn = "Plays when the Duha window begins (separate from Dhuhr)"
        ),
        SoundConfigItem(
            soundKey = SoundHelper.SOUND_DHUHR,
            titleBn = "যোহর সালাত অ্যালার্ম টোন",
            titleEn = "Dhuhr Prayer Alarm Tone",
            descBn = "যোহরের ওয়াক্ত শুরু হলে স্বয়ংক্রিয়ভাবে এই আযান বা সাউন্ড বাজবে",
            descEn = "Plays when Dhuhr prayer window begins"
        ),
        SoundConfigItem(
            soundKey = SoundHelper.SOUND_ASR,
            titleBn = "আসর সালাত অ্যালার্ম টোন",
            titleEn = "Asr Prayer Alarm Tone",
            descBn = "আসরের ওয়াক্ত শুরু হলে স্বয়ংক্রিয়ভাবে এই আযান বা সাউন্ড বাজবে",
            descEn = "Plays when Asr prayer window begins"
        ),
        SoundConfigItem(
            soundKey = SoundHelper.SOUND_MAGHRIB,
            titleBn = "মাগরিব সালাত অ্যালার্ম টোন",
            titleEn = "Maghrib Prayer Alarm Tone",
            descBn = "মাগরিবের ওয়াক্ত শুরু হলে স্বয়ংক্রিয়ভাবে এই আযান বা সাউন্ড বাজবে",
            descEn = "Plays when Maghrib prayer window begins"
        ),
        SoundConfigItem(
            soundKey = SoundHelper.SOUND_ISHA,
            titleBn = "ইশা সালাত অ্যালার্ম টোন",
            titleEn = "Isha Prayer Alarm Tone",
            descBn = "ইশার ওয়াক্ত শুরু হলে স্বয়ংক্রিয়ভাবে এই আযান বা সাউন্ড বাজবে",
            descEn = "Plays when Isha prayer window begins"
        )
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 24.dp + com.example.ui.components.LocalBottomBarSpace.current),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            com.example.ui.components.ScreenTitle(
                if (isBn) "সাউন্ড ও পড়া" else "Sounds & Reading",
                leading = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("custom_sounds_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = com.example.ui.theme.AppColors.TextPrimary)
                    }
                }
            )
        }

        // Arabic font size
        item {
            com.example.ui.components.SettingsGroup(title = if (isBn) "আরবি লেখা" else "Arabic text") {
                com.example.ui.components.GlowCard(modifier = Modifier.fillMaxWidth().testTag("arabic_font_card"), elevation = 0.dp) {
                    Text(
                        text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = settings.arabicFontSize.sp,
                            lineHeight = (settings.arabicFontSize * 1.6f).sp
                        ),
                        textAlign = TextAlign.Center,
                        color = com.example.ui.theme.AppColors.Secondary,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                    Stepper(
                        label = if (isBn) "ফন্ট সাইজ" else "Font size",
                        value = "${settings.arabicFontSize}",
                        canDecrease = settings.arabicFontSize > 18,
                        canIncrease = settings.arabicFontSize < 42,
                        onDecrease = { viewModel.updateArabicFontSize(settings.arabicFontSize - 2) },
                        onIncrease = { viewModel.updateArabicFontSize(settings.arabicFontSize + 2) }
                    )
                }
            }
        }

        // Hijri adjustment
        item {
            val cal = remember { Calendar.getInstance(TimeZone.getDefault()) }
            val previewHijri = CalendarDateHelper.getHijriDate(cal, isBn, settings.hijriOffset)
            com.example.ui.components.SettingsGroup(title = if (isBn) "হিজরি তারিখ (চাঁদ দেখা)" else "Hijri date (moon sighting)") {
                com.example.ui.components.GlowCard(modifier = Modifier.fillMaxWidth().testTag("hijri_adjustment_card"), elevation = 0.dp) {
                    Text(
                        "${previewHijri.day} ${previewHijri.monthName} ${previewHijri.year} AH",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = com.example.ui.theme.AppColors.TextPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        if (isBn) "স্থানীয় চাঁদ দেখার সাথে মেলাতে দিন সমন্বয় করুন" else "Shift by a day to match your local sighting",
                        style = MaterialTheme.typography.bodySmall,
                        color = com.example.ui.theme.AppColors.TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    Stepper(
                        label = if (isBn) "সমন্বয়" else "Adjustment",
                        value = "${if (settings.hijriOffset > 0) "+" else ""}${settings.hijriOffset} ${if (isBn) "দিন" else "day"}",
                        canDecrease = settings.hijriOffset > -2,
                        canIncrease = settings.hijriOffset < 2,
                        onDecrease = { viewModel.updateHijriOffset(settings.hijriOffset - 1) },
                        onIncrease = { viewModel.updateHijriOffset(settings.hijriOffset + 1) }
                    )
                }
            }
        }

        // Sounds
        item {
            com.example.ui.components.SettingsGroup(title = if (isBn) "অ্যালার্ম ও স্মরণিকা সাউন্ড" else "Alarm & reminder sounds") {
                Text(
                    if (isBn) "সারিতে ট্যাপ করে ফোন থেকে অডিও বেছে নিন। ▶ দিয়ে শুনুন।" else "Tap a row to choose audio from your phone. ▶ to preview.",
                    style = MaterialTheme.typography.bodySmall,
                    color = com.example.ui.theme.AppColors.TextSecondary,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )
                soundsList.forEach { item ->
                    val customDisplayName = settings.customSoundNames[item.soundKey]
                    val hasCustom = SoundHelper.hasCustomSound(context, item.soundKey)
                    val isPlaying = playingSound == item.soundKey
                    com.example.ui.components.SettingRow(
                        title = if (isBn) item.titleBn else item.titleEn,
                        subtitle = if (hasCustom) "✓ ${customDisplayName ?: "custom audio"}" else (if (isBn) "অ্যাপের নিজস্ব টোন" else "Built-in tone"),
                        onClick = {
                            activePickingSoundKey = item.soundKey
                            audioPickerLauncher.launch("audio/*")
                        },
                        modifier = Modifier.testTag("sound_card_${item.soundKey}")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (hasCustom) {
                                IconButton(
                                    onClick = {
                                        SoundHelper.removeCustomAudio(context, item.soundKey)
                                        viewModel.unregisterCustomSound(item.soundKey)
                                        Toast.makeText(context, if (isBn) "ডিফল্ট টোনে রিসেট করা হয়েছে" else "Reset to built-in tone", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.RestartAlt, contentDescription = "Reset to default", tint = com.example.ui.theme.AppColors.TextSecondary, modifier = Modifier.size(20.dp))
                                }
                            }
                            Box(
                                Modifier.size(36.dp).clip(CircleShape).background(com.example.ui.theme.AppColors.Surface)
                                    .clickable {
                                        currentPlayer?.let { runCatching { it.stop(); it.release() } }
                                        currentPlayer = null
                                        if (isPlaying) {
                                            playingSound = null
                                        } else {
                                            playingSound = item.soundKey
                                            currentPlayer = SoundHelper.playPreviewSound(context, item.soundKey) {
                                                playingSound = null
                                                currentPlayer = null
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = "Preview tone",
                                    tint = com.example.ui.theme.AppColors.Secondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** − value + control in the soft style. */
@Composable
private fun Stepper(label: String, value: String, canDecrease: Boolean, canIncrease: Boolean, onDecrease: () -> Unit, onIncrease: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(com.example.ui.theme.AppColors.WarmTintContainer).padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium), color = com.example.ui.theme.AppColors.TextPrimary, modifier = Modifier.weight(1f).padding(start = 10.dp))
        IconButton(onClick = onDecrease, enabled = canDecrease) {
            Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = if (canDecrease) com.example.ui.theme.AppColors.Secondary else com.example.ui.theme.AppColors.DisabledTrack)
        }
        Text(value, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"), color = com.example.ui.theme.AppColors.TextPrimary, modifier = Modifier.widthIn(min = 52.dp), textAlign = TextAlign.Center)
        IconButton(onClick = onIncrease, enabled = canIncrease) {
            Icon(Icons.Default.Add, contentDescription = "Increase", tint = if (canIncrease) com.example.ui.theme.AppColors.Secondary else com.example.ui.theme.AppColors.DisabledTrack)
        }
    }
}

private data class SoundConfigItem(
    val soundKey: String,
    val titleBn: String,
    val titleEn: String,
    val descBn: String,
    val descEn: String
)
