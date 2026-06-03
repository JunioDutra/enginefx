package br.com.engine.core;

import br.com.engine.interfaces.LoopSteps;

public class MainLoopFx implements Runnable
{
    private final LoopSteps game;
    private Thread loopThread;
    private volatile boolean running;

    public MainLoopFx( LoopSteps loopSteps )
    {
        super( );
        
        this.game = loopSteps;
    }

    /**
     * Runs the main loop. This method is not thread safe and should not be
     * called more than once.
     */
    public void run( )
    {
        if( running )
        {
            return;
        }

        running = true;
        loopThread = new Thread( this::loopBody, "enginefx-main-loop" );
        loopThread.start( );
    }

    private void loopBody( )
    {
        final long frameTimeNanos = 1_000_000_000L / 60L;
        game.setup( );

        while( running )
        {
            long startedAt = System.nanoTime( );

            try
            {
                game.processLogics( );
                game.renderGraphics( );
                game.paintScreen( );
            }
            catch( Exception exception )
            {
                exception.printStackTrace( );
                game.tearDown( );
                running = false;
                throw new RuntimeException( "Exception during game loop", exception );
            }

            long elapsed = System.nanoTime( ) - startedAt;
            long remaining = frameTimeNanos - elapsed;

            if( remaining > 0 )
            {
                try
                {
                    Thread.sleep( remaining / 1_000_000L, (int)(remaining % 1_000_000L) );
                }
                catch( InterruptedException exception )
                {
                    Thread.currentThread( ).interrupt( );
                    running = false;
                }
            }
        }
    }

    public void stop( )
    {
        running = false;

        if( loopThread != null )
        {
            loopThread.interrupt( );
        }
    }
}
