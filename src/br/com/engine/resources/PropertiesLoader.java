package br.com.engine.resources;

import java.util.Map;

/** Compatibility facade; use ResourceManager.properties with an explicit resource path. */
@Deprecated
public class PropertiesLoader
{
    public Map<String, String> load() { return ResourceManager.properties("mensages/mensages.properties"); }
}
