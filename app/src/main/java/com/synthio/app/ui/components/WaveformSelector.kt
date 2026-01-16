package com.synthio.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.synthio.app.audio.Waveform
import com.synthio.app.ui.theme.*
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun WaveformSelector(
    selectedWaveforms: List<Waveform>,
    onWaveformToggled: (Waveform) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(SynthShapes.large)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        PastelMintLight.copy(alpha = 0.5f),
                        PastelMint.copy(alpha = 0.5f)
                    )
                )
            )
            .padding(16.dp)
    ) {
        Waveform.entries.forEach { waveform ->
            WaveformButton(
                waveform = waveform,
                isSelected = selectedWaveforms.contains(waveform),
                onClick = { onWaveformToggled(waveform) }
            )
        }
    }
}

@Composable
private fun WaveformButton(
    waveform: Waveform,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) PastelPink else SurfaceWhite,
        label = "waveformBgColor"
    )
    
    val shadowElevation by animateDpAsState(
        targetValue = if (isSelected) 8.dp else 4.dp,
        label = "waveformShadow"
    )
    
    val waveColor by animateColorAsState(
        targetValue = if (isSelected) TextOnDark else PastelPinkDark,
        label = "waveColor"
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(56.dp)
                .shadow(shadowElevation, SynthShapes.medium)
                .clip(SynthShapes.medium)
                .background(backgroundColor)
                .clickable { onClick() }
        ) {
            // Draw waveform icon
            Canvas(modifier = Modifier.size(36.dp)) {
                val path = Path()
                val width = size.width
                val height = size.height
                val centerY = height / 2
                
                when (waveform) {
                    Waveform.SINE -> {
                        path.moveTo(0f, centerY)
                        for (x in 0..width.toInt()) {
                            val y = centerY - (height * 0.35f * sin(x / width * 2 * PI.toFloat()))
                            path.lineTo(x.toFloat(), y)
                        }
                    }
                    Waveform.SQUARE -> {
                        path.moveTo(0f, centerY - height * 0.3f)
                        path.lineTo(width * 0.5f, centerY - height * 0.3f)
                        path.lineTo(width * 0.5f, centerY + height * 0.3f)
                        path.lineTo(width, centerY + height * 0.3f)
                    }
                    Waveform.SAWTOOTH -> {
                        path.moveTo(0f, centerY + height * 0.3f)
                        path.lineTo(width * 0.5f, centerY - height * 0.3f)
                        path.lineTo(width * 0.5f, centerY + height * 0.3f)
                        path.lineTo(width, centerY - height * 0.3f)
                    }
                    Waveform.TRIANGLE -> {
                        path.moveTo(0f, centerY)
                        path.lineTo(width * 0.25f, centerY - height * 0.35f)
                        path.lineTo(width * 0.5f, centerY)
                        path.lineTo(width * 0.75f, centerY + height * 0.35f)
                        path.lineTo(width, centerY)
                    }
                }
                
                drawPath(
                    path = path,
                    color = waveColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = waveform.name.lowercase().replaceFirstChar { it.uppercase() },
            style = SynthTypography.smallLabel.copy(
                color = if (isSelected) PastelPinkDark else TextSecondary
            )
        )
    }
}
