package dev.milan.cryptogram.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import dev.milan.cryptogram.CryptogramApp
import dev.milan.cryptogram.di.AppContainer

/** The app-wide [AppContainer], reachable from any ViewModel factory's [CreationExtras]. */
val CreationExtras.appContainer: AppContainer
    get() = (this[APPLICATION_KEY] as CryptogramApp).container
