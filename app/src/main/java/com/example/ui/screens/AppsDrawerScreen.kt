package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material.icons.outlined.Nightlight
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.AuvionyxModule
import com.example.ui.components.GlassCard
import com.example.ui.theme.GlassWhite
import com.example.ui.theme.LocalAuvionyxPalette
import com.example.ui.theme.TextSecondary

private data class AppLauncherEntry(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val module: AuvionyxModule?,
    val testTag: String
)

@Composable
fun AppsDrawerScreen(
    isLandscapeWide: Boolean,
    onSelectModule: (AuvionyxModule) -> Unit,
    onOpenPhoneDialer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = LocalAuvionyxPalette.current.accentPrimary
    val entries = listOf(
        AppLauncherEntry("LumaNAV Drive", "Split Cockpit", Icons.Outlined.Home, AuvionyxModule.DRIVE, "app_drive"),
        AppLauncherEntry("LumaNAV Maps", "Navigation & Routes", Icons.Outlined.Navigation, AuvionyxModule.MAPS, "app_maps"),
        AppLauncherEntry("LumaNAV Music", "Unified Media Hub", Icons.Outlined.MusicNote, AuvionyxModule.MUSIC, "app_music"),
        AppLauncherEntry("LumaNAV HUD", "Heads-Up & Designer", Icons.Outlined.Speed, AuvionyxModule.HUD, "app_hud"),
        AppLauncherEntry("LumaNAV AOD", "Ambient Display", Icons.Outlined.Nightlight, AuvionyxModule.AOD, "app_aod"),
        AppLauncherEntry("LumaNAV Settings", "System & Designer", Icons.Outlined.Settings, AuvionyxModule.SETTINGS, "app_settings"),
        AppLauncherEntry("Phone Dialer", "Android System Call", Icons.Outlined.Phone, null, "app_phone")
    )

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("LumaNAV Cockpit", style = MaterialTheme.typography.headlineMedium, color = GlassWhite)
        LazyVerticalGrid(
            columns = GridCells.Fixed(if (isLandscapeWide) 5 else 2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 28.dp)
        ) {
            items(entries, key = { it.testTag }) { item ->
                GlassCard(
                    modifier = Modifier
                        .testTag(item.testTag)
                        .fillMaxWidth()
                        .height(126.dp)
                        .clickable {
                            if (item.module != null) onSelectModule(item.module) else onOpenPhoneDialer()
                        },
                    cornerRadius = 22.dp,
                    highlighted = false,
                    contentPadding = PaddingValues(14.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.title,
                            tint = accent,
                            modifier = Modifier.size(28.dp)
                        )
                        Column {
                            Text(item.title, style = MaterialTheme.typography.titleMedium, color = GlassWhite)
                            Spacer(Modifier.height(2.dp))
                            Text(item.subtitle, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        }
                    }
                }
            }
        }
    }
}
