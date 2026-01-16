package com.synthio.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.synthio.app.audio.ExportJob
import com.synthio.app.audio.ExportStatus
import com.synthio.app.ui.components.ExportProgressIndicator
import com.synthio.app.ui.components.ExportSpinner
import com.synthio.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Full exports page showing all export jobs and their status
 */
@Composable
fun ExportsPage(
    exportJobs: List<ExportJob>,
    onCancelJob: (String) -> Unit,
    onRemoveJob: (String) -> Unit,
    onDownloadJob: (ExportJob) -> Unit,
    onClearCompleted: () -> Unit,
    onClose: () -> Unit,
    isDarkMode: Boolean,
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
    val accentColor = if (isDarkMode) DarkPastelMint else PastelMint
    
    val hasCompleted = exportJobs.any { 
        it.status == ExportStatus.COMPLETE || it.status == ExportStatus.FAILED 
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(20.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Exports",
                style = SynthTypography.heading.copy(
                    color = textColor,
                    fontSize = 28.sp
                )
            )
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (hasCompleted) {
                    TextButton(onClick = onClearCompleted) {
                        Text(
                            text = "Clear Completed",
                            style = SynthTypography.smallLabel.copy(color = secondaryTextColor)
                        )
                    }
                }
                
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(40.dp)
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
        
        if (exportJobs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No exports yet",
                        style = SynthTypography.subheading.copy(color = textColor)
                    )
                    Text(
                        text = "Export your loops from the Looper modal",
                        style = SynthTypography.smallLabel.copy(color = secondaryTextColor),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(exportJobs, key = { it.id }) { job ->
                    ExportJobCard(
                        job = job,
                        onCancel = { onCancelJob(job.id) },
                        onRemove = { onRemoveJob(job.id) },
                        onDownload = { onDownloadJob(job) },
                        isDarkMode = isDarkMode
                    )
                }
            }
        }
    }
}

@Composable
private fun ExportJobCard(
    job: ExportJob,
    onCancel: () -> Unit,
    onRemove: () -> Unit,
    onDownload: () -> Unit,
    isDarkMode: Boolean
) {
    val cardColor = if (isDarkMode) DarkSurfaceCard else SurfaceWhite
    val textColor = if (isDarkMode) DarkTextPrimary else TextPrimary
    val secondaryTextColor = if (isDarkMode) DarkTextSecondary else TextSecondary
    val accentColor = if (isDarkMode) DarkPastelMint else PastelMint
    
    val isActive = job.status == ExportStatus.PENDING || 
                   job.status == ExportStatus.MIXING || 
                   job.status == ExportStatus.ENCODING
    
    val isClickable = job.status == ExportStatus.COMPLETE && job.outputFilePath != null
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cardColor)
            .then(
                if (isClickable) {
                    Modifier.clickable { onDownload() }
                } else {
                    Modifier
                }
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status indicator / Progress
            Box(
                modifier = Modifier.size(56.dp),
                contentAlignment = Alignment.Center
            ) {
                when (job.status) {
                    ExportStatus.PENDING -> {
                        ExportSpinner(isDarkMode = isDarkMode, size = 40)
                    }
                    ExportStatus.MIXING, ExportStatus.ENCODING -> {
                        ExportProgressIndicator(
                            progress = job.progress,
                            isDarkMode = isDarkMode,
                            modifier = Modifier.size(56.dp)
                        )
                    }
                    ExportStatus.COMPLETE -> {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(accentColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Complete",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    ExportStatus.FAILED -> {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.Red.copy(alpha = 0.8f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Failed",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Job details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = job.filename,
                    style = SynthTypography.label.copy(
                        color = textColor,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Quality badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(accentColor.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = job.getQualityLabel(),
                            style = SynthTypography.smallLabel.copy(
                                color = accentColor,
                                fontSize = 10.sp
                            )
                        )
                    }
                    
                    Text(
                        text = "•",
                        style = SynthTypography.smallLabel.copy(color = secondaryTextColor)
                    )
                    
                    Text(
                        text = job.getTrackDescription(),
                        style = SynthTypography.smallLabel.copy(color = secondaryTextColor)
                    )
                    
                    if (job.includeDrums) {
                        Text(
                            text = "+ Drums",
                            style = SynthTypography.smallLabel.copy(color = secondaryTextColor)
                        )
                    }
                }
                
                // Status text
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when (job.status) {
                        ExportStatus.PENDING -> "Waiting..."
                        ExportStatus.MIXING -> "Mixing..."
                        ExportStatus.ENCODING -> "Encoding..."
                        ExportStatus.COMPLETE -> "Tap to Download"
                        ExportStatus.FAILED -> job.errorMessage ?: "Export failed"
                    },
                    style = SynthTypography.smallLabel.copy(
                        color = when (job.status) {
                            ExportStatus.FAILED -> Color.Red.copy(alpha = 0.8f)
                            ExportStatus.COMPLETE -> accentColor
                            else -> secondaryTextColor
                        }
                    )
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Actions
            if (isActive) {
                IconButton(
                    onClick = onCancel,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel",
                        tint = Color.Red.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else if (job.status == ExportStatus.COMPLETE) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onDownload,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download",
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove",
                            tint = secondaryTextColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            } else {
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove",
                        tint = secondaryTextColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
