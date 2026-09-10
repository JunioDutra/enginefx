package br.com.engine.graphics;

import java.util.Arrays;

/** Immutable RGBA8 image. Pixel channels are ordered red, green, blue, alpha. */
public final class Image
{
    private final int width;
    private final int height;
    private final byte[] rgba;

    public Image( int width, int height, byte[] rgba )
    {
        if( width <= 0 || height <= 0 ) throw new IllegalArgumentException( "Image size must be positive" );
        if( rgba == null || rgba.length != Math.multiplyExact( Math.multiplyExact( width, height ), 4 ) )
            throw new IllegalArgumentException( "RGBA data must contain exactly width * height * 4 bytes" );
        this.width = width;
        this.height = height;
        this.rgba = rgba.clone( );
    }

    public int getWidth( ) { return width; }
    public int getHeight( ) { return height; }
    public byte[] copyRgba( ) { return rgba.clone( ); }

    public int rgbaAt( int x, int y )
    {
        if( x < 0 || y < 0 || x >= width || y >= height ) throw new IndexOutOfBoundsException( "Pixel outside image" );
        int offset = ( y * width + x ) * 4;
        return ((rgba[offset] & 0xff) << 24) | ((rgba[offset + 1] & 0xff) << 16) | ((rgba[offset + 2] & 0xff) << 8) | (rgba[offset + 3] & 0xff);
    }

    @Override public boolean equals( Object other )
    {
        return other instanceof Image image && width == image.width && height == image.height && Arrays.equals( rgba, image.rgba );
    }

    @Override public int hashCode( ) { return 31 * (31 * width + height) + Arrays.hashCode( rgba ); }
}
