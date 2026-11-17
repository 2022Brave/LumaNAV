package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.DeepBlue
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.GlassWhite
import com.example.ui.theme.IceBlue
import com.example.ui.theme.ObsidianBlack

/**
 * Custom synthwave automotive artwork for Lamentis 2077 and companion queue tracks.
 * Uses strictly the official AUVIONYX palette: Obsidian Black, Deep Blue, Ice Blue, Electric Cyan, Glass White.
 */
@Composable
fun AlbumArtworkArt(
    artVariant: Int,
    accentColor: Color,
    cornerRadius: Dp = 16.dp,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .clip(shape)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        GlassWhite.copy(alpha = 0.45f),
                        ElectricCyan.copy(alpha = 0.35f),
                        DeepBlue.copy(alpha = 0.25f)
                    )
                ),
                shape = shape
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val skyColors = when (artVariant) {
                0 -> listOf(ObsidianBlack, Color(0xFF071B3D), DeepBlue, IceBlue.copy(alpha = 0.65f))
                1 -> listOf(ObsidianBlack, Color(0xFF04142B), DeepBlue, ElectricCyan.copy(alpha = 0.7f))
                2 -> listOf(ObsidianBlack, Color(0xFF06162E), Color(0xFF0E3C82), IceBlue)
                else -> listOf(ObsidianBlack, Color(0xFF051124), DeepBlue, accentColor.copy(alpha = 0.75f))
            }

            // Sky gradient
            drawRect(brush = Brush.verticalGradient(skyColors))

            // Horizon Sun (Electric Cyan / Ice Blue / Glass White)
            val sunCenter = Offset(w * 0.52f, h * 0.45f)
            val sunRadius = minOf(w, h) * 0.27f
            drawCircle(
                brush = Brush.verticalGradient(
                    colors = listOf(GlassWhite, IceBlue, ElectricCyan, DeepBlue),
                    startY = sunCenter.y - sunRadius,
                    endY = sunCenter.y + sunRadius
                ),
                radius = sunRadius,
                center = sunCenter
            )

            // Crisp scanline horizon cuts
            for (i in 1..4) {
                val barY = sunCenter.y + sunRadius * (0.18f * i)
                if (barY < sunCenter.y + sunRadius) {
                    drawRect(
                        color = skyColors[1],
                        topLeft = Offset(sunCenter.x - sunRadius, barY),
                        size = Size(sunRadius * 2, h * 0.025f)
                    )
                }
            }

            // Mountain silhouette ridge
            val horizonY = h * 0.58f
            val mountainPath = Path().apply {
                moveTo(0f, horizonY)
                lineTo(w * 0.16f, h * 0.43f)
                lineTo(w * 0.34f, h * 0.53f)
                lineTo(w * 0.68f, h * 0.51f)
                lineTo(w * 0.84f, h * 0.41f)
                lineTo(w, horizonY)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(mountainPath, color = Color(0xFF020712))

            // Obsidian highway floor + cyan perspective grid
            drawRect(
                color = ObsidianBlack,
                topLeft = Offset(0f, horizonY),
                size = Size(w, h - horizonY)
            )
            val gridColor = if (artVariant == 0) ElectricCyan else IceBlue
            for (i in 0..5) {
                val y = horizonY + (h - horizonY) * (i * i / 25f)
                drawLine(
                    color = gridColor.copy(alpha = 0.50f),
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1.2.dp.toPx()
                )
            }
            for (i in -3..3) {
                drawLine(
                    color = gridColor.copy(alpha = 0.42f),
                    start = Offset(w * 0.5f + i * w * 0.06f, horizonY),
                    end = Offset(w * 0.5f + i * w * 0.32f, h),
                    strokeWidth = 1.2.dp.toPx()
                )
            }

            // Dual Electric Cyan & Ice Blue light trails
            drawLine(
                color = ElectricCyan,
                start = Offset(w * 0.44f, h * 0.65f),
                end = Offset(w * 0.35f, h * 0.94f),
                strokeWidth = 2.2.dp.toPx()
            )
            drawLine(
                color = IceBlue,
                start = Offset(w * 0.56f, h * 0.65f),
                end = Offset(w * 0.65f, h * 0.94f),
                strokeWidth = 2.2.dp.toPx()
            )

            // Futuristic skyline silhouettes
            drawRect(
                color = Color(0xFF040D1C),
                topLeft = Offset(w * 0.08f, horizonY - h * 0.12f),
                size = Size(w * 0.07f, h * 0.12f)
            )
            drawRect(
                color = Color(0xFF040D1C),
                topLeft = Offset(w * 0.17f, horizonY - h * 0.17f),
                size = Size(w * 0.06f, h * 0.17f)
            )
            drawRect(
                color = ElectricCyan.copy(alpha = 0.45f),
                topLeft = Offset(w * 0.18f, horizonY - h * 0.15f),
                size = Size(w * 0.04f, h * 0.02f)
            )

            // Subtle specular glass sheen overlay across artwork
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        GlassWhite.copy(alpha = 0.16f),
                        Color.Transparent,
                        Color.Transparent
                    ),
                    start = Offset.Zero,
                    end = Offset(w * 0.65f, h * 0.65f)
                )
            )
            drawCircle(
                color = ElectricCyan.copy(alpha = 0.22f),
                radius = w * 0.46f,
                center = Offset(w * 0.5f, h * 0.5f),
                style = Stroke(width = 1.dp.toPx())
            )
        }
    }
}
