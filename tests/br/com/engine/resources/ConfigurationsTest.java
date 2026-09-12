package br.com.engine.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ConfigurationsTest
{
    @Test void keepsTheHistoricalTitleWhenApplicationHasNoTitle()
    {
        Configurations configurations = new Configurations();
        assertEquals("Enginefx Vulkan", configurations.getTitle());
    }

    @Test void exposesTheConfiguredTitle()
    {
        Configurations configurations = new Configurations();
        configurations.setTitle("FarmFX");
        assertEquals("FarmFX", configurations.getTitle());
    }
}
