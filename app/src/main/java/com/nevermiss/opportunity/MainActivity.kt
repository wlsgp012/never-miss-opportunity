package com.nevermiss.opportunity

import android.Manifest
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nevermiss.opportunity.ui.MarketDashboardScreen
import com.nevermiss.opportunity.ui.MarketDashboardViewModel
import com.nevermiss.opportunity.ui.theme.MarketAlertTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val graph = AppGraph.get(this)
        setContent {
            val viewModel: MarketDashboardViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return MarketDashboardViewModel(graph.repository, graph.scheduleStore, graph.scheduler) as T
                    }
                },
            )
            val state by viewModel.uiState.collectAsState()
            var notificationsEnabled by remember { mutableStateOf(graph.notifications.canPostNotifications()) }
            var pendingExportJson by remember { mutableStateOf<String?>(null) }
            val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
                notificationsEnabled = graph.notifications.canPostNotifications()
            }
            val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
                val json = pendingExportJson
                pendingExportJson = null
                if (uri != null && json != null) {
                    val exported = writeText(uri, json)
                    Toast.makeText(this, if (exported) "Schedule exported" else "Export failed", Toast.LENGTH_SHORT).show()
                }
            }
            val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                if (uri != null) {
                    val imported = readText(uri)?.let(viewModel::importScheduleJson) == true
                    Toast.makeText(this, if (imported) "Schedule imported" else "Import failed", Toast.LENGTH_SHORT).show()
                }
            }

            MarketAlertTheme {
                MarketDashboardScreen(
                    state = state,
                    notificationsEnabled = notificationsEnabled,
                    onEnableNotifications = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    onRefresh = viewModel::refreshAll,
                    onExportSchedules = {
                        pendingExportJson = viewModel.exportScheduleJson()
                        exportLauncher.launch("market-alert-schedules.json")
                    },
                    onImportSchedules = {
                        importLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                    },
                    onAddFixedRule = viewModel::addFixedRule,
                    onAddRecurringRule = viewModel::addRecurringRule,
                    onToggleRule = viewModel::toggleRule,
                    onDeleteRule = viewModel::deleteRule,
                )
            }
        }
    }

    private fun writeText(uri: Uri, text: String): Boolean =
        runCatching {
            contentResolver.openOutputStream(uri)?.use { output ->
                output.write(text.toByteArray(Charsets.UTF_8))
            } != null
        }.getOrDefault(false)

    private fun readText(uri: Uri): String? =
        runCatching {
            contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
        }.getOrNull()
}
