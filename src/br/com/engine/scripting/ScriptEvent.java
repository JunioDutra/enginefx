package br.com.engine.scripting;

/** Event emitted by a script through its explicitly supplied context. */
public record ScriptEvent(String type, ScriptValue payload)
{
    public ScriptEvent
    {
        if (type == null || !type.matches("[a-z][a-z0-9_.-]{0,127}"))
            throw new IllegalArgumentException("Invalid script event type: " + type);
        payload = payload == null ? ScriptValue.of(null) : payload;
    }
}
