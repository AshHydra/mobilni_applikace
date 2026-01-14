package cz.ash.mobilniapplikace.data

import cz.ash.mobilniapplikace.data.local.FavoritePostEntity
import cz.ash.mobilniapplikace.data.local.FavoritesDao
import cz.ash.mobilniapplikace.data.remote.JsonPlaceholderApi
import cz.ash.mobilniapplikace.domain.Post
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PostsRepository(
    private val api: JsonPlaceholderApi,
    private val favoritesDao: FavoritesDao
) {
    fun observeFavoriteIds(): Flow<Set<Int>> =
        favoritesDao.observeAll().map { list -> list.map { it.id }.toSet() }

    fun observeFavorites(): Flow<List<Post>> =
        favoritesDao.observeAll().map { list -> list.map { Post(it.id, it.title, it.body) } }

    fun observeIsFavorite(id: Int): Flow<Boolean> = favoritesDao.observeIsFavorite(id)

    suspend fun fetchPosts(): List<Post> =
        api.getPosts().map { dto -> Post(id = dto.id, title = dto.title, body = dto.body) }

    suspend fun fetchPost(id: Int): Post {
        val dto = api.getPost(id)
        return Post(id = dto.id, title = dto.title, body = dto.body)
    }

    suspend fun setFavorite(post: Post, favorite: Boolean) {
        if (favorite) {
            favoritesDao.upsert(FavoritePostEntity(id = post.id, title = post.title, body = post.body))
        } else {
            favoritesDao.deleteById(post.id)
        }
    }
}

