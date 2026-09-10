package br.com.engine.audio;
import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayInputStream;
import org.junit.jupiter.api.Test;

class AudioClipTest
{
    @Test void closesInputWhenTheAudioFormatIsInvalid()
    {
        boolean[] closed = {false};
        var input = new ByteArrayInputStream(new byte[] {1,2,3}) {
            @Override public void close() { closed[0] = true; }
        };
        var error = assertThrows(IllegalStateException.class, () -> new AudioClip("invalid.wav", input));
        assertTrue(closed[0]);
        assertTrue(error.getMessage().contains("invalid.wav"));
        assertNotNull(error.getCause());
    }
}
