package br.com.engine.componentes.drawable;

import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D.Double;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.mapeditor.core.Map;
import org.mapeditor.core.MapObject;
import org.mapeditor.core.ObjectGroup;
import org.mapeditor.core.Tile;
import org.mapeditor.core.TileLayer;

import br.com.engine.componentes.SimpleComponent;
import br.com.engine.componentes.builders.Colisors;
import br.com.engine.componentes.physics.CustomCubeColisor;
import br.com.engine.core.ControleBase;
import br.com.engine.core.Vector2;
import br.com.engine.graphics.EngineGraphicsContext;
import br.com.engine.graphics.Image;
import br.com.engine.interfaces.CubeColisor;
import br.com.engine.resources.ResourceManager;

public class TmxMap extends SimpleComponent
{
	private String tmxMapFile;

	private Map map;

	private record TileDraw( Image image, Vector2 position ) { }
	private final List<TileDraw> itens = new ArrayList<>( );
	
	public TmxMap( String tmxMapFile )
	{
		this.tmxMapFile = tmxMapFile;
	}
	
	@Override
	public void setup( )
	{
		try 
		{
			map = ResourceManager.loadResource( this.tmxMapFile, ResourceManager.MAP, Map.class );
			itens.clear( );
			java.util.Map<BufferedImage, Image> tileImages = new java.util.IdentityHashMap<>( );
			
			for( int l = 0; l < map.getLayerCount( ); l++ )
			{
				if( map.getLayer( l ) instanceof ObjectGroup )
				{
					ObjectGroup object = (ObjectGroup)map.getLayer( l );
					
					List<MapObject> objects = object.getObjects( );
					
					objects.forEach(mapObject->{
						Double bounds = mapObject.getBounds( );

						CubeColisor colisor = null;
						
						if( mapObject.getShape( ) instanceof Ellipse2D )
						{//TODO: VAlidar novo m�todo de colis�o
//							colisor = Colisors.ovalCustom( new Vector2(bounds.x, bounds.y), bounds.width, bounds.height);
//							((OvalCustomCubeColisor)colisor).setTag( mapObject.getName( ) );
						}
						else
						{
							colisor = Colisors.custom( new Vector2((float)bounds.getX(), (float)bounds.getY()), (int)bounds.getWidth(), (int)bounds.getHeight());
							((CustomCubeColisor)colisor).setTag( mapObject.getName( ) );
						}
						
						if( colisor != null ) getParent( ).addComponente( colisor );
					});
				}
				else if( map.getLayer( l ) instanceof TileLayer )
				{
					TileLayer layer = ((TileLayer)map.getLayer( l ));
					
					
					for (int y = 0; y < layer.getHeight(); y++) 
					{
						for (int x = 0; x < layer.getWidth(); x++) 
						{
							Tile tile = layer.getTileAt( x , y );
							
							if( tile == null )
							{
								continue;
							}
							
							BufferedImage image = (BufferedImage)tile.getImage( );
							if( image != null ) itens.add( new TileDraw( tileImages.computeIfAbsent( image, Image::new ),
								new Vector2( x * map.getTileWidth( ), y * map.getTileHeight( ) ) ) );
						}	
					}
					
				}
			}
		} 
		catch( Exception e )
		{
			throw new IllegalStateException( "Cannot load TMX map: " + tmxMapFile, e );
		}
	}
	
	@Override
	public void draw( )
	{
		EngineGraphicsContext g = ControleBase.getInstance( ).getGraphics2d( );

		Vector2 position = getParent( ).getPosition();
		
		itens.forEach( tile -> g.drawImage( tile.image( ), position.getX( ) + tile.position( ).getX( ), position.getY( ) + tile.position( ).getY( ) ) );
	}

	@Override
	public void update( long time )
	{
		
	}
}
