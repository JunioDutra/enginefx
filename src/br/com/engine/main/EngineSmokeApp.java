package br.com.engine.main;

import br.com.engine.core.Screen;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.util.Duration;

public class EngineSmokeApp extends Application
{
	@Override
	public void start( Stage primaryStage )
	{
		Screen screen = new Screen( 320, 180 );
		Group root = new Group( screen.getCanvas( ) );

		primaryStage.setTitle( "enginefx smoke" );
		primaryStage.setScene( new Scene( root, screen.getWidth( ), screen.getHeight( ) ) );
		primaryStage.show( );

		System.out.println( "enginefx smoke app started" );

		PauseTransition pause = new PauseTransition( Duration.seconds( 1 ) );
		pause.setOnFinished( event -> {
			primaryStage.close( );
			Platform.exit( );
		} );
		pause.play( );
	}

	public static void main( String[] args )
	{
		launch( args );
	}
}