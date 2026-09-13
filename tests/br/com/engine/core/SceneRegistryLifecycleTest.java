package br.com.engine.core;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import br.com.engine.core.annotation.Bootable;
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
        control.getConfigurations().setScenes(List.of(new ScenesDefinition("game:menu", "java")));
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

    @Test void legacyBootAnnotationDoesNotConstructUnusedScenes() throws Exception
    {
        LegacyBoot.created = 0;
        var control = control();
        control.getConfigurations().setScenes(List.of(
            new ScenesDefinition("game:first", "java"), new ScenesDefinition(LegacyBoot.class.getName(), "java")));
        control.getConfigurations().setBootScene(null);
        control.setSceneRegistry(new SceneRegistry().register("game:first", () -> new Scene() { }));
        try
        {
            control.setup();
            assertEquals(1, control.getBootScene());
            assertEquals(0, LegacyBoot.created);
            control.processLogics();
            assertEquals(1, LegacyBoot.created);
        }
        finally { control.stop(); }
    }

    @Bootable public static class LegacyBoot extends Scene
    {
        static int created;
        public LegacyBoot() { created++; }
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
