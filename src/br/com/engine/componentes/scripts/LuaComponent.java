package br.com.engine.componentes.scripts;

import br.com.engine.componentes.SimpleComponent;
import br.com.engine.scripting.ScriptContext;
import br.com.engine.scripting.ScriptEvent;
import br.com.engine.scripting.ScriptInstance;
import br.com.engine.scripting.ScriptModule;
import br.com.engine.scripting.ScriptRuntime;

/** A lifecycle adapter that never exposes its GameObject to Lua. */
public final class LuaComponent extends SimpleComponent
{
    private final ScriptRuntime runtime;
    private final ScriptModule module;
    private final ScriptContext context;
    private ScriptInstance instance;
    private boolean disposed;

    public LuaComponent(ScriptRuntime runtime, ScriptModule module, ScriptContext context)
    {
        if (runtime == null || module == null) throw new IllegalArgumentException("Lua runtime and module are required");
        this.runtime = runtime;
        this.module = module;
        this.context = context == null ? ScriptContext.empty() : context;
    }

    @Override public void setup()
    {
        if (disposed) throw new IllegalStateException("Lua component is disposed");
        if (instance != null) return;
        instance = runtime.createInstance(module, context);
        try { instance.setup(); }
        catch (RuntimeException failure)
        {
            try { dispose(); } catch (RuntimeException cleanup) { failure.addSuppressed(cleanup); }
            throw failure;
        }
    }

    @Override public void update(long deltaMillis)
    {
        requireInstance().update(seconds(deltaMillis));
    }

    @Override public void fixedUpdate(float deltaSeconds)
    {
        requireInstance().fixedUpdate(deltaSeconds);
    }

    /** Delivers a host-authorized event without exposing input or scene singletons. */
    public void onEvent(ScriptEvent event) { requireInstance().onEvent(event); }

    @Override public void draw() { }

    @Override public void dispose()
    {
        if (disposed) return;
        disposed = true;
        ScriptInstance closing = instance;
        instance = null;
        if (closing != null) closing.dispose();
    }

    private ScriptInstance requireInstance()
    {
        if (instance == null) throw new IllegalStateException("Lua component has not been set up");
        return instance;
    }

    private static double seconds(long deltaMillis)
    {
        if (deltaMillis < 0) throw new IllegalArgumentException("Lua delta must be non-negative");
        return deltaMillis / 1_000.0;
    }
}
