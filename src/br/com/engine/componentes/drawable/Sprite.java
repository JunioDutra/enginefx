package br.com.engine.componentes.drawable;

import java.util.HashMap;
import java.util.Map;

import br.com.engine.componentes.SimpleComponent;
import br.com.engine.core.ControleBase;
import br.com.engine.core.Vector2;
import br.com.engine.graphics.Image;
import br.com.engine.graphics.IntPoint;
import br.com.engine.resources.ResourceManager;

public class Sprite extends SimpleComponent
{
	private transient Image   image;
	private           String  imageNome;
	private           Vector2 position;

	protected int spSizeW;
	protected int spSizeH;

	protected int spSizeWScaled;
	protected int spSizeHScaled;

	private int cx;
	private int cy;

	private Map<Integer, IntPoint> pontos = new HashMap<Integer, IntPoint>( );

	protected IntPoint currentPoint;

	protected Sprite( ) { }

	/**
	 * M�todo utilizado para contruir sprites multiplos.
	 * @param image
	 * @param cx
	 * @param cy
	 */
	public Sprite( String image, int cx, int cy )
	{
		this.cx = cx;
		this.cy = cy;
		this.imageNome = image;

		this.position = new Vector2( );
	}

	/**
	 * M�todo utilizado para contruir sprites Unicos.
	 * @param image
	 */
	public Sprite( String image )
	{
		this( image, 1, 1 );
	}

	public Sprite( String image, Vector2 ponto, int w, int h )
	{
		this( image, 1, 1 );

		this.spSizeWScaled = this.spSizeW = w;
        this.spSizeHScaled = this.spSizeH = h;

        currentPoint = new IntPoint( (int)ponto.getX( ), (int)ponto.getY( ) );
	}

	public Image getImage( )
	{
		return image;
	}

	public int getWidth( )
	{
		return this.spSizeWScaled;
	}

	public int getHeight( )
	{
		return this.spSizeHScaled;
	}

	public void setImage( Image image )
	{
		this.image = image;
	}

	public String getImageNome( )
	{
		return imageNome;
	}

	public void setImageNome( String imageNome )
	{
		this.imageNome = imageNome;
	}

	public Vector2 getPosition( )
	{
	    return position;
	}

	public void setPosition( Vector2 position )
	{
	    this.position = position;
	}

	public void scale( int width, int height )
    {
		this.spSizeWScaled = width;
		this.spSizeHScaled = height;
    }

	public void rotate( int angle )
	{
//	    setImage( ControleImagem.rotateImage( getImage( ), angle ) ); //TODO
	}

	@Override
	public void draw( )
	{
	    int x = (int)getParent( ).getPosition( ).getX( ) + (int)getPosition( ).getX( );
        int y = (int)getParent( ).getPosition( ).getY( ) + (int)getPosition( ).getY( );

		ControleBase.getInstance( ).getGraphics2d( ).drawImage( getImage( ), currentPoint.x( ), currentPoint.y( ), spSizeW, spSizeH, x, y, spSizeWScaled, spSizeHScaled );
	}

	@Override
	public void setup( )
	{
		setImage( ResourceManager.image( imageNome ) );

		if( cx > 1 || cy > 1 )
		{
			int tensAdd = 0;

	        int spriteWidth  = (int)( getImage( ).getWidth( )  / cx );
	        int spriteHeight = (int)( getImage( ).getHeight( ) / cy );

	        for( int i = 0; i <= getImage( ).getHeight( ) - spriteHeight; i = spriteHeight + i )
	        {
	            for( int j = 0; j <= getImage( ).getWidth( ) - spriteWidth; j = spriteWidth + j )
	            {
				pontos.put( tensAdd++, new IntPoint( j, i ) );
	            }
	        }

	        this.spSizeWScaled = this.spSizeW = spriteWidth;
	        this.spSizeHScaled = this.spSizeH = spriteHeight;

	        currentPoint = pontos.get( 0 );
		}
		else if( currentPoint == null )
		{
			currentPoint = new IntPoint( 0, 0 );

			this.spSizeWScaled = this.spSizeW = (int)getImage( ).getWidth( );
			this.spSizeHScaled = this.spSizeH = (int)getImage( ).getHeight( );
		}
	}

	/**
	 * Ajusta o Sprite corrente caso ele seja multiplo, do contrario lan�a uma exce��o
	 *
	 * @param index indice do sprite
	 */
	public void setSprite( int index )
	{
		if( cx == 1 && cy == 1 )
		{
			throw new RuntimeException( imageNome + " is not configured to a multiple sprite!" );
		}
		currentPoint = pontos.get( index );
	}

	@Override
	public void update( long time )
	{
	}
}
