package com.tzapps.galleria

import android.app.Application
import com.tzapps.galleria.utils.Prefs

class GalleriaApp: Application() {

    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
    }

}