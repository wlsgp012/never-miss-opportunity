package com.nevermiss.opportunity.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nevermiss.opportunity.domain.AlertScheduleFormatter
import com.nevermiss.opportunity.domain.AlertScheduleRule
import com.nevermiss.opportunity.domain.Indicator
import com.nevermiss.opportunity.domain.MarketQuote
import com.nevermiss.opportunity.domain.MovementFormatter
import com.nevermiss.opportunity.domain.QuoteState
import com.nevermiss.opportunity.ui.theme.MarketGreen
import com.nevermiss.opportunity.ui.theme.MarketRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketDashboardScreen(
    state: MarketDashboardUiState,
    notificationsEnabled: Boolean,
    onEnableNotifications: () -> Unit,
    onRefresh: () -> Unit,
    onExportSchedules: () -> Unit,
    onImportSchedules: () -> Unit,
    onAddFixedRule: (Indicator, String) -> Boolean,
    onAddRecurringRule: (Indicator, String, String, String, Int) -> Boolean,
    onToggleRule: (AlertScheduleRule) -> Unit,
    onDeleteRule: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var editingIndicator by remember { mutableStateOf<Indicator?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Market Alerts") },
                actions = {
                    TextButton(onClick = onImportSchedules) { Text("Import") }
                    TextButton(onClick = onExportSchedules) { Text("Export") }
                    TextButton(onClick = onRefresh) { Text("Refresh") }
                },
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
            LazyColumn(
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.rows, key = { it.indicator.name }) { row ->
                    MarketIndicatorRow(row, onEditSchedule = { editingIndicator = row.indicator })
                }
            }
        }
    }

    editingIndicator?.let { indicator ->
        val row = state.rows.firstOrNull { it.indicator == indicator }
            ?: MarketRowState(indicator, null)
        ScheduleRulesDialog(
            row = row,
            onDismiss = { editingIndicator = null },
            onAddFixedRule = onAddFixedRule,
            onAddRecurringRule = onAddRecurringRule,
            onToggleRule = onToggleRule,
            onDeleteRule = onDeleteRule,
        )
    }
}

@Composable
private fun MarketIndicatorRow(row: MarketRowState, onEditSchedule: () -> Unit) {
    val quote = row.quote
    val movementColor = when {
        quote == null || quote.change == 0.0 -> MaterialTheme.colorScheme.onSurfaceVariant
        quote.change > 0.0 -> MarketGreen
        else -> MarketRed
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 112.dp)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = row.indicator.displayName,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = row.statusText(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End,
                    )
                    TextButton(onClick = onEditSchedule) {
                        Text("Alerts ${row.scheduleRules.count { it.enabled }}/${row.scheduleRules.size}")
                    }
                }
            }
            Text(
                text = quote?.let { MovementFormatter.value(it) } ?: "--",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = if (quote == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.End,
            )
            quote?.signalText()?.let { signalText ->
                Text(
                    text = signalText,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.End,
                )
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = quote?.let { "Updated ${timeFormatter.format(Date(it.observedAtEpochMillis))}" }
                        ?: "Waiting for first update",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = quote?.let { MovementFormatter.movement(it) } ?: "No data",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = movementColor,
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}

@Composable
private fun ScheduleRulesDialog(
    row: MarketRowState,
    onDismiss: () -> Unit,
    onAddFixedRule: (Indicator, String) -> Boolean,
    onAddRecurringRule: (Indicator, String, String, String, Int) -> Boolean,
    onToggleRule: (AlertScheduleRule) -> Unit,
    onDeleteRule: (String) -> Unit,
) {
    var fixedTime by remember(row.indicator) { mutableStateOf("09:00") }
    var rangeStart by remember(row.indicator) { mutableStateOf("09:00") }
    var rangeEnd by remember(row.indicator) { mutableStateOf("15:00") }
    var intervalValue by remember(row.indicator) { mutableStateOf("1") }
    var intervalUnit by remember(row.indicator) { mutableStateOf(IntervalUnit.HOUR) }
    var errorText by remember(row.indicator) { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${row.indicator.displayName} alerts") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                row.scheduleRules.forEach { rule ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(AlertScheduleFormatter.describe(rule), fontWeight = FontWeight.SemiBold)
                            Text(if (rule.enabled) "Enabled" else "Paused", style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(checked = rule.enabled, onCheckedChange = { onToggleRule(rule) })
                        TextButton(onClick = { onDeleteRule(rule.id) }) { Text("Delete") }
                    }
                }

                HorizontalDivider()
                Text("Add fixed time", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = fixedTime,
                        onValueChange = { fixedTime = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text("HH:mm:ss") },
                    )
                    Button(onClick = {
                        if (onAddFixedRule(row.indicator, fixedTime)) errorText = null else errorText = "Invalid fixed time"
                    }) { Text("Add") }
                }

                HorizontalDivider()
                Text("Add range", fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = rangeStart,
                    onValueChange = { rangeStart = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Start HH:mm:ss") },
                )
                OutlinedTextField(
                    value = rangeEnd,
                    onValueChange = { rangeEnd = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("End HH:mm:ss") },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = intervalValue,
                        onValueChange = { intervalValue = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text("Every") },
                    )
                    IntervalUnit.entries.forEach { unit ->
                        TextButton(onClick = { intervalUnit = unit }) {
                            Text(if (intervalUnit == unit) "[${unit.label}]" else unit.label)
                        }
                    }
                }
                Button(
                    onClick = {
                        if (onAddRecurringRule(row.indicator, rangeStart, rangeEnd, intervalValue, intervalUnit.seconds)) {
                            errorText = null
                        } else {
                            errorText = "Invalid range or interval"
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Add range rule") }
                errorText?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

private enum class IntervalUnit(val label: String, val seconds: Int) {
    SECOND("sec", 1),
    MINUTE("min", 60),
    HOUR("hour", 3600),
}

private fun MarketQuote.signalText(): String? {
    val parts = listOfNotNull(
        signalLabel,
        rsi?.let { "RSI 14: ${MovementFormatter.rsi(it)}" },
    )
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}

internal fun MarketRowState.statusText(): String =
    quote?.let { "${it.state.label} · ${it.source}" }
        ?: "${QuoteState.UNAVAILABLE.label} · ${indicator.sourceLabel}"

private val QuoteState.label: String
    get() = name.lowercase().replaceFirstChar { it.titlecase(Locale.US) }

private val timeFormatter = SimpleDateFormat("HH:mm", Locale.KOREA).apply {
    timeZone = TimeZone.getTimeZone("Asia/Seoul")
}
