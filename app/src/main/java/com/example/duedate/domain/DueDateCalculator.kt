package com.example.duedate.domain

import com.example.duedate.model.DueDateResult
import com.example.duedate.model.DurationUnit
import java.time.DateTimeException
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/**
 * Calculates an expected due date from a start date and a duration.
 *
 * Every unit uses its own calendar-aware java.time operation; durations are never converted
 * into days (a month is not 30 days, a year is not 365 days):
 *  - DAYS   -> LocalDate.plusDays
 *  - WEEKS  -> LocalDate.plusWeeks
 *  - MONTHS -> LocalDate.plusMonths
 *  - YEARS  -> LocalDate.plusYears
 *
 * End-of-month policy (delegated to java.time): when the target month is shorter than the start
 * day, the result is clamped to the last valid day of that month. 31 Jan 2026 + 1 month is
 * 28 Feb 2026, 31 Jan 2024 + 1 month is 29 Feb 2024, and 29 Feb 2024 + 1 year is 28 Feb 2025.
 *
 * Include-start-date policy:
 *  - OFF (default): the start date is day zero. Due date = start + duration.
 *    1 Oct + 1 day = 2 Oct.
 *  - ON: the start date is counted as the first day of the period, so the due date is the last
 *    day of a period that begins on the start date: (start + duration) - 1 day. This one rule is
 *    applied to every unit, so inclusive counting is never mixed with elapsed counting:
 *      1 Oct + 1 day    = 1 Oct
 *      1 Oct + 2 days   = 2 Oct
 *      1 Oct + 1 week   = 7 Oct  (seven days, the first being 1 Oct)
 *      1 Oct + 1 month  = 31 Oct (the month that starts on 1 Oct ends on 31 Oct)
 *      14 Oct 2026 + 1 year = 13 Oct 2027
 *    The month/year step still uses plusMonths/plusYears before the one-day adjustment, so the
 *    clamping above applies first (31 Jan 2026 + 1 month inclusive = 27 Feb 2026).
 *  - ON with a zero duration: a zero-length period contains no days, so the start date is
 *    returned unchanged.
 */
object DueDateCalculator {

    /**
     * @throws IllegalArgumentException if [duration] is negative.
     * @throws DateTimeException or [ArithmeticException] if the result is outside the LocalDate range.
     */
    fun calculate(
        startDate: LocalDate,
        duration: Long,
        unit: DurationUnit,
        includeStartDate: Boolean = false
    ): DueDateResult {
        require(duration >= 0) { "Duration cannot be negative." }
        val dueDate = dueDate(startDate, duration, unit, includeStartDate)
        return DueDateResult(
            startDate = startDate,
            duration = duration,
            unit = unit,
            dueDate = dueDate,
            actualCalendarDays = ChronoUnit.DAYS.between(startDate, dueDate),
            dueDayOfWeek = dueDate.dayOfWeek,
            crossedMonth = YearMonth.from(startDate) != YearMonth.from(dueDate),
            crossedYear = startDate.year != dueDate.year,
            crossedLeapDay = containsLeapDay(startDate, dueDate),
            includeStartDate = includeStartDate
        )
    }

    /** Same as [calculate] but returns null instead of throwing when the date cannot be represented. */
    fun calculateOrNull(
        startDate: LocalDate,
        duration: Long,
        unit: DurationUnit,
        includeStartDate: Boolean = false
    ): DueDateResult? = try {
        calculate(startDate, duration, unit, includeStartDate)
    } catch (_: DateTimeException) {
        null
    } catch (_: ArithmeticException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }

    private fun dueDate(
        startDate: LocalDate,
        duration: Long,
        unit: DurationUnit,
        includeStartDate: Boolean
    ): LocalDate {
        val elapsedEnd = when (unit) {
            DurationUnit.DAYS -> startDate.plusDays(duration)
            DurationUnit.WEEKS -> startDate.plusWeeks(duration)
            DurationUnit.MONTHS -> startDate.plusMonths(duration)
            DurationUnit.YEARS -> startDate.plusYears(duration)
        }
        return if (includeStartDate && duration > 0) elapsedEnd.minusDays(1) else elapsedEnd
    }

    /**
     * True when 29 February falls anywhere in the closed range between [a] and [b], including
     * when either endpoint is itself 29 February.
     */
    fun containsLeapDay(a: LocalDate, b: LocalDate): Boolean {
        val from = minOf(a, b)
        val to = maxOf(a, b)
        // Leap years are at most 8 years apart (e.g. 1896 -> 1904), so the first 29 February on
        // or after `from` is always found within 9 candidate years.
        var year = from.year
        repeat(9) {
            if (year > Year.MAX_VALUE) return false
            if (Year.isLeap(year.toLong())) {
                val leapDay = LocalDate.of(year, 2, 29)
                if (!leapDay.isBefore(from)) return !leapDay.isAfter(to)
            }
            year++
        }
        return false
    }
}
