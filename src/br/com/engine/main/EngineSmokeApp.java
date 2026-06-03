package br.com.engine.main;

import br.com.engine.core.Screen;
import br.com.engine.graphics.Color;

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public class EngineSmokeApp
{
	public static void main( String[] args )
	{
		SwingUtilities.invokeLater( () -> {
			Screen screen = new Screen( 320, 180 );
			JFrame frame = new JFrame( "enginefx smoke" );
			frame.setLayout( new BorderLayout( ) );
			frame.add( screen.getCanvas( ), BorderLayout.CENTER );
			frame.pack( );
			frame.setLocationRelativeTo( null );
			frame.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
			frame.setVisible( true );
			screen.initialize( );
			screen.getGraphicsContext( ).setFill( Color.BLACK );
			screen.getGraphicsContext( ).fillRect( 0, 0, screen.getWidth( ), screen.getHeight( ) );
			screen.present( );
			System.out.println( "enginefx smoke app started" );

			Timer timer = new Timer( 1000, event -> frame.dispose( ) );
			timer.setRepeats( false );
			timer.start( );
		} );
	}
}