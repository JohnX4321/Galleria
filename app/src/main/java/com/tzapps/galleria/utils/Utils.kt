package com.tzapps.galleria.utils

import android.content.res.Resources

object Utils {

    fun getScreenWidth() = Resources.getSystem().displayMetrics.widthPixels

    fun getScreenHeight() = Resources.getSystem().displayMetrics.heightPixels

    const val RATIO_4_3=4.0/16.0
    const val RATIO_16_9=16.0/9.0

}