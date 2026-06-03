package br.com.engine.resources;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.stream.Stream;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.mapeditor.io.TMXMapReader;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import br.com.engine.audio.AudioClip;
import br.com.engine.graphics.Font;
import br.com.engine.graphics.Image;

public class ResourceManager 
{
	public static final int IMAGEM        = 1;
	public static final int CONFIGURACOES = 2;
	public static final int XML           = 3;
	public static final int AUDIO         = 4;
	public static final int AUDIOW        = 5;
	public static final int SCENES        = 6;
	public static final int SCRIPT        = 7;
	public static final int FONT          = 8;
	public static final int MAP           = 9;
	public static final int PROPS         = 10;
	
	private static String configSufix  = "config.json";
	private static String applicationConfig = "application.json";
	
	private static String xmlPrefix = "res/mapas/";
	private static String xmlSufix  = ".xml";
	private static String sceneSufix  = "list_scenes.json";
	
    @SuppressWarnings("unchecked")
    public static <T> T loadResource( String name, int nTipo, Class<T> tipo, Map<String, Object> data )
    {
        switch( nTipo )
        {
            case IMAGEM:
                return (T)carregaImagem( name );
            case CONFIGURACOES:
                return (T)carregaConfigs( );
            case XML:
                return (T)carregaXML( name );
            case AUDIO:
                return (T)loadAudio( name );
            case SCENES:
                return (T)carregarScenes( );
            case SCRIPT:
                return (T)carregaScript( name, data );
            case FONT:
                return (T)carregaFont( name, data );
            case MAP:
                return (T)loadMap( name );
            case PROPS:
            	return (T)carregaProps( );
        }
        
        return null;
    }
	
	private static org.mapeditor.core.Map loadMap( String name )
	{
		try 
		{
			return (org.mapeditor.core.Map)ContentLoader.loadContent( name + ".tmx" );
		} 
		catch( Exception e )
		{
			e.printStackTrace( );
		}
		
		return null;
	}

	public static <T> T loadResource( String nome, int nTipo, Class<T> tipo )
	{
	    return loadResource( nome, nTipo, tipo, null );
	}

	private static ScenesDefinition[] carregarScenes( )
	{
		try 
		{
			JsonObject jsonObject = (JsonObject)ContentLoader.loadContent( sceneSufix );
			
			JsonArray jsonArray = jsonObject.get( "mapas" ).getAsJsonArray( );
			
			ScenesDefinition[] ret = new ScenesDefinition[jsonArray.size( )];
			
			for( int i = 0; i < ret.length; i++ )
			{
				JsonObject object = jsonArray.get( i ).getAsJsonObject( );
				
				ret[i] = new ScenesDefinition( object.get( "class" ).getAsString( ), object.get( "type" ).getAsString( ) );
			}

			return ret;
		}
		catch( Exception e )
		{
			e.printStackTrace( );
		}
		
		return null;
	}

	private static Object loadAudio( String name )
	{
		try 
		{
			return ContentLoader.loadContent( name );
		}
		catch( Exception e )
		{
			e.printStackTrace( );
		}
		
		return null;
	}

	private static Object carregaXML( String nome )
	{
		try 
		{
			StringBuilder sb = new StringBuilder( );
			
			Scanner sc = new Scanner( new FileInputStream( xmlPrefix.concat( nome.concat( xmlSufix ) ) ) );
			
			while( sc.hasNextLine( ) )
			{
				sb.append( sc.nextLine( ) );
			}
			
			sc.close( );
			
			return sb.toString( );
		} 
		catch( FileNotFoundException e )
		{
			e.printStackTrace( );
		}
		
		return "";
	}

	private static Object carregaConfigs( )
	{
		try 
		{
			Gson gson = new GsonBuilder( ).create( );
			JsonObject jsonObject;

			try
			{
				jsonObject = (JsonObject)ContentLoader.loadContent( configSufix );
			}
			catch( RuntimeException exception )
			{
				jsonObject = (JsonObject)ContentLoader.loadContent( applicationConfig );
			}

			return gson.fromJson( jsonObject, Configurations.class );
		}
		catch( Exception e )
		{
			e.printStackTrace( );
		}
		
		return null;
	}
	
	private static Map<String, String> carregaProps( )
	{
		Map<String, String> props = new HashMap<String, String>( );
		
		try 
		{
			List<Path> messageRoots = Stream.of( Paths.get( "./res/mensages" ), Paths.get( "./src/main/resources/mensages" ), Paths.get( "./target/classes/mensages" ) )
				.filter( Files::exists )
				.collect( java.util.stream.Collectors.toList( ) );
			
			for( Path messageRoot : messageRoots )
			{
				try( Stream<Path> list = Files.list( messageRoot ) )
				{
					list.forEach( path -> 
					{
						try 
						{
							Properties p = new Properties( );
							p.load( new FileInputStream( path.toString( ) ) );
							p.entrySet().forEach( entry -> props.put( (String)entry.getKey( ), (String)entry.getValue( ) ) );
						} 
						catch( Exception e )
						{
							e.printStackTrace( );
						}
					});
				}
			}
		} 
		catch( IOException e )
		{
			e.printStackTrace( );
		}
		
		return props;
	}

	private static Object carregaImagem( String imageNome )
	{
		try
		{
			return ContentLoader.loadContent( imageNome );
		} 
		catch( RuntimeException e ) 
		{
			e.printStackTrace( );
		}
		
		return null;
	}
	
	private static ScriptEngine carregaScript( String scriptNome, Map<String, Object> data )
    {
        try 
        {
	        return (ScriptEngine)ContentLoader.loadContent( scriptNome, data );
        } 
        catch( Exception e )
        {
            e.printStackTrace( );
        }
        
        return null;
    }
	
	private static Font carregaFont( String nome, Map<String, Object> data )
	{
		try 
		{
			return (Font)ContentLoader.loadContent( nome, data );
		}
		catch( RuntimeException e )
		{
			e.printStackTrace();
		}
		
		return null;
	}

}