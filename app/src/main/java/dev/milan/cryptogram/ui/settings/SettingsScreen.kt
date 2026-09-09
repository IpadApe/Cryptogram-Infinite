package dev.milan.cryptogram.ui.settings

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.milan.cryptogram.BuildConfig
import dev.milan.cryptogram.data.prefs.ThemeMode

private const val PRIVACY_URL =
    "https://ipadape.github.io/Cryptogram-Infinite/privacy.html"

@Composable
fun SettingsScreen(
    onOpenSources: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory()),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall)

        ToggleRow("Sound", state.soundEnabled, viewModel::setSound)
        ToggleRow("Haptics", state.hapticsEnabled, viewModel::setHaptics)

        Text("Theme", style = MaterialTheme.typography.titleMedium)
        Row(
            Modifier.selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ThemeMode.entries.forEach { mode ->
                FilterChip(
                    selected = state.themeMode == mode,
                    onClick = { viewModel.setTheme(mode) },
                    label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) },
                )
            }
        }

        HorizontalDivider()

        if (state.removeAdsOwned) {
            Text("Remove ads: Purchased ✓")
        } else {
            Button(onClick = {
                (context as? Activity)?.let(viewModel::purchaseRemoveAds)
            }) { Text("Remove ads") }
        }
        OutlinedButton(onClick = viewModel::restorePurchases) { Text("Restore purchases") }

        HorizontalDivider()

        TextButton(onClick = onOpenSources) { Text("Sources") }
        TextButton(onClick = {
            runCatching {
                context.startActivity(Intent(Intent.ACTION_VIEW, PRIVACY_URL.toUri()))
            }
        }) { Text("Privacy policy") }
        Text(
            "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
