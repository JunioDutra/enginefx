package br.com.engine.componentes.physics;

import br.com.engine.componentes.SimpleComponent;
import br.com.engine.core.GameObject;
import br.com.engine.core.Vector2;

public class Gravity extends SimpleComponent
{
	private Vector2 gravity;
	private Vector2 gravityVolocity;

	public Gravity( Vector2 gravity )
	{
		this.gravity = gravity;
		gravityVolocity = new Vector2(0, 6f);
	}

	@Override
	public void setup( )
	{
	}

	@Override
	public void draw( )
	{
	}

	@Override
	public void fixedUpdate( float deltaSeconds )
	{
		GameObject parent = getParent( );

		parent.getPosition( ).plus( gravityVolocity.x * deltaSeconds, gravityVolocity.y * deltaSeconds );

//		if( parent.getPosition( ).y < gravity.y )
//		{
//			if( gravity.y - parent.getPosition( ).y < gravityVolocity.y + time )
//			{
//				parent.getPosition( ).plus( 0, gravity.y - parent.getPosition( ).y );
//			}
//			else
//			{
//				parent.getPosition( ).plus( gravityVolocity );
//			}
//		}
//		else if( parent.getPosition( ).y > gravity.y )
//		{
//			if( parent.getPosition( ).y - gravity.y < gravityVolocity.y )
//			{
//				parent.getPosition( ).less( 0, parent.getPosition( ).y - gravity.y );
//			}
//			else
//			{
//				parent.getPosition( ).less( gravityVolocity );
//			}
//		}
	}

	@Override public void update( long time ) { }
}
