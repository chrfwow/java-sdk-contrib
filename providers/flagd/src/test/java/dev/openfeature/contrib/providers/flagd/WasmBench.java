package dev.openfeature.contrib.providers.flagd;

import dev.openfeature.contrib.providers.flagd.resolver.process.WasmResolver;
import dev.openfeature.contrib.providers.flagd.resolver.process.targeting.Fractional;
import dev.openfeature.contrib.providers.flagd.resolver.process.targeting.SemVer;
import dev.openfeature.contrib.providers.flagd.resolver.process.targeting.StringComp;
import io.github.jamsesso.jsonlogic.JsonLogic;

public class WasmBench {
    WasmResolver wasmResolver = new WasmResolver();
    private static final JsonLogic jsonLogicHandler=new JsonLogic();

    static {
        jsonLogicHandler.addOperation(new Fractional());
        jsonLogicHandler.addOperation(new SemVer());
        jsonLogicHandler.addOperation(new StringComp(StringComp.Type.STARTS_WITH));
        jsonLogicHandler.addOperation(new StringComp(StringComp.Type.ENDS_WITH));
    }

}
