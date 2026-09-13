package br.com.engine.main;

import br.com.engine.core.SceneRegistry;

public class Executor
{
    /** Starts a game with explicit scene factories and a fixed runtime profile. */
    public static void loadGame(String[] args, SceneRegistry scenes)
    {
        if (scenes == null) throw new IllegalArgumentException("Scene registry is required");
        RuntimeProfile profile = RuntimeProfile.initialize();
        new LwjglVulkanExecutor(profile, scenes).start();
    }
}
