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
            soundKey = SoundHelper.SOUND_FAJR,
            titleBn = "ফজর সালাত অ্যালার্ম টোন",
            titleEn = "Fajr Prayer Alarm Tone",
            descBn = "ফজরের ওয়াক্ত শুরু হলে স্বয়ংক্রিয়ভাবে এই আযান বা সাউন্ড বাজবে",
            descEn = "Plays when Fajr prayer window begins"
        ),
        SoundConfigItem(
            soundKey = SoundHelper.SOUND_DUHR,
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isBn) "সাউন্ড ও ফন্ট সেটিংস" else "Sounds & Font Settings",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("custom_sounds_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Arabic Font Resizer Section
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("arabic_font_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FormatSize,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = if (isBn) "আরবি ফন্ট সাইজ নিয়ন্ত্রণ" else "Arabic Font Size Control",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = if (isBn) "পড়ার সুবিধার্থে ফন্ট বড় বা ছোট করুন" else "Enlarge or shrink Arabic text size",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Live Arabic Preview Box
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ ۞ قُلْ هُوَ اللَّهُ أَحَدٌ",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontSize = settings.arabicFontSize.sp,
                                    lineHeight = (settings.arabicFontSize * 1.55f).sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            )
                        }

                        // Stepper / Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            FilledTonalButton(
                                onClick = { viewModel.updateArabicFontSize(settings.arabicFontSize - 2) },
                                enabled = settings.arabicFontSize > 18
                            ) {
                                Text("A -  ছোট", fontWeight = FontWeight.Bold)
                            }

                            Text(
                                text = "${settings.arabicFontSize} sp",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )

                            FilledTonalButton(
                                onClick = { viewModel.updateArabicFontSize(settings.arabicFontSize + 2) },
                                enabled = settings.arabicFontSize < 42
                            ) {
                                Text("A +  বড়", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Hijri Date Adjustment Section
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("hijri_adjustment_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = if (isBn) "হিজরি তারিখ সমন্বয় (চাঁদ দেখা)" else "Hijri Date Adjustment (Moon Sighting)",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = if (isBn) "স্থানীয় চাঁদ দেখার ভিত্তিতে তারিখ সমন্বয় করুন" else "Fine-tune Hijri day to match local sighting",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        val cal = remember { Calendar.getInstance(TimeZone.getTimeZone("Asia/Dhaka")) }
                        val previewHijri = CalendarDateHelper.getHijriDate(cal, isBn, settings.hijriOffset)

                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "${previewHijri.day} ${previewHijri.monthName} ${previewHijri.year} AH (${previewHijri.dayName})",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(10.dp),
                                textAlign = TextAlign.Center
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            FilledTonalButton(
                                onClick = { viewModel.updateHijriOffset(settings.hijriOffset - 1) },
                                enabled = settings.hijriOffset > -2
                            ) {
                                Text("- ১ দিন")
                            }

                            Text(
                                text = "${if (settings.hijriOffset > 0) "+" else ""}${settings.hijriOffset} দিন",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )

                            FilledTonalButton(
                                onClick = { viewModel.updateHijriOffset(settings.hijriOffset + 1) },
                                enabled = settings.hijriOffset < 2
                            ) {
                                Text("+ ১ দিন")
                            }
                        }
                    }
                }
            }

            // Custom Notification Sounds Header
            item {
                Column(modifier = Modifier.padding(top = 4.dp)) {
                    Text(
                        text = if (isBn) "কাস্টম নোটিফিকেশন ও অ্যালার্ম সাউন্ড" else "Custom Notification & Alarm Sounds",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (isBn)
                            "সরাসরি আপনার ফোন থেকে যেকোনো পছন্দের আযান বা অডিও ফাইল (.mp3) যোগ করুন"
                        else
                            "Add any custom Adhan or audio file (.mp3) directly from your phone",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Sounds List
            items(soundsList.size) { index ->
                val item = soundsList[index]
                val customDisplayName = settings.customSoundNames[item.soundKey]
                val hasCustom = SoundHelper.hasCustomSound(context, item.soundKey)
                val isCurrentlyPlaying = playingSound == item.soundKey

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sound_card_${item.soundKey}"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isBn) item.titleBn else item.titleEn,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isBn) item.descBn else item.descEn,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Current Tone Status Badge
                        Surface(
                            color = if (hasCustom) Color(0xFFFFF0CC) else MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            border = if (hasCustom) BorderStroke(1.dp, Color(0xFFFFB030)) else null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (hasCustom) Icons.Default.CheckCircle else Icons.Default.NotificationsNone,
                                    contentDescription = null,
                                    tint = if (hasCustom) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (hasCustom) {
                                        val label = customDisplayName ?: "custom_sound.mp3"
                                        if (isBn) "কাস্টম সাউন্ড সক্রিয়: $label" else "Custom audio active: $label"
                                    } else {
                                        if (isBn) "সিস্টেম ডিফল্ট নোটিফিকেশন টোন (ফলব্যাক)" else "Default system tone (Fallback)"
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (hasCustom) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }

                        // Action Buttons: Choose File, Reset, Preview
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1. Choose from device button
                            Button(
                                onClick = {
                                    activePickingSoundKey = item.soundKey
                                    audioPickerLauncher.launch("audio/*")
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AudioFile,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = if (isBn) "সাউন্ড যোগ করুন" else "Choose Audio",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }

                            // 2. Play / Preview button
                            FilledTonalIconButton(
                                onClick = {
                                    if (isCurrentlyPlaying) {
                                        currentPlayer?.stop()
                                        currentPlayer?.release()
                                        currentPlayer = null
                                        playingSound = null
                                    } else {
                                        currentPlayer?.stop()
                                        currentPlayer?.release()
                                        playingSound = item.soundKey
                                        currentPlayer = SoundHelper.playPreviewSound(context, item.soundKey) {
                                            playingSound = null
                                            currentPlayer = null
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (isCurrentlyPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = "Preview Tone",
                                    tint = if (isCurrentlyPlaying) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                            }

                            // 3. Reset button (if custom sound exists)
                            if (hasCustom) {
                                IconButton(
                                    onClick = {
                                        SoundHelper.removeCustomAudio(context, item.soundKey)
                                        viewModel.unregisterCustomSound(item.soundKey)
                                        val msg = if (isBn) "ডিফল্ট টোনে রিসেট করা হয়েছে" else "Reset to default tone"
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.RestartAlt,
                                        contentDescription = "Reset to Default",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
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
