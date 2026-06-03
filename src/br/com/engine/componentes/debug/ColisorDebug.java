package br.com.engine.componentes.debug;

import br.com.engine.componentes.SimpleComponent;
import br.com.engine.componentes.physics.CustomCubeColisor;
import br.com.engine.componentes.physics.OvalCustomCubeColisor;
import br.com.engine.componentes.physics.SpriteColisor;
import br.com.engine.core.ControleBase;
import br.com.engine.graphics.Color;
import br.com.engine.graphics.EngineGraphicsContext;
import br.com.engine.interfaces.CubeColisor;

public class ColisorDebug extends SimpleComponent 
{
	@Override
	public void setup( )
	{
		
	}

	@Override
	public void draw( )
	{
		CubeColisor[] rectangles = getParent( ).getComponent( CubeColisor[].class );
		EngineGraphicsContext graphics = ControleBase.getInstance( ).getGraphics2d( );
		
		for( CubeColisor colisor : rectangles )
		{
			graphics.save( );
			graphics.setStroke( Color.GREEN );
			
			if( colisor instanceof CustomCubeColisor || colisor instanceof SpriteColisor )
			{
				graphics.strokeRoundRect( colisor.getRectangle( ).getX( ), 
													  colisor.getRectangle( ).getY( ), 
													  colisor.getRectangle( ).getWidth( ), 
													  colisor.getRectangle( ).getHeight( ), 0, 0 );
			}
			else if( colisor instanceof OvalCustomCubeColisor )
			{
				graphics.strokeOval( colisor.getRectangle( ).getX( ), 
											 colisor.getRectangle( ).getY( ), 
											 colisor.getRectangle( ).getWidth( ), 
											 colisor.getRectangle( ).getHeight( ) );
			}
			
			graphics.restore( );
		}
	}

	@Override
	public void update(long time) 
	{
	}
}