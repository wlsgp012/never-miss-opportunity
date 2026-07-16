package com.nevermiss.opportunity.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.nevermiss.opportunity.data.local.AlertScheduleStore
import com.nevermiss.opportunity.domain.AlertScheduleRule
import com.nevermiss.opportunity.domain.MarketSchedule
import java.time.ZonedDateTime

internal fun interface AlarmSetter {
    fun schedule(slotId: String, triggerAtEpochMillis: Long)
}

class MarketAlarmScheduler internal constructor(
    private val alarmSetter: AlarmSetter,
    private val rulesProvider: () -> List<AlertScheduleRule>,
) {
    constructor(context: Context, scheduleStore: AlertScheduleStore) : this(AndroidAlarmSetter(context), scheduleStore::rules)

    fun scheduleAll(now: ZonedDateTime = ZonedDateTime.now(MarketSchedule.zoneId)) {
        rulesProvider().forEach { rule ->
            scheduleRule(rule, now)
        }
    }

    fun scheduleSlot(slotId: String, now: ZonedDateTime = ZonedDateTime.now(MarketSchedule.zoneId)) {
        val rule = rulesProvider().firstOrNull { it.id == slotId } ?: return
        scheduleRule(rule, now)
    }

    private fun scheduleRule(rule: AlertScheduleRule, now: ZonedDateTime) {
        val triggerAt = rule.nextOccurrence(now)?.toInstant()?.toEpochMilli() ?: return
        alarmSetter.schedule(rule.id, triggerAt)
    }

    companion object {
        fun nextOccurrence(now: ZonedDateTime, time: java.time.LocalTime): ZonedDateTime {
            val today = now.toLocalDate().atTime(time).atZone(MarketSchedule.zoneId)
            val candidate = if (today.isAfter(now)) today else today.plusDays(1)
            return MarketSchedule.nextAlertDateTime(candidate)
        }
    }
}

private class AndroidAlarmSetter(context: Context) : AlarmSetter {
    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(AlarmManager::class.java)

    override fun schedule(slotId: String, triggerAtEpochMillis: Long) {
        val operation = pendingIntent(slotId)
        if (canScheduleExactAlarm()) {
            runCatching {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtEpochMillis,
                    operation,
                )
            }.getOrElse {
                scheduleAllowWhileIdle(triggerAtEpochMillis, operation)
            }
        } else {
            scheduleAllowWhileIdle(triggerAtEpochMillis, operation)
        }
    }

    private fun scheduleAllowWhileIdle(triggerAtEpochMillis: Long, operation: PendingIntent) {
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtEpochMillis,
            operation,
        )
    }

    private fun canScheduleExactAlarm(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    private fun pendingIntent(slotId: String): PendingIntent {
        val intent = Intent(appContext, MarketAlarmReceiver::class.java)
            .putExtra(MarketAlarmReceiver.EXTRA_SLOT_ID, slotId)
        return PendingIntent.getBroadcast(
            appContext,
            slotId.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}
