package com.example.ui.screens

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.MainViewModel
import com.example.ui.components.*
import com.example.ui.theme.AppColors
import kotlinx.coroutines.launch

private const val PAGE_COUNT = 6

/**
 * First-run experience: 2 intro slides + guided setup (language, location, alarms, optional sign-in).
 * Full-bleed animated sky + mosque skyline with a dark scrim for legible text (WCAG AA on white).
 * [onFinished] receives true when the user chose to sign in.
 */
@Composable
fun OnboardingScreen(viewModel: MainViewModel, onFinished: (openSignIn: Boolean) -> Unit) {
    val settings by viewModel.settings.collectAsState()
    val bn = settings.language == "bn"
    val pager = rememberPagerState { PAGE_COUNT }
    val scope = rememberCoroutineScope()
    fun next() = scope.launch { pager.animateScrollToPage((pager.currentPage + 1).coerceAtMost(PAGE_COUNT - 1)) }

    BackHandler(enabled = pager.currentPage > 0) { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } }


    Box(Modifier.fillMaxSize().testTag("onboarding_screen")) {
        SkyBackdrop(Modifier.fillMaxSize())
        MosqueSkyline(
            Modifier.fillMaxWidth().fillMaxHeight(0.40f).align(Alignment.TopCenter).padding(top = 110.dp, start = 12.dp, end = 12.dp)
        )

        Column(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 24.dp)) {
            // Top bar: Skip on intro slides
            Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                if (pager.currentPage < 2) {
                    Text(
                        text = if (bn) "এড়িয়ে যান" else "Skip",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = AppColors.TextPrimary,
                        modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(AppColors.Surface.copy(alpha = 0.8f))
                            .clickable { scope.launch { pager.animateScrollToPage(2) } }.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
            HorizontalPager(state = pager, modifier = Modifier.weight(1f).fillMaxWidth(), verticalAlignment = Alignment.Bottom) { page ->
                val pageOffset = (pager.currentPage - page) + pager.currentPageOffsetFraction
                Box(Modifier.fillMaxSize().alpha(1f - kotlin.math.abs(pageOffset).coerceIn(0f, 1f) * 0.6f), contentAlignment = Alignment.BottomStart) {
                    when (page) {
                        0 -> WelcomePage(bn)
                        1 -> FeaturesPage(bn)
                        2 -> LanguagePage(settings.language) { viewModel.updateLanguage(it) }
                        3 -> LocationPage(viewModel, bn, settings.district)
                        4 -> AlarmsPage(bn)
                        else -> SignInPage(bn)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            PageDots(PAGE_COUNT, pager.currentPage)
            Spacer(Modifier.height(18.dp))
            if (pager.currentPage < PAGE_COUNT - 1) {
                PrimaryPillButton(
                    text = when (pager.currentPage) {
                        0 -> if (bn) "শুরু করুন" else "Get Started"
                        else -> if (bn) "পরবর্তী" else "Continue"
                    },
                    modifier = Modifier.fillMaxWidth().testTag("onboarding_next")
                ) { next() }
            } else {
                PrimaryPillButton(
                    text = if (bn) "সাইন ইন / নিবন্ধন" else "Sign in / Register",
                    modifier = Modifier.fillMaxWidth().testTag("onboarding_sign_in")
                ) { onFinished(true) }
                Spacer(Modifier.height(10.dp))
                GhostPillButton(
                    text = if (bn) "অ্যাকাউন্ট ছাড়াই চালিয়ে যান" else "Continue without an account",
                    modifier = Modifier.fillMaxWidth().testTag("onboarding_skip_sign_in")
                ) { onFinished(false) }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ---------------------------------------------------------------- pages

@Composable
private fun PageTitle(title: String, subtitle: String) {
    Text(title, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 38.sp), color = AppColors.TextPrimary)
    Spacer(Modifier.height(6.dp))
    Text(subtitle, style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 22.sp), color = AppColors.TextSecondary)
}

@Composable
private fun WelcomePage(bn: Boolean) {
    val pulse by rememberInfiniteTransition(label = "logo").animateFloat(0.96f, 1.04f, infiniteRepeatable(tween(1600), RepeatMode.Reverse), label = "s")
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        AdDeenLogo(size = 76.dp, modifier = Modifier.padding(bottom = 14.dp).scale(pulse))
        Text(
            "Ad-Deen",
            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold, fontSize = 40.sp),
            color = AppColors.TextPrimary
        )
        Text(
            if (bn) "আপনার প্রতিদিনের ইবাদতের সঙ্গী" else "Your daily worship companion",
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
            color = AppColors.TextSecondary
        )
        Spacer(Modifier.height(22.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GlassTile(Icons.Outlined.Checklist, if (bn) "ব্যক্তিগত দৈনিক আমল" else "Personalized daily checklist", Modifier.weight(1f))
            GlassTile(Icons.Outlined.Alarm, if (bn) "ওয়াক্তের অ্যালার্ম ও স্মরণিকা" else "Prayer alarms & reminders", Modifier.weight(1f))
        }
    }
}

@Composable
private fun FeaturesPage(bn: Boolean) {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PageTitle(if (bn) "যা যা পাবেন" else "Everything in one place", if (bn) "নির্ভুল সময়, নিয়মিত আমল, প্রশান্ত মন" else "Accurate times, steady habits, a calm heart")
        Spacer(Modifier.height(6.dp))
        GlassRow(Icons.Outlined.Schedule, if (bn) "নির্ভুল নামাজের সময়" else "Accurate prayer times", if (bn) "৬৪ জেলা ও জিপিএস, নিষিদ্ধ সময়সহ" else "64 districts or GPS, with forbidden times")
        GlassRow(Icons.Outlined.NotificationsActive, if (bn) "প্রতি ওয়াক্তের আলাদা অ্যালার্ম" else "Alarms for every waqt", if (bn) "নিজস্ব সাউন্ড, ১৫ মিনিট আগে সতর্কবার্তা" else "Own sound per waqt, 15-min end warning")
        GlassRow(Icons.Outlined.LocalFireDepartment, if (bn) "আমল ট্র্যাকার ও স্ট্রিক" else "Amal tracker & streaks", if (bn) "১০০ পয়েন্ট, পারফেক্ট ডে ও মাইলস্টোন" else "100-point score, Perfect Days, milestones")
        GlassRow(Icons.Outlined.MenuBook, if (bn) "আযকার ও তাসবীহ" else "Azkhar & tasbih", if (bn) "সকাল-সন্ধ্যা, ঘুম ও দৈনিক যিকির" else "Morning, evening, sleep & daily zikr")
    }
}

@Composable
private fun LanguagePage(current: String, onPick: (String) -> Unit) {
    val bn = current == "bn"
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PageTitle(if (bn) "ভাষা নির্বাচন করুন" else "Choose your language", if (bn) "পরে সেটিংস থেকে পরিবর্তন করা যাবে" else "You can change this later in Settings")
        Spacer(Modifier.height(6.dp))
        ChoiceCard("English", "Prayer names, reminders and menus in English", current == "en", Modifier.testTag("lang_en")) { onPick("en") }
        ChoiceCard("বাংলা", "নামাজ, স্মরণিকা ও মেনু বাংলায়", current == "bn", Modifier.testTag("lang_bn")) { onPick("bn") }
    }
}

@Composable
private fun LocationPage(viewModel: MainViewModel, bn: Boolean, district: String) {
    val context = LocalContext.current
    var showDistricts by remember { mutableStateOf(false) }
    var gpsState by remember { mutableStateOf<String?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
        if (res.values.any { it }) {
            gpsState = if (bn) "অবস্থান খোঁজা হচ্ছে…" else "Finding your location…"
            viewModel.requestGpsLocation { ok -> gpsState = if (ok) null else (if (bn) "অবস্থান পাওয়া যায়নি — জেলা বেছে নিন" else "Couldn't get a location — choose a district") }
        } else {
            gpsState = if (bn) "অনুমতি দেওয়া হয়নি — জেলা বেছে নিন" else "Permission denied — choose a district instead"
        }
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PageTitle(if (bn) "আপনার অবস্থান" else "Your location", if (bn) "সঠিক নামাজের সময়ের জন্য প্রয়োজন" else "Needed for accurate prayer times and alarms")
        Spacer(Modifier.height(6.dp))
        ChoiceCard(
            if (bn) "জিপিএস ব্যবহার করুন" else "Use my location (GPS)",
            if (bn) "স্বয়ংক্রিয়ভাবে নিকটতম জেলা" else "Detects automatically",
            selected = district.endsWith("(GPS)"),
            modifier = Modifier.testTag("onboarding_gps")
        ) { launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) }
        ChoiceCard(
            if (bn) "জেলা বেছে নিন" else "Choose a district",
            if (bn) "বাংলাদেশের ৬৪ জেলা" else "All 64 districts of Bangladesh",
            selected = !district.endsWith("(GPS)"),
            modifier = Modifier.testTag("onboarding_district")
        ) { showDistricts = true }
        StatusLine(Icons.Outlined.Place, (if (bn) "নির্বাচিত: " else "Selected: ") + district)
        gpsState?.let { StatusLine(Icons.Outlined.Info, it) }
    }
    if (showDistricts) {
        com.example.ui.components.DistrictSelectionDialog(
            districts = viewModel.districts,
            currentDistrict = district,
            lang = if (bn) "bn" else "en",
            onDismiss = { showDistricts = false },
            onSelect = { viewModel.selectDistrict(it); showDistricts = false },
            onGpsSelect = {
                showDistricts = false
                launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            }
        )
    }
}

private fun notificationsGranted(context: Context) =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

private fun exactAlarmsGranted(context: Context) =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()

@Composable
private fun AlarmsPage(bn: Boolean) {
    val context = LocalContext.current
    var notif by remember { mutableStateOf(notificationsGranted(context)) }
    var exact by remember { mutableStateOf(exactAlarmsGranted(context)) }
    // Re-check when returning from the system settings screen.
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) { notif = notificationsGranted(context); exact = exactAlarmsGranted(context) } }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { notif = it }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PageTitle(
            if (bn) "অ্যালার্ম ও নোটিফিকেশন" else "Alarms & notifications",
            if (bn) "ওয়াক্ত শুরু, শেষের ১৫ মিনিট আগে এবং ইস্তিগফার/দরূদ স্মরণিকার জন্য অনুমতি দরকার। আপনি প্রতিটি অ্যালার্ম আলাদাভাবে বন্ধ করতে পারবেন।"
            else "Ad-Deen alerts you when each waqt starts, 15 minutes before it ends, and for Istigfar/Durud reminders. Every alarm can be turned off individually."
        )
        Spacer(Modifier.height(6.dp))
        ChoiceCard(
            if (bn) "নোটিফিকেশন চালু করুন" else "Allow notifications",
            if (notif) (if (bn) "চালু আছে" else "Allowed") else (if (bn) "ট্যাপ করে অনুমতি দিন" else "Tap to allow"),
            selected = notif,
            modifier = Modifier.testTag("onboarding_notifications")
        ) {
            if (!notif && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ChoiceCard(
                if (bn) "সঠিক সময়ে অ্যালার্ম" else "Exact-time alarms",
                if (exact) (if (bn) "চালু আছে" else "Allowed") else (if (bn) "ট্যাপ করে সেটিংসে চালু করুন" else "Tap to allow in settings"),
                selected = exact
            ) {
                if (!exact) runCatching {
                    context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")))
                }
            }
        }
    }
}

@Composable
private fun SignInPage(bn: Boolean) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PageTitle(
            if (bn) "ক্লাউডে সংরক্ষণ (ঐচ্ছিক)" else "Keep your progress safe",
            if (bn) "সাইন ইন করলে আপনার আমল, স্ট্রিক ও সেটিংস সব ডিভাইসে সিঙ্ক থাকবে। চাইলে পরেও করতে পারবেন।"
            else "Sign in to sync your amal, streaks and settings across devices. Optional — you can do it later from More."
        )
        Spacer(Modifier.height(6.dp))
        GlassRow(Icons.Outlined.CloudSync, if (bn) "রিয়েল-টাইম সিঙ্ক" else "Real-time sync", if (bn) "অফলাইনেও কাজ করে" else "Works offline, syncs when online")
        GlassRow(Icons.Outlined.Lock, if (bn) "শুধু আপনার ডাটা" else "Private to you", if (bn) "আপনার অ্যাকাউন্ট ছাড়া কেউ দেখতে পারবে না" else "Only your account can read it")
    }
}

// ---------------------------------------------------------------- pieces

@Composable
private fun GlassTile(icon: ImageVector, text: String, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(20.dp)).background(AppColors.Surface).padding(16.dp)
    ) {
        Icon(icon, null, tint = AppColors.Secondary, modifier = Modifier.size(26.dp))
        Spacer(Modifier.height(10.dp))
        Text(text, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold, lineHeight = 19.sp), color = AppColors.TextPrimary, maxLines = 3, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun GlassRow(icon: ImageVector, title: String, desc: String) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(AppColors.Surface).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(AppColors.WarmTintContainer), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = AppColors.Secondary, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = AppColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(desc, style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp), color = AppColors.TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ChoiceCard(title: String, desc: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    Row(
        modifier.fillMaxWidth().pressScale(interaction).clip(RoundedCornerShape(18.dp))
            .background(if (selected) AppColors.Primary.copy(alpha = 0.2f) else AppColors.Surface)
            .border(1.2.dp, if (selected) AppColors.Primary else Color.Transparent, RoundedCornerShape(18.dp))
            .clickable(interactionSource = interaction, indication = androidx.compose.material3.ripple(), onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = AppColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(10.dp))
        if (selected) Icon(Icons.Filled.CheckCircle, null, tint = AppColors.Secondary, modifier = Modifier.size(26.dp))
        else Box(Modifier.size(24.dp).border(2.dp, AppColors.BorderMedium, CircleShape))
    }
}

@Composable
private fun StatusLine(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = AppColors.Secondary, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Start)
    }
}
