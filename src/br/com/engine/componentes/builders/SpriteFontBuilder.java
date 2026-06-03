package br.com.engine.componentes.builders;

import br.com.engine.componentes.drawable.SpriteFont;
import br.com.engine.core.ControleBase;
import br.com.engine.core.GameObject;
import br.com.engine.graphics.Color;

public class SpriteFontBuilder 
{
	private GameObject go;
	private SpriteFont sf;
	
	private boolean centerX;
	private boolean centerY;
	
	public SpriteFontBuilder( )
	{
		go = new GameObject( );
		sf = new SpriteFont( );
		
		centerX = false;
		centerY = false;
	}
	
	public SpriteFontBuilder setSize( int size )
	{
		sf.setFontSize( size );
		
		return this;
	}
	
	public SpriteFontBuilder setFont( String font )
	{
		sf.setFont( font );
		
		return this;
	}
	
	public SpriteFontBuilder setText( String text )
	{
		sf.setText( text );
		
		return this;
	}
	
	public SpriteFontBuilder setColor( Color color )
	{
		sf.setColor( color );
		
		return this;
	}
	
	public SpriteFontBuilder centerX( )
	{
		centerX = true;

		return this;
	}
	
	public SpriteFontBuilder centerY( )
	{
		centerY = true;

		return this;
	}
	
	public GameObject build( )
	{
		go.addComponente( sf );
		
		if( centerX )
		{
			go.getPosition( ).setPosition( ( (int)ControleBase.getInstance().getScreen( ).getWidth( ) / 2 ) - ( sf.getWidth( ) / 2 ), 
										   go.getPosition( ).getY( ) );
		}
		
		if( centerY )
		{
			go.getPosition( ).setPosition( go.getPosition( ).getX( ), 
										   ( (int)ControleBase.getInstance().getScreen( ).getHeight( ) / 2 ) - ( sf.getHeight( ) / 2 ) );
		}
		
		return go;
	}

	public SpriteFontBuilder setTag(String tag) {
		go.setTag(tag);
		return this;
	}
}