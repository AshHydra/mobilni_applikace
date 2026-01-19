package cz.ash.mobilniapplikace.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cz.ash.mobilniapplikace.ui.di.rememberCoinsRepository
import cz.ash.mobilniapplikace.ui.components.CoinRow
import cz.ash.mobilniapplikace.ui.components.CoinRowDivider
import cz.ash.mobilniapplikace.ui.settings.LocalVsCurrency
import cz.ash.mobilniapplikace.ui.viewmodel.FavoritesViewModel
import cz.ash.mobilniapplikace.ui.viewmodel.FavoritesViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onOpenDetail: (String) -> Unit,
) {
    val repository = rememberCoinsRepository()
    val factory = remember(repository) { FavoritesViewModelFactory(repository) }
    val vm: FavoritesViewModel = viewModel(factory = factory)
    val state by vm.state.collectAsState()
    val vsCurrency = LocalVsCurrency.current

    var networkItems by remember { mutableStateOf<List<cz.ash.mobilniapplikace.domain.Coin>>(emptyList()) }
    var networkError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(vsCurrency, state.items.map { it.id }.sorted().joinToString(",")) {
        // Fetch fresh watchlist prices for selected currency in a single request.
        networkError = null
        networkItems = emptyList()
        val ids = state.items.map { it.id }
        if (ids.isNotEmpty()) {
            try {
                networkItems = repository.fetchCoinsByIds(ids = ids, vsCurrency = vsCurrency, force = false)
            } catch (t: Throwable) {
                networkError = t.message ?: "Failed to load watchlist prices"
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Watchlist") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            if (networkError != null) {
                item {
                    Text(
                        text = networkError.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            }

            val list = if (networkItems.isNotEmpty()) networkItems else state.items
            items(list, key = { it.id }) { item ->
                CoinRow(
                    name = item.name,
                    symbol = item.symbol,
                    imageUrl = item.imageUrl,
                    price = item.priceUsd,
                    change24hPct = item.change24hPct,
                    isFavorite = true,
                    onToggleFavorite = { vm.removeFromFavorites(item) },
                    onOpen = { onOpenDetail(item.id) }
                )
                CoinRowDivider()
            }
        }
    }
}

