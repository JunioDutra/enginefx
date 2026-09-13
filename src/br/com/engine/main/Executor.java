package br.com.engine.main;

import br.com.engine.core.SceneRegistry;

public class Executor
{
    public static void loadGame( String[] args ) 
    {
        System.setProperty( "enginefx.backend", "vulkan" );
        new LwjglVulkanExecutor( ).start( );
    }

    /** 2.2 bootstrap path using explicit scene factories. */
    public static void loadGame(String[] args, SceneRegistry scenes)
    {
        System.setProperty("enginefx.backend", "vulkan");
        new LwjglVulkanExecutor(scenes).start();
    }
}
