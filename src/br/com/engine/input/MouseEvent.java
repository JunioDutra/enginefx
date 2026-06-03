package br.com.engine.input;

public class MouseEvent
{
    private final double x;
    private final double y;

    public MouseEvent( double x, double y )
    {
        this.x = x;
        this.y = y;
    }

    public double getX( ) { return x; }
    public double getY( ) { return y; }
    public double getSceneX( ) { return x; }
    public double getSceneY( ) { return y; }
}
