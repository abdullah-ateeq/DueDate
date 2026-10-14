package com.example.duedate.domain

sealed interface DurationValidation {
    data class Valid(val duration: Long) : DurationValidation
    data class Invalid(val message: String) : DurationValidation
}

object DueDateValidator {
    const val MAX_DURATION = 1_000_000L

    const val ERROR_EMPTY = "Enter a duration."
    const val ERROR_NOT_WHOLE = "Enter a whole number."
    const val ERROR_NEGATIVE = "Duration cannot be negative."
    const val ERROR_TOO_LARGE = "Duration is too large."
    const val ERROR_INVALID_DATE = "Unable to calculate a valid date from this duration."

    private val digits = Regex("[0-9]+")
    private val negativeDigits = Regex("-[0-9]+")

    /** Validates raw duration text. Only plain ASCII whole numbers from 0 to [MAX_DURATION] are accepted. */
    fun validateDuration(input: String): DurationValidation {
        val text = input.trim()
        return when {
            text.isEmpty() -> DurationValidation.Invalid(ERROR_EMPTY)
            negativeDigits.matches(text) -> DurationValidation.Invalid(ERROR_NEGATIVE)
            !digits.matches(text) -> DurationValidation.Invalid(ERROR_NOT_WHOLE)
            else -> {
                val value = text.toLongOrNull()
                if (value == null || value > MAX_DURATION) {
                    DurationValidation.Invalid(ERROR_TOO_LARGE)
                } else {
                    DurationValidation.Valid(value)
                }
            }
        }
    }
}
