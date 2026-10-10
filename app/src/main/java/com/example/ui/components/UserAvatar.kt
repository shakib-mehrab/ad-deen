package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AppColors

/** Circle with the first letter of the user's name; a person icon when no name is available (never a placeholder letter). */
@Composable
fun UserAvatar(name: String, size: Dp = 44.dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(size).background(AppColors.Primary, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        val initial = name.trim().firstOrNull()
        if (initial != null) {
            Text(
                text = initial.uppercase(),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = AppColors.OnPrimary
            )
        } else {
            Icon(Icons.Filled.Person, contentDescription = null, tint = AppColors.OnPrimary, modifier = Modifier.size(size * 0.55f))
        }
    }
}
