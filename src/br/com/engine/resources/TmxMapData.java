package br.com.engine.resources;

import java.util.List;

public record TmxMapData(int width, int height, int tileWidth, int tileHeight,
    List<Tileset> tilesets, List<Layer> layers, List<MapObject> objects, String renderOrder)
{
    public record Tileset(int firstGid, int tileWidth, int tileHeight, int columns, String image) { }
    public record Layer(String name, int width, int height, boolean visible, int[] gids) { }
    public record MapObject(String name, float x, float y, float width, float height, boolean ellipse) { }
}
