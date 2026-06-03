package br.com.engine.platform.lwjgl;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.KHRSwapchain.VK_STRUCTURE_TYPE_PRESENT_INFO_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.vkAcquireNextImageKHR;
import static org.lwjgl.vulkan.KHRSwapchain.vkQueuePresentKHR;
import static org.lwjgl.vulkan.VK10.VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT;
import static org.lwjgl.vulkan.VK10.VK_ACCESS_TRANSFER_WRITE_BIT;
import static org.lwjgl.vulkan.VK10.VK_ATTACHMENT_LOAD_OP_CLEAR;
import static org.lwjgl.vulkan.VK10.VK_ATTACHMENT_STORE_OP_STORE;
import static org.lwjgl.vulkan.VK10.VK_COMMAND_BUFFER_LEVEL_PRIMARY;
import static org.lwjgl.vulkan.VK10.VK_COMMAND_BUFFER_USAGE_SIMULTANEOUS_USE_BIT;
import static org.lwjgl.vulkan.VK10.VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT;
import static org.lwjgl.vulkan.VK10.VK_DEPENDENCY_BY_REGION_BIT;
import static org.lwjgl.vulkan.VK10.VK_FENCE_CREATE_SIGNALED_BIT;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_UNDEFINED;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_BIND_POINT_GRAPHICS;
import static org.lwjgl.vulkan.VK10.VK_SHADER_STAGE_VERTEX_BIT;
import static org.lwjgl.vulkan.VK10.vkCmdBindDescriptorSets;
import static org.lwjgl.vulkan.VK10.vkCmdBindPipeline;
import static org.lwjgl.vulkan.VK10.vkCmdBindVertexBuffers;
import static org.lwjgl.vulkan.VK10.vkCmdDraw;
import static org.lwjgl.vulkan.VK10.vkCmdPushConstants;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_TRANSFER_BIT;
import static org.lwjgl.vulkan.VK10.VK_QUEUE_FAMILY_IGNORED;
import static org.lwjgl.vulkan.VK10.VK_SAMPLE_COUNT_1_BIT;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_FENCE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_SUBPASS_EXTERNAL;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;
import static org.lwjgl.vulkan.VK10.vkAllocateCommandBuffers;
import static org.lwjgl.vulkan.VK10.vkBeginCommandBuffer;
import static org.lwjgl.vulkan.VK10.vkCmdBeginRenderPass;
import static org.lwjgl.vulkan.VK10.vkCmdClearAttachments;
import static org.lwjgl.vulkan.VK10.vkCmdCopyBufferToImage;
import static org.lwjgl.vulkan.VK10.vkCmdEndRenderPass;
import static org.lwjgl.vulkan.VK10.vkCmdPipelineBarrier;
import static org.lwjgl.vulkan.VK10.vkCreateCommandPool;
import static org.lwjgl.vulkan.VK10.vkCreateFence;
import static org.lwjgl.vulkan.VK10.vkCreateFramebuffer;
import static org.lwjgl.vulkan.VK10.vkCreateRenderPass;
import static org.lwjgl.vulkan.VK10.vkCreateSemaphore;
import static org.lwjgl.vulkan.VK10.vkDestroyCommandPool;
import static org.lwjgl.vulkan.VK10.vkDestroyFence;
import static org.lwjgl.vulkan.VK10.vkDestroyFramebuffer;
import static org.lwjgl.vulkan.VK10.vkDestroyRenderPass;
import static org.lwjgl.vulkan.VK10.vkDestroySemaphore;
import static org.lwjgl.vulkan.VK10.vkDeviceWaitIdle;
import static org.lwjgl.vulkan.VK10.vkEndCommandBuffer;
import static org.lwjgl.vulkan.VK10.vkQueueSubmit;
import static org.lwjgl.vulkan.VK10.vkResetCommandBuffer;
import static org.lwjgl.vulkan.VK10.vkResetFences;
import static org.lwjgl.vulkan.VK10.vkWaitForFences;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;

import org.lwjgl.PointerBuffer;
import org.lwjgl.stb.STBTTAlignedQuad;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkAttachmentDescription;
import org.lwjgl.vulkan.VkAttachmentReference;
import org.lwjgl.vulkan.VkClearAttachment;
import org.lwjgl.vulkan.VkClearRect;
import org.lwjgl.vulkan.VkClearValue;
import org.lwjgl.vulkan.VkBufferImageCopy;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandBufferAllocateInfo;
import org.lwjgl.vulkan.VkCommandBufferBeginInfo;
import org.lwjgl.vulkan.VkCommandPoolCreateInfo;
import org.lwjgl.vulkan.VkFenceCreateInfo;
import org.lwjgl.vulkan.VkFramebufferCreateInfo;
import org.lwjgl.vulkan.VkImageMemoryBarrier;
import org.lwjgl.vulkan.VkPresentInfoKHR;
import org.lwjgl.vulkan.VkRect2D;
import org.lwjgl.vulkan.VkRenderPassBeginInfo;
import org.lwjgl.vulkan.VkRenderPassCreateInfo;
import org.lwjgl.vulkan.VkSemaphoreCreateInfo;
import org.lwjgl.vulkan.VkSubmitInfo;
import org.lwjgl.vulkan.VkSubpassDependency;
import org.lwjgl.vulkan.VkSubpassDescription;

public class LwjglVulkanFrameRenderer implements AutoCloseable
{
	private final LwjglVulkanDevice device;
        private final LwjglVulkanSwapchain swapchain;
        private final br.com.engine.graphics.Image whiteImage;
	private final long renderPass;
	private final long[] framebuffers;
	private final long commandPool;
	private final VkCommandBuffer[] commandBuffers;
	private final LwjglVulkanImageStagingCache imageStagingCache;
	private final LwjglVulkanQuadPipeline quadPipeline;
	private final LwjglVulkanTextureCache textureCache;
	private final LwjglVulkanFontCache fontCache;
	private final LwjglVulkanDynamicVertexBuffer vertexBuffer;
	private final long imageAvailableSemaphore;
	private final long renderFinishedSemaphore;
	private final long inFlightFence;
	public LwjglVulkanFrameRenderer( LwjglVulkanDevice device, LwjglVulkanSwapchain swapchain )
	{
		this.device = device;
		this.swapchain = swapchain;
		renderPass = createRenderPass( );
		framebuffers = createFramebuffers( );
		commandPool = createCommandPool( );
		commandBuffers = createCommandBuffers( );
		imageStagingCache = new LwjglVulkanImageStagingCache( device );
		quadPipeline = new LwjglVulkanQuadPipeline( device, renderPass );
		textureCache = new LwjglVulkanTextureCache( device, imageStagingCache, quadPipeline.getDescriptorSetLayout( ) );
		fontCache = new LwjglVulkanFontCache( );
                java.awt.image.BufferedImage b = new java.awt.image.BufferedImage( 1, 1, java.awt.image.BufferedImage.TYPE_INT_ARGB );
                b.setRGB( 0, 0, 0xFFFFFFFF );
                whiteImage = new br.com.engine.graphics.Image( b );
		vertexBuffer = new LwjglVulkanDynamicVertexBuffer( device, 65536 * 6 * 8 * 4 ); // 64k quads, 6 verts, 8 floats, 4 bytes
		imageAvailableSemaphore = createSemaphore( );
		renderFinishedSemaphore = createSemaphore( );
		inFlightFence = createFence( );
	}

	public void drawFrame( )
	{
		drawFrame( null );
	}

	public void drawFrame( VulkanGraphicsContext graphicsContext )
	{
		try( MemoryStack stack = stackPush( ) )
		{
			vkWaitForFences( device.getLogicalDevice( ), stack.longs( inFlightFence ), true, Long.MAX_VALUE );
			vkResetFences( device.getLogicalDevice( ), stack.longs( inFlightFence ) );

			IntBuffer imageIndex = stack.ints( 0 );
			int result = vkAcquireNextImageKHR( device.getLogicalDevice( ), swapchain.getHandle( ), Long.MAX_VALUE, imageAvailableSemaphore, 0L, imageIndex );

			if( result != VK_SUCCESS )
			{
				throw new IllegalStateException( "Failed to acquire Vulkan swapchain image: " + result );
			}

			recordCommandBuffer( stack, imageIndex.get( 0 ), graphicsContext );

			VkSubmitInfo submitInfo = VkSubmitInfo.calloc( stack )
				.sType( org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_SUBMIT_INFO )
				.pWaitSemaphores( stack.longs( imageAvailableSemaphore ) )
				.pWaitDstStageMask( stack.ints( VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT ) )
				.pCommandBuffers( stack.pointers( commandBuffers[imageIndex.get( 0 )] ) )
				.pSignalSemaphores( stack.longs( renderFinishedSemaphore ) );

			result = vkQueueSubmit( device.getGraphicsQueue( ), submitInfo, inFlightFence );

			if( result != VK_SUCCESS )
			{
				throw new IllegalStateException( "Failed to submit Vulkan draw command buffer: " + result );
			}

			VkPresentInfoKHR presentInfo = VkPresentInfoKHR.calloc( stack )
				.sType( VK_STRUCTURE_TYPE_PRESENT_INFO_KHR )
				.pWaitSemaphores( stack.longs( renderFinishedSemaphore ) )
				.swapchainCount( 1 )
				.pSwapchains( stack.longs( swapchain.getHandle( ) ) )
				.pImageIndices( imageIndex );

			result = vkQueuePresentKHR( device.getPresentQueue( ), presentInfo );

			if( result != VK_SUCCESS )
			{
				throw new IllegalStateException( "Failed to present Vulkan swapchain image: " + result );
			}
		}
	}

	@Override
	public void close( )
	{
		vkDeviceWaitIdle( device.getLogicalDevice( ) );
		vkDestroyFence( device.getLogicalDevice( ), inFlightFence, null );
		vkDestroySemaphore( device.getLogicalDevice( ), renderFinishedSemaphore, null );
		vkDestroySemaphore( device.getLogicalDevice( ), imageAvailableSemaphore, null );
		fontCache.close( );
		textureCache.close( );
		vertexBuffer.close( );
		quadPipeline.close( );
		imageStagingCache.close( );
		vkDestroyCommandPool( device.getLogicalDevice( ), commandPool, null );

		for( long framebuffer : framebuffers )
		{
			vkDestroyFramebuffer( device.getLogicalDevice( ), framebuffer, null );
		}

		vkDestroyRenderPass( device.getLogicalDevice( ), renderPass, null );
	}

	private long createRenderPass( )
	{
		try( MemoryStack stack = stackPush( ) )
		{
			VkAttachmentDescription.Buffer colorAttachment = VkAttachmentDescription.calloc( 1, stack )
				.format( swapchain.getImageFormat( ) )
				.samples( VK_SAMPLE_COUNT_1_BIT )
				.loadOp( VK_ATTACHMENT_LOAD_OP_CLEAR )
				.storeOp( VK_ATTACHMENT_STORE_OP_STORE )
				.stencilLoadOp( org.lwjgl.vulkan.VK10.VK_ATTACHMENT_LOAD_OP_DONT_CARE )
				.stencilStoreOp( org.lwjgl.vulkan.VK10.VK_ATTACHMENT_STORE_OP_DONT_CARE )
				.initialLayout( VK_IMAGE_LAYOUT_UNDEFINED )
				.finalLayout( VK_IMAGE_LAYOUT_PRESENT_SRC_KHR );

			VkAttachmentReference.Buffer colorAttachmentReference = VkAttachmentReference.calloc( 1, stack )
				.attachment( 0 )
				.layout( VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL );

			VkSubpassDescription.Buffer subpass = VkSubpassDescription.calloc( 1, stack )
				.pipelineBindPoint( VK_PIPELINE_BIND_POINT_GRAPHICS )
				.colorAttachmentCount( 1 )
				.pColorAttachments( colorAttachmentReference );

			VkSubpassDependency.Buffer dependency = VkSubpassDependency.calloc( 1, stack )
				.srcSubpass( VK_SUBPASS_EXTERNAL )
				.dstSubpass( 0 )
				.srcStageMask( VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT )
				.dstStageMask( VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT )
				.dstAccessMask( VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT )
				.dependencyFlags( VK_DEPENDENCY_BY_REGION_BIT );

			VkRenderPassCreateInfo renderPassInfo = VkRenderPassCreateInfo.calloc( stack )
				.sType( VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO )
				.pAttachments( colorAttachment )
				.pSubpasses( subpass )
				.pDependencies( dependency );

			LongBuffer pointer = stack.mallocLong( 1 );
			int result = vkCreateRenderPass( device.getLogicalDevice( ), renderPassInfo, null, pointer );

			if( result != VK_SUCCESS )
			{
				throw new IllegalStateException( "Failed to create Vulkan render pass: " + result );
			}

			return pointer.get( 0 );
		}
	}

	private long[] createFramebuffers( )
	{
		try( MemoryStack stack = stackPush( ) )
		{
			long[] imageViews = swapchain.getImageViews( );
			long[] framebuffers = new long[imageViews.length];

			for( int index = 0; index < imageViews.length; index++ )
			{
				VkFramebufferCreateInfo framebufferInfo = VkFramebufferCreateInfo.calloc( stack )
					.sType( VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO )
					.renderPass( renderPass )
					.pAttachments( stack.longs( imageViews[index] ) )
					.width( swapchain.getWidth( ) )
					.height( swapchain.getHeight( ) )
					.layers( 1 );

				LongBuffer pointer = stack.mallocLong( 1 );
				int result = vkCreateFramebuffer( device.getLogicalDevice( ), framebufferInfo, null, pointer );

				if( result != VK_SUCCESS )
				{
					throw new IllegalStateException( "Failed to create Vulkan framebuffer: " + result );
				}

				framebuffers[index] = pointer.get( 0 );
			}

			return framebuffers;
		}
	}

	private long createCommandPool( )
	{
		try( MemoryStack stack = stackPush( ) )
		{
			VkCommandPoolCreateInfo poolInfo = VkCommandPoolCreateInfo.calloc( stack )
				.sType( VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO )
				.flags( VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT )
				.queueFamilyIndex( device.getQueueFamilyIndices( ).graphicsFamily( ) );

			LongBuffer pointer = stack.mallocLong( 1 );
			int result = vkCreateCommandPool( device.getLogicalDevice( ), poolInfo, null, pointer );

			if( result != VK_SUCCESS )
			{
				throw new IllegalStateException( "Failed to create Vulkan command pool: " + result );
			}

			return pointer.get( 0 );
		}
	}

	private VkCommandBuffer[] createCommandBuffers( )
	{
		try( MemoryStack stack = stackPush( ) )
		{
			VkCommandBufferAllocateInfo allocationInfo = VkCommandBufferAllocateInfo.calloc( stack )
				.sType( VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO )
				.commandPool( commandPool )
				.level( VK_COMMAND_BUFFER_LEVEL_PRIMARY )
				.commandBufferCount( framebuffers.length );

			PointerBuffer pointers = stack.mallocPointer( framebuffers.length );
			int result = vkAllocateCommandBuffers( device.getLogicalDevice( ), allocationInfo, pointers );

			if( result != VK_SUCCESS )
			{
				throw new IllegalStateException( "Failed to allocate Vulkan command buffers: " + result );
			}

			VkCommandBuffer[] buffers = new VkCommandBuffer[framebuffers.length];

			for( int index = 0; index < buffers.length; index++ )
			{
				buffers[index] = new VkCommandBuffer( pointers.get( index ), device.getLogicalDevice( ) );
			}

			return buffers;
		}
	}

	private static class DrawBatch
	{
		LwjglVulkanTextureCache.Texture texture;
		int vertexOffset;
		int vertexCount;
	}

	    private void recordCommandBuffer( MemoryStack stack, int imageIndex, VulkanGraphicsContext graphicsContext )
    {
        vkResetCommandBuffer( commandBuffers[imageIndex], 0 );

        int totalQuads = 0;
        if( graphicsContext != null )
        {
            for( VulkanGraphicsContext.Command commandRaw : graphicsContext.getCommands( ) )
            {
                if( commandRaw instanceof VulkanGraphicsContext.DrawImageCommand ) totalQuads++;
                else if( commandRaw instanceof VulkanGraphicsContext.FillRectCommand ) totalQuads++;
                else if( commandRaw instanceof VulkanGraphicsContext.FillQuadCommand ) totalQuads++;
                else if( commandRaw instanceof VulkanGraphicsContext.DrawTextCommand c ) totalQuads += c.text( ).length( );
            }
        }

        java.util.List<DrawBatch> batches = new java.util.ArrayList<>( );

        if( graphicsContext != null && totalQuads > 0 )
        {
            float[] vertexData = new float[totalQuads * 6 * 8];
            int offset = 0;

            FloatBuffer xBuffer = stack.floats( 0.0f );
            FloatBuffer yBuffer = stack.floats( 0.0f );
            STBTTAlignedQuad q = STBTTAlignedQuad.malloc( stack );

            for( VulkanGraphicsContext.Command commandRaw : graphicsContext.getCommands( ) )
            {
                if( commandRaw instanceof VulkanGraphicsContext.DrawImageCommand command )
                {
                    LwjglVulkanTextureCache.Texture texture = textureCache.get( command.image( ) );

                    float x0 = (float)command.destinationX( );
                    float y0 = (float)command.destinationY( );
                    float x1 = (float)(command.destinationX( ) + command.destinationWidth( ));
                    float y1 = (float)(command.destinationY( ) + command.destinationHeight( ));

                    float u0 = (float)(command.sourceX( ) / texture.width( ));
                    float v0 = (float)(command.sourceY( ) / texture.height( ));
                    float u1 = (float)((command.sourceX( ) + command.sourceWidth( )) / texture.width( ));
                    float v1 = (float)((command.sourceY( ) + command.sourceHeight( )) / texture.height( ));

                    if( batches.isEmpty() || batches.get( batches.size() - 1 ).texture != texture )
                    {
                        DrawBatch batch = new DrawBatch( );
                        batch.texture = texture;
                        batch.vertexOffset = offset / 8;
                        batches.add( batch );
                    }
                    batches.get( batches.size() - 1 ).vertexCount += 6;

                    offset = putVertex( vertexData, offset, x0, y0, u0, v0, 1, 1, 1, 1 );
                    offset = putVertex( vertexData, offset, x0, y1, u0, v1, 1, 1, 1, 1 );
                    offset = putVertex( vertexData, offset, x1, y0, u1, v0, 1, 1, 1, 1 );

                    offset = putVertex( vertexData, offset, x1, y0, u1, v0, 1, 1, 1, 1 );
                    offset = putVertex( vertexData, offset, x0, y1, u0, v1, 1, 1, 1, 1 );
                    offset = putVertex( vertexData, offset, x1, y1, u1, v1, 1, 1, 1, 1 );
                }
                else if( commandRaw instanceof VulkanGraphicsContext.DrawTextCommand command )
                {
                    LwjglVulkanFontCache.VulkanFont vFont = fontCache.get( command.font( ) );
                    LwjglVulkanTextureCache.Texture texture = textureCache.get( vFont.textureImage );

                    xBuffer.put( 0, (float)command.x( ) );
                    float baselineY = (float)command.y( );
                    if( command.baseline( ) == br.com.engine.graphics.VPos.TOP )
                    {
                        baselineY += vFont.ascent;
                    }
                    yBuffer.put( 0, baselineY );

                    java.awt.Color jColor = command.fill( ).toAwtColor( );
                    float r = jColor.getRed( ) / 255.0f;
                    float g = jColor.getGreen( ) / 255.0f;
                    float b = jColor.getBlue( ) / 255.0f;
                    float a = jColor.getAlpha( ) / 255.0f;

                    for( int i = 0; i < command.text( ).length( ); i++ )
                    {
                        char c = command.text( ).charAt( i );
                        if( c < 32 || c >= 128 )
                        {
                            continue;
                        }

                        STBTruetype.stbtt_GetBakedQuad( vFont.charData, vFont.atlasWidth, vFont.atlasHeight, c - 32, xBuffer, yBuffer, q, true );

                        if( batches.isEmpty() || batches.get( batches.size() - 1 ).texture != texture )
                        {
                            DrawBatch batch = new DrawBatch( );
                            batch.texture = texture;
                            batch.vertexOffset = offset / 8;
                            batches.add( batch );
                        }
                        batches.get( batches.size() - 1 ).vertexCount += 6;

                        offset = putVertex( vertexData, offset, q.x0(), q.y0(), q.s0(), q.t0(), r, g, b, a );
                        offset = putVertex( vertexData, offset, q.x0(), q.y1(), q.s0(), q.t1(), r, g, b, a );
                        offset = putVertex( vertexData, offset, q.x1(), q.y0(), q.s1(), q.t0(), r, g, b, a );

                        offset = putVertex( vertexData, offset, q.x1(), q.y0(), q.s1(), q.t0(), r, g, b, a );
                        offset = putVertex( vertexData, offset, q.x0(), q.y1(), q.s0(), q.t1(), r, g, b, a );
                        offset = putVertex( vertexData, offset, q.x1(), q.y1(), q.s1(), q.t1(), r, g, b, a );
                    }
                }
                else if( commandRaw instanceof VulkanGraphicsContext.FillRectCommand command )
                {
                    LwjglVulkanTextureCache.Texture texture = textureCache.get( whiteImage );

                    float x0 = (float)command.x();
                    float y0 = (float)command.y();
                    float x1 = (float)(command.x() + command.width());
                    float y1 = (float)(command.y() + command.height());

                    java.awt.Color jColor = command.fill().toAwtColor();
                    float r = jColor.getRed() / 255.0f;
                    float g = jColor.getGreen() / 255.0f;
                    float b = jColor.getBlue() / 255.0f;
                    float a = jColor.getAlpha() / 255.0f;

                    if( batches.isEmpty() || batches.get( batches.size() - 1 ).texture != texture )
                    {
                        DrawBatch batch = new DrawBatch( );
                        batch.texture = texture;
                        batch.vertexOffset = offset / 8;
                        batches.add( batch );
                    }
                    batches.get( batches.size() - 1 ).vertexCount += 6;

                    offset = putVertex( vertexData, offset, x0, y0, 0, 0, r, g, b, a );
                    offset = putVertex( vertexData, offset, x0, y1, 0, 0, r, g, b, a );
                    offset = putVertex( vertexData, offset, x1, y0, 0, 0, r, g, b, a );

                    offset = putVertex( vertexData, offset, x1, y0, 0, 0, r, g, b, a );
                    offset = putVertex( vertexData, offset, x0, y1, 0, 0, r, g, b, a );
                    offset = putVertex( vertexData, offset, x1, y1, 0, 0, r, g, b, a );
                }
                else if( commandRaw instanceof VulkanGraphicsContext.FillQuadCommand command )
                {
                    LwjglVulkanTextureCache.Texture texture = textureCache.get( whiteImage );

                    java.awt.Color jColor = command.fill().toAwtColor();
                    float r = jColor.getRed() / 255.0f;
                    float g = jColor.getGreen() / 255.0f;
                    float b = jColor.getBlue() / 255.0f;
                    float a = jColor.getAlpha() / 255.0f;

                    if( batches.isEmpty() || batches.get( batches.size() - 1 ).texture != texture )
                    {
                        DrawBatch batch = new DrawBatch( );
                        batch.texture = texture;
                        batch.vertexOffset = offset / 8;
                        batches.add( batch );
                    }
                    batches.get( batches.size() - 1 ).vertexCount += 6;

                    offset = putVertex( vertexData, offset, (float)command.x0(), (float)command.y0(), 0, 0, r, g, b, a );
                    offset = putVertex( vertexData, offset, (float)command.x1(), (float)command.y1(), 0, 0, r, g, b, a );
                    offset = putVertex( vertexData, offset, (float)command.x3(), (float)command.y3(), 0, 0, r, g, b, a );

                    offset = putVertex( vertexData, offset, (float)command.x3(), (float)command.y3(), 0, 0, r, g, b, a );
                    offset = putVertex( vertexData, offset, (float)command.x1(), (float)command.y1(), 0, 0, r, g, b, a );
                    offset = putVertex( vertexData, offset, (float)command.x2(), (float)command.y2(), 0, 0, r, g, b, a );
                }
            }

            vertexBuffer.upload( vertexData, offset );
        }

        VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc( stack )
                .sType( VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO )
                .flags( VK_COMMAND_BUFFER_USAGE_SIMULTANEOUS_USE_BIT );

        int result = vkBeginCommandBuffer( commandBuffers[imageIndex], beginInfo );

        if( result != VK_SUCCESS )
        {
            throw new IllegalStateException( "Failed to begin Vulkan command buffer: " + result );
        }

        VkClearValue.Buffer clearColor = VkClearValue.calloc( 1, stack );
        clearColor.color( ).float32( 0, 0.08f ).float32( 1, 0.10f ).float32( 2, 0.12f ).float32( 3, 1.0f );

        VkRenderPassBeginInfo renderPassInfo = VkRenderPassBeginInfo.calloc( stack )
                .sType( VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO )
                .renderPass( renderPass )
                .framebuffer( framebuffers[imageIndex] )
                .pClearValues( clearColor );

        renderPassInfo.renderArea( ).offset( ).set( 0, 0 );
        renderPassInfo.renderArea( ).extent( ).set( swapchain.getWidth( ), swapchain.getHeight( ) );

        vkCmdBeginRenderPass( commandBuffers[imageIndex], renderPassInfo, org.lwjgl.vulkan.VK10.VK_SUBPASS_CONTENTS_INLINE );

        org.lwjgl.vulkan.VkViewport.Buffer viewport = org.lwjgl.vulkan.VkViewport.calloc( 1, stack )
                .x( 0.0f )
                .y( 0.0f )
                .width( swapchain.getWidth( ) )
                .height( swapchain.getHeight( ) )
                .minDepth( 0.0f )
                .maxDepth( 1.0f );
        org.lwjgl.vulkan.VK10.vkCmdSetViewport( commandBuffers[imageIndex], 0, viewport );

        VkRect2D.Buffer scissor = VkRect2D.calloc( 1, stack );
        scissor.offset( ).set( 0, 0 );
        scissor.extent( ).set( swapchain.getWidth( ), swapchain.getHeight( ) );
        org.lwjgl.vulkan.VK10.vkCmdSetScissor( commandBuffers[imageIndex], 0, scissor );

        if( !batches.isEmpty() )
        {
            vkCmdBindPipeline( commandBuffers[imageIndex], VK_PIPELINE_BIND_POINT_GRAPHICS, quadPipeline.getPipeline( ) );

            LongBuffer pBuffers = stack.longs( vertexBuffer.getBuffer( ) );
            LongBuffer pOffsets = stack.longs( 0 );
            vkCmdBindVertexBuffers( commandBuffers[imageIndex], 0, pBuffers, pOffsets );

            FloatBuffer pushConstants = stack.floats(
                    2.0f / swapchain.getWidth( ), 0.0f, 0.0f, 0.0f,
                    0.0f, 2.0f / swapchain.getHeight( ), 0.0f, 0.0f,
                    0.0f, 0.0f, -1.0f, 0.0f,
                    -1.0f, -1.0f, 0.0f, 1.0f
            );
            vkCmdPushConstants( commandBuffers[imageIndex], quadPipeline.getPipelineLayout( ), VK_SHADER_STAGE_VERTEX_BIT, 0, pushConstants );

            for( DrawBatch batch : batches )
            {
                LongBuffer pDescriptorSets = stack.longs( batch.texture.descriptorSet( ) );
                vkCmdBindDescriptorSets( commandBuffers[imageIndex], VK_PIPELINE_BIND_POINT_GRAPHICS, quadPipeline.getPipelineLayout( ), 0, pDescriptorSets, null );

                vkCmdDraw( commandBuffers[imageIndex], batch.vertexCount, 1, batch.vertexOffset, 0 );
            }
        }

        vkCmdEndRenderPass( commandBuffers[imageIndex] );

        result = vkEndCommandBuffer( commandBuffers[imageIndex] );

        if( result != VK_SUCCESS )
        {
            throw new IllegalStateException( "Failed to record Vulkan command buffer: " + result );
        }
    }


    private int putVertex( float[] vertexData, int offset, float x, float y, float u, float v, float r, float g, float b, float a )
    {
        vertexData[offset++] = x;
        vertexData[offset++] = y;
        vertexData[offset++] = u;
        vertexData[offset++] = v;
        vertexData[offset++] = r;
        vertexData[offset++] = g;
        vertexData[offset++] = b;
        vertexData[offset++] = a;
        return offset;
    }


    private long createSemaphore( )
	{
		try( MemoryStack stack = stackPush( ) )
		{
			VkSemaphoreCreateInfo semaphoreInfo = VkSemaphoreCreateInfo.calloc( stack )
				.sType( VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO );

			LongBuffer pointer = stack.mallocLong( 1 );
			int result = vkCreateSemaphore( device.getLogicalDevice( ), semaphoreInfo, null, pointer );

			if( result != VK_SUCCESS )
			{
				throw new IllegalStateException( "Failed to create Vulkan semaphore: " + result );
			}

			return pointer.get( 0 );
		}
	}

	private long createFence( )
	{
		try( MemoryStack stack = stackPush( ) )
		{
			VkFenceCreateInfo fenceInfo = VkFenceCreateInfo.calloc( stack )
				.sType( VK_STRUCTURE_TYPE_FENCE_CREATE_INFO )
				.flags( VK_FENCE_CREATE_SIGNALED_BIT );

			LongBuffer pointer = stack.mallocLong( 1 );
			int result = vkCreateFence( device.getLogicalDevice( ), fenceInfo, null, pointer );

			if( result != VK_SUCCESS )
			{
				throw new IllegalStateException( "Failed to create Vulkan fence: " + result );
			}

			return pointer.get( 0 );
		}
	}
}

