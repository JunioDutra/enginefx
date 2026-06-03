package br.com.engine.componentes.drawable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import br.com.engine.componentes.SimpleComponent;
import br.com.engine.componentes.builders.ScriptBuilder;
import br.com.engine.core.ControleBase;
import br.com.engine.core.Vector2;
import br.com.engine.graphics.EngineGraphicsContext;
import br.com.engine.graphics.Image;
import br.com.engine.input.KeyBoard;
import br.com.engine.input.KeyCode;
import br.com.engine.resources.ResourceManager;

public class MensageBox extends SimpleComponent 
{
	Image image;
	
	Vector2 position = new Vector2( 0, 150 );
	
	Vector2 posUL = new Vector2( 64, 0 );
	Vector2 posUR = new Vector2( 112, 0 );
	Vector2 posDL = new Vector2( 64, 48 );
	Vector2 posDR = new Vector2( 112, 48 );
	
	Vector2 posML = new Vector2( 64, 16 );
	Vector2 posMR = new Vector2( 112, 16 );
	Vector2 posMU = new Vector2( 80, 0 );
	Vector2 posMD = new Vector2( 80, 48 );
	
	int sizeSprt = 16;
	int sizeSprtSC = 16;
	
	int quantH = 8;
	int quantV = 4;

	List<String> allMessages = new ArrayList<String>( );
	List<String> allMessagesProperties = new ArrayList<String>( );
	
	String texto;
	SpriteFont sf = new SpriteFont( );
	List<String> allLines = new ArrayList<String>( );
	List<String> renderLines = new ArrayList<String>( );
	int positionRender = 0;
	
	long elapsedTime = 0;
	
	public MensageBox( String... allMessagesProperties )
	{
		this.allMessagesProperties = Arrays.asList( allMessagesProperties );
	}
	
	@Override
	public void setup( ) 
	{
		loadMesages( );
		
		image = ResourceManager.loadResource( "System", ResourceManager.IMAGEM, Image.class );
		quantH = (int)(ControleBase.getInstance().getScreen( ).getWidth( ) - 100) / sizeSprtSC;
		
		position.setPosition( 35, position.getY( ) );
		
		sf = new SpriteFont( 20 );
		sf.setPosition( (int)position.getX( ) + (int)sizeSprt, (int)position.getY( ) + (int)sizeSprt );
		
		texto = allMessages.get( 0 );

		ajustaTexto( );
		
		getParent( ).addComponente( ScriptBuilder.create( time -> 
		{
			KeyBoard.infInstace( ).ifKeyPressed( KeyCode.ENTER, ( ) -> 
			{
				if( positionRender < allLines.size( ) && elapsedTime > 500 )
				{
					renderLines.remove( 0 );
					String line = allLines.get(positionRender++);
					renderLines.add( line );
					
					sf.setText( "" );
					
					renderLines.forEach( item -> sf.setText( sf.getTexto( )+item+"\n" ) );
					
					elapsedTime = 0;
				}
				else if( positionRender >= allLines.size( ) && elapsedTime > 500 )
				{
					if( allMessages.indexOf( this.texto ) == allMessages.size( ) - 1 )
					{
						getParent( ).destroy( );
					}
					else
					{
						this.texto = allMessages.get( allMessages.indexOf( this.texto ) + 1 );
						
						elapsedTime = 0;
						
						ajustaTexto( );
					}
				}
			} );
			
			elapsedTime += time;
		} ) );
	}

	private void loadMesages( )
	{
		@SuppressWarnings("unchecked")
		Map<String, String> mensages = ResourceManager.loadResource( "", ResourceManager.PROPS, HashMap.class );
		
		allMessagesProperties.forEach( prop -> allMessages.add( mensages.get( prop ) ) );
	}

	private void ajustaTexto( )
	{
		allLines.clear( );
		renderLines.clear( );
		
		String texto = this.texto;
		sf.setText( "" );
		
		StringTokenizer st = new StringTokenizer( texto );
		
		List<String> palavras = new ArrayList<String>( );
		while(st.hasMoreTokens()){
			palavras.add(st.nextToken());
		}

		int index = 0;
		
		while( true )
		{
			if( index < palavras.size( ) )
			{
				String nextToken = palavras.get( index++ );
				
				sf.setText( sf.getTexto( ) + " " + nextToken );
				getParent( ).addComponente( sf );
				
				if( sf.getWidth( ) > (quantH*sizeSprtSC) )
				{
					index--;
					allLines.add( sf.getTexto( ).replace( nextToken, "" ) );
					sf.setText( "" );
				}
			}
			else
			{
				allLines.add( sf.getTexto( ) );
				sf.setText( "" );
				
				allLines.forEach( line ->
				{
					if( renderLines.size( ) < 3 )
					{
						renderLines.add( line );
						positionRender++;
					}
				} );
				
				renderLines.forEach( line -> sf.setText( sf.getTexto( )+line+"\n" ) );
				
				break;
			}
		}
	}
	
	@Override
	public void draw( )
	{
		EngineGraphicsContext g = ControleBase.getInstance( ).getGraphics2d( );
		
		g.drawImage( getImage( ), posUL.getX( ), posUL.getY( ), sizeSprt, sizeSprt, position.getX( ), position.getY( ), sizeSprtSC, sizeSprtSC );

		int i;
		
		for( i = sizeSprtSC; i <= quantV*sizeSprtSC; i = i + sizeSprtSC )
		{
			g.drawImage( getImage( ), posML.getX( ), posML.getY( ), sizeSprt, sizeSprt, position.getX( ), position.getY( )+i, sizeSprtSC, sizeSprtSC );
		}
		
		g.drawImage( getImage( ), posDL.getX( ), posDL.getY( ), sizeSprt, sizeSprt, position.getX( ), position.getY( )+i, sizeSprtSC, sizeSprtSC );
		
		int j;
		
		for( j = sizeSprtSC; j <= quantH*sizeSprtSC; j = j + sizeSprtSC )
		{
			g.drawImage( getImage( ), posMU.getX( ), posMU.getY( ), sizeSprt, sizeSprt, position.getX( )+j, position.getY( ), sizeSprtSC, sizeSprtSC );
		}
		
		g.drawImage( getImage( ), posUR.getX( ), posUR.getY( ), sizeSprt, sizeSprt, position.getX( )+j, position.getY( ), sizeSprtSC, sizeSprtSC );
		
		for( j = sizeSprtSC; j <= quantH*sizeSprtSC; j = j + sizeSprtSC )
		{
			g.drawImage( getImage( ), posMD.getX( ), posMD.getY( ), sizeSprt, sizeSprt, position.getX( )+j, position.getY( )+i, sizeSprtSC, sizeSprtSC );
		}
		
		for( i = sizeSprtSC; i <= quantV*sizeSprtSC; i = i + sizeSprtSC )
		{
			g.drawImage( getImage( ), posMR.getX( ), posMR.getY( ), sizeSprt, sizeSprt, position.getX( )+j, position.getY( )+i, sizeSprtSC, sizeSprtSC );
		}
		
		g.drawImage( getImage( ), posDR.getX( ), posDR.getY( ), sizeSprt, sizeSprt, position.getX( )+j, position.getY( )+i, sizeSprtSC, sizeSprtSC );
	}

	private Image getImage( )
	{
		return image;
	}

	@Override
	public void update( long time ) 
	{
	}
}