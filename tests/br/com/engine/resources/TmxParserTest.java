package br.com.engine.resources;

import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.DeflaterOutputStream;
import org.junit.jupiter.api.Test;

class TmxParserTest
{
    private static TmxMapData parse(String xml) { return TmxParser.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))); }
    private static String map(String attributes, String content) {
        return "<map orientation='orthogonal' width='2' height='2' tilewidth='16' tileheight='16' " + attributes + ">" + content + "</map>";
    }
    private static String layer(int... gids) throws Exception {
        ByteBuffer raw = ByteBuffer.allocate(gids.length * 4).order(ByteOrder.LITTLE_ENDIAN);
        for (int gid : gids) raw.putInt(gid);
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        try (DeflaterOutputStream zip = new DeflaterOutputStream(compressed)) { zip.write(raw.array()); }
        return "<layer name='base' width='2' height='2'><data encoding='base64' compression='zlib'>" +
            Base64.getEncoder().encodeToString(compressed.toByteArray()) + "</data></layer>";
    }

    @Test void readsInlineLayersObjectsAndRenderOrder() throws Exception {
        TmxMapData data = parse(map("renderorder='right-up'",
            "<tileset firstgid='1' tilewidth='16' tileheight='16' columns='2'><image source='tiles.png'/></tileset>" +
            layer(1,0,2,3) + "<objectgroup><object name='circle' x='2' y='3' width='8' height='8'><ellipse/></object></objectgroup>"));
        assertArrayEquals(new int[] {1,0,2,3}, data.layers().get(0).gids());
        assertEquals("circle", data.objects().get(0).name());
        assertTrue(data.objects().get(0).ellipse());
        assertEquals("right-up", data.renderOrder());
    }

    @Test void supportsOlderTilesetsWithoutColumns() {
        var data = parse(map("", "<tileset firstgid='1' tilewidth='16' tileheight='16'><image source='tiles.png' width='64'/></tileset>"));
        assertEquals(4, data.tilesets().get(0).columns());
    }

    @Test void rejectsUnsupportedTransformsAndWrongDataSize() throws Exception {
        String flipped = map("", layer(1,0,0x80000002,3));
        assertTrue(assertThrows(IllegalStateException.class, () -> parse(flipped)).getMessage().contains("Flipped"));
        String truncated = map("", layer(1,2));
        assertTrue(assertThrows(IllegalStateException.class, () -> parse(truncated)).getMessage().contains("size"));
        assertThrows(IllegalStateException.class, () -> parse(map("infinite='1'", "")));
        assertThrows(IllegalStateException.class, () -> parse(map("", "").replace("orthogonal", "isometric")));
        assertThrows(IllegalStateException.class, () -> parse(map("", "<tileset firstgid='1' source='external.tsx'/>")));
        assertThrows(IllegalStateException.class, () -> parse(map("", "<layer width='2' height='2'/>")));
    }
}
