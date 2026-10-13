package com.example.test

import android.app.Activity
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.Executors

/** Serialize short local database work off the UI thread, then deliver only to a living screen. */
object AppIo {
    private val executor = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())

    fun <T> run(owner: LifecycleOwner, work: () -> T, result: (Result<T>) -> Unit) {
        executor.execute {
            val outcome = runCatching(work)
            main.post {
                if (owner.lifecycle.currentState != Lifecycle.State.DESTROYED &&
                    (owner !is Activity || !owner.isFinishing)) result(outcome)
            }
        }
    }
}
