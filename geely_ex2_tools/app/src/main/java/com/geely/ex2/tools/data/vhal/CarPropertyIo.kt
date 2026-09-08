package com.geely.ex2.tools.data.vhal

import java.util.concurrent.Callable
import java.util.concurrent.Executors

object CarPropertyIo {
    @Volatile
    private var ioThread: Thread? = null

    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "CarPropertyIo").also { ioThread = it }
    }

    fun execute(block: () -> Unit) {
        if (Thread.currentThread() == ioThread) {
            block()
            return
        }
        executor.execute(block)
    }

    fun <T> call(block: () -> T): T {
        if (Thread.currentThread() == ioThread) {
            return block()
        }
        return executor.submit(Callable(block)).get()
    }
}
