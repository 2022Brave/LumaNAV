package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.DeepBlue
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.IceBlue
import com.example.ui.theme.LocalAuvionyxMotion
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun BootSplashScreen(
    onBootComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val reducedMotion = LocalAuvionyxMotion.current.reducedMotion
    val wakeProgress = remember { Animatable(0f) }
    val lockupAlpha = remember { Animatable(0f) }

    LaunchedEffect(reducedMotion) {
        if (reducedMotion) {
            wakeProgress.snapTo(1f)
            lockupAlpha.animateTo(1f, tween(350))
            delay(500L)
            onBootComplete()
        } else {
            wakeProgress.animateTo(1f, tween(1100, easing = FastOutSlowInEasing))
            lockupAlpha.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
            delay(750L)
            onBootComplete()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .systemBarsPadding()
            .padding(24.dp)
    ) {
        // Wake / Arc-build cyan light streak behind lockup
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .align(Alignment.Center)
        ) {
            val w = size.width
            val h = size.height
            val progress = wakeProgress.value
            val maxSteps = (48 * progress).toInt().coerceAtLeast(2)
            val path = Path()
            for (i in 0..maxSteps) {
                val t = i / 48f
                val x = w * t
                val wave = sin(t * PI * 2.2f).toFloat() * (h * 0.24f) * sin(t * PI).toFloat()
                val y = h * 0.55f - wave
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(
                path = path,
                brush = Brush.horizontalGradient(listOf(Color.Transparent, DeepBlue, ElectricCyan, IceBlue)),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
            if (progress > 0.1f && progress < 0.98f) {
                val tipX = w * progress
                val tipY = h * 0.55f - sin(progress * PI * 2.2f).toFloat() * (h * 0.24f) * sin(progress * PI).toFloat()
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(ElectricCyan, Color.Transparent),
                        center = Offset(tipX, tipY),
                        radius = 28.dp.toPx()
                    ),
                    radius = 28.dp.toPx(),
                    center = Offset(tipX, tipY)
                )
            }
        }

        // Official Velocity A Hero Lockup
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .graphicsLayer {
                    alpha = lockupAlpha.value
                    scaleX = 0.92f + 0.08f * lockupAlpha.value
                    scaleY = 0.92f + 0.08f * lockupAlpha.value
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.auvionyx_launcher_fg),
                contentDescription = stringResource(id = R.string.cd_velocity_a_logo),
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(280.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "LumaNav",
                style = MaterialTheme.typography.headlineLarge,
                color = ElectricCyan
            )
        }

        // Bottom status & skip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.boot_powering_up),
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary
            )
            TextButton(
                onClick = onBootComplete,
                modifier = Modifier.testTag("skip_boot_button")
            ) {
                Text(
                    text = stringResource(id = R.string.skip_boot),
                    style = MaterialTheme.typography.labelLarge,
                    color = ElectricCyan
                )
            }
        }
    }
}
