package com.nevermiss.opportunity

import android.Manifest
import android.os.Build
import android.os.Bundle
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
            val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
                notificationsEnabled = graph.notifications.canPostNotifications()
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
                    onAddFixedRule = viewModel::addFixedRule,
                    onAddRecurringRule = viewModel::addRecurringRule,
                    onToggleRule = viewModel::toggleRule,
                    onDeleteRule = viewModel::deleteRule,
                )
            }
        }
    }
}
