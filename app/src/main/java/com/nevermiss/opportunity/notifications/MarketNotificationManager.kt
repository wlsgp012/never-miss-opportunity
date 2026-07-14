package com.nevermiss.opportunity.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.nevermiss.opportunity.MainActivity
import com.nevermiss.opportunity.R
import com.nevermiss.opportunity.data.RefreshResult
import com.nevermiss.opportunity.domain.MovementFormatter

class MarketNotificationManager(private val context: Context) {
    fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Market Alerts", NotificationManager.IMPORTANCE_DEFAULT)
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    fun canPostNotifications(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun showMarketUpdate(result: RefreshResult) {
        if (!canPostNotifications() || result.successes.isEmpty()) return

        val body = result.successes.joinToString(separator = "\n") { quote ->
            "${quote.indicator.displayName} ${MovementFormatter.value(quote)} ${MovementFormatter.movement(quote)}"
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_market)
            .setContentTitle("Market update")
            .setContentText(result.successes.first().indicator.displayName)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(result.slotId.hashCode(), notification)
    }

    companion object {
        const val CHANNEL_ID = "market_alerts"
    }
}
