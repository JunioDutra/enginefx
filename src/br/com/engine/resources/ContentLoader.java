package br.com.engine.resources;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryUtil;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import br.com.engine.audio.AudioClip;
import br.com.engine.graphics.Font;
import br.com.engine.graphics.Image;

/** One resolver for exact paths relative to res/, in development and packaged games. */
public final class ContentLoader
{
    private static final List<Path> RESOURCE_ROOTS = List.of(
        Path.of("res"), Path.of("src/main/resources"), Path.of("target/classes/res"));
    private static final Map<String, Image> IMAGES = new ConcurrentHashMap<>();
    private record FontKey(String source, float size) { }
    private static final Map<FontKey, Font> FONTS = new ConcurrentHashMap<>();

    private ContentLoader() { }

    static String resourcePath(String name)
    {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Resource path is required");
        String normalized = name.replace('\\', '/');
        if (normalized.startsWith("/") || normalized.contains(":"))
            throw new IllegalArgumentException("Resource path must be relative: " + name);
        for (String part : normalized.split("/", -1))
            if (part.isEmpty() || part.equals(".") || part.equals(".."))
                throw new IllegalArgumentException("Invalid resource path: " + name);
        if (!Path.of(normalized).getFileName().toString().contains("."))
            throw new IllegalArgumentException("Resource extension is required: " + name);
        return normalized;
    }

    private static URL resolve(String name) throws IOException
    {
        String relative = resourcePath(name);
        for (Path root : RESOURCE_ROOTS)
        {
            Path file = root.resolve(relative);
            if (Files.isRegularFile(file)) return file.toAbsolutePath().normalize().toUri().toURL();
        }
        URL packaged = ContentLoader.class.getResource("/res/" + relative);
        if (packaged != null) return packaged;
        throw new FileNotFoundException("Resource not found: " + relative);
    }

    public static InputStream openResource(String name) throws IOException
    {
        return resolve(name).openStream();
    }

    public static Object loadContent(String name)
    {
        return loadContent(name, Map.of());
    }

    public static Object loadContent(String name, Map<String, Object> data)
    {
        String resource = resourcePath(name);
        Map<String, Object> bindings = data == null ? Map.of() : data;
        try
        {
            URL source = resolve(resource);
            String extension = resource.substring(resource.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
            if (List.of("png", "jpg", "jpeg", "gif").contains(extension))
                return IMAGES.computeIfAbsent(source.toExternalForm(), ignored -> readImage(source, resource));
            if (extension.equals("ttf"))
            {
                if (!(bindings.get("size") instanceof Number size))
                    throw new IllegalArgumentException("Font size is required");
                return FONTS.computeIfAbsent(new FontKey(source.toExternalForm(), size.floatValue()),
                    key -> readFont(source, key, resource));
            }
            try (InputStream input = source.openStream())
            {
                return switch (extension)
                {
                    case "wav", "mp3" -> new AudioClip(resource, input);
                    case "properties" -> {
                        Properties properties = new Properties();
                        properties.load(new InputStreamReader(input, StandardCharsets.UTF_8));
                        yield properties;
                    }
                    case "json" -> new Gson().fromJson(new InputStreamReader(input, StandardCharsets.UTF_8), JsonObject.class);
                    case "xml" -> new String(input.readAllBytes(), StandardCharsets.UTF_8);
                    case "tmx" -> TmxParser.parse(input);
                    case "js" -> {
                        ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
                        if (engine == null) throw new IllegalStateException("Nashorn provider missing: preserve META-INF/services");
                        bindings.forEach(engine::put);
                        engine.eval(new InputStreamReader(input, StandardCharsets.UTF_8));
                        yield engine;
                    }
                    default -> throw new IllegalArgumentException("Unsupported resource format: " + extension);
                };
            }
        }
        catch (ResourceLoadException exception) { throw exception; }
        catch (Exception exception) { throw new ResourceLoadException(resource, exception); }
    }

    private static Image readImage(URL source, String resource)
    {
        try (InputStream input = source.openStream())
        {
            return decodeImage(input.readAllBytes(), resource);
        }
        catch (IOException exception) { throw new ResourceLoadException(resource, exception); }
    }

    static Image decodeImage(byte[] encoded, String resource)
    {
        ByteBuffer input = MemoryUtil.memAlloc(encoded.length);
        try
        {
            input.put(encoded).flip();
            int[] width = new int[1], height = new int[1], channels = new int[1];
            ByteBuffer pixels = STBImage.stbi_load_from_memory(input, width, height, channels, 4);
            if (pixels == null) throw new ResourceLoadException(resource,
                new IOException("Invalid image: " + STBImage.stbi_failure_reason()));
            try
            {
                byte[] rgba = new byte[Math.multiplyExact(Math.multiplyExact(width[0], height[0]), 4)];
                // Absolute read keeps the native pointer at the allocation's start for stbi_image_free.
                pixels.get(0, rgba);
                return new Image(width[0], height[0], rgba);
            }
            finally { STBImage.stbi_image_free(pixels); }
        }
        finally { MemoryUtil.memFree(input); }
    }

    private static Font readFont(URL source, FontKey key, String resource)
    {
        try (InputStream input = source.openStream())
        {
            return new Font(key.source(), key.size(), input.readAllBytes());
        }
        catch (IOException exception) { throw new ResourceLoadException(resource, exception); }
    }

    /** Releases Java-side asset references when the runtime shuts down. */
    public static void clearCaches()
    {
        IMAGES.clear();
        FONTS.clear();
    }
}
