package br.com.engine.componentes.physics;

import br.com.engine.componentes.SimpleComponent;
import br.com.engine.core.Vector2;
import br.com.engine.geometry.Rectangle;
import br.com.engine.interfaces.CubeColisor;
import br.com.engine.interfaces.OnColisionAction;

public class OvalCustomCubeColisor extends SimpleComponent implements CubeColisor
{
	private String tag;
	
	private Rectangle rectangle;
	
	private OnColisionAction onColisionAction;
	
	private Vector2 position;
	private int radius;
	
	public OvalCustomCubeColisor( Vector2 position, int radius )
	{
		this.position = position;
		this.radius = radius;
	}
	
	@Override
	public void setup( )
	{
		rectangle = new Rectangle( 0, 0, radius * 2, radius * 2 );
	}
	
	@Override
	public void onColision( CubeColisor colided )
	{
		if( onColisionAction != null )
		{
			onColisionAction.execute( colided );
		}
	}
	
	@Override
	public void setOnColisionAction( OnColisionAction onColisionAction ) 
	{
		this.onColisionAction = onColisionAction;
	}
	
	@Override
	public void draw( )
	{
		
	}
	
	@Override
	public Rectangle getRectangle( )
	{
		rectangle.setX( getParent( ).getPosition( ).getX( ) + position.getX( ) - radius );
		rectangle.setY( getParent( ).getPosition( ).getY( ) + position.getY( ) - radius );
		return rectangle;
	}
	
	public void setTag(String tag) {
		this.tag = tag;
	}
	
	public String getTag() {
		return tag;
	}

	@Override
	public void update( long time ) 
	{
	}
}