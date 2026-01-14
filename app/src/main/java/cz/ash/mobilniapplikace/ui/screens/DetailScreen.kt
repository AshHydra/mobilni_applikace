package cz.ash.mobilniapplikace.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cz.ash.mobilniapplikace.ui.components.ErrorView
import cz.ash.mobilniapplikace.ui.components.LoadingView
import cz.ash.mobilniapplikace.ui.di.rememberPostsRepository
import cz.ash.mobilniapplikace.ui.viewmodel.DetailViewModel
import cz.ash.mobilniapplikace.ui.viewmodel.DetailViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    postId: Int,
    onBack: () -> Unit
) {
    val repository = rememberPostsRepository()
    val factory = remember(repository, postId) { DetailViewModelFactory(postId, repository) }
    val vm: DetailViewModel = viewModel(factory = factory)
    val state by vm.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detail") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Zpět")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { vm.toggleFavorite() },
                        enabled = state.post != null
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
                onRetry = { onBack() }
            )
            state.post == null -> LoadingView(padding)
            else -> {
                val post = state.post
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp)
                ) {
                    Text(
                        text = post?.title.orEmpty(),
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = post?.body.orEmpty(),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 12.dp)
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

