package com.synthio.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.synthio.app.ui.theme.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun Knob(
    value: Float,
    onValueChange: (Float) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    minValue: Float = 0f,
    maxValue: Float = 1f,
    color: Color = PastelPink,
    size: Dp = 56.dp  // Smaller default size for better fitting
) {
    var currentAngle by remember { mutableFloatStateOf(valueToAngle(value, minValue, maxValue)) }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .widthIn(min = 64.dp, max = 80.dp)  // Constrain width for equal spacing
            .padding(4.dp)
    ) {
        // Knob
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(size)
                .shadow(6.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            SurfaceWhite,
                            BackgroundLight,
                            color.copy(alpha = 0.2f)
                        )
                    )
                )
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        
                        // Calculate angle change based on drag
                        val sensitivity = 0.01f
                        val deltaAngle = -dragAmount.y * sensitivity
                        currentAngle = (currentAngle + deltaAngle).coerceIn(-0.75f * PI.toFloat(), 0.75f * PI.toFloat())
                        
                        val newValue = angleToValue(currentAngle, minValue, maxValue)
                        onValueChange(newValue)
                    }
                }
        ) {
            // Value arc
            Canvas(modifier = Modifier.size(size - 8.dp)) {
                val sweep = ((value - minValue) / (maxValue - minValue)) * 270f
                
                // Background arc
                drawArc(
                    color = color.copy(alpha = 0.2f),
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                )
                
                // Value arc
                drawArc(
                    color = color,
                    startAngle = 135f,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            
            // Indicator dot
            Canvas(modifier = Modifier.size(size - 16.dp)) {
                val angleRad = (135f + ((value - minValue) / (maxValue - minValue)) * 270f) * (PI.toFloat() / 180f)
                val radius = this.size.minDimension / 2 - 4.dp.toPx()
                val indicatorX = center.x + radius * cos(angleRad)
                val indicatorY = center.y + radius * sin(angleRad)
                
                drawCircle(
                    color = color,
                    radius = 3.dp.toPx(),
                    center = Offset(indicatorX, indicatorY)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Label
        Text(
            text = label,
            style = SynthTypography.smallLabel,
            maxLines = 1
        )
        
        // Value display
        Text(
            text = String.format("%.0f%%", (value - minValue) / (maxValue - minValue) * 100),
            style = SynthTypography.smallLabel.copy(color = color),
            maxLines = 1
        )
    }
}

private fun valueToAngle(value: Float, minValue: Float, maxValue: Float): Float {
    val normalized = (value - minValue) / (maxValue - minValue)
    return (-0.75f + normalized * 1.5f) * PI.toFloat()
}

private fun angleToValue(angle: Float, minValue: Float, maxValue: Float): Float {
    val normalized = (angle / PI.toFloat() + 0.75f) / 1.5f
    return minValue + normalized.coerceIn(0f, 1f) * (maxValue - minValue)
}
