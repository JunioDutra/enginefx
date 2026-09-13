package br.com.engine.resources;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * Read-only resolver for resources packaged with the application. It never
 * exposes a filesystem path, so the same script is used from an IDE and JAR.
 */
public final class ClasspathResourceResolver implements ResourceResolver
{
    private final String packId;
    private final String packHash;
    private final String root;
    private final ClassLoader loader;

    public ClasspathResourceResolver(String packId, String packHash, String resourceRoot)
    {
        this(packId, packHash, resourceRoot, Thread.currentThread().getContextClassLoader());
    }

    public ClasspathResourceResolver(String packId, String packHash, String resourceRoot, ClassLoader loader)
    {
        ResourceRef identity = new ResourceRef(packId, packHash, "placeholder.bin");
        this.packId = identity.packId();
        this.packHash = identity.packHash();
        String candidate = Objects.requireNonNull(resourceRoot, "resourceRoot").replace('\\', '/');
        while (candidate.endsWith("/")) candidate = candidate.substring(0, candidate.length() - 1);
        String normalized = ResourceRef.normalizePath(candidate + "/placeholder.bin");
        this.root = normalized.substring(0, normalized.length() - "/placeholder.bin".length());
        this.loader = Objects.requireNonNullElse(loader, ClasspathResourceResolver.class.getClassLoader());
    }

    @Override public String packId() { return packId; }
    @Override public String packHash() { return packHash; }

    @Override public InputStream open(ResourceRef reference) throws IOException
    {
        requireOwned(reference);
        InputStream stream = loader.getResourceAsStream(root + "/" + reference.path());
        if (stream == null) throw new IOException("Resource not found on classpath: " + reference);
        return stream;
    }
}
