package br.com.engine.componentes.drawable;

import br.com.engine.componentes.SimpleComponent;
import br.com.engine.core.ControleBase;
import br.com.engine.graphics.Paint;

public class Cube extends SimpleComponent 
{
	private int width;
	private int height;
	private Paint color;
	
	private boolean fill;
	
	protected ControleBase base;
	
	public Cube( int width, int height )
	{
		this.width = width;
		this.height = height;
		setColor("black");
	}
	
	public Cube( int width, int height, String color )
	{
		this.width = width;
		this.height = height;
		setColor(color);
	}
	
	@Override
	public void draw( )
	{
		base.getGraphics2d( ).save( );
		
		if( isFill( ) )
		{
			base.getGraphics2d( ).setFill( color );
			base.getGraphics2d( ).fillRect( getParent( ).getPosition( ).getX( ), getParent( ).getPosition( ).getY( ), width, height );
		}
		else
		{
			base.getGraphics2d( ).setStroke( color );
			base.getGraphics2d( ).strokeRect( getParent( ).getPosition( ).getX( ), getParent( ).getPosition( ).getY( ), width, height );
		}
		
		base.getGraphics2d( ).restore( );
	}
	
	@Override
	public void setup( )
	{
		base = ControleBase.getInstance( );
	}
	
	public int getWidth( )
	{
		return this.width;
	}
	
	public int getHeight( )
	{
		return this.height;
	}

	public void setColor( String color )
	{
		this.color = Paint.valueOf( color );
	}
	
	public Paint getColor( )
    {
        return color;
    }

	@Override
	public void update( long time ) 
	{
		
	}

	public boolean isFill() {
		return fill;
	}

	public void setFill(boolean fill) {
		this.fill = fill;
	}
}