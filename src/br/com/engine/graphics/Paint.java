package br.com.engine.graphics;

public class Paint
{
	private final java.awt.Color awtColor;

	protected Paint( java.awt.Color awtColor )
	{
		this.awtColor = awtColor;
	}

	public java.awt.Color toAwtColor( )
	{
		return awtColor;
	}

	public static Paint valueOf( String value )
	{
		return Color.valueOf( value );
	}

	@Override
	public String toString( )
	{
		return String.format( "#%02x%02x%02x", awtColor.getRed( ), awtColor.getGreen( ), awtColor.getBlue( ) );
	}
}