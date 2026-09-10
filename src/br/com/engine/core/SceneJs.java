package br.com.engine.core;

import java.util.HashMap;
import java.util.Map;
import javax.script.Invocable;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import br.com.engine.componentes.builders.ScriptBuilder;
import br.com.engine.componentes.drawable.Sprite;
import br.com.engine.resources.ResourceManager;

/** Scene object definitions and scripts use exact paths relative to res/, with extensions. */
public class SceneJs extends Scene
{
    private final String[] gameObjectResources;
    public SceneJs(String[] resources) { gameObjectResources = resources.clone(); }

    @Override public void setup()
    {
        super.setup();
        for (String resource : gameObjectResources)
        {
            GameObject object = new GameObject();
            try
            {
                JsonObject definition = ResourceManager.json(resource);
                JsonObject position = definition.getAsJsonObject("position");
                object.getPosition().setPosition(position.get("x").getAsFloat(), position.get("y").getAsFloat());
                for (JsonElement item : definition.getAsJsonArray("components"))
                {
                    JsonObject component = item.getAsJsonObject();
                    if (!"Sprite".equalsIgnoreCase(component.get("type").getAsString()))
                        throw new IllegalArgumentException("Unsupported scripted component: " + component.get("type"));
                    object.addComponente(new Sprite(component.get("name").getAsString()));
                }
                Map<String, Object> bindings = new HashMap<>();
                bindings.put("gameObject", object);
                bindings.put("screen", ControleBase.getInstance().getScreen());
                bindings.put("spriteClass", Sprite.class);
                for (JsonElement item : definition.getAsJsonArray("scripts"))
                {
                    String script = item.getAsString();
                    Invocable invocable = ResourceManager.script(script, bindings);
                    object.addComponente(ScriptBuilder.create(time -> {
                        try { invocable.invokeFunction("update", time); }
                        catch (Exception exception) { throw new IllegalStateException("Cannot update script: " + script, exception); }
                    }));
                }
                add(object);
            }
            catch (RuntimeException exception)
            {
                try { object.dispose(); } catch (RuntimeException cleanup) { exception.addSuppressed(cleanup); }
                throw new IllegalStateException("Cannot load scene object: " + resource, exception);
            }
        }
    }

    @Override public String getName() { return "jsScene"; }
}
