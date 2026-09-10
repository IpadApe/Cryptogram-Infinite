package dev.milan.cryptogram.ui.settings

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.milan.cryptogram.BuildConfig
import dev.milan.cryptogram.data.prefs.ThemeMode
import dev.milan.cryptogram.ui.components.AccentButton
import dev.milan.cryptogram.ui.components.BackHeader
import dev.milan.cryptogram.ui.components.OutlineButton
import dev.milan.cryptogram.ui.components.PaperDivider
import dev.milan.cryptogram.ui.theme.CryptoTheme
import dev.milan.cryptogram.ui.theme.Mono
import dev.milan.cryptogram.ui.theme.Serif

private const val PRIVACY_URL = "https://ipadape.github.io/Cryptogram-Infinite/privacy.html"

@Composable
fun SettingsScreen(
    onOpenSources: () -> Unit,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory()),
) {
    val c = CryptoTheme.colors
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(
        modifier
            .fillMaxSize()
            .background(c.paper)
            .verticalScroll(rememberScrollState())
            .padding(top = 56.dp, start = 26.dp, end = 26.dp, bottom = 40.dp),
    ) {
        BackHeader("Settings", onBack)
        Spacer(Modifier.height(24.dp))

        ToggleRow("Sound", state.soundEnabled, viewModel::setSound)
        ToggleRow("Haptics", state.hapticsEnabled, viewModel::setHaptics)

        Spacer(Modifier.height(16.dp))
        Text("THEME", fontFamily = Mono, fontSize = 9.5.sp, letterSpacing = 0.18.em, color = c.muted)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeMode.entries.forEach { mode ->
                val selected = state.themeMode == mode
                Box(
                    Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (selected) c.accent.copy(alpha = 0.12f) else androidx.compose.ui.graphics.Color.Transparent)
                        .border(1.dp, if (selected) c.accent else c.ink.copy(alpha = 0.18f), RoundedCornerShape(999.dp))
                        .clickable { viewModel.setTheme(mode) }
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                ) {
                    Text(
                        mode.name,
                        fontFamily = Mono, fontSize = 9.5.sp, letterSpacing = 0.12.em,
                        color = if (selected) c.accent else c.muted,
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        PaperDivider()
        Spacer(Modifier.height(18.dp))

        if (state.removeAdsOwned) {
            Text(
                "Remove ads — purchased ✓",
                fontFamily = Serif, fontWeight = FontWeight.Light, fontSize = 16.sp, color = c.ink,
            )
        } else {
            AccentButton("Remove ads", onClick = {
                (context as? Activity)?.let(viewModel::purchaseRemoveAds)
            })
        }
        Spacer(Modifier.height(10.dp))
        OutlineButton("Restore purchases", onClick = viewModel::restorePurchases)

        Spacer(Modifier.height(18.dp))
        PaperDivider()
        Spacer(Modifier.height(14.dp))

        LinkRow("Sources", onOpenSources)
        LinkRow("Privacy policy") {
            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, PRIVACY_URL.toUri())) }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "VERSION ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
            fontFamily = Mono, fontSize = 9.sp, letterSpacing = 0.12.em, color = c.muted,
        )
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val c = CryptoTheme.colors
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontFamily = Serif, fontWeight = FontWeight.Light, fontSize = 17.sp, color = c.ink)
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = c.onAccent,
                checkedTrackColor = c.accent,
            ),
        )
    }
}

@Composable
private fun LinkRow(label: String, onClick: () -> Unit) {
    val c = CryptoTheme.colors
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontFamily = Serif, fontWeight = FontWeight.Light, fontSize = 16.sp, color = c.ink)
        Spacer(Modifier.weight(1f))
        Text("›", fontFamily = Serif, fontSize = 18.sp, color = c.muted)
    }
}
