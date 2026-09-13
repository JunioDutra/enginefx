package br.com.engine.scripting;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import br.com.engine.resources.ContentLoader;
import br.com.engine.resources.DirectoryResourceResolver;
import br.com.engine.resources.MemoryResourceResolver;

class LuaScriptRuntimeTest
{
    @TempDir Path temporaryDirectory;

    @AfterEach void clearCaches() { ContentLoader.clearCaches(); }

    @Test void runsEveryLifecycleCallbackWithSafeValuesStateClockAndEvents()
    {
        MemoryResourceResolver resolver = pack("base", "hash-a", "scripts/actor.lua", """
            return {
              setup = function()
                engine.state.set('ready', true)
                engine.events.emit('scene.add', { id = 'actor' })
              end,
              update = function(delta) engine.state.set('update_delta', delta) end,
              fixed_update = function(delta) engine.state.set('fixed_delta', delta) end,
              on_event = function(kind, payload) engine.state.set('event', { kind = kind, amount = payload.amount }) end,
              echo = function(value) return { text = value.text, values = { value.values[1], value.values[2] } } end,
              now = function() return engine.clock.now_millis() end,
              dispose = function() engine.state.set('disposed', true) end
            }
            """);
        List<ScriptEvent> events = new ArrayList<>();
        ScriptContext context = new ScriptContext(Set.of("engine.state", "engine.events", "engine.clock"), Map.of(),
            () -> Instant.ofEpochMilli(1234), events::add);
        try (ScriptRuntime runtime = ScriptRuntime.lua(resolver, new ScriptApiRegistry()))
        {
            ScriptInstance instance = runtime.createInstance(runtime.loadModule(resolver.ref("scripts/actor.lua")), context);
            instance.setup();
            instance.update(0.125);
            instance.fixedUpdate(1.0 / 60.0);
            instance.onEvent(new ScriptEvent("host.tick", ScriptValue.of(Map.of("amount", 7))));
            ScriptValue echo = instance.call("echo", ScriptValue.of(Map.of("text", "ação", "values", List.of(2, 3))));
            assertEquals(Map.of("text", "ação", "values", List.of(2L, 3L)), echo.toJavaObject());
            assertEquals(1234L, instance.call("now").asLong());
            assertTrue(context.state("ready").asBoolean());
            assertEquals(0.125, context.state("update_delta").asDouble());
            assertEquals(1.0 / 60.0, context.state("fixed_delta").asDouble());
            assertEquals(Map.of("kind", "host.tick", "amount", 7L), context.state("event").toJavaObject());
            assertEquals(List.of(new ScriptEvent("scene.add", ScriptValue.of(Map.of("id", "actor")))), events);
            instance.dispose();
            instance.dispose();
            assertTrue(context.state("disposed").asBoolean());
        }
    }

    @Test void keepsGameApisNamespacedAndCapabilityGated()
    {
        MemoryResourceResolver resolver = pack("base", "hash-b", "scripts/api.lua", """
            return { setup = function() engine.state.set('sum', farm.math.add(20, 22)) end }
            """);
        ScriptApiRegistry game = new ScriptApiRegistry()
            .register("farm.math", "add", "farm.math", (context, arguments) ->
                ScriptValue.of(arguments.get(0).asLong() + arguments.get(1).asLong()));
        ScriptContext allowed = new ScriptContext(Set.of("engine.state", "farm.math"), Map.of(), null, null);
        try (ScriptRuntime runtime = ScriptRuntime.lua(resolver, game))
        {
            ScriptInstance instance = runtime.createInstance(runtime.loadModule(resolver.ref("scripts/api.lua")), allowed);
            instance.setup();
            assertEquals(42L, allowed.state("sum").asLong());
        }
        ScriptContext denied = new ScriptContext(Set.of("engine.state"), Map.of(), null, null);
        try (ScriptRuntime runtime = ScriptRuntime.lua(resolver, game))
        {
            ScriptInstance instance = runtime.createInstance(runtime.loadModule(resolver.ref("scripts/api.lua")), denied);
            assertThrows(ScriptException.class, instance::setup);
        }
        assertThrows(IllegalArgumentException.class, () -> new ScriptApiRegistry().register("engine", "bad", null, (context, arguments) -> ScriptValue.of(null)));
    }

    @Test void rejectsUnsafeSourceInvalidCallbacksAndBoundaryValues()
    {
        MemoryResourceResolver unsafe = pack("base", "hash-c", "scripts/unsafe.lua", """
            return { setup = function()
              assert(java == nil and io == nil and os == nil and debug == nil and package == nil and require == nil)
              local ok = pcall(function() return engine:getClass() end)
              assert(not ok)
            end, bad = function() return function() end end, many = function() return 1, 2 end }
            """);
        try (ScriptRuntime runtime = ScriptRuntime.lua(unsafe, new ScriptApiRegistry()))
        {
            ScriptInstance instance = runtime.createInstance(runtime.loadModule(unsafe.ref("scripts/unsafe.lua")), ScriptContext.empty());
            instance.setup();
            assertThrows(ScriptException.class, () -> instance.call("missing"));
            assertThrows(ScriptException.class, () -> instance.call("bad"));
            assertThrows(ScriptException.class, () -> instance.call("many"));
        }

        MemoryResourceResolver syntax = pack("base", "hash-d", "scripts/broken.lua", "return { setup = function( end }");
        try (ScriptRuntime runtime = ScriptRuntime.lua(syntax, new ScriptApiRegistry()))
        {
            assertThrows(ScriptException.class, () -> runtime.createInstance(runtime.loadModule(syntax.ref("scripts/broken.lua")), ScriptContext.empty()));
        }
        assertThrows(IllegalArgumentException.class, () -> ScriptValue.of(Map.of("bad", Double.NaN)));
        assertThrows(IllegalArgumentException.class, () -> ScriptValue.of(Map.of(1, "not-a-string-key")));
    }

    @Test void enforcesInstructionLimitEvenWhenLuaUsesProtectedCalls()
    {
        MemoryResourceResolver resolver = pack("base", "hash-e", "scripts/loop.lua", """
            return { update = function()
              pcall(function() xpcall(function() while true do end end, function(error) return error end) end)
            end }
            """);
        try (ScriptRuntime runtime = ScriptRuntime.lua(resolver, new ScriptApiRegistry(), new ScriptLimits(1024, 5_000, 100, 16, 10_000, 262_144)))
        {
            ScriptInstance instance = runtime.createInstance(runtime.loadModule(resolver.ref("scripts/loop.lua")), ScriptContext.empty());
            assertThrows(ScriptLimitException.class, () -> instance.update(0.016));
        }
    }

    @Test void confinesTheRuntimeToItsCreatingGameThread() throws Exception
    {
        MemoryResourceResolver resolver = pack("base", "hash-f", "scripts/main.lua", "return {}");
        try (ScriptRuntime runtime = ScriptRuntime.lua(resolver, new ScriptApiRegistry()))
        {
            AtomicReference<Throwable> failure = new AtomicReference<>();
            Thread thread = new Thread(() -> {
                try { runtime.loadModule(resolver.ref("scripts/main.lua")); }
                catch (Throwable exception) { failure.set(exception); }
            });
            thread.start();
            thread.join();
            assertInstanceOf(IllegalStateException.class, failure.get());
        }
    }

    @Test void keepsResourcesSeparatedByPackAndRejectsAllResolverEscapes() throws Exception
    {
        byte[] pixel;
        try (InputStream input = getClass().getResourceAsStream("/res/nested/pixel.png")) { pixel = input.readAllBytes(); }
        MemoryResourceResolver first = new MemoryResourceResolver("first", "one", Map.of("same/pixel.png", pixel));
        MemoryResourceResolver second = new MemoryResourceResolver("second", "two", Map.of("same/pixel.png", pixel));
        assertSame(first.image(first.ref("same/pixel.png")), first.image(first.ref("same/pixel.png")));
        assertNotSame(first.image(first.ref("same/pixel.png")), second.image(second.ref("same/pixel.png")));
        assertThrows(IllegalArgumentException.class, () -> first.ref("../same/pixel.png"));
        assertThrows(IllegalArgumentException.class, () -> first.ref("C:/same/pixel.png"));
        assertThrows(IllegalArgumentException.class, () -> first.open(second.ref("same/pixel.png")));

        Path root = Files.createDirectory(temporaryDirectory.resolve("pack"));
        Path outside = Files.createDirectory(temporaryDirectory.resolve("outside"));
        Files.writeString(outside.resolve("module.lua"), "return {}");
        Path link = root.resolve("escape");
        try { Files.createSymbolicLink(link, outside); }
        catch (UnsupportedOperationException | java.nio.file.FileSystemException unavailable)
        {
            Process junction = new ProcessBuilder("cmd", "/c", "mklink", "/J", link.toString(), outside.toString()).start();
            assertEquals(0, junction.waitFor(), "Could not create a symbolic link or directory junction: " + unavailable.getMessage());
        }
        DirectoryResourceResolver directory = new DirectoryResourceResolver("directory", "three", root);
        assertThrows(java.io.IOException.class, () -> directory.open(directory.ref("escape/module.lua")));
    }

    private static MemoryResourceResolver pack(String id, String hash, String path, String source)
    {
        return new MemoryResourceResolver(id, hash, Map.of(path, source.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }
}
