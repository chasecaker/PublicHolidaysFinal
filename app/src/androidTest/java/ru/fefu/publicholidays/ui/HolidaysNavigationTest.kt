package ru.fefu.publicholidays.ui

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.compose.rememberNavController
import org.junit.Rule
import org.junit.Test
import ru.fefu.publicholidays.ui.model.HolidayUi
import ru.fefu.publicholidays.ui.navigation.Routes
import ru.fefu.publicholidays.ui.screen.HolidayDetailScreen
import ru.fefu.publicholidays.ui.screen.HolidaysListScreen
import ru.fefu.publicholidays.ui.state.HolidayDetailUiState
import ru.fefu.publicholidays.ui.state.HolidaysUiState

class HolidaysNavigationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun listItemClick_opensDetailScreenForCorrectHolidayId() {
        val holiday = HolidayUi(
            id = "2026-01-01|RU|New Year's Day",
            date = "2026-01-01",
            localName = "Новый год",
            name = "New Year's Day",
            countryCode = "RU",
            fixed = true,
            global = true,
            types = listOf("Public")
        )

        composeTestRule.setContent {
            MaterialTheme {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = Routes.LIST
                ) {
                    composable(Routes.LIST) {
                        HolidaysListScreen(
                            state = HolidaysUiState.Success(
                                data = listOf(holiday),
                                favourites = emptySet()
                            ),
                            query = "",
                            onQueryChange = {},
                            onItemClick = { id ->
                                navController.navigate(Routes.detailRoute(id))
                            },
                            onOpenFavourites = {},
                            onOpenHistory = {},
                            onOpenSettings = {},
                            onRetry = {},
                            onToggleFavourite = {},
                            onLoad = {}
                        )
                    }

                    composable(
                        route = Routes.DETAIL_ROUTE,
                        arguments = listOf(
                            navArgument(Routes.ARG_ID) {
                                type = NavType.StringType
                            }
                        )
                    ) { backStackEntry ->
                        val id = backStackEntry.arguments
                            ?.getString(Routes.ARG_ID)
                            .orEmpty()

                        val detailState =
                            if (id == holiday.id) {
                                HolidayDetailUiState.Success(
                                    holiday = holiday,
                                    noteText = ""
                                )
                            } else {
                                HolidayDetailUiState.NotFound
                            }

                        HolidayDetailScreen(
                            state = detailState,
                            onBack = {},
                            onRetry = {},
                            onNoteChanged = {}
                        )
                    }
                }
            }
        }

        composeTestRule.onNodeWithText("New Year's Day").performClick()

        composeTestRule.onNodeWithText("Детали праздника").assertIsDisplayed()
        composeTestRule.onNodeWithText("Новый год").assertIsDisplayed()
        composeTestRule.onNodeWithText("Дата: 2026-01-01").assertIsDisplayed()
    }
}