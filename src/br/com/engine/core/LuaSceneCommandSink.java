package br.com.engine.core;

@FunctionalInterface
public interface LuaSceneCommandSink
{
    void execute(LuaSceneCommand command);
}
