package dev.openfeature.contrib.providers.flagd.resolver.process;

import com.dylibso.chicory.compiler.MachineFactoryCompiler;
import com.dylibso.chicory.runtime.ExportFunction;
import com.dylibso.chicory.runtime.HostFunction;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.runtime.Memory;
import com.dylibso.chicory.runtime.Store;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.types.FunctionType;
import com.dylibso.chicory.wasm.types.ValType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.openfeature.sdk.EvaluationContext;
import dev.openfeature.sdk.LayeredEvaluationContext;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import com.dylibso.chicory.compiler.MachineFactoryCompiler;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.WasmModule;

@Slf4j
public class WasmResolver {
    Instance instance;
    ExportFunction exportFunction;
    Memory memory;
    ExportFunction alloc;
    ExportFunction dealloc;

    public WasmResolver() {
        var module = Parser.parse(new File(
                "Z:\\Workspaces\\chrfwow\\java-sdk-contrib\\providers\\flagd\\src\\main\\resources\\flagd_evaluator.wasm"));

        instance = Instance.builder(module)
                //.withMemoryFactory(MachineFactoryCompiler::compile)
                .build();

        memory = instance.memory();

        alloc = instance.export("alloc");

        dealloc = instance.export("dealloc");

        exportFunction = instance.export("evaluate_logic");
        log.info("Wasm resolver initialized");
    }

    public String apply(String targeting, LayeredEvaluationContext ctx) throws JsonProcessingException {

        var getter = new HostFunction("value_getter", "get", FunctionType.returning(ValType.ExternRef),
                (Instance instance, long... args) -> {
                    var address = (int) args[0];
                    var len = (int) args[1];
                    var key = memory.readString(address, len);
                    var value = ctx.getValue(key).asString();
                    var valueBytes = value.getBytes();
                    var valuePtr = alloc.apply(valueBytes.length)[0];
                    memory.write((int) valuePtr, valueBytes);
                    return new long[] {valuePtr << 32 | valueBytes.length};
                });

        log.info("Wasm resolver apply targeting {}, ctx {}", targeting, ctx);
        ObjectMapper objectMapper = new ObjectMapper();
        var dataStr = objectMapper.writeValueAsString(ctx);
        var data = dataStr.getBytes();

        var targetingBytes = targeting.getBytes();

        Store store = new Store();
        store.addFunction(getter);

        long rulePtr = alloc.apply(targetingBytes.length)[0];
        long dataPtr = alloc.apply(data.length)[0];
        memory.write((int) rulePtr, targetingBytes);
        memory.write((int) dataPtr, data);

        log.info("alloc");

// Call evaluate_logic
        long packedResult = exportFunction.apply(rulePtr, targetingBytes.length, dataPtr, data.length)[0];

        int resultPtr = (int) (packedResult >>> 32);
        int resultLen = (int) (packedResult & 0xFFFFFFFFL);

// Read result
        byte[] resultBytes = memory.readBytes(resultPtr, resultLen);
        String result = new String(resultBytes, StandardCharsets.UTF_8);

        var map = objectMapper.readValue(result, Map.class);

        log.info("eval {}", result);

        dealloc.apply(rulePtr);
        dealloc.apply(dataPtr);

        log.info("dealloc");

        return map.get("result").toString();
    }
}
