package br.com.engine.core;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import br.com.engine.componentes.SimpleComponent;
import br.com.engine.componentes.builders.Colisors;
import br.com.engine.input.Mouse;

class LifecycleTest
{
    private static class Component extends SimpleComponent
    {
        int setups, disposals;
        @Override public void setup() { setups++; }
        @Override public void update(long time) { }
        @Override public void draw() { }
        @Override public void dispose() { disposals++; }
    }

    @Test void initializesEachComponentOnceIncludingNestedAdditions()
    {
        GameObject object = new GameObject();
        Component nested = new Component();
        Component first = new Component() { @Override public void setup() { super.setup(); getParent().addComponente(nested); } };
        object.addComponente(first);
        object.addComponente(first);
        object.setup();
        object.setup();
        assertEquals(1, first.setups);
        assertEquals(1, nested.setups);
        Component late = new Component();
        object.addComponente(late);
        assertEquals(1, late.setups);
        object.dispose();
        object.dispose();
        assertEquals(1, first.disposals);
        assertEquals(1, late.disposals);
        assertThrows(IllegalStateException.class, () -> object.addComponente(new Component()));
    }

    @Test void removesPendingObjectsAndReleasesSceneSubscriptionsDuringCallbacks()
    {
        Scene scene = new Scene() { };
        GameObject pending = new GameObject();
        Component pendingComponent = new Component();
        pending.addComponente(pendingComponent);
        AtomicInteger clicks = new AtomicInteger();
        Mouse.infInstace().addListener(scene, event -> clicks.incrementAndGet());
        GameObject first = new GameObject();
        first.addComponente(new Component() { @Override public void update(long time) { scene.add(pending); scene.dispose(); } });
        scene.add(first);
        scene.update(16);
        assertTrue(scene.getNode().isEmpty());
        assertTrue(pending.isDisposed());
        assertEquals(0, pendingComponent.setups);
        assertEquals(1, pendingComponent.disposals);
        Mouse.infInstace().click(0, 0);
        assertEquals(0, clicks.get());
    }

    @Test void cancelsAddThenRemoveAndDefersDrawMutations()
    {
        Scene scene = new Scene() { };
        GameObject cancelled = new GameObject();
        GameObject first = new GameObject();
        GameObject next = new GameObject();
        Component nextComponent = new Component();
        next.addComponente(nextComponent);
        first.addComponente(new Component() { @Override public void draw() {
            scene.add(cancelled); scene.remove(cancelled); scene.add(next); scene.remove(first);
            assertEquals(0, nextComponent.setups);
        } });
        scene.add(first);
        scene.draw(null);
        assertEquals(List.of(next), scene.getNode());
        assertTrue(cancelled.isDisposed());
        assertEquals(1, nextComponent.setups);
        scene.dispose();
    }

    @Test void cleanupContinuesAfterFailuresAndReleasesComponentOwners()
    {
        GameObject object = new GameObject();
        AtomicInteger clicks = new AtomicInteger();
        Component failing = new Component() { @Override public void dispose() { super.dispose(); throw new IllegalStateException("expected"); } };
        Component remaining = new Component();
        object.addComponente(failing);
        object.addComponente(remaining);
        Mouse.infInstace().addListener(remaining, event -> clicks.incrementAndGet());
        assertThrows(IllegalStateException.class, object::dispose);
        object.dispose();
        assertEquals(1, remaining.disposals);
        Mouse.infInstace().click(0, 0);
        assertEquals(0, clicks.get());
    }

    @Test void unlocksSceneAfterCallbackFailure()
    {
        Scene scene = new Scene() { };
        GameObject first = new GameObject();
        first.addComponente(new Component() { @Override public void update(long time) { throw new IllegalStateException("expected"); } });
        scene.add(first);
        assertThrows(IllegalStateException.class, () -> scene.update(16));
        scene.remove(first);
        assertTrue(first.isDisposed());
        assertTrue(scene.getNode().isEmpty());
    }

    @Test void propagatesFixedMovementAndRollsBackToLastAcceptedPosition()
    {
        Scene scene = new Scene() { };
        GameObject mover = new GameObject();
        GameObject child = new GameObject();
        child.setPai(mover);
        mover.addComponente(Colisors.custom(new Vector2(), 2, 2));
        mover.addComponente(new Component() { @Override public void fixedUpdate(float delta) { getParent().getPosition().plus(1, 0); } });
        GameObject wall = new GameObject();
        wall.getPosition().x = 4;
        wall.addComponente(Colisors.custom(new Vector2(), 2, 2));
        scene.add(mover);
        scene.add(child);
        scene.add(wall);
        scene.fixedUpdate(1f / 60);
        scene.fixedUpdate(1f / 60);
        assertEquals(2f, mover.getPosition().x);
        scene.fixedUpdate(1f / 60);
        assertEquals(2f, mover.getPosition().x);
        assertEquals(2f, child.getPosition().x);
        scene.dispose();
    }

    @Test void reportsBothContactsUsingTheActualCollider()
    {
        Scene scene = new Scene() { };
        GameObject actor = new GameObject();
        var hit = Colisors.custom(new Vector2(), 10, 10);
        var unused = Colisors.custom(new Vector2(100, 100), 1, 1);
        List<String> contacts = new ArrayList<>();
        hit.setOnColisionAction(other -> contacts.add(other.getTag()));
        unused.setOnColisionAction(other -> fail("Wrong collider callback"));
        actor.addComponente(hit);
        actor.addComponente(unused);
        scene.add(actor);
        for (int i = 0; i < 2; i++)
        {
            GameObject wall = new GameObject();
            var collider = Colisors.custom(new Vector2(i * 5, 0), 4, 4);
            ((br.com.engine.componentes.physics.CustomCubeColisor)collider).setTag("wall" + i);
            wall.addComponente(collider);
            scene.add(wall);
        }
        scene.fixedUpdate(1f / 60);
        assertEquals(List.of("wall0", "wall1"), contacts);
        scene.dispose();
    }
}
