package br.com.engine.graphics;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.util.ArrayDeque;
import java.util.Deque;

public class Java2DGraphicsContext implements EngineGraphicsContext
{
	private final Graphics2D delegate;
	private final Deque<GraphicsState> states = new ArrayDeque<GraphicsState>( );
	private Paint fill = Color.BLACK;
	private Paint stroke = Color.BLACK;
	private Font font = new Font( "Arial", 12 );
	private VPos baseline = VPos.BASELINE;

	public Java2DGraphicsContext( Graphics2D delegate )
	{
		this.delegate = delegate;
		delegate.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
		delegate.setRenderingHint( RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON );
	}

	@Override
	public void save( )
	{
		states.push( new GraphicsState( delegate.getTransform( ), fill, stroke, font, baseline ) );
	}

	@Override
	public void restore( )
	{
		if( states.isEmpty( ) )
		{
			return;
		}

		GraphicsState state = states.pop( );
		delegate.setTransform( new AffineTransform( state.transform ) );
		fill = state.fill;
		stroke = state.stroke;
		font = state.font;
		baseline = state.baseline;
	}

	@Override
	public void setFill( Paint paint )
	{
		fill = paint;
	}

	@Override
	public void setStroke( Paint paint )
	{
		stroke = paint;
	}

	@Override
	public void fillRect( double x, double y, double width, double height )
	{
		delegate.setColor( fill.toAwtColor( ) );
		delegate.fillRect( (int)Math.round( x ), (int)Math.round( y ), (int)Math.round( width ), (int)Math.round( height ) );
	}

	@Override
	public void strokeRect( double x, double y, double width, double height )
	{
		delegate.setColor( stroke.toAwtColor( ) );
		delegate.drawRect( (int)Math.round( x ), (int)Math.round( y ), (int)Math.round( width ), (int)Math.round( height ) );
	}

	@Override
	public void strokeOval( double x, double y, double width, double height )
	{
		delegate.setColor( stroke.toAwtColor( ) );
		delegate.drawOval( (int)Math.round( x ), (int)Math.round( y ), (int)Math.round( width ), (int)Math.round( height ) );
	}

	@Override
	public void strokeRoundRect( double x, double y, double width, double height, double arcWidth, double arcHeight )
	{
		delegate.setColor( stroke.toAwtColor( ) );
		delegate.drawRoundRect( (int)Math.round( x ), (int)Math.round( y ), (int)Math.round( width ), (int)Math.round( height ), (int)Math.round( arcWidth ), (int)Math.round( arcHeight ) );
	}

	@Override
	public void drawImage( Image image, double x, double y )
	{
		delegate.drawImage( image.getBufferedImage( ), (int)Math.round( x ), (int)Math.round( y ), null );
	}

	@Override
	public void drawImage( Image image, double sourceX, double sourceY, double sourceWidth, double sourceHeight, double destinationX, double destinationY, double destinationWidth, double destinationHeight )
	{
		delegate.drawImage( image.getBufferedImage( ),
			(int)Math.round( destinationX ),
			(int)Math.round( destinationY ),
			(int)Math.round( destinationX + destinationWidth ),
			(int)Math.round( destinationY + destinationHeight ),
			(int)Math.round( sourceX ),
			(int)Math.round( sourceY ),
			(int)Math.round( sourceX + sourceWidth ),
			(int)Math.round( sourceY + sourceHeight ),
			null );
	}

	@Override
	public void fillText( String text, double x, double y )
	{
		delegate.setColor( fill.toAwtColor( ) );
		delegate.setFont( font.toAwtFont( ) );
		int drawY = (int)Math.round( y );

		if( baseline == VPos.TOP )
		{
			drawY += delegate.getFontMetrics( font.toAwtFont( ) ).getAscent( );
		}

		delegate.drawString( text, (int)Math.round( x ), drawY );
	}

	@Override
	public void setFont( Font font )
	{
		this.font = font;
	}

	@Override
	public void setTextBaseline( VPos baseline )
	{
		this.baseline = baseline;
	}

	@Override
	public void translate( double x, double y )
	{
		delegate.translate( x, y );
	}

	@Override
	public EngineGraphicsTransform getTransform( )
	{
		return new Java2DGraphicsTransform( delegate.getTransform( ) );
	}

	private static final class GraphicsState
	{
		private final AffineTransform transform;
		private final Paint fill;
		private final Paint stroke;
		private final Font font;
		private final VPos baseline;

		private GraphicsState( AffineTransform transform, Paint fill, Paint stroke, Font font, VPos baseline )
		{
			this.transform = new AffineTransform( transform );
			this.fill = fill;
			this.stroke = stroke;
			this.font = font;
			this.baseline = baseline;
		}
	}
}