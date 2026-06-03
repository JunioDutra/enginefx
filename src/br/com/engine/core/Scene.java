package br.com.engine.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import br.com.engine.componentes.VectorMonitor;
import br.com.engine.componentes.debug.ColisorDebug;
import br.com.engine.componentes.debug.SpriteDebug;
import br.com.engine.componentes.drawable.Sprite;
import br.com.engine.componentes.scripts.Camera;
import br.com.engine.fisica.Colisao;
import br.com.engine.graphics.EngineGraphicsContext;
import br.com.engine.interfaces.CubeColisor;


public abstract class Scene
{
	private List<GameObject> lGameObjects           = new ArrayList<GameObject>( );
	private List<GameObject> lGameObjectsWaitAdd    = new ArrayList<GameObject>( );
	private List<GameObject> lGameObjectsWaitRemove = new ArrayList<GameObject>( );
	
	private boolean lockList = false;
	
	public Scene( )
	{
		
	}

	public void setup( )
	{
		GameObject gameObject = new GameObject("default_camera");
		gameObject.addComponente(new Camera());
		add(gameObject);
	}
	
	public String getName()
	{
		return getClass().getSimpleName();
	}
	
	public void draw( EngineGraphicsContext g2 ) 
	{
		lGameObjects.forEach( item -> 
			item.getComponentes( ).forEach( componente -> componente.draw( ) )
		);
	}

	public void update( long time ) 
	{		
		lockList = true;
		
		lGameObjects.forEach( go -> 
		{
			go.getComponentes( ).stream( ).filter( o -> !(o instanceof VectorMonitor) ).forEach( componente -> componente.update( time ) );
			go.getComponentes( ).stream( ).filter( o -> o instanceof VectorMonitor ).forEach( componente -> componente.update( time ) ); 
			
			if( go.getComponent( CubeColisor.class ) != null )
			{
				Map<GameObject, CubeColisor> mapInColision = Colisao.inColision( go, lGameObjects );

				if( mapInColision != null )
				{
					mapInColision.keySet( ).stream( ).filter( obj -> obj != go ).findFirst( ).ifPresent(inColision->{
						go.getPosition( ).setPosition        ( go.getPreviusPosition( ).getX( ),         go.getPreviusPosition( ).getY( )         );
						inColision.getPosition( ).setPosition( inColision.getPreviusPosition( ).getX( ), inColision.getPreviusPosition( ).getY( ) );
						
						go.getComponent        ( CubeColisor.class ).onColision( mapInColision.get( inColision ) );
						inColision.getComponent( CubeColisor.class ).onColision( mapInColision.get( go         ) );
					});
				}
			}
			
			go.getComponentes( ).stream( ).filter( o -> o instanceof VectorMonitor ).forEach( componente -> componente.update( time ) );
			
			if( go.isDestroy( ) )
			{
				remove( go );
			}
		} );
		
		lockList = false;
		
		if( lGameObjectsWaitAdd.size( ) > 0 )
		{
			lGameObjectsWaitAdd.forEach( this::add );
			lGameObjectsWaitAdd.clear( );
		}
		
		if( lGameObjectsWaitRemove.size( ) > 0 )
		{
			lGameObjectsWaitRemove.forEach( this::remove );
			lGameObjectsWaitRemove.clear( );
		}
	}
	
	public void onCallChange( )
	{
		
	}
	
	public void add( GameObject obj )
	{
		if( lockList )
		{
			lGameObjectsWaitAdd.add( obj );
		}
		else
		{
			if( ControleBase.getInstance( ).getConfigurations( ).isDebugMode( ) )
			{
				if( obj.getComponent( Sprite.class ) != null )
				{
				    obj.addComponente( new SpriteDebug( ) );
				}
				
				if( obj.getComponent( CubeColisor.class ) != null )
				{
				    obj.addComponente( new ColisorDebug( ) );
				}
			}
			
			this.lGameObjects.add( obj );
			
			obj.setup( );
		}
	}
	
	public void remove( GameObject obj )
	{
		if( lockList )
		{
			this.lGameObjectsWaitRemove.add( obj );
		}
		else
		{
			this.lGameObjects.remove( obj );
		}
	}
	
	public void removeAll( List<GameObject> lObj )
	{
		this.lGameObjects.removeAll( lObj );
	}

    public void clearScene( )
    {
        removeAll( this.lGameObjects );
    }

    public List<GameObject> getNode( )
    {
        return this.lGameObjects;
    }
    
    public GameObject getObject( String name )
    {
        Optional<GameObject> optGameObject = this.lGameObjects.stream( ).filter( obj -> obj.getTag( ).equals( name ) ).findFirst( );
        
        if( optGameObject.isPresent( ) )
        	return optGameObject.get( );
        else
        	throw new RuntimeException( String.format( "GameObject %s not find", name ) );
    }
}