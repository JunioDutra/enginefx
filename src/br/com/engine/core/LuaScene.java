package br.com.engine.core;

import java.util.Set;

import br.com.engine.scripting.ScriptContext;
import br.com.engine.scripting.ScriptEvent;
import br.com.engine.scripting.ScriptInstance;
import br.com.engine.scripting.ScriptModule;
import br.com.engine.scripting.ScriptRuntime;

/**
 * Lua scene adapter. Lua can emit only declared composition commands; Java
 * owns their interpretation and all Scene/GameObject mutation.
 */
public final class LuaScene extends Scene
{
    private final ScriptRuntime runtime;
    private final ScriptModule module;
    private final ScriptContext context;
    private ScriptInstance instance;

    public LuaScene(ScriptRuntime runtime, ScriptModule module, ScriptContext context,
                    Set<String> allowedCommands, LuaSceneCommandSink commands)
    {
        if (runtime == null || module == null || commands == null) throw new IllegalArgumentException("Lua scene dependencies are required");
        Set<String> allowed = Set.copyOf(allowedCommands == null ? Set.of() : allowedCommands);
        if (!allowed.stream().allMatch(type -> type.matches("[a-z][a-z0-9_.-]{0,127}")))
            throw new IllegalArgumentException("Invalid allowed Lua scene command");
        ScriptContext base = context == null ? ScriptContext.empty() : context;
        this.runtime = runtime;
        this.module = module;
        this.context = new ScriptContext(base.capabilities(), base.state(), base.clock(), event -> {
            if (!allowed.contains(event.type())) throw new SecurityException("Lua scene command is not allowed: " + event.type());
            commands.execute(new LuaSceneCommand(event.type(), event.payload()));
        });
    }

    @Override public void setup()
    {
        super.setup();
        if (instance != null) return;
        instance = runtime.createInstance(module, context);
        instance.setup();
    }

    @Override public void update(long deltaMillis)
    {
        if (instance != null) instance.update(deltaMillis / 1_000.0);
        super.update(deltaMillis);
    }

    @Override public void fixedUpdate(float deltaSeconds)
    {
        if (instance != null) instance.fixedUpdate(deltaSeconds);
        super.fixedUpdate(deltaSeconds);
    }

    public void onEvent(ScriptEvent event)
    {
        if (instance == null) throw new IllegalStateException("Lua scene has not been set up");
        instance.onEvent(event);
    }

    @Override public void dispose()
    {
        ScriptInstance closing = instance;
        instance = null;
        RuntimeException failure = null;
        if (closing != null) try { closing.dispose(); } catch (RuntimeException exception) { failure = exception; }
        try { super.dispose(); }
        catch (RuntimeException exception)
        {
            if (failure == null) failure = exception;
            else failure.addSuppressed(exception);
        }
        if (failure != null) throw failure;
    }

    @Override public String getName() { return "luaScene"; }
}
