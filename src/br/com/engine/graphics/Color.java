package br.com.engine.graphics;

import java.util.Locale;
import java.util.Map;

public class Color extends Paint
{
	public static final Color BLACK = new Color( java.awt.Color.BLACK );
	public static final Color WHITE = new Color( java.awt.Color.WHITE );
	public static final Color RED = new Color( java.awt.Color.RED );
	public static final Color GREEN = new Color( java.awt.Color.GREEN );
	public static final Color BLUE = new Color( java.awt.Color.BLUE );
	public static final Color YELLOW = new Color( java.awt.Color.YELLOW );
	public static final Color AQUA = new Color( java.awt.Color.CYAN );

	private static final Map<String, Color> COLORS = Map.of(
		"black", BLACK,
		"white", WHITE,
		"red", RED,
		"green", GREEN,
		"blue", BLUE,
		"yellow", YELLOW,
		"aqua", AQUA,
		"cyan", AQUA
	);

	private Color( java.awt.Color awtColor )
	{
		super( awtColor );
	}

	public static Color valueOf( String value )
	{
		String normalized = value.trim( ).toLowerCase( Locale.ROOT );

		if( COLORS.containsKey( normalized ) )
		{
			return COLORS.get( normalized );
		}

		if( normalized.startsWith( "#" ) )
		{
			return new Color( java.awt.Color.decode( normalized ) );
		}

		throw new IllegalArgumentException( "Unsupported color: " + value );
	}
}