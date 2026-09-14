package br.com.engine.audio;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/** Scene-owned audio line with Java Sound on the JVM and OpenAL in Native Image. */
public class AudioClip implements AutoCloseable
{
    private AudioBackend backend;

    public AudioClip(Path path) { this(path.toString(), open(path)); }

    private static InputStream open(Path path)
    {
        try { return Files.newInputStream(path); }
        catch (IOException exception) { throw new IllegalStateException("Cannot load audio: " + path, exception); }
    }

    public AudioClip(String resourcePath, InputStream input)
    {
        try (InputStream source = input)
        {
            byte[] bytes = source.readAllBytes();
            backend = useOpenAl() ? new OpenAlAudioBackend(bytes) : new JavaSoundAudioBackend(bytes);
        }
        catch (Exception exception)
        {
            throw new IllegalStateException("Cannot load audio: " + resourcePath, exception);
        }
    }

    public void play()
    {
        if (backend != null) backend.play();
    }

    public void stop() { if (backend != null) backend.stop(); }

    public void setVolume(double volume)
    {
        if (backend != null) backend.setVolume(volume);
    }

    @Override public void close()
    {
        AudioBackend closing = backend;
        backend = null;
        if (closing != null) closing.close();
    }

    private static boolean useOpenAl()
    {
        return System.getProperty("org.graalvm.nativeimage.imagecode") != null
            || "native_jni".equalsIgnoreCase(System.getProperty("enginefx.runtimeProfile"))
            || "native-jni".equalsIgnoreCase(System.getProperty("enginefx.runtimeProfile"));
    }
}
