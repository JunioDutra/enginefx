package br.com.engine.platform.lwjgl;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class VertexBufferCapacityTest
{
    @Test void growsWithoutTruncationAndRejectsInvalidOrClosedCapacity()
    {
        assertEquals(64, LwjglVulkanDynamicVertexBuffer.capacityFor(16, 48));
        assertEquals(16, LwjglVulkanDynamicVertexBuffer.capacityFor(16, 0));
        assertThrows(IllegalArgumentException.class, () -> LwjglVulkanDynamicVertexBuffer.capacityFor(0, 48));
        assertThrows(IllegalArgumentException.class, () -> LwjglVulkanDynamicVertexBuffer.capacityFor(16, Long.MAX_VALUE));
    }
}
