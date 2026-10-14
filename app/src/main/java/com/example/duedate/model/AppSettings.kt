package com.example.duedate.model

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val animationsEnabled: Boolean = true,
    val confirmHistoryDelete: Boolean = true,
    val preserveLastConfiguration: Boolean = true
)

/** The inputs of the last configuration, restored on launch when "Preserve Last Configuration" is on. */
data class LastConfiguration(
    val startDate: java.time.LocalDate,
    val durationInput: String,
    val unit: DurationUnit,
    val includeStartDate: Boolean
)
