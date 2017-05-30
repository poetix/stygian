package com.codepoetics.stygian

import java.util.concurrent.CompletableFuture

/**
 * An asynchronously-returned result.
 */
typealias Async<O> = CompletableFuture<O>

/**
 * A function which returns a result asynchronously.
 */
typealias AsyncFunction<I, O> = (I) -> Async<O>

/**
 * A function which returns a result asynchronously.
 */
typealias AsyncFunction2<I1, I2, O> = (I1, I2) -> Async<O>

/**
 * A named action, which accepts an input and returns an asynchronous result.
 */
data class Action<I, O>(val name: String, val operation: AsyncFunction<I, O>): AsyncFunction<I, O> by operation {
    fun decorate(decorator: (AsyncFunction<I, O>) -> AsyncFunction<I, O>): Action<I, O> = copy(operation = decorator(operation))
}

/**
 * A named condition, which accepts an input and asynchronously returns either True orIf False.
 */
data class Condition<T>(val name: String, val operation: AsyncFunction<T, Boolean>): AsyncFunction<T, Boolean> by operation {
    fun decorate(decorator: (AsyncFunction<T, Boolean>) -> AsyncFunction<T, Boolean>): Condition<T> = copy(operation = decorator(operation))
}

/**
 * Convert a synchronous function into an asynchronous one.
 */
fun <I, O> liftAsync(f: (I) -> O): AsyncFunction<I, O> = { i ->
    val result = CompletableFuture<O>()
    try {
        result.complete(f(i))
    } catch (e: Throwable) {
        result.completeExceptionally(e)
    }
    result
}

/**
 * Convert a synchronous function into an asynchronous one.
 */
fun <I1, I2, O> liftAsync2(f: (I1, I2) -> O): AsyncFunction2<I1, I2, O> = { i1, i2 ->
    val result = CompletableFuture<O>()
    try {
        result.complete(f(i1, i2))
    } catch (e: Throwable) {
        result.completeExceptionally(e)
    }
    result
}