package br.com.engine.audio;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;

/** Scene-owned audio line. Loading errors are explicit and all streams/lines are closed. */
public class AudioClip implements AutoCloseable
{
    private Clip clip;

    public AudioClip(Path path) { this(path.toString(), open(path)); }

    private static InputStream open(Path path)
    {
        try { return Files.newInputStream(path); }
        catch (IOException exception) { throw new IllegalStateException("Cannot load audio: " + path, exception); }
    }

    public AudioClip(String resourcePath, InputStream input)
    {
        Clip loaded = null;
        try (InputStream buffered = new BufferedInputStream(input);
             AudioInputStream audio = AudioSystem.getAudioInputStream(buffered))
        {
            loaded = AudioSystem.getClip();
            loaded.open(audio);
            clip = loaded;
        }
        catch (Exception exception)
        {
            if (loaded != null) loaded.close();
            throw new IllegalStateException("Cannot load audio: " + resourcePath, exception);
        }
    }

    public void play()
    {
        if (clip == null) return;
        clip.stop();
        clip.setFramePosition(0);
        clip.start();
    }

    public void stop() { if (clip != null) clip.stop(); }

    public void setVolume(double volume)
    {
        if (clip == null || !clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) return;
        FloatControl gain = (FloatControl)clip.getControl(FloatControl.Type.MASTER_GAIN);
        double clamped = Math.max(0.0001, Math.min(1.0, volume));
        float decibels = (float)(20.0 * Math.log10(clamped));
        gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), decibels)));
    }

    @Override public void close()
    {
        Clip closing = clip;
        clip = null;
        if (closing != null) try { closing.stop(); } finally { closing.close(); }
    }
}
