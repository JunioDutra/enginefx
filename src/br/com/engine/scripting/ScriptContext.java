package br.com.engine.scripting;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Capabilities and data deliberately handed to one script instance. */
public final class ScriptContext
{
    private final Set<String> capabilities;
    private final ScriptClock clock;
    private final ScriptEventEmitter emitter;
    private final Map<String, ScriptValue> state = new LinkedHashMap<>();

    public ScriptContext(Set<String> capabilities, Map<String, ?> initialState, ScriptClock clock, ScriptEventEmitter emitter)
    {
        this.capabilities = Set.copyOf(capabilities == null ? Set.of() : capabilities);
        if (!this.capabilities.stream().allMatch(capability -> "*".equals(capability) || capability.matches("[a-z][a-z0-9.*_-]{0,127}")))
            throw new IllegalArgumentException("Invalid script capability");
        this.clock = clock == null ? Clock.systemUTC()::instant : clock;
        this.emitter = emitter == null ? event -> { } : emitter;
        replaceState(initialState == null ? Map.of() : initialState);
    }

    public static ScriptContext empty() { return new ScriptContext(Set.of(), Map.of(), null, null); }
    public Set<String> capabilities() { return capabilities; }
    public boolean allows(String capability)
    {
        return capabilities.contains(capability) || capabilities.contains("*") ||
            capabilities.stream().anyMatch(allowed -> allowed.endsWith(".*") && capability.startsWith(allowed.substring(0, allowed.length() - 1)));
    }
    public ScriptClock clock() { return clock; }
    public ScriptEventEmitter emitter() { return emitter; }
    public Map<String, ScriptValue> state() { return Map.copyOf(state); }
    public ScriptValue state(String key) { return state.getOrDefault(key, ScriptValue.of(null)); }
    public void putState(String key, Object value)
    {
        if (key == null || !key.matches("[A-Za-z0-9_.-]{1,128}")) throw new IllegalArgumentException("Invalid script state key");
        state.put(key, ScriptValue.of(value));
    }
    public void replaceState(Map<String, ?> values)
    {
        Objects.requireNonNull(values, "values");
        Map<String, ScriptValue> replacement = new LinkedHashMap<>();
        values.forEach((key, value) -> {
            if (key == null || !key.matches("[A-Za-z0-9_.-]{1,128}")) throw new IllegalArgumentException("Invalid script state key");
            replacement.put(key, ScriptValue.of(value));
        });
        state.clear();
        state.putAll(replacement);
    }
}
