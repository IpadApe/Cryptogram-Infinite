package dev.milan.cryptogram.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import dev.milan.cryptogram.CryptogramApp
import dev.milan.cryptogram.di.AppContainer

/** The app-wide [AppContainer], reachable from any ViewModel factory's [CreationExtras]. */
val CreationExtras.appContainer: AppContainer
    get() = (this[APPLICATION_KEY] as CryptogramApp).container

/** The app-wide [AppContainer], for composables that need a repository directly. */
@Composable
fun rememberAppContainer(): AppContainer =
    (LocalContext.current.applicationContext as CryptogramApp).container
