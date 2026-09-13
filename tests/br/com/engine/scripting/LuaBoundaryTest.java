package br.com.engine.scripting;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import br.com.engine.resources.MemoryResourceResolver;

class LuaBoundaryTest
{
    private static final ScriptLimits SMALL = new ScriptLimits(1048576, 100000, 1000, 4, 8, 8);

    @Test void existingValuesAndMapKeysStillObeyRequestedLimits()
    {
        ScriptValue large = ScriptValue.of("longer than eight bytes");
        assertThrows(IllegalArgumentException.class, () -> ScriptValue.of(large, SMALL));
        assertThrows(IllegalArgumentException.class, () -> ScriptValue.of(Map.of("key", large), SMALL));
        assertThrows(IllegalArgumentException.class, () -> ScriptValue.of(Map.of("oversized-key", 1), SMALL));
    }

    @Test void inputAndHostReturnValuesObeyTheRuntimeLimits()
    {
        var resolver = pack("return { echo=function(x) return 1 end, host=function() return game.data() end }");
        var apis = new ScriptApiRegistry().register("game", "data", null, (context, args) -> ScriptValue.of("larger than eight bytes"));
        try (var runtime = ScriptRuntime.lua(resolver, apis, SMALL))
        {
            var instance = runtime.createInstance(runtime.loadModule(resolver.ref("main.lua")), ScriptContext.empty());
            assertThrows(RuntimeException.class, () -> instance.call("echo", ScriptValue.of("larger than eight bytes")));
            assertThrows(ScriptException.class, () -> instance.call("host"));
        }
    }

    @Test void caughtHostErrorsNeverExposeJavaObjects()
    {
        var resolver = pack("return { probe=function() local ok=pcall(function() engine.state.set('x',1) end); "
            + "return type(__jthrowable__) end }");
        try (var runtime = ScriptRuntime.lua(resolver, new ScriptApiRegistry()))
        {
            var instance = runtime.createInstance(runtime.loadModule(resolver.ref("main.lua")), ScriptContext.empty());
            assertEquals("nil", instance.call("probe").asString());
        }
    }

    @Test void lifecycleLookupRunsOnceInsideItsBudget()
    {
        var resolver = pack("local n=0; return setmetatable({read=function() return n end}, {__index=function(t,k) "
            + "if k=='update' then n=n+1; return function() end end end})");
        try (var runtime = ScriptRuntime.lua(resolver, new ScriptApiRegistry()))
        {
            var instance = runtime.createInstance(runtime.loadModule(resolver.ref("main.lua")), ScriptContext.empty());
            instance.update(0.1);
            assertEquals(1L, instance.call("read").asLong());
        }
    }

    @Test void invalidUtf8AndOversizedLuaKeysAreRejected()
    {
        var resolver = pack("return { bad=function() return string.char(255) end, "
            + "keys=function() return { ['oversized-key']=1 } end }");
        try (var runtime = ScriptRuntime.lua(resolver, new ScriptApiRegistry(), SMALL))
        {
            var instance = runtime.createInstance(runtime.loadModule(resolver.ref("main.lua")), ScriptContext.empty());
            assertThrows(ScriptException.class, () -> instance.call("bad"));
            assertThrows(ScriptException.class, () -> instance.call("keys"));
        }
    }

    @Test void boundedConversionRejectsLargeTreesAndCyclesAndKeepsTheInstanceUsable()
    {
        var resolver = pack("return { tree=function() return {{1,2,3},{4,5,6},{7,8,9}} end, "
            + "cycle=function() local t={}; t.self=t; return t end, ok=function() return 42 end }");
        try (var runtime = ScriptRuntime.lua(resolver, new ScriptApiRegistry(), SMALL))
        {
            var instance = runtime.createInstance(runtime.loadModule(resolver.ref("main.lua")), ScriptContext.empty());
            assertThrows(ScriptException.class, () -> instance.call("tree"));
            assertThrows(ScriptException.class, () -> instance.call("cycle"));
            assertEquals(42L, instance.call("ok").asLong());
        }
    }

    @Test void recursiveHostEntryIsRejectedWithoutBreakingTheOuterCallback()
    {
        AtomicReference<ScriptInstance> current = new AtomicReference<>();
        var apis = new ScriptApiRegistry().register("game", "reenter", null, (context, args) -> {
            assertThrows(IllegalStateException.class, () -> current.get().call("inner"));
            return ScriptValue.of(null);
        });
        var resolver = pack("return { inner=function() return 1 end, outer=function() game.reenter(); return 42 end }");
        try (var runtime = ScriptRuntime.lua(resolver, apis))
        {
            current.set(runtime.createInstance(runtime.loadModule(resolver.ref("main.lua")), ScriptContext.empty()));
            assertEquals(42L, current.get().call("outer").asLong());
        }
    }

    @Test void aCachedDependencyCanItselfHaveDeclaredDependencies()
    {
        var resolver = new MemoryResourceResolver("base", "dependencies", Map.of(
            "a.lua", "return {}".getBytes(), "b.lua", "return {}".getBytes(), "c.lua", "return {}".getBytes()));
        try (var runtime = ScriptRuntime.lua(resolver, new ScriptApiRegistry()))
        {
            runtime.loadModule(resolver.ref("b.lua"), List.of(resolver.ref("c.lua")));
            assertDoesNotThrow(() -> runtime.loadModule(resolver.ref("a.lua"), List.of(resolver.ref("b.lua"))));
        }
    }

    @Test void luaCanReturnExplicitNullsAndEmptyListsWithoutJavaObjects()
    {
        var resolver = pack("return { data=function() return {['']=engine.value.null, "
            + "items=engine.value.list({}), values={1,engine.value.null,3}} end }");
        var expected = new java.util.LinkedHashMap<String, Object>();
        expected.put("", null);
        expected.put("items", List.of());
        expected.put("values", java.util.Arrays.asList(1, null, 3));
        try (var runtime = ScriptRuntime.lua(resolver, new ScriptApiRegistry()))
        {
            var instance = runtime.createInstance(runtime.loadModule(resolver.ref("main.lua")), ScriptContext.empty());
            assertEquals(ScriptValue.of(expected), instance.call("data"));
        }
        assertThrows(IllegalArgumentException.class, () -> ScriptValue.of(String.valueOf((char)0xd800)));
    }

    @Test void apiNamesCannotOverwriteLuaGlobalsOrCollideWithNamespaces()
    {
        ScriptApi api = (context, args) -> ScriptValue.of(null);
        assertThrows(IllegalArgumentException.class, () -> new ScriptApiRegistry().register("debug", "hook", null, api));
        assertThrows(IllegalArgumentException.class, () -> new ScriptApiRegistry().register("game", "end", null, api));
        assertThrows(IllegalArgumentException.class, () -> new ScriptApiRegistry()
            .register("game", "data", null, api).register("game.data", "get", null, api));
        assertThrows(IllegalArgumentException.class, () -> new ScriptApiRegistry()
            .register("game.data", "get", null, api).register("game", "data", null, api));
        assertTrue(new ScriptContext(Set.of("*"), Map.of(), null, null).allows("game.data"));
    }

    @Test void resourceIdentityAndNormalizedPathsCannotBeAmbiguous()
    {
        assertThrows(IllegalArgumentException.class, () -> new MemoryResourceResolver("a@b", "c", Map.of()));
        assertThrows(IllegalArgumentException.class, () -> new MemoryResourceResolver("a", "b@c", Map.of()));
        assertThrows(IllegalArgumentException.class, () -> new MemoryResourceResolver("base", "hash",
            Map.of("scripts/main.lua", new byte[0], "scripts\\main.lua", new byte[0])));
    }

    private static MemoryResourceResolver pack(String source)
    {
        return new MemoryResourceResolver("base", "boundary", Map.of("main.lua", source.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }

    @Test void callbackAndDisposeLookupCannotEscapeTheInstructionBudget()
    {
        var resolver = pack("return setmetatable({}, {__index=function() while true do end end})");
        try (var runtime = ScriptRuntime.lua(resolver, new ScriptApiRegistry()))
        {
            var instance = runtime.createInstance(runtime.loadModule(resolver.ref("main.lua")), ScriptContext.empty());
            assertThrows(ScriptLimitException.class, () -> instance.update(0.1));
            assertThrows(ScriptLimitException.class, instance::dispose);
            assertTrue(instance.isDisposed());
            assertDoesNotThrow(instance::dispose);
        }
    }

    @Test void activeInstanceAndRuntimeCannotBeClosedFromAHostCallback()
    {
        AtomicReference<ScriptInstance> current = new AtomicReference<>();
        AtomicReference<ScriptRuntime> owner = new AtomicReference<>();
        var apis = new ScriptApiRegistry().register("game", "close", null, (context, args) -> {
            assertThrows(IllegalStateException.class, current.get()::dispose);
            assertThrows(IllegalStateException.class, owner.get()::close);
            return ScriptValue.of(null);
        });
        var resolver = pack("return { test=function() game.close(); return 42 end }");
        try (var runtime = ScriptRuntime.lua(resolver, apis))
        {
            owner.set(runtime);
            current.set(runtime.createInstance(runtime.loadModule(resolver.ref("main.lua")), ScriptContext.empty()));
            assertEquals(42L, current.get().call("test").asLong());
            assertFalse(current.get().isDisposed());
        }
    }

    @Test void emptyListsAndNestedNullsSurviveTheLuaBoundary()
    {
        var resolver = pack("return { echo=function(value) return value end }");
        var data = new java.util.LinkedHashMap<String, Object>();
        data.put("list", java.util.Arrays.asList(null, 42, null));
        data.put("empty", List.of());
        data.put("null", null);
        try (var runtime = ScriptRuntime.lua(resolver, new ScriptApiRegistry()))
        {
            var instance = runtime.createInstance(runtime.loadModule(resolver.ref("main.lua")), ScriptContext.empty());
            assertEquals(ScriptValue.of(data), instance.call("echo", ScriptValue.of(data)));
        }
    }
}
