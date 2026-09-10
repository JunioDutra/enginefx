package br.com.engine.main;

import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.glfw.GLFWVulkan.glfwVulkanSupported;

import org.lwjgl.glfw.GLFWErrorCallback;

import br.com.engine.platform.lwjgl.LwjglVulkanInstance;
import br.com.engine.platform.lwjgl.LwjglVulkanDevice;
import br.com.engine.platform.lwjgl.LwjglVulkanFrameRenderer;
import br.com.engine.platform.lwjgl.LwjglVulkanSwapchain;
import br.com.engine.platform.lwjgl.LwjglVulkanWindow;

public final class LwjglVulkanSmokeApp
{
	private LwjglVulkanSmokeApp( )
	{
	}

	public static void main( String[] args )
	{
		System.setProperty( "org.lwjgl.system.memoryBackend", System.getProperty( "org.lwjgl.system.memoryBackend", "ffm" ) );
		GLFWErrorCallback.createPrint( System.err ).set( );

		if( !glfwInit( ) )
		{
			throw new IllegalStateException( "Unable to initialize GLFW" );
		}

		if( !glfwVulkanSupported( ) )
		{
			glfwTerminate( );
			throw new IllegalStateException( "Vulkan is not supported on this machine" );
		}

		try( LwjglVulkanInstance instance = new LwjglVulkanInstance( "enginefx-lwjgl-smoke" );
			LwjglVulkanWindow window = new LwjglVulkanWindow( instance, 640, 360, "enginefx lwjgl vulkan smoke" );
			LwjglVulkanDevice device = new LwjglVulkanDevice( instance, window.getSurface( ) );
			LwjglVulkanSwapchain swapchain = new LwjglVulkanSwapchain( device, window.getSurface( ), window.getWidth( ), window.getHeight( ) );
			LwjglVulkanFrameRenderer renderer = new LwjglVulkanFrameRenderer( device, swapchain ) )
		{
			var report = device.getReport( );
			window.show( );
			System.out.println( "LWJGL Vulkan smoke app started" );
			System.out.println( "Vulkan surface created: " + window.getSurface( ) );
			System.out.println( "Vulkan queues selected: graphics=" + device.getQueueFamilyIndices( ).graphicsFamily( ) + ", present=" + device.getQueueFamilyIndices( ).presentFamily( ) );
			System.out.println( "Vulkan device: " + report.deviceName( ) + ", api=" + report.apiVersion( ) + ", driver=" + report.driverVersion( ) + ", swapchain-maintenance1=" + report.supportsSwapchainMaintenance1( ) );
			System.out.println( "Vulkan swapchain created: images=" + swapchain.getImageCount( ) + ", extent=" + swapchain.getWidth( ) + "x" + swapchain.getHeight( ) + ", format=" + swapchain.getImageFormat( ) );

			for( int frame = 0; frame < 120 && !window.shouldClose( ); frame++ )
			{
				window.pollEvents( );
				renderer.drawFrame( );
				try
				{
					Thread.sleep( 16L );
				}
				catch( InterruptedException exception )
				{
					Thread.currentThread( ).interrupt( );
					break;
				}
			}
		}
		finally
		{
			glfwTerminate( );
		}
	}
}
