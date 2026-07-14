package com.nevermiss.opportunity.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nevermiss.opportunity.AppGraph

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> AppGraph.get(context).scheduler.scheduleAll()
        }
    }
}
