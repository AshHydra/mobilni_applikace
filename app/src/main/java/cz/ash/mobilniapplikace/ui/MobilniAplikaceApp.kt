package cz.ash.mobilniapplikace.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cz.ash.mobilniapplikace.ui.screens.DetailScreen
import cz.ash.mobilniapplikace.ui.screens.FavoritesScreen
import cz.ash.mobilniapplikace.ui.screens.HomeScreen

@Composable
fun MobilniAplikaceApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.Home
    ) {
        composable(Routes.Home) {
            HomeScreen(
                onOpenDetail = { id -> navController.navigate(Routes.detail(id)) },
                onOpenFavorites = { navController.navigate(Routes.Favorites) }
            )
        }
        composable(Routes.Favorites) {
            FavoritesScreen(
                onOpenDetail = { id -> navController.navigate(Routes.detail(id)) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.Detail) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id")?.toIntOrNull()
            if (id != null) {
                DetailScreen(
                    postId = id,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

