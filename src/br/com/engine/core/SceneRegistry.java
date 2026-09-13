package br.com.engine.core;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/** Explicit scene factories used by 2.2 bootstrap instead of class-name reflection. */
public final class SceneRegistry
{
    private final Map<String, Supplier<? extends Scene>> factories = new LinkedHashMap<>();
    private final Map<String, String> aliases = new LinkedHashMap<>();

    public SceneRegistry register(String id, Supplier<? extends Scene> factory)
    {
        String normalized = id(id);
        if (aliases.containsKey(normalized)) throw new IllegalArgumentException("Scene id is already an alias: " + normalized);
        if (factories.putIfAbsent(normalized, Objects.requireNonNull(factory, "factory")) != null)
            throw new IllegalArgumentException("Duplicate scene id: " + normalized);
        return this;
    }

    public SceneRegistry alias(String legacyId, String targetId)
    {
        String legacy = id(legacyId), target = id(targetId);
        if (!factories.containsKey(target)) throw new IllegalArgumentException("Unknown target scene: " + target);
        if (factories.containsKey(legacy) || aliases.putIfAbsent(legacy, target) != null)
            throw new IllegalArgumentException("Duplicate scene alias: " + legacy);
        return this;
    }

    public boolean contains(String id)
    {
        if (id == null) return false;
        return factories.containsKey(id) || aliases.containsKey(id);
    }

    public String resolve(String id)
    {
        String normalized = id(id);
        return aliases.getOrDefault(normalized, normalized);
    }

    public Scene create(String id)
    {
        String resolved = resolve(id);
        Supplier<? extends Scene> factory = factories.get(resolved);
        if (factory == null) throw new IllegalArgumentException("Unknown scene id: " + id);
        Scene scene = factory.get();
        if (scene == null) throw new IllegalStateException("Scene factory returned null: " + resolved);
        return scene;
    }

    private static String id(String candidate)
    {
        if (candidate == null || !candidate.matches("[A-Za-z0-9_.:-]{1,160}"))
            throw new IllegalArgumentException("Invalid scene id: " + candidate);
        return candidate;
    }
}
