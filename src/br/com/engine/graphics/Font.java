package br.com.engine.graphics;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class Font
{
	private final java.awt.Font awtFont;
	private java.io.File sourceFile;

	public Font( String name, int size )
	{
		this( new java.awt.Font( name, java.awt.Font.PLAIN, size ) );
	}

	public Font( java.awt.Font awtFont )
	{
		this.awtFont = awtFont;
	}

	public Font( java.awt.Font awtFont, java.io.File sourceFile )
	{
		this.awtFont = awtFont;
		this.sourceFile = sourceFile;
	}

	public java.io.File getSourceFile( )
	{
		return sourceFile;
	}

	public java.awt.Font toAwtFont( )
	{
		return awtFont;
	}

	public int getStringWidth( String text )
	{
		Graphics2D graphics = createMeasureGraphics( );
		try
		{
			graphics.setFont( awtFont );
			return graphics.getFontMetrics( awtFont ).stringWidth( text );
		}
		finally
		{
			graphics.dispose( );
		}
	}

	public int getHeight( )
	{
		Graphics2D graphics = createMeasureGraphics( );
		try
		{
			graphics.setFont( awtFont );
			return graphics.getFontMetrics( awtFont ).getHeight( );
		}
		finally
		{
			graphics.dispose( );
		}
	}

	private Graphics2D createMeasureGraphics( )
	{
		BufferedImage image = new BufferedImage( 1, 1, BufferedImage.TYPE_INT_ARGB );
		return image.createGraphics( );
	}
}