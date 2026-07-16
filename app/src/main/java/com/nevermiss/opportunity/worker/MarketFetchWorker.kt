package com.nevermiss.opportunity.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nevermiss.opportunity.AppGraph
import com.nevermiss.opportunity.data.RefreshResult
import com.nevermiss.opportunity.domain.MarketSchedule
import com.nevermiss.opportunity.domain.QuoteState
import java.time.ZonedDateTime

class MarketFetchWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val slotId = inputData.getString(KEY_SLOT_ID) ?: return Result.failure()
        val graph = AppGraph.get(applicationContext)
        val rule = graph.scheduleStore.rule(slotId) ?: return Result.failure()
        val now = ZonedDateTime.now(MarketSchedule.zoneId)
        if (!MarketSchedule.isAlertDay(now)) {
            graph.scheduler.scheduleAll(now)
            return Result.success()
        }
        val refresh = graph.repository.refreshIndicators(listOf(rule.indicator), rule.id)
        if (shouldPostNotification(refresh, now)) {
            graph.notifications.showMarketUpdate(refresh)
        }
        graph.scheduler.scheduleAll()
        // Partial successes are treated as success to avoid duplicate notifications on retry.
        return if (shouldRetry(refresh)) Result.retry() else Result.success()
    }

    companion object {
        const val KEY_SLOT_ID = "slot_id"

        fun shouldPostNotification(
            result: RefreshResult,
            now: ZonedDateTime = ZonedDateTime.now(MarketSchedule.zoneId),
        ): Boolean =
            MarketSchedule.isAlertDay(now) && result.successes.any { it.state == QuoteState.FRESH }

        fun shouldRetry(result: RefreshResult): Boolean =
            result.successes.isEmpty() && result.failures.any { it.retryable }
    }
}
