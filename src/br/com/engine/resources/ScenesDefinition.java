package br.com.engine.resources;

/** Explicit configuration for one scene id registered by the application. */
public class ScenesDefinition
{
    private String scene;
    private String title;
    private Boolean menu;

    public ScenesDefinition(String scene) { this(scene, null); }

    /**
     * Compatibility input for old JSON/test callers. The type is never kept or
     * used to create a class; only the removed JavaScript type is recognized.
     */
    public ScenesDefinition(String scene, String type)
    {
        this.scene = requireScene(scene);
        requireSupportedType(this.scene, type);
    }

    static void requireSupportedType(String scene, String type)
    {
        if (type == null || type.isBlank() || "java".equalsIgnoreCase(type)) return;
        if ("js".equalsIgnoreCase(type)) throw new ScriptTypeRemovedException(scene);
        throw new IllegalArgumentException("Unsupported scene type: " + type);
    }

    private static String requireScene(String scene)
    {
        if (scene == null || scene.isBlank()) throw new IllegalArgumentException("Scene id is required");
        return scene;
    }

    public String getScene() { return scene; }
    public void setScene(String scene) { this.scene = requireScene(scene); }
    public String getTitle() { return title == null || title.isBlank() ? scene : title; }
    public void setTitle(String title) { this.title = title; }
    public boolean isMenu() { return !Boolean.FALSE.equals(menu); }
    public void setMenu(Boolean menu) { this.menu = menu; }
}
