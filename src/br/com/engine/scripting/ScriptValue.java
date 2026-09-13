package br.com.engine.scripting;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A deeply immutable JSON-compatible value. It is the only value shape that
 * may be exchanged with scripts; engine and game objects are never accepted.
 */
public final class ScriptValue
{
    private final Object value;

    private ScriptValue(Object value) { this.value = value; }

    public static ScriptValue of(Object value) { return of(value, ScriptLimits.DEFAULT); }

    public static ScriptValue of(Object value, ScriptLimits limits)
    {
        Objects.requireNonNull(limits, "limits");
        Counter counter = new Counter(limits.maxValueNodes());
        Object normalized = normalize(value, limits, 0, counter, new IdentityHashMap<>());
        return new ScriptValue(normalized);
    }

    private static Object normalize(Object source, ScriptLimits limits, int depth, Counter counter,
                                    IdentityHashMap<Object, Boolean> ancestors)
    {
        if (source instanceof ScriptValue scriptValue)
            return normalize(scriptValue.value, limits, depth, counter, ancestors);
        if (depth > limits.maxValueDepth()) throw new IllegalArgumentException("Script value exceeds maximum depth");
        counter.next();
        if (source == null || source instanceof Boolean) return source;
        if (source instanceof Byte || source instanceof Short || source instanceof Integer || source instanceof Long)
            return ((Number)source).longValue();
        if (source instanceof Float || source instanceof Double)
        {
            double number = ((Number)source).doubleValue();
            if (!Double.isFinite(number)) throw new IllegalArgumentException("Script numbers must be finite");
            return number;
        }
        if (source instanceof String string)
        {
            checkText(string, limits);
            return string;
        }
        if (source instanceof Map<?, ?> map)
        {
            enter(source, ancestors);
            try
            {
                Map<String, Object> result = new LinkedHashMap<>();
                for (Map.Entry<?, ?> entry : map.entrySet())
                {
                    if (!(entry.getKey() instanceof String key))
                        throw new IllegalArgumentException("Script maps require string keys");
                    checkText(key, limits);
                    result.put(key, normalize(entry.getValue(), limits, depth + 1, counter, ancestors));
                }
                return Collections.unmodifiableMap(result);
            }
            finally { ancestors.remove(source); }
        }
        if (source instanceof Collection<?> collection)
        {
            enter(source, ancestors);
            try
            {
                List<Object> result = new ArrayList<>();
                for (Object item : collection) result.add(normalize(item, limits, depth + 1, counter, ancestors));
                return Collections.unmodifiableList(result);
            }
            finally { ancestors.remove(source); }
        }
        throw new IllegalArgumentException("Unsupported script value type: " + source.getClass().getName());
    }

    private static void enter(Object source, IdentityHashMap<Object, Boolean> ancestors)
    {
        if (ancestors.put(source, Boolean.TRUE) != null) throw new IllegalArgumentException("Script values cannot contain cycles");
    }

    private static void checkText(String text, ScriptLimits limits)
    {
        if (text.length() > limits.maxStringBytes()) throw new IllegalArgumentException("Script string exceeds maximum size");
        try
        {
            int bytes = StandardCharsets.UTF_8.newEncoder()
                .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
                .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)
                .encode(java.nio.CharBuffer.wrap(text)).remaining();
            if (bytes > limits.maxStringBytes()) throw new IllegalArgumentException("Script string exceeds maximum size");
        }
        catch (java.nio.charset.CharacterCodingException exception)
        {
            throw new IllegalArgumentException("Script text must contain valid Unicode", exception);
        }
    }

    /** Returns only null, Boolean, Long, Double, immutable List and immutable Map. */
    public Object toJavaObject() { return value; }
    public boolean isNull() { return value == null; }
    public String asString() { return require(String.class, "string"); }
    public boolean asBoolean() { return require(Boolean.class, "boolean"); }
    public long asLong() { return require(Long.class, "integer"); }
    public double asDouble() { return value instanceof Long number ? number.doubleValue() : require(Double.class, "number"); }

    @SuppressWarnings("unchecked")
    public List<Object> asList()
    {
        if (!(value instanceof List<?>)) throw new IllegalStateException("Script value is not a list");
        return (List<Object>)value;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> asMap()
    {
        if (!(value instanceof Map<?, ?>)) throw new IllegalStateException("Script value is not a map");
        return (Map<String, Object>)value;
    }

    private <T> T require(Class<T> type, String label)
    {
        if (!type.isInstance(value)) throw new IllegalStateException("Script value is not a " + label);
        return type.cast(value);
    }

    @Override public boolean equals(Object other) { return other instanceof ScriptValue that && Objects.equals(value, that.value); }
    @Override public int hashCode() { return Objects.hashCode(value); }
    @Override public String toString() { return String.valueOf(value); }

    private static final class Counter
    {
        private final int maximum;
        private int count;
        private Counter(int maximum) { this.maximum = maximum; }
        private void next()
        {
            if (++count > maximum) throw new IllegalArgumentException("Script value exceeds maximum node count");
        }
    }
}
