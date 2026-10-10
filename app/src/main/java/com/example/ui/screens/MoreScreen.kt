package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.AppUser
import com.example.data.BackupManager
import com.example.model.CalculationMethod
import com.example.model.District
import com.example.model.IstigfarDurudConfig
import com.example.ui.MainViewModel
import com.example.ui.Strings
import com.example.ui.components.DistrictSelectionDialog
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    viewModel: MainViewModel,
    initialSubScreen: String? = null
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val currentUser by viewModel.currentUserAccount.collectAsState()
    val lang = settings.language

    var subScreen by remember { mutableStateOf(initialSubScreen) } // null, "reminders", "quran", "settings", "auth", "backup"

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showBatteryRationaleDialog by remember { mutableStateOf(false) }
    var deleteAccountLoading by remember { mutableStateOf(false) }
    var authErrorMsg by remember { mutableStateOf<String?>(null) }

    when (subScreen) {
        "istigfar_reminders" -> {
            DedicatedReminderSubScreen(
                title = Strings.t("istigfar_reminders", lang),
                isDurud = false,
                viewModel = viewModel,
                lang = lang,
                onBack = { subScreen = null }
            )
        }
        "durud_reminders" -> {
            DedicatedReminderSubScreen(
                title = Strings.t("durud_reminders", lang),
                isDurud = true,
                viewModel = viewModel,
                lang = lang,
                onBack = { subScreen = null }
            )
        }
        "reminders" -> {
            DedicatedReminderSubScreen(
                title = Strings.t("istigfar_reminders", lang),
                isDurud = false,
                viewModel = viewModel,
                lang = lang,
                onBack = { subScreen = null }
            )
        }
        "custom_sounds" -> {
            CustomSoundsAndLogoSubScreen(
                viewModel = viewModel,
                lang = lang,
                onBack = { subScreen = null }
            )
        }
        "quran" -> {
            QuranBrowserSubScreen(
                lang = lang,
                onBack = { subScreen = null }
            )
        }
        "settings" -> {
            SettingsSubScreen(
                viewModel = viewModel,
                lang = lang,
                onBack = { subScreen = null },
                onShowBatteryPrompt = { showBatteryRationaleDialog = true }
            )
        }
        "auth" -> {
            AuthSubScreen(
                viewModel = viewModel,
                currentUser = currentUser,
                lang = lang,
                onBack = { subScreen = null },
                onNavigateToBackup = { subScreen = "backup" }
            )
        }
        "backup" -> {
            CloudSyncBackupSubScreen(
                viewModel = viewModel,
                currentUser = currentUser,
                lang = lang,
                onBack = { subScreen = null },
                onNavigateToAuth = { subScreen = "auth" }
            )
        }
        else -> {
            // Main More hub: grouped settings rows (notification-sheet reference)
            val iv = { c: com.example.model.IstigfarDurudConfig ->
                if (!c.enabled) (if (lang == "bn") "বন্ধ" else "Off")
                else "${com.example.alarm.AlarmScheduler.intervalMinutes(c)} ${if (lang == "bn") "মিনিট পর পর" else "min interval"}"
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 24.dp + com.example.ui.components.LocalBottomBarSpace.current),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                item { com.example.ui.components.ScreenTitle(Strings.t("nav_more", lang)) }

                item {
                    val effectiveUserName = currentUser?.effectiveName ?: ""
                    com.example.ui.components.GlowCard(
                        modifier = Modifier.fillMaxWidth().testTag("account_card"),
                        onClick = { subScreen = "auth" }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (currentUser != null) {
                                com.example.ui.components.UserAvatar(effectiveUserName, size = 48.dp)
                            } else {
                                com.example.ui.components.AdDeenLogo(size = 48.dp)
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = if (currentUser != null) effectiveUserName else Strings.t("guest_mode", lang),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = com.example.ui.theme.AppColors.TextPrimary,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    modifier = Modifier.testTag("authenticated_user_name_tile")
                                )
                                Text(
                                    text = currentUser?.email ?: (if (lang == "bn") "ব্যাকআপ ও সিঙ্ক করতে সাইন ইন করুন" else "Sign in to back up & sync across devices"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = com.example.ui.theme.AppColors.TextSecondary,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                            Icon(Icons.Default.ChevronRight, null, tint = com.example.ui.theme.AppColors.TextSecondary)
                        }
                    }
                }

                item {
                    com.example.ui.components.SettingsGroup(title = if (lang == "bn") "স্মরণিকা" else "Reminders") {
                        com.example.ui.components.SettingRow(
                            title = Strings.t("istigfar_reminders", lang),
                            subtitle = iv(settings.istigfar),
                            icon = Icons.Outlined.Timer,
                            onClick = { subScreen = "istigfar_reminders" },
                            modifier = Modifier.testTag("istigfar_reminders_tile")
                        ) {
                            com.example.ui.components.SoftSwitch(settings.istigfar.enabled, { viewModel.updateIstigfarConfig(settings.istigfar.copy(enabled = it)) })
                        }
                        com.example.ui.components.SettingRow(
                            title = Strings.t("durud_reminders", lang),
                            subtitle = iv(settings.durud),
                            icon = Icons.Outlined.FavoriteBorder,
                            onClick = { subScreen = "durud_reminders" },
                            modifier = Modifier.testTag("durud_reminders_tile")
                        ) {
                            com.example.ui.components.SoftSwitch(settings.durud.enabled, { viewModel.updateDurudConfig(settings.durud.copy(enabled = it)) })
                        }
                        com.example.ui.components.SettingSwitchRow(
                            title = if (lang == "bn") "সকাল-সন্ধ্যার আযকার স্মরণিকা" else "Morning & evening azkhar",
                            checked = settings.morningReminder && settings.eveningReminder,
                            onCheckedChange = { viewModel.updateReminders(it, it) },
                            icon = Icons.Outlined.WbSunny
                        )
                        com.example.ui.components.SettingValueRow(
                            title = if (lang == "bn") "কাস্টম সাউন্ড" else "Custom sounds",
                            value = null,
                            icon = Icons.Outlined.GraphicEq,
                            onClick = { subScreen = "custom_sounds" },
                            modifier = Modifier.testTag("custom_sounds_tile")
                        )
                    }
                }

                item {
                    com.example.ui.components.SettingsGroup(title = if (lang == "bn") "অ্যাপ" else "App") {
                        com.example.ui.components.SettingValueRow(
                            title = Strings.t("settings", lang),
                            value = if (lang == "bn") "বাংলা" else "English",
                            subtitle = if (lang == "bn") "ভাষা, হিসাব পদ্ধতি, অবস্থান" else "Language, calculation method, location",
                            icon = Icons.Outlined.Settings,
                            onClick = { subScreen = "settings" },
                            modifier = Modifier.testTag("settings_tile")
                        )
                        com.example.ui.components.SettingValueRow(
                            title = Strings.t("quran_browser", lang),
                            value = null,
                            icon = Icons.Outlined.MenuBook,
                            onClick = { subScreen = "quran" },
                            modifier = Modifier.testTag("quran_tile")
                        )
                        if (viewModel.selfUpdateEnabled) {
                            com.example.ui.components.SettingValueRow(
                                title = if (lang == "bn") "আপডেট খুঁজুন" else "Check for updates",
                                value = "v${com.example.update.UpdateManager.currentVersionName}",
                                icon = Icons.Outlined.SystemUpdate,
                                onClick = { viewModel.checkForUpdates(manual = true) },
                                modifier = Modifier.testTag("check_updates_tile")
                            )
                        }
                        com.example.ui.components.SettingValueRow(
                            title = Strings.t("battery_optimization", lang),
                            value = null,
                            subtitle = if (lang == "bn") "অ্যালার্ম যেন সময়মতো বাজে" else "Keep alarms on time in the background",
                            icon = Icons.Outlined.BatteryChargingFull,
                            onClick = { showBatteryRationaleDialog = true },
                            modifier = Modifier.testTag("battery_opt_tile")
                        )
                    }
                }

                item {
                    com.example.ui.components.SettingsGroup(title = if (lang == "bn") "ডাটা" else "Data") {
                        com.example.ui.components.SettingValueRow(
                            title = Strings.t("cloud_sync", lang),
                            value = null,
                            subtitle = if (lang == "bn") "ক্লাউড সিঙ্ক ও ব্যাকআপ ফাইল" else "Cloud sync & backup file",
                            icon = Icons.Outlined.CloudSync,
                            onClick = { subScreen = "backup" },
                            modifier = Modifier.testTag("cloud_sync_tile")
                        )
                        if (currentUser != null) {
                            com.example.ui.components.SettingValueRow(
                                title = Strings.t("delete_account", lang),
                                value = null,
                                icon = Icons.Default.DeleteOutline,
                                titleColor = MaterialTheme.colorScheme.error,
                                onClick = { showDeleteConfirmDialog = true },
                                modifier = Modifier.testTag("delete_account_tile")
                            )
                        }
                    }
                }
            }
        }
    }

    // Battery Optimization Rationale Dialog
    if (showBatteryRationaleDialog) {
        AlertDialog(
            onDismissRequest = { showBatteryRationaleDialog = false },
            title = { Text(Strings.t("battery_optimization", lang)) },
            text = {
                Text(Strings.t("battery_opt_desc", lang))
            },
            confirmButton = {
                Button(
                    onClick = {
                        showBatteryRationaleDialog = false
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(intent)
                            }
                        } catch (_: Exception) {}
                    }
                ) {
                    Text(if (lang == "bn") "অনুমতি দিন" else "Allow")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatteryRationaleDialog = false }) {
                    Text(Strings.t("cancel", lang))
                }
            }
        )
    }

    // Account Deletion Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { if (!deleteAccountLoading) showDeleteConfirmDialog = false },
            title = { Text(Strings.t("delete_account", lang)) },
            text = {
                if (deleteAccountLoading) {
                    Box(modifier = Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    Text(Strings.t("delete_account_confirm", lang))
                }
            },
            confirmButton = {
                if (!deleteAccountLoading) {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        onClick = {
                            deleteAccountLoading = true
                            viewModel.deleteAccount { success, error ->
                                deleteAccountLoading = false
                                showDeleteConfirmDialog = false
                                if (!success) {
                                    authErrorMsg = error
                                }
                            }
                        }
                    ) {
                        Text(Strings.t("delete", lang))
                    }
                }
            },
            dismissButton = {
                if (!deleteAccountLoading) {
                    TextButton(onClick = { showDeleteConfirmDialog = false }) {
                        Text(Strings.t("cancel", lang))
                    }
                }
            }
        )
    }
}

@Composable
private fun MoreNavCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = com.example.ui.theme.AppColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(com.example.ui.theme.AppColors.WarmTintContainer, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = com.example.ui.theme.AppColors.Secondary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    ),
                    color = com.example.ui.theme.AppColors.TextPrimary
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = com.example.ui.theme.AppColors.TextSecondary
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = com.example.ui.theme.AppColors.Secondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SubScreenTitle(title: String, onBack: () -> Unit) {
    com.example.ui.components.ScreenTitle(title, leading = {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = com.example.ui.theme.AppColors.TextPrimary)
        }
    })
}

/** Text field in the soft style: cream fill, rounded, brand-coloured focus. */
@Composable
private fun SoftTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    keyboardType: androidx.compose.ui.text.input.KeyboardType = androidx.compose.ui.text.input.KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        visualTransformation = if (isPassword) androidx.compose.ui.text.input.PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = com.example.ui.theme.AppColors.Action,
            unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
            focusedContainerColor = com.example.ui.theme.AppColors.WarmTintContainer,
            unfocusedContainerColor = com.example.ui.theme.AppColors.WarmTintContainer,
            focusedLabelColor = com.example.ui.theme.AppColors.Action,
            cursorColor = com.example.ui.theme.AppColors.Action
        ),
        modifier = modifier.fillMaxWidth()
    )
}

// ----------------- SUB-SCREEN: CLOUD SYNC & BACKUP -----------------

@Composable
private fun CloudSyncBackupSubScreen(
    viewModel: MainViewModel,
    currentUser: AppUser?,
    lang: String,
    onBack: () -> Unit,
    onNavigateToAuth: () -> Unit = {}
) {
    val context = LocalContext.current
    val bn = lang == "bn"
    val isFirebaseInitialized by viewModel.isFirebaseInitialized.collectAsState()
    val lastSyncTime by viewModel.lastSyncTime.collectAsState()
    val syncError by viewModel.syncError.collectAsState()

    var isSyncing by remember { mutableStateOf(false) }
    var syncMessage by remember { mutableStateOf<String?>(null) }

    val createDocLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri != null) {
            viewModel.writeBackupToUri(uri) { success, err ->
                Toast.makeText(context, if (success) (if (bn) "ব্যাকআপ সফলভাবে সেভ হয়েছে" else "Backup saved") else (err ?: "Error saving backup"), Toast.LENGTH_LONG).show()
            }
        }
    }
    val openDocLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            viewModel.restoreBackupFromUri(uri) { success, daysCount, err ->
                Toast.makeText(
                    context,
                    if (success) (if (bn) "$daysCount দিনের আমল রিস্টোর হয়েছে" else "Restored $daysCount days of records") else (err ?: "Error restoring backup"),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    val lastSyncStr = if (lastSyncTime > 0) SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date(lastSyncTime)) else Strings.t("never_synced", lang)

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 24.dp + com.example.ui.components.LocalBottomBarSpace.current),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item { SubScreenTitle(Strings.t("cloud_sync", lang), onBack) }

        // Status card
        item {
            com.example.ui.components.GlowCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    com.example.ui.components.SoftTile(Modifier.size(46.dp), RoundedCornerShape(14.dp)) {
                        Icon(
                            if (currentUser != null && isFirebaseInitialized) Icons.Default.CloudDone else Icons.Default.CloudQueue,
                            null,
                            tint = com.example.ui.theme.AppColors.Secondary,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (currentUser != null) (if (bn) "ক্লাউড সিঙ্ক চালু" else "Cloud sync is on") else (if (bn) "শুধু এই ফোনে সংরক্ষিত" else "Saved on this phone only"),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = com.example.ui.theme.AppColors.TextPrimary
                        )
                        Text("${Strings.t("last_synced", lang)} $lastSyncStr", style = MaterialTheme.typography.bodySmall, color = com.example.ui.theme.AppColors.TextSecondary)
                    }
                }
                if (currentUser != null) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(com.example.ui.theme.AppColors.WarmTintContainer)
                            .padding(10.dp).testTag("backup_user_status_card"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        com.example.ui.components.UserAvatar(currentUser.effectiveName, size = 34.dp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(currentUser.effectiveName, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = com.example.ui.theme.AppColors.TextPrimary, maxLines = 1, modifier = Modifier.testTag("backup_user_name_text"))
                            Text(currentUser.email, style = MaterialTheme.typography.bodySmall, color = com.example.ui.theme.AppColors.TextSecondary, maxLines = 1)
                        }
                    }
                }
                (syncMessage ?: syncError)?.let {
                    Spacer(Modifier.height(10.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall, color = if (syncMessage != null) com.example.ui.theme.AppColors.Secondary else MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.height(14.dp))
                if (currentUser != null) {
                    com.example.ui.components.PrimaryPillButton(
                        text = if (isSyncing) (if (bn) "সিঙ্ক হচ্ছে…" else "Syncing…") else Strings.t("sync_now", lang),
                        enabled = !isSyncing,
                        modifier = Modifier.fillMaxWidth().testTag("sync_now_button")
                    ) {
                        isSyncing = true
                        syncMessage = null
                        viewModel.syncAllToCloud { success, count, err ->
                            isSyncing = false
                            syncMessage = if (success) (if (bn) "$count দিনের রেকর্ড সিঙ্ক হয়েছে" else "Synced $count days of records") else (err ?: "Sync failed")
                        }
                    }
                } else {
                    Text(
                        if (bn) "সব ডিভাইসে আমল সিঙ্ক করতে সাইন ইন করুন।" else "Sign in to keep your amal synced across devices.",
                        style = MaterialTheme.typography.bodySmall,
                        color = com.example.ui.theme.AppColors.TextSecondary
                    )
                    Spacer(Modifier.height(10.dp))
                    com.example.ui.components.PrimaryPillButton(
                        text = Strings.t("sign_in", lang),
                        modifier = Modifier.fillMaxWidth().testTag("backup_sign_in_button"),
                        onClick = onNavigateToAuth
                    )
                }
            }
        }

        item {
            com.example.ui.components.SettingsGroup(title = if (bn) "ব্যাকআপ ফাইল" else "Backup file") {
                com.example.ui.components.SettingValueRow(
                    title = Strings.t("share_backup", lang),
                    value = null,
                    subtitle = if (bn) "গুগল ড্রাইভ বা অন্য অ্যাপে শেয়ার করুন" else "Save to Google Drive or another app",
                    icon = Icons.Default.Share,
                    onClick = {
                        viewModel.exportBackupFile { file ->
                            if (file != null) {
                                context.startActivity(Intent.createChooser(BackupManager.createShareIntent(context, file), "Save backup"))
                            } else {
                                Toast.makeText(context, "Could not generate backup", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
                com.example.ui.components.SettingValueRow(
                    title = Strings.t("save_file", lang),
                    value = null,
                    subtitle = if (bn) "ফোনের স্টোরেজে একটি .json ফাইল" else "A .json file on your phone",
                    icon = Icons.Default.SaveAlt,
                    onClick = { createDocLauncher.launch("AdDeen_Backup_${SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())}.json") }
                )
                com.example.ui.components.SettingValueRow(
                    title = Strings.t("restore_backup", lang),
                    value = null,
                    subtitle = Strings.t("restore_desc", lang),
                    icon = Icons.Default.Restore,
                    onClick = { openDocLauncher.launch(arrayOf("application/json", "*/*")) }
                )
            }
        }
    }
}

// ----------------- SUB-SCREEN: REMINDERS & QUIET HOURS -----------------

@Composable
private fun DedicatedReminderSubScreen(
    title: String,
    isDurud: Boolean,
    viewModel: MainViewModel,
    lang: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val config = if (isDurud) settings.durud else settings.istigfar
    val bn = lang == "bn"
    fun update(c: com.example.model.IstigfarDurudConfig) = if (isDurud) viewModel.updateDurudConfig(c) else viewModel.updateIstigfarConfig(c)

    val modes = listOf(
        "interval_15" to (if (bn) "প্রতি ১৫ মিনিট" else "Every 15 minutes"),
        "interval_30" to (if (bn) "প্রতি ৩০ মিনিট" else "Every 30 minutes"),
        "interval_45" to (if (bn) "প্রতি ৪৫ মিনিট" else "Every 45 minutes"),
        "interval_60" to (if (bn) "প্রতি ১ ঘণ্টা" else "Every hour"),
        "interval_120" to (if (bn) "প্রতি ২ ঘণ্টা" else "Every 2 hours"),
        "interval_180" to (if (bn) "প্রতি ৩ ঘণ্টা" else "Every 3 hours")
    )
    var showModes by remember { mutableStateOf(false) }

    // Quiet hours use the system time picker, so stored values are always valid HH:mm.
    fun pickTime(current: String, onPicked: (String) -> Unit) {
        val parts = runCatching { current.split(":").map { it.trim().toInt() } }.getOrDefault(listOf(23, 0))
        android.app.TimePickerDialog(
            context,
            { _, hh, mm -> onPicked(String.format(java.util.Locale.US, "%02d:%02d", hh, mm)) },
            parts.getOrElse(0) { 23 }, parts.getOrElse(1) { 0 }, true
        ).show()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 24.dp + com.example.ui.components.LocalBottomBarSpace.current),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            com.example.ui.components.ScreenTitle(title, leading = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = com.example.ui.theme.AppColors.TextPrimary)
                }
            })
        }
        item {
            com.example.ui.components.SettingsGroup {
                com.example.ui.components.SettingSwitchRow(
                    title = if (isDurud) (if (bn) "দরূদ স্মরণিকা" else "Durud reminders") else (if (bn) "ইস্তিগফার স্মরণিকা" else "Istigfar reminders"),
                    checked = config.enabled,
                    onCheckedChange = { update(config.copy(enabled = it)) }
                )
                com.example.ui.components.SettingValueRow(
                    title = if (bn) "পুনরাবৃত্তি" else "Repeat",
                    value = modes.firstOrNull { it.first == config.mode }?.second ?: modes[3].second,
                    onClick = { if (config.enabled) showModes = true }
                )
            }
        }
        item {
            com.example.ui.components.SettingsGroup(title = Strings.t("quiet_hours", lang)) {
                com.example.ui.components.SettingValueRow(
                    title = if (bn) "শুরু" else "From",
                    value = settings.quietHoursStart,
                    onClick = { pickTime(settings.quietHoursStart) { viewModel.updateQuietHours(it, settings.quietHoursEnd) } }
                )
                com.example.ui.components.SettingValueRow(
                    title = if (bn) "শেষ" else "Until",
                    value = settings.quietHoursEnd,
                    onClick = { pickTime(settings.quietHoursEnd) { viewModel.updateQuietHours(settings.quietHoursStart, it) } }
                )
                Text(
                    Strings.t("quiet_hours_note", lang),
                    style = MaterialTheme.typography.bodySmall,
                    color = com.example.ui.theme.AppColors.TextSecondary,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )
            }
        }
        item {
            com.example.ui.components.GlowCard(modifier = Modifier.fillMaxWidth(), color = com.example.ui.theme.AppColors.WarmTintContainer, elevation = 0.dp) {
                Text(
                    text = if (isDurud) "اللَّهُمَّ صَلِّ عَلَى مُحَمَّدٍ" else "أَسْتَغْفِرُ اللَّهَ وَأَتُوبُ إِلَيْهِ",
                    style = MaterialTheme.typography.titleLarge,
                    color = com.example.ui.theme.AppColors.Secondary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (isDurud) (if (bn) "হে আল্লাহ! মুহাম্মদ (সা.)-এর উপর রহমত বর্ষণ করুন।" else "O Allah, send blessings upon Muhammad.")
                    else (if (bn) "আমি আল্লাহর নিকট ক্ষমা প্রার্থনা করছি এবং তাঁরই দিকে তওবা করছি।" else "I seek Allah's forgiveness and turn to Him in repentance."),
                    style = MaterialTheme.typography.bodyMedium,
                    color = com.example.ui.theme.AppColors.TextPrimary
                )
                Spacer(Modifier.height(14.dp))
                com.example.ui.components.PrimaryPillButton(
                    text = if (bn) "টেস্ট নোটিফিকেশন পাঠান" else "Send test notification",
                    modifier = Modifier.fillMaxWidth()
                ) {
                    viewModel.sendTestReminder(isIstigfar = !isDurud)
                    Toast.makeText(context, if (bn) "টেস্ট নোটিফিকেশন পাঠানো হয়েছে" else "Test notification sent", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    if (showModes) {
        com.example.ui.components.SoftSheet(title = if (bn) "পুনরাবৃত্তি" else "Repeat", onDismiss = { showModes = false }) {
            modes.forEach { (key, label) ->
                com.example.ui.components.SettingRow(title = label, onClick = {
                    update(config.copy(mode = key))
                    showModes = false
                }) {
                    RadioButton(selected = config.mode == key, onClick = null)
                }
            }
        }
    }
}

// ----------------- SUB-SCREEN: QURAN WEBVIEW BROWSER -----------------

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun QuranBrowserSubScreen(
    lang: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val bn = lang == "bn"
    val uiPrefs = remember { com.example.data.UiPrefs(context) }
    var webView: WebView? by remember { mutableStateOf(null) }
    var currentUrl by remember { mutableStateOf("https://quran.com") }
    var customSites by remember { mutableStateOf(uiPrefs.quranSites) }
    var showAddSite by remember { mutableStateOf(false) }
    var showReminder by remember { mutableStateOf(true) }

    val builtIn = listOf(
        com.example.data.QuranSite("Quran.com", "https://quran.com"),
        com.example.data.QuranSite("Quran.gov.bd", "https://quran.gov.bd"),
        com.example.data.QuranSite("QuraanShareef", "https://quraanshareef.org")
    )
    fun open(url: String) {
        currentUrl = url
        webView?.loadUrl(url)
    }

    Column(modifier = Modifier.fillMaxSize().padding(bottom = com.example.ui.components.LocalBottomBarSpace.current)) {
        // Browser Controls Bar
        Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 3.dp) {
            Column(modifier = Modifier.padding(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) { Icon(Icons.Default.Close, contentDescription = "Close") }
                        IconButton(onClick = { if (webView?.canGoBack() == true) webView?.goBack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                        IconButton(onClick = { if (webView?.canGoForward() == true) webView?.goForward() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Forward")
                        }
                        IconButton(onClick = { webView?.reload() }) { Icon(Icons.Default.Refresh, contentDescription = "Reload") }
                    }
                    Text(
                        text = Strings.t("quran_browser", lang),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Quick switch: built-in sites, user-added sites (removable), and "+ Add site"
                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(builtIn.size) { i ->
                        val site = builtIn[i]
                        FilterChip(selected = currentUrl == site.url, onClick = { open(site.url) }, label = { Text(site.name) })
                    }
                    items(customSites.size) { i ->
                        val site = customSites[i]
                        FilterChip(
                            selected = currentUrl == site.url,
                            onClick = { open(site.url) },
                            label = { Text(site.name, maxLines = 1) },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = if (bn) "মুছুন" else "Remove",
                                    modifier = Modifier.size(16.dp).clickable {
                                        customSites = customSites - site
                                        uiPrefs.quranSites = customSites
                                    }
                                )
                            }
                        )
                    }
                    item {
                        AssistChip(
                            onClick = { showAddSite = true },
                            label = { Text(if (bn) "সাইট যোগ করুন" else "Add site") },
                            leadingIcon = { Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.testTag("quran_add_site")
                        )
                    }
                }
            }
        }

        // Gentle reminder: the physical Mushaf comes first.
        if (showReminder) {
            Row(
                Modifier.fillMaxWidth().background(com.example.ui.theme.AppColors.WarmTintContainer).padding(start = 14.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.MenuBook, null, tint = com.example.ui.theme.AppColors.Secondary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    if (bn) "সম্ভব হলে মুসহাফ (ছাপা কুরআন) হাতে নিয়ে তিলাওয়াত করুন — ফোন শুধু সহায়ক হিসেবে ব্যবহার করুন।"
                    else "Whenever you can, read from a physical Mushaf. Use the screen only as a helper.",
                    style = MaterialTheme.typography.bodySmall,
                    color = com.example.ui.theme.AppColors.TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { showReminder = false }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = com.example.ui.theme.AppColors.TextSecondary, modifier = Modifier.size(16.dp))
                }
            }
        }

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    webViewClient = WebViewClient()
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    loadUrl(currentUrl)
                    webView = this
                }
            },
            update = { view -> webView = view }
        )
    }

    if (showAddSite) {
        var name by remember { mutableStateOf("") }
        var url by remember { mutableStateOf("") }
        var error by remember { mutableStateOf<String?>(null) }
        com.example.ui.components.SoftSheet(
            title = if (bn) "নতুন ওয়েবসাইট" else "Add a website",
            onDismiss = { showAddSite = false },
            cancelText = Strings.t("cancel", lang),
            confirmText = Strings.t("save", lang),
            onConfirm = {
                val clean = com.example.data.UiPrefs.normaliseUrl(url)
                when {
                    clean == null -> error = if (bn) "সঠিক ওয়েব ঠিকানা লিখুন (যেমন quran.example.com)" else "Enter a valid web address (e.g. quran.example.com)"
                    customSites.any { it.url == clean } || builtIn.any { it.url == clean } -> error = if (bn) "এই সাইট আগেই আছে" else "This site is already in the list"
                    else -> {
                        val label = name.trim().ifBlank { android.net.Uri.parse(clean).host.orEmpty().removePrefix("www.") }
                        customSites = customSites + com.example.data.QuranSite(label.take(24), clean)
                        uiPrefs.quranSites = customSites
                        showAddSite = false
                        open(clean)
                    }
                }
            }
        ) {
            Text(
                if (bn) "আপনার পছন্দের কুরআন ওয়েবসাইট যোগ করুন। তবুও আমরা মুসহাফ হাতে নিয়ে পড়তে উৎসাহিত করি।"
                else "Add a Quran website you trust. We still encourage reading from a physical Mushaf whenever possible.",
                style = MaterialTheme.typography.bodySmall,
                color = com.example.ui.theme.AppColors.TextSecondary
            )
            SoftTextField(name, { name = it }, if (bn) "নাম (ঐচ্ছিক)" else "Name (optional)")
            SoftTextField(url, { url = it; error = null }, if (bn) "ওয়েব ঠিকানা" else "Web address", keyboardType = androidx.compose.ui.text.input.KeyboardType.Uri)
            error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
        }
    }
}

// ----------------- SUB-SCREEN: SETTINGS -----------------

@Composable
private fun SettingsSubScreen(
    viewModel: MainViewModel,
    lang: String,
    onBack: () -> Unit,
    onShowBatteryPrompt: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    val bn = lang == "bn"
    var showDistrictDialog by remember { mutableStateOf(false) }
    var showMethodSheet by remember { mutableStateOf(false) }
    var gpsBusy by remember { mutableStateOf(false) }
    val method = CalculationMethod.fromId(settings.calcMethod)

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 24.dp + com.example.ui.components.LocalBottomBarSpace.current),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item { SubScreenTitle(Strings.t("settings", lang), onBack) }

        item {
            com.example.ui.components.SettingsGroup(title = Strings.t("language", lang)) {
                listOf("en" to "English", "bn" to "বাংলা").forEach { (code, label) ->
                    com.example.ui.components.SettingRow(title = label, onClick = { viewModel.updateLanguage(code) }) {
                        RadioButton(selected = settings.language == code, onClick = null)
                    }
                }
            }
        }

        item {
            com.example.ui.components.SettingsGroup(title = if (bn) "নামাজের সময়" else "Prayer times") {
                com.example.ui.components.SettingValueRow(
                    title = Strings.t("location", lang),
                    value = settings.district,
                    subtitle = "${String.format(Locale.US, "%.3f", settings.lat)}, ${String.format(Locale.US, "%.3f", settings.lng)}",
                    icon = Icons.Outlined.Place,
                    onClick = { showDistrictDialog = true }
                )
                com.example.ui.components.SettingRow(
                    title = Strings.t("gps_mode", lang),
                    subtitle = if (gpsBusy) (if (bn) "অবস্থান খোঁজা হচ্ছে…" else "Finding your location…") else (if (bn) "বর্তমান অবস্থান থেকে সময় নির্ধারণ" else "Use your current location"),
                    icon = Icons.Default.MyLocation,
                    onClick = {
                        gpsBusy = true
                        viewModel.requestGpsLocation { gpsBusy = false }
                    }
                )
                com.example.ui.components.SettingValueRow(
                    title = Strings.t("calc_method_label", lang),
                    value = method.id,
                    subtitle = if (bn) method.titleBn else method.titleEn,
                    icon = Icons.Outlined.Calculate,
                    onClick = { showMethodSheet = true }
                )
            }
        }

        item {
            com.example.ui.components.SettingsGroup(title = if (bn) "পটভূমি" else "Background") {
                com.example.ui.components.SettingValueRow(
                    title = Strings.t("battery_optimization", lang),
                    value = null,
                    subtitle = Strings.t("battery_opt_desc", lang),
                    icon = Icons.Outlined.BatteryChargingFull,
                    onClick = onShowBatteryPrompt
                )
            }
        }
    }

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

    if (showMethodSheet) {
        com.example.ui.components.SoftSheet(title = Strings.t("calc_method_label", lang), onDismiss = { showMethodSheet = false }) {
            CalculationMethod.entries.forEach { m ->
                com.example.ui.components.SettingRow(
                    title = if (bn) m.titleBn else m.titleEn,
                    subtitle = "Fajr ${m.fajrAngle}° • Hanafi Asr",
                    onClick = {
                        viewModel.updateCalculationMethod(m.id)
                        showMethodSheet = false
                    }
                ) {
                    RadioButton(selected = settings.calcMethod.equals(m.id, ignoreCase = true), onClick = null)
                }
            }
        }
    }
}

// ----------------- SUB-SCREEN: AUTH & ACCOUNT -----------------

@Composable
private fun AuthSubScreen(
    viewModel: MainViewModel,
    currentUser: AppUser?,
    lang: String,
    onBack: () -> Unit,
    onNavigateToBackup: () -> Unit
) {
    val bn = lang == "bn"
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var isRegisterMode by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var successMsg by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    var showEditName by remember { mutableStateOf(false) }
    var editNameText by remember(currentUser) { mutableStateOf(currentUser?.displayName ?: "") }
    var editNameError by remember { mutableStateOf<String?>(null) }
    var editNameLoading by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 24.dp + com.example.ui.components.LocalBottomBarSpace.current),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item { SubScreenTitle(Strings.t("account", lang), onBack) }

        if (currentUser != null) {
            item {
                com.example.ui.components.GlowCard(modifier = Modifier.fillMaxWidth().testTag("authenticated_user_profile_card")) {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        com.example.ui.components.UserAvatar(currentUser.effectiveName, size = 64.dp)
                        Spacer(Modifier.height(10.dp))
                        Text(
                            currentUser.effectiveName,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = com.example.ui.theme.AppColors.TextPrimary,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("authenticated_user_name_text")
                        )
                        Text(currentUser.email, style = MaterialTheme.typography.bodyMedium, color = com.example.ui.theme.AppColors.TextSecondary, maxLines = 1)
                        Spacer(Modifier.height(10.dp))
                        Row(
                            Modifier.clip(RoundedCornerShape(12.dp)).background(com.example.ui.theme.AppColors.WarmTintContainer).padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CloudDone, null, tint = com.example.ui.theme.AppColors.Secondary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (bn) "ক্লাউড সিঙ্ক চালু" else "Cloud sync on", style = MaterialTheme.typography.labelMedium, color = com.example.ui.theme.AppColors.Secondary)
                        }
                    }
                }
            }
            item {
                com.example.ui.components.SettingsGroup {
                    com.example.ui.components.SettingValueRow(
                        title = if (bn) "নাম পরিবর্তন" else "Edit display name",
                        value = currentUser.displayName.ifBlank { null },
                        icon = Icons.Default.Edit,
                        onClick = {
                            editNameText = currentUser.displayName
                            editNameError = null
                            showEditName = true
                        },
                        modifier = Modifier.testTag("edit_name_button")
                    )
                    com.example.ui.components.SettingValueRow(
                        title = Strings.t("cloud_sync", lang),
                        value = null,
                        icon = Icons.Default.CloudSync,
                        onClick = onNavigateToBackup
                    )
                    com.example.ui.components.SettingValueRow(
                        title = Strings.t("sign_out", lang),
                        value = null,
                        icon = Icons.AutoMirrored.Filled.Logout,
                        onClick = { viewModel.signOut() }
                    )
                }
            }
        } else {
            item {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    com.example.ui.components.AdDeenLogo(size = 64.dp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (isRegisterMode) (if (bn) "নতুন অ্যাকাউন্ট" else "Create your account") else (if (bn) "আবার স্বাগতম" else "Welcome back"),
                        style = MaterialTheme.typography.headlineSmall,
                        color = com.example.ui.theme.AppColors.TextPrimary
                    )
                    Text(
                        if (bn) "সাইন ইন করলে আপনার আমল সব ডিভাইসে সিঙ্ক থাকবে" else "Sign in to keep your amal synced across devices",
                        style = MaterialTheme.typography.bodyMedium,
                        color = com.example.ui.theme.AppColors.TextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
            item {
                com.example.ui.components.GlowCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (isRegisterMode) {
                            SoftTextField(displayName, { displayName = it }, if (bn) "নাম" else "Display name")
                        }
                        SoftTextField(email, { email = it }, if (bn) "ইমেইল" else "Email", keyboardType = androidx.compose.ui.text.input.KeyboardType.Email)
                        SoftTextField(password, { password = it }, if (bn) "পাসওয়ার্ড" else "Password", isPassword = true, keyboardType = androidx.compose.ui.text.input.KeyboardType.Password)
                        errorMsg?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                        successMsg?.let { Text(it, color = com.example.ui.theme.AppColors.Secondary, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)) }
                        Spacer(Modifier.height(4.dp))
                        com.example.ui.components.PrimaryPillButton(
                            text = when {
                                loading -> if (bn) "অপেক্ষা করুন…" else "Please wait…"
                                isRegisterMode -> if (bn) "নিবন্ধন সম্পন্ন করুন" else "Create account"
                                else -> if (bn) "সাইন ইন" else "Sign in"
                            },
                            enabled = !loading && email.isNotBlank() && password.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            loading = true
                            errorMsg = null
                            successMsg = null
                            if (isRegisterMode) {
                                viewModel.registerWithEmail(email, password, displayName) { success, err ->
                                    loading = false
                                    if (success) successMsg = if (bn) "অ্যাকাউন্ট তৈরি হয়েছে, ক্লাউড সিঙ্ক চালু।" else "Account created. Cloud sync is on." else errorMsg = err
                                }
                            } else {
                                viewModel.signInWithEmail(email, password) { success, err ->
                                    loading = false
                                    if (success) successMsg = if (bn) "সাইন ইন সম্পন্ন, আমল সিঙ্ক হয়েছে।" else "Signed in. Your amal is synced." else errorMsg = err
                                }
                            }
                        }
                        TextButton(
                            onClick = {
                                isRegisterMode = !isRegisterMode
                                errorMsg = null
                                successMsg = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (isRegisterMode) (if (bn) "অ্যাকাউন্ট আছে? সাইন ইন করুন" else "Already have an account? Sign in")
                                else (if (bn) "অ্যাকাউন্ট নেই? নতুন অ্যাকাউন্ট তৈরি করুন" else "New here? Create an account"),
                                color = com.example.ui.theme.AppColors.Secondary
                            )
                        }
                    }
                }
            }
        }
    }

    if (showEditName && currentUser != null) {
        com.example.ui.components.SoftSheet(
            title = if (bn) "প্রদর্শিত নাম" else "Display name",
            onDismiss = { showEditName = false },
            cancelText = Strings.t("cancel", lang),
            confirmText = if (editNameLoading) (if (bn) "সংরক্ষণ হচ্ছে…" else "Saving…") else Strings.t("save", lang),
            onConfirm = {
                if (editNameText.isNotBlank() && !editNameLoading) {
                    editNameLoading = true
                    editNameError = null
                    viewModel.updateUserName(editNameText) { success, err ->
                        editNameLoading = false
                        if (success) showEditName = false else editNameError = err
                    }
                }
            }
        ) {
            SoftTextField(editNameText, { editNameText = it }, if (bn) "আপনার নাম" else "Your name", modifier = Modifier.testTag("edit_name_textfield"))
            editNameError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        }
    }
}
