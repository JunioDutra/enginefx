package br.com.engine.scripting;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Registry separating engine-owned APIs from game-owned namespaces. */
public final class ScriptApiRegistry
{
    private final Map<String, ScriptApiDefinition> definitions = new LinkedHashMap<>();
    private static final java.util.Set<String> RESERVED_ROOTS = java.util.Set.of("assert", "collectgarbage", "dofile",
        "error", "getmetatable", "ipairs", "load", "loadfile", "next", "pairs", "pcall", "print", "rawequal",
        "rawget", "rawlen", "rawset", "select", "setmetatable", "tonumber", "tostring", "type", "warn", "xpcall",
        "coroutine", "debug", "io", "java", "math", "os", "package", "require", "string", "table", "utf8");

    public ScriptApiRegistry register(String namespace, String name, String capability, ScriptApi implementation)
    {
        if ("engine".equals(namespace) || (namespace != null && namespace.startsWith("engine.")))
            throw new IllegalArgumentException("Only EngineFX may register the engine namespace");
        return registerInternal(namespace, name, capability, implementation);
    }

    public ScriptApiRegistry registerEngine(String name, String capability, ScriptApi implementation)
    {
        return registerInternal("engine", name, capability, implementation);
    }

    private ScriptApiRegistry registerInternal(String namespace, String name, String capability, ScriptApi implementation)
    {
        ScriptApiDefinition definition = new ScriptApiDefinition(namespace, name, capability, implementation);
        if (RESERVED_ROOTS.contains(namespace.split("\\.")[0]))
            throw new IllegalArgumentException("Script API namespace shadows a Lua global: " + namespace);
        for (String existing : definitions.keySet())
            if (existing.startsWith(definition.qualifiedName() + ".") || definition.qualifiedName().startsWith(existing + "."))
                throw new IllegalArgumentException("Script API conflicts with a namespace: " + definition.qualifiedName());
        if (definitions.putIfAbsent(definition.qualifiedName(), definition) != null)
            throw new IllegalArgumentException("Duplicate script API: " + definition.qualifiedName());
        return this;
    }

    public List<ScriptApiDefinition> definitions() { return List.copyOf(definitions.values()); }

    public ScriptValue invoke(String namespace, String name, ScriptContext context, List<ScriptValue> arguments)
    {
        ScriptApiDefinition definition = definitions.get(namespace + "." + name);
        if (definition == null) throw new IllegalArgumentException("Unknown script API: " + namespace + "." + name);
        if (definition.capability() != null && !context.allows(definition.capability()))
            throw new SecurityException("Missing script capability: " + definition.capability());
        ScriptValue result = definition.implementation().invoke(context, List.copyOf(arguments));
        return result == null ? ScriptValue.of(null) : result;
    }

    ScriptApiRegistry copy()
    {
        ScriptApiRegistry result = new ScriptApiRegistry();
        for (ScriptApiDefinition definition : definitions.values())
            result.registerInternal(definition.namespace(), definition.name(), definition.capability(), definition.implementation());
        return result;
    }

    void addAll(ScriptApiRegistry other)
    {
        for (ScriptApiDefinition definition : other.definitions())
            registerInternal(definition.namespace(), definition.name(), definition.capability(), definition.implementation());
    }
}
