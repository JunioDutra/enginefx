package br.com.engine.luaharness;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.stb.STBImage;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK11;

/** Native platform probe kept separate from the Lua selection gate. */
public final class NativePlatformHarness
{
    private NativePlatformHarness() { }

    public static void main(String[] args) throws Exception
    {
        System.setProperty("org.lwjgl.system.memoryBackend", "unsafe");
        verifyStbImageAndFont();
        System.out.println("PASS STB image decode and font metrics");
        verifyVulkan();
        System.out.println("PASS Vulkan loader version query (no rendering)");
        verifyGlfwCallback();
        System.out.println("PASS GLFW native callback trampoline and cleanup");
        verifyAudio();
        System.out.println("PASS native platform harness: GLFW, Vulkan, image, font, callback, audio and shutdown");
    }

    private static void verifyStbImageAndFont() throws IOException
    {
        ByteBuffer image = copyResource("/fixtures/nested/pixel.png");
        try
        {
            int[] width = new int[1], height = new int[1], channels = new int[1];
            ByteBuffer pixels = STBImage.stbi_load_from_memory(image, width, height, channels, 4);
            if (pixels == null) throw new IllegalStateException("STB image decode failed");
            try
            {
                if (width[0] != 1 || height[0] != 1) throw new IllegalStateException("STB image dimensions differ");
            }
            finally { STBImage.stbi_image_free(pixels); }
        }
        finally { MemoryUtil.memFree(image); }

        ByteBuffer font = copyResource("/fixtures/fonts/test.ttf");
        try (MemoryStack stack = MemoryStack.stackPush())
        {
            STBTTFontinfo info = STBTTFontinfo.malloc(stack);
            if (!STBTruetype.stbtt_InitFont(info, font)) throw new IllegalStateException("STB font init failed");
            if (STBTruetype.stbtt_ScaleForPixelHeight(info, 16) <= 0) throw new IllegalStateException("STB font scale failed");
        }
        finally { MemoryUtil.memFree(font); }
    }

    private static void verifyVulkan()
    {
        try (MemoryStack stack = MemoryStack.stackPush())
        {
            IntBuffer version = stack.mallocInt(1);
            if (VK11.vkEnumerateInstanceVersion(version) != VK_SUCCESS || version.get(0) <= 0)
                throw new IllegalStateException("Vulkan instance version query failed");
        }
    }

    private static void verifyGlfwCallback()
    {
        if (!glfwInit()) throw new IllegalStateException("GLFW init failed");
        long window = NULL;
        try
        {
            glfwDefaultWindowHints();
            glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
            glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
            window = glfwCreateWindow(32, 32, "EngineFX native platform harness", NULL, NULL);
            if (window == NULL) throw new IllegalStateException("GLFW window creation failed");
            AtomicBoolean pressed = new AtomicBoolean();
            try (GLFWKeyCallback callback = GLFWKeyCallback.create((handle, key, scancode, action, modifiers) -> {
                if (key == GLFW_KEY_A && action == GLFW_PRESS) pressed.set(true);
            }))
            {
                glfwSetKeyCallback(window, callback);
                try
                {
                    org.lwjgl.system.JNI.invokePV(window, GLFW_KEY_A, 0, GLFW_PRESS, 0, callback.address());
                    glfwPollEvents();
                    if (!pressed.get()) throw new IllegalStateException("GLFW native callback did not run");
                }
                finally { glfwSetKeyCallback(window, null); }
            }
        }
        finally
        {
            if (window != NULL) glfwDestroyWindow(window);
            glfwTerminate();
        }
    }

    private static void verifyAudio() throws Exception
    {
        try (var source = NativePlatformHarness.class.getResourceAsStream("/fixtures/audio/menu_roll.wav");
             AudioInputStream audio = AudioSystem.getAudioInputStream(source))
        {
            Clip clip = AudioSystem.getClip();
            try
            {
                clip.open(audio);
                clip.start();
                clip.stop();
            }
            finally { clip.close(); }
        }
    }

    private static ByteBuffer copyResource(String resource) throws IOException
    {
        try (var source = NativePlatformHarness.class.getResourceAsStream(resource))
        {
            if (source == null) throw new IOException("Missing fixture: " + resource);
            byte[] bytes = source.readAllBytes();
            ByteBuffer result = MemoryUtil.memAlloc(bytes.length);
            return result.put(bytes).flip();
        }
    }
}
