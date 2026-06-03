package br.com.engine.input;

import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

import br.com.engine.interfaces.IMouseClick;

public class Mouse implements MouseListener
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
	
	public void addListener( IMouseClick onClick ) {
		this.onClick.add( onClick );
	}

	@Override
	public void mouseClicked( java.awt.event.MouseEvent event )
	{
		MouseEvent convertedEvent = MouseEvent.fromAwt( event );
		onClick.forEach( listener -> listener.onClick( convertedEvent ) );
	}

	@Override
	public void mousePressed( java.awt.event.MouseEvent event )
	{
	}

	@Override
	public void mouseReleased( java.awt.event.MouseEvent event )
	{
	}

	@Override
	public void mouseEntered( java.awt.event.MouseEvent event )
	{
	}

	@Override
	public void mouseExited( java.awt.event.MouseEvent event )
	{
	}
}