package com.example.duedate.repository

import com.example.duedate.model.AppSettings
import com.example.duedate.model.DurationUnit
import com.example.duedate.model.ThemeMode
import com.example.duedate.testing.InMemoryStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SettingsRepositoryTest {

    private val store = InMemoryStore()

    @Test fun defaults() {
        val repository = SettingsRepository(store)
        assertEquals(AppSettings(), repository.loadSettings())
        assertTrue(repository.loadSettings().preserveLastConfiguration)
        assertFalse(repository.getIncludeStartDate())
        assertNull(repository.loadLastConfiguration())
    }

    @Test fun settingsPersistAcrossInstances() {
        SettingsRepository(store).apply {
            setThemeMode(ThemeMode.DARK)
            setDynamicColor(false)
            setHapticsEnabled(false)
            setAnimationsEnabled(false)
            setConfirmHistoryDelete(false)
            setPreserveLastConfiguration(false)
            setIncludeStartDate(true)
        }
        val reopened = SettingsRepository(store)
        assertEquals(
            AppSettings(
                themeMode = ThemeMode.DARK,
                dynamicColor = false,
                hapticsEnabled = false,
                animationsEnabled = false,
                confirmHistoryDelete = false,
                preserveLastConfiguration = false
            ),
            reopened.loadSettings()
        )
        assertTrue(reopened.getIncludeStartDate())
    }

    @Test fun lastConfigurationPersists() {
        SettingsRepository(store).saveLastConfiguration(LocalDate.of(2024, 2, 29), "4", DurationUnit.YEARS)
        val config = SettingsRepository(store).loadLastConfiguration()!!
        assertEquals(LocalDate.of(2024, 2, 29), config.startDate)
        assertEquals("4", config.durationInput)
        assertEquals(DurationUnit.YEARS, config.unit)
        assertEquals("2024-02-29", store.getString("last_start_date"))
    }

    @Test fun unreadableStoredValuesFallBack() {
        store.putString("theme_mode", "NEON")
        store.putString("last_start_date", "not-a-date")
        val repository = SettingsRepository(store)
        assertEquals(ThemeMode.SYSTEM, repository.loadSettings().themeMode)
        assertNull(repository.loadLastConfiguration())
    }
}
