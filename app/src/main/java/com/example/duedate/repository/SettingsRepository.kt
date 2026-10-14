package com.example.duedate.repository

import com.example.duedate.data.KeyValueStore
import com.example.duedate.model.AppSettings
import com.example.duedate.model.DurationUnit
import com.example.duedate.model.LastConfiguration
import com.example.duedate.model.ThemeMode
import java.time.LocalDate

class SettingsRepository(private val store: KeyValueStore) {

    fun loadSettings(): AppSettings {
        val defaults = AppSettings()
        return AppSettings(
            themeMode = enumOrNull<ThemeMode>(store.getString(KEY_THEME)) ?: defaults.themeMode,
            dynamicColor = store.getBoolean(KEY_DYNAMIC_COLOR, defaults.dynamicColor),
            hapticsEnabled = store.getBoolean(KEY_HAPTICS, defaults.hapticsEnabled),
            animationsEnabled = store.getBoolean(KEY_ANIMATIONS, defaults.animationsEnabled),
            confirmHistoryDelete = store.getBoolean(KEY_CONFIRM_DELETE, defaults.confirmHistoryDelete),
            preserveLastConfiguration = store.getBoolean(KEY_PRESERVE, defaults.preserveLastConfiguration)
        )
    }

    fun setThemeMode(mode: ThemeMode) = store.putString(KEY_THEME, mode.name)
    fun setDynamicColor(enabled: Boolean) = store.putBoolean(KEY_DYNAMIC_COLOR, enabled)
    fun setHapticsEnabled(enabled: Boolean) = store.putBoolean(KEY_HAPTICS, enabled)
    fun setAnimationsEnabled(enabled: Boolean) = store.putBoolean(KEY_ANIMATIONS, enabled)
    fun setConfirmHistoryDelete(enabled: Boolean) = store.putBoolean(KEY_CONFIRM_DELETE, enabled)
    fun setPreserveLastConfiguration(enabled: Boolean) = store.putBoolean(KEY_PRESERVE, enabled)

    fun getIncludeStartDate(): Boolean = store.getBoolean(KEY_INCLUDE_START, false)
    fun setIncludeStartDate(enabled: Boolean) = store.putBoolean(KEY_INCLUDE_START, enabled)

    /** Returns the stored inputs, or null if none were saved or the stored date is unreadable. */
    fun loadLastConfiguration(): LastConfiguration? {
        val start = store.getString(KEY_LAST_START)?.let {
            try {
                LocalDate.parse(it)
            } catch (_: RuntimeException) {
                null
            }
        } ?: return null
        return LastConfiguration(
            startDate = start,
            durationInput = store.getString(KEY_LAST_DURATION) ?: DEFAULT_DURATION_INPUT,
            unit = enumOrNull<DurationUnit>(store.getString(KEY_LAST_UNIT)) ?: DurationUnit.DAYS,
            includeStartDate = getIncludeStartDate()
        )
    }

    fun saveLastConfiguration(startDate: LocalDate, durationInput: String, unit: DurationUnit) {
        store.putString(KEY_LAST_START, startDate.toString())
        store.putString(KEY_LAST_DURATION, durationInput)
        store.putString(KEY_LAST_UNIT, unit.name)
    }

    private inline fun <reified T : Enum<T>> enumOrNull(name: String?): T? =
        name?.let { value -> enumValues<T>().firstOrNull { it.name == value } }

    companion object {
        const val DEFAULT_DURATION_INPUT = "30"

        private const val KEY_THEME = "theme_mode"
        private const val KEY_DYNAMIC_COLOR = "dynamic_color"
        private const val KEY_HAPTICS = "haptics"
        private const val KEY_ANIMATIONS = "animations"
        private const val KEY_CONFIRM_DELETE = "confirm_history_delete"
        private const val KEY_PRESERVE = "preserve_last_configuration"
        private const val KEY_INCLUDE_START = "include_start_date"
        private const val KEY_LAST_START = "last_start_date"
        private const val KEY_LAST_DURATION = "last_duration"
        private const val KEY_LAST_UNIT = "last_unit"
    }
}
