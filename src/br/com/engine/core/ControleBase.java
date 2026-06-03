package br.com.engine.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import br.com.engine.core.annotation.Bootable;
import br.com.engine.graphics.Color;
import br.com.engine.graphics.EngineGraphicsContext;
import br.com.engine.graphics.Paint;
import br.com.engine.input.KeyBoard;
import br.com.engine.input.Mouse;
import br.com.engine.interfaces.LoopSteps;
import br.com.engine.resources.Configurations;
import br.com.engine.resources.ResourceManager;
import br.com.engine.scenes.Loading;

public class ControleBase implements LoopSteps
{
	private static ControleBase controleBase;
	
	private MainLoopFx loop = new MainLoopFx( this );
	
	private long  previous = System.currentTimeMillis( );
	private final Screen  screen;
	private Scene gameLogic;
	private boolean running = true;
	
	private List<Scene> scenes;
	
	private boolean bMudaScene = false;
	private int     nNextScene = -1;
	private int     nLastScene = -1;

	private EngineGraphicsContext graphics;

	/*
	 * Para quando o modo de debug estiver ativado 
	 */
	private static int framesRender = 0;
	private long       timeOnStart  = System.currentTimeMillis( );
	
	private final Configurations configurations;
	
	private ControleBase( )
	{
		configurations = ResourceManager.loadResource( null, ResourceManager.CONFIGURACOES, Configurations.class );
		
		screen = new Screen( configurations.getSizeW(), configurations.getSizeH() );
		getScreen().getCanvas( ).addKeyListener( KeyBoard.infInstace( ) );
		getScreen().getCanvas( ).addMouseListener( Mouse.infInstace() );
		
		this.scenes = new ArrayList<Scene>( );
	}
	
	public static ControleBase getInstance( )
	{
		if( controleBase == null )
		{
			controleBase = new ControleBase( );
		}
		
		return controleBase;
	}

	@Override
	public void setup( ) 
	{
		renderLoadingScreen();
		
		configurations.getScenes( ).forEach( sc ->
		{
			this.scenes.add( sc.getNewScene( ) );
		} );
		
		Optional<Scene> first = this.scenes.stream( ).filter( sc -> sc.getClass( ).isAnnotationPresent( Bootable.class ) ).findFirst();

		nextScene( first.isPresent( ) ? this.scenes.indexOf(first.get( )) : 0 );
	}
	
	@Override
	public void processLogics( )
	{
		//Calcula o tempo entre dois updates
		long time = System.currentTimeMillis( ) - previous;
		
		if( bMudaScene )
		{
			changeScene( );
		}
		
		if( running )
		{
			gameLogic.update( time );
		}
					
		//Grava o tempo na sa�da do m�todo
		previous = System.currentTimeMillis( );
		
		framesRender++;
	}

	@Override
	public void renderGraphics( ) 
	{
		EngineGraphicsContext g = getScreen().getGraphicsContext( );
		
		//Limpamos a tela
		g.setFill( Color.WHITE );
		g.fillRect( -g.getTransform().getTx(), -g.getTransform().getTy(), getScreen().getWidth( ), getScreen().getHeight( ) );

		graphics = g;
		
		if( running )
		{
			gameLogic.draw( g );
		}
		
		if( configurations.isDebugMode( ) )
		{
		    g.setFill( Paint.valueOf( Color.BLACK.toString( ) ) );
			g.fillText( calculaFrames( ), 25, 25 );
		}
		
		if( isSave( ) )
		{
			save( graphics );
		}
	}
	
	private void save( EngineGraphicsContext graphics2 ) 
	{
//		BufferedImage image = new BufferedImage( (int)Screen.getScreen().getWidth(), (int)Screen.getScreen().getHeight(), ImageType.RGB.ordinal() );
//		
//		graphics2.get
	}

	private boolean isSave( ){
		return true;
	}

	private void renderLoadingScreen( )
	{
		this.gameLogic = new Loading( );
		this.gameLogic.setup( );
	}

	/**
	 * Calcula os Frames por segundo
	 * @return
	 */
	private String calculaFrames( )
	{
		double d = (System.currentTimeMillis( ) / 1000l) - (timeOnStart / 1000l);
		
		d = framesRender / d;
		
		return "" + Math.round(d);
	}

	@Override
	public void paintScreen( )
	{
		getScreen( ).present( );
	}

	/**
	 * Aciona a engine para mudar a scene
	 * @param nScene
	 */
	public void nextScene( int nScene )
	{
	    nLastScene = nNextScene;
	    
		bMudaScene = true;
		nNextScene = nScene;
	}
	
	private void changeScene( )
	{
		if( this.scenes.size() >= nNextScene )
		{
			gameLogic.onCallChange( );
			
			renderLoadingScreen( );
			
			Scene scene = scenes.get( nNextScene );
			scene.clearScene( );
			
			Runnable task = ( ) -> {
				scene.setup( );
				gameLogic = scene;
			};
			
			ScheduledExecutorService ses = Executors.newScheduledThreadPool( 2 );
			ses.execute( task );
			ses.shutdown( );
			
			bMudaScene = false;
		}
	}
	
	@Override
	public void tearDown( )
	{
	}

	public void startMainLoop( )
	{
		//Iniciamos o main loop
		loop.run( );
	}

	public void stop( )
	{
		loop.stop( );
	}

	public EngineGraphicsContext getGraphics2d( )
	{
		if( graphics == null )
		{
		    graphics = getScreen().getGraphicsContext();
		}
		
		return graphics;
	}

    public int getLastScene( )
    {
        return nLastScene;
    }
    
    public Scene getCurrentScene( )
    {
        return this.gameLogic;
    }
    
    public void setRunning( boolean running )
    {
		this.running = running;
	}

	public Configurations getConfigurations( )
	{
		return configurations;
	}

	public Screen getScreen( )
	{
		return screen;
	}
}