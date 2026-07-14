package com.nevermiss.opportunity

import android.app.Application

class MarketAlertApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val graph = AppGraph.get(this)
        graph.notifications.createChannel()
        graph.scheduler.scheduleAll()
    }
}
