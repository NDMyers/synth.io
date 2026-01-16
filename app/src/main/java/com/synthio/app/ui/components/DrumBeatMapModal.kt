package com.synthio.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synthio.app.ui.theme.*
import kotlin.math.roundToInt

/**
 * Step resolution options for the beat map editor
 */
enum class StepResolution(val label: String, val stepsToShow: Int, val stepMultiplier: Int) {
    SIXTEENTH("16th", 16, 1),
    QUARTER("Quarter", 4, 4),
    HALF("Half", 2, 8),
    WHOLE("Whole", 1, 16)
}

/**
 * Drum Beat Map Modal - DAW-style step sequencer for custom drum patterns
 */
@Composable
fun DrumBeatMapModal(
    kickPattern: List<Float>,
    snarePattern: List<Float>,
    hiHatPattern: List<Float>,
    kickVolume: Float,
    snareVolume: Float,
    hiHatVolume: Float,
    isDrumPlaying: Boolean,
    onTogglePlay: () -> Unit,
    onToggleStep: (instrument: Int, step: Int) -> Unit,
    onInstrumentVolumeChange: (instrument: Int, volume: Float) -> Unit,
    onResetPattern: () -> Unit,
    onDismiss: () -> Unit,
    isDarkMode: Boolean,
    modifier: Modifier = Modifier
) {
    var selectedResolution by remember { mutableStateOf(StepResolution.SIXTEENTH) }
    
    val backgroundColor = if (isDarkMode) {
        Brush.verticalGradient(listOf(DarkSurface, DarkBackground))
    } else {
        Brush.verticalGradient(listOf(BackgroundCream, BackgroundLight))
    }
    
    val cardColor = if (isDarkMode) DarkSurfaceCard else SurfaceWhite
    val textColor = if (isDarkMode) DarkTextPrimary else TextPrimary
    val secondaryTextColor = if (isDarkMode) DarkTextSecondary else TextSecondary
    
    // Grid dimensions
    val rowHeight = 48.dp
    val headerHeight = 32.dp
    val spacing = 8.dp
    
    // FULL SCREEN VIEW (No longer a centered modal)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor) // Apply background directly to full screen
            .clickable(enabled = true) { } // Consume all clicks to prevent interaction with underlying UI
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(16.dp) // Outer padding for the page content
        ) {
            // Header Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Drum Machine Pattern",
                    style = SynthTypography.heading.copy(color = textColor, fontSize = 24.sp)
                )
                
                // Resolution Selector
                Row(
                    modifier = Modifier
                        .clip(SynthShapes.medium)
                        .background(cardColor.copy(alpha = 0.5f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    StepResolution.values().forEach { resolution ->
                        ResolutionChip(
                            resolution = resolution,
                            isSelected = selectedResolution == resolution,
                            onClick = { selectedResolution = resolution },
                            isDarkMode = isDarkMode
                        )
                    }
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Play/Stop Button
                    Button(
                        onClick = onTogglePlay,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDrumPlaying) {
                                if (isDarkMode) DarkPastelPink else PastelPink
                            } else {
                                if (isDarkMode) DarkSurfaceLight else SurfaceWhite
                            },
                            contentColor = if (isDrumPlaying && !isDarkMode) Color.White else textColor
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        shape = SynthShapes.medium,
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isDrumPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = if (isDrumPlaying) "Stop" else "Play",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isDrumPlaying) "Stop" else "Play",
                            style = SynthTypography.label
                        )
                    }

                     Button(
                        onClick = onResetPattern,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDarkMode) DarkSurfaceLight else SurfaceWhite
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        shape = SynthShapes.medium,
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = textColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Reset",
                            style = SynthTypography.label.copy(color = textColor)
                        )
                    }
                    
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(SynthShapes.medium)
                            .background(cardColor.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = textColor
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Main Content - Split Pane
            // Left: Controls (Fixed)
            // Right: Grid (Scrollable)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(SynthShapes.large)
                    .background(cardColor.copy(alpha = 0.3f))
                    .padding(8.dp)
            ) {
                // === LEFT PANE: CONTROLS ===
                Column(
                    modifier = Modifier
                        .width(200.dp)
                        .padding(end = 12.dp)
                ) {
                    // Header align spacer
                    Spacer(modifier = Modifier.height(headerHeight))
                    Spacer(modifier = Modifier.height(spacing))
                    
                    // Kick Control
                    InstrumentControlPanel(
                        label = "Kick",
                        volume = kickVolume,
                        onVolumeChange = { onInstrumentVolumeChange(0, it) },
                        color = if (isDarkMode) DarkPastelPeach else PastelPeach,
                        height = rowHeight,
                        isDarkMode = isDarkMode
                    )
                    
                    Spacer(modifier = Modifier.height(spacing))
                    
                    // Snare Control
                    InstrumentControlPanel(
                        label = "Snare",
                        volume = snareVolume,
                        onVolumeChange = { onInstrumentVolumeChange(1, it) },
                        color = if (isDarkMode) DarkPastelBlue else PastelBlue,
                        height = rowHeight,
                        isDarkMode = isDarkMode
                    )
                    
                    Spacer(modifier = Modifier.height(spacing))
                    
                    // Hi-Hat Control
                    InstrumentControlPanel(
                        label = "Hi-Hat",
                        volume = hiHatVolume,
                        onVolumeChange = { onInstrumentVolumeChange(2, it) },
                        color = if (isDarkMode) DarkPastelMint else PastelMint,
                        height = rowHeight,
                        isDarkMode = isDarkMode
                    )
                }
                
                // Vertical Divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(if (isDarkMode) DarkSurfaceLight else SurfaceWhite)
                )
                
                // === RIGHT PANE: SCROLLABLE GRID ===
                // This Column scrolls horizontally, moving headers and all rows together
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState())
                        .padding(start = 12.dp)
                ) {
                    // Beat Numbers Header
                    Row(
                        modifier = Modifier.height(headerHeight),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 0 until selectedResolution.stepsToShow) {
                            val beatNumber = (i * selectedResolution.stepMultiplier / 4) + 1
                             // Show beat number only on beat starts, or show dots/ticks for others
                             val isBeatStart = (i * selectedResolution.stepMultiplier) % 4 == 0
                             
                            Box(
                                modifier = Modifier
                                    .width(42.dp) // Slightly wider than button
                                    .padding(horizontal = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isBeatStart) {
                                    Text(
                                        text = beatNumber.toString(),
                                        style = SynthTypography.smallLabel.copy(
                                            color = secondaryTextColor,
                                            fontSize = 12.sp, // Larger font
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                        )
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .clip(RoundedCornerShape(50))
                                            .background(secondaryTextColor.copy(alpha = 0.3f))
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(spacing))
                    
                    // Kick Steps
                    InstrumentStepsRow(
                        pattern = kickPattern,
                        color = if (isDarkMode) DarkPastelPeach else PastelPeach,
                        resolution = selectedResolution,
                        onToggleStep = { step -> onToggleStep(0, step) },
                        height = rowHeight,
                        isDarkMode = isDarkMode
                    )
                    
                    Spacer(modifier = Modifier.height(spacing))
                    
                    // Snare Steps
                    InstrumentStepsRow(
                        pattern = snarePattern,
                        color = if (isDarkMode) DarkPastelBlue else PastelBlue,
                        resolution = selectedResolution,
                        onToggleStep = { step -> onToggleStep(1, step) },
                        height = rowHeight,
                        isDarkMode = isDarkMode
                    )
                    
                    Spacer(modifier = Modifier.height(spacing))
                    
                    // Hi-Hat Steps
                    InstrumentStepsRow(
                        pattern = hiHatPattern,
                        color = if (isDarkMode) DarkPastelMint else PastelMint,
                        resolution = selectedResolution,
                        onToggleStep = { step -> onToggleStep(2, step) },
                        height = rowHeight,
                        isDarkMode = isDarkMode
                    )
                }
            }
        }
    }
}

@Composable
private fun ResolutionChip(
    resolution: StepResolution,
    isSelected: Boolean,
    onClick: () -> Unit,
    isDarkMode: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        isSelected && isDarkMode -> DarkPastelPink
        isSelected -> PastelPink
        isDarkMode -> DarkSurfaceLight
        else -> SurfaceWhite
    }
    
    val textColor = when {
        isSelected -> if (isDarkMode) DarkTextOnLight else TextPrimary
        isDarkMode -> DarkTextPrimary
        else -> TextPrimary
    }
    
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(horizontal = 12.dp)
    ) {
        Text(
            text = resolution.label,
            style = SynthTypography.smallLabel.copy(color = textColor),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun InstrumentControlPanel(
    label: String,
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    color: Color,
    height: androidx.compose.ui.unit.Dp,
    isDarkMode: Boolean
) {
    val textColor = if (isDarkMode) DarkTextPrimary else TextPrimary
    val secondaryTextColor = if (isDarkMode) DarkTextSecondary else TextSecondary
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(height),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = SynthTypography.label.copy(color = textColor)
            )
            Text(
                text = "Vol: ${(volume * 100).roundToInt()}%",
                style = SynthTypography.smallLabel.copy(
                    color = secondaryTextColor,
                    fontSize = 10.sp
                )
            )
        }
        
        Slider(
            value = volume,
            onValueChange = onVolumeChange,
            valueRange = 0f..1f,
            modifier = Modifier.width(100.dp),
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color,
                inactiveTrackColor = if (isDarkMode) DarkSurfaceLight else PastelLavenderLight
            )
        )
    }
}

@Composable
private fun InstrumentStepsRow(
    pattern: List<Float>,
    color: Color,
    resolution: StepResolution,
    onToggleStep: (Int) -> Unit,
    height: androidx.compose.ui.unit.Dp,
    isDarkMode: Boolean
) {
    Row(
        modifier = Modifier.height(height),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until resolution.stepsToShow) {
            val actualStep = i * resolution.stepMultiplier
            // Handle null safety for pattern access, default to inactive/0f
            val value = if (actualStep < pattern.size) pattern[actualStep] else 0f
            val isActive = value > 0f
            
            StepButton(
                isActive = isActive,
                color = color,
                isBeatStart = actualStep % 4 == 0,
                onClick = { onToggleStep(actualStep) },
                isDarkMode = isDarkMode
            )
        }
    }
}

@Composable
private fun StepButton(
    isActive: Boolean,
    color: Color,
    isBeatStart: Boolean,
    onClick: () -> Unit,
    isDarkMode: Boolean
) {
    val backgroundColor = when {
        isActive -> color
        isDarkMode -> DarkSurfaceLight.copy(alpha = 0.5f)
        else -> SurfaceWhite.copy(alpha = 0.7f)
    }
    
    val borderColor = when {
        isBeatStart -> if (isDarkMode) DarkTextSecondary else TextSecondary
        else -> Color.Transparent
    }
    
    Box(
        modifier = Modifier
            .padding(horizontal = 2.dp)
            .size(40.dp) // Larger touch target
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundColor)
            .border(
                width = if (isBeatStart) 1.5.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(6.dp)
            )
            .clickable { onClick() }
    )
}
