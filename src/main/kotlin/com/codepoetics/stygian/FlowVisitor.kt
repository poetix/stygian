package com.codepoetics.stygian

/**
 * Visitable by a FlowVisitor.
 */
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
 * A visitor which determines an execution strategy for a flow.
 */
interface FlowVisitor {
    fun <I, O> action(action: Action<I, O>): Action<I, O> = action
    fun <I> condition(condition: Condition<I>): Condition<I> = condition
    fun <I, O, N> sequence(first: Action<I, O>, next: Action<O, N>): Action<I, N> =
            Action("sequence", { first(it).thenCompose(next) })
    fun <I, O> branch(condition: Condition<I>, ifTrue: Action<I, O>, ifFalse: Action<I, O>): Action<I, O> =
            Action("branch", { input -> condition(input).thenCompose { outcome -> if(outcome) ifTrue(input) else ifFalse(input) }})
}

/**
 * The default FlowVisitor.
 */
object DefaultFlowVisitor : FlowVisitor

/**
 * A FlowVisitor which logs execution of actions and conditions.
 */
class LoggingFlowVisitor(val visitor: FlowVisitor, val logger: (String) -> Unit) : FlowVisitor {
    override fun <I, O> action(action: Action<I, O>): Action<I, O> {
        return visitor.action<I, O>(action).decorate { operation ->
            decorateWithLogging("Action '${action.name}'", operation)
        }
    }

    private fun <I, O> decorateWithLogging(operationName: String, operation: AsyncFunction<I, O>): AsyncFunction<I, O> {
        return { i ->
            logInvoking(operationName, i)
            operation(i).handle { success, failure ->
                if (failure == null) {
                    logSuccess(operationName, success)
                    success
                } else {
                    logFailure(operationName, failure)
                    throw failure
                }
            }
        }
    }

    private fun logFailure(operationName: String, failure: Throwable?) {
        logger("${operationName} failed with <${failure}>")
    }

    private fun <O> logSuccess(operationName: String, success: O) {
        logger("${operationName} completed with <${success}>")
    }

    private fun <I> logInvoking(operationName: String, i: I) {
        logger("Invoking ${operationName} with <${i}>")
    }

    override fun <T> condition(condition: Condition<T>) = visitor.condition(condition).decorate { operation ->
        decorateWithLogging("Condition '${condition.name}'", operation)
    }
}

/**
 * Decorate a visitor with logging behaviour.
 */
fun FlowVisitor.logging(logger: (String) -> Unit): FlowVisitor = LoggingFlowVisitor(this, logger)