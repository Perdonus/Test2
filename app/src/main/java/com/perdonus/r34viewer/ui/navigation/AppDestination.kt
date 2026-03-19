package com.perdonus.r34viewer.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.ManageSearch
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class AppDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    data object Search : AppDestination("search", "Search", Icons.Outlined.ManageSearch)
    data object Favorites : AppDestination("favorites", "Favorites", Icons.Outlined.FavoriteBorder)
    data object SavedSearches : AppDestination("saved_searches", "Bookmarks", Icons.Outlined.Bookmarks)
    data object Settings : AppDestination("settings", "Settings", Icons.Outlined.Settings)
    data object Details : AppDestination("details", "Post", Icons.Outlined.FavoriteBorder)
}

val topLevelDestinations = listOf(
    AppDestination.Search,
    AppDestination.Favorites,
    AppDestination.SavedSearches,
    AppDestination.Settings,
)
