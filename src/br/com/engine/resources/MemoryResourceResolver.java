package br.com.engine.resources;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Immutable in-memory pack, useful for tests and generated built-in content. */
public final class MemoryResourceResolver implements ResourceResolver
{
    private final String packId;
    private final String packHash;
    private final Map<String, byte[]> resources;

    public MemoryResourceResolver(String packId, String packHash, Map<String, byte[]> resources)
    {
        this.packId = new ResourceRef(packId, packHash, "placeholder.bin").packId();
        this.packHash = new ResourceRef(packId, packHash, "placeholder.bin").packHash();
        Objects.requireNonNull(resources, "resources");
        Map<String, byte[]> copy = new LinkedHashMap<>();
        resources.forEach((path, bytes) -> {
            String normalized = ResourceRef.normalizePath(path);
            if (bytes == null) throw new IllegalArgumentException("Resource bytes are required: " + normalized);
            if (copy.putIfAbsent(normalized, bytes.clone()) != null)
                throw new IllegalArgumentException("Duplicate normalized resource path: " + normalized);
        });
        this.resources = Map.copyOf(copy);
    }

    @Override public String packId() { return packId; }
    @Override public String packHash() { return packHash; }

    @Override public InputStream open(ResourceRef reference) throws IOException
    {
        requireOwned(reference);
        byte[] bytes = resources.get(reference.path());
        if (bytes == null) throw new IOException("Resource not found: " + reference);
        return new ByteArrayInputStream(bytes);
    }
}
