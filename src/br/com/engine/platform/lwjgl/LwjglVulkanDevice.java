package br.com.engine.platform.lwjgl;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceSupportKHR;
import static org.lwjgl.vulkan.KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME;
import static org.lwjgl.vulkan.VK10.VK_QUEUE_GRAPHICS_BIT;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;
import static org.lwjgl.vulkan.VK10.vkCreateDevice;
import static org.lwjgl.vulkan.VK10.vkDestroyDevice;
import static org.lwjgl.vulkan.VK10.vkEnumeratePhysicalDevices;
import static org.lwjgl.vulkan.VK10.vkGetDeviceQueue;
import static org.lwjgl.vulkan.VK10.vkGetPhysicalDeviceQueueFamilyProperties;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkDeviceCreateInfo;
import org.lwjgl.vulkan.VkDeviceQueueCreateInfo;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkQueue;
import org.lwjgl.vulkan.VkQueueFamilyProperties;

public class LwjglVulkanDevice implements AutoCloseable
{
	private final VkPhysicalDevice physicalDevice;
	private final VkDevice logicalDevice;
	private final VkQueue graphicsQueue;
	private final VkQueue presentQueue;
	private final QueueFamilyIndices queueFamilyIndices;

	public LwjglVulkanDevice( LwjglVulkanInstance instance, long surface )
	{
		physicalDevice = choosePhysicalDevice( instance, surface );
		queueFamilyIndices = findQueueFamilies( physicalDevice, surface );
		logicalDevice = createLogicalDevice( physicalDevice, queueFamilyIndices );
		graphicsQueue = getQueue( queueFamilyIndices.graphicsFamily( ) );
		presentQueue = getQueue( queueFamilyIndices.presentFamily( ) );
	}

	public VkPhysicalDevice getPhysicalDevice( )
	{
		return physicalDevice;
	}

	public VkDevice getLogicalDevice( )
	{
		return logicalDevice;
	}

	public VkQueue getGraphicsQueue( )
	{
		return graphicsQueue;
	}

	public VkQueue getPresentQueue( )
	{
		return presentQueue;
	}

	public QueueFamilyIndices getQueueFamilyIndices( )
	{
		return queueFamilyIndices;
	}

	@Override
	public void close( )
	{
		vkDestroyDevice( logicalDevice, null );
	}

	private VkPhysicalDevice choosePhysicalDevice( LwjglVulkanInstance instance, long surface )
	{
		try( MemoryStack stack = stackPush( ) )
		{
			IntBuffer deviceCount = stack.ints( 0 );
			int result = vkEnumeratePhysicalDevices( instance.getHandle( ), deviceCount, null );

			if( result != VK_SUCCESS || deviceCount.get( 0 ) == 0 )
			{
				throw new IllegalStateException( "No Vulkan physical devices are available" );
			}

			PointerBuffer devices = stack.mallocPointer( deviceCount.get( 0 ) );
			result = vkEnumeratePhysicalDevices( instance.getHandle( ), deviceCount, devices );

			if( result != VK_SUCCESS )
			{
				throw new IllegalStateException( "Failed to enumerate Vulkan physical devices: " + result );
			}

			for( int index = 0; index < devices.capacity( ); index++ )
			{
				VkPhysicalDevice candidate = new VkPhysicalDevice( devices.get( index ), instance.getHandle( ) );

				if( findQueueFamilies( candidate, surface ).isComplete( ) )
				{
					return candidate;
				}
			}

			throw new IllegalStateException( "No Vulkan physical device supports graphics and presentation queues" );
		}
	}

	private QueueFamilyIndices findQueueFamilies( VkPhysicalDevice physicalDevice, long surface )
	{
		try( MemoryStack stack = stackPush( ) )
		{
			IntBuffer queueFamilyCount = stack.ints( 0 );
			vkGetPhysicalDeviceQueueFamilyProperties( physicalDevice, queueFamilyCount, null );

			VkQueueFamilyProperties.Buffer queueFamilies = VkQueueFamilyProperties.calloc( queueFamilyCount.get( 0 ), stack );
			vkGetPhysicalDeviceQueueFamilyProperties( physicalDevice, queueFamilyCount, queueFamilies );

			int graphicsFamily = -1;
			int presentFamily = -1;

			for( int index = 0; index < queueFamilies.capacity( ); index++ )
			{
				VkQueueFamilyProperties queueFamily = queueFamilies.get( index );

				if( (queueFamily.queueFlags( ) & VK_QUEUE_GRAPHICS_BIT) != 0 )
				{
					graphicsFamily = index;
				}

				IntBuffer presentSupport = stack.ints( 0 );
				int result = vkGetPhysicalDeviceSurfaceSupportKHR( physicalDevice, index, surface, presentSupport );

				if( result == VK_SUCCESS && presentSupport.get( 0 ) == 1 )
				{
					presentFamily = index;
				}

				if( graphicsFamily >= 0 && presentFamily >= 0 )
				{
					break;
				}
			}

			return new QueueFamilyIndices( graphicsFamily, presentFamily );
		}
	}

	private VkDevice createLogicalDevice( VkPhysicalDevice physicalDevice, QueueFamilyIndices indices )
	{
		try( MemoryStack stack = stackPush( ) )
		{
			int uniqueQueueFamilyCount = indices.graphicsFamily( ) == indices.presentFamily( ) ? 1 : 2;
			VkDeviceQueueCreateInfo.Buffer queueCreateInfos = VkDeviceQueueCreateInfo.calloc( uniqueQueueFamilyCount, stack );
			FloatBuffer queuePriority = stack.floats( 1.0f );

			queueCreateInfos.get( 0 )
				.sType( VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO )
				.queueFamilyIndex( indices.graphicsFamily( ) )
				.pQueuePriorities( queuePriority );

			if( uniqueQueueFamilyCount == 2 )
			{
				queueCreateInfos.get( 1 )
					.sType( VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO )
					.queueFamilyIndex( indices.presentFamily( ) )
					.pQueuePriorities( queuePriority );
			}

			PointerBuffer deviceExtensions = stack.pointers( stack.UTF8( VK_KHR_SWAPCHAIN_EXTENSION_NAME ) );

			VkDeviceCreateInfo createInfo = VkDeviceCreateInfo.calloc( stack )
				.sType( VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO )
				.pQueueCreateInfos( queueCreateInfos )
				.ppEnabledExtensionNames( deviceExtensions );

			PointerBuffer pointer = stack.mallocPointer( 1 );
			int result = vkCreateDevice( physicalDevice, createInfo, null, pointer );

			if( result != VK_SUCCESS )
			{
				throw new IllegalStateException( "Failed to create Vulkan logical device: " + result );
			}

			return new VkDevice( pointer.get( 0 ), physicalDevice, createInfo );
		}
	}

	private VkQueue getQueue( int queueFamily )
	{
		try( MemoryStack stack = stackPush( ) )
		{
			PointerBuffer pointer = stack.mallocPointer( 1 );
			vkGetDeviceQueue( logicalDevice, queueFamily, 0, pointer );
			return new VkQueue( pointer.get( 0 ), logicalDevice );
		}
	}

	public record QueueFamilyIndices( int graphicsFamily, int presentFamily )
	{
		public boolean isComplete( )
		{
			return graphicsFamily >= 0 && presentFamily >= 0;
		}
	}
}