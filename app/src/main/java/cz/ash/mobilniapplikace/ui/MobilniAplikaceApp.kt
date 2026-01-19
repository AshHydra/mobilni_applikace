package cz.ash.mobilniapplikace.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import cz.ash.mobilniapplikace.ui.di.rememberAuthRepository
import cz.ash.mobilniapplikace.ui.di.rememberCoinsRepository
import cz.ash.mobilniapplikace.ui.di.rememberSettingsRepository
import cz.ash.mobilniapplikace.ui.screens.AuthScreen
import cz.ash.mobilniapplikace.ui.viewmodel.AuthViewModel
import cz.ash.mobilniapplikace.ui.viewmodel.AuthViewModelFactory

@Composable
fun MobilniAplikaceApp() {
    val authRepository = rememberAuthRepository()
    val coinsRepository = rememberCoinsRepository()
    val settingsRepository = rememberSettingsRepository()
    val factory = remember(authRepository, coinsRepository) { AuthViewModelFactory(authRepository, coinsRepository) }
    val authVm: AuthViewModel = viewModel(factory = factory)
    val authState by authVm.state.collectAsState()

    if (authState.user == null) {
        AuthScreen(authViewModel = authVm)
    } else {
        MainScaffold(authViewModel = authVm, settingsRepository = settingsRepository)
    }
}

