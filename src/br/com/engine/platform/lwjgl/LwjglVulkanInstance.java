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

	public LwjglVulkanInstance( String applicationName )
	{
		handle = createInstance( applicationName );
	}

	public VkInstance getHandle( )
	{
		return handle;
	}

	@Override
	public void close( )
	{
		vkDestroyInstance( handle, null );
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
			int result = vkCreateInstance( createInfo, null, pointer );

			if( result != VK_SUCCESS )
			{
				throw new IllegalStateException( "Failed to create Vulkan instance: " + result );
			}

			return new VkInstance( pointer.get( 0 ), createInfo );
		}
	}
}