package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

/**
 * Reusable branding component that displays the Ad-Deen logo and name together.
 */
@Composable
fun AdDeenBrandHeader(
    modifier: Modifier = Modifier,
    logoSize: Dp = 42.dp,
    showSubtitle: Boolean = true,
    subtitleText: String? = null,
    textColor: Color = com.example.ui.theme.AppColors.Secondary
) {
    Row(
        modifier = modifier.testTag("brand_header"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Logo with polished ambient ring
        AdDeenLogo(size = logoSize)

        Column {
            Text(
                text = "Ad-Deen",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    letterSpacing = (-0.3).sp
                )
            )
            if (showSubtitle && !subtitleText.isNullOrEmpty()) {
                Text(
                    text = subtitleText,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = com.example.ui.theme.AppColors.TextSecondary
                )
            }
        }
    }
}

/**
 * Standard Ad-Deen circular emblem with subtle shadow and border.
 */
@Composable
fun AdDeenLogo(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    animated: Boolean = false
) {
    val scale = if (animated) {
        val infiniteTransition = rememberInfiniteTransition(label = "logo_pulse")
        val pulseScale by infiniteTransition.animateFloat(
            initialValue = 0.96f,
            targetValue = 1.04f,
            animationSpec = infiniteRepeatable(
                animation = tween(1400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_scale"
        )
        pulseScale
    } else 1f

    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .shadow(8.dp, CircleShape, ambientColor = com.example.ui.theme.AppColors.Glow, spotColor = com.example.ui.theme.AppColors.Glow)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF9A5A1C),
                        Color(0xFF5E3208)
                    )
                )
            )
            .border(1.2.dp, Color(0xFFFFB030).copy(alpha = 0.85f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_app_logo),
            contentDescription = "Ad-Deen Official Logo",
            modifier = Modifier
                .fillMaxSize()
                .padding(size * 0.14f)
        )
    }
}
