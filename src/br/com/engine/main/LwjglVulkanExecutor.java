package br.com.engine.main;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFWVulkan.glfwVulkanSupported;
import org.lwjgl.glfw.GLFWErrorCallback;
import br.com.engine.core.ControleBase;
import br.com.engine.platform.lwjgl.*;

public class LwjglVulkanExecutor
{
    public void start()
    {
        System.setProperty("org.lwjgl.system.memoryBackend", System.getProperty("org.lwjgl.system.memoryBackend", "ffm"));
        GLFWErrorCallback callback = GLFWErrorCallback.createPrint(System.err).set();
        ControleBase control = null;
        try
        {
            if (!glfwInit()) throw new IllegalStateException("Unable to initialize GLFW");
            if (!glfwVulkanSupported()) throw new IllegalStateException("Vulkan is not supported on this machine");
            control = ControleBase.getInstance();
            int width = (int)control.getScreen().getWidth(), height = (int)control.getScreen().getHeight();
            VulkanGraphicsContext graphics = new VulkanGraphicsContext();
            graphics.setCanvasSize(width, height);
            control.getScreen().setGraphicsContext(graphics);
            try (var instance = new LwjglVulkanInstance("enginefx");
                 var window = new LwjglVulkanWindow(instance, width, height, "Enginefx Vulkan");
                 var device = new LwjglVulkanDevice(instance, window.getSurface());
                 var renderer = new LwjglVulkanRenderSession(device, window))
            {
                window.show();
                control.setup();
                while (!window.shouldClose())
                {
                    window.pollEvents();
                    control.processLogics();
                    graphics.beginFrame();
                    control.renderGraphics();
                    renderer.drawFrame(graphics);
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
