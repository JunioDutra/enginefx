package br.com.engine.resources;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import br.com.engine.core.Scene;
import br.com.engine.core.SceneJs;

public class ScenesDefinition 
{
	private String scene;
	private String type;
	
	public ScenesDefinition(String scene, String type) {
		super();
		this.scene = scene;
		this.type = type;
	}

	public Scene getNewScene( )
	{
		try 
		{
		    if( type.equalsIgnoreCase( "JS" ) )
		    {
		        Constructor<SceneJs> declaredConstructors = SceneJs.class.getConstructor( String[].class );
		        
		        String[] array = (String[])Array.newInstance( String.class, 1 );
		        array[0] = "characters/player";
		        
		        return (Scene)declaredConstructors.newInstance( (Object)array );
		    }
		    else
		    {
		        return Class.forName( getScene( ) ).asSubclass( Scene.class ).getDeclaredConstructor( ).newInstance( );
		    }
		} 
		catch( InstantiationException | IllegalAccessException | 
				ClassNotFoundException | IllegalArgumentException | 
				NoSuchMethodException | InvocationTargetException | 
				SecurityException e )
		{
			throw new IllegalStateException( "Cannot create scene: " + scene, e );
		}
				
	}

	public String getScene( )
	{
		return scene;
	}

	public void setScene( String scene )
	{
		this.scene = scene;
	}
}
