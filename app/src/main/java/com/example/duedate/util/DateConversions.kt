package com.example.duedate.util

import java.time.LocalDate
import java.time.ZoneOffset

/** Conversions between LocalDate and the UTC-midnight millis used by the Material 3 DatePicker. */
object DateConversions {
    fun toPickerMillis(date: LocalDate): Long =
        date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    fun fromPickerMillis(millis: Long): LocalDate =
        java.time.Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
}
