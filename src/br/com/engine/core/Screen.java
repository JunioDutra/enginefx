package br.com.engine.core;

import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;

import br.com.engine.graphics.EngineGraphicsContext;
import br.com.engine.graphics.Java2DGraphicsContext;


public class Screen
{
	private final Canvas canvas;
	private final BufferedImage frameBuffer;
	private final EngineGraphicsContext graphicsContext;
	private BufferStrategy bufferStrategy;
	
	public Screen( double width, double height )
	{
		canvas = new Canvas( );
		canvas.setPreferredSize( new Dimension( (int)width, (int)height ) );
		canvas.setSize( (int)width, (int)height );
		frameBuffer = new BufferedImage( (int)width, (int)height, BufferedImage.TYPE_INT_ARGB );
		Graphics2D graphics = frameBuffer.createGraphics( );
		graphicsContext = new Java2DGraphicsContext( graphics );
	}
	
	public Canvas getCanvas( )
	{
		return canvas;
	}
	
	public EngineGraphicsContext getGraphicsContext( )
	{
		return graphicsContext;
	}

	public double getWidth( )
	{
		return frameBuffer.getWidth( );
	}

	public double getHeight( )
	{
		return frameBuffer.getHeight( );
	}

	public void initialize( )
	{
		if( bufferStrategy == null && canvas.isDisplayable( ) && canvas.isValid( ) )
		{
			try
			{
				canvas.createBufferStrategy( 2 );
				bufferStrategy = canvas.getBufferStrategy( );
			}
			catch( IllegalStateException exception )
			{
				bufferStrategy = null;
			}
		}
	}

	public void present( )
	{
		if( !canvas.isDisplayable( ) || !canvas.isValid( ) )
		{
			bufferStrategy = null;
			return;
		}

		if( bufferStrategy == null )
		{
			initialize( );
		}

		if( bufferStrategy == null )
		{
			return;
		}

		try
		{
			Graphics graphics = bufferStrategy.getDrawGraphics( );
			try
			{
				graphics.drawImage( frameBuffer, 0, 0, null );
			}
			finally
			{
				graphics.dispose( );
			}

			bufferStrategy.show( );
			Toolkit.getDefaultToolkit( ).sync( );
		}
		catch( IllegalStateException exception )
		{
			bufferStrategy = null;
		}
	}
}