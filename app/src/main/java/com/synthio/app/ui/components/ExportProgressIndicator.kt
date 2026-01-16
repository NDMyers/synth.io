package com.synthio.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import com.synthio.app.ui.theme.*
import kotlinx.coroutines.delay

/**
 * Animated circular progress indicator with rotating subtitles
 * for the export process.
 */
@Composable
fun ExportProgressIndicator(
    progress: Float,
    isDarkMode: Boolean,
    modifier: Modifier = Modifier
) {
    val accentColor = if (isDarkMode) DarkPastelMint else PastelMint
    val secondaryColor = if (isDarkMode) DarkPastelPink else PastelPink
    val textColor = if (isDarkMode) DarkTextSecondary else TextSecondary
    
    // Rotating subtitles
    val subtitles = listOf("mixing magic...", "exporting...")
    var currentSubtitleIndex by remember { mutableIntStateOf(0) }
    
    // Change subtitle every 7 seconds
    LaunchedEffect(Unit) {
        while (true) {
            delay(7000)
            currentSubtitleIndex = (currentSubtitleIndex + 1) % subtitles.size
        }
    }
    
    // Animated rotation for the progress arc
    val infiniteTransition = rememberInfiniteTransition(label = "progress_rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    // Pulsing glow effect
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )
    
    // Subtitle fade animation
    val subtitleAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 500),
        label = "subtitle_alpha"
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(80.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 6.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2
                val center = Offset(size.width / 2, size.height / 2)
                
                // Background circle
                drawCircle(
                    color = accentColor.copy(alpha = 0.2f),
                    radius = radius,
                    center = center,
                    style = Stroke(width = strokeWidth)
                )
                
                // Glow effect
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = glowAlpha * 0.3f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = radius * 1.5f
                    ),
                    radius = radius * 1.3f,
                    center = center
                )
                
                // Progress arc (spinning)
                val sweepAngle = if (progress > 0) progress * 360f else 90f
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            secondaryColor,
                            accentColor,
                            secondaryColor.copy(alpha = 0.5f)
                        )
                    ),
                    startAngle = rotation - 90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
            
            // Center percentage (if progress is known)
            if (progress > 0) {
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = SynthTypography.label.copy(color = accentColor)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Rotating subtitle
        Text(
            text = subtitles[currentSubtitleIndex],
            style = SynthTypography.smallLabel.copy(
                color = textColor.copy(alpha = subtitleAlpha)
            ),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Simple spinning loader for pending state
 */
@Composable
fun ExportSpinner(
    isDarkMode: Boolean,
    size: Int = 24,
    modifier: Modifier = Modifier
) {
    val accentColor = if (isDarkMode) DarkPastelMint else PastelMint
    
    val infiniteTransition = rememberInfiniteTransition(label = "spinner")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spinner_rotation"
    )
    
    Canvas(modifier = modifier.size(size.dp)) {
        val strokeWidth = 3.dp.toPx()
        val radius = (this.size.minDimension - strokeWidth) / 2
        val center = Offset(this.size.width / 2, this.size.height / 2)
        
        // Background arc
        drawArc(
            color = accentColor.copy(alpha = 0.2f),
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = strokeWidth)
        )
        
        // Spinning arc
        drawArc(
            color = accentColor,
            startAngle = rotation,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}
