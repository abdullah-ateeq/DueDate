package com.example.duedate.model

import java.time.LocalDate

data class DueDateHistory(
    val id: String,
    val startDate: LocalDate,
    val duration: Long,
    val unit: DurationUnit,
    val dueDate: LocalDate,
    val includeStartDate: Boolean,
    val calculatedAt: Long
) {
    fun hasSameConfiguration(other: DueDateHistory): Boolean =
        startDate == other.startDate && duration == other.duration &&
            unit == other.unit && includeStartDate == other.includeStartDate
}
