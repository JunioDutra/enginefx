package br.com.engine.core;

import java.io.File;
import java.io.FileReader;
import java.util.HashMap;

import javax.script.Invocable;

import br.com.engine.componentes.builders.ScriptBuilder;
import br.com.engine.componentes.drawable.Sprite;
import br.com.engine.resources.ResourceManager;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;


public class SceneJs extends Scene
{
	String[] gameObectsNames;
	
	public SceneJs( String[] gameObectsNames )
	{
		this.gameObectsNames = gameObectsNames;
	}
	
	public void setup( )
	{
		super.setup();
	     
		try 
		{
			for( String goName : gameObectsNames )
			{
				JsonObject object = (JsonObject)br.com.engine.resources.ContentLoader.loadContent( "scripts/" + goName + ".json" );
				
				loadGameObect( object );
			}
			
		}
		catch( Exception e )
		{
			e.printStackTrace();
		}
		
	}

	private void loadGameObect( JsonObject object ) 
	{
		GameObject gameObject = new GameObject( );
	
		HashMap<String, Object> hashMap = new HashMap<String, Object>( );
        hashMap.put( "gameObject",  gameObject );
        hashMap.put( "screen",      ControleBase.getInstance().getScreen( ) );
        hashMap.put( "spriteClass", Sprite.class );
        
		try
        {
            JsonObject position = object.get( "position" ).getAsJsonObject( );
            
            gameObject.getPosition( ).setPosition( position.get( "x" ).getAsInt( ),
                                                   position.get( "y" ).getAsInt( ) );
            
            JsonArray components = object.get( "components" ).getAsJsonArray( );
            carregaComponentes( gameObject, components) ;
            
            JsonArray scripts = object.get( "scripts" ).getAsJsonArray( );
            carregaScripts( gameObject, hashMap, scripts );
        } 
        catch( JsonSyntaxException | JsonIOException e )
        {
            e.printStackTrace( );
        }
        
        add( gameObject );
	}

	private void carregaScripts(GameObject gameObject,
			HashMap<String, Object> hashMap, JsonArray scripts) {
		for( JsonElement script : scripts )
		{
		    Invocable invoc = ResourceManager.loadResource( script.getAsString( ), ResourceManager.SCRIPT, Invocable.class, hashMap );
		    
		    gameObject.addComponente( ScriptBuilder.create( time ->
	        {
	            try
	            {
	                invoc.invokeFunction( "update", time );
	            } 
	            catch( Exception e )
	            {
	                e.printStackTrace( );
	            }
	        }
		    ) );
		}
	}

	@Override
	public String getName() {
		return "jsScene";
	}
	
	private void carregaComponentes(GameObject gameObject, JsonArray components) {
		for( JsonElement component : components )
		{
		    if( component.getAsJsonObject( ).get( "type" ).getAsString( ).equalsIgnoreCase( "Sprite" ) )
		    {
		    	gameObject.addComponente( new Sprite( component.getAsJsonObject( ).get( "name" ).getAsString( ) ) );
		    }
//		    else if( component.getAsJsonObject( ).get( "type" ).getAsString( ).equalsIgnoreCase( "Animator" ) )
//		    {
//		    	//TODO: Austar as possibilidades do animator deixando ele mas coniguravel e sucetivel a mudanças
//		        JsonObject animator = component.getAsJsonObject( ).get( "animator" ).getAsJsonObject( );
//		        
//		        AnimatorComponent playerAnimator = new AnimatorComponent( animator.get( "cx" ).getAsInt( ), animator.get( "cy" ).getAsInt( ) );
//		        gameObject.addComponente( playerAnimator );
//		        playerAnimator.setup( );
//		        
//		        playerAnimator.setInterval( animator.get( "interval" ).getAsInt( ) );
//		        playerAnimator.setInitialSprite( animator.get( "initialSprite" ).getAsInt( ) );
//
//		        JsonArray animations = animator.get( "animations" ).getAsJsonArray( );
//		        
//		        for( JsonElement jsonElement : animations )
//		        {
//		            JsonObject animation = jsonElement.getAsJsonObject( );
//		            
//		            playerAnimator.creatAnimation( animation.get( "name" ).getAsString( ), 
//		                                           animation.get ( "initialFrame" ).getAsInt( ), 
//		                                           animation.get ( "length" ).getAsInt( )  );
//		        }
//		        
//		        playerAnimator.play( animator.get( "initialAnimation" ).getAsString( ) );
//		    }
		}
	}
}