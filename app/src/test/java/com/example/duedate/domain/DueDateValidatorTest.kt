package com.example.duedate.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class DueDateValidatorTest {

    private fun invalid(message: String) = DurationValidation.Invalid(message)

    @Test fun emptyDuration() {
        assertEquals(invalid(DueDateValidator.ERROR_EMPTY), DueDateValidator.validateDuration(""))
        assertEquals(invalid(DueDateValidator.ERROR_EMPTY), DueDateValidator.validateDuration("   "))
    }

    @Test fun zeroIsValid() = assertEquals(DurationValidation.Valid(0), DueDateValidator.validateDuration("0"))

    @Test fun wholeNumberIsValid() = assertEquals(DurationValidation.Valid(30), DueDateValidator.validateDuration("30"))

    @Test fun surroundingWhitespaceIsIgnored() =
        assertEquals(DurationValidation.Valid(30), DueDateValidator.validateDuration(" 30 "))

    @Test fun negativeDuration() =
        assertEquals(invalid(DueDateValidator.ERROR_NEGATIVE), DueDateValidator.validateDuration("-5"))

    @Test fun decimalDuration() {
        assertEquals(invalid(DueDateValidator.ERROR_NOT_WHOLE), DueDateValidator.validateDuration("1.5"))
        assertEquals(invalid(DueDateValidator.ERROR_NOT_WHOLE), DueDateValidator.validateDuration("1,000"))
        assertEquals(invalid(DueDateValidator.ERROR_NOT_WHOLE), DueDateValidator.validateDuration("-1.5"))
    }

    @Test fun nonNumericDuration() {
        listOf("abc", "12a", "+5", "1e3", "#", "٣", "3 0").forEach {
            assertEquals("input '$it'", invalid(DueDateValidator.ERROR_NOT_WHOLE), DueDateValidator.validateDuration(it))
        }
    }

    @Test fun maximumDurationIsAllowed() =
        assertEquals(DurationValidation.Valid(1_000_000), DueDateValidator.validateDuration("1000000"))

    @Test fun overLimitDuration() {
        assertEquals(invalid(DueDateValidator.ERROR_TOO_LARGE), DueDateValidator.validateDuration("1000001"))
        assertEquals(invalid(DueDateValidator.ERROR_TOO_LARGE), DueDateValidator.validateDuration("99999999999999999999"))
    }
}
