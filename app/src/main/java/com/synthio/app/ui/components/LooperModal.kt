package com.synthio.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
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
import com.synthio.app.audio.LooperState
import com.synthio.app.ui.theme.*
import com.synthio.app.viewmodel.LoopTrackState

/**
 * Modal dialog for managing multi-track looper
 */
@Composable
fun LooperModal(
    tracks: List<LoopTrackState>,
    looperState: LooperState,
    activeRecordingTrack: Int,
    currentBeat: Int,
    currentBar: Int,
    onStartRecordingTrack: (Int) -> Unit,
    onTrackVolumeChange: (Int, Float) -> Unit,
    onToggleMute: (Int) -> Unit,
    onToggleSolo: (Int) -> Unit,
    onDeleteTrack: (Int) -> Unit,
    onDeleteAll: () -> Unit,
    onPlayStop: () -> Unit,
    onDismiss: () -> Unit,
    isDarkMode: Boolean
) {
    val backgroundColor = if (isDarkMode) DarkSurface else SurfaceWhite
    val textColor = if (isDarkMode) DarkTextPrimary else TextPrimary
    val secondaryTextColor = if (isDarkMode) DarkTextSecondary else TextSecondary
    val cardColor = if (isDarkMode) DarkSurfaceCard else BackgroundLight
    val accentColor = if (isDarkMode) DarkPastelPink else PastelPink
    
    val hasAnyContent = tracks.any { it.hasContent }
    val usedTrackCount = tracks.count { it.hasContent }
    val allTracksFull = usedTrackCount >= 4
    val isRecording = looperState == LooperState.RECORDING || looperState == LooperState.PRE_COUNT
    
    Dialog(
        onDismissRequest = { if (!isRecording) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = !isRecording,
            dismissOnClickOutside = !isRecording,
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
                        text = "Looper",
                        style = SynthTypography.heading.copy(
                            color = textColor,
                            fontSize = 24.sp
                        )
                    )
                    
                    if (!isRecording) {
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
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Status indicator
                if (isRecording) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        val recordingDot by animateColorAsState(
                            targetValue = if (currentBeat % 2 == 0) Color.Red else Color.Red.copy(alpha = 0.5f),
                            label = "recording_dot"
                        )
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(recordingDot)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (looperState == LooperState.PRE_COUNT) {
                                "Count-in: ${currentBeat + 1}/4"
                            } else {
                                "Recording Track ${activeRecordingTrack + 1} - Bar ${currentBar + 1}/4"
                            },
                            style = SynthTypography.label.copy(color = accentColor),
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else if (looperState == LooperState.PLAYING) {
                    Text(
                        text = "Playing - Bar ${currentBar + 1}/4, Beat ${currentBeat + 1}/4",
                        style = SynthTypography.label.copy(color = secondaryTextColor),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Track rows
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    tracks.forEachIndexed { index, track ->
                        LooperTrackRow(
                            trackIndex = index,
                            track = track,
                            isRecording = isRecording && activeRecordingTrack == index,
                            isOtherTrackRecording = isRecording && activeRecordingTrack != index,
                            canRecord = !isRecording && !track.hasContent && !allTracksFull,
                            onStartRecording = { onStartRecordingTrack(index) },
                            onVolumeChange = { volume -> onTrackVolumeChange(index, volume) },
                            onToggleMute = { onToggleMute(index) },
                            onToggleSolo = { onToggleSolo(index) },
                            onDelete = { onDeleteTrack(index) },
                            isDarkMode = isDarkMode
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Bottom controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Delete All button
                    if (hasAnyContent && !isRecording) {
                        TextButton(
                            onClick = onDeleteAll,
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = Color.Red.copy(alpha = 0.8f)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete All")
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }
                    
                    // Play/Stop button
                    if (hasAnyContent && !isRecording) {
                        Button(
                            onClick = onPlayStop,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentColor
                            ),
                            modifier = Modifier.height(44.dp)
                        ) {
                            Text(
                                text = if (looperState == LooperState.PLAYING) "Stop" else "Play",
                                style = SynthTypography.label.copy(color = textColor)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LooperTrackRow(
    trackIndex: Int,
    track: LoopTrackState,
    isRecording: Boolean,
    isOtherTrackRecording: Boolean,
    canRecord: Boolean,
    onStartRecording: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onToggleMute: () -> Unit,
    onToggleSolo: () -> Unit,
    onDelete: () -> Unit,
    isDarkMode: Boolean
) {
    val cardColor = if (isDarkMode) DarkSurfaceCard else BackgroundLight
    val textColor = if (isDarkMode) DarkTextPrimary else TextPrimary
    val secondaryTextColor = if (isDarkMode) DarkTextSecondary else TextSecondary
    val accentColor = if (isDarkMode) DarkPastelPink else PastelPink
    
    // Track colors for visual distinction
    val trackColors = listOf(
        if (isDarkMode) DarkPastelPeach else PastelPeach,
        if (isDarkMode) DarkPastelBlue else PastelBlue,
        if (isDarkMode) DarkPastelMint else PastelMint,
        if (isDarkMode) DarkPastelLavender else PastelLavender
    )
    val trackColor = trackColors.getOrElse(trackIndex) { accentColor }
    
    val borderColor = if (isRecording) Color.Red else trackColor.copy(alpha = 0.5f)
    val borderWidth = if (isRecording) 2.dp else 1.dp
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(borderWidth, borderColor, RoundedCornerShape(12.dp))
            .background(cardColor)
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Track number indicator
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(trackColor.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${trackIndex + 1}",
                    style = SynthTypography.label.copy(
                        color = textColor,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            if (track.hasContent) {
                // Track with content
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Track ${trackIndex + 1}",
                        style = SynthTypography.label.copy(color = textColor)
                    )
                    
                    // Volume slider
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Slider(
                            value = track.volume,
                            onValueChange = onVolumeChange,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = trackColor,
                                activeTrackColor = trackColor
                            ),
                            enabled = !isOtherTrackRecording
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Mute button
                MuteButton(
                    isMuted = track.isMuted,
                    onClick = onToggleMute,
                    isDarkMode = isDarkMode,
                    enabled = !isOtherTrackRecording
                )
                
                Spacer(modifier = Modifier.width(4.dp))
                
                // Solo button
                SoloButton(
                    isSolo = track.isSolo,
                    onClick = onToggleSolo,
                    isDarkMode = isDarkMode,
                    enabled = !isOtherTrackRecording
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Delete button
                if (!isOtherTrackRecording) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Delete track",
                            tint = Color.Red.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                
            } else if (isRecording) {
                // This track is currently recording
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    val pulseAlpha by animateFloatAsState(
                        targetValue = 1f,
                        label = "pulse"
                    )
                    // Recording indicator dot
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color.Red.copy(alpha = pulseAlpha))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Recording...",
                        style = SynthTypography.label.copy(color = Color.Red)
                    )
                }
                
            } else {
                // Empty track slot
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .then(
                            if (canRecord && !isOtherTrackRecording) {
                                Modifier.clickable(onClick = onStartRecording)
                            } else {
                                Modifier
                            }
                        )
                        .background(
                            if (canRecord && !isOtherTrackRecording) {
                                trackColor.copy(alpha = 0.15f)
                            } else {
                                Color.Transparent
                            }
                        )
                        .padding(vertical = 8.dp, horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (canRecord && !isOtherTrackRecording) {
                            "Tap to Record"
                        } else if (isOtherTrackRecording) {
                            "Recording on another track..."
                        } else {
                            "Empty"
                        },
                        style = SynthTypography.label.copy(
                            color = if (canRecord && !isOtherTrackRecording) trackColor else secondaryTextColor
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun MuteButton(
    isMuted: Boolean,
    onClick: () -> Unit,
    isDarkMode: Boolean,
    enabled: Boolean
) {
    val bgColor by animateColorAsState(
        targetValue = if (isMuted) Color.Red.copy(alpha = 0.8f) else Color.Gray.copy(alpha = 0.3f),
        label = "mute_bg"
    )
    val textColor = if (isMuted) Color.White else if (isDarkMode) DarkTextSecondary else TextSecondary
    
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "M",
            style = SynthTypography.label.copy(
                color = textColor,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        )
    }
}

@Composable
private fun SoloButton(
    isSolo: Boolean,
    onClick: () -> Unit,
    isDarkMode: Boolean,
    enabled: Boolean
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSolo) {
            if (isDarkMode) DarkPastelMint else PastelMint
        } else {
            Color.Gray.copy(alpha = 0.3f)
        },
        label = "solo_bg"
    )
    val textColor = if (isSolo) {
        if (isDarkMode) DarkTextPrimary else TextPrimary
    } else {
        if (isDarkMode) DarkTextSecondary else TextSecondary
    }
    
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "S",
            style = SynthTypography.label.copy(
                color = textColor,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        )
    }
}

/**
 * Confirmation dialog for deleting a single track
 */
@Composable
fun DeleteTrackDialog(
    trackIndex: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isDarkMode: Boolean
) {
    val backgroundColor = if (isDarkMode) DarkSurface else SurfaceWhite
    val textColor = if (isDarkMode) DarkTextPrimary else TextPrimary
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = backgroundColor,
        title = {
            Text(
                text = "Delete Track ${trackIndex + 1}?",
                color = textColor
            )
        },
        text = {
            Text(
                text = "This will permanently delete the recorded audio on Track ${trackIndex + 1}.",
                color = textColor.copy(alpha = 0.8f)
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = textColor)
            }
        }
    )
}

/**
 * Confirmation dialog for deleting all tracks
 */
@Composable
fun DeleteAllTracksDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isDarkMode: Boolean
) {
    val backgroundColor = if (isDarkMode) DarkSurface else SurfaceWhite
    val textColor = if (isDarkMode) DarkTextPrimary else TextPrimary
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = backgroundColor,
        title = {
            Text(
                text = "Delete All Tracks?",
                color = textColor
            )
        },
        text = {
            Text(
                text = "This will permanently delete all recorded loops. This action cannot be undone.",
                color = textColor.copy(alpha = 0.8f)
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
            ) {
                Text("Delete All")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = textColor)
            }
        }
    )
}
