package com.synthio.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.synthio.app.ui.theme.*

/**
 * A small, unintrusive fullscreen toggle button for the keyboard.
 * Positioned to the right of the octave selector.
 */
@Composable
fun FullscreenKeyboardButton(
    isFullscreen: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDarkMode: Boolean = false
) {
    val backgroundColor = if (isDarkMode) {
        DarkSurfaceCard.copy(alpha = 0.6f)
    } else {
        SurfaceWhite.copy(alpha = 0.8f)
    }
    
    val accentColor = if (isDarkMode) DarkPastelMint else PastelMint
    val iconColor = if (isDarkMode) DarkTextPrimary else TextPrimary
    
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(
                width = 1.dp,
                color = accentColor.copy(alpha = 0.4f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isFullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
            contentDescription = if (isFullscreen) "Exit fullscreen" else "Enter fullscreen",
            tint = iconColor,
            modifier = Modifier.size(22.dp)
        )
    }
}
