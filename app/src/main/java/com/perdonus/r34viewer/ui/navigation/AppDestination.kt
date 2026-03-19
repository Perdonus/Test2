package com.perdonus.r34viewer.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.ui.graphics.vector.ImageVector

sealed class AppDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    data object Search : AppDestination("search", "Поиск", Icons.Outlined.Search)
    data object Favorites : AppDestination("favorites", "Избранное", Icons.Outlined.FavoriteBorder)
    data object SavedSearches : AppDestination("saved_searches", "Закладки", Icons.Outlined.BookmarkBorder)
    data object Settings : AppDestination("settings", "Настройки", Icons.Outlined.Settings)
    data object Preferences : AppDestination("preferences", "Предпочтения", Icons.Outlined.Tune)
    data object Details : AppDestination("details", "Пост", Icons.Outlined.FavoriteBorder)
}

val topLevelDestinations = listOf(
    AppDestination.Search,
    AppDestination.Favorites,
    AppDestination.SavedSearches,
    AppDestination.Settings,
)
