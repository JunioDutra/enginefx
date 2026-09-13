package br.com.engine.scripting;

/** One named, capability-gated function visible from Lua. */
public record ScriptApiDefinition(String namespace, String name, String capability, ScriptApi implementation)
{
    private static final java.util.Set<String> KEYWORDS = java.util.Set.of("and", "break", "do", "else", "elseif",
        "end", "false", "for", "function", "goto", "if", "in", "local", "nil", "not", "or", "repeat", "return",
        "then", "true", "until", "while");
    public ScriptApiDefinition
    {
        if (!validPath(namespace) || !validPath(name)) throw new IllegalArgumentException("Invalid script API name");
        if (capability != null && !capability.matches("[a-z][a-z0-9.*_-]{0,127}"))
            throw new IllegalArgumentException("Invalid script API capability");
        if (implementation == null) throw new IllegalArgumentException("Script API implementation is required");
    }

    public String qualifiedName() { return namespace + "." + name; }

    static boolean validPath(String value)
    {
        return value != null && value.length() <= 128 && value.matches("[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)*")
            && java.util.Arrays.stream(value.split("\\.")).noneMatch(KEYWORDS::contains);
    }
}
