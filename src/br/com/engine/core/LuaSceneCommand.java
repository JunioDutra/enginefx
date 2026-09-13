package br.com.engine.core;

import br.com.engine.scripting.ScriptValue;

/** A validated composition request emitted by a Lua scene. */
public record LuaSceneCommand(String type, ScriptValue payload)
{
    public LuaSceneCommand
    {
        if (type == null || !type.matches("[a-z][a-z0-9_.-]{0,127}"))
            throw new IllegalArgumentException("Invalid Lua scene command: " + type);
        payload = payload == null ? ScriptValue.of(null) : payload;
    }
}
