package com.example.duedate.model

import java.time.DayOfWeek
import java.time.LocalDate

data class DueDateResult(
    val startDate: LocalDate,
    val duration: Long,
    val unit: DurationUnit,
    val dueDate: LocalDate,
    /** Elapsed calendar days from [startDate] to [dueDate] (ChronoUnit.DAYS.between). */
    val actualCalendarDays: Long,
    val dueDayOfWeek: DayOfWeek,
    val crossedMonth: Boolean,
    val crossedYear: Boolean,
    val crossedLeapDay: Boolean,
    val includeStartDate: Boolean
) {
    val isSameDate: Boolean get() = startDate == dueDate

    /**
     * Number of calendar days counted when the start date is included (start and due date both
     * counted). Only meaningful when [includeStartDate] is true and [duration] is greater than zero.
     */
    val inclusiveCalendarDays: Long get() = if (duration == 0L) 0L else actualCalendarDays + 1

    fun matches(startDate: LocalDate, duration: Long, unit: DurationUnit, includeStartDate: Boolean): Boolean =
        this.startDate == startDate && this.duration == duration &&
            this.unit == unit && this.includeStartDate == includeStartDate
}
