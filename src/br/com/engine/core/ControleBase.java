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
import br.com.engine.interfaces.LoopSteps;
import br.com.engine.resources.Configurations;
import br.com.engine.resources.ResourceManager;
import br.com.engine.resources.ScenesDefinition;
import br.com.engine.scenes.Loading;

public class ControleBase implements LoopSteps
{
    private static ControleBase controleBase;

    private long  previous = System.currentTimeMillis( );
    private long  previousNanos = System.nanoTime( );
    private long  nanosCarry = 0;
    private final Screen  screen;
    private Scene gameLogic;
    private boolean running = true;

    private List<Scene> scenes;
    private List<ScenesDefinition> sceneDefinitions;

    private boolean bMudaScene = false;
    private int     nNextScene = -1;
    private int     nLastScene = -1;
    private int     nBootScene = 0;

    private EngineGraphicsContext graphics;

    private static int framesRender = 0;
    private long       timeOnStart  = System.currentTimeMillis( );

    private final Configurations configurations;

    private ControleBase( )
    {
        configurations = ResourceManager.loadResource( null, ResourceManager.CONFIGURACOES, Configurations.class );
        screen = new Screen( configurations.getSizeW(), configurations.getSizeH() );
        this.scenes = new ArrayList<Scene>( );
        this.sceneDefinitions = new ArrayList<ScenesDefinition>( );
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
            this.sceneDefinitions.add( sc );
            this.scenes.add( sc.getNewScene( ) );
        } );

        Optional<Scene> first = this.scenes.stream( ).filter( sc -> sc.getClass( ).isAnnotationPresent( Bootable.class ) ).findFirst();

        nBootScene = first.isPresent( ) ? this.scenes.indexOf(first.get( )) : 0;

        nextScene( nBootScene );
    }

    @Override
    public void processLogics( )
    {
        long now = System.nanoTime( );
        long elapsedNanos = now - previousNanos;
        previousNanos = now;

        Time.update( elapsedNanos );

        // Milissegundos legados para o param update(long time): acumula o resto
        // em nanos para não perder frações em frames de menos de 1 ms.
        nanosCarry += elapsedNanos;
        long time = nanosCarry / 1_000_000L;
        nanosCarry -= time * 1_000_000L;

        if( bMudaScene )
        {
            changeScene( );
        }

        if( running )
        {
            gameLogic.update( time );
        }

        framesRender++;
    }

    @Override
    public void renderGraphics( ) 
    {
        EngineGraphicsContext g = getScreen().getGraphicsContext( );

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
    }

    private void renderLoadingScreen( )
    {
        this.gameLogic = new Loading( );
        this.gameLogic.setup( );
    }

    private String calculaFrames( )
    {
        double d = (System.currentTimeMillis( ) / 1000l) - (timeOnStart / 1000l);
        if (d <= 0) d = 1;
        d = framesRender / d;
        return "" + Math.round(d);
    }

    @Override
    public void paintScreen( )
    {
    }

    public void nextScene( int nScene )
    {
        nLastScene = nNextScene;
        bMudaScene = true;
        nNextScene = nScene;
    }

    public void goToBootScene( )
    {
        if( !bMudaScene && nNextScene != nBootScene )
        {
            nextScene( nBootScene );
        }
    }

    public int getBootScene( )
    {
        return nBootScene;
    }

    private void changeScene( )
    {
        if( this.scenes.size() >= nNextScene )
        {
            gameLogic.onCallChange( );
            renderLoadingScreen( );

            getScreen( ).getGraphicsContext( ).resetTransform( );

            Scene scene = sceneDefinitions.get( nNextScene ).getNewScene( );
            scenes.set( nNextScene, scene );

            Runnable task = () -> { try { scene.setup(); gameLogic = scene; } catch (Exception e) { e.printStackTrace(); } }; task.run();

            bMudaScene = false;
        }
    }

    @Override
    public void tearDown( )
    {
    }

    public void stop( )
    {
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

