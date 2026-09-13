package br.com.engine.scripting;

import java.util.List;

@FunctionalInterface
public interface ScriptApi
{
    ScriptValue invoke(ScriptContext context, List<ScriptValue> arguments);
}
