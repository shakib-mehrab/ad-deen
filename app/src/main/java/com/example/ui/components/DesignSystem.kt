package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AppColors
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

// ============================================================================
// Ad-Deen soft design language (Deen-style references):
//  - warm paper background, white/cream surfaces, NO outlines, very faint warm shadow
//  - one accent (amber) + brand brown ink; colourful flat icons only in the quick grid
//  - Poppins / Hind Siliguri, medium weights, generous spacing
// ============================================================================

/** Gentle scale-down while pressed. */
fun Modifier.pressScale(interaction: MutableInteractionSource, pressedScale: Float = 0.98f): Modifier = composed {
    val pressed by interaction.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) pressedScale else 1f, spring(stiffness = Spring.StiffnessMediumLow), label = "press")
    this.scale(s)
}

/** Soft surface card: white on paper, 20dp corners, barely-there warm shadow, no outline. */
@Composable
fun GlowCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(20.dp),
    color: Color = AppColors.Surface,
    elevation: Dp = 2.dp,
    highlighted: Boolean = false,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .then(if (onClick != null) Modifier.pressScale(interaction) else Modifier)
            .shadow(if (AppColors.IsNight) 0.dp else elevation.coerceAtMost(3.dp), shape, ambientColor = AppColors.CardShadow, spotColor = AppColors.CardShadow)
            .clip(shape)
            .background(color)
            .then(if (highlighted) Modifier.border(BorderStroke(1.2.dp, AppColors.Primary), shape) else Modifier)
            .then(if (onClick != null) Modifier.clickable(interactionSource = interaction, indication = ripple(), onClick = onClick) else Modifier)
            .padding(contentPadding),
        content = content
    )
}

/** Cream tile used inside cards (prayer chips, icon wells, setting rows). */
@Composable
fun SoftTile(modifier: Modifier = Modifier, shape: RoundedCornerShape = RoundedCornerShape(16.dp), content: @Composable BoxScope.() -> Unit) {
    Box(modifier.clip(shape).background(AppColors.WarmTintContainer), content = content)
}

/** Centered uppercase screen title, like "DEEN MUSLIM" / "QURAN" in the reference. */
@Composable
fun ScreenTitle(title: String, modifier: Modifier = Modifier, leading: (@Composable () -> Unit)? = null, trailing: (@Composable () -> Unit)? = null) {
    Box(modifier.fillMaxWidth().height(52.dp), contentAlignment = Alignment.Center) {
        if (leading != null) Box(Modifier.align(Alignment.CenterStart)) { leading() }
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp),
            color = AppColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 56.dp)
        )
        if (trailing != null) Box(Modifier.align(Alignment.CenterEnd)) { trailing() }
    }
}

/** Section title row with optional "See all ›" action. */
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
            color = AppColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (action != null && onAction != null) {
            Row(
                Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onAction).padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(action, style = MaterialTheme.typography.labelLarge, color = AppColors.Secondary)
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = AppColors.Secondary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/** Filled action button (Save / Continue). Brand brown by day, amber at night. */
@Composable
fun PrimaryPillButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .pressScale(interaction)
            .heightIn(min = 54.dp)
            .clip(RoundedCornerShape(27.dp))
            .background(if (enabled) AppColors.Action else AppColors.DisabledTrack)
            .clickable(interactionSource = interaction, indication = ripple(), enabled = enabled, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = AppColors.OnAction, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Outlined companion button (Cancel). */
@Composable
fun GhostPillButton(text: String, modifier: Modifier = Modifier, contentColor: Color = AppColors.Action, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .pressScale(interaction)
            .heightIn(min = 54.dp)
            .clip(RoundedCornerShape(27.dp))
            .border(1.2.dp, contentColor, RoundedCornerShape(27.dp))
            .clickable(interactionSource = interaction, indication = ripple(), onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = contentColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun PageDots(count: Int, current: Int, modifier: Modifier = Modifier, active: Color = AppColors.Action, inactive: Color = AppColors.DisabledTrack) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(count) { i ->
            val w by animateDpAsState(if (i == current) 20.dp else 6.dp, spring(stiffness = Spring.StiffnessMediumLow), label = "dot")
            Box(Modifier.height(6.dp).width(w).clip(CircleShape).background(if (i == current) active else inactive))
        }
    }
}

// ---------------------------------------------------------------------------- toggles & settings

/** The one switch style used everywhere: brown (day) / amber (night) track, white thumb, no outline. */
@Composable
fun SoftSwitch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        modifier = modifier,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = AppColors.Action,
            checkedBorderColor = Color.Transparent,
            uncheckedThumbColor = Color.White,
            uncheckedTrackColor = AppColors.DisabledTrack,
            uncheckedBorderColor = Color.Transparent,
            disabledCheckedTrackColor = AppColors.Action.copy(alpha = 0.4f),
            disabledUncheckedTrackColor = AppColors.DisabledTrack.copy(alpha = 0.6f)
        )
    )
}

/**
 * List row with icon tile, title, subtitle and a switch (prayer-list reference).
 * Tapping the row calls [onClick] (e.g. opens a settings sheet); the switch toggles [checked].
 */
@Composable
fun ToggleRow(
    icon: ImageVector?,
    title: String,
    subtitle: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    highlighted: Boolean = false,
    onClick: (() -> Unit)? = null,
    switchTag: String? = null
) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (highlighted) AppColors.WarmTintAlternate else AppColors.Surface)
            .clickable(interactionSource = interaction, indication = ripple(), enabled = enabled) { (onClick ?: { onCheckedChange(!checked) })() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            SoftTile(Modifier.size(44.dp), RoundedCornerShape(12.dp)) {
                Icon(icon, null, tint = AppColors.Secondary, modifier = Modifier.size(22.dp).align(Alignment.Center))
            }
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp), color = AppColors.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"), color = AppColors.TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.width(8.dp))
        SoftSwitch(checked, if (enabled) onCheckedChange else null, enabled = enabled, modifier = if (switchTag != null) Modifier.testTag(switchTag) else Modifier)
    }
}

/** Grouped settings container: soft card holding [SettingRow]s with hairline separators. */
@Composable
fun SettingsGroup(title: String? = null, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth()) {
        if (title != null) {
            Text(
                title.uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp, fontWeight = FontWeight.SemiBold),
                color = AppColors.TextSecondary,
                modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

/**
 * Settings row (notification-sheet reference): cream rounded row, label left, trailing slot right.
 */
@Composable
fun SettingRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    titleColor: Color = AppColors.TextPrimary,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(AppColors.WarmTintContainer)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, null, tint = AppColors.Secondary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium), color = titleColor, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = AppColors.TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        }
    }
}

@Composable
fun SettingSwitchRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier, subtitle: String? = null, icon: ImageVector? = null, enabled: Boolean = true) {
    SettingRow(title, modifier, subtitle, icon, onClick = if (enabled) ({ onCheckedChange(!checked) }) else null) {
        SoftSwitch(checked, if (enabled) onCheckedChange else null, enabled = enabled)
    }
}

/** Row with a value and a chevron ("Repeat  Every day ›"). */
@Composable
fun SettingValueRow(title: String, value: String?, onClick: () -> Unit, modifier: Modifier = Modifier, subtitle: String? = null, icon: ImageVector? = null, titleColor: Color = AppColors.TextPrimary) {
    SettingRow(title, modifier, subtitle, icon, titleColor, onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (value != null) {
                Text(value, style = MaterialTheme.typography.bodyMedium, color = AppColors.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 150.dp))
                Spacer(Modifier.width(2.dp))
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = AppColors.TextSecondary, modifier = Modifier.size(20.dp))
        }
    }
}

/**
 * Bottom sheet in the notification-sheet style: centred title, close (x), content, Cancel / Save.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoftSheet(
    title: String,
    onDismiss: () -> Unit,
    cancelText: String? = null,
    confirmText: String? = null,
    onConfirm: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = AppColors.Surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { Box(Modifier.padding(top = 10.dp).size(40.dp, 4.dp).clip(CircleShape).background(AppColors.DisabledTrack)) }
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp).navigationBarsPadding()) {
            Box(Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.Center) {
                Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = AppColors.TextPrimary, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 48.dp))
                IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterEnd)) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = AppColors.TextPrimary)
                }
            }
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
            if (confirmText != null && onConfirm != null) {
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GhostPillButton(cancelText ?: "Cancel", Modifier.weight(1f), onClick = onDismiss)
                    PrimaryPillButton(confirmText, Modifier.weight(1f), onClick = onConfirm)
                }
            }
        }
    }
}

/**
 * Space to leave at the end of scrolling content so the last item can scroll clear of the
 * bottom navigation bar (the bar floats over content and hides while scrolling).
 */
val LocalBottomBarSpace = staticCompositionLocalOf { 0.dp }

// ---------------------------------------------------------------------------- hero art

private data class Star(val x: Float, val y: Float, val r: Float, val phase: Float)

/**
 * Hero backdrop: a soft amber glow blooming from the top (day), dimmer with a few faint stars (night).
 * Fades into the page background so the hero sits on the page instead of in a box.
 */
@Composable
fun SkyBackdrop(
    modifier: Modifier = Modifier,
    isNight: Boolean = AppColors.IsNight,
    top: Color = AppColors.palette.heroTop,
    bottom: Color = AppColors.palette.heroBottom,
    celestialAt: Offset? = null,
    /**
     * Real sun/moon position: 0 = rising (left horizon), 1 = setting (right horizon), null = hidden.
     * Drawn faintly on a dotted arc behind the hero content.
     */
    celestialProgress: Float? = null
) {
    val stars = remember { List(26) { Random(it * 31 + 7).let { r -> Star(r.nextFloat(), r.nextFloat() * 0.5f, 0.5f + r.nextFloat() * 1.1f, r.nextFloat() * 6.28f) } } }
    val transition = rememberInfiniteTransition(label = "sky")
    val time by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(20_000, easing = LinearEasing)), label = "t")
    val breathe by transition.animateFloat(0.94f, 1.04f, infiniteRepeatable(tween(4200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "breathe")
    Canvas(modifier) {
        drawRect(bottom)
        val c = Offset(size.width / 2f, size.height * 0.30f)
        val r = size.maxDimension * 0.62f * breathe
        drawCircle(Brush.radialGradient(listOf(top, top.copy(alpha = 0.55f), bottom.copy(alpha = 0f)), c, r), r, c)
        if (isNight) {
            stars.forEach { s ->
                val tw = 0.25f + 0.55f * ((sin(time * 2 * PI * 2 + s.phase).toFloat() + 1f) / 2f)
                drawCircle(Color(0xFFFFE9C2).copy(alpha = tw), s.r.dp.toPx(), Offset(s.x * size.width, s.y * size.height))
            }
        }
        celestialAt?.let { drawCrescent(Offset(size.width * it.x, size.height * it.y), 14.dp.toPx(), if (isNight) Color(0xFFF6D9A6) else Color(0xFFFFF4DF)) }
        celestialProgress?.let { raw ->
            val p = raw.coerceIn(0f, 1f)
            val left = size.width * 0.06f
            val right = size.width * 0.94f
            val baseY = size.height * 0.60f
            val amp = size.height * 0.42f
            fun pointAt(t: Float) = Offset(left + (right - left) * t, baseY - amp * sin(PI * t).toFloat())
            // Faint dotted path of the sky
            val dot = if (isNight) Color(0x33F6D9A6) else Color(0x40B8742A)
            var t = 0f
            while (t <= 1f) {
                drawCircle(dot, 1.2.dp.toPx(), pointAt(t))
                t += 0.025f
            }
            val at = pointAt(p)
            if (isNight) {
                drawCircle(Brush.radialGradient(listOf(Color(0x33F6D9A6), Color(0x00F6D9A6)), at, 34.dp.toPx()), 34.dp.toPx(), at)
                drawCrescent(at, 11.dp.toPx(), Color(0x99F6D9A6))
            } else {
                drawCircle(Brush.radialGradient(listOf(Color(0x66FFF3D6), Color(0x00FFF3D6)), at, 40.dp.toPx()), 40.dp.toPx(), at)
                drawCircle(Color(0x8CFFE6B0), 12.dp.toPx(), at)
            }
        }
    }
}

private fun DrawScope.drawCrescent(center: Offset, r: Float, color: Color) {
    val moon = Path().apply { addOval(Rect(center, r)) }
    val bite = Path().apply { addOval(Rect(Offset(center.x + r * 0.42f, center.y - r * 0.28f), r * 0.9f)) }
    drawPath(Path.combine(PathOperation.Difference, moon, bite), color)
}

/** Pale mosque skyline that fades downward into the page (reference hero). */
@Composable
fun MosqueSkyline(modifier: Modifier = Modifier, color: Color = AppColors.palette.skyline, glow: Color? = null) {
    Canvas(modifier) { drawMosqueSkyline(color, glow) }
}

fun DrawScope.drawMosqueSkyline(color: Color, glow: Color?) {
    val w = size.width
    val h = size.height
    val base = h
    val cx = w / 2f
    if (glow != null) {
        drawCircle(Brush.radialGradient(listOf(glow.copy(alpha = 0.45f), glow.copy(alpha = 0f)), Offset(cx, h * 0.5f), w * 0.3f), w * 0.3f, Offset(cx, h * 0.5f))
    }
    fun dome(p: Path, centerX: Float, baseY: Float, halfW: Float, height: Float) {
        p.moveTo(centerX - halfW, baseY)
        p.cubicTo(centerX - halfW * 1.08f, baseY - height * 0.55f, centerX - halfW * 0.35f, baseY - height * 0.82f, centerX, baseY - height)
        p.cubicTo(centerX + halfW * 0.35f, baseY - height * 0.82f, centerX + halfW * 1.08f, baseY - height * 0.55f, centerX + halfW, baseY)
        p.close()
    }
    fun finial(p: Path, x: Float, topY: Float, s: Float) {
        p.addRect(Rect(x - s * 0.08f, topY - s * 1.2f, x + s * 0.08f, topY))
        p.addOval(Rect(Offset(x, topY - s * 1.5f), s * 0.32f))
    }
    val p = Path()
    // Hall
    p.addRect(Rect(w * 0.16f, base - h * 0.22f, w * 0.84f, base))
    // Main dome on drum
    p.addRect(Rect(cx - w * 0.13f, base - h * 0.34f, cx + w * 0.13f, base - h * 0.21f))
    dome(p, cx, base - h * 0.335f, w * 0.15f, h * 0.34f)
    finial(p, cx, base - h * 0.675f, h * 0.06f)
    // Secondary domes
    for ((sx, sc) in listOf(cx - w * 0.25f to 0.55f, cx + w * 0.25f to 0.55f, cx - w * 0.38f to 0.38f, cx + w * 0.38f to 0.38f)) {
        val by = base - h * 0.215f
        dome(p, sx, by, w * 0.15f * sc, h * 0.3f * sc)
        finial(p, sx, by - h * 0.3f * sc, h * 0.045f)
    }
    // Slender minarets with balconies and pointed caps
    for ((mx, tall) in listOf(w * 0.06f to 0.86f, w * 0.94f to 0.86f, w * 0.22f to 0.62f, w * 0.78f to 0.62f)) {
        val mw = w * 0.012f
        val top = base - h * tall
        p.addRect(Rect(mx - mw, top, mx + mw, base))
        p.addRect(Rect(mx - mw * 1.9f, top + h * tall * 0.28f, mx + mw * 1.9f, top + h * tall * 0.28f + h * 0.018f))
        p.moveTo(mx - mw * 1.4f, top)
        p.lineTo(mx, top - h * 0.08f)
        p.lineTo(mx + mw * 1.4f, top)
        p.close()
        finial(p, mx, top - h * 0.08f, h * 0.035f)
    }
    drawPath(p, Brush.verticalGradient(listOf(color, color.copy(alpha = color.alpha * 0.55f), color.copy(alpha = 0f)), startY = 0f, endY = h))
}
