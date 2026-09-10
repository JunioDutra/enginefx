package br.com.engine.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import br.com.engine.core.annotation.Bootable;
import br.com.engine.graphics.Color;
import br.com.engine.graphics.EngineGraphicsContext;
import br.com.engine.graphics.Font;
import br.com.engine.graphics.Paint;
import br.com.engine.interfaces.LoopSteps;
import br.com.engine.resources.Configurations;
import br.com.engine.resources.ResourceManager;
import br.com.engine.resources.ScenesDefinition;
import br.com.engine.scenes.Loading;

public class ControleBase implements LoopSteps
{
    private static ControleBase controleBase;

    private long  previousNanos = System.nanoTime( );
    private static final float FIXED_STEP_SECONDS = 1f / 60f;
    private static final int MAX_FIXED_STEPS_PER_FRAME = 5;
    private float physicsAccumulator;
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
    private final Font debugFont;

    private ControleBase( )
    {
        configurations = ResourceManager.configurations( );
        debugFont = Boolean.TRUE.equals(configurations.isDebugMode()) ? ResourceManager.font("fonts/font.ttf", 12) : null;
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

        if( bMudaScene )
        {
            changeScene( );
        }

        if( running )
        {
            gameLogic.update( Time.getDeltaMillis( ) );
            physicsAccumulator = Math.min( physicsAccumulator + Time.getDeltaTime( ), FIXED_STEP_SECONDS * MAX_FIXED_STEPS_PER_FRAME );
            int steps = 0;
            while( physicsAccumulator >= FIXED_STEP_SECONDS && steps++ < MAX_FIXED_STEPS_PER_FRAME )
            {
                gameLogic.fixedUpdate( FIXED_STEP_SECONDS );
                physicsAccumulator -= FIXED_STEP_SECONDS;
            }
            Time.setInterpolationAlpha( physicsAccumulator / FIXED_STEP_SECONDS );
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

        if( Boolean.TRUE.equals(configurations.isDebugMode()) )
        {
            g.setFill( Paint.valueOf( Color.BLACK.toString( ) ) );
			g.setFont( debugFont );
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
        if( nScene < 0 || nScene >= scenes.size( ) ) throw new IllegalArgumentException( "Invalid scene index: " + nScene );
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
        if( nNextScene >= 0 && nNextScene < this.scenes.size( ) )
        {
            try { gameLogic.onCallChange(); } finally { gameLogic.dispose(); }


            getScreen( ).getGraphicsContext( ).resetTransform( );
            Scene scene = sceneDefinitions.get( nNextScene ).getNewScene( );
            scenes.set( nNextScene, scene );

            gameLogic = scene;
            try { scene.setup(); }
            catch (RuntimeException failure) {
                try { scene.dispose(); } catch (RuntimeException cleanup) { failure.addSuppressed(cleanup); }
                throw failure;
            }
            physicsAccumulator = 0;
            Time.reset();
            previousNanos = System.nanoTime();

            bMudaScene = false;
        }
    }

    @Override
    public void tearDown( )
    {
        stop();
    }

    public void stop( )
    {
        running = false;
        try { if (gameLogic != null) gameLogic.dispose(); }
        finally { scenes.clear(); br.com.engine.resources.ContentLoader.clearCaches(); }
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

    public List<ScenesDefinition> getSceneDefinitions( )
    {
        return List.copyOf( sceneDefinitions );
    }

    public Screen getScreen( )
    {
        return screen;
    }
}
