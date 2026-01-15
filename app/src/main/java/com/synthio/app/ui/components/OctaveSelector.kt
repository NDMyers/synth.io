package com.synthio.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synthio.app.ui.theme.*

/**
 * Octave selector component with left/right arrows and current octave display.
 * Allows users to transpose the keyboard up or down by octaves.
 */
@Composable
fun OctaveSelector(
    currentOctave: Int,
    onOctaveUp: () -> Unit,
    onOctaveDown: () -> Unit,
    modifier: Modifier = Modifier,
    isDarkMode: Boolean = false,
    minOctave: Int = 1,
    maxOctave: Int = 7
) {
    val backgroundColor = if (isDarkMode) {
        DarkSurfaceCard.copy(alpha = 0.6f)
    } else {
        SurfaceWhite.copy(alpha = 0.8f)
    }
    
    val textColor = if (isDarkMode) DarkTextPrimary else TextPrimary
    val secondaryTextColor = if (isDarkMode) DarkTextSecondary else TextSecondary
    val accentColor = if (isDarkMode) DarkPastelLavender else PastelLavender
    val buttonBgColor = if (isDarkMode) DarkPastelLavenderDark else PastelLavenderLight
    val disabledColor = if (isDarkMode) DarkSurfaceLight else PastelLavenderLight
    
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Label
        Text(
            text = "OCT",
            style = SynthTypography.label.copy(
                color = secondaryTextColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            ),
            modifier = Modifier.padding(end = 8.dp)
        )
        
        // Down button
        OctaveButton(
            onClick = onOctaveDown,
            enabled = currentOctave > minOctave,
            isDarkMode = isDarkMode,
            isLeft = true
        )
        
        // Current octave display
        Box(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .width(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    Brush.verticalGradient(
                        colors = if (isDarkMode) 
                            listOf(accentColor.copy(alpha = 0.3f), accentColor.copy(alpha = 0.15f))
                        else 
                            listOf(accentColor.copy(alpha = 0.4f), accentColor.copy(alpha = 0.2f))
                    )
                )
                .border(
                    width = 1.dp,
                    color = accentColor.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = currentOctave.toString(),
                style = SynthTypography.label.copy(
                    color = textColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
        
        // Up button
        OctaveButton(
            onClick = onOctaveUp,
            enabled = currentOctave < maxOctave,
            isDarkMode = isDarkMode,
            isLeft = false
        )
    }
}

@Composable
private fun OctaveButton(
    onClick: () -> Unit,
    enabled: Boolean,
    isDarkMode: Boolean,
    isLeft: Boolean
) {
    val accentColor = if (isDarkMode) DarkPastelLavender else PastelLavender
    val buttonBgColor = if (enabled) {
        if (isDarkMode) DarkPastelLavenderDark else PastelLavenderLight
    } else {
        if (isDarkMode) DarkSurfaceLight.copy(alpha = 0.3f) else PastelLavenderLight.copy(alpha = 0.3f)
    }
    val iconColor = if (enabled) {
        if (isDarkMode) DarkTextPrimary else TextPrimary
    } else {
        if (isDarkMode) DarkTextSecondary.copy(alpha = 0.4f) else TextSecondary.copy(alpha = 0.4f)
    }
    
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(buttonBgColor)
            .then(
                if (enabled) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
            .border(
                width = 1.dp,
                color = if (enabled) accentColor.copy(alpha = 0.4f) else Color.Transparent,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isLeft) Icons.Default.KeyboardArrowLeft else Icons.Default.KeyboardArrowRight,
            contentDescription = if (isLeft) "Octave down" else "Octave up",
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
    }
}
