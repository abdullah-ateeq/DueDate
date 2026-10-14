package com.example.duedate.ui

import com.example.duedate.domain.DueDateValidator
import com.example.duedate.model.CalculationState
import com.example.duedate.model.DurationPreset
import com.example.duedate.model.DurationUnit
import com.example.duedate.model.ThemeMode
import com.example.duedate.repository.DueDateHistoryRepository
import com.example.duedate.repository.SettingsRepository
import com.example.duedate.testing.InMemoryStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DueDateViewModelTest {

    private val today = LocalDate.of(2026, 10, 14)
    private val historyStore = InMemoryStore()
    private val settingsStore = InMemoryStore()
    private var time = 0L
    private var ids = 0

    private fun newViewModel() = DueDateViewModel(
        historyRepository = DueDateHistoryRepository(historyStore),
        settingsRepository = SettingsRepository(settingsStore),
        today = { today },
        clock = { ++time },
        idGenerator = { "id-${++ids}" }
    )

    private val DueDateViewModel.state get() = uiState.value

    @Test fun initialStateUsesDefaults() {
        val vm = newViewModel()
        assertEquals(today, vm.state.startDate)
        assertEquals("30", vm.state.durationInput)
        assertEquals(DurationUnit.DAYS, vm.state.unit)
        assertFalse(vm.state.includeStartDate)
        assertNull(vm.state.result)
        assertTrue(vm.state.canCalculate)
    }

    @Test fun calculateProducesCriticalExampleResult() {
        val vm = newViewModel()
        vm.calculate()
        val result = vm.state.result!!
        assertEquals(LocalDate.of(2026, 11, 13), result.dueDate)
        assertEquals(CalculationState.Calculated, vm.state.calculationState)
        assertFalse(vm.state.isCalculating)
    }

    @Test fun eachCalculationIsSavedToHistory() {
        val vm = newViewModel()
        vm.calculate()
        assertEquals(1, vm.state.history.size)
        vm.onDurationChange("7")
        vm.calculate()
        assertEquals(2, vm.state.history.size)
        assertEquals(7, vm.state.history.first().duration)
    }

    @Test fun historyIsCappedAtTen() {
        val vm = newViewModel()
        (1..11).forEach {
            vm.onDurationChange(it.toString())
            vm.calculate()
        }
        assertEquals(10, vm.state.history.size)
        assertEquals(11, vm.state.history.first().duration)
        assertTrue(vm.state.history.none { it.duration == 1L })
    }

    @Test fun rapidRepeatedCalculationIsIgnored() {
        val vm = newViewModel()
        vm.calculate()
        val first = vm.state.result
        repeat(5) { vm.calculate() }
        assertEquals(1, vm.state.history.size)
        assertTrue(first === vm.state.result)
        assertEquals(1L, time)
    }

    @Test fun changingAnyInputClearsStaleResult() {
        val vm = newViewModel()
        vm.calculate(); vm.onStartDateSelected(LocalDate.of(2026, 1, 1)); assertNull(vm.state.result)
        vm.calculate(); vm.onDurationChange("31"); assertNull(vm.state.result)
        vm.calculate(); vm.onUnitSelected(DurationUnit.WEEKS); assertNull(vm.state.result)
        vm.calculate(); vm.onIncludeStartDateChange(true); assertNull(vm.state.result)
        assertEquals(CalculationState.Idle, vm.state.calculationState)
    }

    @Test fun reselectingSameValueKeepsResult() {
        val vm = newViewModel()
        vm.calculate()
        vm.onUnitSelected(DurationUnit.DAYS)
        vm.onDurationChange("30")
        assertNotNull(vm.state.result)
    }

    @Test fun invalidInputShowsErrorAndDoesNotCalculate() {
        val vm = newViewModel()
        vm.onDurationChange("1.5")
        assertEquals(DueDateValidator.ERROR_NOT_WHOLE, vm.state.errorMessage)
        assertFalse(vm.state.canCalculate)
        vm.calculate()
        assertNull(vm.state.result)
        assertTrue(vm.state.history.isEmpty())

        vm.onDurationChange("")
        assertEquals(DueDateValidator.ERROR_EMPTY, vm.state.errorMessage)
        vm.onDurationChange("-3")
        assertEquals(DueDateValidator.ERROR_NEGATIVE, vm.state.errorMessage)
        vm.onDurationChange("abc")
        assertEquals(DueDateValidator.ERROR_NOT_WHOLE, vm.state.errorMessage)
        vm.onDurationChange("1000001")
        assertEquals(DueDateValidator.ERROR_TOO_LARGE, vm.state.errorMessage)
        vm.onDurationChange("1000000")
        assertNull(vm.state.errorMessage)
    }

    @Test fun calculationOverflowShowsFriendlyError() {
        val vm = newViewModel()
        vm.onStartDateSelected(LocalDate.MAX.minusDays(10))
        vm.onDurationChange("1000")
        vm.calculate()
        assertNull(vm.state.result)
        assertEquals(DueDateValidator.ERROR_INVALID_DATE, vm.state.errorMessage)
        assertEquals(CalculationState.Error(DueDateValidator.ERROR_INVALID_DATE), vm.state.calculationState)
        assertTrue(vm.state.history.isEmpty())
    }

    @Test fun zeroDurationGivesSameDate() {
        val vm = newViewModel()
        vm.onDurationChange("0")
        vm.calculate()
        assertTrue(vm.state.result!!.isSameDate)
    }

    @Test fun includeStartDateIsApplied() {
        val vm = newViewModel()
        vm.onStartDateSelected(LocalDate.of(2026, 10, 1))
        vm.onDurationChange("1")
        vm.onIncludeStartDateChange(true)
        vm.calculate()
        assertEquals(LocalDate.of(2026, 10, 1), vm.state.result!!.dueDate)
        assertTrue(vm.state.result!!.includeStartDate)
    }

    @Test fun quickPresetPopulatesInputsWithoutCalculating() {
        val vm = newViewModel()
        vm.onPresetSelected(DurationPreset(6, DurationUnit.MONTHS))
        assertEquals("6", vm.state.durationInput)
        assertEquals(DurationUnit.MONTHS, vm.state.unit)
        assertNull(vm.state.result)
        assertTrue(vm.state.history.isEmpty())
        vm.calculate()
        assertEquals(LocalDate.of(2027, 4, 14), vm.state.result!!.dueDate)
    }

    @Test fun todayButtonSetsCurrentDate() {
        val vm = newViewModel()
        vm.onStartDateSelected(LocalDate.of(2020, 1, 1))
        vm.onTodaySelected()
        assertEquals(today, vm.state.startDate)
    }

    @Test fun resetRestoresDefaultsAndKeepsHistory() {
        val vm = newViewModel()
        vm.onStartDateSelected(LocalDate.of(2024, 2, 29))
        vm.onDurationChange("4")
        vm.onUnitSelected(DurationUnit.YEARS)
        vm.onIncludeStartDateChange(true)
        vm.calculate()

        vm.reset()

        assertEquals(today, vm.state.startDate)
        assertEquals("30", vm.state.durationInput)
        assertEquals(DurationUnit.DAYS, vm.state.unit)
        assertFalse(vm.state.includeStartDate)
        assertNull(vm.state.result)
        assertEquals(1, vm.state.history.size)
    }

    @Test fun restoringHistoryRestoresInputsAndRecalculates() {
        val vm = newViewModel()
        vm.onStartDateSelected(LocalDate.of(2024, 1, 31))
        vm.onDurationChange("1")
        vm.onUnitSelected(DurationUnit.MONTHS)
        vm.onIncludeStartDateChange(true)
        vm.calculate()
        val entry = vm.state.history.single()

        vm.reset()
        vm.restoreHistory(entry)

        assertEquals(LocalDate.of(2024, 1, 31), vm.state.startDate)
        assertEquals("1", vm.state.durationInput)
        assertEquals(DurationUnit.MONTHS, vm.state.unit)
        assertTrue(vm.state.includeStartDate)
        assertEquals(LocalDate.of(2024, 2, 28), vm.state.result!!.dueDate)
        assertEquals(1, vm.state.history.size)
    }

    @Test fun deleteAndClearHistory() {
        val vm = newViewModel()
        vm.calculate()
        vm.onDurationChange("7")
        vm.calculate()
        vm.deleteHistory(vm.state.history.first().id)
        assertEquals(listOf(30L), vm.state.history.map { it.duration })
        vm.clearHistory()
        assertTrue(vm.state.history.isEmpty())
        assertTrue(newViewModel().state.history.isEmpty())
    }

    @Test fun historySurvivesRestart() {
        newViewModel().calculate()
        assertEquals(1, newViewModel().state.history.size)
    }

    @Test fun settingsSurviveRestart() {
        val vm = newViewModel()
        vm.setThemeMode(ThemeMode.DARK)
        vm.onIncludeStartDateChange(true)
        vm.setHapticsEnabled(false)
        vm.setAnimationsEnabled(false)
        vm.setDynamicColor(false)
        vm.setConfirmHistoryDelete(false)

        val reopened = newViewModel()
        assertEquals(ThemeMode.DARK, reopened.state.settings.themeMode)
        assertTrue(reopened.state.includeStartDate)
        assertFalse(reopened.state.settings.hapticsEnabled)
        assertFalse(reopened.state.settings.animationsEnabled)
        assertFalse(reopened.state.settings.dynamicColor)
        assertFalse(reopened.state.settings.confirmHistoryDelete)
    }

    @Test fun lastConfigurationIsRestoredWhenPreserved() {
        val vm = newViewModel()
        vm.onStartDateSelected(LocalDate.of(2025, 5, 31))
        vm.onDurationChange("3")
        vm.onUnitSelected(DurationUnit.WEEKS)

        val reopened = newViewModel()
        assertEquals(LocalDate.of(2025, 5, 31), reopened.state.startDate)
        assertEquals("3", reopened.state.durationInput)
        assertEquals(DurationUnit.WEEKS, reopened.state.unit)
        assertNull("results are never restored as current", reopened.state.result)
    }

    @Test fun defaultsAreUsedWhenPreserveIsOff() {
        val vm = newViewModel()
        vm.onStartDateSelected(LocalDate.of(2025, 5, 31))
        vm.onDurationChange("3")
        vm.onUnitSelected(DurationUnit.WEEKS)
        vm.onIncludeStartDateChange(true)
        vm.setPreserveLastConfiguration(false)

        val reopened = newViewModel()
        assertEquals(today, reopened.state.startDate)
        assertEquals("30", reopened.state.durationInput)
        assertEquals(DurationUnit.DAYS, reopened.state.unit)
        assertFalse(reopened.state.includeStartDate)
    }

    @Test fun copyAndShareTextRequireAResult() {
        val vm = newViewModel()
        assertNull(vm.copyText())
        assertNull(vm.shareText())
        vm.calculate()
        assertTrue(vm.copyText()!!.contains("Expected due date: 13 November 2026"))
        assertTrue(vm.shareText()!!.contains("13 November 2026"))
    }

    @Test fun overlongInputIsTruncatedAndRejected() {
        val vm = newViewModel()
        vm.onDurationChange("9".repeat(50))
        assertEquals(12, vm.state.durationInput.length)
        assertEquals(DueDateValidator.ERROR_TOO_LARGE, vm.state.errorMessage)
    }
}
