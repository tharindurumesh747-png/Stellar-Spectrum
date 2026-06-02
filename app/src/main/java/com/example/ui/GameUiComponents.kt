package com.example.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.SoundSynth

// Utility helper to trigger haptic feedback vibrate Android devices
fun triggerVibration(context: Context, durationMs: Long = 30) {
    try {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(durationMs)
        }
    } catch (e: Exception) {
        // Fallback silently if device lacks vibrator hardware
    }
}

@Composable
fun NeonButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    buttonColor: Color = Color(0xFF00ADB5), // Cyan default
    hapticStrengthMs: Long = 25,
    vibrationEnabled: Boolean = true,
    testTag: String = ""
) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Smooth pulsing effect for beautiful neon glow
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseGlowVal by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "glow"
    )

    // Button click shrinkage scale
    val scaleVal by animateFloatAsState(
        targetValue = if (!enabled) 1.0f else if (isPressed) 0.95f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium), label = "button_scale"
    )

    val currentGlowAlpha = if (isPressed) 0.9f else pulseGlowVal * 0.45f
    val resolvedBorderColor = if (enabled) buttonColor else Color.Gray

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .testTag(testTag)
            .scale(scaleVal)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0A0C16).copy(alpha = 0.85f))
            .border(
                width = 2.dp,
                color = resolvedBorderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled
            ) {
                SoundSynth.playUiClick()
                if (vibrationEnabled) {
                    triggerVibration(context, hapticStrengthMs)
                }
                onClick()
            }
            .padding(vertical = 12.dp, horizontal = 24.dp)
    ) {
        // Glow layer drawn inside
        Canvas(modifier = Modifier.matchParentSize()) {
            if (enabled) {
                drawRoundRect(
                    color = buttonColor.copy(alpha = currentGlowAlpha),
                    style = Stroke(width = 6.dp.toPx() * pulseGlowVal),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx(), 12.dp.toPx())
                )
            }
        }
        Text(
            text = text,
            color = if (enabled) Color.White else Color.Gray,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif
        )
    }
}

@Composable
fun NeonCard(
    borderColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0E111F).copy(alpha = 0.82f))
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(listOf(borderColor, borderColor.copy(alpha = 0.15f))),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp),
        content = content
    )
}

@Composable
fun SynthwaveGridBackground(
    modifier: Modifier = Modifier,
    scrollSpeed: Float = 140f, // pixels per second
    gridColor: Color = Color(0xFFBD00FF).copy(alpha = 0.15f)
) {
    val transition = rememberInfiniteTransition(label = "grid_flow")
    val scrollOffset by transition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "scroll"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Draw parallax background gradient
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF05000A),
                    Color(0xFF0C0E1F)
                ),
                startY = 0f,
                endY = h
            )
        )

        // Draw classic synthwave vanishing point grid
        val center = Offset(w / 2f, h * 0.35f) // Vanishing point

        // Draw perspective lines (converging rays outwards)
        val numPerspectiveLines = 14
        for (i in 0..numPerspectiveLines) {
            val fraction = i.toFloat() / numPerspectiveLines
            val endX = w * (fraction * 2f - 0.5f) // expand projection
            drawLine(
                color = gridColor,
                start = center,
                end = Offset(endX, h),
                strokeWidth = 1.51dp.toPx()
            )
        }

        // Draw horizontal grid tiers that move down in exponential sizing
        val horizontalLinesCount = 9
        for (i in 0 until horizontalLinesCount) {
            // Apply exponential multiplier for real 3D depth spacing
            val baseFraction = (i.toFloat() + (scrollOffset / 100f)) / horizontalLinesCount
            val yFactor = Math.pow(baseFraction.toDouble(), 2.0).toFloat()
            val gridY = center.y + (h - center.y) * yFactor

            drawLine(
                color = gridColor.copy(alpha = baseFraction * 0.45f),
                start = Offset(0f, gridY),
                end = Offset(w, gridY),
                strokeWidth = (1f + yFactor * 2.5f).dp.toPx()
            )
        }
    }
}
