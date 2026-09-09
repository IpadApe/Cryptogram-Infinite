package dev.milan.cryptogram.ui.settings

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.milan.cryptogram.billing.BillingManager
import dev.milan.cryptogram.data.prefs.SettingsStore
import dev.milan.cryptogram.data.prefs.ThemeMode
import dev.milan.cryptogram.ui.appContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val removeAdsOwned: Boolean = false,
)

class SettingsViewModel(
    private val settings: SettingsStore,
    private val billing: BillingManager,
) : ViewModel() {

    val state: StateFlow<SettingsUiState> = combine(
        settings.soundEnabled,
        settings.hapticsEnabled,
        settings.themeMode,
        settings.removeAdsOwned,
    ) { sound, haptics, theme, ownsAds ->
        SettingsUiState(sound, haptics, theme, ownsAds)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setSound(value: Boolean) = viewModelScope.launch { settings.setSoundEnabled(value) }
    fun setHaptics(value: Boolean) = viewModelScope.launch { settings.setHapticsEnabled(value) }
    fun setTheme(value: ThemeMode) = viewModelScope.launch { settings.setThemeMode(value) }

    fun purchaseRemoveAds(activity: Activity) = billing.launchPurchase(activity)
    fun restorePurchases() = viewModelScope.launch { billing.refreshOwned() }

    companion object {
        fun factory() = viewModelFactory {
            initializer {
                val c = appContainer
                SettingsViewModel(c.settingsStore, c.billingManager)
            }
        }
    }
}
