package com.nevermiss.opportunity.scheduler

import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.nevermiss.opportunity.worker.MarketFetchWorker

class MarketAlarmReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val slotId = intent.getStringExtra(EXTRA_SLOT_ID) ?: return
        val request = OneTimeWorkRequestBuilder<MarketFetchWorker>()
            .setInputData(workDataOf(MarketFetchWorker.KEY_SLOT_ID to slotId))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork("market-fetch-$slotId", ExistingWorkPolicy.REPLACE, request)
    }

    companion object {
        const val EXTRA_SLOT_ID = "slot_id"
    }
}
