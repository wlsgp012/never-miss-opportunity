package com.nevermiss.opportunity.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fetch_logs")
data class FetchLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val slotId: String,
    val startedAtEpochMillis: Long,
    val completedAtEpochMillis: Long,
    val successCount: Int,
    val failureCount: Int,
    val errorSummary: String,
)
