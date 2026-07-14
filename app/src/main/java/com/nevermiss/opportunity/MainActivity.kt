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
                        return MarketDashboardViewModel(graph.repository) as T
                    }
                },
            )
            val state by viewModel.uiState.collectAsState()
            val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

            MarketAlertTheme {
                MarketDashboardScreen(
                    state = state,
                    notificationsEnabled = graph.notifications.canPostNotifications(),
                    onEnableNotifications = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    onRefresh = viewModel::refreshAll,
                )
            }
        }
    }
}
