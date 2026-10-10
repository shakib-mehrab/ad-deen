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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
                contentPadding = PaddingValues(bottom = 24.dp),
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

// ----------------- SUB-SCREEN: CLOUD SYNC & GOOGLE DRIVE BACKUP -----------------

@Composable
private fun CloudSyncBackupSubScreen(
    viewModel: MainViewModel,
    currentUser: AppUser?,
    lang: String,
    onBack: () -> Unit,
    onNavigateToAuth: () -> Unit = {}
) {
    val context = LocalContext.current
    val isFirebaseInitialized by viewModel.isFirebaseInitialized.collectAsState()
    val firebaseStatus by viewModel.firebaseStatus.collectAsState()
    val lastSyncTime by viewModel.lastSyncTime.collectAsState()

    var isSyncing by remember { mutableStateOf(false) }
    var syncMessage by remember { mutableStateOf<String?>(null) }

    // SAF Create Document Launcher (allows user to select Google Drive folder or local storage)
    val createDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.writeBackupToUri(uri) { success, err ->
                if (success) {
                    Toast.makeText(context, if (lang == "bn") "ব্যাকআপ সফলভাবে সেভ হয়েছে!" else "Backup saved successfully!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, err ?: "Error saving backup", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // SAF Open Document Launcher (allows user to pick a backup file from Google Drive or local storage)
    val openDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.restoreBackupFromUri(uri) { success, daysCount, err ->
                if (success) {
                    Toast.makeText(
                        context,
                        if (lang == "bn") "$daysCount দিনের আমল সফলভাবে রিস্টোর হয়েছে!" else "Successfully restored $daysCount days of worship records!",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(context, err ?: "Error restoring backup", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val lastSyncStr = if (lastSyncTime > 0) {
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date(lastSyncTime))
    } else {
        Strings.t("never_synced", lang)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.Transparent)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = Strings.t("cloud_sync", lang),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        // Cloud Status Banner
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isFirebaseInitialized) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            imageVector = if (isFirebaseInitialized) Icons.Default.CloudDone else Icons.Default.CloudQueue,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (isFirebaseInitialized) Strings.t("firebase_connected", lang) else Strings.t("local_mode_notice", lang),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (currentUser != null) {
                        val effectiveUserName = currentUser.effectiveName
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth().testTag("backup_user_status_card")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(
                                            Brush.linearGradient(
                                                listOf(
                                                    MaterialTheme.colorScheme.primary,
                                                    MaterialTheme.colorScheme.secondary
                                                )
                                            ),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = effectiveUserName.trim().take(1).uppercase(),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = effectiveUserName,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.testTag("backup_user_name_text")
                                        )
                                    }
                                    Text(
                                        text = currentUser.email,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = "${Strings.t("last_synced", lang)} $lastSyncStr",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (currentUser != null) {
                        Button(
                            onClick = {
                                isSyncing = true
                                syncMessage = null
                                viewModel.syncAllToCloud { success, count, err ->
                                    isSyncing = false
                                    if (success) {
                                        syncMessage = if (lang == "bn") "$count দিনের নামাজের হিস্ট্রি ও আমল ক্লাউডে সিঙ্ক হয়েছে!" else "Successfully synced $count days of prayer history with Firebase Cloud!"
                                    } else {
                                        syncMessage = err ?: "Sync failed"
                                    }
                                }
                            },
                            enabled = !isSyncing,
                            modifier = Modifier.fillMaxWidth().testTag("sync_now_button")
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
                                Spacer(Modifier.width(8.dp))
                            }
                            Icon(Icons.Default.Sync, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(Strings.t("sync_now", lang))
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = if (lang == "bn") "নামাজের হিস্ট্রি ক্লাউডে সিঙ্ক ও ব্যাকআপ করতে সাইন ইন করুন।" else "Sign in with your registered account to backup & sync prayer history across devices.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = onNavigateToAuth,
                                modifier = Modifier.fillMaxWidth().testTag("backup_sign_in_button")
                            ) {
                                Icon(Icons.Default.Login, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(Strings.t("sign_in", lang))
                            }
                        }
                    }

                    if (syncMessage != null) {
                        Text(
                            text = syncMessage ?: "",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Google Drive / Share Backup Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = Strings.t("backup_google_drive", lang),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Text(
                        text = Strings.t("backup_desc", lang),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Option 1: Share sheet (has Google Drive "Save to Drive" directly)
                    Button(
                        onClick = {
                            viewModel.exportBackupFile { file ->
                                if (file != null) {
                                    val shareIntent = BackupManager.createShareIntent(context, file)
                                    context.startActivity(Intent.createChooser(shareIntent, "Save to Google Drive / Share"))
                                } else {
                                    Toast.makeText(context, "Could not generate backup", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(Strings.t("share_backup", lang))
                    }

                    // Option 2: File Picker (SAF - user selects Google Drive folder or Storage)
                    OutlinedButton(
                        onClick = {
                            val defaultName = "AdDin_Backup_${SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())}.json"
                            createDocLauncher.launch(defaultName)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.SaveAlt, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(Strings.t("save_file", lang))
                    }
                }
            }
        }

        // Restore Backup Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Text(
                            text = Strings.t("restore_backup", lang),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Text(
                        text = Strings.t("restore_desc", lang),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = {
                            openDocLauncher.launch(arrayOf("application/json", "*/*"))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(Strings.t("restore_backup", lang))
                    }
                }
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
        contentPadding = PaddingValues(bottom = 24.dp),
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
    var webView: WebView? by remember { mutableStateOf(null) }
    var currentUrl by remember { mutableStateOf("https://quran.com") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.Transparent)
    ) {
        // Browser Controls Bar
        Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 3.dp) {
            Column(modifier = Modifier.padding(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                        IconButton(onClick = { if (webView?.canGoBack() == true) webView?.goBack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                        IconButton(onClick = { if (webView?.canGoForward() == true) webView?.goForward() }) {
                            Icon(Icons.Default.ArrowForward, contentDescription = "Forward")
                        }
                        IconButton(onClick = { webView?.reload() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reload")
                        }
                    }

                    Text(
                        text = Strings.t("quran_browser", lang),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Quick Switch Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = currentUrl.contains("quran.com"),
                        onClick = {
                            currentUrl = "https://quran.com"
                            webView?.loadUrl("https://quran.com")
                        },
                        label = { Text("Quran.com") }
                    )
                    FilterChip(
                        selected = currentUrl.contains("quran.gov.bd"),
                        onClick = {
                            currentUrl = "https://quran.gov.bd"
                            webView?.loadUrl("https://quran.gov.bd")
                        },
                        label = { Text("Quran.gov.bd") }
                    )
                    FilterChip(
                        selected = currentUrl.contains("quraanshareef.org"),
                        onClick = {
                            currentUrl = "https://quraanshareef.org"
                            webView?.loadUrl("https://quraanshareef.org")
                        },
                        label = { Text("QuraanShareef") }
                    )
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
            update = { view ->
                webView = view
            }
        )
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

    var showDistrictDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.Transparent)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = Strings.t("settings", lang),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        // Language Selection
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = Strings.t("language", lang),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilterChip(
                            selected = settings.language == "en",
                            onClick = { viewModel.updateLanguage("en") },
                            label = { Text("English") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = settings.language == "bn",
                            onClick = { viewModel.updateLanguage("bn") },
                            label = { Text("বাংলা (Bangla)") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Calculation Method
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = Strings.t("calc_method_label", lang),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    CalculationMethod.entries.forEach { method ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.updateCalculationMethod(method.id) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = settings.calcMethod.equals(method.id, ignoreCase = true),
                                onClick = { viewModel.updateCalculationMethod(method.id) }
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (lang == "bn") method.titleBn else method.titleEn,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                                )
                                Text(
                                    text = "Fajr: ${method.fajrAngle}° • Asr: Hanafi (Shadow 2)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Location & 64 Districts Selector
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = Strings.t("location_mode", lang),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "${settings.district} (${String.format(Locale.US, "%.3f", settings.lat)}, ${String.format(Locale.US, "%.3f", settings.lng)})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { showDistrictDialog = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(Strings.t("district_mode", lang), style = MaterialTheme.typography.labelSmall)
                        }
                        OutlinedButton(
                            onClick = { viewModel.requestGpsLocation { } },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(Strings.t("gps_mode", lang), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        // Battery Optimization
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = Strings.t("battery_optimization", lang),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = Strings.t("battery_opt_desc", lang),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(onClick = onShowBatteryPrompt, modifier = Modifier.fillMaxWidth()) {
                        Text(if (lang == "bn") "অনুমতি যাচাই করুন" else "Check / Allow Background Execution")
                    }
                }
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
            onGpsSelect = {
                viewModel.requestGpsLocation { success ->
                    if (success) showDistrictDialog = false
                }
            }
        )
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
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var isRegisterMode by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var successMsg by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    var showEditNameDialog by remember { mutableStateOf(false) }
    var editNameText by remember(currentUser) { mutableStateOf(currentUser?.displayName ?: "") }
    var editNameLoading by remember { mutableStateOf(false) }
    var editNameError by remember { mutableStateOf<String?>(null) }

    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text(if (lang == "bn") "প্রদর্শিত নাম পরিবর্তন" else "Update Display Name") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editNameText,
                        onValueChange = { editNameText = it },
                        label = { Text(if (lang == "bn") "আপনার নাম" else "Your Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("edit_name_textfield")
                    )
                    if (editNameError != null) {
                        Text(
                            text = editNameError ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editNameText.isNotBlank()) {
                            editNameLoading = true
                            editNameError = null
                            viewModel.updateUserName(editNameText) { success, err ->
                                editNameLoading = false
                                if (success) {
                                    showEditNameDialog = false
                                } else {
                                    editNameError = err
                                }
                            }
                        }
                    },
                    enabled = editNameText.isNotBlank() && !editNameLoading,
                    modifier = Modifier.testTag("save_name_button")
                ) {
                    Text(if (lang == "bn") "সংরক্ষণ" else "Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text(if (lang == "bn") "বাতিল" else "Cancel")
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.Transparent)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = Strings.t("account", lang),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        if (currentUser != null) {
            val effectiveUserName = currentUser.effectiveName
            // Already Signed In
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().testTag("authenticated_user_profile_card")
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.secondary
                                            )
                                        ),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = effectiveUserName.trim().take(1).uppercase(),
                                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = if (lang == "bn") "লগইন আছেন" else "SIGNED IN AS",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.5.sp,
                                            fontSize = 9.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = effectiveUserName,
                                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.testTag("authenticated_user_name_text")
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = currentUser.email,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (currentUser.isCloudConnected) Icons.Default.CloudDone else Icons.Default.CloudQueue,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = if (currentUser.isCloudConnected) (if (lang == "bn") "ক্লাউড সিঙ্ক সক্রিয়" else "Firebase Cloud Sync Active") else Strings.t("guest_mode", lang),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                editNameText = currentUser.displayName
                                showEditNameDialog = true
                            },
                            modifier = Modifier.fillMaxWidth().testTag("edit_name_button")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(if (lang == "bn") "নাম পরিবর্তন করুন" else "Edit User Name")
                        }

                        Button(
                            onClick = onNavigateToBackup,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.CloudSync, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(Strings.t("cloud_sync", lang))
                        }

                        OutlinedButton(
                            onClick = { viewModel.signOut() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(Strings.t("sign_out", lang))
                        }
                    }
                }
            }
        } else {
            // Sign in / Registration Form
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = if (isRegisterMode) (if (lang == "bn") "নতুন অ্যাকাউন্ট নিবন্ধন" else "Create New Account")
                            else (if (lang == "bn") "লগইন করুন" else "Sign In"),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )

                        if (isRegisterMode) {
                            OutlinedTextField(
                                value = displayName,
                                onValueChange = { displayName = it },
                                label = { Text(if (lang == "bn") "নাম" else "Display Name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text(if (lang == "bn") "ইমেইল" else "Email") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text(if (lang == "bn") "পাসওয়ার্ড" else "Password") },
                            singleLine = true,
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (errorMsg != null) {
                            Text(
                                text = errorMsg ?: "",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        if (successMsg != null) {
                            Text(
                                text = successMsg ?: "",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        if (loading) {
                            Box(modifier = Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        } else {
                            Button(
                                onClick = {
                                    if (email.isNotBlank() && password.isNotBlank()) {
                                        loading = true
                                        errorMsg = null
                                        successMsg = null
                                        if (isRegisterMode) {
                                            viewModel.registerWithEmail(email, password, displayName) { success, err ->
                                                loading = false
                                                if (success) {
                                                    successMsg = if (lang == "bn") "অ্যাকাউন্ট তৈরি হয়েছে এবং ক্লাউড সিঙ্ক চালু হয়েছে!" else "Account created and cloud sync enabled!"
                                                } else {
                                                    errorMsg = err
                                                }
                                            }
                                        } else {
                                            viewModel.signInWithEmail(email, password) { success, err ->
                                                loading = false
                                                if (success) {
                                                    successMsg = if (lang == "bn") "লগইন সম্পন্ন হয়েছে এবং নামাজের হিস্ট্রি সিঙ্ক হয়েছে!" else "Signed in successfully! Prayer history synced from cloud."
                                                } else {
                                                    errorMsg = err
                                                }
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = email.isNotBlank() && password.isNotBlank()
                            ) {
                                Text(if (isRegisterMode) (if (lang == "bn") "নিবন্ধন সম্পন্ন করুন" else "Complete Registration") else Strings.t("sign_in", lang))
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
                                text = if (isRegisterMode) (if (lang == "bn") "পূর্বে অ্যাকাউন্ট থাকলে লগইন করুন" else "Already have an account? Sign In")
                                else (if (lang == "bn") "অ্যাকাউন্ট নেই? নতুন অ্যাকাউন্ট তৈরি করুন" else "Don't have an account? Register")
                            )
                        }
                    }
                }
            }
        }
    }
}
