package br.com.engine.platform.lwjgl;

import static org.junit.jupiter.api.Assertions.*;
import static org.lwjgl.vulkan.KHRSurface.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.system.MemoryStack;

class VulkanPresentModeTest
{
    @AfterEach void clearProperty( ) { System.clearProperty( "enginefx.vulkan.presentMode" ); }

    @Test void autoPrefersMailboxAndFallsBackToFifo( )
    {
        try( MemoryStack stack = MemoryStack.stackPush( ) )
        {
            assertEquals( VK_PRESENT_MODE_MAILBOX_KHR, LwjglVulkanSwapchain.choosePresentMode( stack.ints( VK_PRESENT_MODE_FIFO_KHR, VK_PRESENT_MODE_MAILBOX_KHR ) ) );
            assertEquals( VK_PRESENT_MODE_FIFO_KHR, LwjglVulkanSwapchain.choosePresentMode( stack.ints( VK_PRESENT_MODE_FIFO_KHR ) ) );
        }
    }

    @Test void requestedModeMustBeAvailable( )
    {
        System.setProperty( "enginefx.vulkan.presentMode", "mailbox" );
        try( MemoryStack stack = MemoryStack.stackPush( ) )
        {
            assertThrows( IllegalStateException.class, () -> LwjglVulkanSwapchain.choosePresentMode( stack.ints( VK_PRESENT_MODE_FIFO_KHR ) ) );
        }
    }
}
