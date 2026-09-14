package br.com.engine.graphics;

/** Aspect-preserving canvas fit shared by rendering and pointer mapping. */
public record CanvasViewport(float x, float y, float width, float height, float scale) {
    public static CanvasViewport fit(int canvasWidth,int canvasHeight,int surfaceWidth,int surfaceHeight) {
        if(canvasWidth<=0||canvasHeight<=0||surfaceWidth<=0||surfaceHeight<=0) throw new IllegalArgumentException("Positive dimensions required");
        float scale=Math.min((float)surfaceWidth/canvasWidth,(float)surfaceHeight/canvasHeight);
        float width=canvasWidth*scale,height=canvasHeight*scale;
        return new CanvasViewport((surfaceWidth-width)/2,(surfaceHeight-height)/2,width,height,scale);
    }
    public boolean contains(double px,double py){return px>=x&&py>=y&&px<x+width&&py<y+height;}
    public double canvasX(double px){return (px-x)/scale;}
    public double canvasY(double py){return (py-y)/scale;}
}
