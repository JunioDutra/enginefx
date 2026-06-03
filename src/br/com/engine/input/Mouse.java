package br.com.engine.input;

import java.util.ArrayList;
import java.util.List;

import br.com.engine.interfaces.IMouseClick;

public class Mouse
{
    private static Mouse instance;

    private List<IMouseClick> onClick = new ArrayList<>( );

    private Mouse( ){ }

    public static Mouse infInstace( )
    {
        if( instance == null )
        {
            instance = new Mouse( );
        }
        return instance;
    }

    public void addListener( IMouseClick click )
    {
        onClick.add( click );
    }

    public void click( double x, double y )
    {
        MouseEvent event = new MouseEvent( x, y );
        for( IMouseClick iMouseClick : onClick )
        {
            iMouseClick.onClick( event );
        }
    }

    public void clear( )
    {
        onClick.clear( );
    }
}
