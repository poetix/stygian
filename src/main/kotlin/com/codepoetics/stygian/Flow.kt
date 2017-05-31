package com.codepoetics.stygian

import java.util.stream.Stream
import kotlin.streams.toList

/**
 * Represents a condition together with the flow that should be followed if that condition is met.
 */
data class ConditionalFlow<I, O>(val condition: Condition<I>, val flow: Flow<I, O>)

/**
 * Represents a flow of execution.
 */
sealed class Flow<I, O>: Visitable<I, O>, DescribableFlow {

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
        override fun describe(describer: FlowDescriber): FlowDescriber = describer.describeOnly(action.name)

        override fun visit(visitor: FlowVisitor): Action<I, O> = visitor.action(action)
    }

    /**
     * A branching flow which tests all of its branching conditions, executing the flow associated with the first condition that succeeds, orIf falling through to the default flow otherwise.
     * Note that no ordering is defined on the branching conditions, which may be tested in any order.
     */
    class Branch<I, O>(val default: Flow<I, O>, val branches: Map<String, ConditionalFlow<I, O>>): Flow<I, O>() {
        override fun describe(describer: FlowDescriber): FlowDescriber =
                describer.describeBranch(
                        branches.mapValues { it.value.flow },
                        default)

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
    class Sequence<I, O>(val first: Flow<I, *>, val middle: List<Flow<*, *>>, val last: Flow<*, O>): Flow<I, O>() {
        override fun describe(describer: FlowDescriber): FlowDescriber =
            describer.describeSequence(streamAll().toList())

        private fun streamMiddleToLast(): Stream<Flow<*, *>> =
            Stream.concat(
                    middle.stream(),
                    Stream.of(last)
            )

        private fun streamAll(): Stream<Flow<*, *>> =
            Stream.concat(
                    Stream.of(first),
                    streamMiddleToLast()
            )

        override fun visit(visitor: FlowVisitor): Action<I, O> =
            streamMiddleToLast()
                    .map { it.visit(visitor) as Action<Any, Any> }
                    .reduce(
                            first.visit(visitor) as Action<Any, Any>,
                            { f1, f2 -> visitor.sequence(f1, f2) })
                    as Action<I, O>

        override fun <O2> then(next: Flow<O, O2>): Flow<I, O2> = Sequence(
                first,
                middle.plus(last),
                next as Flow<*, O2>)
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