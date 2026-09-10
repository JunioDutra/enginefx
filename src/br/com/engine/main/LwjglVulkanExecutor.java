package br.com.engine.main;

import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.glfw.GLFWVulkan.glfwVulkanSupported;

import org.lwjgl.glfw.GLFWErrorCallback;

import br.com.engine.core.ControleBase;
import br.com.engine.platform.lwjgl.LwjglVulkanDevice;
import br.com.engine.platform.lwjgl.LwjglVulkanFrameRenderer;
import br.com.engine.platform.lwjgl.LwjglVulkanInstance;
import br.com.engine.platform.lwjgl.LwjglVulkanSwapchain;
import br.com.engine.platform.lwjgl.LwjglVulkanWindow;
import br.com.engine.platform.lwjgl.VulkanGraphicsContext;

public class LwjglVulkanExecutor
{
	public void start( )
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

		ControleBase controle = ControleBase.getInstance( );
		int width = (int)controle.getScreen( ).getWidth( );
		int height = (int)controle.getScreen( ).getHeight( );
		VulkanGraphicsContext graphicsContext = new VulkanGraphicsContext( );
		graphicsContext.setCanvasSize( width, height );
		controle.getScreen( ).setGraphicsContext( graphicsContext );

		try( LwjglVulkanInstance instance = new LwjglVulkanInstance( "enginefx" );
			LwjglVulkanWindow window = new LwjglVulkanWindow( instance, width, height, "Enginefx Vulkan" );
			LwjglVulkanDevice device = new LwjglVulkanDevice( instance, window.getSurface( ) );
			br.com.engine.platform.lwjgl.LwjglVulkanRenderSession renderer = new br.com.engine.platform.lwjgl.LwjglVulkanRenderSession( device, window ) )
		{
			window.show( );
			controle.setup( );

			while( !window.shouldClose( ) )
			{
				window.pollEvents( );
				controle.processLogics( );
				graphicsContext.beginFrame( );
				controle.renderGraphics( );
				renderer.drawFrame( graphicsContext );
			}
		}
		finally
		{
			controle.stop( );
			glfwTerminate( );
		}
	}
}
