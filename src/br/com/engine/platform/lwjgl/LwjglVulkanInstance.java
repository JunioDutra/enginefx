package br.com.engine.platform.lwjgl;

import static org.lwjgl.glfw.GLFWVulkan.glfwGetRequiredInstanceExtensions;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.VK_MAKE_API_VERSION;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_APPLICATION_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;
import static org.lwjgl.vulkan.VK10.vkCreateInstance;
import static org.lwjgl.vulkan.VK10.vkDestroyInstance;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkApplicationInfo;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkInstanceCreateInfo;

public class LwjglVulkanInstance implements AutoCloseable
{
	private final VkInstance handle;
	private final boolean validation = Boolean.getBoolean( "enginefx.vulkan.validation" );
	private org.lwjgl.vulkan.VkDebugUtilsMessengerCallbackEXT debugCallback;
	private long debugMessenger;

	public LwjglVulkanInstance( String applicationName )
	{
		handle = createInstance( applicationName );
		if( validation )
		{
			try( MemoryStack stack = stackPush( ) )
			{
				java.nio.LongBuffer pointer = stack.mallocLong( 1 );
				int result = org.lwjgl.vulkan.EXTDebugUtils.vkCreateDebugUtilsMessengerEXT( handle, debugInfo( stack ), null, pointer );
				if( result != VK_SUCCESS )
				{
					vkDestroyInstance( handle, null );
					debugCallback.free( );
					throw new IllegalStateException( "Failed to create Vulkan debug messenger: " + result );
				}
				debugMessenger = pointer.get( 0 );
			}
		}
	}

	public VkInstance getHandle( )
	{
		return handle;
	}

	@Override
	public void close( )
	{
		if( debugMessenger != 0 ) org.lwjgl.vulkan.EXTDebugUtils.vkDestroyDebugUtilsMessengerEXT( handle, debugMessenger, null );
		vkDestroyInstance( handle, null );
		if( debugCallback != null ) debugCallback.free( );
	}

	private org.lwjgl.vulkan.VkDebugUtilsMessengerCreateInfoEXT debugInfo( MemoryStack stack )
	{
		if( debugCallback == null ) debugCallback = org.lwjgl.vulkan.VkDebugUtilsMessengerCallbackEXT.create(
			(severity, type, data, user) -> {
				System.err.println( "[Vulkan validation] " + org.lwjgl.vulkan.VkDebugUtilsMessengerCallbackDataEXT.create( data ).pMessageString( ) );
				return 0;
			} );
		return org.lwjgl.vulkan.VkDebugUtilsMessengerCreateInfoEXT.calloc( stack ).sType$Default( )
			.messageSeverity( org.lwjgl.vulkan.EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT | org.lwjgl.vulkan.EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT )
			.messageType( org.lwjgl.vulkan.EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT | org.lwjgl.vulkan.EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT | org.lwjgl.vulkan.EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT )
			.pfnUserCallback( debugCallback );
	}

	private VkInstance createInstance( String applicationName )
	{
		try( MemoryStack stack = stackPush( ) )
		{
			VkApplicationInfo applicationInfo = VkApplicationInfo.calloc( stack )
				.sType( VK_STRUCTURE_TYPE_APPLICATION_INFO )
				.pApplicationName( stack.UTF8( applicationName ) )
				.applicationVersion( 1 )
				.pEngineName( stack.UTF8( "enginefx" ) )
				.engineVersion( 1 )
				.apiVersion( VK_MAKE_API_VERSION( 0, 1, 0, 0 ) );

			PointerBuffer requiredExtensions = glfwGetRequiredInstanceExtensions( );

			if( requiredExtensions == null )
			{
				throw new IllegalStateException( "GLFW did not provide Vulkan instance extensions" );
			}

			VkInstanceCreateInfo createInfo = VkInstanceCreateInfo.calloc( stack )
				.sType( VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO )
				.pApplicationInfo( applicationInfo )
				.ppEnabledExtensionNames( requiredExtensions );

			PointerBuffer pointer = stack.mallocPointer( 1 );
			if( validation )
			{
				// Explicit opt-in: fail visibly if the requested SDK layer is missing.
				PointerBuffer extensions = stack.mallocPointer( requiredExtensions.remaining( ) + 1 );
				extensions.put( requiredExtensions.duplicate( ) ).put( stack.UTF8( org.lwjgl.vulkan.EXTDebugUtils.VK_EXT_DEBUG_UTILS_EXTENSION_NAME ) ).flip( );
				createInfo.ppEnabledExtensionNames( extensions )
					.ppEnabledLayerNames( stack.pointers( stack.UTF8( "VK_LAYER_KHRONOS_validation" ) ) )
					.pNext( debugInfo( stack ) );
			}
			int result = vkCreateInstance( createInfo, null, pointer );

			if( result != VK_SUCCESS )
			{
				if( debugCallback != null ) debugCallback.free( );
				throw new IllegalStateException( "Failed to create Vulkan instance: " + result );
			}

			return new VkInstance( pointer.get( 0 ), createInfo );
		}
	}
}
