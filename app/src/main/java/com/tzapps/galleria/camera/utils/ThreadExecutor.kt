package com.tzapps.galleria.camera.utils

import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executor

open class ThreadExecutor(protected val handler: Handler): Executor {
    override fun execute(command: Runnable) {
        handler.post(command)
    }
}

class MainExecutor: ThreadExecutor(Handler(Looper.getMainLooper())) {
    override fun execute(command: Runnable) {
        handler.post(command)
    }
}