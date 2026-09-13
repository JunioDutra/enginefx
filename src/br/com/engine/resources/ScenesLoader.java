package br.com.engine.resources;

import java.util.List;
import br.com.engine.core.Scene;
import br.com.engine.core.SceneRegistry;

/** Compatibility facade over the same configuration used by the runtime. */
@Deprecated
public class ScenesLoader
{
    public List<Scene> load(SceneRegistry registry)
    {
        if (registry == null) throw new IllegalArgumentException("Scene registry is required");
        return ResourceManager.configurations().getScenes().stream().map(definition -> registry.create(definition.getScene())).toList();
    }
}
