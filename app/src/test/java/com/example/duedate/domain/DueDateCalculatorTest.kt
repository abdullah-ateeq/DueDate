package com.example.duedate.domain

import com.example.duedate.model.DurationUnit
import com.example.duedate.model.DurationUnit.DAYS
import com.example.duedate.model.DurationUnit.MONTHS
import com.example.duedate.model.DurationUnit.WEEKS
import com.example.duedate.model.DurationUnit.YEARS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DateTimeException
import java.time.DayOfWeek
import java.time.LocalDate

class DueDateCalculatorTest {

    private fun date(year: Int, month: Int, day: Int) = LocalDate.of(year, month, day)

    private fun due(start: LocalDate, duration: Long, unit: DurationUnit, include: Boolean = false) =
        DueDateCalculator.calculate(start, duration, unit, include).dueDate

    // region Days

    @Test fun zeroDaysReturnsSameDate() {
        val result = DueDateCalculator.calculate(date(2026, 10, 14), 0, DAYS)
        assertEquals(date(2026, 10, 14), result.dueDate)
        assertTrue(result.isSameDate)
        assertEquals(0, result.actualCalendarDays)
    }

    @Test fun oneDayTreatsStartAsDayZero() = assertEquals(date(2026, 10, 2), due(date(2026, 10, 1), 1, DAYS))

    @Test fun sevenDays() = assertEquals(date(2026, 10, 8), due(date(2026, 10, 1), 7, DAYS))

    @Test fun thirtyDaysCriticalExample() = assertEquals(date(2026, 11, 13), due(date(2026, 10, 14), 30, DAYS))

    @Test fun twoDaysIncludeStartOff() = assertEquals(date(2026, 10, 3), due(date(2026, 10, 1), 2, DAYS))

    @Test fun largeDayCountUsesPlusDays() {
        val start = date(2026, 1, 1)
        val result = due(start, 1000, DAYS)
        assertEquals(start.plusDays(1000), result)
        // 2028 is a leap year, so the true answer differs from a naive 365-day-year approximation.
        assertEquals(date(2028, 9, 27), result)
        assertNotEquals(date(2028, 9, 28), result)
    }

    // endregion

    // region Weeks

    @Test fun oneWeek() = assertEquals(date(2026, 10, 8), due(date(2026, 10, 1), 1, WEEKS))

    @Test fun fourWeeks() = assertEquals(date(2026, 11, 11), due(date(2026, 10, 14), 4, WEEKS))

    // endregion

    // region Months

    @Test fun oneMonth() = assertEquals(date(2026, 11, 14), due(date(2026, 10, 14), 1, MONTHS))

    @Test fun threeMonths() = assertEquals(date(2027, 1, 14), due(date(2026, 10, 14), 3, MONTHS))

    @Test fun threeMonthsFromMonthEndIsCalendarAwareNotNinetyDays() {
        val start = date(2026, 10, 31)
        val result = due(start, 3, MONTHS)
        assertEquals(date(2027, 1, 31), result)
        assertNotEquals(start.plusDays(90), result)
    }

    @Test fun january31PlusOneMonthClampsToFebruary28() =
        assertEquals(date(2026, 2, 28), due(date(2026, 1, 31), 1, MONTHS))

    @Test fun january31PlusOneMonthInLeapYearIsFebruary29() =
        assertEquals(date(2024, 2, 29), due(date(2024, 1, 31), 1, MONTHS))

    @Test fun february28PlusOneMonthKeepsDay28() =
        assertEquals(date(2026, 3, 28), due(date(2026, 2, 28), 1, MONTHS))

    @Test fun april30PlusOneMonth() = assertEquals(date(2026, 5, 30), due(date(2026, 4, 30), 1, MONTHS))

    @Test fun may31PlusOneMonthClampsToJune30() = assertEquals(date(2026, 6, 30), due(date(2026, 5, 31), 1, MONTHS))

    @Test fun december31PlusOneMonth() = assertEquals(date(2027, 1, 31), due(date(2026, 12, 31), 1, MONTHS))

    @Test fun monthEndIsNotChainedFromClampedValue() {
        // A single +2 months from 31 Jan lands on 31 Mar, not 28 Mar (no stepwise drift).
        assertEquals(date(2026, 3, 31), due(date(2026, 1, 31), 2, MONTHS))
    }

    @Test fun largeMonthCountIsTenCalendarYears() =
        assertEquals(date(2036, 1, 1), due(date(2026, 1, 1), 120, MONTHS))

    // endregion

    // region Years

    @Test fun oneYear() = assertEquals(date(2027, 10, 14), due(date(2026, 10, 14), 1, YEARS))

    @Test fun multipleYears() = assertEquals(date(2028, 10, 14), due(date(2026, 10, 14), 2, YEARS))

    @Test fun december31PlusOneYear() = assertEquals(date(2027, 12, 31), due(date(2026, 12, 31), 1, YEARS))

    @Test fun leapDayPlusOneYearClampsToFebruary28() =
        assertEquals(date(2025, 2, 28), due(date(2024, 2, 29), 1, YEARS))

    @Test fun leapDayPlusFourYearsReturnsToFebruary29() =
        assertEquals(date(2028, 2, 29), due(date(2024, 2, 29), 4, YEARS))

    @Test fun leapDayPlusHundredYearsHandlesNonLeapCentury() {
        // 2124 is a leap year; 2100 is not (divisible by 100 but not 400).
        assertEquals(date(2124, 2, 29), due(date(2024, 2, 29), 100, YEARS))
        assertEquals(date(2100, 2, 28), due(date(2096, 2, 29), 4, YEARS))
    }

    @Test fun largeYearCount() = assertEquals(date(2126, 1, 1), due(date(2026, 1, 1), 100, YEARS))

    // endregion

    // region Result details

    @Test fun dayOfWeekResult() {
        val result = DueDateCalculator.calculate(date(2026, 10, 14), 30, DAYS)
        assertEquals(DayOfWeek.FRIDAY, result.dueDayOfWeek)
    }

    @Test fun calendarDayDifferenceForMonths() {
        val result = DueDateCalculator.calculate(date(2026, 10, 14), 3, MONTHS)
        assertEquals(92, result.actualCalendarDays) // Oct 14 -> Jan 14 spans 17 + 30 + 31 + 14
    }

    @Test fun calendarDayDifferenceForDays() {
        assertEquals(30, DueDateCalculator.calculate(date(2026, 10, 14), 30, DAYS).actualCalendarDays)
    }

    @Test fun monthCrossing() {
        val result = DueDateCalculator.calculate(date(2026, 9, 30), 3, DAYS)
        assertEquals(date(2026, 10, 3), result.dueDate)
        assertTrue(result.crossedMonth)
        assertFalse(result.crossedYear)
    }

    @Test fun noMonthCrossingWithinMonth() {
        val result = DueDateCalculator.calculate(date(2026, 10, 1), 7, DAYS)
        assertFalse(result.crossedMonth)
        assertFalse(result.crossedYear)
        assertFalse(result.crossedLeapDay)
    }

    @Test fun yearCrossing() {
        val result = DueDateCalculator.calculate(date(2026, 12, 31), 1, DAYS)
        assertEquals(date(2027, 1, 1), result.dueDate)
        assertTrue(result.crossedYear)
        assertTrue(result.crossedMonth)
    }

    @Test fun sameMonthNameInDifferentYearCountsAsMonthChange() {
        val result = DueDateCalculator.calculate(date(2026, 10, 14), 1, YEARS)
        assertTrue(result.crossedMonth)
        assertTrue(result.crossedYear)
    }

    // endregion

    // region Leap-day detection

    @Test fun leapDayDetectedWhenRangeSpansFebruary29() =
        assertTrue(DueDateCalculator.calculate(date(2028, 2, 1), 30, DAYS).crossedLeapDay)

    @Test fun leapDayDetectedWhenStartIsFebruary29() =
        assertTrue(DueDateCalculator.calculate(date(2024, 2, 29), 1, DAYS).crossedLeapDay)

    @Test fun leapDayDetectedWhenEndIsFebruary29() {
        val result = DueDateCalculator.calculate(date(2024, 2, 1), 28, DAYS)
        assertEquals(date(2024, 2, 29), result.dueDate)
        assertTrue(result.crossedLeapDay)
    }

    @Test fun leapDayNotDetectedInNonLeapYear() =
        assertFalse(DueDateCalculator.calculate(date(2026, 2, 1), 60, DAYS).crossedLeapDay)

    @Test fun leapDayNotDetectedWhenRangeEndsBeforeFebruary29() =
        assertFalse(DueDateCalculator.calculate(date(2028, 1, 1), 30, DAYS).crossedLeapDay)

    @Test fun leapDayNotDetectedWhenRangeStartsAfterFebruary29() =
        assertFalse(DueDateCalculator.calculate(date(2028, 3, 1), 300, DAYS).crossedLeapDay)

    @Test fun leapDayDetectedAcrossMultipleYears() {
        assertTrue(DueDateCalculator.calculate(date(2025, 3, 1), 3, YEARS).crossedLeapDay)
        assertFalse(DueDateCalculator.calculate(date(2025, 3, 1), 2, YEARS).crossedLeapDay)
    }

    @Test fun leapDayDetectionHandlesCenturyAndEightYearGap() {
        assertFalse(DueDateCalculator.containsLeapDay(date(2100, 1, 1), date(2100, 12, 31)))
        assertTrue(DueDateCalculator.containsLeapDay(date(1896, 3, 1), date(1904, 3, 1)))
        assertFalse(DueDateCalculator.containsLeapDay(date(1896, 3, 1), date(1904, 2, 28)))
    }

    // endregion

    // region Include start date

    @Test fun includeStartOffIsDefault() {
        val result = DueDateCalculator.calculate(date(2026, 10, 1), 1, DAYS)
        assertFalse(result.includeStartDate)
        assertEquals(date(2026, 10, 2), result.dueDate)
    }

    @Test fun includeStartOneDayIsStartDate() {
        val result = DueDateCalculator.calculate(date(2026, 10, 1), 1, DAYS, includeStartDate = true)
        assertEquals(date(2026, 10, 1), result.dueDate)
        assertTrue(result.includeStartDate)
        assertEquals(0, result.actualCalendarDays)
        assertEquals(1, result.inclusiveCalendarDays)
    }

    @Test fun includeStartTwoDays() =
        assertEquals(date(2026, 10, 2), due(date(2026, 10, 1), 2, DAYS, include = true))

    @Test fun includeStartOneWeekEndsOnSixDayOffset() =
        assertEquals(date(2026, 10, 7), due(date(2026, 10, 1), 1, WEEKS, include = true))

    @Test fun includeStartOneMonthEndsDayBeforeCalendarMonth() =
        assertEquals(date(2026, 10, 31), due(date(2026, 10, 1), 1, MONTHS, include = true))

    @Test fun includeStartMonthAppliesClampBeforeAdjustment() =
        assertEquals(date(2026, 2, 27), due(date(2026, 1, 31), 1, MONTHS, include = true))

    @Test fun includeStartOneYear() =
        assertEquals(date(2027, 10, 13), due(date(2026, 10, 14), 1, YEARS, include = true))

    @Test fun includeStartZeroDurationReturnsStartDate() {
        val result = DueDateCalculator.calculate(date(2026, 10, 14), 0, DAYS, includeStartDate = true)
        assertEquals(date(2026, 10, 14), result.dueDate)
        assertTrue(result.isSameDate)
    }

    // endregion

    // region Errors

    @Test(expected = IllegalArgumentException::class)
    fun negativeDurationIsRejected() {
        DueDateCalculator.calculate(date(2026, 10, 14), -1, DAYS)
    }

    @Test fun negativeDurationReturnsNullFromSafeCalculation() =
        assertNull(DueDateCalculator.calculateOrNull(date(2026, 10, 14), -1, DAYS))

    @Test fun maximumAllowedDurationsDoNotOverflow() {
        val start = date(2026, 10, 14)
        for (unit in DurationUnit.entries) {
            val result = DueDateCalculator.calculateOrNull(start, DueDateValidator.MAX_DURATION, unit)
            assertTrue("$unit should be representable", result != null)
        }
    }

    @Test fun overflowReturnsNullInsteadOfCrashing() {
        assertNull(DueDateCalculator.calculateOrNull(date(2026, 1, 1), Long.MAX_VALUE, DAYS))
        assertNull(DueDateCalculator.calculateOrNull(date(2026, 1, 1), Long.MAX_VALUE, MONTHS))
        assertNull(DueDateCalculator.calculateOrNull(date(2026, 1, 1), 2_000_000_000L, YEARS))
        assertNull(DueDateCalculator.calculateOrNull(LocalDate.MAX.minusDays(10), 1_000_000, DAYS))
    }

    @Test(expected = DateTimeException::class)
    fun overflowThrowsFromUnsafeCalculation() {
        DueDateCalculator.calculate(LocalDate.MAX.minusDays(10), 1_000_000, DAYS)
    }

    // endregion
}
