package br.com.engine.graphics;

import java.awt.image.BufferedImage;

public class Image
{
	private final BufferedImage bufferedImage;

	public Image( BufferedImage bufferedImage )
	{
		this.bufferedImage = bufferedImage;
	}

	public BufferedImage getBufferedImage( )
	{
		return bufferedImage;
	}

	public double getWidth( )
	{
		return bufferedImage.getWidth( );
	}

	public double getHeight( )
	{
		return bufferedImage.getHeight( );
	}
}