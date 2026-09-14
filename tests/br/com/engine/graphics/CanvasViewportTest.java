package br.com.engine.graphics;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
class CanvasViewportTest {
    @Test void maximizeKeepsAspectAndMapsPointerThroughLetterbox() {
        var fit=CanvasViewport.fit(960,640,1920,1080);
        assertEquals(150,fit.x(),0.01);assertEquals(0,fit.y(),0.01);
        assertEquals(1.5,fit.width()/fit.height(),0.001);
        assertFalse(fit.contains(100,540));assertTrue(fit.contains(960,540));
        assertEquals(480,fit.canvasX(960),0.01);assertEquals(320,fit.canvasY(540),0.01);
    }
}
