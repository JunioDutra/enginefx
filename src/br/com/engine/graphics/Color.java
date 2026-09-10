package br.com.engine.graphics;

import java.util.Locale;
import java.util.Map;

public final class Color extends Paint
{
    public static final Color BLACK = new Color( 0, 0, 0 );
    public static final Color WHITE = new Color( 255, 255, 255 );
    public static final Color RED = new Color( 255, 0, 0 );
    public static final Color GREEN = new Color( 0, 255, 0 );
    public static final Color BLUE = new Color( 0, 0, 255 );
    public static final Color YELLOW = new Color( 255, 255, 0 );
    public static final Color AQUA = new Color( 0, 255, 255 );

    private static final Map<String, Color> COLORS = Map.of(
        "black", BLACK, "white", WHITE, "red", RED, "green", GREEN,
        "blue", BLUE, "yellow", YELLOW, "aqua", AQUA, "cyan", AQUA );

    public Color( int red, int green, int blue ) { this( red, green, blue, 255 ); }
    public Color( int red, int green, int blue, int alpha ) { super( red, green, blue, alpha ); }
    public static Color rgba( int red, int green, int blue, int alpha ) { return new Color( red, green, blue, alpha ); }

    public static Color valueOf( String value )
    {
        if( value == null ) throw new IllegalArgumentException( "Color cannot be null" );
        String normalized = value.trim( ).toLowerCase( Locale.ROOT );
        Color named = COLORS.get( normalized );
        if( named != null ) return named;
        if( normalized.startsWith( "#" ) && (normalized.length( ) == 7 || normalized.length( ) == 9) )
        {
            try
            {
                int red = Integer.parseInt( normalized.substring( 1, 3 ), 16 );
                int green = Integer.parseInt( normalized.substring( 3, 5 ), 16 );
                int blue = Integer.parseInt( normalized.substring( 5, 7 ), 16 );
                int alpha = normalized.length( ) == 9 ? Integer.parseInt( normalized.substring( 7, 9 ), 16 ) : 255;
                return new Color( red, green, blue, alpha );
            }
            catch( NumberFormatException exception ) { throw new IllegalArgumentException( "Unsupported color: " + value, exception ); }
        }
        throw new IllegalArgumentException( "Unsupported color: " + value );
    }
}
