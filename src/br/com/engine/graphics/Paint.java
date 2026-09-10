package br.com.engine.graphics;

/** Immutable paint expressed as sRGB RGB channels and linear alpha. */
public class Paint
{
    private final float red;
    private final float green;
    private final float blue;
    private final float alpha;

    protected Paint( int red, int green, int blue, int alpha )
    {
        this.red = channel( red );
        this.green = channel( green );
        this.blue = channel( blue );
        this.alpha = channel( alpha );
    }

    private static float channel( int value )
    {
        if( value < 0 || value > 255 ) throw new IllegalArgumentException( "RGBA channel must be between 0 and 255" );
        return value / 255f;
    }

    public float red( ) { return red; }
    public float green( ) { return green; }
    public float blue( ) { return blue; }
    public float alpha( ) { return alpha; }
    public int red8( ) { return Math.round( red * 255f ); }
    public int green8( ) { return Math.round( green * 255f ); }
    public int blue8( ) { return Math.round( blue * 255f ); }
    public int alpha8( ) { return Math.round( alpha * 255f ); }
    public static Paint valueOf( String value ) { return Color.valueOf( value ); }

    @Override public String toString( )
    {
        return alpha8( ) == 255 ? String.format( "#%02x%02x%02x", red8( ), green8( ), blue8( ) )
            : String.format( "#%02x%02x%02x%02x", red8( ), green8( ), blue8( ), alpha8( ) );
    }
}
