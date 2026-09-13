package br.com.engine.resources;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import br.com.engine.audio.AudioClip;
import br.com.engine.graphics.Font;
import br.com.engine.graphics.Image;

/** A resolver scoped to exactly one content pack. */
public interface ResourceResolver
{
    String packId();
    String packHash();
    InputStream open(ResourceRef reference) throws IOException;

    default ResourceRef ref(String path) { return new ResourceRef(packId(), packHash(), path); }

    default boolean owns(ResourceRef reference)
    {
        return reference != null && packId().equals(reference.packId()) && packHash().equals(reference.packHash());
    }

    default byte[] read(ResourceRef reference, int maximumBytes) throws IOException
    {
        if (maximumBytes < 0) throw new IllegalArgumentException("Maximum size must be non-negative");
        requireOwned(reference);
        try (InputStream input = open(reference))
        {
            int requested = maximumBytes == Integer.MAX_VALUE ? Integer.MAX_VALUE : maximumBytes + 1;
            byte[] bytes = input.readNBytes(requested);
            if (maximumBytes != Integer.MAX_VALUE && bytes.length > maximumBytes)
                throw new IllegalArgumentException("Resource exceeds " + maximumBytes + " bytes: " + reference);
            return bytes;
        }
    }

    default String readUtf8(ResourceRef reference, int maximumBytes) throws IOException
    {
        return new String(read(reference, maximumBytes), StandardCharsets.UTF_8);
    }

    default Image image(ResourceRef reference) { return ResourceManager.image(this, reference); }
    default Font font(ResourceRef reference, float pixelSize) { return ResourceManager.font(this, reference, pixelSize); }
    default AudioClip audio(ResourceRef reference) { return ResourceManager.audio(this, reference); }
    default TmxMapData map(ResourceRef reference) { return ResourceManager.map(this, reference); }

    default void requireOwned(ResourceRef reference)
    {
        if (!owns(reference)) throw new IllegalArgumentException("Resource belongs to another pack: " + reference);
    }
}
