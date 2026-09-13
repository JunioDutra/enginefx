package br.com.engine.resources;

import br.com.engine.core.Scene;
import br.com.engine.core.SceneRegistry;

public class ScenesDefinition
{
    private String scene;
    private String type;
    private String title;
    private Boolean menu;

    public ScenesDefinition(String scene, String type) { this.scene = scene; this.type = type; }

    /** @deprecated Register scene factories and use {@link #getNewScene(SceneRegistry)}. */
    @Deprecated(since = "2.2", forRemoval = true)
    public Scene getNewScene()
    {
        return getNewScene(null);
    }

    /**
     * Prefer an explicit registered id. The reflection branch remains only for
     * old configuration files and is scheduled for removal in the Native Image
     * contract step. JavaScript scene resources were removed in EngineFX 3.0.
     */
    public Scene getNewScene(SceneRegistry registry)
    {
        if (scene == null || scene.isBlank()) throw new IllegalArgumentException("Scene resource/class is required");
        if ("js".equalsIgnoreCase(type)) throw new ScriptTypeRemovedException(scene);
        if (registry != null && registry.contains(scene)) return registry.create(scene);
        try
        {
            if (!"java".equalsIgnoreCase(type)) throw new IllegalArgumentException("Unsupported scene type: " + type);
            return Class.forName(scene).asSubclass(Scene.class).getDeclaredConstructor().newInstance();
        }
        catch (ReflectiveOperationException | IllegalArgumentException exception)
        {
            throw new IllegalStateException("Cannot create scene: " + scene, exception);
        }
    }

    public String getScene() { return scene; }
    /** Legacy annotation lookup without constructing or initializing an unused scene. */
    public boolean isLegacyBootable(SceneRegistry registry)
    {
        if (registry != null && registry.contains(scene)) return false;
        if ("js".equalsIgnoreCase(type)) throw new ScriptTypeRemovedException(scene);
        if (!"java".equalsIgnoreCase(type)) throw new IllegalArgumentException("Unsupported scene type: " + type);
        try
        {
            return Class.forName(scene, false, ScenesDefinition.class.getClassLoader()).asSubclass(Scene.class)
                .isAnnotationPresent(br.com.engine.core.annotation.Bootable.class);
        }
        catch (ClassNotFoundException exception) { throw new IllegalStateException("Cannot inspect scene: " + scene, exception); }
    }
    public void setScene(String scene) { this.scene = scene; }
    public String getTitle() { return title == null || title.isBlank() ? scene : title; }
    public void setTitle(String title) { this.title = title; }
    public boolean isMenu() { return !Boolean.FALSE.equals(menu); }
    public void setMenu(Boolean menu) { this.menu = menu; }
}
