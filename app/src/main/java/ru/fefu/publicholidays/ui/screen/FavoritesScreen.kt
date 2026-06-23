package ru.fefu.publicholidays.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import ru.fefu.publicholidays.ui.model.HolidayUi
import ru.fefu.publicholidays.ui.state.FavouriteHolidaysUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavouritesScreen(
    state: FavouriteHolidaysUiState,
    onBack: () -> Unit,
    onItemClick: (String) -> Unit,
    onRemoveFavourite: (HolidayUi) -> Unit,
    onFavoriteNoteChange: (HolidayUi, String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Избранные праздники") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    titleContentColor = MaterialTheme.colorScheme.onSecondary
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when (state) {
                is FavouriteHolidaysUiState.Loading -> {
                    CircularProgressIndicator()
                }

                is FavouriteHolidaysUiState.Error -> {
                    Text("Ошибка: ${state.message}")
                }

                is FavouriteHolidaysUiState.Empty -> {
                    Text("Нет избранных праздников")
                }

                is FavouriteHolidaysUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = state.data,
                            key = { it.id }
                        ) { holiday ->
                            FavoriteHolidayItem(
                                holiday = holiday,
                                onClick = { onItemClick(holiday.id) },
                                onRemoveFavourite = { onRemoveFavourite(holiday) },
                                onNoteSave = { updatedText -> onFavoriteNoteChange(holiday, updatedText) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FavoriteHolidayItem(
    holiday: HolidayUi,
    onClick: () -> Unit,
    onRemoveFavourite: () -> Unit,
    onNoteSave: (String) -> Unit
) {
    var localNoteText by remember(holiday.id) { mutableStateOf(holiday.favoriteNote) }

    LaunchedEffect(localNoteText) {
        if (localNoteText == holiday.favoriteNote) return@LaunchedEffect

        delay(600L)
        onNoteSave(localNoteText)
    }

    DisposableEffect(holiday.id) {
        onDispose {
            if (localNoteText != holiday.favoriteNote) {
                onNoteSave(localNoteText)
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = holiday.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = holiday.localName,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "${holiday.date} · ${holiday.countryCode}",
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(onClick = onRemoveFavourite) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = "Удалить из избранного",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = localNoteText,
                onValueChange = { localNoteText = it },
                label = { Text("Личная пометка") },
                placeholder = { Text("Например: купить подарок") },
                minLines = 1,
                maxLines = 3
            )
        }
    }
}