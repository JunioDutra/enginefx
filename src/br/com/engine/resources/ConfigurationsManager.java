package br.com.engine.resources;

/** Compatibility facade over application.json/config.json. */
@Deprecated
public final class ConfigurationsManager
{
    private static final ConfigurationsManager INSTANCE = new ConfigurationsManager();
    private final Configurations configuration = ResourceManager.configurations();
    private ConfigurationsManager() { }
    public static ConfigurationsManager getInstance() { return INSTANCE; }
    public Integer getWidth() { return configuration.getSizeW(); }
    public Integer getHeight() { return configuration.getSizeH(); }
    public boolean getDebugMode() { return Boolean.TRUE.equals(configuration.isDebugMode()); }
}
