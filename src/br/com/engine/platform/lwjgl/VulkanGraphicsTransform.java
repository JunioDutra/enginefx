package br.com.engine.platform.lwjgl;

import br.com.engine.graphics.EngineGraphicsTransform;

public class VulkanGraphicsTransform implements EngineGraphicsTransform
{
	private final double tx;
	private final double ty;

	public VulkanGraphicsTransform( double tx, double ty )
	{
		this.tx = tx;
		this.ty = ty;
	}

	@Override
	public double getTx( )
	{
		return tx;
	}

	@Override
	public double getTy( )
	{
		return ty;
	}
}