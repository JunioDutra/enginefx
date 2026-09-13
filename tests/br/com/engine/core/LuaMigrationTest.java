package br.com.engine.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import br.com.engine.componentes.scripts.LuaComponent;
import br.com.engine.resources.ClasspathResourceResolver;
import br.com.engine.resources.ScriptTypeRemovedException;
import br.com.engine.resources.ScenesDefinition;
import br.com.engine.platform.lwjgl.VulkanGraphicsContext;
import br.com.engine.scripting.ScriptApiRegistry;
import br.com.engine.scripting.ScriptContext;
import br.com.engine.scripting.ScriptModule;
import br.com.engine.scripting.ScriptRuntime;
import br.com.engine.scripting.ScriptValue;

class LuaMigrationTest
{
    @Test void convertsTheLegacyObjectScriptToAnAuthorizedLuaComponent()
    {
        ControleBase.getInstance().getScreen().setGraphicsContext(new VulkanGraphicsContext());
        ClasspathResourceResolver resolver = new ClasspathResourceResolver("engine-tests", "3.0", "res");
        GameObject object = new GameObject("migrated-object");
        object.getPosition().setPosition(4, 5);
        AtomicBoolean disposed = new AtomicBoolean();
        ScriptApiRegistry apis = new ScriptApiRegistry()
            .register("demo.object", "move", "demo.object", (context, arguments) -> {
                object.getPosition().plus((float)(arguments.get(0).asDouble() * 125.0), 0);
                return ScriptValue.of(null);
            })
            .register("demo.object", "disposed", "demo.object", (context, arguments) -> {
                disposed.set(true);
                return ScriptValue.of(null);
            });
        ScriptContext context = new ScriptContext(Set.of("demo.object"), Map.of(), null, null);
        try (ScriptRuntime runtime = ScriptRuntime.lua(resolver, apis))
        {
            ScriptModule module = runtime.loadModule(resolver.ref("scripts/object.lua"));
            object.addComponente(new LuaComponent(runtime, module, context));
            Scene scene = new Scene() { };
            scene.setup();
            scene.add(object);
            scene.update(16);
            assertEquals(6f, object.getPosition().x);
            scene.dispose();
        }
        assertTrue(disposed.get());
    }

    @Test void rejectsTheRemovedJavaScriptSceneTypeWithAMigrationCode()
    {
        ScriptTypeRemovedException exception = assertThrows(ScriptTypeRemovedException.class,
            () -> new ScenesDefinition("scripts/object.json", "js"));
        assertEquals(ScriptTypeRemovedException.CODE, exception.code());
        assertTrue(exception.getMessage().contains("Lua"));
    }
}
