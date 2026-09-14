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

    @Test void readsFrameAlignedLittleEndianPcm()
    {
        PcmWave wave = PcmWave.read(pcmWave(1, 16, new byte[] {0, 0, 12, 0}));
        assertEquals(1, wave.channels());
        assertEquals(16, wave.bitsPerSample());
        assertEquals(22_050, wave.sampleRate());
        assertEquals(4, wave.samples().remaining());
    }

    @Test void rejectsNonPcmAndIncompleteSamples()
    {
        byte[] floatWave = pcmWave(1, 16, new byte[] {0, 0});
        floatWave[20] = 3;
        floatWave[21] = 0;
        assertThrows(IllegalArgumentException.class, () -> PcmWave.read(floatWave));
        assertThrows(IllegalArgumentException.class, () -> PcmWave.read(pcmWave(2, 16, new byte[] {0, 0, 1})));
    }

    private static byte[] pcmWave(int channels, int bits, byte[] samples)
    {
        int sampleRate = 22_050;
        int blockAlign = channels * bits / 8;
        java.nio.ByteBuffer bytes = java.nio.ByteBuffer.allocate(44 + samples.length).order(java.nio.ByteOrder.LITTLE_ENDIAN);
        bytes.put("RIFF".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        bytes.putInt(36 + samples.length);
        bytes.put("WAVEfmt ".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        bytes.putInt(16).putShort((short)1).putShort((short)channels).putInt(sampleRate);
        bytes.putInt(sampleRate * blockAlign).putShort((short)blockAlign).putShort((short)bits);
        bytes.put("data".getBytes(java.nio.charset.StandardCharsets.US_ASCII)).putInt(samples.length).put(samples);
        return bytes.array();
    }
}
