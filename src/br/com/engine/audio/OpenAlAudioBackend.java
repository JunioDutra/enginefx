package br.com.engine.audio;

import static org.lwjgl.openal.AL10.AL_BUFFER;
import static org.lwjgl.openal.AL10.AL_FORMAT_MONO16;
import static org.lwjgl.openal.AL10.AL_FORMAT_MONO8;
import static org.lwjgl.openal.AL10.AL_FORMAT_STEREO16;
import static org.lwjgl.openal.AL10.AL_FORMAT_STEREO8;
import static org.lwjgl.openal.AL10.AL_GAIN;
import static org.lwjgl.openal.AL10.alBufferData;
import static org.lwjgl.openal.AL10.alDeleteBuffers;
import static org.lwjgl.openal.AL10.alDeleteSources;
import static org.lwjgl.openal.AL10.alGenBuffers;
import static org.lwjgl.openal.AL10.alGenSources;
import static org.lwjgl.openal.AL10.alGetError;
import static org.lwjgl.openal.AL10.alSourcePlay;
import static org.lwjgl.openal.AL10.alSourceStop;
import static org.lwjgl.openal.AL10.alSourcef;
import static org.lwjgl.openal.AL10.alSourcei;
import static org.lwjgl.openal.AL10.AL_NO_ERROR;
import static org.lwjgl.openal.ALC10.ALC_NO_ERROR;
import static org.lwjgl.openal.ALC10.alcCloseDevice;
import static org.lwjgl.openal.ALC10.alcCreateContext;
import static org.lwjgl.openal.ALC10.alcDestroyContext;
import static org.lwjgl.openal.ALC10.alcGetError;
import static org.lwjgl.openal.ALC10.alcMakeContextCurrent;
import static org.lwjgl.openal.ALC10.alcOpenDevice;

import java.nio.ByteBuffer;

import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.system.MemoryUtil;

/** OpenAL Soft backend for Native Image. One owned device/context is shared by all engine clips on the game thread. */
final class OpenAlAudioBackend implements AudioBackend
{
    private static final Object LOCK = new Object();
    private static Runtime runtime;

    private int source;
    private int buffer;
    private boolean closed;

    OpenAlAudioBackend(byte[] bytes)
    {
        PcmWave wave = PcmWave.read(bytes);
        synchronized (LOCK)
        {
            Runtime active = acquire();
            try
            {
                buffer = alGenBuffers();
                checkAl("generate buffer");
                ByteBuffer samples = MemoryUtil.memAlloc(wave.samples().remaining());
                try
                {
                    samples.put(wave.samples()).flip();
                    alBufferData(buffer, format(wave), samples, wave.sampleRate());
                    checkAl("upload PCM WAV");
                }
                finally { MemoryUtil.memFree(samples); }
                source = alGenSources();
                checkAl("generate source");
                alSourcei(source, AL_BUFFER, buffer);
                checkAl("attach buffer");
                active.references++;
            }
            catch (RuntimeException exception)
            {
                if (source != 0) alDeleteSources(source);
                if (buffer != 0) alDeleteBuffers(buffer);
                releaseIfUnused(active);
                throw exception;
            }
        }
    }

    @Override public void play()
    {
        synchronized (LOCK)
        {
            if (closed) return;
            requireRuntime();
            alSourceStop(source);
            alSourcePlay(source);
            checkAl("play source");
        }
    }

    @Override public void stop()
    {
        synchronized (LOCK)
        {
            if (closed) return;
            requireRuntime();
            alSourceStop(source);
            checkAl("stop source");
        }
    }

    @Override public void setVolume(double volume)
    {
        synchronized (LOCK)
        {
            if (closed) return;
            requireRuntime();
            alSourcef(source, AL_GAIN, (float)Math.max(0.0, Math.min(1.0, volume)));
            checkAl("set source volume");
        }
    }

    @Override public void close()
    {
        synchronized (LOCK)
        {
            if (closed) return;
            closed = true;
            requireRuntime();
            try
            {
                alSourceStop(source);
                alDeleteSources(source);
                alDeleteBuffers(buffer);
                checkAl("dispose source");
            }
            finally
            {
                source = 0;
                buffer = 0;
                runtime.references--;
                releaseIfUnused(runtime);
            }
        }
    }

    private static Runtime acquire()
    {
        if (runtime == null) runtime = Runtime.open();
        runtime.requireOwnerThread();
        return runtime;
    }

    private static void requireRuntime()
    {
        if (runtime == null) throw new IllegalStateException("OpenAL runtime is closed");
        runtime.requireOwnerThread();
    }

    private static void releaseIfUnused(Runtime active)
    {
        if (active.references != 0) return;
        active.close();
        if (runtime == active) runtime = null;
    }

    private static int format(PcmWave wave)
    {
        return switch (wave.channels() * 100 + wave.bitsPerSample())
        {
            case 108 -> AL_FORMAT_MONO8;
            case 116 -> AL_FORMAT_MONO16;
            case 208 -> AL_FORMAT_STEREO8;
            case 216 -> AL_FORMAT_STEREO16;
            default -> throw new IllegalArgumentException("Unsupported PCM WAV format");
        };
    }

    private static void checkAl(String operation)
    {
        int code = alGetError();
        if (code != AL_NO_ERROR) throw new IllegalStateException("OpenAL cannot " + operation + ": 0x" + Integer.toHexString(code));
    }

    private static final class Runtime
    {
        private final long device;
        private final long context;
        private final Thread owner;
        private int references;

        private Runtime(long device, long context)
        {
            this.device = device;
            this.context = context;
            this.owner = Thread.currentThread();
        }

        static Runtime open()
        {
            long device = alcOpenDevice((ByteBuffer)null);
            if (device == MemoryUtil.NULL) throw new IllegalStateException("OpenAL default device is unavailable");
            long context = MemoryUtil.NULL;
            try
            {
                ALCCapabilities capabilities = ALC.createCapabilities(device);
                context = alcCreateContext(device, (java.nio.IntBuffer)null);
                if (context == MemoryUtil.NULL) throw new IllegalStateException("OpenAL context is unavailable");
                if (!alcMakeContextCurrent(context)) throw new IllegalStateException("OpenAL cannot make context current");
                AL.createCapabilities(capabilities);
                int error = alcGetError(device);
                if (error != ALC_NO_ERROR) throw new IllegalStateException("OpenAL initialization failed: 0x" + Integer.toHexString(error));
                return new Runtime(device, context);
            }
            catch (RuntimeException exception)
            {
                if (context != MemoryUtil.NULL) alcDestroyContext(context);
                alcCloseDevice(device);
                throw exception;
            }
        }

        void requireOwnerThread()
        {
            if (owner != Thread.currentThread()) throw new IllegalStateException("OpenAL audio must stay on its game thread");
        }

        void close()
        {
            requireOwnerThread();
            alcMakeContextCurrent(MemoryUtil.NULL);
            alcDestroyContext(context);
            alcCloseDevice(device);
        }
    }
}
