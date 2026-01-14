package cz.ash.mobilniapplikace.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cz.ash.mobilniapplikace.data.PostsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiItem(
    val id: Int,
    val title: String,
    val body: String,
    val isFavorite: Boolean
)

data class HomeUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val items: List<HomeUiItem> = emptyList()
)

class HomeViewModel(
    private val repository: PostsRepository
) : ViewModel() {

    private val posts = MutableStateFlow<List<HomeUiItem>>(emptyList())
    private val _state = MutableStateFlow(HomeUiState(isLoading = true))
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        // Combine network posts with favorites from DB
        viewModelScope.launch {
            combine(posts, repository.observeFavoriteIds()) { items, favoriteIds ->
                items.map { it.copy(isFavorite = favoriteIds.contains(it.id)) }
            }.collect { combined ->
                _state.update { it.copy(items = combined) }
            }
        }

        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val loaded = repository.fetchPosts()
                    .map { p -> HomeUiItem(p.id, p.title, p.body, isFavorite = false) }
                posts.value = loaded
                _state.update { it.copy(isLoading = false) }
            } catch (t: Throwable) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = t.message ?: "Chyba při načítání dat"
                    )
                }
            }
        }
    }

    fun toggleFavorite(item: HomeUiItem) {
        viewModelScope.launch {
            repository.setFavorite(
                post = cz.ash.mobilniapplikace.domain.Post(item.id, item.title, item.body),
                favorite = !item.isFavorite
            )
        }
    }
}

class HomeViewModelFactory(
    private val repository: PostsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel: $modelClass")
    }
}

