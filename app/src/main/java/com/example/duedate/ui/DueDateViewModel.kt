package com.example.duedate.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.duedate.domain.DueDateCalculator
import com.example.duedate.domain.DueDateFormatter
import com.example.duedate.domain.DueDateValidator
import com.example.duedate.domain.DurationValidation
import com.example.duedate.model.CalculationState
import com.example.duedate.model.DueDateHistory
import com.example.duedate.model.DurationPreset
import com.example.duedate.model.DurationUnit
import com.example.duedate.model.ThemeMode
import com.example.duedate.repository.DueDateHistoryRepository
import com.example.duedate.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.util.UUID

class DueDateViewModel(
    private val historyRepository: DueDateHistoryRepository,
    private val settingsRepository: SettingsRepository,
    private val formatter: DueDateFormatter = DueDateFormatter(),
    private val today: () -> LocalDate = { LocalDate.now() },
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val idGenerator: () -> String = { UUID.randomUUID().toString() }
) : ViewModel() {

    private val _uiState = MutableStateFlow(initialState())
    val uiState: StateFlow<DueDateUiState> = _uiState.asStateFlow()

    private fun initialState(): DueDateUiState {
        val settings = settingsRepository.loadSettings()
        val last = if (settings.preserveLastConfiguration) settingsRepository.loadLastConfiguration() else null
        val durationInput = last?.durationInput ?: SettingsRepository.DEFAULT_DURATION_INPUT
        return DueDateUiState(
            startDate = last?.startDate ?: today(),
            durationInput = durationInput,
            unit = last?.unit ?: DurationUnit.DAYS,
            includeStartDate = if (settings.preserveLastConfiguration) settingsRepository.getIncludeStartDate() else false,
            result = null,
            history = historyRepository.load(),
            errorMessage = validationMessage(durationInput),
            isCalculating = false,
            settings = settings
        )
    }

    // region Inputs

    fun onStartDateSelected(date: LocalDate) = updateInputs { it.copy(startDate = date) }

    fun onTodaySelected() = onStartDateSelected(today())

    fun onDurationChange(input: String) {
        // Bound the raw text so pasted garbage cannot grow unbounded; validation reports the error.
        updateInputs { it.copy(durationInput = input.take(MAX_INPUT_LENGTH)) }
    }

    fun onUnitSelected(unit: DurationUnit) = updateInputs { it.copy(unit = unit) }

    fun onIncludeStartDateChange(include: Boolean) {
        settingsRepository.setIncludeStartDate(include)
        updateInputs { it.copy(includeStartDate = include) }
    }

    /** Populates duration and unit only; the user still confirms with Calculate. */
    fun onPresetSelected(preset: DurationPreset) =
        updateInputs { it.copy(durationInput = preset.duration.toString(), unit = preset.unit) }

    /** Applies an input change, invalidating any result that belonged to the previous inputs. */
    private fun updateInputs(transform: (DueDateUiState) -> DueDateUiState) {
        _uiState.update { current ->
            val next = transform(current)
            if (next.startDate == current.startDate && next.durationInput == current.durationInput &&
                next.unit == current.unit && next.includeStartDate == current.includeStartDate
            ) {
                current
            } else {
                next.copy(
                    result = null,
                    errorMessage = validationMessage(next.durationInput),
                    calculationState = CalculationState.Idle
                )
            }
        }
        persistConfiguration()
    }

    private fun persistConfiguration() {
        val state = _uiState.value
        if (state.settings.preserveLastConfiguration) {
            settingsRepository.saveLastConfiguration(state.startDate, state.durationInput, state.unit)
        }
    }

    private fun validationMessage(input: String): String? =
        (DueDateValidator.validateDuration(input) as? DurationValidation.Invalid)?.message

    // endregion

    // region Calculation

    /**
     * Calculates the due date for the current inputs and records it in history.
     * Repeated taps for an unchanged configuration are ignored, so a double tap never produces a
     * second calculation or a duplicate history entry.
     */
    fun calculate() {
        val state = _uiState.value
        if (state.isCalculating) return

        val duration = when (val validation = DueDateValidator.validateDuration(state.durationInput)) {
            is DurationValidation.Invalid -> {
                _uiState.update {
                    it.copy(errorMessage = validation.message, calculationState = CalculationState.Error(validation.message))
                }
                return
            }
            is DurationValidation.Valid -> validation.duration
        }

        if (state.result?.matches(state.startDate, duration, state.unit, state.includeStartDate) == true) return

        _uiState.update { it.copy(isCalculating = true) }
        val result = DueDateCalculator.calculateOrNull(state.startDate, duration, state.unit, state.includeStartDate)
        if (result == null) {
            val message = DueDateValidator.ERROR_INVALID_DATE
            _uiState.update {
                it.copy(
                    result = null,
                    errorMessage = message,
                    isCalculating = false,
                    calculationState = CalculationState.Error(message)
                )
            }
            return
        }

        val history = historyRepository.save(
            DueDateHistory(
                id = idGenerator(),
                startDate = result.startDate,
                duration = result.duration,
                unit = result.unit,
                dueDate = result.dueDate,
                includeStartDate = result.includeStartDate,
                calculatedAt = clock()
            )
        )
        _uiState.update {
            it.copy(
                result = result,
                history = history,
                errorMessage = null,
                isCalculating = false,
                calculationState = CalculationState.Calculated
            )
        }
    }

    /** Restores a history entry's inputs and recalculates it without adding a new history entry. */
    fun restoreHistory(entry: DueDateHistory) {
        val durationInput = entry.duration.toString()
        settingsRepository.setIncludeStartDate(entry.includeStartDate)
        val result = DueDateCalculator.calculateOrNull(entry.startDate, entry.duration, entry.unit, entry.includeStartDate)
        _uiState.update {
            it.copy(
                startDate = entry.startDate,
                durationInput = durationInput,
                unit = entry.unit,
                includeStartDate = entry.includeStartDate,
                result = result,
                errorMessage = if (result == null) DueDateValidator.ERROR_INVALID_DATE else validationMessage(durationInput),
                isCalculating = false,
                calculationState = if (result == null) {
                    CalculationState.Error(DueDateValidator.ERROR_INVALID_DATE)
                } else {
                    CalculationState.Calculated
                }
            )
        }
        persistConfiguration()
    }

    /** Restores default inputs and clears the result. History is kept. */
    fun reset() {
        settingsRepository.setIncludeStartDate(false)
        _uiState.update {
            it.copy(
                startDate = today(),
                durationInput = SettingsRepository.DEFAULT_DURATION_INPUT,
                unit = DurationUnit.DAYS,
                includeStartDate = false,
                result = null,
                errorMessage = null,
                isCalculating = false,
                calculationState = CalculationState.Idle
            )
        }
        persistConfiguration()
    }

    // endregion

    // region History

    fun deleteHistory(id: String) {
        val history = historyRepository.delete(id)
        _uiState.update { it.copy(history = history) }
    }

    fun clearHistory() {
        val history = historyRepository.clear()
        _uiState.update { it.copy(history = history) }
    }

    // endregion

    // region Settings

    fun setThemeMode(mode: ThemeMode) {
        settingsRepository.setThemeMode(mode)
        _uiState.update { it.copy(settings = it.settings.copy(themeMode = mode)) }
    }

    fun setDynamicColor(enabled: Boolean) {
        settingsRepository.setDynamicColor(enabled)
        _uiState.update { it.copy(settings = it.settings.copy(dynamicColor = enabled)) }
    }

    fun setHapticsEnabled(enabled: Boolean) {
        settingsRepository.setHapticsEnabled(enabled)
        _uiState.update { it.copy(settings = it.settings.copy(hapticsEnabled = enabled)) }
    }

    fun setAnimationsEnabled(enabled: Boolean) {
        settingsRepository.setAnimationsEnabled(enabled)
        _uiState.update { it.copy(settings = it.settings.copy(animationsEnabled = enabled)) }
    }

    fun setConfirmHistoryDelete(enabled: Boolean) {
        settingsRepository.setConfirmHistoryDelete(enabled)
        _uiState.update { it.copy(settings = it.settings.copy(confirmHistoryDelete = enabled)) }
    }

    fun setPreserveLastConfiguration(enabled: Boolean) {
        settingsRepository.setPreserveLastConfiguration(enabled)
        _uiState.update { it.copy(settings = it.settings.copy(preserveLastConfiguration = enabled)) }
        persistConfiguration()
    }

    // endregion

    // region Copy / share

    fun copyText(): String? = _uiState.value.result?.let(formatter::copyText)

    fun shareText(): String? = _uiState.value.result?.let(formatter::shareText)

    // endregion

    class Factory(
        private val historyRepository: DueDateHistoryRepository,
        private val settingsRepository: SettingsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            DueDateViewModel(historyRepository, settingsRepository) as T
    }

    companion object {
        private const val MAX_INPUT_LENGTH = 12
    }
}
