package br.com.engine.platform.lwjgl;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import br.com.engine.graphics.Font;
import br.com.engine.resources.ResourceManager;

class FontMetricsTest
{
    @Test void measuredWidthsMatchBakedAdvancesAndMultilineFallback()
    {
        Font font = ResourceManager.font("fonts/test.ttf", 24);
        assertSame(font, ResourceManager.font("fonts/test.ttf", 24));
        try (var cache = new LwjglVulkanFontCache())
        {
            var baked = cache.get(font);
            float width = baked.charData.get('W' - Font.FIRST_CODE_POINT).xadvance()
                + baked.charData.get('i' - Font.FIRST_CODE_POINT).xadvance();
            assertEquals(Math.round(width), font.getStringWidth("Wi"));
            assertTrue(font.getStringWidth("WWW") > font.getStringWidth("iii"));
            assertEquals(Math.max(font.getStringWidth("Wi"), font.getStringWidth("i")), font.getStringWidth("Wi\ni"));
            assertEquals(font.getStringWidth("?"), font.getStringWidth("\uD83D\uDE00"));
            assertEquals(Math.round(baked.ascent - baked.descent), font.getHeight());
        }
    }

    @Test void rejectsNonFiniteFontSizes()
    {
        assertThrows(IllegalArgumentException.class, () -> new Font("font.ttf", Float.NaN, new byte[12]));
        assertThrows(IllegalArgumentException.class, () -> new Font("font.ttf", Float.POSITIVE_INFINITY, new byte[12]));
    }
}
