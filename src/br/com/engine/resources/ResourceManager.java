package br.com.engine.resources;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Properties;

import com.google.gson.JsonObject;

import br.com.engine.audio.AudioClip;
import br.com.engine.graphics.Font;
import br.com.engine.graphics.Image;

/** Typed resource facade. Exact paths are relative to res/ and include their extension. */
public final class ResourceManager
{
    private static final Map<ResourceRef, Image> PACK_IMAGES = new ConcurrentHashMap<>();
    private static final Map<PackFontKey, Font> PACK_FONTS = new ConcurrentHashMap<>();
    private static final Map<ResourceRef, TmxMapData> PACK_MAPS = new ConcurrentHashMap<>();
    private record PackFontKey(ResourceRef reference, float pixelSize) { }

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

    /** Loads an image with a cache key that includes the content-pack identity. */
    public static Image image(ResourceResolver resolver, ResourceRef reference)
    {
        require(resolver, reference);
        return PACK_IMAGES.computeIfAbsent(reference, ignored -> {
            try { return ContentLoader.decodeImage(resolver.read(reference, Integer.MAX_VALUE), reference.toString()); }
            catch (IOException exception) { throw new ResourceLoadException(reference.toString(), exception); }
        });
    }

    /** Loads an immutable font with a cache key that includes its content pack and size. */
    public static Font font(ResourceResolver resolver, ResourceRef reference, float pixelSize)
    {
        require(resolver, reference);
        if (!Float.isFinite(pixelSize) || pixelSize <= 0) throw new IllegalArgumentException("Font size must be finite and positive");
        PackFontKey key = new PackFontKey(reference, pixelSize);
        return PACK_FONTS.computeIfAbsent(key, ignored -> {
            try { return new Font(reference.toString(), pixelSize, resolver.read(reference, Integer.MAX_VALUE)); }
            catch (IOException exception) { throw new ResourceLoadException(reference.toString(), exception); }
        });
    }

    /** Creates a scene-owned audio clip from a resource that belongs to this pack. */
    public static AudioClip audio(ResourceResolver resolver, ResourceRef reference)
    {
        require(resolver, reference);
        try
        {
            InputStream input = resolver.open(reference);
            return new AudioClip(reference.toString(), input);
        }
        catch (IOException exception) { throw new ResourceLoadException(reference.toString(), exception); }
    }

    /** Parses and caches a map using the pack-qualified reference as its key. */
    public static TmxMapData map(ResourceResolver resolver, ResourceRef reference)
    {
        require(resolver, reference);
        return PACK_MAPS.computeIfAbsent(reference, ignored -> {
            try (InputStream input = resolver.open(reference)) { return TmxParser.parse(input); }
            catch (IOException exception) { throw new ResourceLoadException(reference.toString(), exception); }
        });
    }

    private static void require(ResourceResolver resolver, ResourceRef reference)
    {
        if (resolver == null) throw new IllegalArgumentException("Resource resolver is required");
        resolver.requireOwned(reference);
    }

    static void clearPackCaches()
    {
        PACK_IMAGES.clear();
        PACK_FONTS.clear();
        PACK_MAPS.clear();
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
        return ConfigurationsParser.parse(config);
    }
}
