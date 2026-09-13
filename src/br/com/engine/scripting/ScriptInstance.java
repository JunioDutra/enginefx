package br.com.engine.scripting;

/** One isolated state created from a ScriptModule. */
public interface ScriptInstance extends AutoCloseable
{
    void setup();
    void update(double deltaSeconds);
    void fixedUpdate(double deltaSeconds);
    void onEvent(ScriptEvent event);
    ScriptValue call(String callback, ScriptValue... arguments);
    boolean isDisposed();
    void dispose();
    @Override default void close() { dispose(); }
}
