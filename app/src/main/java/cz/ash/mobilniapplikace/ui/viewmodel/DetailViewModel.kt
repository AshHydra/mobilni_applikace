package cz.ash.mobilniapplikace.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cz.ash.mobilniapplikace.data.PostsRepository
import cz.ash.mobilniapplikace.domain.Post
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DetailUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val post: Post? = null,
    val isFavorite: Boolean = false
)

class DetailViewModel(
    private val postId: Int,
    private val repository: PostsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(DetailUiState(isLoading = true))
    val state: StateFlow<DetailUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeIsFavorite(postId).collect { fav ->
                _state.update { it.copy(isFavorite = fav) }
            }
        }
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val post = repository.fetchPost(postId)
                _state.update { it.copy(isLoading = false, post = post) }
            } catch (t: Throwable) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = t.message ?: "Chyba při načítání detailu"
                    )
                }
            }
        }
    }

    fun toggleFavorite() {
        val post = _state.value.post ?: return
        viewModelScope.launch {
            repository.setFavorite(post, favorite = !_state.value.isFavorite)
        }
    }
}

class DetailViewModelFactory(
    private val postId: Int,
    private val repository: PostsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DetailViewModel(postId, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel: $modelClass")
    }
}

