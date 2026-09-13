package br.com.engine.scripting;

/** Immutable limits enforced whenever data or source crosses the Lua boundary. */
public record ScriptLimits(int maxSourceBytes, int maxInstructionsPerCallback, int hookGranularity,
                           int maxValueDepth, int maxValueNodes, int maxStringBytes)
{
    public static final ScriptLimits DEFAULT = new ScriptLimits(1_048_576, 100_000, 1_000, 16, 10_000, 262_144);

    public ScriptLimits
    {
        if (maxSourceBytes <= 0 || maxInstructionsPerCallback <= 0 || hookGranularity <= 0 ||
            maxValueDepth <= 0 || maxValueNodes <= 0 || maxStringBytes <= 0)
            throw new IllegalArgumentException("All script limits must be positive");
        if (hookGranularity > maxInstructionsPerCallback)
            throw new IllegalArgumentException("Hook granularity cannot exceed the instruction limit");
    }
}
