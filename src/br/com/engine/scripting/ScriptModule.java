package br.com.engine.scripting;

import java.util.List;
import br.com.engine.resources.ResourceRef;

/** A validated Lua source module and its explicitly declared dependencies. */
public interface ScriptModule
{
    ResourceRef reference();
    List<ResourceRef> dependencies();
}
