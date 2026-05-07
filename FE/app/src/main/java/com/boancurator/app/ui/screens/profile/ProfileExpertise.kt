package com.boancurator.app.ui.screens.profile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.boancurator.app.ui.theme.Cyan
import com.boancurator.app.ui.theme.DarkCard
import com.boancurator.app.ui.theme.DarkSurface
import com.boancurator.app.ui.theme.TextMuted
import com.boancurator.app.ui.theme.TextPrimary
import com.boancurator.app.ui.theme.TextSecondary
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
internal fun StatCard(label: String, value: String, icon: ImageVector, modifier: Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = DarkCard)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, null, tint = Cyan, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(8.dp))
            Text(value, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(label, color = TextMuted, fontSize = 12.sp)
        }
    }
}

@Composable
internal fun ExpertiseRadar(expertise: Map<String, Double>, modifier: Modifier) {
    val entries = expertise.entries.toList()
    val maxVal = 5.0 // 5점 만점 고정
    val cyanColor = Cyan

    Canvas(modifier = modifier) {
        val cx = size.width / 2
        val cy = size.height / 2
        val radius = min(cx, cy) * 0.55f
        val n = entries.size
        if (n < 3) return@Canvas

        val angleStep = 2 * PI / n
        val startAngle = -PI / 2

        // Grid lines
        for (ring in 1..4) {
            val r = radius * ring / 4
            val path = Path()
            for (i in 0..n) {
                val angle = startAngle + angleStep * (i % n)
                val x = cx + r * cos(angle).toFloat()
                val y = cy + r * sin(angle).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.close()
            drawPath(path, cyanColor.copy(alpha = 0.08f), style = Stroke(width = 1f))
        }

        // Axis lines
        for (i in 0 until n) {
            val angle = startAngle + angleStep * i
            drawLine(
                cyanColor.copy(alpha = 0.1f),
                Offset(cx, cy),
                Offset(cx + radius * cos(angle).toFloat(), cy + radius * sin(angle).toFloat()),
                strokeWidth = 1f
            )
        }

        // Data polygon
        val dataPath = Path()
        for (i in entries.indices) {
            val ratio = (entries[i].value / maxVal).toFloat().coerceIn(0f, 1f)
            val r = radius * ratio
            val angle = startAngle + angleStep * i
            val x = cx + r * cos(angle).toFloat()
            val y = cy + r * sin(angle).toFloat()
            if (i == 0) dataPath.moveTo(x, y) else dataPath.lineTo(x, y)
        }
        dataPath.close()
        drawPath(dataPath, cyanColor.copy(alpha = 0.15f))
        drawPath(dataPath, cyanColor.copy(alpha = 0.6f), style = Stroke(width = 2f))

        // Data points + labels
        for (i in entries.indices) {
            val ratio = (entries[i].value / maxVal).toFloat().coerceIn(0f, 1f)
            val r = radius * ratio
            val angle = startAngle + angleStep * i
            val x = cx + r * cos(angle).toFloat()
            val y = cy + r * sin(angle).toFloat()
            drawCircle(cyanColor, 4f, Offset(x, y))

            val labelR = radius + 32f
            val lx = cx + labelR * cos(angle).toFloat()
            val ly = cy + labelR * sin(angle).toFloat()
            val label = domainLabels[entries[i].key] ?: entries[i].key
            drawContext.canvas.nativeCanvas.drawText(
                label, lx, ly + 5f,
                android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#8B949E")
                    textSize = 24f
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
            )
        }
    }
}

@Composable
internal fun DomainBar(label: String, value: Double, maxValue: Double) {
    val ratio = (value / maxValue).toFloat().coerceIn(0f, 1f)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(110.dp))
        Box(
            modifier = Modifier.weight(1f).height(8.dp)
                .clip(RoundedCornerShape(4.dp)).background(DarkSurface)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(ratio).height(8.dp)
                    .clip(RoundedCornerShape(4.dp)).background(Cyan.copy(alpha = 0.7f))
            )
        }
        Spacer(Modifier.width(8.dp))
        Text("%.1f".format(value), color = TextMuted, fontSize = 11.sp, modifier = Modifier.width(30.dp))
    }
}
