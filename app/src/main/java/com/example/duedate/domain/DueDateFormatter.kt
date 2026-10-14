package com.example.duedate.domain

import com.example.duedate.model.DueDateHistory
import com.example.duedate.model.DueDateResult
import com.example.duedate.model.DurationUnit
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.SignStyle
import java.time.format.TextStyle
import java.time.temporal.ChronoField
import java.util.Locale

/** Turns calculation results into display, copy and share text. Contains no date arithmetic. */
class DueDateFormatter(private val locale: Locale = Locale.ENGLISH) {

    private val longDate: DateTimeFormatter = dateFormatter("MMMM")
    private val shortDate: DateTimeFormatter = dateFormatter("MMM")

    private fun dateFormatter(monthPattern: String): DateTimeFormatter =
        DateTimeFormatterBuilder()
            .appendValue(ChronoField.DAY_OF_MONTH)
            .appendLiteral(' ')
            .appendPattern(monthPattern)
            .appendLiteral(' ')
            .appendValue(ChronoField.YEAR, 4, 10, SignStyle.NORMAL)
            .toFormatter(locale)

    fun formatDate(date: LocalDate): String = longDate.format(date)

    fun formatShortDate(date: LocalDate): String = shortDate.format(date)

    fun formatDayOfWeek(day: DayOfWeek): String = day.getDisplayName(TextStyle.FULL, locale)

    fun formatDuration(duration: Long, unit: DurationUnit): String {
        val singular = when (unit) {
            DurationUnit.DAYS -> "day"
            DurationUnit.WEEKS -> "week"
            DurationUnit.MONTHS -> "month"
            DurationUnit.YEARS -> "year"
        }
        val number = String.format(locale, "%,d", duration)
        return if (duration == 1L) "$number $singular" else "$number ${singular}s"
    }

    fun formatUnit(unit: DurationUnit): String = when (unit) {
        DurationUnit.DAYS -> "Days"
        DurationUnit.WEEKS -> "Weeks"
        DurationUnit.MONTHS -> "Months"
        DurationUnit.YEARS -> "Years"
    }

    fun formatCount(value: Long): String = String.format(locale, "%,d", value)

    /** Short badge describing how the due date relates to the start, e.g. "30 DAYS AFTER START". */
    fun formatHeadline(result: DueDateResult): String = when {
        result.duration == 0L -> "SAME DATE"
        result.includeStartDate -> "START DATE INCLUDED"
        else -> "${formatDuration(result.duration, result.unit).uppercase(locale)} AFTER START"
    }

    /** e.g. "30 days after" or "1 day, start date included". */
    fun formatRelation(result: DueDateResult): String {
        val duration = formatDuration(result.duration, result.unit)
        return if (result.includeStartDate && result.duration > 0) {
            "$duration, counting from"
        } else {
            "$duration after"
        }
    }

    /** One-line natural description, e.g. "30 days after 14 October 2026." */
    fun formatDescription(result: DueDateResult): String {
        val duration = formatDuration(result.duration, result.unit)
        val start = formatDate(result.startDate)
        return when {
            result.includeStartDate && result.duration == 0L ->
                "A zero duration contains no days, so the due date is the start date, $start."
            result.includeStartDate ->
                "$duration counting $start as the first day."
            else -> "$duration after $start."
        }
    }

    fun formatCountingMode(includeStartDate: Boolean): String =
        if (includeStartDate) "Start date counted as day 1" else "Start date is day 0"

    fun formatCalendarDays(result: DueDateResult): String {
        val days = result.actualCalendarDays
        return if (days == 1L) "1 calendar day" else "${formatCount(days)} calendar days"
    }

    fun formatHistoryDuration(entry: DueDateHistory): String {
        val base = "+${formatDuration(entry.duration, entry.unit)}"
        return if (entry.includeStartDate) "$base · start included" else base
    }

    fun copyText(result: DueDateResult): String = buildString {
        appendLine("DueDate Result")
        appendLine()
        appendLine("Start date: ${formatDate(result.startDate)}")
        appendLine("Duration: ${formatDuration(result.duration, result.unit)}")
        if (result.includeStartDate) appendLine("Start date included: Yes (counted as day 1)")
        appendLine("Expected due date: ${formatDate(result.dueDate)}")
        appendLine("Day: ${formatDayOfWeek(result.dueDayOfWeek)}")
        if (result.includeStartDate) {
            appendLine("Calendar days elapsed: ${formatCount(result.actualCalendarDays)}")
            append("Days counted including start: ${formatCount(result.inclusiveCalendarDays)}")
        } else {
            append("Calendar days: ${formatCount(result.actualCalendarDays)}")
        }
    }

    fun shareText(result: DueDateResult): String = buildString {
        appendLine("DueDate")
        appendLine()
        appendLine(formatDate(result.startDate))
        append("+ ${formatDuration(result.duration, result.unit)}")
        if (result.includeStartDate) append(" (start date included)")
        appendLine()
        appendLine()
        appendLine("Expected due date:")
        appendLine(formatDate(result.dueDate))
        appendLine()
        appendLine(formatDayOfWeek(result.dueDayOfWeek))
        if (result.includeStartDate) {
            append("${formatCalendarDays(result)} elapsed · ${formatCount(result.inclusiveCalendarDays)} days counted")
        } else {
            append(formatCalendarDays(result))
        }
    }

    fun resultAccessibilityText(result: DueDateResult): String =
        "Expected due date, ${formatDate(result.dueDate)}, ${formatDayOfWeek(result.dueDayOfWeek)}."
}
