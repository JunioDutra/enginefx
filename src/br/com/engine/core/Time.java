package br.com.engine.core;

/** Clamped frame time shared by modern seconds-based code and legacy millisecond callbacks. */
public final class Time
{
    public static final float MAX_DELTA_SECONDS = 0.25f;
    private static final long MAX_DELTA_NANOS = 250_000_000L;
    private static float deltaTime;
    private static long deltaMillis;
    private static long nanosCarry;
    private static float interpolationAlpha;

    private Time() { }

    public static void update(long elapsedNanos)
    {
        long clamped = Math.max(0L, Math.min(MAX_DELTA_NANOS, elapsedNanos));
        deltaTime = (float)(clamped / 1_000_000_000.0);
        nanosCarry += clamped;
        deltaMillis = nanosCarry / 1_000_000L;
        nanosCarry %= 1_000_000L;
    }

    public static float getDeltaTime() { return deltaTime; }
    public static long getDeltaMillis() { return deltaMillis; }
    public static float getInterpolationAlpha() { return interpolationAlpha; }

    static void reset()
    {
        deltaTime = 0;
        deltaMillis = nanosCarry = 0;
        interpolationAlpha = 0;
    }

    static void setInterpolationAlpha(float value)
    {
        interpolationAlpha = Math.max(0f, Math.min(1f, value));
    }
}
