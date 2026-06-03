package br.com.engine.main;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import br.com.engine.core.ControleBase;
import br.com.engine.input.KeyBoard;

public class Executor
{
	public static void loadGame( String[] args ) 
	{
        SwingUtilities.invokeLater( () -> new Executor( ).start( ) );
	}

    public void start( )
    {
        JFrame frame = new JFrame( "Enginefx" );
        frame.setLayout( new BorderLayout( ) );
        frame.add( ControleBase.getInstance( ).getScreen( ).getCanvas( ), BorderLayout.CENTER );
        frame.pack( );
        frame.setLocationRelativeTo( null );
        frame.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
        frame.addWindowListener( new WindowAdapter( )
        {
            @Override
            public void windowClosed( WindowEvent event )
            {
                ControleBase.getInstance( ).stop( );
            }
        } );
        frame.setVisible( true );

        ControleBase.getInstance( ).getScreen( ).initialize( );
        ControleBase.getInstance( ).getScreen( ).getCanvas( ).addKeyListener( KeyBoard.infInstace( ) );
        ControleBase.getInstance( ).getScreen( ).getCanvas( ).addMouseListener( br.com.engine.input.Mouse.infInstace( ) );
        ControleBase.getInstance( ).getScreen( ).getCanvas( ).setFocusable( true );
        ControleBase.getInstance( ).getScreen( ).getCanvas( ).requestFocusInWindow( );

        ControleBase.getInstance( ).startMainLoop( );
    }
}