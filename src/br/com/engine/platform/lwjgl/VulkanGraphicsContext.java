package br.com.engine.platform.lwjgl;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import br.com.engine.graphics.Color;
import br.com.engine.graphics.EngineGraphicsContext;
import br.com.engine.graphics.EngineGraphicsTransform;
import br.com.engine.graphics.Font;
import br.com.engine.graphics.Image;
import br.com.engine.graphics.Paint;
import br.com.engine.graphics.VPos;

public class VulkanGraphicsContext implements EngineGraphicsContext
{
    public interface Command {}

    private final List<Command> commands = new ArrayList<Command>( );
    private final Deque<GraphicsState> states = new LinkedList<GraphicsState>( );
    private Paint fill = Color.BLACK;
    private Paint stroke = Color.BLACK;
    private Font font = new Font( "Arial", 12 );
    private VPos baseline = VPos.BASELINE;
    private double translateX;
    private double translateY;
    private int canvasWidth;
    private int canvasHeight;

    public void setCanvasSize( int width, int height )
    {
        if( width <= 0 || height <= 0 ) throw new IllegalArgumentException( "Canvas size must be positive" );
        canvasWidth = width;
        canvasHeight = height;
    }

    public int getCanvasWidth( ) { return canvasWidth; }
    public int getCanvasHeight( ) { return canvasHeight; }

    public void beginFrame( )
    {
        commands.clear( );
    }

    public List<Command> getCommands( )
    {
        return List.copyOf( commands );
    }

    @Override
    public void save( )
    {
        states.push( new GraphicsState( fill, stroke, font, baseline, translateX, translateY ) );
    }

    @Override
    public void restore( )
    {
        if( !states.isEmpty( ) )
        {
            GraphicsState state = states.pop( );
            fill = state.fill;
            stroke = state.stroke;
            font = state.font;
            baseline = state.baseline;
            translateX = state.translateX;
            translateY = state.translateY;
        }
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
        commands.add( new FillRectCommand( translateX + x, translateY + y, width, height, fill ) );
    }

    @Override
    public void strokeRect( double x, double y, double width, double height )
    {
        double t = 1.0;
        commands.add( new FillRectCommand( translateX + x,                     translateY + y,                      width, t, stroke ) );
        commands.add( new FillRectCommand( translateX + x,                     translateY + y + height - t,         width, t, stroke ) );
        commands.add( new FillRectCommand( translateX + x,                     translateY + y + t,                  t, height - 2 * t, stroke ) );
        commands.add( new FillRectCommand( translateX + x + width - t,         translateY + y + t,                  t, height - 2 * t, stroke ) );
    }

    @Override
    public void strokeOval( double x, double y, double width, double height )
    {
        double cx = translateX + x + width / 2;
        double cy = translateY + y + height / 2;
        double rx = width / 2;
        double ry = height / 2;
        int segments = 16;
        double t = 1.0;
        
        double prevX = cx + rx;
        double prevY = cy;
        
        for (int i = 1; i <= segments; i++) {
            double angle = i * 2 * Math.PI / segments;
            double nextX = cx + rx * Math.cos(angle);
            double nextY = cy + ry * Math.sin(angle);
            
            // Draw a line segment
            double dx = nextX - prevX;
            double dy = nextY - prevY;
            double length = Math.hypot(dx, dy);
            double nx = -dy / length * (t/2);
            double ny = dx / length * (t/2);
            
            commands.add(new FillQuadCommand(
                prevX + nx, prevY + ny,
                prevX - nx, prevY - ny,
                nextX - nx, nextY - ny,
                nextX + nx, nextY + ny,
                stroke
            ));
            
            prevX = nextX;
            prevY = nextY;
        }
    }

    @Override
    public void strokeRoundRect( double x, double y, double width, double height, double arcWidth, double arcHeight )
    {
        // approximate as strokeRect for debug purposes, typical in simple engines
        strokeRect(x, y, width, height);
    }

    @Override
    public void drawImage( Image image, double x, double y )
    {
        commands.add( new DrawImageCommand( image, 0, 0, image.getWidth( ), image.getHeight( ), x + translateX, y + translateY, image.getWidth( ), image.getHeight( ) ) );
    }

    @Override
    public void drawImage( Image image, double sourceX, double sourceY, double sourceWidth, double sourceHeight, double destinationX, double destinationY, double destinationWidth, double destinationHeight )
    {
        commands.add( new DrawImageCommand( image, sourceX, sourceY, sourceWidth, sourceHeight, destinationX + translateX, destinationY + translateY, destinationWidth, destinationHeight ) );
    }

    @Override
    public void fillText( String text, double x, double y )
    {
        commands.add( new DrawTextCommand( text, font, fill, baseline, x + translateX, y + translateY ) );
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
        translateX += x;
        translateY += y;
    }

    @Override
    public void resetTransform( )
    {
        translateX = 0;
        translateY = 0;
    }

    @Override
    public EngineGraphicsTransform getTransform( )
    {
        return new VulkanGraphicsTransform( translateX, translateY );
    }

    public record FillRectCommand( double x, double y, double width, double height, Paint fill ) implements Command {}

    public record FillQuadCommand( double x0, double y0, double x1, double y1, double x2, double y2, double x3, double y3, Paint fill ) implements Command {}

    public record DrawImageCommand( Image image, double sourceX, double sourceY, double sourceWidth, double sourceHeight, double destinationX, double destinationY, double destinationWidth, double destinationHeight ) implements Command {}

    public record DrawTextCommand( String text, Font font, Paint fill, VPos baseline, double x, double y ) implements Command {}

    private record GraphicsState( Paint fill, Paint stroke, Font font, VPos baseline, double translateX, double translateY ) {}
}
