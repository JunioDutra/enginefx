package br.com.engine.platform.lwjgl;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.VK_ACCESS_SHADER_READ_BIT;
import static org.lwjgl.vulkan.VK10.VK_ACCESS_TRANSFER_WRITE_BIT;
import static org.lwjgl.vulkan.VK10.VK_COMMAND_BUFFER_LEVEL_PRIMARY;
import static org.lwjgl.vulkan.VK10.VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT;
import static org.lwjgl.vulkan.VK10.VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT;
import static org.lwjgl.vulkan.VK10.VK_COMPONENT_SWIZZLE_IDENTITY;
import static org.lwjgl.vulkan.VK10.VK_FILTER_NEAREST;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_R8G8B8A8_UNORM;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_ASPECT_COLOR_BIT;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_UNDEFINED;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_TILING_OPTIMAL;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_TYPE_2D;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_USAGE_SAMPLED_BIT;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_USAGE_TRANSFER_DST_BIT;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_VIEW_TYPE_2D;
import static org.lwjgl.vulkan.VK10.VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_TRANSFER_BIT;
import static org.lwjgl.vulkan.VK10.VK_QUEUE_FAMILY_IGNORED;
import static org.lwjgl.vulkan.VK10.VK_SAMPLE_COUNT_1_BIT;
import static org.lwjgl.vulkan.VK10.VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE;
import static org.lwjgl.vulkan.VK10.VK_SAMPLER_MIPMAP_MODE_NEAREST;
import static org.lwjgl.vulkan.VK10.VK_SHARING_MODE_EXCLUSIVE;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_SUBMIT_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;
import static org.lwjgl.vulkan.VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER;
import static org.lwjgl.vulkan.VK10.vkAllocateCommandBuffers;
import static org.lwjgl.vulkan.VK10.vkAllocateDescriptorSets;
import static org.lwjgl.vulkan.VK10.vkAllocateMemory;
import static org.lwjgl.vulkan.VK10.vkBeginCommandBuffer;
import static org.lwjgl.vulkan.VK10.vkBindImageMemory;
import static org.lwjgl.vulkan.VK10.vkCmdCopyBufferToImage;
import static org.lwjgl.vulkan.VK10.vkCmdPipelineBarrier;
import static org.lwjgl.vulkan.VK10.vkCreateCommandPool;
import static org.lwjgl.vulkan.VK10.vkCreateDescriptorPool;
import static org.lwjgl.vulkan.VK10.vkCreateImage;
import static org.lwjgl.vulkan.VK10.vkCreateImageView;
import static org.lwjgl.vulkan.VK10.vkCreateSampler;
import static org.lwjgl.vulkan.VK10.vkDestroyCommandPool;
import static org.lwjgl.vulkan.VK10.vkDestroyDescriptorPool;
import static org.lwjgl.vulkan.VK10.vkDestroyImage;
import static org.lwjgl.vulkan.VK10.vkDestroyImageView;
import static org.lwjgl.vulkan.VK10.vkDestroySampler;
import static org.lwjgl.vulkan.VK10.vkEndCommandBuffer;
import static org.lwjgl.vulkan.VK10.vkFreeMemory;
import static org.lwjgl.vulkan.VK10.vkGetImageMemoryRequirements;
import static org.lwjgl.vulkan.VK10.vkGetPhysicalDeviceMemoryProperties;
import static org.lwjgl.vulkan.VK10.vkQueueSubmit;
import static org.lwjgl.vulkan.VK10.vkQueueWaitIdle;
import static org.lwjgl.vulkan.VK10.vkUpdateDescriptorSets;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.IdentityHashMap;
import java.util.Map;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkBufferImageCopy;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandBufferAllocateInfo;
import org.lwjgl.vulkan.VkCommandBufferBeginInfo;
import org.lwjgl.vulkan.VkCommandPoolCreateInfo;
import org.lwjgl.vulkan.VkDescriptorImageInfo;
import org.lwjgl.vulkan.VkDescriptorPoolCreateInfo;
import org.lwjgl.vulkan.VkDescriptorPoolSize;
import org.lwjgl.vulkan.VkDescriptorSetAllocateInfo;
import org.lwjgl.vulkan.VkImageCreateInfo;
import org.lwjgl.vulkan.VkImageMemoryBarrier;
import org.lwjgl.vulkan.VkImageViewCreateInfo;
import org.lwjgl.vulkan.VkMemoryAllocateInfo;
import org.lwjgl.vulkan.VkMemoryRequirements;
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties;
import org.lwjgl.vulkan.VkSamplerCreateInfo;
import org.lwjgl.vulkan.VkSubmitInfo;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

import br.com.engine.graphics.Image;

public class LwjglVulkanTextureCache implements AutoCloseable
{
	private final LwjglVulkanDevice device;
	private final LwjglVulkanImageStagingCache stagingCache;
	private final long descriptorSetLayout;
	private final long descriptorPool;
	private final long commandPool;
	private final Map<Image, Texture> textures = new IdentityHashMap<Image, Texture>( );

	public LwjglVulkanTextureCache( LwjglVulkanDevice device, LwjglVulkanImageStagingCache stagingCache, long descriptorSetLayout )
	{
		this.device = device;
		this.stagingCache = stagingCache;
		this.descriptorSetLayout = descriptorSetLayout;
		this.descriptorPool = createDescriptorPool( );
		commandPool = createCommandPool( );
	}

	public Texture get( Image image )
	{
		return textures.computeIfAbsent( image, this::createTexture );
	}

	/** Caller must have waited for the previous frame fence. */
	public void retain( java.util.Set<Image> required )
	{
		var iterator = textures.entrySet( ).iterator( );
		while( iterator.hasNext( ) )
		{
			var entry = iterator.next( );
			if( required.contains( entry.getKey( ) ) ) continue;
			Texture texture = entry.getValue( );
			int result = org.lwjgl.vulkan.VK10.vkFreeDescriptorSets( device.getLogicalDevice( ), descriptorPool, texture.descriptorSet( ) );
			if( result != VK_SUCCESS ) throw new IllegalStateException( "Cannot release texture descriptor: " + result );
			vkDestroySampler( device.getLogicalDevice( ), texture.sampler( ), null );
			vkDestroyImageView( device.getLogicalDevice( ), texture.imageView( ), null );
			vkDestroyImage( device.getLogicalDevice( ), texture.image( ), null );
			vkFreeMemory( device.getLogicalDevice( ), texture.memory( ), null );
			iterator.remove( );
		}
	}

	@Override
	public void close( )
	{
		for( Texture texture : textures.values( ) )
		{
			vkDestroySampler( device.getLogicalDevice( ), texture.sampler, null );
			vkDestroyImageView( device.getLogicalDevice( ), texture.imageView, null );
			vkDestroyImage( device.getLogicalDevice( ), texture.image, null );
			vkFreeMemory( device.getLogicalDevice( ), texture.memory, null );
		}

		textures.clear( );
		vkDestroyCommandPool( device.getLogicalDevice( ), commandPool, null );
		vkDestroyDescriptorPool( device.getLogicalDevice( ), descriptorPool, null );
	}

	private Texture createTexture( Image image )
	{
		LwjglVulkanImageStagingCache.StagedImage stagedImage = stagingCache.get( image );
		long textureImage = createImage( stagedImage.width( ), stagedImage.height( ) );
		long textureMemory = allocateImageMemory( textureImage );
		long textureImageView = createImageView( textureImage );
		long textureSampler = createSampler( );
		long descriptorSet = allocateDescriptorSet( textureImageView, textureSampler );
		uploadTexture( stagedImage, textureImage );
		stagingCache.release( image );
		return new Texture( textureImage, textureMemory, textureImageView, textureSampler, descriptorSet, stagedImage.width( ), stagedImage.height( ) );
	}


	private long createImage( int width, int height )
	{
		try( MemoryStack stack = stackPush( ) )
		{
			VkImageCreateInfo createInfo = VkImageCreateInfo.calloc( stack )
				.sType( VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO )
				.imageType( VK_IMAGE_TYPE_2D )
				.format( VK_FORMAT_R8G8B8A8_UNORM )
				.mipLevels( 1 )
				.arrayLayers( 1 )
				.samples( VK_SAMPLE_COUNT_1_BIT )
				.tiling( VK_IMAGE_TILING_OPTIMAL )
				.usage( VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT )
				.sharingMode( VK_SHARING_MODE_EXCLUSIVE )
				.initialLayout( VK_IMAGE_LAYOUT_UNDEFINED );

			createInfo.extent( ).set( width, height, 1 );

			LongBuffer pointer = stack.mallocLong( 1 );
			int result = vkCreateImage( device.getLogicalDevice( ), createInfo, null, pointer );

			if( result != VK_SUCCESS )
			{
				throw new IllegalStateException( "Failed to create Vulkan texture image: " + result );
			}

			return pointer.get( 0 );
		}
	}

	private long allocateImageMemory( long image )
	{
		try( MemoryStack stack = stackPush( ) )
		{
			VkMemoryRequirements requirements = VkMemoryRequirements.malloc( stack );
			vkGetImageMemoryRequirements( device.getLogicalDevice( ), image, requirements );

			VkMemoryAllocateInfo allocateInfo = VkMemoryAllocateInfo.calloc( stack )
				.sType( VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO )
				.allocationSize( requirements.size( ) )
				.memoryTypeIndex( findMemoryType( stack, requirements.memoryTypeBits( ), VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT ) );

			LongBuffer pointer = stack.mallocLong( 1 );
			int result = vkAllocateMemory( device.getLogicalDevice( ), allocateInfo, null, pointer );

			if( result != VK_SUCCESS )
			{
				throw new IllegalStateException( "Failed to allocate Vulkan texture memory: " + result );
			}

			result = vkBindImageMemory( device.getLogicalDevice( ), image, pointer.get( 0 ), 0L );

			if( result != VK_SUCCESS )
			{
				throw new IllegalStateException( "Failed to bind Vulkan texture memory: " + result );
			}

			return pointer.get( 0 );
		}
	}

	private long createImageView( long image )
	{
		try( MemoryStack stack = stackPush( ) )
		{
			VkImageViewCreateInfo createInfo = VkImageViewCreateInfo.calloc( stack )
				.sType( VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO )
				.image( image )
				.viewType( VK_IMAGE_VIEW_TYPE_2D )
				.format( VK_FORMAT_R8G8B8A8_UNORM );

			createInfo.components( )
				.r( VK_COMPONENT_SWIZZLE_IDENTITY )
				.g( VK_COMPONENT_SWIZZLE_IDENTITY )
				.b( VK_COMPONENT_SWIZZLE_IDENTITY )
				.a( VK_COMPONENT_SWIZZLE_IDENTITY );

			createInfo.subresourceRange( )
				.aspectMask( VK_IMAGE_ASPECT_COLOR_BIT )
				.baseMipLevel( 0 )
				.levelCount( 1 )
				.baseArrayLayer( 0 )
				.layerCount( 1 );

			LongBuffer pointer = stack.mallocLong( 1 );
			int result = vkCreateImageView( device.getLogicalDevice( ), createInfo, null, pointer );

			if( result != VK_SUCCESS )
			{
				throw new IllegalStateException( "Failed to create Vulkan texture image view: " + result );
			}

			return pointer.get( 0 );
		}
	}

	private long createSampler( )
	{
		try( MemoryStack stack = stackPush( ) )
		{
			VkSamplerCreateInfo createInfo = VkSamplerCreateInfo.calloc( stack )
				.sType( VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO )
				.magFilter( VK_FILTER_NEAREST )
				.minFilter( VK_FILTER_NEAREST )
				.addressModeU( VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE )
				.addressModeV( VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE )
				.addressModeW( VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE )
				.mipmapMode( VK_SAMPLER_MIPMAP_MODE_NEAREST )
				.minLod( 0.0f )
				.maxLod( 0.0f );

			LongBuffer pointer = stack.mallocLong( 1 );
			int result = vkCreateSampler( device.getLogicalDevice( ), createInfo, null, pointer );

			if( result != VK_SUCCESS )
			{
				throw new IllegalStateException( "Failed to create Vulkan texture sampler: " + result );
			}

			return pointer.get( 0 );
		}
	}

	private void uploadTexture( LwjglVulkanImageStagingCache.StagedImage stagedImage, long textureImage )
	{
		try( MemoryStack stack = stackPush( ) )
		{
			VkCommandBuffer commandBuffer = beginSingleTimeCommands( stack );
			transitionImage( stack, commandBuffer, textureImage, VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL );

			VkBufferImageCopy.Buffer copyRegion = VkBufferImageCopy.calloc( 1, stack )
				.bufferOffset( 0L )
				.bufferRowLength( 0 )
				.bufferImageHeight( 0 );

			copyRegion.imageSubresource( )
				.aspectMask( VK_IMAGE_ASPECT_COLOR_BIT )
				.mipLevel( 0 )
				.baseArrayLayer( 0 )
				.layerCount( 1 );

			copyRegion.imageOffset( ).set( 0, 0, 0 );
			copyRegion.imageExtent( ).set( stagedImage.width( ), stagedImage.height( ), 1 );

			vkCmdCopyBufferToImage( commandBuffer, stagedImage.buffer( ), textureImage, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, copyRegion );
			transitionImage( stack, commandBuffer, textureImage, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL );
			endSingleTimeCommands( stack, commandBuffer );
		}
	}

	private VkCommandBuffer beginSingleTimeCommands( MemoryStack stack )
	{
		VkCommandBufferAllocateInfo allocateInfo = VkCommandBufferAllocateInfo.calloc( stack )
			.sType( VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO )
			.commandPool( commandPool )
			.level( VK_COMMAND_BUFFER_LEVEL_PRIMARY )
			.commandBufferCount( 1 );

		PointerBuffer pointer = stack.mallocPointer( 1 );
		int result = vkAllocateCommandBuffers( device.getLogicalDevice( ), allocateInfo, pointer );

		if( result != VK_SUCCESS )
		{
			throw new IllegalStateException( "Failed to allocate Vulkan texture upload command buffer: " + result );
		}

		VkCommandBuffer commandBuffer = new VkCommandBuffer( pointer.get( 0 ), device.getLogicalDevice( ) );
		VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.calloc( stack )
			.sType( VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO )
			.flags( VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT );

		result = vkBeginCommandBuffer( commandBuffer, beginInfo );

		if( result != VK_SUCCESS )
		{
			throw new IllegalStateException( "Failed to begin Vulkan texture upload command buffer: " + result );
		}

		return commandBuffer;
	}

	private void endSingleTimeCommands( MemoryStack stack, VkCommandBuffer commandBuffer )
	{
		int result = vkEndCommandBuffer( commandBuffer );

		if( result != VK_SUCCESS )
		{
			throw new IllegalStateException( "Failed to end Vulkan texture upload command buffer: " + result );
		}

		VkSubmitInfo submitInfo = VkSubmitInfo.calloc( stack )
			.sType( VK_STRUCTURE_TYPE_SUBMIT_INFO )
			.pCommandBuffers( stack.pointers( commandBuffer ) );

		result = vkQueueSubmit( device.getGraphicsQueue( ), submitInfo, 0L );

		if( result != VK_SUCCESS )
		{
			throw new IllegalStateException( "Failed to submit Vulkan texture upload command buffer: " + result );
		}

		result = vkQueueWaitIdle( device.getGraphicsQueue( ) );
		if( result != VK_SUCCESS ) throw new IllegalStateException( "Texture upload wait failed: " + result );
		org.lwjgl.vulkan.VK10.vkFreeCommandBuffers( device.getLogicalDevice( ), commandPool, commandBuffer );
	}

	private void transitionImage( MemoryStack stack, VkCommandBuffer commandBuffer, long image, int oldLayout, int newLayout )
	{
		VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.calloc( 1, stack )
			.sType( VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER )
			.oldLayout( oldLayout )
			.newLayout( newLayout )
			.srcQueueFamilyIndex( VK_QUEUE_FAMILY_IGNORED )
			.dstQueueFamilyIndex( VK_QUEUE_FAMILY_IGNORED )
			.image( image );

		barrier.subresourceRange( )
			.aspectMask( VK_IMAGE_ASPECT_COLOR_BIT )
			.baseMipLevel( 0 )
			.levelCount( 1 )
			.baseArrayLayer( 0 )
			.layerCount( 1 );

		if( newLayout == VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL )
		{
			barrier.srcAccessMask( 0 );
			barrier.dstAccessMask( VK_ACCESS_TRANSFER_WRITE_BIT );
			vkCmdPipelineBarrier( commandBuffer, VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT, 0, null, null, barrier );
		}
		else
		{
			barrier.srcAccessMask( VK_ACCESS_TRANSFER_WRITE_BIT );
			barrier.dstAccessMask( VK_ACCESS_SHADER_READ_BIT );
			vkCmdPipelineBarrier( commandBuffer, VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT, 0, null, null, barrier );
		}
	}

	private long createCommandPool( )
	{
		try( MemoryStack stack = stackPush( ) )
		{
			VkCommandPoolCreateInfo createInfo = VkCommandPoolCreateInfo.calloc( stack )
				.sType( VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO )
				.flags( VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT )
				.queueFamilyIndex( device.getQueueFamilyIndices( ).graphicsFamily( ) );

			LongBuffer pointer = stack.mallocLong( 1 );
			int result = vkCreateCommandPool( device.getLogicalDevice( ), createInfo, null, pointer );

			if( result != VK_SUCCESS )
			{
				throw new IllegalStateException( "Failed to create Vulkan texture command pool: " + result );
			}

			return pointer.get( 0 );
		}
	}

	private int findMemoryType( MemoryStack stack, int typeFilter, int properties )
	{
		VkPhysicalDeviceMemoryProperties memoryProperties = VkPhysicalDeviceMemoryProperties.malloc( stack );
		vkGetPhysicalDeviceMemoryProperties( device.getPhysicalDevice( ), memoryProperties );

		for( int index = 0; index < memoryProperties.memoryTypeCount( ); index++ )
		{
			if( (typeFilter & (1 << index)) != 0 && (memoryProperties.memoryTypes( index ).propertyFlags( ) & properties) == properties )
			{
				return index;
			}
		}

		throw new IllegalStateException( "No compatible Vulkan memory type found" );
	}

	private long createDescriptorPool( )
	{
		try( MemoryStack stack = stackPush( ) )
		{
			VkDescriptorPoolSize.Buffer poolSizes = VkDescriptorPoolSize.calloc( 1, stack )
				.type( VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER )
				.descriptorCount( 1024 );

			VkDescriptorPoolCreateInfo createInfo = VkDescriptorPoolCreateInfo.calloc( stack )
				.sType( VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO )
				.flags( org.lwjgl.vulkan.VK10.VK_DESCRIPTOR_POOL_CREATE_FREE_DESCRIPTOR_SET_BIT )
				.pPoolSizes( poolSizes )
				.maxSets( 1024 );

			LongBuffer pointer = stack.mallocLong( 1 );
			int result = vkCreateDescriptorPool( device.getLogicalDevice( ), createInfo, null, pointer );

			if( result != VK_SUCCESS )
			{
				throw new IllegalStateException( "Failed to create Vulkan dynamic descriptor pool: " + result );
			}

			return pointer.get( 0 );
		}
	}

	private long allocateDescriptorSet( long imageView, long sampler )
	{
		try( MemoryStack stack = stackPush( ) )
		{
			VkDescriptorSetAllocateInfo allocateInfo = VkDescriptorSetAllocateInfo.calloc( stack )
				.sType( VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO )
				.descriptorPool( descriptorPool )
				.pSetLayouts( stack.longs( descriptorSetLayout ) );

			LongBuffer pointer = stack.mallocLong( 1 );
			int result = vkAllocateDescriptorSets( device.getLogicalDevice( ), allocateInfo, pointer );

			if( result != VK_SUCCESS )
			{
				throw new IllegalStateException( "Failed to allocate Vulkan texture descriptor set: " + result );
			}

			long descriptorSet = pointer.get( 0 );

			VkDescriptorImageInfo.Buffer imageInfo = VkDescriptorImageInfo.calloc( 1, stack )
				.imageLayout( VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL )
				.imageView( imageView )
				.sampler( sampler );

			VkWriteDescriptorSet.Buffer descriptorWrite = VkWriteDescriptorSet.calloc( 1, stack )
				.sType( VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET )
				.dstSet( descriptorSet )
				.dstBinding( 0 )
				.dstArrayElement( 0 )
				.descriptorType( VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER )
				.descriptorCount( 1 )
				.pImageInfo( imageInfo );

			vkUpdateDescriptorSets( device.getLogicalDevice( ), descriptorWrite, null );

			return descriptorSet;
		}
	}

	public record Texture( long image, long memory, long imageView, long sampler, long descriptorSet, int width, int height )
	{
	}
}
