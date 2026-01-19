package cz.ash.mobilniapplikace.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cz.ash.mobilniapplikace.ui.components.ErrorView
import cz.ash.mobilniapplikace.ui.components.LoadingView
import cz.ash.mobilniapplikace.ui.di.rememberCoinsRepository
import cz.ash.mobilniapplikace.ui.components.CoinRow
import cz.ash.mobilniapplikace.ui.components.CoinRowDivider
import cz.ash.mobilniapplikace.ui.viewmodel.HomeUiItem
import cz.ash.mobilniapplikace.ui.viewmodel.HomeViewModel
import cz.ash.mobilniapplikace.ui.viewmodel.HomeViewModelFactory
import cz.ash.mobilniapplikace.ui.settings.LocalVsCurrency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    onOpenDetail: (String) -> Unit,
    onOpenFavorites: () -> Unit
) {
    val repository = rememberCoinsRepository()
    val factory = remember(repository) { HomeViewModelFactory(repository) }
    val vm: HomeViewModel = viewModel(factory = factory)
    val state by vm.state.collectAsState()
    val chipScroll = rememberScrollState()
    val vsCurrency = LocalVsCurrency.current

    LaunchedEffect(vsCurrency) {
        // When user changes currency, refresh list (cache prevents excessive API calls).
        vm.refresh(vsCurrency = vsCurrency, force = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Coin") },
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            item {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .horizontalScroll(chipScroll),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(selected = true, onClick = { /* TODO */ }, label = { Text("Hot") })
                    FilterChip(selected = false, onClick = { /* TODO */ }, label = { Text("Market cap") })
                    FilterChip(selected = false, onClick = { /* TODO */ }, label = { Text("Price") })
                    FilterChip(selected = false, onClick = { /* TODO */ }, label = { Text("24h change") })
                    FilterChip(selected = false, onClick = onOpenFavorites, label = { Text("Watchlist") })
                }
            }

            when {
                state.isLoading -> item { LoadingView(PaddingValues(24.dp)) }
                state.errorMessage != null -> item {
                    ErrorView(
                        padding = PaddingValues(24.dp),
                        message = state.errorMessage ?: "Error",
                        onRetry = { vm.refresh(vsCurrency = vsCurrency, force = true) }
                    )
                }
                else -> {
                    items(state.items, key = { it.id }) { item ->
                        CoinRow(
                            name = item.name,
                            symbol = item.symbol,
                            imageUrl = item.imageUrl,
                            price = item.priceUsd,
                            change24hPct = item.change24hPct,
                            isFavorite = item.isFavorite,
                            onToggleFavorite = { vm.toggleFavorite(item) },
                            onOpen = { onOpenDetail(item.id) }
                        )
                        CoinRowDivider()
                    }
                }
            }
        }
    }
}
