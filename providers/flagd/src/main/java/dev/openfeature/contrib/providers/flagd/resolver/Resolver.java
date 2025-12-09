package dev.openfeature.contrib.providers.flagd.resolver;

import dev.openfeature.sdk.EvaluationContext;
import dev.openfeature.sdk.LayeredEvaluationContext;
import dev.openfeature.sdk.ProviderEvaluation;
import dev.openfeature.sdk.Value;

/** Abstraction that resolves flag values in from some source. */
public interface Resolver {
    void init() throws Exception;

    void shutdown() throws Exception;

    default void onError() {}

    default ProviderEvaluation<Boolean> booleanEvaluation(String key, Boolean defaultValue, EvaluationContext ctx) {
        return booleanEvaluation(key, defaultValue, new LayeredEvaluationContext(null, null, null, ctx));
    }

    default ProviderEvaluation<String> stringEvaluation(String key, String defaultValue, EvaluationContext ctx) {
        return stringEvaluation(key, defaultValue, new LayeredEvaluationContext(null, null, null, ctx));
    }

    default ProviderEvaluation<Double> doubleEvaluation(String key, Double defaultValue, EvaluationContext ctx) {
        return doubleEvaluation(key, defaultValue, new LayeredEvaluationContext(null, null, null, ctx));
    }

    default ProviderEvaluation<Integer> integerEvaluation(String key, Integer defaultValue, EvaluationContext ctx) {
        return integerEvaluation(key, defaultValue, new LayeredEvaluationContext(null, null, null, ctx));
    }

    default ProviderEvaluation<Value> objectEvaluation(String key, Value defaultValue, EvaluationContext ctx) {
        return objectEvaluation(key, defaultValue, new LayeredEvaluationContext(null, null, null, ctx));
    }

    default ProviderEvaluation<Boolean> booleanEvaluation(String key, Boolean defaultValue,
            LayeredEvaluationContext ctx) {
        return booleanEvaluation(key, defaultValue, (EvaluationContext) ctx);
    }

    default ProviderEvaluation<String> stringEvaluation(String key, String defaultValue, LayeredEvaluationContext ctx) {
        return stringEvaluation(key, defaultValue, (EvaluationContext) ctx);
    }

    default ProviderEvaluation<Double> doubleEvaluation(String key, Double defaultValue, LayeredEvaluationContext ctx) {
        return doubleEvaluation(key, defaultValue, (EvaluationContext) ctx);
    }

    default ProviderEvaluation<Integer> integerEvaluation(String key, Integer defaultValue,
            LayeredEvaluationContext ctx) {
        return integerEvaluation(key, defaultValue, (EvaluationContext) ctx);
    }

    default ProviderEvaluation<Value> objectEvaluation(String key, Value defaultValue, LayeredEvaluationContext ctx) {
        return objectEvaluation(key, defaultValue, (EvaluationContext) ctx);
    }
}
