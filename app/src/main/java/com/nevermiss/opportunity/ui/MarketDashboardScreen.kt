package com.nevermiss.opportunity.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nevermiss.opportunity.domain.MovementFormatter
import com.nevermiss.opportunity.domain.QuoteState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketDashboardScreen(
    state: MarketDashboardUiState,
    notificationsEnabled: Boolean,
    onEnableNotifications: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Market Alerts") },
                actions = { TextButton(onClick = onRefresh) { Text("Refresh") } },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            if (!notificationsEnabled) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Notifications are off", style = MaterialTheme.typography.bodyMedium)
                    Button(onClick = onEnableNotifications) { Text("Enable") }
                }
            }
            LazyColumn {
                items(state.rows, key = { it.indicator.name }) { row ->
                    MarketIndicatorRow(row)
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun MarketIndicatorRow(row: MarketRowState) {
    val quote = row.quote
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(row.indicator.displayName, fontWeight = FontWeight.SemiBold)
            Text(quote?.let { MovementFormatter.value(it) } ?: "--")
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(quote?.let { MovementFormatter.movement(it) } ?: "No data")
            Text(row.statusText())
        }
        Text(
            text = quote?.let { "Updated ${timeFormatter.format(Date(it.observedAtEpochMillis))}" } ?: "Waiting for first update",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

internal fun MarketRowState.statusText(): String =
    quote?.let { "${it.state.label} · ${it.source}" }
        ?: "${QuoteState.UNAVAILABLE.label} · ${indicator.sourceLabel}"

private val QuoteState.label: String
    get() = name.lowercase().replaceFirstChar { it.titlecase(Locale.US) }

private val timeFormatter = SimpleDateFormat("HH:mm", Locale.KOREA)
