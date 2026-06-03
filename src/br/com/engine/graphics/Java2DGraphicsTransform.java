package br.com.engine.graphics;

import java.awt.geom.AffineTransform;

public class Java2DGraphicsTransform implements EngineGraphicsTransform
{
	private final AffineTransform transform;

	public Java2DGraphicsTransform( AffineTransform transform )
	{
		this.transform = transform;
	}

	@Override
	public double getTx( )
	{
		return transform.getTranslateX( );
	}

	@Override
	public double getTy( )
	{
		return transform.getTranslateY( );
	}
}