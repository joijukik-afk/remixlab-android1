package com.joijukik.remixlab.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.sin

@Composable
fun AudioVisualizer(
    amplitudes: List<Float>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxWidth().height(90.dp)) {
        if (amplitudes.isEmpty()) return@Canvas
        val barCount = amplitudes.size
        val gap = 2.dp.toPx()
        val barWidth = (size.width - gap * (barCount - 1)) / barCount
        val gradient = Brush.verticalGradient(
            colors = listOf(Color(0xFF00E5FF), Color(0xFF7C4DFF))
        )
        amplitudes.forEachIndexed { i, raw ->
            val amp = raw.coerceIn(0f, 1f)
            val barHeight = size.height * (0.15f + amp * 0.85f)
            drawRoundRect(
                brush = gradient,
                topLeft = Offset(
                    x = i * (barWidth + gap),
                    y = (size.height - barHeight) / 2f
                ),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f)
            )
        }
    }
}

fun fakeAmplitudes(positionMs: Long, bars: Int = 48): List<Float> {
    val t = positionMs / 250.0
    return List(bars) { i ->
        val v = sin(t + i * 0.4) * 0.5 +
                sin(t * 1.7 + i * 0.9) * 0.3 +
                sin(t * 2.3 + i * 1.3) * 0.2
        abs(v).toFloat().coerceIn(0.05f, 1f)
    }
}
