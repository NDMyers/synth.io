package com.synthio.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.synthio.app.audio.Waveform
import com.synthio.app.ui.theme.*
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

@Composable
fun WaveformDisplay(
    waveform: Waveform,
    modifier: Modifier = Modifier
) {
    // Animate the wave phase for visual effect
    val infiniteTransition = rememberInfiniteTransition(label = "waveAnimation")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
            .clip(SynthShapes.large)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        BackgroundLight,
                        PastelPinkLight.copy(alpha = 0.3f)
                    )
                )
            )
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            val width = size.width
            val height = size.height
            val centerY = height / 2
            val amplitude = height * 0.35f
            
            val path = Path()
            path.moveTo(0f, centerY)
            
            for (x in 0..width.toInt()) {
                val normalizedX = x / width
                val waveValue = when (waveform) {
                    Waveform.SINE -> {
                        sin(normalizedX * 4 * PI.toFloat() + phase)
                    }
                    Waveform.SQUARE -> {
                        val sineVal = sin(normalizedX * 4 * PI.toFloat() + phase)
                        if (sineVal >= 0) 1f else -1f
                    }
                    Waveform.SAWTOOTH -> {
                        val p = (normalizedX * 2 + phase / PI.toFloat()) % 1f
                        2f * p - 1f
                    }
                    Waveform.TRIANGLE -> {
                        val p = (normalizedX * 2 + phase / PI.toFloat()) % 1f
                        4f * abs(p - 0.5f) - 1f
                    }
                }
                
                val y = centerY - amplitude * waveValue
                path.lineTo(x.toFloat(), y)
            }
            
            // Draw glow effect
            drawPath(
                path = path,
                color = PastelPink.copy(alpha = 0.3f),
                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
            )
            
            // Draw main wave
            drawPath(
                path = path,
                color = PastelPink,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
            
            // Draw center line
            drawLine(
                color = PastelLavender.copy(alpha = 0.5f),
                start = Offset(0f, centerY),
                end = Offset(width, centerY),
                strokeWidth = 1.dp.toPx()
            )
        }
    }
}
