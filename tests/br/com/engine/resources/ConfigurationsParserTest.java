package br.com.engine.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonParser;

class ConfigurationsParserTest
{
    @Test void buildsConfigurationThroughTheExplicitAdapter()
    {
        Configurations configuration = ConfigurationsParser.parse(JsonParser.parseString("""
            {"title":"Native ready","debugMode":false,"sizeW":960,"sizeH":640,
             "bootScene":"game:menu","scenes":[
               {"scene":"game:menu","title":"Menu","menu":true},
               {"scene":"game:hidden","menu":false,"type":"java"}]}
            """).getAsJsonObject());
        assertEquals("Native ready", configuration.getTitle());
        assertEquals("game:menu", configuration.getBootScene());
        assertEquals(2, configuration.getScenes().size());
        assertFalse(configuration.getScenes().get(1).isMenu());
    }

    @Test void rejectsLooseOrRemovedConfigurationShapes()
    {
        assertThrows(IllegalArgumentException.class, () -> ConfigurationsParser.parse(JsonParser.parseString(
            "{\"sizeW\":1.5,\"sizeH\":640,\"scenes\":[]}").getAsJsonObject()));
        assertThrows(ScriptTypeRemovedException.class, () -> ConfigurationsParser.parse(JsonParser.parseString(
            "{\"sizeW\":960,\"sizeH\":640,\"scenes\":[{\"scene\":\"old\",\"type\":\"js\"}]}").getAsJsonObject()));
    }
}
