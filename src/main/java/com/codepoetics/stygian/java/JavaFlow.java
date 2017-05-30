package com.codepoetics.stygian.java;

import com.codepoetics.stygian.*;
import kotlin.Unit;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A Java API for creating and executing flows.
 */
public final class JavaFlow {

    private JavaFlow() {
    }

    public static <I, O> Flow<I, O> flow(String name, Function<I, O> action) {
        return FlowKt.flow(name, i -> CompletableFuture.supplyAsync(() -> action.apply(i)));
    }

    public static <I, O> Flow<I, O> asyncFlow(String name, Function<I, CompletableFuture<O>> action) {
        return FlowKt.flow(name, action::apply);
    }

    public static <I> Condition<I> condition(String name, Predicate<I> predicate) {
        return new Condition<>(name, i -> CompletableFuture.supplyAsync(() -> predicate.test(i)));
    }

    public static <I> Condition<I> asyncCondition(String name, Function<I, CompletableFuture<Boolean>> predicate) {
        return new Condition<>(name, predicate::apply);
    }

    public static <I, O> String describe(Flow<I, O> flow) {
        return FlowDescriberKt.describe(flow);
    }

    public static FlowVisitor getDefaultFlowVisitor() {
        return DefaultFlowVisitor.INSTANCE;
    }

    public static FlowVisitor withLogging(FlowVisitor visitor, Consumer<String> logger) {
        return FlowVisitorKt.logging(visitor, message -> {
            logger.accept(message);
            return Unit.INSTANCE;
        });
    }

    public static <I, O> CompletableFuture<O> run(Visitable<I, O> flow, I input) {
        return run(flow, input, getDefaultFlowVisitor());
    }

    public static <I, O> CompletableFuture<O> runLogging(Visitable<I, O> flow, I input, Consumer<String> logger) {
        return run(flow, input, withLogging(getDefaultFlowVisitor(), logger));
    }

    public static <I, O> CompletableFuture<O> run(Visitable<I, O> flow, I input, FlowVisitor visitor) {
        return flow.run(input, visitor);
    }

}
