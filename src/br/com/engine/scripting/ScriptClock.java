package br.com.engine.scripting;

import java.time.Instant;

@FunctionalInterface
public interface ScriptClock
{
    Instant now();
}
