package br.com.engine.componentes.drawable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import br.com.engine.componentes.SimpleComponent;
import br.com.engine.componentes.builders.Colisors;
import br.com.engine.componentes.physics.CustomCubeColisor;
import br.com.engine.core.ControleBase;
import br.com.engine.core.Vector2;
import br.com.engine.graphics.EngineGraphicsContext;
import br.com.engine.graphics.Image;
import br.com.engine.resources.ResourceManager;
import br.com.engine.resources.TmxMapData;

/** Draws finite orthogonal TMX layers. Collision objects currently use their AABB. */
public class TmxMap extends SimpleComponent
{
    private final String tmxMapFile;
    private TmxMapData map;
    private record TileDraw(Image image, int sourceX, int sourceY, int width, int height, Vector2 position) { }
    private final List<TileDraw> tiles = new ArrayList<>();

    public TmxMap(String tmxMapFile) { this.tmxMapFile = tmxMapFile; }

    @Override public void setup()
    {
        try
        {
            map = ResourceManager.map(tmxMapFile);
            tiles.clear();
            java.util.Map<String, Image> images = new HashMap<>();
            for (TmxMapData.MapObject object : map.objects())
            {
                // Preserve the full bounds even for ellipses. Exact ellipse response is not implemented.
                CustomCubeColisor collider = (CustomCubeColisor)Colisors.custom(new Vector2(object.x(), object.y()),
                    Math.round(object.width()), Math.round(object.height()));
                collider.setTag(object.name());
                getParent().addComponente(collider);
            }
            boolean left = map.renderOrder().startsWith("left");
            boolean up = map.renderOrder().endsWith("up");
            for (TmxMapData.Layer layer : map.layers())
            {
                if (!layer.visible()) continue;
                for (int row = 0; row < layer.height(); row++) for (int column = 0; column < layer.width(); column++)
                {
                    int x = left ? layer.width() - 1 - column : column;
                    int y = up ? layer.height() - 1 - row : row;
                    int gid = layer.gids()[y * layer.width() + x];
                    if (gid == 0) continue;
                    TmxMapData.Tileset tileset = tilesetFor(gid);
                    if (tileset == null) throw new IllegalArgumentException("No tileset for GID " + gid);
                    int local = gid - tileset.firstGid();
                    Image image = images.computeIfAbsent(tileset.image(), this::loadTilesetImage);
                    int sx = (local % tileset.columns()) * tileset.tileWidth();
                    int sy = (local / tileset.columns()) * tileset.tileHeight();
                    if (sx + tileset.tileWidth() > image.getWidth() || sy + tileset.tileHeight() > image.getHeight())
                        throw new IllegalArgumentException("GID outside tileset image: " + gid);
                    tiles.add(new TileDraw(image, sx, sy, tileset.tileWidth(), tileset.tileHeight(),
                        new Vector2(x * map.tileWidth(), (y + 1) * map.tileHeight() - tileset.tileHeight())));
                }
            }
        }
        catch (Exception exception) { throw new IllegalStateException("Cannot load TMX map: " + tmxMapFile, exception); }
    }

    @Override public void draw()
    {
        EngineGraphicsContext graphics = ControleBase.getInstance().getGraphics2d();
        Vector2 position = getParent().getPosition();
        for (TileDraw tile : tiles)
            graphics.drawImage(tile.image(), tile.sourceX(), tile.sourceY(), tile.width(), tile.height(),
                position.x + tile.position().x, position.y + tile.position().y, tile.width(), tile.height());
    }

    @Override public void update(long time) { }
    @Override public void dispose() { tiles.clear(); map = null; }

    private TmxMapData.Tileset tilesetFor(int gid)
    {
        TmxMapData.Tileset selected = null;
        for (TmxMapData.Tileset candidate : map.tilesets())
            if (candidate.firstGid() <= gid && (selected == null || candidate.firstGid() > selected.firstGid())) selected = candidate;
        return selected;
    }

    private Image loadTilesetImage(String source)
    {
        String normalized = source.replace('\\', '/');
        if (normalized.startsWith("/") || normalized.contains(":")) throw new IllegalArgumentException("Tileset image must be relative");
        Path mapPath = Path.of(tmxMapFile.replace('\\', '/'));
        Path parent = mapPath.getParent();
        Path imagePath = (parent == null ? Path.of(normalized) : parent.resolve(normalized)).normalize();
        return ResourceManager.image(imagePath.toString());
    }
}
