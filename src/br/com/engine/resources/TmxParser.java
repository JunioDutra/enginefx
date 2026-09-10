package br.com.engine.resources;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.zip.InflaterInputStream;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

/** Reader for finite orthogonal maps, inline image tilesets and base64/zlib tile layers. */
public final class TmxParser
{
    private TmxParser() { }

    public static TmxMapData parse(InputStream input)
    {
        XMLStreamReader xml = null;
        try
        {
            XMLInputFactory factory = XMLInputFactory.newFactory();
            factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            factory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);
            xml = factory.createXMLStreamReader(input);
            int width = 0, height = 0, tileWidth = 0, tileHeight = 0;
            String order = "right-down";
            List<TmxMapData.Tileset> tilesets = new ArrayList<>();
            List<TmxMapData.Layer> layers = new ArrayList<>();
            List<TmxMapData.MapObject> objects = new ArrayList<>();
            while (xml.hasNext())
            {
                if (xml.next() != XMLStreamConstants.START_ELEMENT) continue;
                switch (xml.getLocalName())
                {
                    case "map" -> {
                        if (!"orthogonal".equals(attribute(xml, "orientation")) || integer(xml, "infinite", 0) != 0)
                            throw new IllegalArgumentException("Only finite orthogonal TMX maps are supported");
                        width = positive(xml, "width"); height = positive(xml, "height");
                        tileWidth = positive(xml, "tilewidth"); tileHeight = positive(xml, "tileheight");
                        if (attribute(xml, "renderorder") != null) order = attribute(xml, "renderorder");
                        if (!List.of("right-down", "right-up", "left-down", "left-up").contains(order))
                            throw new IllegalArgumentException("Unsupported TMX render order: " + order);
                    }
                    case "tileset" -> tilesets.add(tileset(xml));
                    case "layer" -> layers.add(layer(xml));
                    case "objectgroup" -> objects.addAll(objects(xml));
                    case "group", "imagelayer" -> throw new IllegalArgumentException("Unsupported TMX layer: " + xml.getLocalName());
                }
            }
            if (width <= 0 || height <= 0) throw new IllegalArgumentException("TMX map element is required");
            return new TmxMapData(width, height, tileWidth, tileHeight, List.copyOf(tilesets), List.copyOf(layers), List.copyOf(objects), order);
        }
        catch (Exception exception) { throw new IllegalStateException("Cannot parse TMX: " + exception.getMessage(), exception); }
        finally
        {
            if (xml != null) try { xml.close(); } catch (javax.xml.stream.XMLStreamException ignored) { }
        }
    }

    private static TmxMapData.Tileset tileset(XMLStreamReader xml) throws Exception
    {
        if (attribute(xml, "source") != null) throw new IllegalArgumentException("External TSX tilesets are not supported");
        int first = positive(xml, "firstgid"), tw = positive(xml, "tilewidth"), th = positive(xml, "tileheight");
        int columns = integer(xml, "columns", 0);
        if (integer(xml, "spacing", 0) != 0 || integer(xml, "margin", 0) != 0)
            throw new IllegalArgumentException("Tileset spacing and margin are not supported");
        String image = null;
        while (xml.hasNext())
        {
            int event = xml.next();
            if (event == XMLStreamConstants.START_ELEMENT && xml.getLocalName().equals("image"))
            {
                if (image != null) throw new IllegalArgumentException("Image collection tilesets are not supported");
                image = attribute(xml, "source");
                if (columns == 0) columns = integer(xml, "width", 0) / tw;
            }
            if (event == XMLStreamConstants.START_ELEMENT && xml.getLocalName().equals("tileoffset"))
                throw new IllegalArgumentException("Tileset offsets are not supported");
            if (event == XMLStreamConstants.END_ELEMENT && xml.getLocalName().equals("tileset")) break;
        }
        if (image == null || columns <= 0) throw new IllegalArgumentException("Tileset requires an image and positive columns/image width");
        return new TmxMapData.Tileset(first, tw, th, columns, image);
    }

    private static void untransformed(XMLStreamReader xml)
    {
        if (decimal(xml, "offsetx") != 0 || decimal(xml, "offsety") != 0 || decimal(xml, "x") != 0 || decimal(xml, "y") != 0)
            throw new IllegalArgumentException("TMX layer offsets are not supported");
        String opacity = attribute(xml, "opacity");
        if (opacity != null && Float.parseFloat(opacity) != 1)
            throw new IllegalArgumentException("TMX layer opacity is not supported");
    }

    private static TmxMapData.Layer layer(XMLStreamReader xml) throws Exception
    {
        untransformed(xml);
        String name = attribute(xml, "name");
        int width = positive(xml, "width"), height = positive(xml, "height");
        int count = Math.multiplyExact(width, height);
        boolean visible = integer(xml, "visible", 1) != 0;
        int[] data = null;
        while (xml.hasNext())
        {
            int event = xml.next();
            if (event == XMLStreamConstants.START_ELEMENT && xml.getLocalName().equals("data"))
            {
                String encoding = attribute(xml, "encoding"), compression = attribute(xml, "compression");
                data = gids(xml.getElementText(), encoding, compression, count);
            }
            if (event == XMLStreamConstants.END_ELEMENT && xml.getLocalName().equals("layer")) break;
        }
        if (data == null) throw new IllegalArgumentException("TMX layer data is required");
        return new TmxMapData.Layer(name, width, height, visible, data);
    }

    private static List<TmxMapData.MapObject> objects(XMLStreamReader xml) throws Exception
    {
        untransformed(xml);
        List<TmxMapData.MapObject> result = new ArrayList<>();
        while (xml.hasNext())
        {
            int event = xml.next();
            if (event == XMLStreamConstants.END_ELEMENT && xml.getLocalName().equals("objectgroup")) break;
            if (event != XMLStreamConstants.START_ELEMENT || !xml.getLocalName().equals("object")) continue;
            if (attribute(xml, "gid") != null || decimal(xml, "rotation") != 0)
                throw new IllegalArgumentException("Rotated and tile TMX objects are not supported");
            String name = attribute(xml, "name");
            float x = decimal(xml, "x"), y = decimal(xml, "y"), w = decimal(xml, "width"), h = decimal(xml, "height");
            boolean ellipse = false;
            while (xml.hasNext())
            {
                event = xml.next();
                if (event == XMLStreamConstants.START_ELEMENT)
                {
                    String tag = xml.getLocalName();
                    if (tag.equals("ellipse")) ellipse = true;
                    if (List.of("polygon", "polyline", "point", "text").contains(tag))
                        throw new IllegalArgumentException("Unsupported TMX object shape: " + tag);
                }
                if (event == XMLStreamConstants.END_ELEMENT && xml.getLocalName().equals("object")) break;
            }
            if (w <= 0 || h <= 0) throw new IllegalArgumentException("TMX collider must have positive dimensions");
            result.add(new TmxMapData.MapObject(name == null ? "" : name, x, y, w, h, ellipse));
        }
        return result;
    }

    private static int[] gids(String text, String encoding, String compression, int expected) throws Exception
    {
        if (!"base64".equals(encoding) || !(compression == null || "zlib".equals(compression)))
            throw new IllegalArgumentException("TMX data requires base64 with optional zlib");
        byte[] encoded = Base64.getMimeDecoder().decode(text);
        int size = Math.multiplyExact(expected, 4);
        byte[] raw;
        try (InputStream stream = compression == null ? new ByteArrayInputStream(encoded) : new InflaterInputStream(new ByteArrayInputStream(encoded)))
        {
            raw = stream.readNBytes(size);
            if (raw.length != size || stream.read() != -1) throw new IllegalArgumentException("TMX layer size does not match dimensions");
        }
        int[] gids = new int[expected];
        ByteBuffer buffer = ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < gids.length; i++)
        {
            int gid = buffer.getInt();
            if ((gid & 0xf0000000) != 0) throw new IllegalArgumentException("Flipped/rotated TMX tiles are not supported");
            gids[i] = gid;
        }
        return gids;
    }

    private static String attribute(XMLStreamReader xml, String name) { return xml.getAttributeValue(null, name); }
    private static int integer(XMLStreamReader xml, String name, int fallback) { String value = attribute(xml, name); return value == null ? fallback : Integer.parseInt(value); }
    private static int positive(XMLStreamReader xml, String name)
    {
        int value = integer(xml, name, 0);
        if (value <= 0) throw new IllegalArgumentException("TMX " + name + " must be positive");
        return value;
    }
    private static float decimal(XMLStreamReader xml, String name)
    {
        String value = attribute(xml, name);
        float parsed = value == null ? 0 : Float.parseFloat(value);
        if (!Float.isFinite(parsed)) throw new IllegalArgumentException("Invalid TMX " + name);
        return parsed;
    }
}
