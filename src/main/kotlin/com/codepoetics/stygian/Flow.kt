package com.codepoetics.stygian

import java.util.stream.Stream

/**
 * Represents a condition together with the flow that should be followed if that condition is met.
 */
data class ConditionalFlow<I, O>(val condition: Condition<I>, val flow: Flow<I, O>)

interface Visitable<I, O> {
    /**
     * Run the flow with the supplied input, using the supplied FlowVisitor to define the execution strategy.
     */
    fun run(input: I, visitor: FlowVisitor = DefaultFlowVisitor): Async<O> = visit(visitor).invoke(input)

    /**
     * Use a FlowVisitor to convert the flow into a single Action that can be executed.
     */
    fun visit(visitor: FlowVisitor): Action<I, O>
}

/**
 * Represents a flow of execution.
 */
sealed class Flow<I, O>: Visitable<I, O> {

    /**
     * Sequence this flow with another flow, creating a new flow that first performs this flow, then passes its output to the second flow.
     */
    open fun <O2> then(next: Flow<O, O2>): Flow<I, O2> = Sequence(
            this as Flow<I, Any>,
            listOf(),
            next as Flow<Any, O2>)

    /**
     * Create a branching flow which tests the supplied condition, and branches to the supplied flow if it is met, otherwise continuing with the current flow.
     */
    open fun orIf(condition: Condition<I>, ifTrue: Flow<I, O>): Flow<I, O> =
            Branch(this, mapOf(condition.name to ConditionalFlow(condition, ifTrue)))

    /**
     * A flow which executes a single Action.
     */
    class Only<I, O>(val action: Action<I, O>): Flow<I, O>() {
        override fun visit(visitor: FlowVisitor): Action<I, O> = visitor.action(action)
    }

    /**
     * A branching flow which tests all of its branching conditions, executing the flow associated with the first condition that succeeds, orIf falling through to the default flow otherwise.
     * Note that no ordering is defined on the branching conditions, which may be tested in any order.
     */
    class Branch<I, O>(val default: Flow<I, O>, val branches: Map<String, ConditionalFlow<I, O>>): Flow<I, O>() {

        override fun visit(visitor: FlowVisitor): Action<I, O> {
            val branchIter = branches.values.iterator()
            val first = branchIter.next()
            var action = visitor.branch(visitor.condition(first.condition), first.flow.visit(visitor), default.visit(visitor))

            while(branchIter.hasNext()) {
                val nextBranch = branchIter.next()
                action = visitor.branch(visitor.condition(nextBranch.condition), nextBranch.flow.visit(visitor), action)
            }

            return action
        }

        override fun orIf(condition: Condition<I>, ifTrue: Flow<I, O>): Flow<I, O> = Branch(
                default,
                branches.plus(condition.name to ConditionalFlow(condition, ifTrue)))

    }

    /**
     * A flow which executes a sequence of at least two flows ("first" and "last")
     */
    class Sequence<I, O>(val first: Flow<I, Any>, val middle: List<Flow<Any, Any>>, val last: Flow<Any, O>): Flow<I, O>() {
        override fun visit(visitor: FlowVisitor): Action<I, O> =
            Stream.concat(
                    middle.stream(),
                    Stream.of(last as Flow<Any, Any>))
                    .map { it.visit(visitor) }
                    .reduce(
                            first.visit(visitor) as Action<Any, Any>,
                            { f1, f2 -> visitor.sequence(f1, f2) })
                    as Action<I, O>

        override fun <O2> then(next: Flow<O, O2>): Flow<I, O2> = Sequence(
                first,
                middle.plus(last as Flow<Any, Any>),
                next as Flow<Any, O2>)
    }
}

/**
 * Create a new flow from an operation name and an operation.
 */
fun <I, O> flow(name: String, operation: AsyncFunction<I, O>) = flow(Action(name, operation))

/**
 * Create a new flow from a single action.
 */
fun <I, O> flow(action: Action<I, O>) = Flow.Only(action)