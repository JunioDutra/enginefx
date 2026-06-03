package br.com.engine.core;

import br.com.engine.graphics.EngineGraphicsContext;

public class Screen
{
    private final double width;
    private final double height;
    private EngineGraphicsContext graphicsContext;

    public Screen( double width, double height )
    {
        this.width = width;
        this.height = height;
    }

    public EngineGraphicsContext getGraphicsContext( )
    {
        if( graphicsContext == null )
        {
            throw new IllegalStateException( "Graphics context was not initialized" );
        }
        return graphicsContext;
    }

    public void setGraphicsContext( EngineGraphicsContext graphicsContext )
    {
        this.graphicsContext = graphicsContext;
    }

    public double getWidth( )
    {
        return width;
    }

    public double getHeight( )
    {
        return height;
    }
}
