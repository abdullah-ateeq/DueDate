package com.example.duedate.repository

import com.example.duedate.data.HistoryCodec
import com.example.duedate.data.KeyValueStore
import com.example.duedate.model.DueDateHistory

/** Stores the most recent calculations, newest first, capped at [MAX_ENTRIES]. */
class DueDateHistoryRepository(private val store: KeyValueStore) {

    fun load(): List<DueDateHistory> =
        HistoryCodec.decode(store.getString(KEY_HISTORY)).take(MAX_ENTRIES)

    /**
     * Saves [entry] as the newest item. An existing entry with the same configuration is replaced
     * rather than duplicated, and the oldest entries beyond [MAX_ENTRIES] are dropped.
     */
    fun save(entry: DueDateHistory): List<DueDateHistory> {
        val updated = (listOf(entry) + load().filterNot { it.hasSameConfiguration(entry) })
            .take(MAX_ENTRIES)
        persist(updated)
        return updated
    }

    fun delete(id: String): List<DueDateHistory> {
        val updated = load().filterNot { it.id == id }
        persist(updated)
        return updated
    }

    fun clear(): List<DueDateHistory> {
        store.remove(KEY_HISTORY)
        return emptyList()
    }

    private fun persist(entries: List<DueDateHistory>) {
        store.putString(KEY_HISTORY, HistoryCodec.encode(entries))
    }

    companion object {
        const val MAX_ENTRIES = 10
        private const val KEY_HISTORY = "history"
    }
}
