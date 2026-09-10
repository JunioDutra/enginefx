package br.com.engine.resources;

import br.com.engine.core.Scene;
import br.com.engine.core.SceneJs;

public class ScenesDefinition
{
    private String scene;
    private String type;
    private String title;
    private Boolean menu;

    public ScenesDefinition(String scene, String type) { this.scene = scene; this.type = type; }

    public Scene getNewScene()
    {
        if (scene == null || scene.isBlank()) throw new IllegalArgumentException("Scene resource/class is required");
        try
        {
            if ("js".equalsIgnoreCase(type)) return new SceneJs(new String[] {scene});
            if (!"java".equalsIgnoreCase(type)) throw new IllegalArgumentException("Unsupported scene type: " + type);
            return Class.forName(scene).asSubclass(Scene.class).getDeclaredConstructor().newInstance();
        }
        catch (ReflectiveOperationException | IllegalArgumentException exception)
        {
            throw new IllegalStateException("Cannot create scene: " + scene, exception);
        }
    }

    public String getScene() { return scene; }
    public void setScene(String scene) { this.scene = scene; }
    public String getTitle() { return title == null || title.isBlank() ? scene : title; }
    public void setTitle(String title) { this.title = title; }
    public boolean isMenu() { return !Boolean.FALSE.equals(menu); }
    public void setMenu(Boolean menu) { this.menu = menu; }
}
