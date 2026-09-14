package br.com.engine.platform.lwjgl;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.junit.jupiter.api.Test;

class ShaderAssetTest
{
    @Test void shipsAllThreePrecompiledSpirvVariants() throws Exception
    {
        for (String resource : java.util.List.of("shaders/quad.vert.spv", "shaders/quad-srgb.frag.spv", "shaders/quad-unorm.frag.spv"))
        {
            try (var input = getClass().getClassLoader().getResourceAsStream(resource))
            {
                assertNotNull(input, resource);
                byte[] bytes = input.readAllBytes();
                assertTrue(bytes.length >= 20 && bytes.length % 4 == 0, resource);
                assertEquals(0x07230203, ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getInt(), resource);
            }
        }
    }
}
