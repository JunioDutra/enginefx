package br.com.engine.componentes.physics;

import br.com.engine.componentes.SimpleComponent;
import br.com.engine.componentes.drawable.Cube;
import br.com.engine.geometry.Rectangle;
import br.com.engine.interfaces.CubeColisor;
import br.com.engine.interfaces.OnColisionAction;

public class ShapeCubeColisor extends SimpleComponent implements CubeColisor
{
	private String tag;
	
	private Rectangle rectangle;
	
	private OnColisionAction onColision;

	private boolean byPass;
	
	@Override
	public void setup( )
	{
		if( getParent( ).getComponent( Cube.class ) == null )
		{
			throw new RuntimeException( "Erro ao criar colisor para Cube, o GameObject n�o possue cube's!" );
		}
		
		Cube cube = getParent( ).getComponent( Cube.class );
		
		rectangle = new Rectangle( getParent( ).getPosition( ).getX( ), 
								   getParent( ).getPosition( ).getY( ), 
								   cube.getWidth( ), 
								   cube.getHeight( ) );
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
		rectangle.setX( getParent( ).getPosition( ).getX( ) );
		rectangle.setY( getParent( ).getPosition( ).getY( ) ); 
		
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