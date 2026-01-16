package com.synthio.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.synthio.app.audio.MusicConstants.KeySignature
import com.synthio.app.audio.MusicConstants.KeyboardNote
import com.synthio.app.ui.theme.*

/**
 * Fullscreen keyboard view optimized for landscape orientation.
 * Displays a larger keyboard for better finger playability without filter knobs.
 */
@Composable
fun FullscreenKeyboardScreen(
    currentOctave: Int,
    onOctaveUp: () -> Unit,
    onOctaveDown: () -> Unit,
    onNoteOn: (KeyboardNote, Int) -> Unit,
    onNoteOff: (KeyboardNote, Int) -> Unit,
    onExitFullscreen: () -> Unit,
    isChordMode: Boolean,
    keySignature: KeySignature,
    isDarkMode: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isDarkMode) {
        Brush.verticalGradient(listOf(DarkSurface, DarkSurfaceLight))
    } else {
        Brush.verticalGradient(listOf(BackgroundCream, BackgroundLight))
    }
    
    var visibleWhiteKeys by remember { mutableFloatStateOf(8f) } // Default 8 white keys (C to C)
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Top controls row: Octave selector on left, Zoom in middle, Exit button on right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Octave selector
                OctaveSelector(
                    currentOctave = currentOctave,
                    onOctaveUp = onOctaveUp,
                    onOctaveDown = onOctaveDown,
                    isDarkMode = isDarkMode
                )
                
                // Zoom Slider
                // REMOVED: visibleWhiteKeys was here (too deep)
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 32.dp)
                ) {
                    Text(
                        text = "Scale",
                        style = SynthTypography.smallLabel.copy(
                            color = if (isDarkMode) DarkTextSecondary else TextSecondary
                        )
                    )
                    androidx.compose.material3.Slider(
                        value = visibleWhiteKeys,
                        onValueChange = { visibleWhiteKeys = it },
                        valueRange = 8f..15f,
                        steps = 6, // 8, 9, 10, 11, 12, 13, 14, 15
                        colors = androidx.compose.material3.SliderDefaults.colors(
                            thumbColor = if (isDarkMode) DarkPastelPink else PastelPink,
                            activeTrackColor = if (isDarkMode) DarkPastelPink else PastelPink,
                            inactiveTrackColor = if (isDarkMode) DarkSurfaceLight else SurfaceWhite
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                    )
                }
                
                // Exit fullscreen button
                FullscreenKeyboardButton(
                    isFullscreen = true,
                    onClick = onExitFullscreen,
                    isDarkMode = isDarkMode
                )
            }
            
            // Fullscreen keyboard - takes up remaining space
            FullscreenKeyboard(
                visibleWhiteKeys = visibleWhiteKeys.toInt(),
                onNoteOn = { note, shift -> onNoteOn(note, shift) },
                onNoteOff = { note, shift -> onNoteOff(note, shift) },
                isChordMode = isChordMode,
                keySignature = keySignature,
                isDarkMode = isDarkMode,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }
    }
}
