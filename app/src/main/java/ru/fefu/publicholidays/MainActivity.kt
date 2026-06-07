package ru.fefu.publicholidays

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import ru.fefu.publicholidays.ui.navigation.HolidaysNavGraph
import ru.fefu.publicholidays.ui.theme.PublicHolidaysTheme
import ru.fefu.publicholidays.ui.viewmodel.ThemeViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val themeViewModel: ThemeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val isDarkTheme = themeViewModel.isDarkTheme.collectAsStateWithLifecycle()

            PublicHolidaysTheme(
                darkTheme = isDarkTheme.value
            ) {
                HolidaysNavGraph()
            }
        }
    }
}