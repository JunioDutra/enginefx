package br.com.engine.main;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFWVulkan.glfwVulkanSupported;
import org.lwjgl.glfw.GLFWErrorCallback;
import br.com.engine.core.ControleBase;
import br.com.engine.core.SceneRegistry;
import br.com.engine.platform.lwjgl.*;

public class LwjglVulkanExecutor
{
    private final RuntimeProfile runtimeProfile;
    private final SceneRegistry sceneRegistry;

    public LwjglVulkanExecutor(RuntimeProfile runtimeProfile, SceneRegistry sceneRegistry)
    {
        this.runtimeProfile = runtimeProfile;
        this.sceneRegistry = sceneRegistry;
        if (runtimeProfile == null || sceneRegistry == null) throw new IllegalArgumentException("Runtime profile and scene registry are required");
    }

    public void start()
    {
        runtimeProfile.requireInitialized();
        GLFWErrorCallback callback = GLFWErrorCallback.createPrint(System.err).set();
        ControleBase control = null;
        try
        {
            if (!glfwInit()) throw new IllegalStateException("Unable to initialize GLFW");
            if (!glfwVulkanSupported()) throw new IllegalStateException("Vulkan is not supported on this machine");
            control = ControleBase.getInstance();
            control.setSceneRegistry(sceneRegistry);
            int width = (int)control.getScreen().getWidth(), height = (int)control.getScreen().getHeight();
            VulkanGraphicsContext graphics = new VulkanGraphicsContext();
            graphics.setCanvasSize(width, height);
            control.getScreen().setGraphicsContext(graphics);
            try (var instance = new LwjglVulkanInstance("enginefx");
                 var window = new LwjglVulkanWindow(instance, width, height, control.getConfigurations().getTitle());
                 var device = new LwjglVulkanDevice(instance, window.getSurface());
                 var renderer = new LwjglVulkanRenderSession(device, window))
            {
                window.show();
                control.setup();
                while (!window.shouldClose() && !control.isExitRequested())
                {
                    try
                    {
                        window.pollEvents();
                        if (window.shouldClose() || control.isExitRequested()) break;
                        control.processLogics();
                        graphics.beginFrame();
                        control.renderGraphics();
                        renderer.drawFrame(graphics);
                    }
                    finally { window.endFrame(); }
                }
            }
        }
        finally
        {
            try { if (control != null) control.stop(); }
            finally { glfwTerminate(); glfwSetErrorCallback(null); callback.free(); }
        }
    }
}
