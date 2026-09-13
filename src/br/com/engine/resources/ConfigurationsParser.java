package br.com.engine.resources;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/** Explicit JSON adapter for application.json; no model reflection is used. */
final class ConfigurationsParser
{
    private ConfigurationsParser() { }

    static Configurations parse(JsonObject root)
    {
        if (root == null) throw new IllegalArgumentException("Configuration JSON is required");
        Configurations result = new Configurations();
        result.setSizeW(positiveInt(root, "sizeW"));
        result.setSizeH(positiveInt(root, "sizeH"));
        result.setDebugMode(optionalBoolean(root, "debugMode"));
        result.setTitle(optionalString(root, "title"));
        result.setBootScene(optionalString(root, "bootScene"));
        result.setScenes(scenes(root));
        return result;
    }

    private static List<ScenesDefinition> scenes(JsonObject root)
    {
        JsonElement element = root.get("scenes");
        if (element == null || !element.isJsonArray()) throw new IllegalArgumentException("Configuration requires a scenes array");
        JsonArray array = element.getAsJsonArray();
        List<ScenesDefinition> result = new ArrayList<>();
        for (int index = 0; index < array.size(); index++)
        {
            if (!array.get(index).isJsonObject()) throw new IllegalArgumentException("Scene at index " + index + " must be an object");
            JsonObject object = array.get(index).getAsJsonObject();
            String scene = requiredString(object, "scene", "Scene at index " + index + " requires an id");
            String type = optionalString(object, "type");
            ScenesDefinition definition = new ScenesDefinition(scene, type);
            definition.setTitle(optionalString(object, "title"));
            definition.setMenu(optionalBoolean(object, "menu"));
            result.add(definition);
        }
        return List.copyOf(result);
    }

    private static int positiveInt(JsonObject object, String property)
    {
        JsonElement element = object.get(property);
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber())
            throw new IllegalArgumentException("Configuration requires numeric " + property);
        try
        {
            int value = new BigDecimal(element.getAsString()).intValueExact();
            if (value <= 0) throw new IllegalArgumentException("Configuration requires positive " + property);
            return value;
        }
        catch (NumberFormatException | ArithmeticException exception)
        {
            throw new IllegalArgumentException("Configuration requires an integer " + property, exception);
        }
    }

    private static String requiredString(JsonObject object, String property, String message)
    {
        String value = optionalString(object, property);
        if (value == null || value.isBlank()) throw new IllegalArgumentException(message);
        return value;
    }

    private static String optionalString(JsonObject object, String property)
    {
        JsonElement element = object.get(property);
        if (element == null || element.isJsonNull()) return null;
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString())
            throw new IllegalArgumentException("Configuration property " + property + " must be a string");
        return element.getAsString();
    }

    private static Boolean optionalBoolean(JsonObject object, String property)
    {
        JsonElement element = object.get(property);
        if (element == null || element.isJsonNull()) return null;
        JsonPrimitive primitive = element.isJsonPrimitive() ? element.getAsJsonPrimitive() : null;
        if (primitive == null || !primitive.isBoolean())
            throw new IllegalArgumentException("Configuration property " + property + " must be a boolean");
        return primitive.getAsBoolean();
    }
}
