package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.UpdateUiState
import com.example.update.UpdateManager
import com.example.ui.theme.AppColors

/** "Update available" sheet: notes, download progress, install-permission step, and errors. */
@Composable
fun UpdateSheet(
    state: UpdateUiState,
    lang: String,
    onDismiss: () -> Unit,
    onDownload: () -> Unit,
    onInstall: () -> Unit
) {
    val bn = lang == "bn"
    val context = LocalContext.current
    // Re-check "Install unknown apps" permission when the user comes back from Settings.
    var canInstall by remember { mutableStateOf(UpdateManager.canInstall(context)) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) canInstall = UpdateManager.canInstall(context) }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
    val manifest = when (state) {
        is UpdateUiState.Available -> state.manifest
        is UpdateUiState.Downloading -> state.manifest
        is UpdateUiState.Ready -> state.manifest
        is UpdateUiState.Failed -> state.manifest
        else -> null
    }
    val mandatory = manifest?.isMandatoryFor(UpdateManager.currentVersionCode) == true

    SoftSheet(
        title = when (state) {
            UpdateUiState.UpToDate -> if (bn) "আপডেট" else "Updates"
            UpdateUiState.Checking -> if (bn) "আপডেট খোঁজা হচ্ছে" else "Checking for updates"
            else -> if (bn) "নতুন আপডেট" else "Update available"
        },
        onDismiss = { if (state !is UpdateUiState.Downloading) onDismiss() }
    ) {
        Column(Modifier.fillMaxWidth().testTag("update_sheet"), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                SoftTile(Modifier.fillMaxSize(), RoundedCornerShape(32.dp)) {
                    Icon(
                        if (state is UpdateUiState.UpToDate) Icons.Filled.CheckCircle else Icons.Filled.SystemUpdate,
                        null,
                        tint = AppColors.Secondary,
                        modifier = Modifier.size(30.dp).align(Alignment.Center)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                when (state) {
                    UpdateUiState.Checking -> if (bn) "অনুগ্রহ করে অপেক্ষা করুন…" else "Please wait…"
                    UpdateUiState.UpToDate -> (if (bn) "আপনি সর্বশেষ সংস্করণ ব্যবহার করছেন" else "You're on the latest version") + " (${UpdateManager.currentVersionName})"
                    else -> manifest?.let { "Ad-Deen ${it.versionName}" + if (bn) "  •  বর্তমান ${UpdateManager.currentVersionName}" else "  •  you have ${UpdateManager.currentVersionName}" } ?: ""
                },
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = AppColors.TextPrimary,
                textAlign = TextAlign.Center
            )
            val notes = manifest?.let { if (bn) it.notesBn else it.notesEn }.orEmpty()
            if (notes.isNotBlank() && state !is UpdateUiState.UpToDate) {
                Spacer(Modifier.height(10.dp))
                Text(
                    notes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextSecondary,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                )
            }
            if (mandatory && state is UpdateUiState.Available) {
                Spacer(Modifier.height(8.dp))
                Text(
                    if (bn) "এই সংস্করণটি এখন আর সমর্থিত নয় — অনুগ্রহ করে আপডেট করুন।" else "This version is no longer supported. Please update.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        when (state) {
            UpdateUiState.Checking -> LinearProgressIndicator(Modifier.fillMaxWidth().clip(CircleShape), color = AppColors.Action, trackColor = AppColors.ProgressTrack)
            UpdateUiState.UpToDate, UpdateUiState.Idle -> PrimaryPillButton(if (bn) "ঠিক আছে" else "OK", Modifier.fillMaxWidth(), onClick = onDismiss)
            is UpdateUiState.Available -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!mandatory) GhostPillButton(if (bn) "পরে" else "Later", Modifier.weight(1f), onClick = onDismiss)
                PrimaryPillButton(if (bn) "আপডেট করুন" else "Update", Modifier.weight(1f).testTag("update_download"), onClick = onDownload)
            }
            is UpdateUiState.Downloading -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                LinearProgressIndicator(
                    progress = { state.progress },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                    color = AppColors.Action,
                    trackColor = AppColors.ProgressTrack,
                    drawStopIndicator = {}
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    (if (bn) "ডাউনলোড হচ্ছে " else "Downloading ") + "${(state.progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = AppColors.TextSecondary
                )
            }
            is UpdateUiState.Ready -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!canInstall) {
                    Text(
                        if (bn) "একবারের জন্য Ad-Deen-কে অ্যাপ ইনস্টল করার অনুমতি দিন, তারপর ফিরে এসে ইনস্টল চাপুন।"
                        else "Allow Ad-Deen to install updates (one time), then come back and tap Install.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    PrimaryPillButton(if (bn) "অনুমতি দিন" else "Allow installs", Modifier.fillMaxWidth()) {
                        UpdateManager.openInstallPermissionSettings(context)
                    }
                } else {
                    Text(
                        if (bn) "ডাউনলোড ও যাচাই সম্পন্ন। আপনার সব ডাটা সংরক্ষিত থাকবে।" else "Downloaded and verified. Your data stays as it is.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    PrimaryPillButton(if (bn) "ইনস্টল করুন" else "Install", Modifier.fillMaxWidth().testTag("update_install"), onClick = onInstall)
                }
            }
            is UpdateUiState.Failed -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(state.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GhostPillButton(if (bn) "বন্ধ" else "Close", Modifier.weight(1f), onClick = onDismiss)
                    if (state.manifest != null) PrimaryPillButton(if (bn) "আবার চেষ্টা" else "Retry", Modifier.weight(1f), onClick = onDownload)
                }
            }
        }
    }
}
