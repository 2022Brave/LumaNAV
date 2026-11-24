package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.GlassWhite
import com.example.ui.theme.LocalAuvionyxPalette
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.TextSecondary

@Composable
fun VelocityACompactWordmark(
    modifier: Modifier = Modifier,
    showIcon: Boolean = true
) {
    val accent = LocalAuvionyxPalette.current.accentPrimary
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showIcon) {
            LumaNavLogo(
                contentDescription = stringResource(id = R.string.cd_velocity_a_logo),
                modifier = Modifier.size(30.dp).clip(RoundedCornerShape(8.dp))
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = GlassWhite)) { append("Luma") }
                withStyle(SpanStyle(color = accent)) { append("NAV") }
            },
            fontFamily = SpaceGroteskFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            letterSpacing = 2.4.sp
        )
    }
}

@Composable
fun VelocityAHeroLockup(
    logoSize: Dp = 156.dp,
    modifier: Modifier = Modifier
) {
    val accent = LocalAuvionyxPalette.current.accentPrimary
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        LumaNavLogo(
            contentDescription = stringResource(id = R.string.cd_velocity_a_logo),
            modifier = Modifier
                .size(logoSize)
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(accent.copy(alpha = 0.28f), Color.Transparent),
                            radius = size.maxDimension * 0.65f
                        )
                    )
                }
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = GlassWhite)) { append("Luma") }
                withStyle(SpanStyle(color = ElectricCyan)) { append("Nav") }
            },
            fontFamily = SpaceGroteskFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            letterSpacing = 6.sp
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(id = R.string.brand_tagline),
            style = MaterialTheme.typography.bodyMedium.copy(letterSpacing = 2.sp),
            color = TextSecondary
        )
    }
}


@Composable
fun LumaNavLogo(
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val logoBitmap = remember(context) {
        val encoded = context.resources.openRawResource(R.raw.lumanav_logo_base64)
            .bufferedReader()
            .use { it.readText() }
        val bytes = Base64.decode(encoded, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size).asImageBitmap()
    }
    Image(
        bitmap = logoBitmap,
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier = modifier
    )
}
