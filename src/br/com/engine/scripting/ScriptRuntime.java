package br.com.engine.scripting;

import java.util.List;
import br.com.engine.resources.ResourceRef;
import br.com.engine.resources.ResourceResolver;
import br.com.engine.scripting.internal.LuaScriptRuntime;

/** Game-thread-confined runtime for loading Lua modules and isolated instances. */
public interface ScriptRuntime extends AutoCloseable
{
    ScriptModule loadModule(ResourceRef reference);
    ScriptModule loadModule(ResourceRef reference, List<ResourceRef> dependencies);
    ScriptInstance createInstance(ScriptModule module, ScriptContext context);
    boolean isClosed();
    @Override void close();

    static ScriptRuntime lua(ResourceResolver resolver, ScriptApiRegistry gameApis)
    {
        return lua(resolver, gameApis, ScriptLimits.DEFAULT);
    }

    static ScriptRuntime lua(ResourceResolver resolver, ScriptApiRegistry gameApis, ScriptLimits limits)
    {
        return new LuaScriptRuntime(resolver, gameApis, limits);
    }
}
