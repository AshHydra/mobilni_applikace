package cz.ash.mobilniapplikace.ui

object Routes {
    const val Home = "home"
    const val Favorites = "favorites"

    // Nav argument is string, we'll parse to Int
    const val Detail = "detail/{id}"
    fun detail(id: Int) = "detail/$id"
}

