package com.codepoetics.stygian

import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicLong
import java.util.function.BiFunction
import java.util.stream.Collectors
import java.util.stream.Stream

fun <T> Stream<T>.forEachNumbered(startIndex: Int = 0, f: (Int, T) -> Unit) {
    var index = startIndex
    forEach { item -> f(index++, item)}
}

fun <T> Stream<CompletableFuture<T>>.toFutureList(): CompletableFuture<List<T>> {
    val futures = this.collect(Collectors.toList())
    val resultsRemaining = AtomicLong(futures.size.toLong())
    val result = CompletableFuture<List<T>>()

    val handler = BiFunction<T?, Throwable?, Void?> { success, failure ->
        if (failure == null) {
            if (resultsRemaining.decrementAndGet() == 0L) {
                result.complete(futures.map { f -> f.get() })
            }
        } else {
            result.completeExceptionally(failure)
        }
        null
    }

    futures.forEach { future -> future.handle<Void>(handler) }
    return result
}