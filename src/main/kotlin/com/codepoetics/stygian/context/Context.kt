package com.codepoetics.stygian.context

import com.codepoetics.stygian.*
import java.util.concurrent.CompletableFuture

data class BranchBuilder<C, F: FluentContextFlow<C, F>>(val previous: FluentContextFlow<C, F>, val completer: (F) -> F = {  it }) {
    fun orIf(condition: Condition<C>, ifTrue: F): BranchBuilder<C, F> = copy(completer = { completer(it.orIf(condition, ifTrue)) })
    fun otherwise(default: F): F = previous.then(completer(default))
}

interface FluentContextFlow<C, F: FluentContextFlow<C, F>>: Visitable<C, C> {
    fun thenIf(condition: Condition<C>, ifTrue: F): BranchBuilder<C, F> = BranchBuilder<C, F>(this).orIf(condition, ifTrue)
    fun then(next: F): F
    fun orIf(condition: Condition<C>, ifTrue: F): F
}

data class ContextFlow<C>(val flow: Flow<C, C>): Visitable<C, C> by flow, FluentContextFlow<C, ContextFlow<C>> {
    companion object {
        fun <C> withAction(action: Action<C, C>): ContextFlow<C> = ContextFlow(Flow.Only(action))

        fun <C> asyncFlow(name: String, update: C.() -> Async<C>): ContextFlow<C> =
                withAction(Action(name, update))

        fun <C> flow(name: String, update: C.() -> C): ContextFlow<C> =
                withAction(Action(name, liftAsync(update)))
    }

    override fun then(next: ContextFlow<C>) = copy(flow = flow.then(next.flow))
    override fun orIf(condition: Condition<C>, ifTrue: ContextFlow<C>) = copy(flow = flow.orIf(condition, ifTrue.flow))
}

interface ContextFlowBuilder<I, D, C, F: FluentContextFlow<C, F>> {
    fun get(context: C, index: I): D?
    fun put(context: C, index: I, document: D): C
    fun make(flow: ContextFlow<C>): F

    fun withAction(action: Action<C, C>): F = make(ContextFlow.withAction(action))
    fun asyncFlow(name: String, update: C.() -> Async<C>): F =
            withAction(Action(name, update))

    fun flow(name: String, update: C.() -> C): F = asyncFlow(name, liftAsync(update))

    fun asyncFlow(name: String, target: I, update: C.() -> Async<D>): F =
            asyncFlow(name) {
                this.update().thenApply { put(this, target, it) }
            }

    fun flow(name: String, target: I, update: C.() -> D): F = asyncFlow(name, target, liftAsync(update))

    fun asyncFlow(name: String, source: I, target: I, update: D.() -> Async<D>): F =
            asyncFlow(name) {
                get(this, source)?.update()?.thenApply { put(this, target, it) }
                    ?: CompletableFuture<C>().apply { completeExceptionally(Throwable("Source ${source} not found")) }
            }

    fun flow(name: String, source: I, target: I, update: D.() -> D): F = asyncFlow(name, source, target, liftAsync(update))

    fun asyncFlow(name: String, sourceA: I, sourceB: I, target: I, update: (D, D) -> Async<D>): F =
            asyncFlow(name) {
                val inputA = get(this, sourceA)
                val inputB = get(this, sourceB)
                if (inputA == null) {
                    CompletableFuture<C>().apply { completeExceptionally(Throwable("Source ${sourceA} not found")) }
                } else if (inputB == null) {
                    CompletableFuture<C>().apply { completeExceptionally(Throwable("Source ${sourceB} not found")) }
                } else update(inputA, inputB).thenApply { put(this, target, it) }
            }

    fun flow(name: String, sourceA: I, sourceB: I, target: I, update: (D, D) -> D): F =
            asyncFlow(name, sourceA, sourceB, target, liftAsync2(update))
}