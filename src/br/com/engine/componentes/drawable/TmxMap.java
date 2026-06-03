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

	List<java.util.Map<Image, Vector2>> itens = new ArrayList<java.util.Map<Image,Vector2>>();
	
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
						
						getParent( ).addComponente( colisor );
					});
				}
				else
				{
					TileLayer layer = ((TileLayer)map.getLayer( l ));
					
					java.util.Map<Image, Vector2> images = new HashMap<Image, Vector2>( );
					
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
							images.put( new Image( image ), new Vector2( x*image.getWidth( ), y*image.getHeight() ) );
						}	
					}
					
					itens.add(images);
				}
			}
		} 
		catch( Exception e )
		{
			e.printStackTrace( );
		}
	}
	
	@Override
	public void draw( )
	{
		EngineGraphicsContext g = ControleBase.getInstance( ).getGraphics2d( );

		Vector2 position = getParent( ).getPosition();
		
		itens.forEach( item -> item.forEach( (key, value) -> g.drawImage( key, 
				                                                          position.getX( ) + value.getX( ), 
				 													      position.getY( ) + value.getY( ) ) ) );
	}

	@Override
	public void update( long time )
	{
		
	}
}