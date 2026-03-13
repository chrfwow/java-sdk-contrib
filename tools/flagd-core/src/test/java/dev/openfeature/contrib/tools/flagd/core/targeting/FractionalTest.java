package dev.openfeature.contrib.tools.flagd.core.targeting;

import static dev.openfeature.contrib.tools.flagd.core.targeting.Operator.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import io.github.jamsesso.jsonlogic.JsonLogic;
import io.github.jamsesso.jsonlogic.JsonLogicException;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluator;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.converter.ArgumentConversionException;
import org.junit.jupiter.params.converter.ConvertWith;
import org.junit.jupiter.params.converter.TypedArgumentConverter;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

class FractionalTest {

    @ParameterizedTest
    @MethodSource("allFilesInDir")
    void validate_emptyJson_targetingReturned(@ConvertWith(FileContentConverter.class) TestData testData)
            throws JsonLogicException {
        // given
        var jsonLogic = new JsonLogic();
        jsonLogic.addOperation(new Fractional());

        Map<String, Object> data = new HashMap<>();
        data.put(FLAG_KEY, "headerColor");
        data.put(TARGET_KEY, "foo@foo.com");
        data.put("tier", "tiervalue");
        data.put("headerColor", "bucket");

        Map<String, String> flagdProperties = new HashMap<>();
        flagdProperties.put(FLAG_KEY, "flagA");
        data.put(FLAGD_PROPS_KEY, flagdProperties);

        Object evaluate = jsonLogic.apply(testData.getRule(), data);
        // then
        assertEquals(testData.result, evaluate);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, -1, Integer.MAX_VALUE, Integer.MAX_VALUE - 1, Integer.MIN_VALUE, Integer.MIN_VALUE + 1})
    void edgeCasesDoNotThrow(int hash) throws JsonLogicException {
        var evaluator = Mockito.mock(JsonLogicEvaluator.class);
        var data = new Object();
        int totalWeight = 8;
        int buckets = 4;
        List<Fractional.FractionProperty> bucketsList = new ArrayList<>(buckets);
        for (int i = 0; i < buckets; i++) {
            bucketsList.add(
                    new Fractional.FractionProperty(evaluator, List.of("bucket" + i, totalWeight / buckets), data, ""));
        }

        AtomicReference<String> result = new AtomicReference<>();
        assertDoesNotThrow(() -> result.set(Fractional.distributeValueFromHash(hash, bucketsList, totalWeight, "")));

        assertNotNull(result.get());
        assertTrue(result.get().startsWith("bucket"));
    }

    @Test
    void statistics() throws JsonLogicException {
        var evaluator = Mockito.mock(JsonLogicEvaluator.class);
        var data = new Object();
        int totalWeight = Integer.MAX_VALUE;
        int buckets = 16;
        int[] hits = new int[buckets];
        List<Fractional.FractionProperty> bucketsList = new ArrayList<>(buckets);
        int weight = totalWeight / buckets;
        for (int i = 0; i < buckets - 1; i++) {
            bucketsList.add(new Fractional.FractionProperty(evaluator, List.of("" + i, weight), data, ""));
        }
        bucketsList.add(new Fractional.FractionProperty(
                evaluator, List.of("" + (buckets - 1), totalWeight - weight * (buckets - 1)), data, ""));

        for (long i = Integer.MIN_VALUE; i <= Integer.MAX_VALUE; i += 127) {
            String bucketStr = Fractional.distributeValueFromHash((int) i, bucketsList, totalWeight, "");
            int bucket = Integer.parseInt(bucketStr);
            hits[bucket]++;
        }

        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int i = 0; i < hits.length; i++) {
            int current = hits[i];
            if (current < min) {
                min = current;
            }
            if (current > max) {
                max = current;
            }
        }

        int delta = max - min;
        assertTrue(
                delta < 3,
                "Delta should be less than 3, but was " + delta + ". Distributions: " + Arrays.toString(hits));
    }

    public static Stream<?> allFilesInDir() throws IOException {
        return Files.list(Paths.get("src", "test", "resources", "fractional"))
                .map(path -> arguments(named(path.getFileName().toString(), path)));
    }

    static class FileContentConverter extends TypedArgumentConverter<Path, TestData> {
        protected FileContentConverter() {
            super(Path.class, TestData.class);
        }

        @Override
        protected TestData convert(Path path) throws ArgumentConversionException {
            try {
                Stream<String> lines = Files.lines(path);
                String data = lines.collect(Collectors.joining("\n"));
                lines.close();
                ObjectMapper mapper = new ObjectMapper();
                return mapper.readValue(data, TestData.class);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    static class TestData {
        @JsonProperty("result")
        Object result;

        @JsonProperty("rule")
        List<Object> rule;

        public String getRule() {
            return "{\"fractional\":" + new Gson().toJson(rule) + "}";
        }
    }
}
