package br.com.engine.core;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import br.com.engine.componentes.drawable.Sprite;

class SpriteScaleTest {
    @Test void explicitSizeSurvivesResourceSetup() {
        Sprite sprite=new Sprite("nested/pixel.png");
        sprite.scale(48,58);
        sprite.setup();
        assertEquals(48,sprite.getWidth());
        assertEquals(58,sprite.getHeight());
    }
}
