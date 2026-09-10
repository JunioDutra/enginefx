package br.com.engine.resources;

import java.io.FileNotFoundException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import javax.script.Invocable;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import br.com.engine.audio.AudioClip;
import br.com.engine.graphics.Font;
import br.com.engine.graphics.Image;

/** Typed resource facade. Exact paths are relative to res/ and include their extension. */
public final class ResourceManager
{
    private ResourceManager() { }

    private static <T> T load(String path, Class<T> type, Map<String, Object> data)
    {
        try { return type.cast(ContentLoader.loadContent(path, data)); }
        catch (ClassCastException exception) { throw new ResourceLoadException(path, exception); }
    }

    public static Image image(String path) { return load(path, Image.class, Map.of()); }
    public static AudioClip audio(String path) { return load(path, AudioClip.class, Map.of()); }
    public static Font font(String path, float pixelSize) { return load(path, Font.class, Map.of("size", pixelSize)); }
    public static TmxMapData map(String path) { return load(path, TmxMapData.class, Map.of()); }
    public static JsonObject json(String path) { return load(path, JsonObject.class, Map.of()); }

    public static Map<String, String> properties(String path)
    {
        Properties properties = load(path, Properties.class, Map.of());
        Map<String, String> result = new LinkedHashMap<>();
        properties.forEach((key, value) -> result.put(String.valueOf(key), String.valueOf(value)));
        return Map.copyOf(result);
    }

    public static Invocable script(String path, Map<String, Object> bindings)
    {
        return load(path, Invocable.class, bindings == null ? Map.of() : bindings);
    }

    public static Configurations configurations()
    {
        JsonObject config;
        try { config = json("config.json"); }
        catch (ResourceLoadException exception)
        {
            if (!(exception.getCause() instanceof FileNotFoundException)) throw exception;
            config = json("application.json");
        }
        Configurations result = new Gson().fromJson(config, Configurations.class);
        if (result == null || result.getScenes() == null || result.getSizeW() == null ||
            result.getSizeH() == null || result.getSizeW() <= 0 || result.getSizeH() <= 0)
            throw new IllegalArgumentException("Configuration requires scenes and positive sizeW/sizeH");
        return result;
    }
}
