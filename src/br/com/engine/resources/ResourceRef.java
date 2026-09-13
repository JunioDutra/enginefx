package br.com.engine.resources;

import java.util.Objects;

/**
 * Identifies one resource owned by a content pack. The identity deliberately
 * includes the pack hash, so two mods can safely use the same relative path.
 */
public record ResourceRef(String packId, String packHash, String path)
{
    public ResourceRef
    {
        packId = requirePart(packId, "Pack id");
        packHash = requirePart(packHash, "Pack hash");
        path = normalizePath(path);
    }

    static String normalizePath(String candidate)
    {
        if (candidate == null || candidate.isBlank()) throw new IllegalArgumentException("Resource path is required");
        String normalized = candidate.replace('\\', '/');
        if (normalized.startsWith("/") || normalized.contains(":"))
            throw new IllegalArgumentException("Resource path must be relative: " + candidate);
        for (String part : normalized.split("/", -1))
            if (part.isEmpty() || part.equals(".") || part.equals(".."))
                throw new IllegalArgumentException("Invalid resource path: " + candidate);
        return normalized;
    }

    private static String requirePart(String value, String label)
    {
        Objects.requireNonNull(value, label);
        String result = value.trim();
        if (result.isEmpty() || result.indexOf('/') >= 0 || result.indexOf('\\') >= 0
            || result.indexOf('@') >= 0 || result.indexOf(':') >= 0 || result.chars().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException(label + " is invalid: " + value);
        return result;
    }

    @Override public String toString() { return packId + "@" + packHash + ":" + path; }
}
