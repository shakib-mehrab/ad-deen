package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AdDeenLogo
import kotlinx.coroutines.delay

@Composable
fun LoadingScreen(
    onFinished: () -> Unit
) {
    var startAnim by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        startAnim = true
        delay(1800L) // Graceful presentation time
        onFinished()
    }

    val alphaAnim by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0f,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "alpha"
    )

    val scaleAnim by animateFloatAsState(
        targetValue = if (startAnim) 1f else 0.88f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFFFE4))
            .testTag("app_loading_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(32.dp)
                .alpha(alphaAnim)
                .scale(scaleAnim)
        ) {
            // Animated Ad-Deen Official Logo
            AdDeenLogo(
                size = 110.dp,
                animated = true,
                modifier = Modifier.testTag("loading_screen_logo")
            )

            Spacer(Modifier.height(24.dp))

            // App Name & Arabic Title
            Text(
                text = "Ad-Deen",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF874D14), // Secondary Warm Chestnut
                    letterSpacing = 1.sp
                )
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "أَدِّين",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 20.sp,
                    color = Color(0xFF1F1F1F), // Clean Black #1F1F1F
                    letterSpacing = 2.sp
                )
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = "আপনার নির্ভরযোগ্য দ্বীনি সঙ্গী",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFFFB030) // Warm Primary Amber accent
                )
            )

            Spacer(Modifier.height(36.dp))

            // Subtle loading indicator
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                color = Color(0xFFFFB030),
                strokeWidth = 2.5.dp,
                trackColor = Color(0xFFFFEBD9)
            )
        }
    }
}
