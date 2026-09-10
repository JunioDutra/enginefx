package br.com.engine.platform.lwjgl;

import org.junit.jupiter.api.Test;
import org.lwjgl.system.MemoryStack;
import static org.junit.jupiter.api.Assertions.*;
import static org.lwjgl.vulkan.VK10.*;

class VulkanSubmissionTest
{
    @Test void submissionWaitsForAcquisitionBeforeWritingTheImage( )
    {
        // Inspect the native structure actually used by the renderer; no GPU needed.
        try( MemoryStack stack = MemoryStack.stackPush( ) )
        {
            var submit = LwjglVulkanFrameRenderer.createSubmitInfo( stack, 11L, 22L, 33L );
            assertEquals( 1, submit.waitSemaphoreCount( ), "Zero silently disables the acquire wait" );
            assertEquals( 22L, submit.pWaitSemaphores( ).get( 0 ) );
            assertEquals( VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT, submit.pWaitDstStageMask( ).get( 0 ) );
            assertEquals( 1, submit.commandBufferCount( ) );
            assertEquals( 11L, submit.pCommandBuffers( ).get( 0 ) );
            assertEquals( 1, submit.signalSemaphoreCount( ) );
            assertEquals( 33L, submit.pSignalSemaphores( ).get( 0 ) );
        }
    }
}
