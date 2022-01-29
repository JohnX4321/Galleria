package com.tzapps.galleria.utils

import android.content.res.Resources

object Utils {

    fun getScreenWidth() = Resources.getSystem().displayMetrics.widthPixels

    fun getScreenHeight() = Resources.getSystem().displayMetrics.heightPixels

}