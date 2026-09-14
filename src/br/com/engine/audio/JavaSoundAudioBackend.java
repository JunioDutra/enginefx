package br.com.engine.audio;

import java.io.ByteArrayInputStream;
import java.io.BufferedInputStream;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;

/** JVM backend. Native Image deliberately selects OpenAL before Java Sound can inspect java.home. */
final class JavaSoundAudioBackend implements AudioBackend
{
    private Clip clip;

    JavaSoundAudioBackend(byte[] bytes) throws Exception
    {
        Clip loaded = null;
        try (var input = new BufferedInputStream(new ByteArrayInputStream(bytes));
             AudioInputStream audio = AudioSystem.getAudioInputStream(input))
        {
            loaded = AudioSystem.getClip();
            loaded.open(audio);
            clip = loaded;
        }
        catch (Exception exception)
        {
            if (loaded != null) loaded.close();
            throw exception;
        }
    }

    @Override public void play()
    {
        if (clip == null) return;
        clip.stop();
        clip.setFramePosition(0);
        clip.start();
    }

    @Override public void stop() { if (clip != null) clip.stop(); }

    @Override public void setVolume(double volume)
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
