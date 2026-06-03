package br.com.engine.componentes.physics;

import br.com.engine.componentes.SimpleComponent;
import br.com.engine.componentes.drawable.Sprite;
import br.com.engine.geometry.Rectangle;
import br.com.engine.interfaces.CubeColisor;
import br.com.engine.interfaces.OnColisionAction;

public class SpriteColisor extends SimpleComponent implements CubeColisor
{
	private String tag;
	
	private Rectangle rectangle;
	
	private OnColisionAction onColision;
	
	@Override
	public void setup( )
	{
		if( getParent( ).getComponent( Sprite.class ) == null )
		{
			throw new RuntimeException( "Erro ao criar colisor para sprite, o GameObject n�o possue sprites!" );
		}
		
		Sprite sprite = getParent( ).getComponent( Sprite.class );
		
		rectangle = new Rectangle( getParent( ).getPosition( ).getX( ) + sprite.getPosition( ).getX( ), 
								   getParent( ).getPosition( ).getY( ) + sprite.getPosition( ).getY( ), 
								   sprite.getWidth( ), 
								   sprite.getHeight( ) );
	}
	
	@Override
	public void onColision( CubeColisor colided )
	{
		if( onColision != null )
		{
			onColision.execute( colided );
		}
	}
	
	@Override
	public void setOnColisionAction( OnColisionAction onColision )
	{
		this.onColision = onColision;
	}
	
	@Override
	public void draw( )
	{
		
	}
	
	@Override
	public Rectangle getRectangle( )
	{
		Sprite sprite = getParent( ).getComponent( Sprite.class );
		
		rectangle.setX( getParent( ).getPosition( ).getX( ) + sprite.getPosition( ).getX( ) );
		rectangle.setY( getParent( ).getPosition( ).getY( ) + sprite.getPosition( ).getY( ) ); 
		
		return rectangle;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}
	
	@Override
	public String getTag() {
		return tag;
	}
	
	@Override
	public void update( long time ) 
	{
		
	}
}