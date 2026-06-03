package br.com.engine.graphics;

public interface EngineGraphicsContext
{
	void save( );

	void restore( );

	void setFill( Paint paint );

	void setStroke( Paint paint );

	void fillRect( double x, double y, double width, double height );

	void strokeRect( double x, double y, double width, double height );

	void strokeOval( double x, double y, double width, double height );

	void strokeRoundRect( double x, double y, double width, double height, double arcWidth, double arcHeight );

	void drawImage( Image image, double x, double y );

	void drawImage( Image image, double sourceX, double sourceY, double sourceWidth, double sourceHeight, double destinationX, double destinationY, double destinationWidth, double destinationHeight );

	void fillText( String text, double x, double y );

	void setFont( Font font );

	void setTextBaseline( VPos baseline );

	void translate( double x, double y );

	void resetTransform( );

	EngineGraphicsTransform getTransform( );
}