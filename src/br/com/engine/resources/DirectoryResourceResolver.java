package br.com.engine.resources;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Resolver for an installed pack. Real-path containment closes both lexical
 * traversal and symlink escapes before a resource is opened.
 */
public final class DirectoryResourceResolver implements ResourceResolver
{
    private final String packId;
    private final String packHash;
    private final Path root;

    public DirectoryResourceResolver(String packId, String packHash, Path root)
    {
        ResourceRef identity = new ResourceRef(packId, packHash, "placeholder.bin");
        this.packId = identity.packId();
        this.packHash = identity.packHash();
        this.root = Objects.requireNonNull(root, "root").toAbsolutePath().normalize();
    }

    @Override public String packId() { return packId; }
    @Override public String packHash() { return packHash; }

    @Override public InputStream open(ResourceRef reference) throws IOException
    {
        requireOwned(reference);
        Path rootReal = root.toRealPath();
        Path candidate = root.resolve(reference.path()).normalize();
        if (!candidate.startsWith(root)) throw new IOException("Resource escapes pack root: " + reference);
        Path real = candidate.toRealPath();
        if (!real.startsWith(rootReal)) throw new IOException("Resource symlink escapes pack root: " + reference);
        return Files.newInputStream(real);
    }
}
