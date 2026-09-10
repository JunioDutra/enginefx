package br.com.engine.platform.lwjgl;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT;
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

import java.nio.FloatBuffer;
import java.nio.LongBuffer;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkBufferCreateInfo;
import org.lwjgl.vulkan.VkMemoryAllocateInfo;
import org.lwjgl.vulkan.VkMemoryRequirements;
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties;

public class LwjglVulkanDynamicVertexBuffer implements AutoCloseable
{
	private final LwjglVulkanDevice device;
	private final long buffer;
	private final long memory;
	private final long size;

	public LwjglVulkanDynamicVertexBuffer( LwjglVulkanDevice device, long size )
	{
		this.device = device;
		this.size = size;

		try( MemoryStack stack = stackPush( ) )
		{
			VkBufferCreateInfo bufferInfo = VkBufferCreateInfo.calloc( stack )
				.sType( VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO )
				.size( size )
				.usage( VK_BUFFER_USAGE_VERTEX_BUFFER_BIT )
				.sharingMode( org.lwjgl.vulkan.VK10.VK_SHARING_MODE_EXCLUSIVE );

			LongBuffer pBuffer = stack.mallocLong( 1 );
			int result = vkCreateBuffer( device.getLogicalDevice( ), bufferInfo, null, pBuffer );

			if( result != VK_SUCCESS )
			{
				throw new IllegalStateException( "Failed to create Vulkan dynamic vertex buffer: " + result );
			}

			buffer = pBuffer.get( 0 );

			VkMemoryRequirements memRequirements = VkMemoryRequirements.malloc( stack );
			vkGetBufferMemoryRequirements( device.getLogicalDevice( ), buffer, memRequirements );

			VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.calloc( stack )
				.sType( VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO )
				.allocationSize( memRequirements.size( ) )
				.memoryTypeIndex( findMemoryType( stack, memRequirements.memoryTypeBits( ), VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT ) );

			LongBuffer pBufferMemory = stack.mallocLong( 1 );
			result = vkAllocateMemory( device.getLogicalDevice( ), allocInfo, null, pBufferMemory );

			if( result != VK_SUCCESS )
			{
				throw new IllegalStateException( "Failed to allocate Vulkan dynamic vertex buffer memory: " + result );
			}

			memory = pBufferMemory.get( 0 );

			vkBindBufferMemory( device.getLogicalDevice( ), buffer, memory, 0 );
		}
	}

	public long getBuffer( )
	{
		return buffer;
	}

	public void upload( float[] data, int floatCount )
	{
		int capacity = (int)(size / 4);

		// Draw counts must always match the uploaded range. Never truncate a frame.
		if( floatCount < 0 || floatCount > data.length || floatCount > capacity )
		{
			throw new IllegalArgumentException( "Vertex data exceeds source or GPU buffer capacity: " + floatCount );
		}

		try( MemoryStack stack = stackPush( ) )
		{
			PointerBuffer pointer = stack.mallocPointer( 1 );
			int result = vkMapMemory( device.getLogicalDevice( ), memory, 0, size, 0, pointer );

			if( result != VK_SUCCESS )
			{
				throw new IllegalStateException( "Failed to map Vulkan dynamic vertex buffer memory" );
			}

			FloatBuffer pData = pointer.getFloatBuffer( (int)(size / 4) );
			pData.put( data, 0, floatCount );

			vkUnmapMemory( device.getLogicalDevice( ), memory );
		}
	}

	@Override
	public void close( )
	{
		vkDestroyBuffer( device.getLogicalDevice( ), buffer, null );
		vkFreeMemory( device.getLogicalDevice( ), memory, null );
	}

	private int findMemoryType( MemoryStack stack, int typeFilter, int properties )
	{
		VkPhysicalDeviceMemoryProperties memProperties = VkPhysicalDeviceMemoryProperties.malloc( stack );
		vkGetPhysicalDeviceMemoryProperties( device.getPhysicalDevice( ), memProperties );

		for( int i = 0; i < memProperties.memoryTypeCount( ); i++ )
		{
			if( (typeFilter & (1 << i)) != 0 && (memProperties.memoryTypes( i ).propertyFlags( ) & properties) == properties )
			{
				return i;
			}
		}

		throw new RuntimeException( "Failed to find suitable Vulkan dynamic vertex buffer memory type" );
	}
}
