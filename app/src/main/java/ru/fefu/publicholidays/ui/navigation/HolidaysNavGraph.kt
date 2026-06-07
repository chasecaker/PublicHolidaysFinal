package ru.fefu.publicholidays.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ru.fefu.publicholidays.ui.screen.FavouritesScreen
import ru.fefu.publicholidays.ui.screen.HistoryScreen
import ru.fefu.publicholidays.ui.screen.HolidayDetailScreen
import ru.fefu.publicholidays.ui.screen.HolidaysListScreen
import ru.fefu.publicholidays.ui.screen.SettingsScreen
import ru.fefu.publicholidays.ui.viewmodel.FavoritesViewModel
import ru.fefu.publicholidays.ui.viewmodel.HistoryViewModel
import ru.fefu.publicholidays.ui.viewmodel.HolidayDetailViewModel
import ru.fefu.publicholidays.ui.viewmodel.HolidaysViewModel
import ru.fefu.publicholidays.ui.viewmodel.SettingsViewModel

@Composable
fun HolidaysNavGraph() {
    val navController = rememberNavController()
    val holidaysViewModel: HolidaysViewModel = hiltViewModel()

    val listState by holidaysViewModel.uiState.collectAsStateWithLifecycle()
    val query by holidaysViewModel.query.collectAsStateWithLifecycle()

    NavHost(
        navController = navController,
        startDestination = Routes.LIST
    ) {
        composable(Routes.LIST) {
            HolidaysListScreen(
                state = listState,
                query = query,
                onQueryChange = holidaysViewModel::onSearchQueryChange,
                onItemClick = { holidayId ->
                    navController.navigate(Routes.detailRoute(holidayId))
                },
                onOpenFavourites = { navController.navigate(Routes.FAVOURITES) },
                onOpenHistory = { navController.navigate(Routes.HISTORY) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onRetry = holidaysViewModel::refresh,
                onToggleFavourite = { holidayId ->
                    holidaysViewModel.toggleFavorite(holidayId)
                },
                onLoad = { holidaysViewModel.loadHolidays() }
            )
        }

        composable(Routes.FAVOURITES) {
            val favoritesViewModel: FavoritesViewModel = hiltViewModel()
            val favoritesState by favoritesViewModel.uiState.collectAsStateWithLifecycle()

            FavouritesScreen(
                state = favoritesState,
                onBack = { navController.popBackStack() },
                onItemClick = { holidayId ->
                    navController.navigate(Routes.detailRoute(holidayId))
                },
                onRemoveFavourite = { holiday ->
                    favoritesViewModel.removeFromFavorites(holiday)
                },
                onFavoriteNoteChange = { holiday, note ->
                    favoritesViewModel.updateFavoriteNote(holiday, note)
                }
            )
        }

        composable(
            route = Routes.DETAIL_ROUTE,
            arguments = listOf(navArgument(Routes.ARG_ID) { type = NavType.StringType })
        ) {
            val detailViewModel: HolidayDetailViewModel = hiltViewModel()

            HolidayDetailScreen(
                state = detailViewModel.uiState,
                onBack = { navController.popBackStack() },
                onRetry = { detailViewModel.loadHoliday() },
                onNoteChanged = { text -> detailViewModel.saveNote(text) }
            )
        }

        composable(Routes.HISTORY) {
            val historyViewModel: HistoryViewModel = hiltViewModel()

            HistoryScreen(
                onBackClick = { navController.popBackStack() },
                viewModel = historyViewModel
            )
        }

        composable(Routes.SETTINGS) {
            val settingsViewModel: SettingsViewModel = hiltViewModel()

            SettingsScreen(
                onBackClick = { navController.popBackStack() },
                viewModel = settingsViewModel
            )
        }
    }
}