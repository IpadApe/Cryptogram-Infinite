package dev.milan.cryptogram

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import dev.milan.cryptogram.data.prefs.ThemeMode
import dev.milan.cryptogram.ui.navigation.NavGraph
import dev.milan.cryptogram.ui.theme.CryptoTheme
import dev.milan.cryptogram.ui.theme.CryptogramInfiniteTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as CryptogramApp).container
        container.consentManager.gatherConsent(this, lifecycleScope)

        setContent {
            val themeMode by container.settingsStore.themeMode
                .collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
            val dark = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            CryptogramInfiniteTheme(darkTheme = dark) {
                NavGraph(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(CryptoTheme.colors.paper),
                )
            }
        }
    }
}
