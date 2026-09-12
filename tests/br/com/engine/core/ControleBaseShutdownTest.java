package br.com.engine.core;

import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ControleBaseShutdownTest
{
    @Test void repeatedRequestsAndStopsDisposeTheSceneOnce() throws Exception
    {
        ControleBase control = newControl();
        AtomicInteger disposals = new AtomicInteger();
        installScene(control, new Scene() { @Override public void dispose() { disposals.incrementAndGet(); } });
        control.requestExit();
        control.requestExit();
        assertTrue(control.isExitRequested());
        assertEquals(0, disposals.get());
        control.stop();
        control.stop();
        assertEquals(1, disposals.get());
    }

    @Test void cleanupFailureDoesNotRepeatDisposalOnTheOuterFinally() throws Exception
    {
        ControleBase control = newControl();
        AtomicInteger disposals = new AtomicInteger();
        installScene(control, new Scene() { @Override public void dispose() {
            disposals.incrementAndGet();
            throw new IllegalStateException("cleanup failure");
        } });
        assertThrows(IllegalStateException.class, control::stop);
        assertDoesNotThrow(control::stop);
        assertTrue(control.isExitRequested());
        assertEquals(1, disposals.get());
    }

    private static ControleBase newControl() throws Exception
    {
        var constructor = ControleBase.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    private static void installScene(ControleBase control, Scene scene) throws Exception
    {
        var field = ControleBase.class.getDeclaredField("gameLogic");
        field.setAccessible(true);
        field.set(control, scene);
    }
}
