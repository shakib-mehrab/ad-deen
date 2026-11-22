package com.example.ui.screens

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AllahName
import com.example.model.AzkharItem
import com.example.model.ZikrItem
import com.example.ui.MainViewModel
import com.example.ui.Strings
import com.example.ui.components.CalendarDateHelper
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.ui.theme.AppColors
import com.example.ui.theme.ArabicFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.ui.theme.JamatGreen
import com.example.ui.theme.PerfectGold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AzkharScreen(
    viewModel: MainViewModel,
    initialTab: Int = 0
) {
    val context = LocalContext.current
    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    val settings by viewModel.settings.collectAsState()
    val record by viewModel.todayRecord.collectAsState()
    val customZikrItems by viewModel.activeZikrItems.collectAsState()
    val lang = settings.language

    var selectedTabIndex by remember { mutableStateOf(initialTab.coerceIn(0, 5)) }

    val tabTitles = listOf(
        Strings.t("morning_azkhar", lang),
        Strings.t("evening_azkhar", lang),
        Strings.t("sleep_azkhar", lang),
        Strings.t("wake_azkhar", lang),
        Strings.t("names_99", lang),
        Strings.t("daily_zikr", lang)
    )

    var showAddCustomDialog by remember { mutableStateOf(false) }

    fun vibrateTap() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(30)
            }
        } catch (_: Exception) {}
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.Transparent)
    ) {
        // App Brand Header & Arabic Font Size Controller
        com.example.ui.components.ScreenTitle(
            title = if (lang == "bn") "আযকার" else "Azkhar",
            modifier = Modifier.padding(horizontal = 16.dp),
            trailing = {
                // Arabic font size control
                Row(
                    Modifier.clip(RoundedCornerShape(18.dp)).background(com.example.ui.theme.AppColors.WarmTintContainer),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.updateArabicFontSize(settings.arabicFontSize - 2) }, modifier = Modifier.size(34.dp), enabled = settings.arabicFontSize > 18) {
                        Text("A-", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = com.example.ui.theme.AppColors.Secondary)
                    }
                    IconButton(onClick = { viewModel.updateArabicFontSize(settings.arabicFontSize + 2) }, modifier = Modifier.size(34.dp), enabled = settings.arabicFontSize < 42) {
                        Text("A+", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = com.example.ui.theme.AppColors.Secondary)
                    }
                }
            }
        )

        // Segmented pill tabs (reference "Surah | Juz | Page")
        androidx.compose.foundation.lazy.LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(com.example.ui.theme.AppColors.WarmTintContainer)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(tabTitles.size) { index ->
                val isSelected = selectedTabIndex == index
                Text(
                    text = tabTitles[index],
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal),
                    color = if (isSelected) com.example.ui.theme.AppColors.Secondary else com.example.ui.theme.AppColors.TextSecondary,
                    maxLines = 1,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) com.example.ui.theme.AppColors.Surface else androidx.compose.ui.graphics.Color.Transparent)
                        .clickable { selectedTabIndex = index }
                        .padding(horizontal = 14.dp, vertical = 9.dp)
                )
            }
        }
        Spacer(Modifier.height(6.dp))

        Box(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            when (selectedTabIndex) {
                0 -> AzkharListContent(
                    sectionKey = "morning",
                    items = viewModel.morningAzkhar,
                    counters = record.counters,
                    lang = lang,
                    arabicFontSize = settings.arabicFontSize,
                    onCount = { id, target ->
                        vibrateTap()
                        viewModel.incrementCounter("morning", id, target)
                    },
                    onComplete = { id, target -> viewModel.completeCounter("morning", id, target) },
                    onReset = { id -> viewModel.resetCounter("morning", id) }
                )
                1 -> AzkharListContent(
                    sectionKey = "evening",
                    items = viewModel.eveningAzkhar,
                    counters = record.counters,
                    lang = lang,
                    arabicFontSize = settings.arabicFontSize,
                    onCount = { id, target ->
                        vibrateTap()
                        viewModel.incrementCounter("evening", id, target)
                    },
                    onComplete = { id, target -> viewModel.completeCounter("evening", id, target) },
                    onReset = { id -> viewModel.resetCounter("evening", id) }
                )
                2 -> AzkharListContent(
                    sectionKey = "sleep",
                    items = viewModel.sleepAzkhar,
                    counters = record.counters,
                    lang = lang,
                    arabicFontSize = settings.arabicFontSize,
                    onCount = { id, target ->
                        vibrateTap()
                        viewModel.incrementCounter("sleep", id, target)
                    },
                    onComplete = { id, target -> viewModel.completeCounter("sleep", id, target) },
                    onReset = { id -> viewModel.resetCounter("sleep", id) }
                )
                3 -> AzkharListContent(
                    sectionKey = "wake",
                    items = viewModel.wakeAzkhar,
                    counters = record.counters,
                    lang = lang,
                    arabicFontSize = settings.arabicFontSize,
                    onCount = { id, target ->
                        vibrateTap()
                        viewModel.incrementCounter("wake", id, target)
                    },
                    onComplete = { id, target -> viewModel.completeCounter("wake", id, target) },
                    onReset = { id -> viewModel.resetCounter("wake", id) }
                )
                4 -> AsmaulHusnaListContent(
                    items = viewModel.asmaulHusna,
                    lang = lang,
                    arabicFontSize = settings.arabicFontSize
                )
                5 -> CustomZikrContent(
                    items = customZikrItems,
                    counters = record.counters,
                    lang = lang,
                    onCount = { id, target ->
                        vibrateTap()
                        viewModel.incrementCounter("zikr", id, target)
                    },
                    onReset = { id -> viewModel.resetCounter("zikr", id) },
                    onAddClick = { showAddCustomDialog = true },
                    onDeleteClick = { viewModel.deleteCustomZikr(it) }
                )
            }
        }
    }

    if (showAddCustomDialog) {
        AddCustomZikrDialog(
            lang = lang,
            onDismiss = { showAddCustomDialog = false },
            onSave = { text, target ->
                viewModel.addCustomZikr(text, target)
                showAddCustomDialog = false
            }
        )
    }
}

/**
 * One azkhar per page: swipe (or the arrows) for the next one. Each card can be counted by tapping,
 * or marked complete at once for people who count on a hand tasbih. Transliteration and translation
 * stay folded until asked for; the choice carries over while swiping through the section.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AzkharListContent(
    sectionKey: String,
    items: List<AzkharItem>,
    counters: Map<String, Int>,
    lang: String,
    arabicFontSize: Int = 26,
    onCount: (String, Int) -> Unit,
    onComplete: (String, Int) -> Unit,
    onReset: (String) -> Unit
) {
    if (items.isEmpty()) return
    val isBn = lang == "bn"
    fun isDone(it: AzkharItem) = (counters["$sectionKey:${it.id}"] ?: 0) >= it.target
    val totalDone = items.count { isDone(it) }
    val pager = rememberPagerState(initialPage = items.indexOfFirst { !isDone(it) }.coerceAtLeast(0)) { items.size }
    val scope = rememberCoroutineScope()
    var showTranslit by rememberSaveable { mutableStateOf(false) }
    var showTranslation by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(bottom = com.example.ui.components.LocalBottomBarSpace.current)) {
        Spacer(Modifier.height(10.dp))
        // Progress: "Completed 3 / 15" + one segment per azkhar (filled = done, dark = current). Tap a segment to jump.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${Strings.t("completed", lang)}: ${num(totalDone, isBn)} / ${num(items.size, isBn)}",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp, fontFeatureSettings = "tnum"),
                color = AppColors.Secondary
            )
            if (totalDone == items.size) {
                Surface(color = AppColors.Primary, shape = RoundedCornerShape(6.dp)) {
                    Text(
                        text = "100%",
                        color = AppColors.OnPrimary,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            items.forEachIndexed { i, it ->
                val current = i == pager.currentPage
                Box(
                    Modifier
                        .weight(1f)
                        .height(18.dp)
                        .clickable { scope.launch { pager.animateScrollToPage(i) } },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(if (current) 6.dp else 4.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                when {
                                    current -> AppColors.Secondary
                                    isDone(it) -> AppColors.Primary
                                    else -> AppColors.WarmTintContainer
                                }
                            )
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        HorizontalPager(
            state = pager,
            pageSpacing = 12.dp,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) { page ->
            val item = items[page]
            val count = counters["$sectionKey:${item.id}"] ?: 0
            val isCompleted = count >= item.target

            // Finishing the card you are on moves to the next one after a short pause.
            var wasCompleted by remember(item.id) { mutableStateOf(isCompleted) }
            LaunchedEffect(isCompleted) {
                if (isCompleted && !wasCompleted && pager.currentPage == page && page < items.lastIndex) {
                    delay(650)
                    pager.animateScrollToPage(page + 1)
                }
                wasCompleted = isCompleted
            }

            AzkharCard(
                item = item,
                count = count,
                isCompleted = isCompleted,
                lang = lang,
                arabicFontSize = arabicFontSize,
                showTranslit = showTranslit,
                showTranslation = showTranslation,
                onToggleTranslit = { showTranslit = !showTranslit },
                onToggleTranslation = { showTranslation = !showTranslation },
                onCount = { onCount(item.id, item.target) },
                onComplete = { onComplete(item.id, item.target) },
                onReset = { onReset(item.id) }
            )
        }

        // Prev · "2 / 15" · Next  (swipe works too; the arrows help discoverability and accessibility)
        Row(
            Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } },
                enabled = pager.currentPage > 0
            ) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = if (isBn) "আগেরটি" else "Previous", tint = AppColors.Secondary) }
            Text(
                text = "${num(pager.currentPage + 1, isBn)} / ${num(items.size, isBn)}",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
                color = AppColors.TextSecondary
            )
            IconButton(
                onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                enabled = pager.currentPage < items.lastIndex
            ) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = if (isBn) "পরেরটি" else "Next", tint = AppColors.Secondary) }
        }
    }
}

private const val BN_DIGITS = "০১২৩৪৫৬৭৮৯"

private fun num(n: Int, isBn: Boolean): String =
    if (isBn) n.toString().map { c -> if (c.isDigit()) BN_DIGITS[c - '0'] else c }.joinToString("") else n.toString()

@Composable
private fun AzkharCard(
    item: AzkharItem,
    count: Int,
    isCompleted: Boolean,
    lang: String,
    arabicFontSize: Int,
    showTranslit: Boolean,
    showTranslation: Boolean,
    onToggleTranslit: () -> Unit,
    onToggleTranslation: () -> Unit,
    onCount: () -> Unit,
    onComplete: () -> Unit,
    onReset: () -> Unit
) {
    val isBn = lang == "bn"
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = if (isCompleted) AppColors.WarmTintContainer else AppColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = if (isCompleted) BorderStroke(1.2.dp, AppColors.Primary) else null,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Text(
                    text = "${num(item.order, isBn)}. ${if (isBn) item.titleBn else item.titleEn}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
                    color = AppColors.Secondary,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Surface(color = AppColors.Primary, shape = RoundedCornerShape(8.dp)) {
                    Text(
                        text = "${num(count, isBn)} / ${num(item.target, isBn)}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFeatureSettings = "tnum"),
                        color = AppColors.OnPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Long duas scroll inside the card; the actions stay pinned at the bottom.
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 10.dp)
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text(
                        text = item.arabic,
                        style = TextStyle(
                            fontFamily = ArabicFontFamily,
                            fontSize = arabicFontSize.sp,
                            lineHeight = (arabicFontSize * 2.0f).sp,
                            textDirection = TextDirection.Rtl
                        ),
                        textAlign = TextAlign.Start,
                        color = AppColors.TextPrimary,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                AnimatedVisibility(visible = showTranslit) {
                    Text(
                        text = item.transliteration,
                        style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic, fontSize = 14.sp, lineHeight = 21.sp),
                        color = AppColors.Secondary,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }
                AnimatedVisibility(visible = showTranslation) {
                    Text(
                        text = if (isBn) item.translationBn else item.translationEn,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp, lineHeight = 22.sp),
                        color = AppColors.TextSecondary,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }
            }

            // Show / hide toggles
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FoldChip(Strings.t("transliteration", lang), showTranslit, onToggleTranslit, Modifier.weight(1f))
                FoldChip(Strings.t("translation", lang), showTranslation, onToggleTranslation, Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))

            // Count | Mark complete | Reset
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = onCount,
                    enabled = !isCompleted,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("azkhar_tap_btn_${item.id}"),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.Primary,
                        contentColor = AppColors.OnPrimary,
                        disabledContainerColor = AppColors.Secondary,
                        disabledContentColor = AppColors.OnSecondary
                    )
                ) {
                    Icon(if (isCompleted) Icons.Default.CheckCircle else Icons.Default.TouchApp, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (isCompleted) Strings.t("completed", lang) else "${Strings.t("tap_count_short", lang)} (${num(count, isBn)}/${num(item.target, isBn)})",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp, fontFeatureSettings = "tnum"),
                        maxLines = 1
                    )
                }
                if (!isCompleted) {
                    OutlinedButton(
                        onClick = onComplete,
                        modifier = Modifier.heightIn(min = 48.dp).testTag("azkhar_done_btn_${item.id}"),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        border = BorderStroke(1.2.dp, AppColors.Secondary),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AppColors.Secondary)
                    ) {
                        Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            Strings.t("mark_complete", lang),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                            maxLines = 1
                        )
                    }
                }
                if (count > 0) {
                    IconButton(onClick = onReset, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.Refresh, contentDescription = Strings.t("reset", lang), tint = AppColors.Secondary)
                    }
                }
            }
        }
    }
}

/** Small pill that folds/unfolds a section of the card ("Transliteration ⌄"). */
@Composable
private fun FoldChip(label: String, open: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (open) AppColors.Secondary.copy(alpha = 0.12f) else AppColors.WarmTintContainer)
            .clickable(onClick = onClick)
            .semantics { stateDescription = if (open) "expanded" else "collapsed" }
            .heightIn(min = 40.dp)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold), color = AppColors.Secondary, maxLines = 1)
        Spacer(Modifier.width(4.dp))
        Icon(
            if (open) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = AppColors.Secondary,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun CustomZikrContent(
    items: List<ZikrItem>,
    counters: Map<String, Int>,
    lang: String,
    onCount: (String, Int) -> Unit,
    onReset: (String) -> Unit,
    onAddClick: () -> Unit,
    onDeleteClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 14.dp, bottom = 14.dp + com.example.ui.components.LocalBottomBarSpace.current),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Button(
                onClick = onAddClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = com.example.ui.theme.AppColors.Secondary,
                    contentColor = com.example.ui.theme.AppColors.OnSecondary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("add_custom_zikr_btn")
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = Strings.t("add_custom_zikr", lang),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        items(items) { item ->
            val count = counters["zikr:${item.id}"] ?: 0
            val isCompleted = count >= item.target

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isCompleted) com.example.ui.theme.AppColors.WarmTintContainer else com.example.ui.theme.AppColors.Surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = if (isCompleted) BorderStroke(1.2.dp, com.example.ui.theme.AppColors.Primary) else null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.text,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = com.example.ui.theme.AppColors.TextPrimary,
                            modifier = Modifier.weight(1f)
                        )

                        if (!item.isDefault) {
                            IconButton(
                                onClick = { onDeleteClick(item.id) },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete",
                                    tint = com.example.ui.theme.AppColors.Secondary
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Big Counter Button: #FFB030 or #874D14 if completed
                        Button(
                            onClick = { onCount(item.id, item.target) },
                            enabled = !isCompleted,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isCompleted) com.example.ui.theme.AppColors.Secondary else com.example.ui.theme.AppColors.Primary,
                                contentColor = if (isCompleted) com.example.ui.theme.AppColors.OnSecondary else com.example.ui.theme.AppColors.OnPrimary,
                                disabledContainerColor = com.example.ui.theme.AppColors.Secondary,
                                disabledContentColor = com.example.ui.theme.AppColors.OnSecondary
                            )
                        ) {
                            Icon(
                                imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.TouchApp,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (isCompleted) "${Strings.t("completed", lang)} ($count/${item.target})" else "$count / ${item.target}",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontFeatureSettings = "tnum"
                                )
                            )
                        }

                        if (count > 0) {
                            Spacer(Modifier.width(8.dp))
                            TextButton(
                                onClick = { onReset(item.id) },
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Reset",
                                    tint = com.example.ui.theme.AppColors.Secondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = Strings.t("reset", lang),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    ),
                                    color = com.example.ui.theme.AppColors.Secondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AsmaulHusnaListContent(items: List<AllahName>, lang: String, arabicFontSize: Int = 26) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 14.dp, bottom = 14.dp + com.example.ui.components.LocalBottomBarSpace.current),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items) { name ->
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = com.example.ui.theme.AppColors.Surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Number Badge: 36dp #FFF0CC circle with #874D14 text
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(com.example.ui.theme.AppColors.WarmTintContainer)
                            .border(1.dp, com.example.ui.theme.AppColors.BorderSubtle, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (lang == "bn") CalendarDateHelper.toBnDigits("${name.number}") else "${name.number}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                fontFeatureSettings = "tnum"
                            ),
                            color = com.example.ui.theme.AppColors.Secondary
                        )
                    }

                    Spacer(Modifier.width(14.dp))

                    // English / Transliteration & Meaning Column (Even weight 1f)
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = name.transliteration,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            ),
                            color = com.example.ui.theme.AppColors.TextPrimary
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = if (lang == "bn") name.meaningBn else name.meaningEn,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = com.example.ui.theme.AppColors.TextSecondary
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    // Arabic Name Column (Even weight 1f) in #874D14 (never gold)
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = name.arabic,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = ArabicFontFamily,
                                fontSize = (arabicFontSize + 2).sp
                            ),
                            textAlign = TextAlign.End,
                            color = com.example.ui.theme.AppColors.Secondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddCustomZikrDialog(
    lang: String,
    onDismiss: () -> Unit,
    onSave: (String, Int) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var targetStr by remember { mutableStateOf("33") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Strings.t("add_custom_zikr", lang)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(Strings.t("zikr_title", lang)) },
                    placeholder = { Text("e.g. Subhanallahi wa bihamdih") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = targetStr,
                    onValueChange = { targetStr = it.filter { ch -> ch.isDigit() } },
                    label = { Text(Strings.t("target_count", lang)) },
                    placeholder = { Text("33") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val target = targetStr.toIntOrNull()?.coerceAtLeast(1) ?: 33
                    if (text.isNotBlank()) {
                        onSave(text, target)
                    }
                },
                enabled = text.isNotBlank()
            ) {
                Text(Strings.t("save", lang))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Strings.t("cancel", lang))
            }
        }
    )
}
