package com.example.duedate.repository

import com.example.duedate.model.DueDateHistory
import com.example.duedate.model.DurationUnit
import com.example.duedate.testing.InMemoryStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DueDateHistoryRepositoryTest {

    private val store = InMemoryStore()
    private val repository = DueDateHistoryRepository(store)
    private val start = LocalDate.of(2026, 10, 14)

    private fun entry(duration: Long, id: String = "id-$duration", include: Boolean = false) = DueDateHistory(
        id = id,
        startDate = start,
        duration = duration,
        unit = DurationUnit.DAYS,
        dueDate = start.plusDays(duration),
        includeStartDate = include,
        calculatedAt = duration
    )

    @Test fun saveAndLoadRoundTrip() {
        val saved = entry(30, include = true)
        repository.save(saved)
        assertEquals(listOf(saved), DueDateHistoryRepository(store).load())
    }

    @Test fun storesIsoDatesNotDisplayText() {
        repository.save(entry(30))
        val raw = store.getString("history")!!
        assertTrue(raw.contains("2026-10-14"))
        assertTrue(raw.contains("2026-11-13"))
    }

    @Test fun newestEntryComesFirst() {
        repository.save(entry(1))
        repository.save(entry(2))
        assertEquals(listOf("id-2", "id-1"), repository.load().map { it.id })
    }

    @Test fun limitIsTenAndOldestIsRemoved() {
        (1L..10L).forEach { repository.save(entry(it)) }
        assertEquals(10, repository.load().size)
        repository.save(entry(11))
        val ids = repository.load().map { it.id }
        assertEquals(10, ids.size)
        assertEquals("id-11", ids.first())
        assertTrue("oldest entry removed", "id-1" !in ids)
    }

    @Test fun identicalConfigurationIsNotDuplicated() {
        repository.save(entry(30, id = "a"))
        repository.save(entry(7, id = "b"))
        repository.save(entry(30, id = "c"))
        assertEquals(listOf("c", "b"), repository.load().map { it.id })
    }

    @Test fun sameDurationWithDifferentCountingModeIsDistinct() {
        repository.save(entry(30, id = "a", include = false))
        repository.save(entry(30, id = "b", include = true))
        assertEquals(2, repository.load().size)
    }

    @Test fun deleteRemovesOnlyThatEntry() {
        repository.save(entry(1))
        repository.save(entry(2))
        val remaining = repository.delete("id-1")
        assertEquals(listOf("id-2"), remaining.map { it.id })
        assertEquals(remaining, repository.load())
    }

    @Test fun clearRemovesEverything() {
        repository.save(entry(1))
        repository.save(entry(2))
        repository.clear()
        assertTrue(repository.load().isEmpty())
    }

    @Test fun corruptLinesAreSkipped() {
        store.putString("history", "garbage\nx|2026-10-14|30|DAYS|2026-11-13|false|5\nbad|date|1|DAYS|x|false|1")
        assertEquals(listOf("x"), repository.load().map { it.id })
    }
}
