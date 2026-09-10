package br.com.engine.input;

import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class MouseTest
{
    @Test void releaseDuringDispatchSkipsLaterListenersAndOldHandleCannotRemoveNewSubscription()
    {
        Mouse mouse = Mouse.infInstace();
        Object owner = new Object();
        AtomicInteger calls = new AtomicInteger();
        var first = mouse.addListener(owner, event -> mouse.releaseOwner(owner));
        mouse.addListener(owner, event -> calls.incrementAndGet());
        mouse.click(0, 0);
        assertEquals(0, calls.get());
        var next = mouse.addListener(owner, event -> calls.incrementAndGet());
        first.close();
        mouse.click(0, 0);
        assertEquals(1, calls.get());
        next.close();
    }
}
