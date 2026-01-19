package cz.ash.mobilniapplikace.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cz.ash.mobilniapplikace.ui.components.ErrorView
import cz.ash.mobilniapplikace.ui.components.LoadingView
import cz.ash.mobilniapplikace.ui.di.rememberCoinsRepository
import cz.ash.mobilniapplikace.ui.settings.LocalVsCurrency
import cz.ash.mobilniapplikace.ui.viewmodel.DetailViewModel
import cz.ash.mobilniapplikace.ui.viewmodel.DetailViewModelFactory
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    coinId: String,
    onBack: () -> Unit
) {
    val repository = rememberCoinsRepository()
    val factory = remember(repository, coinId) { DetailViewModelFactory(coinId, repository) }
    val vm: DetailViewModel = viewModel(factory = factory)
    val state by vm.state.collectAsState()
    val vsCurrency = LocalVsCurrency.current
    val currency = remember(vsCurrency) {
        NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
            runCatching { this.currency = Currency.getInstance(vsCurrency.uppercase()) }
        }
    }

    LaunchedEffect(vsCurrency, coinId) {
        vm.load(vsCurrency = vsCurrency, force = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detail") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zpět")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { vm.toggleFavorite() },
                        enabled = state.coin != null
                    ) {
                        if (state.isFavorite) {
                            Icon(Icons.Default.Favorite, contentDescription = "Odebrat z oblíbených")
                        } else {
                            Icon(Icons.Default.FavoriteBorder, contentDescription = "Přidat do oblíbených")
                        }
                    }
                }
            )
        }
    ) { padding ->
        when {
            state.isLoading -> LoadingView(padding)
            state.errorMessage != null -> ErrorView(
                padding = padding,
                message = state.errorMessage ?: "Chyba",
                onRetry = { vm.load(vsCurrency = vsCurrency, force = true) }
            )
            state.coin == null -> LoadingView(padding)
            else -> {
                val coin = state.coin
                val change = coin?.change24hPct
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "${coin?.name.orEmpty()} (${coin?.symbol?.uppercase().orEmpty()})",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = buildString {
                            append("Cena: ")
                            append(coin?.priceUsd?.let { currency.format(it) } ?: "—")
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    Text(
                        text = buildString {
                            append("Změna 24h: ")
                            append(change?.let { String.format(Locale.US, "%.2f%%", it) } ?: "—")
                        },
                        color = when {
                            change == null -> MaterialTheme.colorScheme.onSurface
                            change >= 0 -> Color(0xFF2E7D32)
                            else -> Color(0xFFC62828)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                    Text(
                        text = buildString {
                            append("Market cap: ")
                            append(coin?.marketCapUsd?.let { currency.format(it) } ?: "—")
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                    Text(
                        text = if (state.isFavorite) "Uloženo v oblíbených" else "Není v oblíbených",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier
                            .padding(top = 20.dp)
                            .align(Alignment.End)
                    )
                }
            }
        }
    }
}

