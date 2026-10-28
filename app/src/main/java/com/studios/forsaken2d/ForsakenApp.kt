package com.studios.forsaken2d

import android.util.Log
import androidx.multidex.MultiDexApplication

/**
 * ForsakenApp - Application class.
 * - Catch crash global để log rõ ràng vào logcat
 * - Kế thừa MultiDexApplication cho máy 32-bit
 */
class ForsakenApp : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()
        Log.d("FORSAKEN", "=== App started ===")

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("FORSAKEN_CRASH", "Thread: ${thread.name}", throwable)
            Log.e("FORSAKEN_CRASH", "Message: ${throwable.message}")
            Log.e("FORSAKEN_CRASH", "Stack: ${throwable.stackTraceToString()}")
            throwable.printStackTrace()
        }
    }
}
