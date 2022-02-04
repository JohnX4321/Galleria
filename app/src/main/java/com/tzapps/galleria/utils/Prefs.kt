package com.tzapps.galleria.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.camera.core.ImageCapture
import androidx.preference.PreferenceManager

object Prefs {

    private lateinit var prefs: SharedPreferences
    private const val KEY_TRASH="TRASH"
    private const val KEY_FLASH="FLASH"
    private const val KEY_GRID="GRID"
    private const val KEY_HDR="HDR"


    fun init(context: Context) {
        prefs=PreferenceManager.getDefaultSharedPreferences(context)
    }

    fun isTrashEnabled() = prefs.getBoolean(KEY_TRASH,false)

    infix fun setTrashEnabled(value: Boolean) = prefs.edit().putBoolean(KEY_TRASH,value).apply()

    fun getFlashMode() = prefs.getInt(KEY_FLASH,ImageCapture.FLASH_MODE_OFF)

    fun setFlashMode(value: Int) = prefs.edit().putInt(KEY_FLASH,value).apply()

    fun hasGrid() = prefs.getBoolean(KEY_GRID,false)

    fun setGrid(value: Boolean) = prefs.edit().putBoolean(KEY_GRID,value).apply()

    fun hasHDR() = prefs.getBoolean(KEY_HDR,false)

    fun setHDR(value: Boolean) = prefs.edit().putBoolean(KEY_HDR,false).apply()




}