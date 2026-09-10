package br.com.engine.graphics;

import java.nio.ByteBuffer;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

/** Immutable font bytes and metrics, using the same STB advances as the renderer. */
public final class Font
{
    public static final int FIRST_CODE_POINT = 32;
    public static final int CODE_POINT_COUNT = 224;
    private final String resourceId;
    private final float pixelSize;
    private final byte[] data;
    private final float[] advances = new float[CODE_POINT_COUNT];
    private final float lineHeight;

    public Font(String resourceId, float pixelSize, byte[] data)
    {
        if (resourceId == null || resourceId.isBlank()) throw new IllegalArgumentException("Font resource id is required");
        if (!Float.isFinite(pixelSize) || pixelSize <= 0) throw new IllegalArgumentException("Font size must be finite and positive");
        if (data == null || data.length < 12) throw new IllegalArgumentException("Font bytes are required");
        this.resourceId = resourceId;
        this.pixelSize = pixelSize;
        this.data = data.clone();
        ByteBuffer encoded = MemoryUtil.memAlloc(data.length);
        try (MemoryStack stack = MemoryStack.stackPush())
        {
            encoded.put(data).flip();
            STBTTFontinfo info = STBTTFontinfo.malloc(stack);
            if (!STBTruetype.stbtt_InitFont(info, encoded)) throw new IllegalArgumentException("Invalid font: " + resourceId);
            float scale = STBTruetype.stbtt_ScaleForPixelHeight(info, pixelSize);
            int[] ascent = new int[1], descent = new int[1], gap = new int[1], advance = new int[1];
            STBTruetype.stbtt_GetFontVMetrics(info, ascent, descent, gap);
            lineHeight = (ascent[0] - descent[0]) * scale;
            for (int i = 0; i < advances.length; i++)
            {
                STBTruetype.stbtt_GetCodepointHMetrics(info, FIRST_CODE_POINT + i, advance, null);
                advances[i] = advance[0] * scale;
            }
        }
        finally { MemoryUtil.memFree(encoded); }
    }

    public String getResourceId() { return resourceId; }
    public float getPixelSize() { return pixelSize; }
    public byte[] copyData() { return data.clone(); }
    public float getLineHeight() { return lineHeight; }

    /** Atlas glyph used both for layout and drawing; unsupported code points use one '?'. */
    public static int glyphCodePoint(int codePoint)
    {
        return codePoint >= FIRST_CODE_POINT && codePoint < FIRST_CODE_POINT + CODE_POINT_COUNT ? codePoint : '?';
    }

    public int getStringWidth(String text)
    {
        if (text == null || text.isEmpty()) return 0;
        float line = 0, longest = 0;
        for (int i = 0; i < text.length();)
        {
            int codePoint = text.codePointAt(i);
            i += Character.charCount(codePoint);
            if (codePoint == '\n') { longest = Math.max(longest, line); line = 0; }
            else if (codePoint >= FIRST_CODE_POINT) line += advances[glyphCodePoint(codePoint) - FIRST_CODE_POINT];
        }
        return Math.round(Math.max(longest, line));
    }

    public int getHeight() { return Math.round(lineHeight); }
}
