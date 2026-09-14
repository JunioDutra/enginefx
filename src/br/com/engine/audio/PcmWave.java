package br.com.engine.audio;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** Strict PCM WAV reader shared by the OpenAL backend and tests. */
final class PcmWave
{
    private final ByteBuffer samples;
    private final int channels;
    private final int bitsPerSample;
    private final int sampleRate;

    private PcmWave(ByteBuffer samples, int channels, int bitsPerSample, int sampleRate)
    {
        this.samples = samples.asReadOnlyBuffer();
        this.channels = channels;
        this.bitsPerSample = bitsPerSample;
        this.sampleRate = sampleRate;
    }

    ByteBuffer samples() { return samples.duplicate(); }
    int channels() { return channels; }
    int bitsPerSample() { return bitsPerSample; }
    int sampleRate() { return sampleRate; }

    static PcmWave read(byte[] bytes)
    {
        if (bytes == null || bytes.length < 12) throw new IllegalArgumentException("WAV header is incomplete");
        ByteBuffer source = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        if (!chunk(source, 0, "RIFF") || !chunk(source, 8, "WAVE")) throw new IllegalArgumentException("Expected RIFF/WAVE PCM");
        int cursor = 12;
        int channels = 0;
        int sampleRate = 0;
        int bits = 0;
        int dataOffset = -1;
        int dataLength = 0;
        while (cursor + 8 <= bytes.length)
        {
            int length = source.getInt(cursor + 4);
            long end = (long)cursor + 8L + Integer.toUnsignedLong(length);
            if (end > bytes.length) throw new IllegalArgumentException("WAV chunk exceeds input");
            if (chunk(source, cursor, "fmt "))
            {
                if (length < 16) throw new IllegalArgumentException("WAV fmt chunk is incomplete");
                int format = Short.toUnsignedInt(source.getShort(cursor + 8));
                channels = Short.toUnsignedInt(source.getShort(cursor + 10));
                sampleRate = source.getInt(cursor + 12);
                bits = Short.toUnsignedInt(source.getShort(cursor + 22));
                if (format != 1) throw new IllegalArgumentException("Only PCM WAV is supported");
            }
            else if (chunk(source, cursor, "data"))
            {
                dataOffset = cursor + 8;
                dataLength = length;
            }
            cursor = (int)end + (length & 1);
        }
        if (channels < 1 || channels > 2 || sampleRate <= 0 || (bits != 8 && bits != 16))
            throw new IllegalArgumentException("Unsupported PCM WAV format");
        if (dataOffset < 0 || dataLength == 0) throw new IllegalArgumentException("WAV data chunk is missing");
        int frameBytes = channels * (bits / 8);
        if (dataLength % frameBytes != 0) throw new IllegalArgumentException("WAV data is not frame-aligned");
        ByteBuffer samples = ByteBuffer.wrap(bytes, dataOffset, dataLength).slice();
        return new PcmWave(samples, channels, bits, sampleRate);
    }

    private static boolean chunk(ByteBuffer bytes, int offset, String expected)
    {
        return bytes.get(offset) == expected.charAt(0) && bytes.get(offset + 1) == expected.charAt(1)
            && bytes.get(offset + 2) == expected.charAt(2) && bytes.get(offset + 3) == expected.charAt(3);
    }
}
