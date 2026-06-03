package br.com.engine.input;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Collection;
import java.util.HashSet;

public class KeyBoard implements KeyListener
{
    private static KeyBoard instance;

    private Collection<KeyCode> lstCurrent = new HashSet<KeyCode>( );
    
	public static KeyBoard infInstace( )
	{
	    if( instance == null )
	    {
	        instance = new KeyBoard( );
	    }
	    
        return instance;
	}
	
	private KeyBoard( )
	{
	    
    }

    public void ifKeyPressed( KeyCode key, Runnable run )
    {
        lstCurrent.stream( ).forEach( code -> 
        {
            if( key == code )
            {
                run.run( );
            }
        } );
    }

    @Override
    public void keyTyped( KeyEvent event )
    {
    }

    @Override
    public void keyPressed( KeyEvent event )
    {
        KeyCode code = KeyCode.fromAwt( event.getKeyCode( ) );

        if( code != null )
        {
            lstCurrent.add( code );
        }
    }

    @Override
    public void keyReleased( KeyEvent event )
    {
        KeyCode code = KeyCode.fromAwt( event.getKeyCode( ) );

        if( code != null )
        {
            lstCurrent.remove( code );
        }
    }
}