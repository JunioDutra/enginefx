package br.com.engine.platform.lwjgl;

import static org.lwjgl.vulkan.VK10.*;
import java.nio.LongBuffer;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkBufferCreateInfo;
import org.lwjgl.vulkan.VkMemoryAllocateInfo;
import org.lwjgl.vulkan.VkMemoryRequirements;
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties;

/** Host-visible vertex storage. The caller must wait for all readers before upload/growth. */
public final class LwjglVulkanDynamicVertexBuffer implements AutoCloseable
{
    private static final long MAX_BYTES = Integer.MAX_VALUE - 3L;
    private final LwjglVulkanDevice device;
    private long buffer;
    private long memory;
    private long size;

    public LwjglVulkanDynamicVertexBuffer(LwjglVulkanDevice device, long initialSize)
    {
        if (initialSize <= 0 || initialSize > MAX_BYTES || initialSize % Float.BYTES != 0)
            throw new IllegalArgumentException("Vertex buffer size must be positive, float-aligned and below 2 GiB");
        this.device = device;
        allocate(initialSize);
    }

    public long getBuffer() { return buffer; }
    public long getCapacityBytes() { return size; }

    static long capacityFor(long current, long required)
    {
        if (current <= 0 || current > MAX_BYTES || required < 0 || required > MAX_BYTES)
            throw new IllegalArgumentException("Invalid vertex buffer capacity");
        long grown = current;
        while (grown < required) grown = Math.min(MAX_BYTES, grown * 2);
        return grown;
    }

    public void upload(float[] data, int floatCount)
    {
        if (buffer == 0) throw new IllegalStateException("Vertex buffer is closed");
        if (data == null || floatCount < 0 || floatCount > data.length)
            throw new IllegalArgumentException("Invalid vertex float count: " + floatCount);
        if (floatCount == 0) return;
        long required = (long)floatCount * Float.BYTES;
        if (required > size)
        {
            long oldBuffer = buffer, oldMemory = memory;
            allocate(capacityFor(size, required));
            vkDestroyBuffer(device.getLogicalDevice(), oldBuffer, null);
            vkFreeMemory(device.getLogicalDevice(), oldMemory, null);
        }
        try (MemoryStack stack = MemoryStack.stackPush())
        {
            PointerBuffer pointer = stack.mallocPointer(1);
            check(vkMapMemory(device.getLogicalDevice(), memory, 0, required, 0, pointer), "map vertex memory");
            try { pointer.getFloatBuffer(floatCount).put(data, 0, floatCount); }
            finally { vkUnmapMemory(device.getLogicalDevice(), memory); }
        }
    }

    private void allocate(long bytes)
    {
        long newBuffer = 0, newMemory = 0;
        boolean installed = false;
        try (MemoryStack stack = MemoryStack.stackPush())
        {
            LongBuffer pointer = stack.mallocLong(1);
            VkBufferCreateInfo info = VkBufferCreateInfo.calloc(stack).sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
                .size(bytes).usage(VK_BUFFER_USAGE_VERTEX_BUFFER_BIT).sharingMode(VK_SHARING_MODE_EXCLUSIVE);
            check(vkCreateBuffer(device.getLogicalDevice(), info, null, pointer), "create vertex buffer");
            newBuffer = pointer.get(0);
            VkMemoryRequirements requirements = VkMemoryRequirements.malloc(stack);
            vkGetBufferMemoryRequirements(device.getLogicalDevice(), newBuffer, requirements);
            VkMemoryAllocateInfo allocation = VkMemoryAllocateInfo.calloc(stack).sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                .allocationSize(requirements.size()).memoryTypeIndex(findMemoryType(stack, requirements.memoryTypeBits()));
            check(vkAllocateMemory(device.getLogicalDevice(), allocation, null, pointer), "allocate vertex memory");
            newMemory = pointer.get(0);
            check(vkBindBufferMemory(device.getLogicalDevice(), newBuffer, newMemory, 0), "bind vertex memory");
            buffer = newBuffer;
            memory = newMemory;
            size = bytes;
            installed = true;
        }
        finally
        {
            if (!installed)
            {
                if (newBuffer != 0) vkDestroyBuffer(device.getLogicalDevice(), newBuffer, null);
                if (newMemory != 0) vkFreeMemory(device.getLogicalDevice(), newMemory, null);
            }
        }
    }

    private int findMemoryType(MemoryStack stack, int bits)
    {
        VkPhysicalDeviceMemoryProperties properties = VkPhysicalDeviceMemoryProperties.malloc(stack);
        vkGetPhysicalDeviceMemoryProperties(device.getPhysicalDevice(), properties);
        int required = VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT;
        for (int i = 0; i < properties.memoryTypeCount(); i++)
            if ((bits & (1 << i)) != 0 && (properties.memoryTypes(i).propertyFlags() & required) == required) return i;
        throw new IllegalStateException("No compatible Vulkan vertex memory type");
    }

    private static void check(int result, String operation)
    {
        if (result != VK_SUCCESS) throw new IllegalStateException("Failed to " + operation + ": " + result);
    }

    @Override public void close()
    {
        if (buffer != 0) vkDestroyBuffer(device.getLogicalDevice(), buffer, null);
        if (memory != 0) vkFreeMemory(device.getLogicalDevice(), memory, null);
        buffer = memory = size = 0;
    }
}
