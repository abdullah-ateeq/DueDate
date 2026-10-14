package com.example.duedate.ui

import com.example.duedate.model.AppSettings
import com.example.duedate.model.CalculationState
import com.example.duedate.model.DueDateHistory
import com.example.duedate.model.DueDateResult
import com.example.duedate.model.DurationUnit
import java.time.LocalDate

data class DueDateUiState(
    val startDate: LocalDate,
    val durationInput: String,
    val unit: DurationUnit,
    val includeStartDate: Boolean,
    /** Always belongs to the current inputs; cleared whenever an input changes. */
    val result: DueDateResult?,
    val history: List<DueDateHistory>,
    /** Validation or calculation error for the current inputs, or null when they are valid. */
    val errorMessage: String?,
    val isCalculating: Boolean,
    val calculationState: CalculationState = CalculationState.Idle,
    val settings: AppSettings = AppSettings()
) {
    val canCalculate: Boolean get() = errorMessage == null && !isCalculating
}
