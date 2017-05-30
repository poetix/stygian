package com.codepoetics.stygian.java.json;

import com.codepoetics.stygian.Condition;
import com.codepoetics.stygian.DomainKt;
import com.codepoetics.stygian.json.JsonContext;
import com.codepoetics.stygian.json.JsonFlow;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

public final class JavaJsonFlow {

    private JavaJsonFlow() {
    }

    public static JsonFlow flow(String name, String target, Function<JsonContext, JsonNode> action) {
        return JsonFlow.Companion.flow(name, target, action::apply);
    }

    public static JsonFlow asyncFlow(String name, String target, Function<JsonContext, CompletableFuture<JsonNode>> action) {
        return JsonFlow.Companion.asyncFlow(name, target, action::apply);
    }

    public static JsonFlow flow(String name, String source, String target, UnaryOperator<JsonNode> action) {
        return JsonFlow.Companion.flow(name, source, target, action::apply);
    }

    public static JsonFlow asyncFlow(String name, String source, String target, Function<JsonNode, CompletableFuture<JsonNode>> action) {
        return JsonFlow.Companion.asyncFlow(name, source, target, action::apply);
    }

    public static Condition<JsonContext> condition(String name, Predicate<JsonContext> predicate) {
        return new Condition<>(name, DomainKt.liftAsync(predicate::test));
    }

    public static Condition<JsonContext> asyncCondition(String name, Function<JsonContext, CompletableFuture<Boolean>> predicate) {
        return new Condition<>(name, predicate::apply);
    }

    public static <I> Condition<JsonContext> condition(String name, String source, Predicate<JsonNode> predicate) {
        return new Condition<>(name, DomainKt.liftAsync(c -> c.containsKey(source) && predicate.test(c.get(source))));
    }

    public static <I> Condition<JsonContext> asyncCondition(String name, String source, Function<JsonNode, CompletableFuture<Boolean>> predicate) {
        return new Condition<>(name, c -> c.containsKey(source)
                ? predicate.apply(c.get(source))
                : CompletableFuture.completedFuture(false));
    }
}
