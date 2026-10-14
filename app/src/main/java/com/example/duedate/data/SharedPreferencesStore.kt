package com.example.duedate.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class SharedPreferencesStore(private val prefs: SharedPreferences) : KeyValueStore {

    constructor(context: Context, name: String) :
        this(context.applicationContext.getSharedPreferences(name, Context.MODE_PRIVATE))

    override fun getString(key: String): String? = prefs.getString(key, null)

    override fun putString(key: String, value: String) = prefs.edit { putString(key, value) }

    override fun getBoolean(key: String, default: Boolean): Boolean = prefs.getBoolean(key, default)

    override fun putBoolean(key: String, value: Boolean) = prefs.edit { putBoolean(key, value) }

    override fun remove(key: String) = prefs.edit { remove(key) }
}
