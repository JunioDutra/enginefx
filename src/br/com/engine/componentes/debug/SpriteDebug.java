package br.com.engine.componentes.debug;

import br.com.engine.componentes.SimpleComponent;
import br.com.engine.componentes.drawable.Sprite;
import br.com.engine.core.ControleBase;
import br.com.engine.graphics.Color;
import br.com.engine.graphics.EngineGraphicsContext;

public class SpriteDebug extends SimpleComponent 
{
	@Override
	public void setup( )
	{
		
	}

	@Override
	public void draw( )
	{
		Sprite[] sprites = getParent( ).getComponent( Sprite[].class );
		EngineGraphicsContext graphics = ControleBase.getInstance( ).getGraphics2d( );
		
		for( Sprite sprite : sprites )
		{
			int x = (int)getParent( ).getPosition( ).getX( ) + (int)sprite.getPosition( ) .getX( );
			int y = (int)getParent( ).getPosition( ).getY( ) + (int)sprite.getPosition( ) .getY( );
			
			graphics.save( );
			graphics.setStroke( Color.RED );
			graphics.strokeRoundRect( x, y, sprite.getWidth( ), sprite.getHeight( ), 0, 0 );
			graphics.restore( );
		}
	}
	
	@Override
	public void update( long time ) 
	{
	}
}