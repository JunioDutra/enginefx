package br.com.engine.platform.lwjgl;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.IdentityHashMap;
import java.util.Map;

import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBTTBakedChar;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTruetype;

import br.com.engine.graphics.Font;
import br.com.engine.graphics.Image;

public class LwjglVulkanFontCache implements AutoCloseable
{
	private final Map<Font, VulkanFont> fonts = new IdentityHashMap<>( );

	public static class VulkanFont
	{
		public final Image textureImage;
		public final STBTTBakedChar.Buffer charData;
		public final int atlasWidth;
		public final int atlasHeight;
		public final float ascent;
		public final float descent;

		public VulkanFont( Image textureImage, STBTTBakedChar.Buffer charData, int atlasWidth, int atlasHeight, float ascent, float descent )
		{
			this.textureImage = textureImage;
			this.charData = charData;
			this.atlasWidth = atlasWidth;
			this.atlasHeight = atlasHeight;
			this.ascent = ascent;
			this.descent = descent;
		}
	}

	public VulkanFont get( Font font )
	{
		return fonts.computeIfAbsent( font, this::createFont );
	}

	private VulkanFont createFont( Font font )
	{
		File file = font.getSourceFile( );
		if( file == null || !file.exists() )
		{
			file = new File( "C:\\Windows\\Fonts\\arial.ttf" );
		}

		if( !file.exists( ) )
		{
			throw new RuntimeException( "Font file not found!" );
		}

		try
		{
			byte[] bytes = Files.readAllBytes( file.toPath( ) );
			ByteBuffer ttfBuffer = BufferUtils.createByteBuffer( bytes.length );
			ttfBuffer.put( bytes );
			ttfBuffer.flip( );

			float pixelHeight = font.toAwtFont().getSize() * 1.5f;

			STBTTFontinfo fontInfo = STBTTFontinfo.create( );
			if( !STBTruetype.stbtt_InitFont( fontInfo, ttfBuffer ) )
			{
				throw new RuntimeException( "Failed to init font" );
			}
			float scale = STBTruetype.stbtt_ScaleForPixelHeight( fontInfo, pixelHeight );
			int[] ascentI = new int[1];
			int[] descentI = new int[1];
			int[] lineGapI = new int[1];
			STBTruetype.stbtt_GetFontVMetrics( fontInfo, ascentI, descentI, lineGapI );
			float ascent = ascentI[0] * scale;
			float descent = descentI[0] * scale;

			int atlasWidth = 512;
			int atlasHeight = 512;
			ByteBuffer bitmapBuffer = BufferUtils.createByteBuffer( atlasWidth * atlasHeight );
			STBTTBakedChar.Buffer charData = STBTTBakedChar.malloc( 96 );

			int result = STBTruetype.stbtt_BakeFontBitmap( ttfBuffer, pixelHeight, bitmapBuffer, atlasWidth, atlasHeight, 32, charData );

			if( result <= 0 )
			{
				throw new RuntimeException( "Failed to bake font bitmap" );
			}

			BufferedImage bufferedImage = new BufferedImage( atlasWidth, atlasHeight, BufferedImage.TYPE_INT_ARGB );
			for( int y = 0; y < atlasHeight; y++ )
			{
				for( int x = 0; x < atlasWidth; x++ )
				{
					byte alpha = bitmapBuffer.get( y * atlasWidth + x );
					int argb = ((alpha & 0xFF) << 24) | 0x00FFFFFF;
					bufferedImage.setRGB( x, y, argb );
				}
			}

			return new VulkanFont( new Image( bufferedImage ), charData, atlasWidth, atlasHeight, ascent, descent );
		}
		catch( Exception e )
		{
			throw new RuntimeException( e );
		}
	}

	@Override
	public void close( )
	{
		for( VulkanFont font : fonts.values( ) )
		{
			if( font.charData != null )
			{
				font.charData.free( );
			}
		}
		fonts.clear( );
	}
}