package br.com.engine.platform.lwjgl;

import static org.lwjgl.glfw.GLFW.GLFW_CLIENT_API;
import static org.lwjgl.glfw.GLFW.GLFW_FALSE;
import static org.lwjgl.glfw.GLFW.GLFW_NO_API;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;
import static org.lwjgl.glfw.GLFW.GLFW_RESIZABLE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDefaultWindowHints;
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSetCursorPosCallback;
import static org.lwjgl.glfw.GLFW.glfwSetKeyCallback;
import static org.lwjgl.glfw.GLFW.glfwSetMouseButtonCallback;
import static org.lwjgl.glfw.GLFW.glfwShowWindow;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import static org.lwjgl.glfw.GLFWVulkan.glfwCreateWindowSurface;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.vulkan.KHRSurface.vkDestroySurfaceKHR;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;

import java.nio.LongBuffer;

import org.lwjgl.system.MemoryStack;

import br.com.engine.input.KeyBoard;
import br.com.engine.input.KeyCode;
import br.com.engine.input.Mouse;

public class LwjglVulkanWindow implements AutoCloseable
{
	private final LwjglVulkanInstance instance;
	private final long handle;
	private final long surface;
	private final int width;
	private final int height;
	private double mouseX;
	private double mouseY;

	public LwjglVulkanWindow( LwjglVulkanInstance instance, int width, int height, String title )
	{
		this.instance = instance;
		this.width = width;
		this.height = height;
		handle = createWindow( width, height, title );
		surface = createSurface( );
		registerInputCallbacks( );
	}

	public void show( )
	{
		glfwShowWindow( handle );
	}

	public void pollEvents( )
	{
		glfwPollEvents( );
	}

	public boolean shouldClose( )
	{
		return glfwWindowShouldClose( handle );
	}

	public long getHandle( )
	{
		return handle;
	}

	public long getSurface( )
	{
		return surface;
	}

	public int getWidth( )
	{
		return width;
	}

	public int getHeight( )
	{
		return height;
	}

	@Override
	public void close( )
	{
		vkDestroySurfaceKHR( instance.getHandle( ), surface, null );
		org.lwjgl.glfw.Callbacks.glfwFreeCallbacks( handle );
		glfwDestroyWindow( handle );
	}

	private long createWindow( int width, int height, String title )
	{
		glfwDefaultWindowHints( );
		glfwWindowHint( GLFW_CLIENT_API, GLFW_NO_API );
		glfwWindowHint( GLFW_RESIZABLE, GLFW_FALSE );

		long windowHandle = glfwCreateWindow( width, height, title, NULL, NULL );

		if( windowHandle == NULL )
		{
			throw new IllegalStateException( "Unable to create GLFW window" );
		}

		return windowHandle;
	}

	private long createSurface( )
	{
		try( MemoryStack stack = stackPush( ) )
		{
			LongBuffer surfacePointer = stack.mallocLong( 1 );
			int result = glfwCreateWindowSurface( instance.getHandle( ), handle, null, surfacePointer );

			if( result != VK_SUCCESS )
			{
				throw new IllegalStateException( "Failed to create GLFW Vulkan surface: " + result );
			}

			return surfacePointer.get( 0 );
		}
	}

	private void registerInputCallbacks( )
	{
		glfwSetKeyCallback( handle, (window, key, scancode, action, mods) ->
		{
			KeyCode code = KeyCode.fromGlfw( key );

			if( action == GLFW_PRESS )
			{
				KeyBoard.infInstace( ).press( code );
			}
			else if( action == GLFW_RELEASE )
			{
				KeyBoard.infInstace( ).release( code );
			}
		} );

		glfwSetCursorPosCallback( handle, (window, x, y) ->
		{
			mouseX = x;
			mouseY = y;
		} );

		glfwSetMouseButtonCallback( handle, (window, button, action, mods) ->
		{
			if( action == GLFW_PRESS )
			{
				try( MemoryStack stack = stackPush( ) )
				{
					var sizeX = stack.ints( 0 );
					var sizeY = stack.ints( 0 );
					org.lwjgl.glfw.GLFW.glfwGetWindowSize( handle, sizeX, sizeY );
					if( sizeX.get( 0 ) > 0 && sizeY.get( 0 ) > 0 )
						Mouse.infInstace( ).click( mouseX * width / sizeX.get( 0 ), mouseY * height / sizeY.get( 0 ) );
				}
			}
		} );
	}
}
