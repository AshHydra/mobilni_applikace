package cz.ash.mobilniapplikace.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cz.ash.mobilniapplikace.ui.components.ErrorView
import cz.ash.mobilniapplikace.ui.components.LoadingView
import cz.ash.mobilniapplikace.ui.di.rememberPostsRepository
import cz.ash.mobilniapplikace.ui.viewmodel.HomeUiItem
import cz.ash.mobilniapplikace.ui.viewmodel.HomeViewModel
import cz.ash.mobilniapplikace.ui.viewmodel.HomeViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenDetail: (Int) -> Unit,
    onOpenFavorites: () -> Unit
) {
    val repository = rememberPostsRepository()
    val factory = remember(repository) { HomeViewModelFactory(repository) }
    val vm: HomeViewModel = viewModel(factory = factory)
    val state by vm.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Home") },
                actions = {
                    IconButton(onClick = onOpenFavorites) {
                        Icon(Icons.Default.Favorite, contentDescription = "Oblíbené")
                    }
                    IconButton(onClick = { vm.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Obnovit")
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
                onRetry = { vm.refresh() }
            )
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.items, key = { it.id }) { item ->
                        PostCard(
                            item = item,
                            onToggleFavorite = { vm.toggleFavorite(item) },
                            onOpen = { onOpenDetail(item.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PostCard(
    item: HomeUiItem,
    onToggleFavorite: () -> Unit,
    onOpen: () -> Unit
) {
    Card(
        onClick = onOpen,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.body,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 6.dp)
            )

            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.align(Alignment.End)
            ) {
                if (item.isFavorite) {
                    Icon(Icons.Default.Favorite, contentDescription = "Odebrat z oblíbených")
                } else {
                    Icon(Icons.Default.FavoriteBorder, contentDescription = "Přidat do oblíbených")
                }
            }
        }
    }
}

