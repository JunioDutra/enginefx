package br.com.engine.resources;

import java.util.List;
import br.com.engine.core.Scene;

/** Compatibility facade over the same configuration used by the runtime. */
@Deprecated
public class ScenesLoader
{
    public List<Scene> load()
    {
        return ResourceManager.configurations().getScenes().stream().map(ScenesDefinition::getNewScene).toList();
    }
}
