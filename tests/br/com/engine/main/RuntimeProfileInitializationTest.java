package br.com.engine.main;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

public class RuntimeProfileInitializationTest
{
    @Test void anAppliedPropertyCannotAuthorizeStartup() throws Exception { probe("spoof"); }
    @Test void backendMutationIsRejectedBeforePlatformStartup() throws Exception { probe("backend"); }
    @Test void clearingPropertiesCannotResetTheProcessProfile() throws Exception { probe("reset"); }

    private static void probe(String scenario) throws Exception
    {
        var process = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
            "-cp", System.getProperty("java.class.path"), RuntimeProfileInitializationTest.class.getName(), scenario)
            .redirectErrorStream(true).start();
        try
        {
            assertTrue(process.waitFor(20, TimeUnit.SECONDS), "Profile probe timed out");
            assertEquals(0, process.exitValue(), new String(process.getInputStream().readAllBytes()));
        }
        finally { if (process.isAlive()) process.destroyForcibly(); }
    }

    public static void main(String[] args)
    {
        System.clearProperty(RuntimeProfile.PROFILE_PROPERTY);
        System.clearProperty("org.graalvm.nativeimage.imagecode");
        System.clearProperty("org.lwjgl.system.memoryBackend");
        if (args[0].equals("spoof"))
        {
            System.setProperty("enginefx.runtimeProfile.applied", "JVM_FFM");
            rejected(() -> RuntimeProfile.JVM_FFM.requireInitialized());
            return;
        }
        RuntimeProfile.initialize();
        RuntimeProfile.initialize();
        if (args[0].equals("backend"))
        {
            System.setProperty("org.lwjgl.system.memoryBackend", "unsafe");
            rejected(() -> RuntimeProfile.JVM_FFM.requireInitialized());
        }
        else
        {
            System.clearProperty("enginefx.runtimeProfile.applied");
            System.clearProperty("org.lwjgl.system.memoryBackend");
            System.setProperty(RuntimeProfile.PROFILE_PROPERTY, "native_jni");
            rejected(RuntimeProfile::initialize);
        }
    }

    private static void rejected(Runnable action)
    {
        try { action.run(); }
        catch (IllegalStateException expected) { return; }
        throw new AssertionError("Runtime profile change was accepted");
    }
}
