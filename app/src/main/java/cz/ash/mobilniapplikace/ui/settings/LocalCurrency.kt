package cz.ash.mobilniapplikace.ui.settings

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Selected fiat currency code used for displaying prices and for CoinGecko `vs_currency`.
 * Stored in SettingsRepository, provided from MainScaffold.
 */
val LocalVsCurrency = staticCompositionLocalOf { "usd" }

