package org.cangnova.cangjie.lsp.server

import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

class CangjieRequestExecutor(
    threadNamePrefix: String = "cangjie-lsp-worker",
) : AutoCloseable {
    private val threadCounter = AtomicInteger(1)
    private val executor: ExecutorService = Executors.newSingleThreadExecutor(
        ThreadFactory { runnable ->
            Thread(runnable, "$threadNamePrefix-${threadCounter.getAndIncrement()}").apply {
                isDaemon = true
            }
        },
    )

    fun <T> compute(action: () -> T): CompletableFuture<T> {
        return CompletableFuture.supplyAsync(action, executor)
    }

    override fun close() {
        executor.shutdownNow()
    }
}
