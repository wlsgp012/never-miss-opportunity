package com.nevermiss.opportunity.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.nevermiss.opportunity.domain.MarketSchedule
import java.time.ZonedDateTime

class MarketAlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun scheduleAll(now: ZonedDateTime = ZonedDateTime.now(MarketSchedule.zoneId)) {
        MarketSchedule.dailySlots.forEach { slot ->
            val triggerAt = nextOccurrence(now, slot.time).toInstant().toEpochMilli()
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pendingIntent(slot.id),
            )
        }
    }

    private fun pendingIntent(slotId: String): PendingIntent {
        val intent = Intent(context, MarketAlarmReceiver::class.java).putExtra(MarketAlarmReceiver.EXTRA_SLOT_ID, slotId)
        return PendingIntent.getBroadcast(context, slotId.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    companion object {
        fun nextOccurrence(now: ZonedDateTime, time: java.time.LocalTime): ZonedDateTime {
            val today = now.toLocalDate().atTime(time).atZone(MarketSchedule.zoneId)
            return if (today.isAfter(now)) today else today.plusDays(1)
        }
    }
}
