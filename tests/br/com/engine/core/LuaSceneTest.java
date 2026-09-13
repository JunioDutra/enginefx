package br.com.engine.core;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import br.com.engine.componentes.scripts.LuaComponent;
import br.com.engine.resources.MemoryResourceResolver;
import br.com.engine.scripting.ScriptContext;
import br.com.engine.scripting.ScriptEvent;
import br.com.engine.scripting.ScriptModule;
import br.com.engine.scripting.ScriptRuntime;
import br.com.engine.scripting.ScriptValue;
import br.com.engine.platform.lwjgl.VulkanGraphicsContext;

class LuaSceneTest
{
    @Test void composesOnlyDeclaredCommandsAndLuaComponentsUseSecondDeltas()
    {
        ControleBase.getInstance().getScreen().setGraphicsContext(new VulkanGraphicsContext());
        MemoryResourceResolver resolver = new MemoryResourceResolver("base", "lua-scene", Map.of(
            "scripts/scene.lua", "return { setup=function() engine.events.emit('scene.add', {tag='lua'}) end }".getBytes(),
            "scripts/component.lua", "return { update=function(delta) engine.state.set('delta', delta) end, dispose=function() engine.state.set('closed', true) end }".getBytes()));
        ScriptContext sceneContext = new ScriptContext(Set.of("engine.events"), Map.of(), null, null);
        ScriptContext componentContext = new ScriptContext(Set.of("engine.state"), Map.of(), null, null);
        List<LuaSceneCommand> commands = new ArrayList<>();
        try (ScriptRuntime runtime = ScriptRuntime.lua(resolver, new br.com.engine.scripting.ScriptApiRegistry()))
        {
            ScriptModule sceneModule = runtime.loadModule(resolver.ref("scripts/scene.lua"));
            ScriptModule componentModule = runtime.loadModule(resolver.ref("scripts/component.lua"));
            LuaScene scene = new LuaScene(runtime, sceneModule, sceneContext, Set.of("scene.add"), commands::add);
            GameObject object = new GameObject("host");
            object.addComponente(new LuaComponent(runtime, componentModule, componentContext));
            scene.setup();
            scene.add(object);
            scene.update(125);
            assertEquals(List.of(new LuaSceneCommand("scene.add", ScriptValue.of(Map.of("tag", "lua")))), commands);
            assertEquals(0.125, componentContext.state("delta").asDouble());
            scene.dispose();
            assertTrue(componentContext.state("closed").asBoolean());
            assertThrows(IllegalStateException.class, () -> scene.onEvent(new ScriptEvent("host.tick", ScriptValue.of(null))));
        }
    }

    @Test void registersStableSceneIdsWithoutReflection()
    {
        SceneRegistry registry = new SceneRegistry().register("game:menu", () -> new Scene() { }).alias("legacy.Menu", "game:menu");
        assertEquals("game:menu", registry.resolve("legacy.Menu"));
        assertNotNull(registry.create("game:menu"));
        assertNotNull(registry.create("legacy.Menu"));
        assertThrows(IllegalArgumentException.class, () -> registry.create("missing:scene"));
    }

    @Test void failedComponentSetupDisposesOnceAndCannotBeReused()
    {
        var resolver = new MemoryResourceResolver("base", "failed-component", Map.of("main.lua",
            ("return { setup=function() error('setup failed') end, "
                + "dispose=function() engine.state.set('closed', true) end }").getBytes()));
        var context = new ScriptContext(Set.of("engine.state"), Map.of(), null, null);
        try (var runtime = ScriptRuntime.lua(resolver, new br.com.engine.scripting.ScriptApiRegistry()))
        {
            var component = new LuaComponent(runtime, runtime.loadModule(resolver.ref("main.lua")), context);
            assertThrows(br.com.engine.scripting.ScriptException.class, component::setup);
            assertTrue(context.state("closed").asBoolean());
            assertDoesNotThrow(component::dispose);
            assertThrows(IllegalStateException.class, component::setup);
        }
    }
}
