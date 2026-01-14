package cz.ash.mobilniapplikace.ui.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import cz.ash.mobilniapplikace.data.PostsRepository
import cz.ash.mobilniapplikace.data.local.AppDatabase
import cz.ash.mobilniapplikace.data.remote.ApiClient

@Composable
fun rememberPostsRepository(): PostsRepository {
    val appContext = LocalContext.current.applicationContext
    return remember(appContext) {
        PostsRepository(
            api = ApiClient.api,
            favoritesDao = AppDatabase.get(appContext).favoritesDao()
        )
    }
}

