package com.nevermiss.opportunity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Text
import com.nevermiss.opportunity.ui.theme.MarketAlertTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MarketAlertTheme {
                Text("Market Alerts")
            }
        }
    }
}
