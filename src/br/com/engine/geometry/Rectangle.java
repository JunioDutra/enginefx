package br.com.engine.geometry;

public class Rectangle
{
	private double x;
	private double y;
	private double width;
	private double height;

	public Rectangle( double x, double y, double width, double height )
	{
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}

	public double getX( )
	{
		return x;
	}

	public void setX( double x )
	{
		this.x = x;
	}

	public double getY( )
	{
		return y;
	}

	public void setY( double y )
	{
		this.y = y;
	}

	public double getWidth( )
	{
		return width;
	}

	public double getHeight( )
	{
		return height;
	}

	public Rectangle getBoundsInLocal( )
	{
		return this;
	}

	public boolean intersects( Rectangle other )
	{
		return x < other.x + other.width && x + width > other.x && y < other.y + other.height && y + height > other.y;
	}
}