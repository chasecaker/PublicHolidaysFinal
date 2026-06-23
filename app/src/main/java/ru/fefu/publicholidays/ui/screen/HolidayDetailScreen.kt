package ru.fefu.publicholidays.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.fefu.publicholidays.ui.state.HolidayDetailUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HolidayDetailScreen(
    state: HolidayDetailUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onNoteChanged: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Детали праздника") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        when (state) {
            is HolidayDetailUiState.Loading -> {
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            is HolidayDetailUiState.NotFound -> {
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Праздник не найден",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Button(
                        modifier = Modifier.padding(top = 12.dp),
                        onClick = onRetry
                    ) {
                        Text("Повторить")
                    }
                }
            }

            is HolidayDetailUiState.Error -> {
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Ошибка: ${state.message}",
                        color = MaterialTheme.colorScheme.error
                    )
                    Button(
                        modifier = Modifier.padding(top = 12.dp),
                        onClick = onRetry
                    ) {
                        Text("Повторить")
                    }
                }
            }

            is HolidayDetailUiState.Success -> {
                val holiday = state.holiday

                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .padding(PaddingValues(16.dp)),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = holiday.name,
                        style = MaterialTheme.typography.headlineSmall
                    )

                    Text(
                        text = holiday.localName,
                        style = MaterialTheme.typography.titleMedium
                    )

                    HorizontalDivider()

                    Text("Дата: ${holiday.date}")
                    Text("Страна: ${holiday.countryCode}")
                    Text("Фиксированный праздник: ${if (holiday.fixed) "да" else "нет"}")
                    Text("Глобальный праздник: ${if (holiday.global) "да" else "нет"}")

                    if (holiday.types.isNotEmpty()) {
                        Text("Типы: ${holiday.types.joinToString()}")
                    }

                    HorizontalDivider()

                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = state.noteText,
                        onValueChange = onNoteChanged,
                        label = { Text("Личная заметка") },
                        placeholder = { Text("Например: купить подарок") },
                        minLines = 3
                    )
                }
            }
        }
    }
}