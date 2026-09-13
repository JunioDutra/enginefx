package br.com.engine.core;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import br.com.engine.platform.lwjgl.VulkanGraphicsContext;
import br.com.engine.resources.ScenesDefinition;

class SceneRegistryLifecycleTest
{
    @Test void factoriesRunOnlyWhenVisitedAndBootAliasesResolveBothWays() throws Exception
    {
        var created = new AtomicInteger();
        var disposed = new AtomicInteger();
        var registry = new SceneRegistry().register("game:menu", () -> {
            created.incrementAndGet();
            return new Scene() {
                private boolean closed;
                @Override public void dispose() {
                    if (closed) return;
                    closed = true;
                    super.dispose();
                    disposed.incrementAndGet();
                }
            };
        }).alias("legacy.Menu", "game:menu");
        assertThrows(IllegalArgumentException.class, () -> registry.register("legacy.Menu", () -> new Scene() { }));
        var control = control();
        control.getConfigurations().setScenes(List.of(new ScenesDefinition("game:menu")));
        control.getConfigurations().setBootScene("legacy.Menu");
        control.setSceneRegistry(registry);
        try
        {
            control.setup();
            assertEquals(0, created.get());
            assertEquals(0, control.getBootScene());
            control.processLogics();
            assertEquals(1, created.get());
            control.nextScene(0);
            control.processLogics();
            assertEquals(2, created.get());
            assertEquals(1, disposed.get());
        }
        finally { control.stop(); }
        assertEquals(2, disposed.get());
    }

    @Test void firstRegisteredConfigurationSceneIsTheDeterministicBootFallback() throws Exception
    {
        var firstCreated = new AtomicInteger();
        var secondCreated = new AtomicInteger();
        var control = control();
        control.getConfigurations().setScenes(List.of(
            new ScenesDefinition("game:first"), new ScenesDefinition("game:second")));
        control.getConfigurations().setBootScene(null);
        control.setSceneRegistry(new SceneRegistry()
            .register("game:first", () -> { firstCreated.incrementAndGet(); return new Scene() { }; })
            .register("game:second", () -> { secondCreated.incrementAndGet(); return new Scene() { }; }));
        try
        {
            control.setup();
            assertEquals(0, control.getBootScene());
            assertEquals(0, firstCreated.get());
            assertEquals(0, secondCreated.get());
            control.processLogics();
            assertEquals(1, firstCreated.get());
            assertEquals(0, secondCreated.get());
        }
        finally { control.stop(); }
    }

    @Test void invalidConfigurationDoesNotPartiallyStartTheController() throws Exception
    {
        var control = control();
        var registry = new SceneRegistry().register("game:valid", () -> new Scene() { });
        control.setSceneRegistry(registry);
        control.getConfigurations().setScenes(List.of(new ScenesDefinition("game:valid"), new ScenesDefinition("game:missing")));
        control.getConfigurations().setBootScene(null);
        try
        {
            assertThrows(IllegalArgumentException.class, control::setup);
            assertNull(control.getCurrentScene(), "Invalid config must not start the loading scene");
            assertTrue(control.getSceneDefinitions().isEmpty());
            control.getConfigurations().setScenes(List.of(new ScenesDefinition("game:valid")));
            control.getConfigurations().setBootScene("game:missing");
            assertThrows(IllegalArgumentException.class, control::setup);
            assertNull(control.getCurrentScene());
            assertTrue(control.getSceneDefinitions().isEmpty());
            control.getConfigurations().setBootScene("game:valid");
            assertDoesNotThrow(control::setup);
        }
        finally { control.stop(); }
    }

    private static ControleBase control() throws Exception
    {
        var constructor = ControleBase.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        var control = constructor.newInstance();
        control.getScreen().setGraphicsContext(new VulkanGraphicsContext());
        ControleBase.getInstance().getScreen().setGraphicsContext(new VulkanGraphicsContext());
        return control;
    }
}
