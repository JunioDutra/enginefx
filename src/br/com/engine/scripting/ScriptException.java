package br.com.engine.scripting;

/** Base exception carrying a deterministic script-boundary failure. */
public class ScriptException extends RuntimeException
{
    public ScriptException(String message) { super(message); }
    public ScriptException(String message, Throwable cause) { super(message, cause); }
}
