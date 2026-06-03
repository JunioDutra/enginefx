package br.com.engine.resources;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.mapeditor.io.TMXMapReader;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import javax.imageio.ImageIO;

import br.com.engine.audio.AudioClip;
import br.com.engine.graphics.Font;
import br.com.engine.graphics.Image;

public class ContentLoader 
{
	private static final String[] RESOURCE_ROOTS = { "./res", "./src/main/resources", "./target/classes" };
	
	public static Object loadContent( String name, Map<String, Object> data )
	{
		try
		{
			List<Path> filesPaths = discoverFiles( );
			List<Path> listDiscovered = filesPaths.stream( )
				.filter( path -> matches( path, name ) )
				.collect( Collectors.toList( ) );
			
			if(listDiscovered.size( ) == 0)
			{
				throw new Exception( "file "+name+" Not found!" );
			}
			else if( listDiscovered.size( ) == 1 )
			{
				Path file = listDiscovered.get( 0 );
				
				if( data != null )
					return discoveryAndLoad( file, data );
				else
					return discoveryAndLoad( file );
			}
			else
			{
				throw new Exception( "many files named "+ name +", format specification is required ex:(\"FileName.png\")");
			}
		}
		catch(Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	private static List<Path> discoverFiles( ) throws IOException
	{
		Map<String, Path> filesPaths = new LinkedHashMap<String, Path>( );

		for( String resourceRoot : RESOURCE_ROOTS )
		{
			Path srcPath = Paths.get( resourceRoot );

			if( !Files.exists( srcPath ) )
			{
				continue;
			}

			Files.find( srcPath, Integer.MAX_VALUE, (path, basicFileAttributes) -> !Files.isDirectory( path ) )
				.forEach( path -> filesPaths.putIfAbsent( normalize( srcPath.relativize( path ).toString( ) ), path ) );
		}

		return new ArrayList<Path>( filesPaths.values( ) );
	}

	private static boolean matches( Path path, String name )
	{
		String normalizedName = normalize( name );
		String fileName = normalize( path.getFileName( ).toString( ) );

		for( String resourceRoot : RESOURCE_ROOTS )
		{
			Path root = Paths.get( resourceRoot );

			if( !path.startsWith( root ) )
			{
				continue;
			}

			String relativePath = normalize( root.relativize( path ).toString( ) );
			String relativeWithoutExtension = removeExtension( relativePath );
			String fileNameWithoutExtension = removeExtension( fileName );

			if( normalizedName.contains( "." ) )
			{
				return relativePath.equals( normalizedName ) || relativePath.endsWith( "/" + normalizedName ) || fileName.equals( normalizedName );
			}

			return relativeWithoutExtension.equals( normalizedName ) ||
				relativeWithoutExtension.endsWith( "/" + normalizedName ) ||
				fileNameWithoutExtension.equals( normalizedName );
		}

		return false;
	}

	private static String normalize( String value )
	{
		return value.replace( '\\', '/' );
	}

	private static String removeExtension( String value )
	{
		return value.replaceAll( "\\.[^.]+$", "" );
	}
	
	public static Object loadContent( String name )
	{
		return loadContent( name, Collections.emptyMap( ) );
	}

	private static Object discoveryAndLoad(Path file, Map<String, Object> data) throws Exception
	{
		if( file.getFileName().toString().matches( ".*(\\.(gif|jpg|png))$" ) )
		{
			return loadImage(file);
		}
		else if( file.getFileName().toString().matches( ".*(\\.(properties))$" ) )
		{
			return loadConfigs(file);
		}
		else if( file.getFileName().toString().matches( ".*(\\.(mp3|wav))$" ) )
		{
			return loadAudio(file);
		}
		else if( file.getFileName().toString().matches( ".*(\\.(js))$" ) )
		{
			return loadScript( file, data );
		}
		else if( file.getFileName().toString().matches( ".*(\\.(ttf))$" ) )
		{
			return loadFont( file, data );
		}
		else if( file.getFileName().toString().matches( ".*(\\.(json))$" ) )
		{
			return loadJson( file );
		}
		else if( file.getFileName().toString().matches( ".*(\\.(xml))$" ) )
		{
			return Files.readString( file );
		}
		else if( file.getFileName().toString().matches( ".*(\\.(tmx))$" ) )
		{
			return loadMap( file );
		}
		else
		{
			throw new Exception("File format is not supported, "+file.getFileName().toString().replaceAll( "^.*\\.", "" ) );
		}
	}
	
	private static Object discoveryAndLoad(Path file) throws Exception 
	{
		return discoveryAndLoad(file, Collections.emptyMap());
	}
	
	private static Object loadImage( Path path ) throws FileNotFoundException
	{
		try
		{
			return new Image( ImageIO.read( path.toFile( ) ) );
		}
		catch( IOException exception )
		{
			throw new RuntimeException( exception );
		}
	}
	
	private static Object loadConfigs( Path path ) throws FileNotFoundException, IOException
	{
		Properties p = new Properties( );
		
		p.load( new FileInputStream( path.toFile( ) ) );
		
		return p;
	}
	
	private static Object loadAudio( Path path )
	{
		return new AudioClip( path );
	}
	
	private static Object loadScript( Path path, Map<String, Object> data ) throws FileNotFoundException, ScriptException
    {
        ScriptEngineManager scriptEngineManager = new ScriptEngineManager( );
        ScriptEngine nashorn = scriptEngineManager.getEngineByName( "nashorn" );
        
        if( data != null )
        {
            data.forEach( (key, value) ->
            {
                nashorn.put( key, value );
            } );
        }
        
        nashorn.eval( new FileReader( path.toFile( ) ) );
        
        return nashorn; 
    }
	
	private static Object loadFont( Path path, Map<String, Object> data ) throws FileNotFoundException
	{
		try
		{
			java.awt.Font awtFont = java.awt.Font.createFont( java.awt.Font.TRUETYPE_FONT, path.toFile( ) ).deriveFont( ((Number)data.get( "size" )).floatValue( ) );
			return new Font( awtFont );
		}
		catch( java.awt.FontFormatException | IOException exception )
		{
			throw new RuntimeException( exception );
		}
	}

	private static Object loadJson( Path path ) throws JsonSyntaxException, JsonIOException, FileNotFoundException
	{
		Gson gson = new GsonBuilder( ).create( );
		JsonObject jsonObject = gson.fromJson( new FileReader( path.toFile( ) ), JsonObject.class );
		
		return jsonObject;
	}

	private static org.mapeditor.core.Map loadMap( Path path ) throws FileNotFoundException, Exception
	{
		return new TMXMapReader( ).readMap( new FileInputStream( path.toFile( ) ) );
	}
	
	public static void main(String[] args) throws Exception {
		Image img = (Image)loadContent("Player.png");
		Properties conf = (Properties)loadContent("config");
		
		System.out.println(img.getWidth());
		
		conf.keySet().forEach( o -> System.out.println(o) );
	}
}