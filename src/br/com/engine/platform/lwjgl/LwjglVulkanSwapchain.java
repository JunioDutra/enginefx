package br.com.engine.platform.lwjgl;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.KHRSurface.VK_COLOR_SPACE_SRGB_NONLINEAR_KHR;
import static org.lwjgl.vulkan.KHRSurface.VK_PRESENT_MODE_FIFO_KHR;
import static org.lwjgl.vulkan.KHRSurface.VK_PRESENT_MODE_MAILBOX_KHR;
import static org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceCapabilitiesKHR;
import static org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceFormatsKHR;
import static org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfacePresentModesKHR;
import static org.lwjgl.vulkan.KHRSwapchain.VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.vkCreateSwapchainKHR;
import static org.lwjgl.vulkan.KHRSwapchain.vkDestroySwapchainKHR;
import static org.lwjgl.vulkan.KHRSwapchain.vkGetSwapchainImagesKHR;
import static org.lwjgl.vulkan.VK10.VK_COMPONENT_SWIZZLE_IDENTITY;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_B8G8R8A8_SRGB;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_ASPECT_COLOR_BIT;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_USAGE_TRANSFER_DST_BIT;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_VIEW_TYPE_2D;
import static org.lwjgl.vulkan.VK10.VK_SHARING_MODE_CONCURRENT;
import static org.lwjgl.vulkan.VK10.VK_SHARING_MODE_EXCLUSIVE;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;
import static org.lwjgl.vulkan.VK10.vkCreateImageView;
import static org.lwjgl.vulkan.VK10.vkDestroyImageView;

import java.nio.IntBuffer;
import java.nio.LongBuffer;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkImageViewCreateInfo;
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR;
import org.lwjgl.vulkan.VkSurfaceFormatKHR;
import org.lwjgl.vulkan.VkSwapchainCreateInfoKHR;

import br.com.engine.platform.lwjgl.LwjglVulkanDevice.QueueFamilyIndices;

public class LwjglVulkanSwapchain implements AutoCloseable
{
	private final LwjglVulkanDevice device;
	private final long handle;
	private final int imageFormat;
	private final int width;
	private final int height;
	private final long[] images;
	private final long[] imageViews;

	public LwjglVulkanSwapchain( LwjglVulkanDevice device, long surface, int requestedWidth, int requestedHeight )
	{
		this.device = device;

		try( MemoryStack stack = stackPush( ) )
		{
			SwapchainSupportDetails support = querySwapchainSupport( stack, device, surface );
			VkSurfaceFormatKHR surfaceFormat = chooseSurfaceFormat( support.formats );
			int presentMode = choosePresentMode( support.presentModes );
			VkExtent2D extent = chooseExtent( stack, support.capabilities, requestedWidth, requestedHeight );
			int imageCount = chooseImageCount( support.capabilities );

			handle = createSwapchain( stack, surface, surfaceFormat, presentMode, extent, imageCount );
			imageFormat = surfaceFormat.format( );
			width = extent.width( );
			height = extent.height( );
			images = getSwapchainImages( stack );
			imageViews = createImageViews( stack );
		}
	}

	public long getHandle( )
	{
		return handle;
	}

	public int getImageFormat( )
	{
		return imageFormat;
	}

	public int getWidth( )
	{
		return width;
	}

	public int getHeight( )
	{
		return height;
	}

	public int getImageCount( )
	{
		return images.length;
	}

	public long[] getImageViews( )
	{
		return imageViews.clone( );
	}

	public long getImage( int index )
	{
		return images[index];
	}

	@Override
	public void close( )
	{
		for( long imageView : imageViews )
		{
			vkDestroyImageView( device.getLogicalDevice( ), imageView, null );
		}

		vkDestroySwapchainKHR( device.getLogicalDevice( ), handle, null );
	}

	private SwapchainSupportDetails querySwapchainSupport( MemoryStack stack, LwjglVulkanDevice device, long surface )
	{
		VkSurfaceCapabilitiesKHR capabilities = VkSurfaceCapabilitiesKHR.malloc( stack );
		int result = vkGetPhysicalDeviceSurfaceCapabilitiesKHR( device.getPhysicalDevice( ), surface, capabilities );

		if( result != VK_SUCCESS )
		{
			throw new IllegalStateException( "Failed to query Vulkan surface capabilities: " + result );
		}

		IntBuffer formatCount = stack.ints( 0 );
		result = vkGetPhysicalDeviceSurfaceFormatsKHR( device.getPhysicalDevice( ), surface, formatCount, null );

		if( result != VK_SUCCESS || formatCount.get( 0 ) == 0 )
		{
			throw new IllegalStateException( "No Vulkan surface formats are available" );
		}

		VkSurfaceFormatKHR.Buffer formats = VkSurfaceFormatKHR.malloc( formatCount.get( 0 ), stack );
		result = vkGetPhysicalDeviceSurfaceFormatsKHR( device.getPhysicalDevice( ), surface, formatCount, formats );

		if( result != VK_SUCCESS )
		{
			throw new IllegalStateException( "Failed to query Vulkan surface formats: " + result );
		}

		IntBuffer presentModeCount = stack.ints( 0 );
		result = vkGetPhysicalDeviceSurfacePresentModesKHR( device.getPhysicalDevice( ), surface, presentModeCount, null );

		if( result != VK_SUCCESS || presentModeCount.get( 0 ) == 0 )
		{
			throw new IllegalStateException( "No Vulkan present modes are available" );
		}

		IntBuffer presentModes = stack.mallocInt( presentModeCount.get( 0 ) );
		result = vkGetPhysicalDeviceSurfacePresentModesKHR( device.getPhysicalDevice( ), surface, presentModeCount, presentModes );

		if( result != VK_SUCCESS )
		{
			throw new IllegalStateException( "Failed to query Vulkan present modes: " + result );
		}

		return new SwapchainSupportDetails( capabilities, formats, presentModes );
	}

	private VkSurfaceFormatKHR chooseSurfaceFormat( VkSurfaceFormatKHR.Buffer formats )
	{
		for( int index = 0; index < formats.capacity( ); index++ )
		{
			VkSurfaceFormatKHR format = formats.get( index );

			if( format.format( ) == VK_FORMAT_B8G8R8A8_SRGB && format.colorSpace( ) == VK_COLOR_SPACE_SRGB_NONLINEAR_KHR )
			{
				return format;
			}
		}

		return formats.get( 0 );
	}

	private int choosePresentMode( IntBuffer presentModes )
	{
		for( int index = 0; index < presentModes.capacity( ); index++ )
		{
			if( presentModes.get( index ) == VK_PRESENT_MODE_MAILBOX_KHR )
			{
				return VK_PRESENT_MODE_MAILBOX_KHR;
			}
		}

		return VK_PRESENT_MODE_FIFO_KHR;
	}

	private VkExtent2D chooseExtent( MemoryStack stack, VkSurfaceCapabilitiesKHR capabilities, int requestedWidth, int requestedHeight )
	{
		if( capabilities.currentExtent( ).width( ) != -1 )
		{
			return VkExtent2D.malloc( stack ).set( capabilities.currentExtent( ) );
		}

		int width = Math.max( capabilities.minImageExtent( ).width( ), Math.min( capabilities.maxImageExtent( ).width( ), requestedWidth ) );
		int height = Math.max( capabilities.minImageExtent( ).height( ), Math.min( capabilities.maxImageExtent( ).height( ), requestedHeight ) );

		return VkExtent2D.malloc( stack ).set( width, height );
	}

	private int chooseImageCount( VkSurfaceCapabilitiesKHR capabilities )
	{
		int imageCount = capabilities.minImageCount( ) + 1;

		if( capabilities.maxImageCount( ) > 0 && imageCount > capabilities.maxImageCount( ) )
		{
			imageCount = capabilities.maxImageCount( );
		}

		return imageCount;
	}

	private long createSwapchain( MemoryStack stack, long surface, VkSurfaceFormatKHR surfaceFormat, int presentMode, VkExtent2D extent, int imageCount )
	{
		QueueFamilyIndices indices = device.getQueueFamilyIndices( );
		IntBuffer queueFamilyIndices = stack.ints( indices.graphicsFamily( ), indices.presentFamily( ) );

		VkSwapchainCreateInfoKHR createInfo = VkSwapchainCreateInfoKHR.calloc( stack )
			.sType( VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR )
			.surface( surface )
			.minImageCount( imageCount )
			.imageFormat( surfaceFormat.format( ) )
			.imageColorSpace( surfaceFormat.colorSpace( ) )
			.imageExtent( extent )
			.imageArrayLayers( 1 )
			.imageUsage( VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT | VK_IMAGE_USAGE_TRANSFER_DST_BIT )
			.preTransform( queryCurrentTransform( surface ) )
			.compositeAlpha( org.lwjgl.vulkan.KHRSurface.VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR )
			.presentMode( presentMode )
			.clipped( true )
			.oldSwapchain( 0L );

		if( indices.graphicsFamily( ) != indices.presentFamily( ) )
		{
			createInfo.imageSharingMode( VK_SHARING_MODE_CONCURRENT )
				.pQueueFamilyIndices( queueFamilyIndices );
		}
		else
		{
			createInfo.imageSharingMode( VK_SHARING_MODE_EXCLUSIVE );
		}

		LongBuffer pointer = stack.mallocLong( 1 );
		int result = vkCreateSwapchainKHR( device.getLogicalDevice( ), createInfo, null, pointer );

		if( result != VK_SUCCESS )
		{
			throw new IllegalStateException( "Failed to create Vulkan swapchain: " + result );
		}

		return pointer.get( 0 );
	}

	private int queryCurrentTransform( long surface )
	{
		try( MemoryStack stack = stackPush( ) )
		{
			VkSurfaceCapabilitiesKHR capabilities = VkSurfaceCapabilitiesKHR.malloc( stack );
			int result = vkGetPhysicalDeviceSurfaceCapabilitiesKHR( device.getPhysicalDevice( ), surface, capabilities );

			if( result != VK_SUCCESS )
			{
				throw new IllegalStateException( "Failed to query Vulkan surface transform: " + result );
			}

			return capabilities.currentTransform( );
		}
	}

	private long[] getSwapchainImages( MemoryStack stack )
	{
		IntBuffer imageCount = stack.ints( 0 );
		int result = vkGetSwapchainImagesKHR( device.getLogicalDevice( ), handle, imageCount, null );

		if( result != VK_SUCCESS )
		{
			throw new IllegalStateException( "Failed to query Vulkan swapchain image count: " + result );
		}

		LongBuffer imagePointers = stack.mallocLong( imageCount.get( 0 ) );
		result = vkGetSwapchainImagesKHR( device.getLogicalDevice( ), handle, imageCount, imagePointers );

		if( result != VK_SUCCESS )
		{
			throw new IllegalStateException( "Failed to query Vulkan swapchain images: " + result );
		}

		long[] images = new long[imagePointers.capacity( )];

		for( int index = 0; index < images.length; index++ )
		{
			images[index] = imagePointers.get( index );
		}

		return images;
	}

	private long[] createImageViews( MemoryStack stack )
	{
		long[] views = new long[images.length];

		for( int index = 0; index < images.length; index++ )
		{
			VkImageViewCreateInfo createInfo = VkImageViewCreateInfo.calloc( stack )
				.sType( VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO )
				.image( images[index] )
				.viewType( VK_IMAGE_VIEW_TYPE_2D )
				.format( imageFormat );

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
				throw new IllegalStateException( "Failed to create Vulkan image view: " + result );
			}

			views[index] = pointer.get( 0 );
		}

		return views;
	}

	private record SwapchainSupportDetails( VkSurfaceCapabilitiesKHR capabilities, VkSurfaceFormatKHR.Buffer formats, IntBuffer presentModes )
	{
	}
}