package com.synthio.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.synthio.app.audio.ExportQuality
import com.synthio.app.ui.theme.*
import com.synthio.app.viewmodel.LoopTrackState

/**
 * Modal for configuring audio export settings
 */
@Composable
fun ExportModal(
    tracks: List<LoopTrackState>,
    selectedTracks: List<Int>,
    includesDrums: Boolean,
    onToggleTrack: (Int) -> Unit,
    onSelectAll: () -> Unit,
    onSetIncludeDrums: (Boolean) -> Unit,
    onStartExport: (ExportQuality) -> Unit,
    onDismiss: () -> Unit,
    isDarkMode: Boolean
) {
    val backgroundColor = if (isDarkMode) DarkSurface else SurfaceWhite
    val textColor = if (isDarkMode) DarkTextPrimary else TextPrimary
    val secondaryTextColor = if (isDarkMode) DarkTextSecondary else TextSecondary
    val cardColor = if (isDarkMode) DarkSurfaceCard else BackgroundLight
    val accentColor = if (isDarkMode) DarkPastelMint else PastelMint
    val accentPink = if (isDarkMode) DarkPastelPink else PastelPink
    
    val hasSelection = selectedTracks.isNotEmpty()
    val tracksWithContent = tracks.mapIndexedNotNull { index, track -> 
        if (track.hasContent) index else null 
    }
    val allSelected = tracksWithContent.all { selectedTracks.contains(it) }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(24.dp))
                .background(backgroundColor)
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Export Audio",
                        style = SynthTypography.heading.copy(
                            color = textColor,
                            fontSize = 24.sp
                        )
                    )
                    
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = textColor
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Track Selection Section
                Text(
                    text = "Select Tracks",
                    style = SynthTypography.subheading.copy(color = textColor),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(cardColor)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Select All row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSelectAll() }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Select All",
                            style = SynthTypography.label.copy(
                                color = accentColor,
                                fontWeight = FontWeight.Medium
                            )
                        )
                        
                        ExportCheckbox(
                            checked = allSelected && tracksWithContent.isNotEmpty(),
                            accentColor = accentColor,
                            isDarkMode = isDarkMode
                        )
                    }
                    
                    Divider(color = secondaryTextColor.copy(alpha = 0.2f))
                    
                    // Individual tracks
                    tracks.forEachIndexed { index, track ->
                        if (track.hasContent) {
                            val isSelected = selectedTracks.contains(index)
                            val trackColors = listOf(
                                if (isDarkMode) DarkPastelPeach else PastelPeach,
                                if (isDarkMode) DarkPastelBlue else PastelBlue,
                                if (isDarkMode) DarkPastelMint else PastelMint,
                                if (isDarkMode) DarkPastelLavender else PastelLavender
                            )
                            val trackColor = trackColors.getOrElse(index) { accentColor }
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) trackColor.copy(alpha = 0.15f)
                                        else Color.Transparent
                                    )
                                    .clickable { onToggleTrack(index) }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(trackColor.copy(alpha = 0.3f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${index + 1}",
                                            style = SynthTypography.smallLabel.copy(
                                                color = textColor,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.width(12.dp))
                                    
                                    Text(
                                        text = "Track ${index + 1}",
                                        style = SynthTypography.label.copy(color = textColor)
                                    )
                                }
                                
                                ExportCheckbox(
                                    checked = isSelected,
                                    accentColor = trackColor,
                                    isDarkMode = isDarkMode
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Include Drums Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(cardColor)
                        .clickable { onSetIncludeDrums(!includesDrums) }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Include Drums",
                            style = SynthTypography.label.copy(color = textColor)
                        )
                        Text(
                            text = "Add drum machine to mixdown",
                            style = SynthTypography.smallLabel.copy(color = secondaryTextColor)
                        )
                    }
                    
                    Switch(
                        checked = includesDrums,
                        onCheckedChange = { onSetIncludeDrums(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = accentPink,
                            checkedTrackColor = accentPink.copy(alpha = 0.5f),
                            uncheckedThumbColor = secondaryTextColor,
                            uncheckedTrackColor = secondaryTextColor.copy(alpha = 0.3f)
                        )
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Quality Selection / Export Buttons
                Text(
                    text = "Choose Format",
                    style = SynthTypography.subheading.copy(color = textColor),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                // Standard Quality Button (Recommended)
                Button(
                    onClick = { onStartExport(ExportQuality.COMPRESSED) },
                    enabled = hasSelection,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        disabledContainerColor = secondaryTextColor.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = "Standard Quality",
                            style = SynthTypography.label.copy(
                                color = if (hasSelection) textColor else secondaryTextColor,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "Recommended • .m4a • Smaller file",
                            style = SynthTypography.smallLabel.copy(
                                color = if (hasSelection) textColor.copy(alpha = 0.7f) else secondaryTextColor
                            )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // High Quality Button
                OutlinedButton(
                    onClick = { onStartExport(ExportQuality.HIGH_QUALITY) },
                    enabled = hasSelection,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = accentColor,
                        disabledContentColor = secondaryTextColor.copy(alpha = 0.5f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        2.dp,
                        if (hasSelection) accentColor else secondaryTextColor.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = "High Quality",
                            style = SynthTypography.label.copy(
                                color = if (hasSelection) textColor else secondaryTextColor,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "Lossless • .wav • Larger file",
                            style = SynthTypography.smallLabel.copy(
                                color = if (hasSelection) secondaryTextColor else secondaryTextColor.copy(alpha = 0.5f)
                            )
                        )
                    }
                }
                
                if (!hasSelection) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Select at least one track to export",
                        style = SynthTypography.smallLabel.copy(color = Color.Red.copy(alpha = 0.7f)),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun ExportCheckbox(
    checked: Boolean,
    accentColor: Color,
    isDarkMode: Boolean
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (checked) accentColor else Color.Transparent,
        label = "checkbox_bg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (checked) accentColor else if (isDarkMode) DarkTextSecondary else TextSecondary,
        label = "checkbox_border"
    )
    
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundColor)
            .border(2.dp, borderColor, RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = if (isDarkMode) DarkTextOnLight else Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
