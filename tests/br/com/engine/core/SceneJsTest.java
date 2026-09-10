package br.com.engine.core;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import br.com.engine.resources.ScenesDefinition;
import br.com.engine.platform.lwjgl.VulkanGraphicsContext;

class SceneJsTest
{
    @Test void loadsConfiguredObjectAndScriptFromClasspathAndPropagatesFailure()
    {
        ControleBase.getInstance().getScreen().setGraphicsContext(new VulkanGraphicsContext());
        Scene scene = new ScenesDefinition("scripts/object.json", "js").getNewScene();
        try
        {
            scene.setup();
            GameObject object = scene.getNode().get(1);
            assertEquals(4f, object.getPosition().x);
            scene.update(16);
            assertEquals(6f, object.getPosition().x);
        }
        finally { scene.dispose(); }
        Scene missing = new ScenesDefinition("scripts/missing.json", "js").getNewScene();
        try { assertThrows(IllegalStateException.class, missing::setup); }
        finally { missing.dispose(); }
    }
}
