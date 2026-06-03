package br.com.engine.platform.lwjgl;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_TRANSFER_SRC_BIT;
import static org.lwjgl.vulkan.VK10.VK_MEMORY_PROPERTY_HOST_COHERENT_BIT;
import static org.lwjgl.vulkan.VK10.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;
import static org.lwjgl.vulkan.VK10.vkAllocateMemory;
import static org.lwjgl.vulkan.VK10.vkBindBufferMemory;
import static org.lwjgl.vulkan.VK10.vkCreateBuffer;
import static org.lwjgl.vulkan.VK10.vkDestroyBuffer;
import static org.lwjgl.vulkan.VK10.vkFreeMemory;
import static org.lwjgl.vulkan.VK10.vkGetBufferMemoryRequirements;
import static org.lwjgl.vulkan.VK10.vkGetPhysicalDeviceMemoryProperties;
import static org.lwjgl.vulkan.VK10.vkMapMemory;
import static org.lwjgl.vulkan.VK10.vkUnmapMemory;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.IdentityHashMap;
import java.util.Map;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkBufferCreateInfo;
import org.lwjgl.vulkan.VkMemoryAllocateInfo;
import org.lwjgl.vulkan.VkMemoryRequirements;
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties;

import br.com.engine.graphics.Image;

public class LwjglVulkanImageStagingCache implements AutoCloseable
{
	private final LwjglVulkanDevice device;
	private final Map<Image, StagedImage> stagedImages = new IdentityHashMap<Image, StagedImage>( );

	public LwjglVulkanImageStagingCache( LwjglVulkanDevice device )
	{
		this.device = device;
	}

	public StagedImage get( Image image )
	{
		return stagedImages.computeIfAbsent( image, this::createStagedImage );
	}

	@Override
	public void close( )
	{
		for( StagedImage stagedImage : stagedImages.values( ) )
		{
			vkDestroyBuffer( device.getLogicalDevice( ), stagedImage.buffer, null );
			vkFreeMemory( device.getLogicalDevice( ), stagedImage.memory, null );
		}

		stagedImages.clear( );
	}

	private StagedImage createStagedImage( Image image )
	{
		int width = (int)image.getWidth( );
		int height = (int)image.getHeight( );
		long size = (long)width * height * 4L;
		long buffer = createBuffer( size );
		long memory = allocateMemory( buffer );
		upload( image.getBufferedImage( ), memory, size );
		return new StagedImage( buffer, memory, width, height );
	}

	private long createBuffer( long size )
	{
		try( MemoryStack stack = stackPush( ) )
		{
			VkBufferCreateInfo createInfo = VkBufferCreateInfo.calloc( stack )
				.sType( VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO )
				.size( size )
				.usage( VK_BUFFER_USAGE_TRANSFER_SRC_BIT )
				.sharingMode( org.lwjgl.vulkan.VK10.VK_SHARING_MODE_EXCLUSIVE );

			LongBuffer pointer = stack.mallocLong( 1 );
			int result = vkCreateBuffer( device.getLogicalDevice( ), createInfo, null, pointer );

			if( result != VK_SUCCESS )
			{
				throw new IllegalStateException( "Failed to create Vulkan image staging buffer: " + result );
			}

			return pointer.get( 0 );
		}
	}

	private long allocateMemory( long buffer )
	{
		try( MemoryStack stack = stackPush( ) )
		{
			VkMemoryRequirements requirements = VkMemoryRequirements.malloc( stack );
			vkGetBufferMemoryRequirements( device.getLogicalDevice( ), buffer, requirements );

			VkMemoryAllocateInfo allocateInfo = VkMemoryAllocateInfo.calloc( stack )
				.sType( VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO )
				.allocationSize( requirements.size( ) )
				.memoryTypeIndex( findMemoryType( stack, requirements.memoryTypeBits( ), VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT ) );

			LongBuffer pointer = stack.mallocLong( 1 );
			int result = vkAllocateMemory( device.getLogicalDevice( ), allocateInfo, null, pointer );

			if( result != VK_SUCCESS )
			{
				throw new IllegalStateException( "Failed to allocate Vulkan image staging memory: " + result );
			}

			result = vkBindBufferMemory( device.getLogicalDevice( ), buffer, pointer.get( 0 ), 0L );

			if( result != VK_SUCCESS )
			{
				throw new IllegalStateException( "Failed to bind Vulkan image staging memory: " + result );
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

	private void upload( BufferedImage image, long memory, long size )
	{
		try( MemoryStack stack = stackPush( ) )
		{
			PointerBuffer dataPointer = stack.mallocPointer( 1 );
			int result = vkMapMemory( device.getLogicalDevice( ), memory, 0L, size, 0, dataPointer );

			if( result != VK_SUCCESS )
			{
				throw new IllegalStateException( "Failed to map Vulkan image staging memory: " + result );
			}

			ByteBuffer target = MemoryUtil.memByteBuffer( dataPointer.get( 0 ), (int)size );

			for( int y = 0; y < image.getHeight( ); y++ )
			{
				for( int x = 0; x < image.getWidth( ); x++ )
				{
					int pixel = image.getRGB( x, y );
					target.put( (byte)((pixel >> 16) & 0xFF) );
					target.put( (byte)((pixel >> 8) & 0xFF) );
					target.put( (byte)(pixel & 0xFF) );
					target.put( (byte)((pixel >> 24) & 0xFF) );
				}
			}

			vkUnmapMemory( device.getLogicalDevice( ), memory );
		}
	}

	public record StagedImage( long buffer, long memory, int width, int height )
	{
	}
}