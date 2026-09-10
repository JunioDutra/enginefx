package br.com.engine.core;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TimeTest
{
    @BeforeEach void reset() { Time.reset(); }

    @Test void preservesFractionalMillisecondsAtHighFrameRates()
    {
        long total = 0;
        for (int i = 0; i < 2000; i++) { Time.update(500_000); total += Time.getDeltaMillis(); }
        assertEquals(1000, total);
    }

    @Test void clampsSecondsAndLegacyMillisecondsTogether()
    {
        Time.update(5_000_000_000L);
        assertEquals(0.25f, Time.getDeltaTime());
        assertEquals(250, Time.getDeltaMillis());
        Time.update(-10);
        assertEquals(0, Time.getDeltaMillis());
    }
}
