package com.synthio.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synthio.app.audio.MusicConstants.ChordType
import com.synthio.app.audio.MusicConstants.KeySignature
import com.synthio.app.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun SideMenuButton(
    onClick: () -> Unit,
    isDarkMode: Boolean,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(48.dp)
            .clip(SynthShapes.medium)
            .background(
                if (isDarkMode) DarkSurfaceCard.copy(alpha = 0.8f)
                else SurfaceWhite.copy(alpha = 0.8f)
            )
    ) {
        Icon(
            imageVector = Icons.Default.Menu,
            contentDescription = "Menu",
            tint = if (isDarkMode) DarkTextPrimary else TextPrimary
        )
    }
}

@Composable
fun SideMenuContent(
    selectedKeySignature: KeySignature,
    onKeySignatureSelected: (KeySignature) -> Unit,
    selectedChordType: ChordType,
    onChordTypeSelected: (ChordType) -> Unit,
    isDarkMode: Boolean,
    onDarkModeToggle: () -> Unit,
    // Drum machine props
    isDrumEnabled: Boolean,
    onDrumToggle: () -> Unit,
    isKickEnabled: Boolean,
    onKickToggle: () -> Unit,
    isSnareEnabled: Boolean,
    onSnareToggle: () -> Unit,
    isHiHatEnabled: Boolean,
    onHiHatToggle: () -> Unit,
    isHiHat16thNotes: Boolean,
    onHiHatModeToggle: () -> Unit,
    drumBPM: Float,
    onDrumBPMChange: (Float) -> Unit,
    drumVolume: Float,
    onDrumVolumeChange: (Float) -> Unit,
    // Wurlitzer props
    isWurlitzerMode: Boolean,
    onWurlitzerToggle: (Boolean) -> Unit,
    // Advanced settings
    onEditDrumPattern: () -> Unit,
    // Exports
    hasActiveExports: Boolean = false,
    exportCount: Int = 0,
    onOpenExports: () -> Unit = {},
    onCloseMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isDarkMode) {
        Brush.verticalGradient(listOf(DarkSurface, DarkBackground))
    } else {
        Brush.verticalGradient(listOf(BackgroundCream, BackgroundLight))
    }
    
    val textColor = if (isDarkMode) DarkTextPrimary else TextPrimary
    val secondaryTextColor = if (isDarkMode) DarkTextSecondary else TextSecondary
    val cardColor = if (isDarkMode) DarkSurfaceCard else SurfaceWhite
    val accentColor = if (isDarkMode) DarkPastelPink else PastelPink
    val accentLightColor = if (isDarkMode) DarkPastelPinkDark else PastelPinkLight
    
    val scrollState = rememberScrollState()
    
    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(300.dp)
            .background(backgroundColor)
            .padding(20.dp)
            .verticalScroll(scrollState)
    ) {
        // Header with close button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Settings",
                style = SynthTypography.heading.copy(color = textColor)
            )
            
            IconButton(
                onClick = onCloseMenu,
                modifier = Modifier
                    .size(40.dp)
                    .clip(SynthShapes.medium)
                    .background(cardColor.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close menu",
                    tint = textColor
                )
            }
        }
        
        // ========== DRUM MACHINE SECTION ==========
        Text(
            text = "Drum Machine",
            style = SynthTypography.subheading.copy(color = textColor),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(SynthShapes.large)
                .background(cardColor.copy(alpha = 0.5f))
                .padding(12.dp)
        ) {
            // Enable Toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isDrumEnabled) "Playing" else "Stopped",
                    style = SynthTypography.label.copy(color = textColor)
                )
                Switch(
                    checked = isDrumEnabled,
                    onCheckedChange = { onDrumToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = accentColor,
                        checkedTrackColor = accentLightColor,
                        uncheckedThumbColor = if (isDarkMode) DarkPastelLavender else PastelLavender,
                        uncheckedTrackColor = if (isDarkMode) DarkPastelLavenderDark else PastelLavenderLight
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Kick Toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Kick",
                    style = SynthTypography.label.copy(color = textColor)
                )
                Switch(
                    checked = isKickEnabled,
                    onCheckedChange = { onKickToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = if (isDarkMode) DarkPastelPeach else PastelPeach,
                        checkedTrackColor = if (isDarkMode) DarkPastelPeachDark else PastelPeachLight,
                        uncheckedThumbColor = if (isDarkMode) DarkPastelLavender else PastelLavender,
                        uncheckedTrackColor = if (isDarkMode) DarkPastelLavenderDark else PastelLavenderLight
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Snare Toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Snare",
                    style = SynthTypography.label.copy(color = textColor)
                )
                Switch(
                    checked = isSnareEnabled,
                    onCheckedChange = { onSnareToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = if (isDarkMode) DarkPastelBlue else PastelBlue,
                        checkedTrackColor = if (isDarkMode) DarkPastelBlueDark else PastelBlueLight,
                        uncheckedThumbColor = if (isDarkMode) DarkPastelLavender else PastelLavender,
                        uncheckedTrackColor = if (isDarkMode) DarkPastelLavenderDark else PastelLavenderLight
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Hi-Hat Toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Hi-Hat",
                    style = SynthTypography.label.copy(color = textColor)
                )
                Switch(
                    checked = isHiHatEnabled,
                    onCheckedChange = { onHiHatToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = if (isDarkMode) DarkPastelMint else PastelMint,
                        checkedTrackColor = if (isDarkMode) DarkPastelMintDark else PastelMintLight,
                        uncheckedThumbColor = if (isDarkMode) DarkPastelLavender else PastelLavender,
                        uncheckedTrackColor = if (isDarkMode) DarkPastelLavenderDark else PastelLavenderLight
                    )
                )
            }
            
            // Hi-Hat Mode Toggle (8th vs 16th notes) - only show when hi-hat is enabled
            if (isHiHatEnabled) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp)  // Indent to show it's a sub-option
                ) {
                    Text(
                        text = if (isHiHat16thNotes) "16th notes" else "8th notes",
                        style = SynthTypography.smallLabel.copy(color = secondaryTextColor)
                    )
                    Switch(
                        checked = isHiHat16thNotes,
                        onCheckedChange = { onHiHatModeToggle() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = if (isDarkMode) DarkPastelBlue else PastelBlue,
                            checkedTrackColor = if (isDarkMode) DarkPastelBlueDark else PastelBlueLight,
                            uncheckedThumbColor = if (isDarkMode) DarkPastelLavender else PastelLavender,
                            uncheckedTrackColor = if (isDarkMode) DarkPastelLavenderDark else PastelLavenderLight
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // BPM Slider
            Text(
                text = "Tempo: ${drumBPM.roundToInt()} BPM",
                style = SynthTypography.smallLabel.copy(color = secondaryTextColor)
            )
            Slider(
                value = drumBPM,
                onValueChange = onDrumBPMChange,
                valueRange = 60f..200f,
                colors = SliderDefaults.colors(
                    thumbColor = accentColor,
                    activeTrackColor = accentColor,
                    inactiveTrackColor = if (isDarkMode) DarkSurfaceLight else PastelLavenderLight
                ),
                modifier = Modifier.padding(vertical = 4.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Volume Slider
            Text(
                text = "Drum Volume: ${(drumVolume * 100).roundToInt()}%",
                style = SynthTypography.smallLabel.copy(color = secondaryTextColor)
            )
            Slider(
                value = drumVolume,
                onValueChange = onDrumVolumeChange,
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = accentColor,
                    activeTrackColor = accentColor,
                    inactiveTrackColor = if (isDarkMode) DarkSurfaceLight else PastelLavenderLight
                ),
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // ========== ADVANCED SETTINGS SECTION ==========
        Text(
            text = "Advanced Settings",
            style = SynthTypography.subheading.copy(color = textColor),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(SynthShapes.large)
                .background(cardColor.copy(alpha = 0.5f))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Edit Drum Pattern button
            Button(
                onClick = onEditDrumPattern,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDarkMode) DarkPastelPeach else PastelPeach
                ),
                shape = SynthShapes.medium
            ) {
                Text(
                    text = "Edit Drum Pattern",
                    style = SynthTypography.label.copy(
                        color = if (isDarkMode) DarkTextOnLight else TextPrimary
                    )
                )
            }
            
            // Exports button
            Button(
                onClick = onOpenExports,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDarkMode) DarkPastelMint else PastelMint
                ),
                shape = SynthShapes.medium
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Exports",
                        style = SynthTypography.label.copy(
                            color = if (isDarkMode) DarkTextOnLight else TextPrimary
                        )
                    )
                    if (exportCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (hasActiveExports) {
                                        if (isDarkMode) DarkPastelPink else PastelPink
                                    } else {
                                        if (isDarkMode) DarkTextSecondary else TextSecondary
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$exportCount",
                                style = SynthTypography.smallLabel.copy(
                                    color = if (isDarkMode) DarkTextOnLight else Color.White,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // ========== KEY SIGNATURE SECTION ==========
        Text(
            text = "Key Signature",
            style = SynthTypography.subheading.copy(color = textColor),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Key Signature Grid
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)  // Fixed height since parent is scrollable
                .clip(SynthShapes.large)
                .background(cardColor.copy(alpha = 0.5f))
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(KeySignature.entries.chunked(3)) { rowKeys ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    rowKeys.forEach { key ->
                        KeySignatureChip(
                            keySignature = key,
                            isSelected = key == selectedKeySignature,
                            isDarkMode = isDarkMode,
                            onClick = { onKeySignatureSelected(key) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    repeat(3 - rowKeys.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // ========== CHORD TYPE SECTION ==========
        Text(
            text = "Chord Type (for Chord Mode)",
            style = SynthTypography.subheading.copy(color = textColor),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Chord Type Selection Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(SynthShapes.large)
                .background(cardColor.copy(alpha = 0.5f))
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ChordType.entries.forEach { type ->
                ChordTypeChip(
                    chordType = type,
                    isSelected = type == selectedChordType,
                    isDarkMode = isDarkMode,
                    onClick = { onChordTypeSelected(type) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // ========== KEYBOARDS SECTION ==========
        Text(
            text = "Keyboards",
            style = SynthTypography.subheading.copy(color = textColor),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Keyboard Mode Selection
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .clip(SynthShapes.large)
                .background(cardColor.copy(alpha = 0.5f))
                .padding(16.dp)
        ) {
            Text(
                text = "Wurlitzer 200A",
                style = SynthTypography.label.copy(color = textColor)
            )
            Switch(
                checked = isWurlitzerMode,
                onCheckedChange = onWurlitzerToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = accentColor,
                    checkedTrackColor = if (isDarkMode) DarkPastelPeach else PastelPeach,
                    uncheckedThumbColor = if (isDarkMode) DarkPastelLavender else PastelLavender,
                    uncheckedTrackColor = if (isDarkMode) DarkPastelLavenderDark else PastelLavenderLight
                )
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // ========== APPEARANCE SECTION ==========
        Text(
            text = "Appearance",
            style = SynthTypography.subheading.copy(color = textColor),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Dark Mode Toggle
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .clip(SynthShapes.large)
                .background(cardColor.copy(alpha = 0.5f))
                .padding(16.dp)
        ) {
            Text(
                text = "Dark Mode",
                style = SynthTypography.label.copy(color = textColor)
            )
            
            Switch(
                checked = isDarkMode,
                onCheckedChange = { onDarkModeToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = accentColor,
                    checkedTrackColor = accentLightColor,
                    uncheckedThumbColor = if (isDarkMode) DarkPastelLavender else PastelLavender,
                    uncheckedTrackColor = if (isDarkMode) DarkPastelLavenderDark else PastelLavenderLight
                )
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun KeySignatureChip(
    keySignature: KeySignature,
    isSelected: Boolean,
    isDarkMode: Boolean,
    onClick: () -> Unit,
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
            .height(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = keySignature.displayName.take(2),
            style = SynthTypography.label.copy(color = textColor),
            maxLines = 1
        )
    }
}

/**
 * Chip component for chord type selection
 */
@Composable
private fun ChordTypeChip(
    chordType: ChordType,
    isSelected: Boolean,
    isDarkMode: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        isSelected && isDarkMode -> DarkPastelBlue
        isSelected -> PastelBlue
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
            .height(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = chordType.displayName,
            style = SynthTypography.label.copy(color = textColor),
            maxLines = 1
        )
    }
}
