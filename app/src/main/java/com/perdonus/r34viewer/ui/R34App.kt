package com.perdonus.r34viewer.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.paging.compose.collectAsLazyPagingItems
import com.perdonus.r34viewer.ui.navigation.AppDestination
import com.perdonus.r34viewer.ui.navigation.topLevelDestinations
import com.perdonus.r34viewer.ui.screens.FavoritesScreen
import com.perdonus.r34viewer.ui.screens.PostDetailScreen
import com.perdonus.r34viewer.ui.screens.SavedSearchesScreen
import com.perdonus.r34viewer.ui.screens.SearchScreen
import com.perdonus.r34viewer.ui.screens.SettingsScreen
import com.perdonus.r34viewer.ui.viewmodel.AppViewModel
import com.perdonus.r34viewer.ui.viewmodel.AppViewModelProvider
import com.perdonus.r34viewer.ui.viewmodel.FavoritesViewModel
import com.perdonus.r34viewer.ui.viewmodel.SavedSearchesViewModel
import com.perdonus.r34viewer.ui.viewmodel.SearchViewModel
import com.perdonus.r34viewer.ui.viewmodel.SettingsViewModel
import com.perdonus.r34viewer.R34Application

@Composable
fun R34App() {
    val application = LocalContext.current.applicationContext as R34Application
    val navController = rememberNavController()
    val appViewModel: AppViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val searchViewModel: SearchViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val favoritesViewModel: FavoritesViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val savedSearchesViewModel: SavedSearchesViewModel = viewModel(factory = AppViewModelProvider.Factory)
    val settingsViewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory)

    val selectedPost by appViewModel.selectedPost.collectAsStateWithLifecycle()
    val query by searchViewModel.queryText.collectAsStateWithLifecycle()
    val feedbackMessage by searchViewModel.feedbackMessage.collectAsStateWithLifecycle()
    val hasSubmittedSearch by searchViewModel.hasSubmittedSearch.collectAsStateWithLifecycle()
    val isResolvingQuery by searchViewModel.isResolvingQuery.collectAsStateWithLifecycle()
    val favoriteIds by searchViewModel.favoriteIds.collectAsStateWithLifecycle()
    val settings by searchViewModel.settings.collectAsStateWithLifecycle()
    val favorites by favoritesViewModel.favorites.collectAsStateWithLifecycle()
    val savedSearches by savedSearchesViewModel.savedSearches.collectAsStateWithLifecycle()
    val settingsForm by settingsViewModel.state.collectAsStateWithLifecycle()

    val pagingItems = searchViewModel.pagingData.collectAsLazyPagingItems()

    val okHttpClient = remember(settings.proxyConfig.signature()) {
        application.container.networkClientFactory.create(settings)
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute != AppDestination.Details.route

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    topLevelDestinations.forEach { destination ->
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { androidx.compose.material3.Icon(destination.icon, contentDescription = destination.label) },
                            label = { Text(destination.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.Search.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(AppDestination.Search.route) {
                SearchScreen(
                    query = query,
                    hasSubmittedSearch = hasSubmittedSearch,
                    pagingItems = pagingItems,
                    favoriteIds = favoriteIds,
                    settings = settings,
                    feedbackMessage = feedbackMessage,
                    isResolvingQuery = isResolvingQuery,
                    okHttpClient = okHttpClient,
                    onQueryChanged = searchViewModel::updateQuery,
                    onSearch = searchViewModel::submitSearch,
                    onAiSearch = searchViewModel::runAiSearch,
                    onSaveSearch = searchViewModel::saveCurrentSearch,
                    onToggleAiFilter = searchViewModel::updateHideAiContent,
                    onSelectService = searchViewModel::selectService,
                    onOpenSettings = {
                        navController.navigate(AppDestination.Settings.route)
                    },
                    onOpenPost = { post ->
                        appViewModel.selectPost(post)
                        navController.navigate(AppDestination.Details.route)
                    },
                    onToggleFavorite = searchViewModel::toggleFavorite,
                    onDismissMessage = searchViewModel::clearMessage,
                )
            }

            composable(AppDestination.Favorites.route) {
                FavoritesScreen(
                    favorites = favorites,
                    okHttpClient = okHttpClient,
                    onOpenPost = { post ->
                        appViewModel.selectPost(post)
                        navController.navigate(AppDestination.Details.route)
                    },
                    onToggleFavorite = favoritesViewModel::toggleFavorite,
                )
            }

            composable(AppDestination.SavedSearches.route) {
                SavedSearchesScreen(
                    savedSearches = savedSearches,
                    onRunSearch = { search ->
                        searchViewModel.runSearch(search.query, search.service)
                        navController.navigate(AppDestination.Search.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onRename = savedSearchesViewModel::rename,
                    onDelete = savedSearchesViewModel::delete,
                )
            }

            composable(AppDestination.Settings.route) {
                SettingsScreen(
                    state = settingsForm,
                    onProxyEnabledChanged = settingsViewModel::updateProxyEnabled,
                    onProxyTypeChanged = settingsViewModel::updateProxyType,
                    onProxyHostChanged = settingsViewModel::updateProxyHost,
                    onProxyPortChanged = settingsViewModel::updateProxyPort,
                    onProxyUsernameChanged = settingsViewModel::updateProxyUsername,
                    onProxyPasswordChanged = settingsViewModel::updateProxyPassword,
                    onSave = settingsViewModel::save,
                )
            }

            composable(AppDestination.Details.route) {
                PostDetailScreen(
                    post = selectedPost,
                    isFavorite = selectedPost?.serviceScopedId?.let(favoriteIds::contains) == true,
                    okHttpClient = okHttpClient,
                    onBack = { navController.popBackStack() },
                    onToggleFavorite = { post ->
                        searchViewModel.toggleFavorite(post)
                    },
                    onTagSelected = { tag ->
                        searchViewModel.runSearch(tag, selectedPost?.service)
                        navController.navigate(AppDestination.Search.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        }
    }
}
