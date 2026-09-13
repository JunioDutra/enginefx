package br.com.engine.resources;

/** Signals a configuration that still selects the removed JavaScript runtime. */
public final class ScriptTypeRemovedException extends IllegalStateException
{
    public static final String CODE = "SCRIPT_TYPE_REMOVED";

    public ScriptTypeRemovedException(String resource)
    {
        super(CODE + ": JavaScript scene '" + resource
            + "' is no longer supported. Migrate it to a Lua module and register its Java scene factory.");
    }

    public String code() { return CODE; }
}
