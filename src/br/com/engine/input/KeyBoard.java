package br.com.engine.input;

import java.util.Collection;
import java.util.EnumSet;

public class KeyBoard
{
    private static KeyBoard instance;

    private final Collection<KeyCode> lstCurrent = EnumSet.noneOf(KeyCode.class);
    private final Collection<KeyCode> pressedThisFrame = EnumSet.noneOf(KeyCode.class);
    private final Collection<KeyCode> releasedThisFrame = EnumSet.noneOf(KeyCode.class);
    private final Collection<KeyCode> pendingPresses = EnumSet.noneOf(KeyCode.class);
    private final Collection<KeyCode> pendingReleases = EnumSet.noneOf(KeyCode.class);
    
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
        if (isDown(key)) run.run();
    }

    /** Returns true while the key is held down. */
    public synchronized boolean isDown(KeyCode key)
    {
        return key != null && lstCurrent.contains(key);
    }

    /** Returns true only during the logical frame in which the key went down. */
    public synchronized boolean wasPressedThisFrame(KeyCode key)
    {
        return key != null && pressedThisFrame.contains(key);
    }

    /** Returns true only during the logical frame in which the key was released. */
    public synchronized boolean wasReleasedThisFrame(KeyCode key)
    {
        return key != null && releasedThisFrame.contains(key);
    }

    /** Captures callbacks after event polling for the next logical frame. */
    public synchronized void beginFrame()
    {
        pressedThisFrame.clear();
        releasedThisFrame.clear();
        pressedThisFrame.addAll(pendingPresses);
        releasedThisFrame.addAll(pendingReleases);
        pendingPresses.clear();
        pendingReleases.clear();
    }

    /** Ends a logical frame and consumes its edge state. */
    public synchronized void endFrame()
    {
        pressedThisFrame.clear();
        releasedThisFrame.clear();
    }

    public synchronized void press( KeyCode code )
    {
        if( code != null )
        {
            if (lstCurrent.add(code)) pendingPresses.add(code);
        }
    }

    public synchronized void release( KeyCode code )
    {
        if( code != null )
        {
            if (lstCurrent.remove(code)) pendingReleases.add(code);
        }
    }

    /** Clears input state when a window or game session is discarded. */
    public synchronized void reset()
    {
        lstCurrent.clear();
        pressedThisFrame.clear();
        releasedThisFrame.clear();
        pendingPresses.clear();
        pendingReleases.clear();
    }
}
