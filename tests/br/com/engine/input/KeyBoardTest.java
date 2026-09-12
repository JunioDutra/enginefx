package br.com.engine.input;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class KeyBoardTest
{
    @Test void separatesHeldStateFromThePressEdge()
    {
        KeyBoard keyboard = KeyBoard.infInstace();
        keyboard.reset();

        keyboard.press(KeyCode.I);
        keyboard.press(KeyCode.I); // GLFW repeat/duplicate callbacks do not create a second edge.
        keyboard.beginFrame();
        assertTrue(keyboard.isDown(KeyCode.I));
        assertTrue(keyboard.wasPressedThisFrame(KeyCode.I));
        keyboard.endFrame();
        assertFalse(keyboard.wasPressedThisFrame(KeyCode.I));

        keyboard.beginFrame();
        assertTrue(keyboard.isDown(KeyCode.I));
        assertFalse(keyboard.wasPressedThisFrame(KeyCode.I));
        keyboard.endFrame();

        keyboard.release(KeyCode.I);
        keyboard.beginFrame();
        assertFalse(keyboard.isDown(KeyCode.I));
        assertTrue(keyboard.wasReleasedThisFrame(KeyCode.I));
        keyboard.reset();
    }

    @Test void callbacksDuringRenderingSurviveUntilTheNextLogicalFrame()
    {
        KeyBoard keyboard = KeyBoard.infInstace();
        keyboard.reset();
        keyboard.beginFrame();
        keyboard.press(KeyCode.E); // e.g. glfwWaitEventsTimeout while minimized.
        keyboard.endFrame();
        keyboard.beginFrame();
        assertTrue(keyboard.wasPressedThisFrame(KeyCode.E));
        keyboard.endFrame();
        keyboard.beginFrame();
        assertFalse(keyboard.wasPressedThisFrame(KeyCode.E));
        assertTrue(keyboard.isDown(KeyCode.E));
        keyboard.reset();
    }

    @Test void aCompleteTapBetweenFramesPreservesBothEdges()
    {
        KeyBoard keyboard = KeyBoard.infInstace();
        keyboard.reset();
        keyboard.press(KeyCode.E);
        keyboard.release(KeyCode.E);
        keyboard.beginFrame();
        assertTrue(keyboard.wasPressedThisFrame(KeyCode.E));
        assertTrue(keyboard.wasReleasedThisFrame(KeyCode.E));
        assertFalse(keyboard.isDown(KeyCode.E));
        keyboard.reset();
    }
}
