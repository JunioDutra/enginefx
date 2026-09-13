package br.com.engine.main;

import java.util.Locale;

/**
 * Chooses the LWJGL memory backend before any GLFW, Vulkan or Lua runtime is
 * initialized. The profile is process-wide and cannot change after startup.
 */
public enum RuntimeProfile
{
    JVM_FFM("ffm"),
    NATIVE_JNI("unsafe");

    public static final String PROFILE_PROPERTY = "enginefx.runtimeProfile";
    private static final String APPLIED_PROPERTY = "enginefx.runtimeProfile.applied";
    private static RuntimeProfile initialized;
    private final String memoryBackend;

    RuntimeProfile(String memoryBackend) { this.memoryBackend = memoryBackend; }

    public String memoryBackend() { return memoryBackend; }

    /** Selects the requested profile, or detects a Native Image process. */
    public static RuntimeProfile current()
    {
        return select(System.getProperty(PROFILE_PROPERTY), System.getProperty("org.graalvm.nativeimage.imagecode"));
    }

    static RuntimeProfile select(String configured, String imageCode)
    {
        if (configured == null || configured.isBlank())
            return imageCode == null || imageCode.isBlank() ? JVM_FFM : NATIVE_JNI;
        return switch (configured.trim().toLowerCase(Locale.ROOT))
        {
            case "jvm_ffm", "jvm-ffm" -> JVM_FFM;
            case "native_jni", "native-jni" -> NATIVE_JNI;
            default -> throw new IllegalArgumentException("Unsupported runtime profile: " + configured);
        };
    }

    /** Applies the process-wide profile before a platform class is used. */
    public static synchronized RuntimeProfile initialize()
    {
        RuntimeProfile selected = current();
        if (initialized != null && selected != initialized)
            throw new IllegalStateException("Runtime profile is already active: " + initialized);
        String configuredBackend = System.getProperty("org.lwjgl.system.memoryBackend");
        if (configuredBackend != null && !selected.memoryBackend.equals(configuredBackend))
            throw new IllegalStateException("LWJGL memory backend conflicts with runtime profile " + selected + ": " + configuredBackend);
        System.setProperty("enginefx.backend", "vulkan");
        System.setProperty("org.lwjgl.system.memoryBackend", selected.memoryBackend);
        System.setProperty(APPLIED_PROPERTY, selected.name());
        initialized = selected;
        return selected;
    }

    void requireInitialized()
    {
        synchronized (RuntimeProfile.class)
        {
            if (initialized != this)
                throw new IllegalStateException("Runtime profile must be initialized before platform startup: " + this);
            if (current() != this || !memoryBackend.equals(System.getProperty("org.lwjgl.system.memoryBackend")))
                throw new IllegalStateException("Runtime profile changed before platform startup: " + this);
        }
    }
}
