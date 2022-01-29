package com.tzapps.galleria.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

object Prefs {

    private lateinit var prefs: SharedPreferences
    private const val KEY_TRASH="TRASH"

    fun init(context: Context) {
        prefs=PreferenceManager.getDefaultSharedPreferences(context)
    }

    fun isTrashEnabled() = prefs.getBoolean(KEY_TRASH,false)

    infix fun setTrashEnabled(value: Boolean) = prefs.edit().putBoolean(KEY_TRASH,value).apply()





}