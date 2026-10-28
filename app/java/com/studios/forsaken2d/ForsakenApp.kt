package com.studios.forsaken2d

import android.app.Application
import android.util.Log

class ForsakenApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.d("FORSAKEN", "=== App started ===")

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("FORSAKEN_CRASH", "Thread: ${thread.name}", throwable)
            Log.e("FORSAKEN_CRASH", "Message: ${throwable.message}")
        }
    }
}
