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
	static final int FIRST_CHAR = 32;
	static final int CHAR_COUNT = 224;
	private record FontKey( String source, float size ) { }
	private final Map<FontKey, VulkanFont> fonts = new java.util.HashMap<>( );

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
		String source = font.getSourceFile( ) == null ? "Arial" : font.getSourceFile( ).getAbsolutePath( );
		return fonts.computeIfAbsent( new FontKey( source, font.toAwtFont( ).getSize2D( ) ), ignored -> createFont( font ) );
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

			float pixelHeight = font.toAwtFont( ).getSize2D( );

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

			int atlasWidth = 1024;
			int atlasHeight = 1024;
			ByteBuffer bitmapBuffer = BufferUtils.createByteBuffer( atlasWidth * atlasHeight );
			STBTTBakedChar.Buffer charData = STBTTBakedChar.malloc( CHAR_COUNT );

			int result = STBTruetype.stbtt_BakeFontBitmap( ttfBuffer, pixelHeight, bitmapBuffer, atlasWidth, atlasHeight, FIRST_CHAR, charData );

			if( result <= 0 )
			{
				charData.free( );
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
