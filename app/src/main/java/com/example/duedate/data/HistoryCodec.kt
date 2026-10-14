package com.example.duedate.data

import com.example.duedate.model.DueDateHistory
import com.example.duedate.model.DurationUnit
import java.time.LocalDate

/**
 * Serializes history as one entry per line with '|'-separated fields. Dates are stored as
 * ISO-8601 strings (e.g. 2026-10-14); display text is never stored. Malformed lines are skipped.
 */
internal object HistoryCodec {
    private const val SEPARATOR = "|"

    fun encode(entries: List<DueDateHistory>): String = entries.joinToString("\n") { entry ->
        listOf(
            entry.id,
            entry.startDate.toString(),
            entry.duration.toString(),
            entry.unit.name,
            entry.dueDate.toString(),
            entry.includeStartDate.toString(),
            entry.calculatedAt.toString()
        ).joinToString(SEPARATOR)
    }

    fun decode(raw: String?): List<DueDateHistory> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.lineSequence().mapNotNull(::decodeLine).toList()
    }

    private fun decodeLine(line: String): DueDateHistory? {
        val parts = line.split(SEPARATOR)
        if (parts.size != 7) return null
        return try {
            DueDateHistory(
                id = parts[0],
                startDate = LocalDate.parse(parts[1]),
                duration = parts[2].toLong(),
                unit = DurationUnit.valueOf(parts[3]),
                dueDate = LocalDate.parse(parts[4]),
                includeStartDate = parts[5].toBooleanStrict(),
                calculatedAt = parts[6].toLong()
            )
        } catch (_: RuntimeException) {
            null
        }
    }
}
