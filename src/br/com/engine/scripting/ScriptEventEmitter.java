package br.com.engine.scripting;

@FunctionalInterface
public interface ScriptEventEmitter
{
    void emit(ScriptEvent event);
}
