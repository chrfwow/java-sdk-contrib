package dev.openfeature.contrib.tools.flagd.core.targeting;

import static dev.openfeature.contrib.tools.flagd.core.targeting.Operator.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import io.github.jamsesso.jsonlogic.JsonLogic;
import io.github.jamsesso.jsonlogic.JsonLogicException;
import io.github.jamsesso.jsonlogic.ast.JsonLogicArray;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import io.github.jamsesso.jsonlogic.evaluator.JsonLogicEvaluator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.converter.ArgumentConversionException;
import org.junit.jupiter.params.converter.ConvertWith;
import org.junit.jupiter.params.converter.TypedArgumentConverter;
import org.junit.jupiter.params.provider.MethodSource;

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
                var test = mapper.readValue(data, TestData.class);
                return test;
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
