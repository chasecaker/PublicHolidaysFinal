package ru.fefu.publicholidays.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ru.fefu.publicholidays.ui.state.HolidaysUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HolidaysListScreen(
    state: HolidaysUiState,
    query: String,
    onQueryChange: (String) -> Unit,
    onItemClick: (String) -> Unit,
    onOpenFavourites: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onRetry: () -> Unit,
    onToggleFavourite: (String) -> Unit,
    onLoad: () -> Unit
) {
    LaunchedEffect(Unit) {
        onLoad()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Праздники 2026") },
                actions = {
                    IconButton(onClick = onOpenHistory) {
                        Icon(
                            Icons.Filled.History,
                            contentDescription = "История просмотров",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = onOpenFavourites) {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = "Любимые праздники",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = "Настройки",
                            tint = Color.White
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
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            when (state) {
                is HolidaysUiState.Loading -> {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }

                is HolidaysUiState.Error -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Ошибка: ${state.message}")
                        Button(onClick = onRetry) {
                            Text("Перезагрузить")
                        }
                    }
                }

                is HolidaysUiState.Empty -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        OutlinedTextField(
                            value = query,
                            onValueChange = onQueryChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            singleLine = true,
                            label = { Text("Поиск") }
                        )

                        Spacer(Modifier.height(12.dp))
                        Text("Праздники не найдены")
                    }
                }

                is HolidaysUiState.Success -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        OutlinedTextField(
                            value = query,
                            onValueChange = onQueryChange,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("Поиск") }
                        )

                        Spacer(Modifier.height(12.dp))

                        if (state.data.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Ничего не найдено :(")
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 16.dp)
                            ) {
                                items(
                                    items = state.data,
                                    key = { holiday -> holiday.id }
                                ) { holiday ->
                                    val isFav = state.favourites.contains(holiday.id)

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 8.dp),
                                        onClick = { onItemClick(holiday.id) }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    holiday.name,
                                                    style = MaterialTheme.typography.titleMedium
                                                )
                                                Text(
                                                    holiday.localName,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(
                                                        alpha = 0.6f
                                                    )
                                                )
                                                Text(
                                                    holiday.date,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }

                                            IconButton(
                                                onClick = { onToggleFavourite(holiday.id) }
                                            ) {
                                                Icon(
                                                    imageVector = if (isFav) {
                                                        Icons.Filled.Star
                                                    } else {
                                                        Icons.Outlined.StarBorder
                                                    },
                                                    contentDescription = "Любимые праздники",
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}