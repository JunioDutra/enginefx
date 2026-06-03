package br.com.engine.componentes.drawable;

import br.com.engine.componentes.SimpleComponent;
import br.com.engine.core.ControleBase;
import br.com.engine.graphics.Color;

public class Circle extends SimpleComponent
{
	private int radius;
	private String color;
	private ControleBase base;
	
    public Circle( int radius )
    {
        this.radius = radius;
        this.color = "black";
    }
    
    public Circle( int raio, String color )
    {
    	this(raio);
    	
    	this.color = color;
    }

    @Override
	public void setup( )
    {
    	base = ControleBase.getInstance( );
	}
    
    @Override
    public void draw()
    {
        base.getGraphics2d( ).save( );
        base.getGraphics2d( ).setStroke( getColor( ) );
        base.getGraphics2d( ).strokeOval( getParent( ).getPosition( ).getX( ), getParent( ).getPosition( ).getY( ), this.radius*2, this.radius*2 );
        base.getGraphics2d( ).restore( );
    }

	@Override
	public void update( long time ) 
	{
	}
	
	public Color getColor() {
		return Color.valueOf(color.toLowerCase());
	}
}