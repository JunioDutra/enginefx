package br.com.engine.input;

import java.util.Collection;
import java.util.HashSet;

public class KeyBoard
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

        private KeyBoard( ) { }

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

    public void press( KeyCode code )
    {
        if( code != null )
        {
            lstCurrent.add( code );
        }
    }

    public void release( KeyCode code )
    {
        if( code != null )
        {
            lstCurrent.remove( code );
        }
    }
}
