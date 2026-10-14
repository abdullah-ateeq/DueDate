package com.example.duedate.testing

import com.example.duedate.data.KeyValueStore

/** In-memory [KeyValueStore]. Reusing one instance across repositories simulates an app restart. */
class InMemoryStore : KeyValueStore {
    val values = mutableMapOf<String, Any>()

    override fun getString(key: String): String? = values[key] as? String
    override fun putString(key: String, value: String) { values[key] = value }
    override fun getBoolean(key: String, default: Boolean): Boolean = values[key] as? Boolean ?: default
    override fun putBoolean(key: String, value: Boolean) { values[key] = value }
    override fun remove(key: String) { values.remove(key) }
}
