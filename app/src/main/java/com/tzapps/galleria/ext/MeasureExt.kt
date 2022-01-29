package com.tzapps.galleria.ext

import android.content.Context
import kotlin.math.ceil

fun Double.pxToDp(context: Context): Int {
    val logicalDensity = context.resources.configuration.densityDpi.toLong()
    val px= ceil(this*logicalDensity).toInt()
    return px
}