package br.com.engine.main;

public class Executor
{
    public static void loadGame( String[] args ) 
    {
        System.setProperty( "enginefx.backend", "vulkan" );
        new LwjglVulkanExecutor( ).start( );
    }
}
