package br.com.engine.bootstrap;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import br.com.engine.core.Scene;
import br.com.engine.core.SceneRegistry;
import br.com.engine.main.RuntimeProfile;
import br.com.engine.resources.Configurations;
import br.com.engine.resources.DirectoryResourceResolver;
import br.com.engine.resources.ResourceManager;
import br.com.engine.scripting.*;

/** Native acceptance of packaged configuration, factories and the integrated Lua boundary. */
public final class NativeBootstrapHarness
{
    private NativeBootstrapHarness() { }

    public static void main(String[] arguments)
    {
        if (arguments.length != 1) throw new IllegalArgumentException("Expected one external Lua file");
        RuntimeProfile profile = RuntimeProfile.initialize();
        if (profile != RuntimeProfile.NATIVE_JNI || !"runtime".equals(System.getProperty("org.graalvm.nativeimage.imagecode")))
            throw new IllegalStateException("This harness requires a Native Image process with NATIVE_JNI");

        Configurations configuration = ResourceManager.configurations();
        SceneRegistry scenes = new SceneRegistry().register("native:scene", HarnessScene::new);
        if (!"native:scene".equals(configuration.getBootScene()) || configuration.getScenes().size() != 1
            || !scenes.contains(configuration.getScenes().getFirst().getScene()))
            throw new IllegalStateException("Packaged application.json was not bound to the explicit scene registry");
        if (!(scenes.create(configuration.getBootScene()) instanceof HarnessScene))
            throw new IllegalStateException("Scene registry returned the wrong scene");

        Path external = Path.of(arguments[0]).toAbsolutePath().normalize();
        var resolver = new DirectoryResourceResolver("external", "native", external.getParent());
        var payload = new LinkedHashMap<String, Object>();
        payload.put("text", "acao / ação");
        payload.put("values", Arrays.asList(null, 42L, 0.25, null));
        payload.put("empty", List.of());
        ScriptValue expected = ScriptValue.of(payload);
        AtomicInteger callbacks = new AtomicInteger();
        var apis = new ScriptApiRegistry().register("game", "echo", "game.echo", (context, values) -> {
            if (values.size() != 1 || !expected.equals(values.getFirst()))
                throw new IllegalStateException("Native host boundary mismatch");
            callbacks.incrementAndGet();
            return values.getFirst();
        });
        try (ScriptRuntime runtime = ScriptRuntime.lua(resolver, apis))
        {
            ScriptModule module = runtime.loadModule(resolver.ref(external.getFileName().toString()));
            for (int cycle = 0; cycle < 32; cycle++)
            {
                var context = new ScriptContext(Set.of("engine.state", "game.echo"), Map.of(), null, null);
                ScriptInstance instance = runtime.createInstance(module, context);
                try (instance)
                {
                    instance.setup();
                    instance.update(0.125);
                    instance.fixedUpdate(0.25);
                    instance.onEvent(new ScriptEvent("host.tick", ScriptValue.of(7)));
                    if (!context.state("ready").asBoolean() || context.state("delta").asDouble() != 0.125
                        || context.state("fixed").asDouble() != 0.25 || context.state("event").asLong() != 7)
                        throw new IllegalStateException("Native lifecycle callback mismatch");
                    if (instance.call("value").asLong() != 84)
                        throw new IllegalStateException("External Lua value mismatch");
                    if (!expected.equals(instance.call("echo", expected)))
                        throw new IllegalStateException("Native roundtrip mismatch");
                }
                instance.dispose();
                if (!instance.isDisposed() || !context.state("disposed").asBoolean())
                    throw new IllegalStateException("Native dispose lifecycle mismatch");
            }
        }
        if (callbacks.get() != 32) throw new IllegalStateException("Native host callback count mismatch");
        System.out.println("PASS native bootstrap: registry=true resources=true externalLua=true profile=" + profile
            + " lifecycle=32 hostCallbacks=32 values=true");
    }

    private static final class HarnessScene extends Scene { }
}
