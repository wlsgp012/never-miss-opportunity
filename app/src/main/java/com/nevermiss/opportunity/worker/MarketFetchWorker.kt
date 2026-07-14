package com.nevermiss.opportunity.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nevermiss.opportunity.AppGraph
import com.nevermiss.opportunity.data.RefreshResult
import com.nevermiss.opportunity.domain.MarketSchedule

class MarketFetchWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val slotId = inputData.getString(KEY_SLOT_ID) ?: return Result.failure()
        val slot = MarketSchedule.dailySlots.firstOrNull { it.id == slotId } ?: return Result.failure()
        val graph = AppGraph.get(applicationContext)
        val refresh = graph.repository.refreshIndicators(slot.indicators, slot.id)
        if (shouldPostNotification(refresh)) {
            graph.notifications.showMarketUpdate(refresh)
        }
        graph.scheduler.scheduleAll()
        // Partial successes are treated as success to avoid duplicate notifications on retry.
        return if (refresh.failures.any { it.retryable } && refresh.successes.isEmpty()) Result.retry() else Result.success()
    }

    companion object {
        const val KEY_SLOT_ID = "slot_id"

        fun shouldPostNotification(result: RefreshResult): Boolean = result.successes.isNotEmpty()
    }
}
