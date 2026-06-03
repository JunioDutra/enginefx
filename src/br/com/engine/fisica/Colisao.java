package br.com.engine.fisica;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import br.com.engine.componentes.drawable.Sprite;
import br.com.engine.core.GameObject;
import br.com.engine.geometry.Rectangle;
import br.com.engine.input.KeyMap;
import br.com.engine.interfaces.CubeColisor;

public final class Colisao 
{
    public static boolean validateMovment( GameObject obj, int vel, int nKey, List<GameObject> lmap )
    {
        int nx = (int)obj.getPosition( ).getX( );
        int ny = (int)obj.getPosition( ).getY( );
        
        if( nKey == KeyMap.UP.getKey( ) )
        {
            ny -= vel;
        }
        else if( nKey == KeyMap.DOWN.getKey( ) )
        {
            ny += vel;
        }
        else if( nKey == KeyMap.LEFT.getKey( ) )
        {
            nx -= vel;
        }
        else if( nKey == KeyMap.RIGHT.getKey( ) )
        {
            nx += vel;
        }
        
        if( lmap != null )
        {
        	for( GameObject go : lmap )
        	{
        		if( go == obj )
    			{
    				continue;
    			}
        		
        		CubeColisor cube = go.getComponent( CubeColisor.class );
        		
        		if( inColision( go, new Rectangle( nx, ny, (int)cube.getRectangle().getWidth( ), (int)cube.getRectangle().getHeight( ) ) ) )
        			return false;
        	}
        }
        
        return true;
    }
   
    public boolean blocked( )
    {
        return false;
    }
        
	public static CubeColisor[] inColision( GameObject obj1, GameObject obj2 )
	{
		if( obj1 == null || obj2 == null )
			return null;
		
		CubeColisor[] cubeColisors = obj1.getComponent( CubeColisor[].class );

		for (CubeColisor cubeColisor : cubeColisors)
		{
			CubeColisor[] cubeColisors2 = obj2.getComponent( CubeColisor[].class );
			
			for( CubeColisor cubeColisor2 : cubeColisors2 )
			{
//				Rectangle bounds = cubeColisor.getRectangle( ).getBounds( );
//				Rectangle bounds2 = cubeColisor2.getRectangle( ).getBounds( );
				
//				if( bounds.getHeight( ) > bounds2.getHeight( ) )
//				{
					if( cubeColisor.getRectangle( ).intersects( cubeColisor2.getRectangle( ).getBoundsInLocal( ) ) )
					{
						return new CubeColisor[]{ cubeColisor, cubeColisor2 };
					}	
//				}
//				else
//				{
//					if( cubeColisor2.getRectangle( ).intersects( cubeColisor.getRectangle( ).getBounds( ) ) )
//					{
//						return new CubeColisor[]{ cubeColisor, cubeColisor2 };
//					}
//				}
			}
		}
		
		return null;
	}
	
	public static boolean inColision( GameObject obj, Rectangle rect )
    {
       Rectangle rec = obj.getComponent(CubeColisor.class).getRectangle( );
        
       return rec.intersects( rect.getBoundsInLocal( ) );
    }
	
	public static boolean inColision( GameObject obj, Sprite sp, Rectangle rect )
    {
		Rectangle rec  = toRectangle( obj, sp );
		Rectangle rec2 = rect;
        
        return rec.intersects( rec2.getBoundsInLocal( ) );
    }
	
	public static boolean isColision( GameObject obj, List<GameObject> lobjs )
	{
		return !inColision(obj, lobjs).isEmpty( );
	}
	
	public static Map<GameObject, CubeColisor> inColision( GameObject obj, List<GameObject> lobjs )
	{
		Map<GameObject, CubeColisor> map = new HashMap<GameObject, CubeColisor>( );
		
		for( GameObject item : lobjs )
		{
			if( item == obj || item.getComponent( CubeColisor.class ) == null )
			{
				continue;
			}
			
			CubeColisor[] inColision = inColision( obj, item );
			
			if( inColision != null )
			{
				map.put( obj,  inColision[0] );
				map.put( item, inColision[1] );
				
				return map;
			}
		}
		
		return map;
	}
	
	public static Rectangle toRectangle( GameObject obj, Sprite sp )
	{
		return new Rectangle( obj.getPosition( ).getX( ), obj.getPosition( ).getY( ), sp.getWidth( ), sp.getHeight( ) );
	}
}