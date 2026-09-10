package br.com.engine.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import br.com.engine.componentes.debug.ColisorDebug;
import br.com.engine.componentes.debug.SpriteDebug;
import br.com.engine.componentes.drawable.Sprite;
import br.com.engine.componentes.scripts.Camera;
import br.com.engine.fisica.Colisao;
import br.com.engine.graphics.EngineGraphicsContext;
import br.com.engine.input.Mouse;
import br.com.engine.interfaces.CubeColisor;

public abstract class Scene
{
    private final List<GameObject> objects = new ArrayList<>();
    private final Set<GameObject> pendingAdd = new LinkedHashSet<>();
    private final Set<GameObject> pendingRemove = new LinkedHashSet<>();
    private int callbackDepth;
    private boolean flushing;
    private boolean disposed;
    private boolean initialized;

    public void setup()
    {
        if (disposed) throw new IllegalStateException("Cannot set up a disposed scene");
        if (initialized) return;
        initialized = true;
        GameObject camera = new GameObject("default_camera");
        camera.addComponente(new Camera());
        add(camera);
    }

    public String getName() { return getClass().getSimpleName(); }

    public void draw(EngineGraphicsContext graphics) { visit(GameObject::drawComponents); }

    public void update(long time)
    {
        visit(object -> {
            object.updateComponents(time);
            object.synchronizePosition();
        });
    }

    /** Updates physics and processes each overlapping object pair once, in scene order. */
    public void fixedUpdate(float deltaSeconds)
    {
        if (disposed) return;
        callbackDepth++;
        try
        {
            List<GameObject> snapshot = List.copyOf(objects);
            for (GameObject object : snapshot)
                if (active(object)) object.fixedUpdateComponents(deltaSeconds);
            for (GameObject object : snapshot)
                if (active(object)) object.synchronizePosition();

            // Detect before rolling positions back: resolving one contact must not hide another.
            record Contact(GameObject first, GameObject second, CubeColisor a, CubeColisor b) { }
            List<Contact> contacts = new ArrayList<>();
            for (int i = 0; i < snapshot.size(); i++)
            {
                GameObject first = snapshot.get(i);
                if (!active(first)) continue;
                for (int j = i + 1; j < snapshot.size(); j++)
                {
                    GameObject second = snapshot.get(j);
                    if (!active(second)) continue;
                    CubeColisor[] pair = Colisao.inColision(first, second);
                    if (pair != null) contacts.add(new Contact(first, second, pair[0], pair[1]));
                }
            }
            for (Contact contact : contacts)
            {
                if (!active(contact.first()) || !active(contact.second())) continue;
                rollback(contact.first());
                rollback(contact.second());
                contact.a().onColision(contact.b());
                contact.b().onColision(contact.a());
            }
            for (GameObject object : snapshot)
                if (active(object))
                {
                    object.synchronizePosition();
                    object.capturePosition();
                }
        }
        finally { callbackDepth--; reapDestroyed(); flushMutations(); }
    }

    private static void rollback(GameObject object)
    {
        Vector2 previous = object.getPreviusPosition();
        object.getPosition().setPosition(previous.x, previous.y);
    }

    private boolean active(GameObject object)
    {
        return !disposed && !object.isDestroy() && !object.isDisposed() && !pendingRemove.contains(object);
    }

    private void visit(Consumer<GameObject> callback)
    {
        if (disposed) return;
        callbackDepth++;
        try
        {
            for (GameObject object : List.copyOf(objects))
                if (active(object)) callback.accept(object);
        }
        finally { callbackDepth--; reapDestroyed(); flushMutations(); }
    }

    private void reapDestroyed()
    {
        for (GameObject object : objects)
            if (object.isDestroy() || object.isDisposed()) pendingRemove.add(object);
    }

    public void onCallChange() { dispose(); }

    public void dispose()
    {
        if (disposed) return;
        disposed = true;
        Mouse.infInstace().releaseOwner(this);
        clearScene();
    }

    public void add(GameObject object)
    {
        if (object == null) throw new IllegalArgumentException("Object is required");
        if (disposed || object.isDisposed()) throw new IllegalStateException("Cannot add to/from a disposed scene/object");
        if (objects.contains(object) || pendingAdd.contains(object)) return;
        pendingAdd.add(object);
        flushMutations();
    }

    public void remove(GameObject object)
    {
        if (objects.contains(object) || pendingAdd.contains(object)) pendingRemove.add(object);
        flushMutations();
    }

    public void removeAll(List<GameObject> toRemove)
    {
        for (GameObject object : List.copyOf(toRemove))
            if (objects.contains(object) || pendingAdd.contains(object)) pendingRemove.add(object);
        flushMutations();
    }

    public void clearScene()
    {
        pendingRemove.addAll(objects);
        pendingRemove.addAll(pendingAdd);
        flushMutations();
    }

    public List<GameObject> getNode() { return Collections.unmodifiableList(objects); }

    public GameObject getObject(String name)
    {
        return objects.stream().filter(object -> object.getTag().equals(name)).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("GameObject not found: " + name));
    }

    private void flushMutations()
    {
        if (callbackDepth != 0 || flushing) return;
        flushing = true;
        RuntimeException failure = null;
        try
        {
            while (!pendingRemove.isEmpty() || !pendingAdd.isEmpty())
            {
                for (GameObject object : List.copyOf(pendingRemove))
                {
                    pendingRemove.remove(object);
                    pendingAdd.remove(object);
                    objects.remove(object);
                    try { object.dispose(); } catch (RuntimeException exception) { failure = GameObject.combine(failure, exception); }
                }
                if (pendingAdd.isEmpty()) continue;
                GameObject object = pendingAdd.iterator().next();
                pendingAdd.remove(object);
                if (disposed || object.isDestroy())
                {
                    try { object.dispose(); } catch (RuntimeException exception) { failure = GameObject.combine(failure, exception); }
                    continue;
                }
                try
                {
                    if (Boolean.TRUE.equals(ControleBase.getInstance().getConfigurations().isDebugMode()))
                    {
                        if (object.getComponent(Sprite.class) != null && object.getComponent(SpriteDebug.class) == null)
                            object.addComponente(new SpriteDebug());
                        if (object.getComponent(CubeColisor.class) != null && object.getComponent(ColisorDebug.class) == null)
                            object.addComponente(new ColisorDebug());
                    }
                    objects.add(object);
                    object.setup();
                }
                catch (RuntimeException exception)
                {
                    objects.remove(object);
                    try { object.dispose(); } catch (RuntimeException cleanup) { exception.addSuppressed(cleanup); }
                    failure = GameObject.combine(failure, exception);
                }
            }
        }
        finally { flushing = false; }
        if (failure != null) throw failure;
    }
}
