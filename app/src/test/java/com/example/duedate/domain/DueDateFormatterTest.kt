package com.example.duedate.domain

import com.example.duedate.model.DueDateHistory
import com.example.duedate.model.DurationUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

class DueDateFormatterTest {

    private val formatter = DueDateFormatter(Locale.ENGLISH)
    private val start = LocalDate.of(2026, 10, 14)

    @Test fun formatsDurations() {
        assertEquals("30 days", formatter.formatDuration(30, DurationUnit.DAYS))
        assertEquals("1 day", formatter.formatDuration(1, DurationUnit.DAYS))
        assertEquals("4 weeks", formatter.formatDuration(4, DurationUnit.WEEKS))
        assertEquals("3 months", formatter.formatDuration(3, DurationUnit.MONTHS))
        assertEquals("1 year", formatter.formatDuration(1, DurationUnit.YEARS))
        assertEquals("1,000,000 days", formatter.formatDuration(1_000_000, DurationUnit.DAYS))
    }

    @Test fun formatsDates() {
        assertEquals("14 October 2026", formatter.formatDate(start))
        assertEquals("14 Oct 2026", formatter.formatShortDate(start))
        assertEquals("1 January 12026", formatter.formatDate(LocalDate.of(12026, 1, 1)))
    }

    @Test fun descriptionAndHeadline() {
        val result = DueDateCalculator.calculate(start, 30, DurationUnit.DAYS)
        assertEquals("30 days after 14 October 2026.", formatter.formatDescription(result))
        assertEquals("30 DAYS AFTER START", formatter.formatHeadline(result))
        assertEquals("30 calendar days", formatter.formatCalendarDays(result))
    }

    @Test fun sameDateHeadline() {
        val result = DueDateCalculator.calculate(start, 0, DurationUnit.DAYS)
        assertEquals("SAME DATE", formatter.formatHeadline(result))
        assertEquals("0 days after 14 October 2026.", formatter.formatDescription(result))
    }

    @Test fun includeStartHeadlineAndDescription() {
        val result = DueDateCalculator.calculate(LocalDate.of(2026, 10, 1), 1, DurationUnit.DAYS, true)
        assertEquals("START DATE INCLUDED", formatter.formatHeadline(result))
        assertEquals("1 day counting 1 October 2026 as the first day.", formatter.formatDescription(result))
    }

    @Test fun copyFormatting() {
        val result = DueDateCalculator.calculate(start, 30, DurationUnit.DAYS)
        val expected = """
            DueDate Result

            Start date: 14 October 2026
            Duration: 30 days
            Expected due date: 13 November 2026
            Day: Friday
            Calendar days: 30
        """.trimIndent()
        assertEquals(expected, formatter.copyText(result))
    }

    @Test fun copyFormattingDistinguishesInclusiveCounting() {
        val result = DueDateCalculator.calculate(LocalDate.of(2026, 10, 1), 2, DurationUnit.DAYS, true)
        val text = formatter.copyText(result)
        assertTrue(text.contains("Start date included: Yes"))
        assertTrue(text.contains("Calendar days elapsed: 1"))
        assertTrue(text.contains("Days counted including start: 2"))
    }

    @Test fun shareFormatting() {
        val result = DueDateCalculator.calculate(start, 30, DurationUnit.DAYS)
        val expected = """
            DueDate

            14 October 2026
            + 30 days

            Expected due date:
            13 November 2026

            Friday
            30 calendar days
        """.trimIndent()
        assertEquals(expected, formatter.shareText(result))
    }

    @Test fun historyFormatting() {
        val entry = DueDateHistory("id", start, 30, DurationUnit.DAYS, LocalDate.of(2026, 11, 13), false, 0)
        assertEquals("14 Oct 2026", formatter.formatShortDate(entry.startDate))
        assertEquals("+30 days", formatter.formatHistoryDuration(entry))
        assertEquals("13 Nov 2026", formatter.formatShortDate(entry.dueDate))
        assertEquals("+30 days · start included", formatter.formatHistoryDuration(entry.copy(includeStartDate = true)))
    }

    @Test fun accessibilityText() {
        val result = DueDateCalculator.calculate(start, 30, DurationUnit.DAYS)
        assertEquals("Expected due date, 13 November 2026, Friday.", formatter.resultAccessibilityText(result))
    }
}
