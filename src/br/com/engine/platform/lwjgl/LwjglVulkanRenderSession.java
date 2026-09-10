package br.com.engine.platform.lwjgl;

import org.lwjgl.system.MemoryStack;
import static org.lwjgl.glfw.GLFW.*;

/** Owns resources whose lifetime follows the framebuffer size. */
public final class LwjglVulkanRenderSession implements AutoCloseable
{
    private final LwjglVulkanDevice device;
    private final LwjglVulkanWindow window;
    private LwjglVulkanSwapchain swapchain;
    private LwjglVulkanFrameRenderer renderer;
    private int framebufferWidth;
    private int framebufferHeight;

    public LwjglVulkanRenderSession( LwjglVulkanDevice device, LwjglVulkanWindow window )
    {
        this.device = device;
        this.window = window;
    }

    public void drawFrame( VulkanGraphicsContext context )
    {
        try( MemoryStack stack = MemoryStack.stackPush( ) )
        {
            var width = stack.ints( 0 );
            var height = stack.ints( 0 );
            glfwGetFramebufferSize( window.getHandle( ), width, height );
            if( width.get( 0 ) == 0 || height.get( 0 ) == 0 )
            {
                glfwWaitEventsTimeout( 0.05 );
                return;
            }
            if( renderer == null || renderer.isRecreationRequired( ) || width.get( 0 ) != framebufferWidth || height.get( 0 ) != framebufferHeight )
            {
                close( );
                framebufferWidth = width.get( 0 );
                framebufferHeight = height.get( 0 );
                swapchain = new LwjglVulkanSwapchain( device, window.getSurface( ), framebufferWidth, framebufferHeight );
                renderer = new LwjglVulkanFrameRenderer( device, swapchain );
            }
            renderer.drawFrame( context );
        }
    }

    @Override public void close( )
    {
        if( renderer != null ) { renderer.close( ); renderer = null; }
        if( swapchain != null ) { swapchain.close( ); swapchain = null; }
    }
}
