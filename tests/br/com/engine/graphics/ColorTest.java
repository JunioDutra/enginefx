package br.com.engine.graphics;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class ColorTest
{
    @Test void parsesRgbAndRgbaWithoutAwt( )
    {
        assertEquals( "#ff0080", Color.valueOf( "#ff0080" ).toString( ) );
        Color translucent = Color.valueOf( "#01020380" );
        assertEquals( 128, translucent.alpha8( ) );
        assertEquals( 1f / 255f, translucent.red( ) );
    }
}
