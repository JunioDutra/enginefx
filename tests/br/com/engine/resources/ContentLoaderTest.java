package br.com.engine.resources;

import static org.junit.jupiter.api.Assertions.*;
import java.io.FileNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ContentLoaderTest
{
    @AfterEach void clearCache() { ContentLoader.clearCaches(); }

    @Test void sharesClasspathImagesAndPreservesPixels() throws Exception
    {
        var first = ResourceManager.image("nested/pixel.png");
        assertSame(first, ResourceManager.image("nested/pixel.png"));
        assertEquals(1, first.getWidth());
        assertEquals(1, first.getHeight());
        byte[] encoded;
        try (var stream = ContentLoader.openResource("nested/pixel.png")) { encoded = stream.readAllBytes(); }
        for (int i = 0; i < 200; i++)
            assertArrayEquals(first.copyRgba(), ContentLoader.decodeImage(encoded, "pixel.png").copyRgba());
    }

    @Test void resolvesExactPathsWithoutBasenameFallback()
    {
        assertEquals("nested", ResourceManager.json("nested/sample.json").get("source").getAsString());
        var missing = assertThrows(ResourceLoadException.class, () -> ResourceManager.json("sample.json"));
        assertInstanceOf(FileNotFoundException.class, missing.getCause());
        assertTrue(missing.getMessage().contains("sample.json"));
    }

    @Test void rejectsPathsOutsideResourceRoot()
    {
        for (String path : new String[] { "../pixel.png", "/pixel.png", "C:\\pixel.png", "nested/../pixel.png", "pixel" })
            assertThrows(IllegalArgumentException.class, () -> ResourceManager.image(path));
    }

    @Test void reportsInvalidMapAndImageWithTheirPath()
    {
        var image = assertThrows(ResourceLoadException.class, () -> ContentLoader.decodeImage(new byte[] {1,2,3}, "bad.png"));
        assertTrue(image.getMessage().contains("bad.png"));
        assertNotNull(image.getCause());
    }
}
